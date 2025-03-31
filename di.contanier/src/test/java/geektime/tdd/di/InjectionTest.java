package geektime.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.ParameterizedType;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InjectionTest {

    private Dependency dependency = mock(Dependency.class);

    private Provider<Dependency> dependencyProvider = mock(Provider.class);
    private ParameterizedType dependencyProviderType;

    private Context context = mock(Context.class);

    @BeforeEach
    void setUp() throws NoSuchFieldException {
        dependencyProviderType = (ParameterizedType) InjectionTest.class.getDeclaredField("dependencyProvider").getGenericType();
        when(context.get(eq(ComponentRef.of(Dependency.class)))).thenReturn(Optional.of(dependency));
        when(context.get(eq(ComponentRef.of(dependencyProviderType)))).thenReturn(Optional.of(dependencyProvider));
    }

    @Nested
    public class ConstructorInjection {

        @Nested
        class Injection {
            static class DefaultConstructor implements TestComponent {
                public DefaultConstructor() {
                }
            }

            @Test
            public void should_call_default_constructor_if_no_inject_constructor() {
                DefaultConstructor instance = new InjectionProvider<>(DefaultConstructor.class).get(context);

                assertNotNull(instance);
            }

            static class InjectConstructor implements TestComponent {
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
                InjectConstructor instance = new InjectionProvider<>(InjectConstructor.class).get(context);

                assertNotNull(instance);
                assertSame(dependency, instance.getDependency());
            }

            @Test
            public void should_include_dependency_from_inject_constructor() {
                InjectionProvider<InjectConstructor> provider = new InjectionProvider<>(InjectConstructor.class);
                assertArrayEquals(new ComponentRef[] {ComponentRef.of(Dependency.class)}, provider.getDependencies().toArray());
            }

            @Test
            public void should_include_provider_type_from_inject_constructor() {
                InjectionProvider<ProviderInjectConstructor> provider = new InjectionProvider<>(ProviderInjectConstructor.class);
                assertArrayEquals(new ComponentRef[] {ComponentRef.of(dependencyProviderType)}, provider.getDependencies().toArray());
            }

            static class ProviderInjectConstructor {
                private Provider<Dependency> dependency;

                @Inject
                public ProviderInjectConstructor(Provider<Dependency> dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            public void should_inject_provider_via_inject_constructor() {
                ProviderInjectConstructor instance = new InjectionProvider<>(ProviderInjectConstructor.class).get(context);

                assertSame(dependencyProvider, instance.dependency);
            }
        }

        @Nested
        class IllegalInjectConstructor {
            abstract class AbstractTestComponent implements TestComponent {
                @Inject
                public AbstractTestComponent() {
                }
            }

            @Test
            public void should_throw_an_exception_if_component_is_abstract() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(AbstractTestComponent.class));
            }

            @Test
            public void should_throw_an_exception_if_component_is_interface() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(TestComponent.class));
            }

            static class MultiInjectConstructor implements TestComponent {

                @Inject
                public MultiInjectConstructor(String name, Double value) {
                }

                @Inject
                public MultiInjectConstructor(Dependency dependency) {
                }

            }

            @Test
            public void should_throw_an_exception_if_multi_inject_constructor_provided() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(MultiInjectConstructor.class));
            }

            static class NoInjectConstructorNorDefaultConstructor implements TestComponent {
                public NoInjectConstructorNorDefaultConstructor(String name) {
                }
            }

            @Test
            public void should_throw_an_exception_if_no_inject_nor_default_constructors_provided() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(NoInjectConstructorNorDefaultConstructor.class));
            }
        }

        @Nested
        class WithQualifier {

            @BeforeEach
            void before() {
                Mockito.reset(context);
                when(context.get(eq(ComponentRef.of(Dependency.class, new NameLiteral("ChosenOne"))))).thenReturn(Optional.of(dependency));
            }

            static class InjectConstructor {
                Dependency dependency;

                @Inject
                public InjectConstructor(@Named("ChosenOne") Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            public void should_inject_dependency_with_qualifier_via_constructor() {
                InjectionProvider<InjectConstructor> provider = new InjectionProvider<>(InjectConstructor.class);
                InjectConstructor component = provider.get(context);

                assertSame(dependency, component.dependency);
            }

            @Test
            public void should_include_dependency_with_qualifier() {
                InjectionProvider<InjectConstructor> provider = new InjectionProvider<>(InjectConstructor.class);

                assertArrayEquals(new ComponentRef<?>[] {ComponentRef.of(Dependency.class, new NameLiteral("ChosenOne"))},
                    provider.getDependencies().toArray());
            }

            static class MultiQualifierInjectConstructor {
                @Inject
                public MultiQualifierInjectConstructor(@Named("ChosenOne") @Skywalker Dependency dependency) {
                }
            }

            @Test
            public void should_throw_exception_if_multi_qualifier_given() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(MultiQualifierInjectConstructor.class));
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

            @Test
            public void should_call_inject_method_even_if_no_dependency_declared() {
                InjectMethodWithNoDependency instance = new InjectionProvider<>(InjectMethodWithNoDependency.class).get(context);

                assertTrue(instance.called);
            }

            static class InjectMethodWithDependency {
                Dependency dependency;

                @Inject
                void install(Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            public void should_inject_dependency_via_inject_method() {
                InjectMethodWithDependency instance = new InjectionProvider<>(InjectMethodWithDependency.class).get(context);

                assertSame(dependency, instance.dependency);
            }

            @Test
            public void should_include_provider_type_from_inject_method() {
                InjectionProvider<ProviderInjectMethod> provider = new InjectionProvider<>(ProviderInjectMethod.class);
                assertArrayEquals(new ComponentRef[] {ComponentRef.of(dependencyProviderType)}, provider.getDependencies().toArray());
            }


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
                SubClassWithInjectMethod instance = new InjectionProvider<>(SubClassWithInjectMethod.class).get(context);

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
                SubClassOverrideSuperClassWithInject instance = new InjectionProvider<>(SubClassOverrideSuperClassWithInject.class).get(context);
                assertEquals(1, instance.superCalled);
            }

            static class SubClassOverrideSuperClassWithNoInject extends SuperClassWithInjectMethod {

                void install() {
                    super.install();
                }
            }

            @Test
            public void should_not_call_inject_method_if_override_with_no_inject() {
                SubClassOverrideSuperClassWithNoInject instance = new InjectionProvider<>(SubClassOverrideSuperClassWithNoInject.class).get(context);

                assertEquals(0, instance.superCalled);
            }

            @Test
            public void should_include_dependencies_from_inject_method() {
                InjectionProvider<InjectMethodWithDependency> provider = new InjectionProvider<>(InjectMethodWithDependency.class);
                assertArrayEquals(new ComponentRef[] {ComponentRef.of(Dependency.class)}, provider.getDependencies().toArray());
            }

            static class ProviderInjectMethod {
                private Provider<Dependency> dependency;

                @Inject
                public void install(Provider<Dependency> dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            public void should_inject_provider_via_inject_method() {
                ProviderInjectMethod instance = new InjectionProvider<>(ProviderInjectMethod.class).get(context);

                assertSame(dependencyProvider, instance.dependency);
            }
        }

        @Nested
        class IllegalInjectMethod {
            static class InjectMethodWithTypeParameter {
                @Inject
                <T> void install(T t) {
                }
            }

            @Test
            public void should_throw_exception_if_inject_method_has_type_parameter() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(InjectMethodWithTypeParameter.class));
            }
        }

        @Nested
        class WithQualifier {
            @BeforeEach
            void before() {
                Mockito.reset(context);
                when(context.get(eq(ComponentRef.of(Dependency.class, new NameLiteral("ChosenOne"))))).thenReturn(Optional.of(dependency));
            }

            static class InjectMethod {
                Dependency dependency;

                @Inject
                void install(@Named("ChosenOne") Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            public void should_inject_dependency_with_qualifier_via_Method() {
                InjectionProvider<InjectMethod> provider = new InjectionProvider<>(InjectMethod.class);
                InjectMethod component = provider.get(context);

                assertSame(dependency, component.dependency);
            }


            @Test
            public void should_include_dependency_with_qualifier() {
                InjectionProvider<InjectMethod> provider = new InjectionProvider<>(InjectMethod.class);

                assertArrayEquals(new ComponentRef<?>[] {ComponentRef.of(Dependency.class, new NameLiteral("ChosenOne"))},
                    provider.getDependencies().toArray());
            }

            static class MultiQualifierInjectMethod {
                @Inject
                void MultiQualifierInjectConstructor(@Named("ChosenOne") @Skywalker Dependency dependency) {
                }
            }

            @Test
            public void should_throw_exception_if_multi_qualifier_given() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(MultiQualifierInjectMethod.class));
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
                ComponentWithFieldInjection component = new InjectionProvider<>(ComponentWithFieldInjection.class).get(context);

                assertSame(dependency, component.dependency);
            }

            @Test
            public void should_inject_dependency_via_superclass_inject_field() {
                SubClassWithFieldInjection component = new InjectionProvider<>(SubClassWithFieldInjection.class).get(context);

                assertSame(dependency, component.dependency);
            }

            @Test
            public void should_include_provider_type_from_inject_field() {
                InjectionProvider<ProviderInjectField> provider = new InjectionProvider<>(ProviderInjectField.class);
                assertArrayEquals(new ComponentRef[] {ComponentRef.of(dependencyProviderType)}, provider.getDependencies().toArray());
            }

            @Test
            public void should_include_field_from_field__dependency() {
                InjectionProvider<ComponentWithFieldInjection> provider = new InjectionProvider<>(Injection.ComponentWithFieldInjection.class);
                assertArrayEquals(new ComponentRef[] {ComponentRef.of(Dependency.class)}, provider.getDependencies().toArray());
            }


            static class ProviderInjectField {
                @Inject
                private Provider<Dependency> dependency;
            }

            @Test
            public void should_inject_provider_via_inject_field() {
                ProviderInjectField instance = new InjectionProvider<>(ProviderInjectField.class).get(context);

                assertSame(dependencyProvider, instance.dependency);
            }
        }

        @Nested
        class IllegalInjectFields {
            static class FinalInjectField {
                @Inject
                final Dependency dependency = null;
            }

            @Test
            public void should_throw_exception_if_field_is_final() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(FinalInjectField.class));
            }
        }

        @Nested
        class WithQualifier {
            @BeforeEach
            void before() {
                Mockito.reset(context);
                when(context.get(eq(ComponentRef.of(Dependency.class, new NameLiteral("ChosenOne"))))).thenReturn(Optional.of(dependency));
            }

            static class InjectField {
                @Inject
                @Named("ChosenOne")
                Dependency dependency;
            }

            @Test
            public void should_inject_dependency_with_qualifier_via_field() {
                InjectionProvider<InjectField> provider = new InjectionProvider<>(InjectField.class);
                InjectField component = provider.get(context);

                assertSame(dependency, component.dependency);
            }

            @Test
            public void should_include_dependency_with_qualifier() {
                InjectionProvider<InjectField> provider = new InjectionProvider<>(InjectField.class);

                assertArrayEquals(new ComponentRef<?>[] {ComponentRef.of(Dependency.class, new NameLiteral("ChosenOne"))},
                    provider.getDependencies().toArray());
            }

            static class MultiQualifierInjectField {
                @Inject
                @Skywalker
                @Named("ChosenOne")
                Dependency dependency;
            }

            @Test
            public void should_throw_exception_if_multi_qualifier_given() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(MultiQualifierInjectField.class));
            }
        }
    }

}
