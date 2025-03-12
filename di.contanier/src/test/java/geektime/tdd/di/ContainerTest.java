package geektime.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {

    Context context;

    @BeforeEach
    void setUp() {
        context = new Context();
    }

    @Nested
    public class ComponentConstruction {
        //TODO: instance
        @Test
        public void should_bind_type_to_a_specific_instance() {

            Components instance = new Components() {
            };
            context.bind(Components.class, instance);

            assertSame(instance, context.get(Components.class));
        }

        //TODO: abstract class
        //TODO: interface
        @Nested
        public class ConstructorInjection {

            //TODO: no args constructor
            @Test
            public void should_bind_type_to_a_class_with_default_constructor() {

                context.bind(Components.class, ComponentWithDefaultConstructor.class);
                Components instance = context.get(Components.class);

                assertNotNull(instance);
                assertTrue(instance instanceof ComponentWithDefaultConstructor);
            }

            //TODO: with dependencies
            @Test
            public void should_bind_type_to_a_class_with_inject_constructor() {
                Dependency dependency = new Dependency() {
                };
                context.bind(Components.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, dependency);

                Components instance = context.get(Components.class);

                assertNotNull(instance);
                assertSame(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
            }

            //TODO: A->B-C
            @Test
            public void should_bind_type_to_a_class_with_transitive_dependencies() {
                context.bind(Components.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, DependencyWithInjectConstructor.class);
                context.bind(String.class, "indirect dependency");

                Components instance = context.get(Components.class);
                assertNotNull(instance);

                Dependency dependency = ((ComponentWithInjectConstructor) instance).getDependency();
                assertNotNull(dependency);

                assertSame("indirect dependency", ((DependencyWithInjectConstructor) dependency).getDependency());
            }

            //TODO: multi inject constructors
            @Test
            public void should_throw_an_exception_if_multi_inject_constructor_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    context.bind(Components.class, ComponentWithMultiInjectConstructor.class);
                });
            }

            //TODO: no default constructor and inject constructor
            @Test
            public void should_throw_an_exception_if_no_inject_nor_default_constructors_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    context.bind(Components.class, ComponentWithNoInjectConstructorNorDefaultConstructor.class);
                });
            }

            //TODO: dependencies not exist

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