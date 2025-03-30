package geektime.tdd.di;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

public class ComponentRef<ComponentType> {
    public static <ComponentType> ComponentRef<ComponentType> of(Class<ComponentType> component) {
        return new ComponentRef(component, null);
    }

    public static <ComponentType> ComponentRef<ComponentType> of(Class<ComponentType> component, Annotation qualifier) {
        return new ComponentRef(component, qualifier);
    }

    public static ComponentRef of(Type type) {
        return new ComponentRef(type, null);
    }

    private Type container;
    private Class<ComponentType> component;
    private Annotation qualifier;

    ComponentRef(Type type, Annotation qualifier) {
        init(type);
        this.qualifier = qualifier;
    }

    protected ComponentRef() {
        Type type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        init(type);
    }

    private void init(Type type) {
        if (type instanceof ParameterizedType container) {
            this.container = container.getRawType();
            this.component = (Class<ComponentType>) container.getActualTypeArguments()[0];
        } else {
            this.component = (Class<ComponentType>) type;
        }
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

    public Annotation getQualifier() {
        return qualifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ComponentRef componentRef = (ComponentRef) o;
        return Objects.equals(container, componentRef.container) && component.equals(componentRef.component);
    }

    @Override
    public int hashCode() {
        return Objects.hash(container, component);
    }
}
