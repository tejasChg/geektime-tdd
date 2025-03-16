package geektime.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

        //TODO: abstract class
        //TODO: interface
        @Nested
        public class ConstructorInjection {

            @Test
            public void should_bind_type_to_a_class_with_default_constructor() {

                config.bind(Components.class, ComponentWithDefaultConstructor.class);
                Components instance = config.getContext().get(Components.class).get();

                assertNotNull(instance);
                assertTrue(instance instanceof ComponentWithDefaultConstructor);
            }

            @Test
            public void should_bind_type_to_a_class_with_inject_constructor() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Components.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, dependency);

                Components instance = config.getContext().get(Components.class).get();

                assertNotNull(instance);
                assertSame(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
            }

            @Test
            public void should_bind_type_to_a_class_with_transitive_dependencies() {
                config.bind(Components.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyWithInjectConstructor.class);
                config.bind(String.class, "indirect dependency");

                Components instance = config.getContext().get(Components.class).get();
                assertNotNull(instance);

                Dependency dependency = ((ComponentWithInjectConstructor) instance).getDependency();
                assertNotNull(dependency);

                assertSame("indirect dependency", ((DependencyWithInjectConstructor) dependency).getDependency());
            }

            @Test
            public void should_throw_an_exception_if_multi_inject_constructor_provided() {
                assertThrows(IllegalComponentException.class, () -> config.bind(Components.class, ComponentWithMultiInjectConstructor.class));
            }

            @Test
            public void should_throw_an_exception_if_no_inject_nor_default_constructors_provided() {
                assertThrows(IllegalComponentException.class, () -> config.bind(Components.class, ComponentWithNoInjectConstructorNorDefaultConstructor.class));
            }

            @Test
            public void should_throw_an_exception_if_dependency_not_found() {
                config.bind(Components.class, ComponentWithInjectConstructor.class);

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext());

                assertEquals(Dependency.class, exception.getDependency());
                assertEquals(Components.class, exception.getComponent());

            }

            @Test
            public void should_throw_an_exception_if_transitive_dependency_not_found() {
                config.bind(Components.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyWithInjectConstructor.class);

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext().get(Components.class));

                assertEquals(String.class, exception.getDependency());
                assertEquals(Dependency.class, exception.getComponent());
            }

            @Test
            public void should_throw_an_exception_if_cycli_dependency_found() {
                config.bind(Components.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyDependedOnComponent.class);

                CycliDependencyFoundException exception = assertThrows(CycliDependencyFoundException.class, () -> config.getContext().get(Components.class));

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

                CycliDependencyFoundException exception = assertThrows(CycliDependencyFoundException.class, () -> config.getContext().get(Components.class));

                Set<Class<?>> components = exception.getComponents();
                assertEquals(3, components.size());
                assertTrue(components.contains(Components.class));
                assertTrue(components.contains(Dependency.class));
                assertTrue(components.contains(AnotherDependency.class));
            }
        }

        @Nested
        public class FieldInjection {

        }

        @Nested
        public class MethodInjection {

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