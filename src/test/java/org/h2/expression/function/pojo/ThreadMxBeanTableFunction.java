package org.h2.expression.function.pojo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.function.Function;

public class ThreadMxBeanTableFunction {

    public static ResultSet getRuntimeStat(Connection connection) throws Exception{
        HashMap<String, FieldRenderer> renderer = new HashMap<>();
        renderer.put("threads", new FieldRenderer<>(String.class, new Function<ThreadMXBean, String>() {
            ThreadLocal<ObjectMapper> mapperThreadLocal = ThreadLocal.withInitial(ObjectMapper::new);
            @Override
            public String apply(ThreadMXBean threadMXBean) {
                try {
                    return mapperThreadLocal.get().writeValueAsString(threadMXBean.getThreadInfo(
                            threadMXBean.getAllThreadIds()));
                } catch (JsonProcessingException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }));
        return H2PojoAdapter.toTable(connection, new CollectionWraper<>(ThreadMXBean.class,
                Collections.emptyMap(), ManagementFactory::getThreadMXBean));
    }
}
