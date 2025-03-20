package geektime.tdd.di;

import jakarta.inject.Provider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;


public class ContextConfig {

    private Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, (ComponentProvider<Type>) context -> instance);
    }

    public <Type, Implementation extends Type> void bind(Class<Type> type, Class<Implementation> implementation) {
        providers.put(type, new InjectionProvider<>(implementation));
    }

    public Context getContext() {
        providers.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Context() {
            @Override
            public Optional get(Type type) {
                if (isContainerType(type)) {
                    return getContainerType((ParameterizedType) type);
                }
                return getComponent((Class<?>) type);
            }

            private <Type> Optional<Type> getComponent(Class<Type> type) {
                return Optional.ofNullable(providers.get(type)).map(provider -> (Type) provider.get(this));
            }

            private Optional getContainerType(ParameterizedType type) {
                // Step 1: Check if the raw type is Provider
                if (type.getRawType() != Provider.class) {
                    return Optional.empty();
                }
                // Step 2: Extract the component type from the ParameterizedType
                Class<?> componentType = getComponentType(type);
                // Step 3: Retrieve the provider for the component type
                return Optional.ofNullable(providers.get(componentType))
                    // Step 4: Wrap the provider in a Provider instance
                    .map(provider -> (Provider<Object>) () -> provider.get(this));
            }
        };
    }

    /**
     * Extracts the component type from a container type.
     * For example, if the type is Provider<String>, it returns String.
     */
    private static Class<?> getComponentType(Type type) {
        return (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
    }

    /**
     * Checks if a type is a container type.
     * For example, Provider<String> is a container type.
     */
    private static boolean isContainerType(Type type) {
        return type instanceof ParameterizedType;
    }

    private void checkDependencies(Class<?> component, Stack<Class<?>> visiting) {
        for (Type dependency : providers.get(component).getDependencies()) {
            if (isContainerType(dependency)) {
                checkContainerTypeDependency(component, dependency);
            } else {
                checkComponentDependency(component, visiting, (Class<?>) dependency);
            }
        }
    }

    private void checkContainerTypeDependency(Class<?> component, Type dependency) {
        if (!providers.containsKey(getComponentType(dependency))) {
            throw new DependencyNotFoundException(component, getComponentType(dependency));
        }
    }

    private void checkComponentDependency(Class<?> component, Stack<Class<?>> visiting, Class<?> dependency) {
        if (!providers.containsKey(dependency)) {
            throw new DependencyNotFoundException(component, dependency);
        }
        if (visiting.contains(dependency)) {
            throw new CycliDependencyFoundException(visiting);
        }
        visiting.push(dependency);
        checkDependencies(dependency, visiting);
        visiting.pop();
    }

    interface ComponentProvider<T> {
        T get(Context context);

        default List<Type> getDependencies() {
            return List.of();
        }
    }

}
