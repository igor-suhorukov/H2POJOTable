package org.h2.expression.function.pojo;

import org.h2.tools.SimpleResultSet;
import org.h2.value.DataType;
import org.h2.value.Value;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;

public class H2PojoAdapter {

    private static final int DEFAULT_PRECISION = Integer.MAX_VALUE;
    public static ResultSet toTable(Connection connection, CollectionWraper table) throws SQLException {
        return toTable(connection, table, false);
    }

    public static ResultSet toTable(@SuppressWarnings("unused") Connection connection, CollectionWraper wraper,
                                    boolean failOnUnknownFields) throws SQLException {
        SimpleResultSet result = new SimpleResultSet();
        List<Function<Object, Serializable>> descriptors = defineColumnTypes(wraper, result, failOnUnknownFields);
        if (isDataRequest(connection)) {
            fillDataRequest(result, wraper, descriptors);
        }
        return result;
    }

    private static List<Function<Object, Serializable>> defineColumnTypes(CollectionWraper wraper,
                                                                          SimpleResultSet result,
                                                                          boolean failOnUnknownFields) {
        Set<String> syntheticFields = wraper.getRenderer().keySet();
        Set<String> unknownFields = new TreeSet<>();
        List<Function<Object, Serializable>> descriptors = new ArrayList<>();

        for(PropertyDescriptor descriptor: getPropertyDescriptors(wraper.getEntityType())){
            Method readMethod = descriptor.getReadMethod();
            if(readMethod!=null){
                FieldRenderer rendererFuction = (FieldRenderer) wraper.getRenderer().get(descriptor.getName());
                if(rendererFuction!=null) {
                    syntheticFields.remove(descriptor.getName());
                    descriptors.add(rendererFuction.getRenderer());
                    addColumn(result, DataType.convertTypeToSQLType(DataType.getTypeFromClass(
                            rendererFuction.getReturnType())), descriptor.getName());
                } else {
                    int returnType = DataType.getTypeFromClass(readMethod.getReturnType());
                    if(Value.JAVA_OBJECT!= returnType){
                        descriptors.add(media -> {
                            try {
                                return (Serializable) descriptor.getReadMethod().invoke(media);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                        String name = descriptor.getName();
                        addColumn(result, DataType.convertTypeToSQLType(returnType), name);

                    } else {
                        if(descriptor.getReadMethod()!=null) {
                            unknownFields.add(descriptor.getName());
                        }
                    }
                }
            }
        }
        validateUnknownFields(wraper, failOnUnknownFields, unknownFields);
        appendSyntheticFields(wraper, result, syntheticFields, descriptors);
        return descriptors;
    }

    private static void validateUnknownFields(CollectionWraper table, boolean failOnUnknownFields,
                                              Set<String> unknownFields) {
        if(!unknownFields.isEmpty() && failOnUnknownFields){
            throw new UnsupportedOperationException("Class "+table.getEntityType().getName()+
                    " contain object fields without custom transformer: "+
                    String.join(", ", unknownFields));
        }
    }

    private static void appendSyntheticFields(CollectionWraper table, SimpleResultSet result,
                                              Set<String> syntheticFields,
                                              List<Function<Object, Serializable>> descriptors) {
        for(String synthetic: syntheticFields){
            FieldRenderer rendererFuction = (FieldRenderer) table.getRenderer().get(synthetic);
            if(rendererFuction!=null) {
                descriptors.add(rendererFuction.getRenderer());
                addColumn(result, DataType.convertTypeToSQLType(
                        DataType.getTypeFromClass(rendererFuction.getReturnType())), synthetic);
            }
        }
    }

    private static void fillDataRequest(SimpleResultSet result, CollectionWraper table,
                                        List<Function<Object, Serializable>> descriptors) {
        Collection items = (Collection) table.getSupplier().get();
        Object[] values = new Object[descriptors.size()];
        for(Object item: items){
            for (int i = 0; i < descriptors.size(); i++) {
                values[i] = descriptors.get(i).apply(item);
            }
            result.addRow(values);
        }
    }

    private static PropertyDescriptor[] getPropertyDescriptors(Class clazz){
        try {
            return Introspector.getBeanInfo(clazz).getPropertyDescriptors();
        } catch (IntrospectionException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean isDataRequest(Connection connection) throws SQLException {
        return !connection.getMetaData().getURL().startsWith("jdbc:columnlist:");
    }

    private static void addColumn(SimpleResultSet result, int type, String name) {
        result.addColumn(name, type, DEFAULT_PRECISION, DEFAULT_PRECISION);
    }
}
