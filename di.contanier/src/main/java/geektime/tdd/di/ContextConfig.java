package geektime.tdd.di;

import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContextConfig {

    private Map<Component, ComponentProvider<?>> components = new HashMap<>();
    private Map<Class<?>, ScopeProvider> scopes = new HashMap<>();

    public ContextConfig() {
        scope(Singleton.class, SingletonProvider::new);
    }

    public <Type> void bind(Class<Type> type, Type instance) {
        components.put(new Component(type, null), context -> instance);
    }

    public <Type> void bind(Class<Type> type, Type instance, Annotation... qualifiers) {
        if (Arrays.stream(qualifiers).anyMatch(q -> !q.annotationType().isAnnotationPresent(Qualifier.class))) {
            throw new IllegalComponentException();
        }
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type, qualifier), context -> instance);
        }
    }

    public <Type, Implementation extends Type> void bind(Class<Type> type, Class<Implementation> implementation) {
        bind(type, implementation, implementation.getAnnotations());
    }

    public <Type, Implementation extends Type> void bind(
            Class<Type> type, Class<Implementation> implementation, Annotation... annotations) {
        Map<Class<?>, List<Annotation>> annotationGroups =
                Arrays.stream(annotations).collect(Collectors.groupingBy(this::typeOf, Collectors.toList()));

        if (annotationGroups.containsKey(Illegal.class)) {
            throw new IllegalComponentException();
        }

        bind(
                type,
                annotationGroups.getOrDefault(Qualifier.class, List.of()),
                createScopeProvider(implementation, annotationGroups.getOrDefault(Scope.class, List.of())));
    }

    private <Type, Implementation extends Type> ComponentProvider<Implementation> createScopeProvider(
            Class<Implementation> implementation, List<Annotation> scopes) {
        if (scopes.size() > 1) {
            throw new IllegalComponentException();
        }
        ComponentProvider<Implementation> injectionProvider = new InjectionProvider<>(implementation);
        return scopes.stream()
                .findFirst()
                .or(() -> fromType(implementation))
                .map(s -> (ComponentProvider<Implementation>) getScopeProvider(s, injectionProvider))
                .orElse(injectionProvider);
    }

    private <Type> void bind(Class<Type> type, List<Annotation> qualifiers, ComponentProvider<?> provider) {
        if (qualifiers.isEmpty()) {
            components.put(new Component(type, null), provider);
        }
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type, qualifier), provider);
        }
    }

    private <Implementation> Optional<Annotation> fromType(Class<Implementation> implementation) {
        return Arrays.stream(implementation.getAnnotations())
                .filter(a -> a.annotationType().isAnnotationPresent(Scope.class))
                .findFirst();
    }

    private Class<?> typeOf(Annotation annotation) {
        return Stream.of(Qualifier.class, Scope.class)
                .filter(i -> annotation.annotationType().isAnnotationPresent(i))
                .findFirst()
                .orElse(Illegal.class);
    }

    private @interface Illegal {}

    private ComponentProvider<?> getScopeProvider(Annotation scope, ComponentProvider<?> provider) {
        if (!scopes.containsKey(scope.annotationType())) {
            throw new IllegalComponentException();
        }
        return scopes.get(scope.annotationType()).create(provider);
    }

    public <ScopeType extends Annotation> void scope(Class<ScopeType> scope, ScopeProvider provider) {
        scopes.put(scope, provider);
    }

    public Context getContext() {
        components.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Context() {
            @Override
            public Optional<?> get(ComponentRef componentRef) {
                if (componentRef.isContainer()) {
                    if (componentRef.getContainer() != Provider.class) {
                        return Optional.empty();
                    }
                    return Optional.ofNullable(getProvider(componentRef))
                            .map(provider -> (Provider<Object>) () -> provider.get(this));
                }
                return Optional.ofNullable(getProvider(componentRef)).map(provider -> provider.get(this));
            }

            private ComponentProvider<?> getProvider(ComponentRef componentRef) {
                return components.get(componentRef.component());
            }
        };
    }

    private void checkDependencies(Component component, Stack<Component> visiting) {
        for (ComponentRef dependency : components.get(component).getDependencies()) {
            if (!components.containsKey(dependency.component())) {
                throw new DependencyNotFoundException(component, dependency.component());
            }
            if (!dependency.isContainer()) {
                if (visiting.contains(dependency.component())) {
                    throw new CycliDependencyFoundException(visiting);
                }
                visiting.push(dependency.component());
                checkDependencies(dependency.component(), visiting);
                visiting.pop();
            }
        }
    }

    interface ScopeProvider {
        ComponentProvider<?> create(ComponentProvider<?> provider);
    }
}
