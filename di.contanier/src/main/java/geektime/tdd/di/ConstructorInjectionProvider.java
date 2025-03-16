package geektime.tdd.di;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

class ConstructorInjectionProvider<Type> implements ContextConfig.ComponentProvider<Type> {
    private Constructor<Type> injectionConstructor;
    private List<Field> injectionFields;
    private List<Method> injectionMethods;

    public ConstructorInjectionProvider(Class<Type> component) {
        this.injectionConstructor = getInjectConstructor(component);
        this.injectionFields = getInjectionFields(component);
        this.injectionMethods = getInjectionMethods(component);
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
            for (Method method : injectionMethods) {
                method.setAccessible(true);
                method.invoke(instance, stream(method.getParameters()).map(p -> context.get(p.getType()).get()).toArray());
            }
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Class<?>> getDependencies() {
        return concat(concat(stream(injectionConstructor.getParameterTypes()), injectionFields.stream().map(Field::getType)),
            injectionMethods.stream().flatMap(m -> stream(m.getParameterTypes()))).collect(Collectors.toList());
    }

    private static <Type> List<Field> getInjectionFields(Class<Type> component) {
        List<Field> injectFields = new ArrayList<>();
        Class<Type> currentClass = component;
        while (currentClass != Object.class) {
            injectFields.addAll(stream(currentClass.getDeclaredFields()).filter(f -> f.isAnnotationPresent(Inject.class)).toList());
            currentClass = (Class<Type>) currentClass.getSuperclass();
        }
        return injectFields;
    }

    private static <Type> List<Method> getInjectionMethods(Class<Type> component) {
        List<Method> injectMethods = new ArrayList<>();
        Class<Type> currentClass = component;
        while (currentClass != Object.class) {
            injectMethods.addAll(stream(currentClass.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(Inject.class))
                .filter(m -> injectMethods.stream().noneMatch(o -> o.getName().equals(m.getName()) && Arrays.equals(o.getParameterTypes(), m.getExceptionTypes()))).filter(
                    m -> stream(component.getDeclaredMethods()).filter(m1 -> !m1.isAnnotationPresent(Inject.class))
                        .noneMatch(o -> o.getName().equals(m.getName()) && Arrays.equals(o.getParameterTypes(), m.getExceptionTypes()))).toList());
            currentClass = (Class<Type>) currentClass.getSuperclass();
        }
        //!!Important ensure the superclasses' methods are injected first
        Collections.reverse(injectMethods);
        return injectMethods;
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

