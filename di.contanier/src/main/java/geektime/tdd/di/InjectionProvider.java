package geektime.tdd.di;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

class InjectionProvider<T> implements ComponentProvider<T> {
    private Injectable<Constructor<T>> injectConstructor;
    private List<Injectable<Method>> injectMethods;
    private List<Injectable<Field>> injectFields;

    public InjectionProvider(Class<T> component) {
        if (Modifier.isAbstract(component.getModifiers())) {
            throw new IllegalComponentException();
        }
        this.injectConstructor = getInjectConstructor(component);
        this.injectMethods = getInjectMethods(component);
        this.injectFields = getInjectFields(component);
        if (injectFields.stream().map(Injectable::element).anyMatch(f -> Modifier.isFinal(f.getModifiers()))) {
            throw new IllegalComponentException();
        }
        if (injectMethods.stream().map(Injectable::element).anyMatch(m -> m.getTypeParameters().length != 0)) {
            throw new IllegalComponentException();
        }
    }

    @Override
    public T get(Context context) {
        try {
            T instance = injectConstructor.element().newInstance(injectConstructor.toDependencies(context));
            for (Injectable<Field> field : injectFields) {
                field.element().setAccessible(true);
                field.element().set(instance, field.toDependencies(context)[0]);
            }
            for (Injectable<Method> method : injectMethods) {
                method.element().invoke(instance, method.toDependencies(context));
            }
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ComponentRef<?>> getDependencies() {
        return concat(concat(Stream.of(injectConstructor), injectFields.stream()), injectMethods.stream())
                .flatMap(i -> stream(i.required()))
                .toList();
    }

    static record Injectable<Element extends AccessibleObject>(Element element, ComponentRef<?>[] required) {

        static <Element extends Executable> Injectable<Element> of(Element constructor) {
            return new Injectable<>(
                    constructor,
                    stream(constructor.getParameters())
                            .map(Injectable::toComponentRef)
                            .toArray(ComponentRef[]::new));
        }

        static Injectable<Field> of(Field field) {
            return new Injectable<>(field, new ComponentRef[] {toComponentRef(field)});
        }

        Object[] toDependencies(Context context) {
            return stream(required).map(context::get).map(Optional::get).toArray();
        }

        private static ComponentRef toComponentRef(Field field) {
            return ComponentRef.of(field.getGenericType(), getQualifier(field));
        }

        private static ComponentRef toComponentRef(Parameter parameter) {
            return ComponentRef.of(parameter.getParameterizedType(), getQualifier(parameter));
        }

        private static Annotation getQualifier(AnnotatedElement element) {
            List<Annotation> annotations = stream(element.getAnnotations())
                    .filter(q -> q.annotationType().isAnnotationPresent(Qualifier.class))
                    .toList();
            if (annotations.size() > 1) {
                throw new IllegalComponentException();
            }
            return annotations.stream().findFirst().orElse(null);
        }
    }

    private static <T> Injectable<Constructor<T>> getInjectConstructor(Class<T> component) {
        List<Constructor<?>> injectConstructors =
                injectable(component.getConstructors()).toList();
        if (injectConstructors.size() > 1) {
            throw new IllegalComponentException();
        }
        return Injectable.of((Constructor<T>)
                injectConstructors.stream().findFirst().orElseGet(() -> defaultConstructor(component)));
    }

    private static <T> List<Injectable<Method>> getInjectMethods(Class<T> component) {
        List<Method> injectMethods = traverse(component, (methods, current) -> injectable(current.getDeclaredMethods())
                .filter(m1 -> isOverrideByInjectMethod(m1, methods))
                .filter(m1 -> isOverrideByNoInjectMethod(component, m1))
                .toList());
        // !!Important ensure the superclasses' methods are injected first
        Collections.reverse(injectMethods);
        return injectMethods.stream().map(m -> Injectable.of(m)).toList();
    }

    private static <T> List<Injectable<Field>> getInjectFields(Class<T> component) {
        return InjectionProvider.<Field>traverse(
                        component, (injectionFields, current) -> injectable(current.getDeclaredFields())
                                .toList())
                .stream()
                .map(f -> Injectable.of(f))
                .toList();
    }

    private static boolean isOverride(Method m, Method o) {
        return o.getName().equals(m.getName()) && Arrays.equals(o.getParameterTypes(), m.getParameterTypes());
    }

    private static <T extends AnnotatedElement> Stream<T> injectable(T[] declaredFields) {
        return stream(declaredFields).filter(f -> f.isAnnotationPresent(Inject.class));
    }

    private static <T> boolean isOverrideByNoInjectMethod(Class<T> component, Method m) {
        return stream(component.getDeclaredMethods())
                .filter(m1 -> !m1.isAnnotationPresent(Inject.class))
                .noneMatch(o -> isOverride(m, o));
    }

    private static boolean isOverrideByInjectMethod(Method m, List<Method> injectMethods) {
        return injectMethods.stream().noneMatch(o -> isOverride(m, o));
    }

    private static <T> Constructor<T> defaultConstructor(Class<T> implementation) {
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
            current = current.getSuperclass();
        }
        return members;
    }
}
