package geektime.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ContainerTest {

    ContextConfig config;

    @BeforeEach
    void setUp() {
        config = new ContextConfig();
    }

    @Nested
    public class ComponentConstruction {
        @Test
        public void should_bind_type_to_a_specific_instance() {

            Components instance = new Components() {
            };
            config.bind(Components.class, instance);

            assertSame(instance, config.getContext().get(Components.class).get());
        }

        @Test
        public void should_return_empty_if_component_is_not_defined() {
            Context context = config.getContext();
            Optional<Components> optionalComponents = context.get(Components.class);
            assertTrue(optionalComponents.isEmpty());
        }

        abstract class AbstractComponent implements Components {
            @Inject
            public AbstractComponent() {
            }
        }

        //TODO: abstract class
        @Test
        public void should_throw_an_exception_if_component_is_abstract() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(AbstractComponent.class));
        }

        //TODO: interface
        @Test
        public void should_throw_an_exception_if_component_is_interface() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(Components.class));
        }

        @Test
        public void should_throw_an_exception_if_multi_inject_constructor_provided() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(ComponentWithMultiInjectConstructor.class));
        }


        @Test
        public void should_throw_an_exception_if_no_inject_nor_default_constructors_provided() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(ComponentWithNoInjectConstructorNorDefaultConstructor.class));
        }

        @Test
        public void should_include_dependency_from_inject_constructor() {
            ConstructorInjectionProvider<ComponentWithInjectConstructor> provider = new ConstructorInjectionProvider<>(ComponentWithInjectConstructor.class);
            assertArrayEquals(new Class<?>[] {Dependency.class}, provider.getDependencies().toArray());
        }

        @Nested
        public class DependencyCheck {

            @Test
            public void should_throw_an_exception_if_dependency_not_found() {
                config.bind(Components.class, ComponentWithInjectConstructor.class);

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext());

                assertEquals(Dependency.class, exception.getDependency());
                assertEquals(Components.class, exception.getComponent());

            }

            @Test
            public void should_throw_an_exception_if_cycli_dependency_found() {
                config.bind(Components.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyDependedOnComponent.class);

                CycliDependencyFoundException exception = assertThrows(CycliDependencyFoundException.class, () -> config.getContext());

                Set<Class<?>> components = exception.getComponents();
                assertEquals(2, components.size());
                assertTrue(components.contains(Components.class));
                assertTrue(components.contains(Dependency.class));
            }

            @Test
            public void should_throw_an_exception_if_transitive_cycli_dependencies_found() {
                config.bind(Components.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyDependedOnAnotherDependency.class);
                config.bind(AnotherDependency.class, AnotherDependencyDependedOnComponent.class);

                CycliDependencyFoundException exception = assertThrows(CycliDependencyFoundException.class, () -> config.getContext());

                Set<Class<?>> components = exception.getComponents();
                assertEquals(3, components.size());
                assertTrue(components.contains(Components.class));
                assertTrue(components.contains(Dependency.class));
                assertTrue(components.contains(AnotherDependency.class));
            }
        }

    }

    @Nested
    public class DependenciesSelection {

    }

    @Nested
    public class LifecycleManagement {

    }
}

interface Components {

}

interface Dependency {

}

interface AnotherDependency {

}

class ComponentWithDefaultConstructor implements Components {
    public ComponentWithDefaultConstructor() {
    }
}

class ComponentWithInjectConstructor implements Components {
    private Dependency dependency;

    @Inject
    public ComponentWithInjectConstructor(Dependency dependency) {
        this.dependency = dependency;
    }

    public Dependency getDependency() {
        return dependency;
    }
}

class ComponentWithMultiInjectConstructor implements Components {

    @Inject
    public ComponentWithMultiInjectConstructor(String name, Double value) {
    }

    @Inject
    public ComponentWithMultiInjectConstructor(Dependency dependency) {
    }

}

class ComponentWithNoInjectConstructorNorDefaultConstructor implements Components {
    public ComponentWithNoInjectConstructorNorDefaultConstructor(String name) {
    }
}

class DependencyWithInjectConstructor implements Dependency {
    private String dependency;

    @Inject
    public DependencyWithInjectConstructor(String dependency) {
        this.dependency = dependency;
    }

    public String getDependency() {
        return dependency;
    }
}

class DependencyDependedOnComponent implements Dependency {
    private Components components;

    @Inject
    public DependencyDependedOnComponent(Components components) {
        this.components = components;
    }

    public Components getComponents() {
        return components;
    }
}

class DependencyDependedOnAnotherDependency implements Dependency {
    private AnotherDependency anotherDependency;

    @Inject
    public DependencyDependedOnAnotherDependency(AnotherDependency anotherDependency) {
        this.anotherDependency = anotherDependency;
    }

}

class AnotherDependencyDependedOnComponent implements AnotherDependency {
    private Components components;

    @Inject
    public AnotherDependencyDependedOnComponent(Components components) {
        this.components = components;
    }
}