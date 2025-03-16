package geektime.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

        abstract class AbstractComponent implements Components {
            @Inject
            public AbstractComponent() {
            }
        }

        //TODO: abstract class
        @Test
        public void should_throw_an_exception_if_component_is_abstract() {
            assertThrows(IllegalComponentException.class, ()-> new ConstructorInjectionProvider<>(AbstractComponent.class));
        }

        //TODO: interface
        @Test
        public void should_throw_an_exception_if_component_is_interface() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(Components.class));
        }

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

        @Nested
        public class FieldInjection {
            static class ComponentWithFieldInjection {
                @Inject
                Dependency dependency;
            }

            static class SubClassWithFieldInjection extends ComponentWithFieldInjection {

            }

            @Test
            public void should_inject_dependency_via_field() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(ComponentWithFieldInjection.class, ComponentWithFieldInjection.class);

                ComponentWithFieldInjection component = config.getContext().get(ComponentWithFieldInjection.class).get();

                assertSame(dependency, component.dependency);
            }

            @Test
            public void should_inject_dependency_via_superclass_inject_field() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(SubClassWithFieldInjection.class, SubClassWithFieldInjection.class);

                SubClassWithFieldInjection component = config.getContext().get(SubClassWithFieldInjection.class).get();

                assertSame(dependency, component.dependency);
            }

            //TODO throw exception if field is final
            static class FinalInjectField {
                @Inject
                final Dependency dependency = null;
            }

            @Test
            public void should_throw_exception_if_field_is_final() {
                assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(FinalInjectField.class));
            }

            @Test
            public void should_inject_field_dependency_in_dependencies() {
                ConstructorInjectionProvider<ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class);
                assertArrayEquals(new Class<?>[] {Dependency.class}, provider.getDependencies().toArray());
            }
        }

        @Nested
        public class MethodInjection {
            static class InjectMethodWithNoDependency {
                boolean called = false;

                @Inject
                void install() {
                    this.called = true;
                }
            }

            //TODO  inject method with no dependencies will be called
            @Test
            public void should_call_inject_method_even_if_no_dependency_declared() {
                config.bind(InjectMethodWithNoDependency.class, InjectMethodWithNoDependency.class);

                InjectMethodWithNoDependency instance = config.getContext().get(InjectMethodWithNoDependency.class).get();

                assertTrue(instance.called);
            }

            static class InjectMethodWithDependency {
                Dependency dependency;

                @Inject
                void install(Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            //TODO  inject method with dependencies will be injected
            @Test
            public void should_inject_dependency_via_inject_method() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(InjectMethodWithDependency.class, InjectMethodWithDependency.class);

                InjectMethodWithDependency instance = config.getContext().get(InjectMethodWithDependency.class).get();
                assertSame(dependency, instance.dependency);
            }

            //TODO  inject override inject method from superclass
            static class SuperClassWithInjectMethod {
                int superCalled = 0;

                @Inject
                void install() {
                    superCalled++;
                }
            }

            static class SubClassWithInjectMethod extends SuperClassWithInjectMethod {
                int subCalled = 0;

                @Inject
                void installAnother() {
                    subCalled = superCalled + 1;
                }
            }

            @Test
            public void should_inject_dependencies_via_inject_method_from_superclass() {
                config.bind(SubClassWithInjectMethod.class, SubClassWithInjectMethod.class);

                SubClassWithInjectMethod instance = config.getContext().get(SubClassWithInjectMethod.class).get();
                assertEquals(1, instance.superCalled);
                assertEquals(2, instance.subCalled);
            }

            static class SubClassOverrideSuperClassWithInject extends SuperClassWithInjectMethod {

                @Inject
                void install() {
                    super.install();
                }
            }

            @Test
            public void should_only_call_once_if_subclass_override_superclass_inject_method_with_inject() {
                config.bind(SubClassOverrideSuperClassWithInject.class, SubClassOverrideSuperClassWithInject.class);

                SubClassOverrideSuperClassWithInject instance = config.getContext().get(SubClassOverrideSuperClassWithInject.class).get();
                assertEquals(1, instance.superCalled);
            }

            static class SubClassOverrideSuperClassWithNoInject extends SuperClassWithInjectMethod {

                void install() {
                    super.install();
                }
            }

            @Test
            public void should_not_call_inject_method_if_override_with_no_inject() {
                config.bind(SubClassOverrideSuperClassWithNoInject.class, SubClassOverrideSuperClassWithNoInject.class);

                SubClassOverrideSuperClassWithNoInject instance = config.getContext().get(SubClassOverrideSuperClassWithNoInject.class).get();

                assertEquals(0, instance.superCalled);
            }

            //TODO  include dependencies from inject methods
            @Test
            public void should_include_dependencies_from_inject_method() {
                ConstructorInjectionProvider<InjectMethodWithDependency> provider = new ConstructorInjectionProvider<>(InjectMethodWithDependency.class);
                assertArrayEquals(new Class<?>[] {Dependency.class}, provider.getDependencies().toArray());
            }

            //TODO  inject throw exception if type parameter defined
            static class InjectMethodWithTypeParameter {
                @Inject
                <T> void install(T t) {
                }
            }

            @Test
            public void should_throw_exception_if_inject_method_has_type_parameter() {
                assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(InjectMethodWithTypeParameter.class));
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