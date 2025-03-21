package geektime.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InjectionTest {

    private Dependency dependency = mock(Dependency.class);

    private Context context = mock(Context.class);

    @BeforeEach
    void setUp() {
        when(context.get(Dependency.class)).thenReturn(Optional.of(dependency));
    }

    @Nested
    public class ConstructorInjection {

        @Nested
        class Injection {
            static class DefaultConstructor implements Component {
                public DefaultConstructor() {
                }
            }

            @Test
            public void should_call_default_constructor_if_no_inject_constructor() {
                DefaultConstructor instance = new ConstructorInjectionProvider<>(DefaultConstructor.class).get(context);

                assertNotNull(instance);
            }

            static class InjectConstructor implements Component {
                private Dependency dependency;

                @Inject
                public InjectConstructor(Dependency dependency) {
                    this.dependency = dependency;
                }

                public Dependency getDependency() {
                    return dependency;
                }
            }

            @Test
            public void should_inject_dependency_via_inject_constructor() {
                InjectConstructor instance = new ConstructorInjectionProvider<>(InjectConstructor.class).get(context);

                assertNotNull(instance);
                assertSame(dependency, instance.getDependency());
            }

            @Test
            public void should_include_dependency_from_inject_constructor() {
                ConstructorInjectionProvider<InjectConstructor> provider = new ConstructorInjectionProvider<>(InjectConstructor.class);
                assertArrayEquals(new Class<?>[] {Dependency.class}, provider.getDependencies().toArray());
            }
        }

        @Nested
        class IllegalInjectConstructor {
            abstract class AbstractComponent implements Component {
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
                assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(Component.class));
            }

            static class MultiInjectConstructor implements Component {

                @Inject
                public MultiInjectConstructor(String name, Double value) {
                }

                @Inject
                public MultiInjectConstructor(Dependency dependency) {
                }

            }

            @Test
            public void should_throw_an_exception_if_multi_inject_constructor_provided() {
                assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(MultiInjectConstructor.class));
            }

            static class NoInjectConstructorNorDefaultConstructor implements Component {
                public NoInjectConstructorNorDefaultConstructor(String name) {
                }
            }

            @Test
            public void should_throw_an_exception_if_no_inject_nor_default_constructors_provided() {
                assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(NoInjectConstructorNorDefaultConstructor.class));
            }
        }
    }

    @Nested
    public class FieldInjection {
        @Nested
        class Injection {
            static class ComponentWithFieldInjection {
                @Inject
                Dependency dependency;
            }

            static class SubClassWithFieldInjection extends ComponentWithFieldInjection {

            }

            @Test
            public void should_inject_dependency_via_field() {
                ComponentWithFieldInjection component = new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class).get(context);

                assertSame(dependency, component.dependency);
            }

            @Test
            public void should_inject_dependency_via_superclass_inject_field() {
                SubClassWithFieldInjection component = new ConstructorInjectionProvider<>(SubClassWithFieldInjection.class).get(context);

                assertSame(dependency, component.dependency);
            }

            @Test
            public void should_include_field_from_field__dependency() {
                ConstructorInjectionProvider<Injection.ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(Injection.ComponentWithFieldInjection.class);
                assertArrayEquals(new Class<?>[] {Dependency.class}, provider.getDependencies().toArray());
            }
        }

        @Nested
        class IllegalInjectFields {
            //TODO throw exception if field is final
            static class FinalInjectField {
                @Inject
                final Dependency dependency = null;
            }

            @Test
            public void should_throw_exception_if_field_is_final() {
                assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(FinalInjectField.class));
            }
        }

    }

    @Nested
    public class MethodInjection {
        @Nested
        class Injection {

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
                InjectMethodWithNoDependency instance = new ConstructorInjectionProvider<>(InjectMethodWithNoDependency.class).get(context);

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
                InjectMethodWithDependency instance = new ConstructorInjectionProvider<>(InjectMethodWithDependency.class).get(context);

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
                SubClassWithInjectMethod instance = new ConstructorInjectionProvider<>(SubClassWithInjectMethod.class).get(context);

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
                SubClassOverrideSuperClassWithInject instance = new ConstructorInjectionProvider<>(SubClassOverrideSuperClassWithInject.class).get(context);
                assertEquals(1, instance.superCalled);
            }

            static class SubClassOverrideSuperClassWithNoInject extends SuperClassWithInjectMethod {

                void install() {
                    super.install();
                }
            }

            @Test
            public void should_not_call_inject_method_if_override_with_no_inject() {
                SubClassOverrideSuperClassWithNoInject instance = new ConstructorInjectionProvider<>(SubClassOverrideSuperClassWithNoInject.class).get(context);

                assertEquals(0, instance.superCalled);
            }

            //TODO  include dependencies from inject methods
            @Test
            public void should_include_dependencies_from_inject_method() {
                ConstructorInjectionProvider<InjectMethodWithDependency> provider = new ConstructorInjectionProvider<>(InjectMethodWithDependency.class);
                assertArrayEquals(new Class<?>[] {Dependency.class}, provider.getDependencies().toArray());
            }
        }

        @Nested
        class IllegalInjectMethod {
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
}
