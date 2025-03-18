package geektime.tdd.di;

import jakarta.inject.Inject;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

class InjectionProvider<Type> implements ContextConfig.ComponentProvider<Type> {
    private Constructor<Type> injectionConstructor;
    private List<Field> injectionFields;
    private List<Method> injectionMethods;

    public InjectionProvider(Class<Type> component) {
        if (Modifier.isAbstract(component.getModifiers())) {
            throw new IllegalComponentException();
        }
        this.injectionConstructor = getInjectConstructor(component);
        this.injectionFields = getInjectionFields(component);
        this.injectionMethods = getInjectionMethods(component);

        if (injectionFields.stream().anyMatch(f -> Modifier.isFinal(f.getModifiers()))) {
            throw new IllegalComponentException();
        }
        if (injectionMethods.stream().anyMatch(m -> m.getTypeParameters().length != 0)) {
            throw new IllegalComponentException();
        }
    }

    @Override
    public Type get(Context context) {
        try {
            Type instance = injectionConstructor.newInstance(toDependencies(context, injectionConstructor));
            for (Field field : injectionFields) {
                field.setAccessible(true);
                field.set(instance, toDependency(context, field));
            }
            for (Method method : injectionMethods) {
                method.setAccessible(true);
                method.invoke(instance, toDependencies(context, method));
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

    private static <Type> List<Method> getInjectionMethods(Class<Type> component) {
        List<Method> injectMethods = traverse(component, (methods, current) -> injectable(current.getDeclaredMethods())
            .filter(m -> isOverrideByInjectMethod(m, methods))
            .filter(m -> isOverrideByNoInjectMethod(component, m))
            .toList());
        //!!Important ensure the superclasses' methods are injected first
        Collections.reverse(injectMethods);
        return injectMethods;
    }

    private static <Type> List<Field> getInjectionFields(Class<Type> component) {
        return traverse(component, (injectionFields, current) -> injectable(current.getDeclaredFields()).toList());
    }

    private static boolean isOverride(Method m, Method o) {
        return o.getName().equals(m.getName()) && Arrays.equals(o.getParameterTypes(), m.getExceptionTypes());
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        List<Constructor<?>> injectConstructors = injectable(implementation.getConstructors()).toList();
        if (injectConstructors.size() > 1) {
            throw new IllegalComponentException();
        }
        return (Constructor<Type>) injectConstructors.stream().findFirst().orElseGet(() -> defaultConstructor(implementation));
    }

    private static <T extends AnnotatedElement> Stream<T> injectable(T[] declaredFields) {
        return stream(declaredFields).filter(f -> f.isAnnotationPresent(Inject.class));
    }

    private static <Type> boolean isOverrideByNoInjectMethod(Class<Type> component, Method m) {
        return stream(component.getDeclaredMethods()).filter(m1 -> !m1.isAnnotationPresent(Inject.class)).noneMatch(o -> isOverride(m, o));
    }

    private static boolean isOverrideByInjectMethod(Method m, List<Method> injectMethods) {
        return injectMethods.stream().noneMatch(o -> isOverride(m, o));
    }

    private static Object[] toDependencies(Context context, Executable executable) {
        return stream(executable.getParameterTypes())
            .map(t -> context.get(t).get()).toArray(Object[]::new);
    }

    private static Object toDependency(Context context, Field field) {
        return context.get(field.getType()).get();
    }

    private static <Type> Constructor<Type> defaultConstructor(Class<Type> implementation) {
        try {
            return implementation.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalComponentException();
        }
    }

    private static <T> List<T> traverse(Class<?> component, BiFunction<List<T>, Class<?>, List<T>> finder) {
        List<T> members = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            members.addAll(finder.apply(members, current));
            current =  current.getSuperclass();
        }
        return members;
    }
}

