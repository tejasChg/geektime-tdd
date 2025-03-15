package geektime.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.stream;

public class ContextConfig {

    private Map<Class<?>, Provider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, () -> instance);
    }

    public <Type, Implementation extends Type> void bind(Class<Type> type, Class<Implementation> implementation) {
        Constructor<Implementation> injectionConstructor = getInjectConstructor(implementation);
        providers.put(type, new ConstructorInjectionProvider<>(type, injectionConstructor));
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        List<Constructor<?>> injectConstructors = stream(implementation.getConstructors()).filter(c -> c.isAnnotationPresent(Inject.class)).toList();
        if (injectConstructors.size() > 1) {
            throw new IllegalComponentException();
        }
        return (Constructor<Type>) injectConstructors.stream().findFirst().orElseGet(() -> {
            try {
                return implementation.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new IllegalComponentException();
            }
        });
    }

    public Context getContext() {
        return new Context() {
            @Override
            public <Type> Optional<Type> get(Class<Type> type) {
                return Optional.ofNullable(providers.get(type)).map(provider -> (Type) provider.get());
            }
        };
    }

    class ConstructorInjectionProvider<Type> implements Provider<Type> {
        private Constructor<Type> injectionConstructor;
        private Class<?> componentType;

        private boolean constructing = false;

        public ConstructorInjectionProvider(Class<?> componentType, Constructor<Type> injectionConstructor) {
            this.componentType = componentType;
            this.injectionConstructor = injectionConstructor;
        }

        @Override
        public Type get() {
            if (constructing) {
                throw new CycliDependencyFoundException(componentType);
            }
            try {
                constructing = true;
                Object[] dependencies =
                    stream(injectionConstructor.getParameters()).map(
                        p -> {
                            Class<?> type = p.getType();
                            return getContext().get(type).orElseThrow(() -> new DependencyNotFoundException(p.getType(), componentType));
                        }).toArray(Object[]::new);
                return injectionConstructor.newInstance(dependencies);
            } catch (CycliDependencyFoundException e) {
                throw new CycliDependencyFoundException(componentType, e);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            } finally {
                constructing = false;
            }
        }
    }

}
