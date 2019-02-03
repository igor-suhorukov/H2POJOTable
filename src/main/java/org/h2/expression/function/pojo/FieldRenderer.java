package org.h2.expression.function.pojo;

import java.io.Serializable;
import java.util.function.Function;

public class FieldRenderer<T, R extends Serializable>{
    private Function<T, R> renderer;
    private Class<R> returnType;

    public Function<T, R> getRenderer() {
        return renderer;
    }

    public Class<R> getReturnType() {
        return returnType;
    }

    public FieldRenderer(Class<R> type, Function<T, R> renderer) {
        if(renderer==null){
            throw new NullPointerException();
        }
        if(type == null){
            throw new NullPointerException();
        }
        this.returnType = type;
        this.renderer = renderer;
    }
}
