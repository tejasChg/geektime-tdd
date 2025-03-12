package geektime.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.stream;

public class Context {

    private Map<Class<?>, Provider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, () -> instance);
    }

    public <Type, Implementation extends Type> void bind(Class<Type> type, Class<Implementation> implementation) {
        Constructor[] injectConstructors = stream(implementation.getConstructors()).filter(c -> c.isAnnotationPresent(Inject.class)).toArray(Constructor[]::new);
        if (injectConstructors.length > 1) {
            throw new IllegalComponentException();
        }
        Boolean defaultConstructorFlag = stream(implementation.getConstructors()).filter(c -> c.getParameters().length == 0).findFirst().map(c -> false).orElse(true);
        if (injectConstructors.length == 0 && defaultConstructorFlag) {throw new IllegalComponentException();}
        providers.put(type, (Provider<Type>) () -> {
            try {
                Constructor<Implementation> injectionConstructor = getInjectConstructor(implementation);
                Object[] dependencies = stream(injectionConstructor.getParameters()).map(p -> get(p.getType())).toArray(Object[]::new);
                return (Type) injectionConstructor.newInstance(dependencies);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        List<Constructor<?>> injectConstructors = stream(implementation.getConstructors()).filter(c -> c.isAnnotationPresent(Inject.class)).toList();
        return (Constructor<Type>) injectConstructors.stream().findFirst().orElseGet(() -> {
            try {
                return implementation.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        });
    }


    public <Type> Type get(Class<Type> type) {
        return (Type) providers.get(type).get();
    }

}
