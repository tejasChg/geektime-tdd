package geektime.tdd.di;

import jakarta.inject.Provider;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;


public class ContextConfig {

    private Map<Component, ComponentProvider<?>> components = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        components.put(new Component(type, null), context -> instance);
    }

    public <Type> void bind(Class<Type> type, Type instance, Annotation... qualifiers) {
        for (Annotation qualifier:qualifiers)
            components.put(new Component(type, qualifier), context -> instance);
    }

    public <Type, Implementation extends Type> void bind(Class<Type> type, Class<Implementation> implementation) {
        components.put(new Component(type, null), new InjectionProvider<>(implementation));
    }

    public <Type, Implementation extends Type> void bind(Class<Type> type, Class<Implementation> implementation, Annotation... qualifiers) {
        for (Annotation qualifier:qualifiers)
            components.put(new Component(type, qualifier), new InjectionProvider<>(implementation));
    }

    record Component(Class<?> type, Annotation qualifier) {

    }

    public Context getContext() {
        components.keySet().forEach(component -> checkDependencies(component, new Stack<>()));
        return new Context() {
            @Override
            public Optional<?> get(Ref ref) {
                if (ref.isContainer()) {
                    if (ref.getContainer() != Provider.class) {
                        return Optional.empty();
                    }
                    return Optional.ofNullable(getProvider(ref))
                        .map(provider -> (Provider<Object>) () -> provider.get(this));
                }
                return Optional.ofNullable(getProvider(ref)).map(provider -> provider.get(this));
            }

            private ComponentProvider<?> getProvider(Ref ref) {
                return components.get(new Component(ref.getComponent(),ref.getQualifier()));
            }
        };
    }

    private void checkDependencies(Component component, Stack<Class<?>> visiting) {
        for (Context.Ref dependency : components.get(component).getDependencies()) {
            if (!components.containsKey(new Component(dependency.getComponent(),dependency.getQualifier()))) {
                throw new DependencyNotFoundException(component.type, dependency.getComponent());
            }
            if (!dependency.isContainer()) {
                if (visiting.contains(dependency.getComponent())) {
                    throw new CycliDependencyFoundException(visiting);
                }
                visiting.push(dependency.getComponent());
                checkDependencies(new Component(dependency.getComponent(),dependency.getQualifier()), visiting);
                visiting.pop();
            }
        }
    }

    interface ComponentProvider<T> {
        T get(Context context);

        default List<Context.Ref> getDependencies() {
            return List.of();
        }
    }
}