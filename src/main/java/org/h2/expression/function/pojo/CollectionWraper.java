package org.h2.expression.function.pojo;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

public class CollectionWraper<T>{
    private Class<T> entityType;
    private Supplier<Collection<T>> supplier;
    private Map<String, FieldRenderer> renderer;

    public CollectionWraper(Class<T> entityType, Supplier<Collection<T>> supplier, Map<String, FieldRenderer> renderer) {
        if(entityType==null){
            throw new NullPointerException("entityType");
        }
        this.entityType = entityType;
        if(supplier==null){
            throw new NullPointerException("supplier");
        }
        this.supplier = supplier;
        if(renderer==null){
            throw new NullPointerException("renderer");
        }
        this.renderer = renderer;
    }

    public CollectionWraper(Class<T> entityType, Map<String, FieldRenderer> renderer, Supplier<T> supplier) {
        this(entityType, () -> Collections.singletonList(supplier.get()), renderer);
        if(supplier==null){
            throw new NullPointerException("supplier");
        }
    }


    public Class<?> getEntityType() {
        return entityType;
    }

    public Supplier<Collection<T>> getSupplier() {
        return supplier;
    }

    public Map<String, FieldRenderer> getRenderer() {
        return renderer;
    }
}
