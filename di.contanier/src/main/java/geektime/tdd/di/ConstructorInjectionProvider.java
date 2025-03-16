package geektime.tdd.di;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

class ConstructorInjectionProvider<Type> implements ContextConfig.ComponentProvider<Type> {
    private Constructor<Type> injectionConstructor;
    private List<Field> injectionFields;

    public ConstructorInjectionProvider(Class<Type> component) {
        this.injectionConstructor = getInjectConstructor(component);
        this.injectionFields = getInjectionFields(component);
    }

    @Override
    public Type get(Context context) {
        try {
            Object[] dependencies = stream(injectionConstructor.getParameters()).map(p -> {
                Class<?> type = p.getType();
                return context.get(type).get();
            }).toArray(Object[]::new);
            Type instance = injectionConstructor.newInstance(dependencies);
            for (Field field : injectionFields) {
                field.setAccessible(true);
                field.set(instance, context.get(field.getType()).get());
            }
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Class<?>> getDependencies() {
        return Stream.concat(stream(injectionConstructor.getParameterTypes()),
            injectionFields.stream().map(Field::getType)).collect(Collectors.toList());
    }

    private static <Type> List<Field> getInjectionFields(Class<Type> component) {
        List<Field> injectFields = new ArrayList<>();
        Class<Type> currentClass = component;
        while(currentClass != Object.class) {
            injectFields.addAll(stream(currentClass.getDeclaredFields()).filter(f -> f.isAnnotationPresent(Inject.class)).toList());
            currentClass = (Class<Type>) currentClass.getSuperclass();
        }
        return injectFields;
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        List<Constructor<?>> injectConstructors = stream(implementation.getConstructors()).filter(c -> c.isAnnotationPresent(Inject.class)).toList();
        if (injectConstructors.size() > 1) {
            throw new IllegalComponentException();
        }
        return (Constructor<Type>) injectConstructors.stream().findFirst().orElseGet(() -> {
            try {
                return implementation.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                throw new IllegalComponentException();
            }
        });
    }

}

