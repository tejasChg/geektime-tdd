package geektime.tdd.di;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

class ConstructorInjectionProvider<Type> implements ContextConfig.ComponentProvider<Type> {
    private Constructor<Type> injectionConstructor;

    public ConstructorInjectionProvider(Class<Type> component) {
        this.injectionConstructor = getInjectConstructor(component);
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

    @Override
    public Type get(Context context) {
        try {
            Object[] dependencies = stream(injectionConstructor.getParameters()).map(p -> {
                Class<?> type = p.getType();
                return context.get(type).get();
            }).toArray(Object[]::new);
            return injectionConstructor.newInstance(dependencies);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Class<?>> getDependencies() {
        return stream(injectionConstructor.getParameters()).map(p -> p.getType()).collect(Collectors.toList());
    }
}
