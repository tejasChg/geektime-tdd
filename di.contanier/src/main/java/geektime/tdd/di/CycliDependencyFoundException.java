package geektime.tdd.di;

import java.util.HashSet;
import java.util.Set;

public class CycliDependencyFoundException extends RuntimeException {
    private Set<Class<?>> components = new HashSet<>();

    public CycliDependencyFoundException(Class<?> componentType) {
        components.add(componentType);
    }

    public CycliDependencyFoundException(Class<?> componentType, CycliDependencyFoundException e) {
        components.add(componentType);
        components.addAll(e.getComponents());
    }

    public Set<Class<?>> getComponents() {
        return components;
    }
}
