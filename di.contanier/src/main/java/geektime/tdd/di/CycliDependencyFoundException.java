package geektime.tdd.di;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CycliDependencyFoundException extends RuntimeException {
    private Set<Component> components = new HashSet<>();

    public CycliDependencyFoundException(List<Component> visiting) {
        components.addAll(visiting);
    }

    public Class<?>[] getComponents() {
        return components.stream().map(Component::type).toArray(Class[]::new);
    }
}
