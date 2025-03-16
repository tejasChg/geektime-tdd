package geektime.tdd.di;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class CycliDependencyFoundException extends RuntimeException {
    private Set<Class<?>> components = new HashSet<>();

    public CycliDependencyFoundException(Stack<Class<?>> visiting) {
        components.addAll(visiting);
    }

    public Set<Class<?>> getComponents() {
        return components;
    }
}
