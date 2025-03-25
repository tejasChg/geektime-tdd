package geektime.tdd.di;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class Ref {
    private Type container;
    private Class<?> component;

    Ref(ParameterizedType container) {
        this.container = container.getRawType();
        this.component = (Class<?>) container.getActualTypeArguments()[0];
    }

    Ref(Class<?> component) {
        this.component = component;
    }

    static Ref of(Type type) {
        if (type instanceof ParameterizedType container) {
            return new Ref(container);
        }
        return new Ref((Class<?>) type);
    }

    public Type getContainer() {
        return container;
    }

    public Class<?> getComponent() {
        return component;
    }

    public boolean isContainer() {
        return container != null;
    }
}
