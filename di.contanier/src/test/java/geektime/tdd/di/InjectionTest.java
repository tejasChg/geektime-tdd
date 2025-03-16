package geektime.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InjectionTest {

    ContextConfig config;

    @BeforeEach
    void setUp() {
        config = new ContextConfig();
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

    }

    @Nested
    public class FieldInjection {
        static class ComponentWithFieldInjection {
            @Inject
            Dependency dependency;
        }

        static class SubClassWithFieldInjection extends FieldInjection.ComponentWithFieldInjection {

        }

        @Test
        public void should_inject_dependency_via_field() {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(FieldInjection.ComponentWithFieldInjection.class, FieldInjection.ComponentWithFieldInjection.class);

            FieldInjection.ComponentWithFieldInjection component = config.getContext().get(FieldInjection.ComponentWithFieldInjection.class).get();

            assertSame(dependency, component.dependency);
        }

        @Test
        public void should_inject_dependency_via_superclass_inject_field() {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(FieldInjection.SubClassWithFieldInjection.class, FieldInjection.SubClassWithFieldInjection.class);

            FieldInjection.SubClassWithFieldInjection component = config.getContext().get(FieldInjection.SubClassWithFieldInjection.class).get();

            assertSame(dependency, component.dependency);
        }

        //TODO throw exception if field is final
        static class FinalInjectField {
            @Inject
            final Dependency dependency = null;
        }

        @Test
        public void should_throw_exception_if_field_is_final() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(FieldInjection.FinalInjectField.class));
        }

        @Test
        public void should_inject_field_dependency_in_dependencies() {
            ConstructorInjectionProvider<FieldInjection.ComponentWithFieldInjection> provider =
                new ConstructorInjectionProvider<>(FieldInjection.ComponentWithFieldInjection.class);
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
            config.bind(MethodInjection.InjectMethodWithNoDependency.class, MethodInjection.InjectMethodWithNoDependency.class);

            MethodInjection.InjectMethodWithNoDependency instance = config.getContext().get(MethodInjection.InjectMethodWithNoDependency.class).get();

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
            config.bind(MethodInjection.InjectMethodWithDependency.class, MethodInjection.InjectMethodWithDependency.class);

            MethodInjection.InjectMethodWithDependency instance = config.getContext().get(MethodInjection.InjectMethodWithDependency.class).get();
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

        static class SubClassWithInjectMethod extends MethodInjection.SuperClassWithInjectMethod {
            int subCalled = 0;

            @Inject
            void installAnother() {
                subCalled = superCalled + 1;
            }
        }

        @Test
        public void should_inject_dependencies_via_inject_method_from_superclass() {
            config.bind(MethodInjection.SubClassWithInjectMethod.class, MethodInjection.SubClassWithInjectMethod.class);

            MethodInjection.SubClassWithInjectMethod instance = config.getContext().get(MethodInjection.SubClassWithInjectMethod.class).get();
            assertEquals(1, instance.superCalled);
            assertEquals(2, instance.subCalled);
        }

        static class SubClassOverrideSuperClassWithInject extends MethodInjection.SuperClassWithInjectMethod {

            @Inject
            void install() {
                super.install();
            }
        }

        @Test
        public void should_only_call_once_if_subclass_override_superclass_inject_method_with_inject() {
            config.bind(MethodInjection.SubClassOverrideSuperClassWithInject.class, MethodInjection.SubClassOverrideSuperClassWithInject.class);

            MethodInjection.SubClassOverrideSuperClassWithInject instance = config.getContext().get(MethodInjection.SubClassOverrideSuperClassWithInject.class).get();
            assertEquals(1, instance.superCalled);
        }

        static class SubClassOverrideSuperClassWithNoInject extends MethodInjection.SuperClassWithInjectMethod {

            void install() {
                super.install();
            }
        }

        @Test
        public void should_not_call_inject_method_if_override_with_no_inject() {
            config.bind(MethodInjection.SubClassOverrideSuperClassWithNoInject.class, MethodInjection.SubClassOverrideSuperClassWithNoInject.class);

            MethodInjection.SubClassOverrideSuperClassWithNoInject instance = config.getContext().get(MethodInjection.SubClassOverrideSuperClassWithNoInject.class).get();

            assertEquals(0, instance.superCalled);
        }

        //TODO  include dependencies from inject methods
        @Test
        public void should_include_dependencies_from_inject_method() {
            ConstructorInjectionProvider<MethodInjection.InjectMethodWithDependency> provider =
                new ConstructorInjectionProvider<>(MethodInjection.InjectMethodWithDependency.class);
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
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(MethodInjection.InjectMethodWithTypeParameter.class));
        }
    }
}
