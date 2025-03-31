package geektime.tdd.di;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import geektime.tdd.di.InjectionTest.ConstructorInjection.Injection.InjectConstructor;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Nested
public class ContextTest {

    ContextConfig config;

    @BeforeEach
    void setUp() {
        config = new ContextConfig();
    }

    @Nested
    class TypeBinding {
        @Test
        public void should_bind_type_to_a_specific_instance() {
            TestComponent instance = new TestComponent() {};

            config.bind(TestComponent.class, instance);

            Context context = config.getContext();
            assertSame(
                    instance, context.get(ComponentRef.of(TestComponent.class)).get());
        }

        @ParameterizedTest(name = "supporting {0}")
        @MethodSource
        public void should_bing_type_to_an_injectable_component(Class<? extends TestComponent> componentType) {
            Dependency dependency = new Dependency() {};
            config.bind(Dependency.class, dependency);
            config.bind(TestComponent.class, componentType);

            Context context = config.getContext();
            Optional<TestComponent> component = context.get(ComponentRef.of(TestComponent.class));

            assertTrue(component.isPresent());
            assertSame(dependency, component.get().dependency());
        }

        public static Stream<Arguments> should_bing_type_to_an_injectable_component() {
            return Stream.of(
                    Arguments.of(Named.of("Constructor Injection", ConstructorInjection.class)),
                    Arguments.of(Named.of("Field Injection", FieldInjection.class)),
                    Arguments.of(Named.of("Method Injection", MethodInjection.class)));
        }

        static class ConstructorInjection implements TestComponent {
            private Dependency dependency;

            @Inject
            public ConstructorInjection(Dependency dependency) {
                this.dependency = dependency;
            }

            public Dependency getDependency() {
                return dependency;
            }

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class FieldInjection implements TestComponent {
            @Inject
            Dependency dependency;

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class MethodInjection implements TestComponent {
            private Dependency dependency;

            @Inject
            void install(Dependency dependency) {
                this.dependency = dependency;
            }

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        @Test
        public void should_retrieve_empty_for_unbind_type() {
            Context context = config.getContext();
            Optional<TestComponent> component = context.get(ComponentRef.of(TestComponent.class));
            assertTrue(component.isEmpty());
        }

        @Test
        public void should_retrieve_bind_type_as_provider() {
            TestComponent instance = new TestComponent() {};
            config.bind(TestComponent.class, instance);

            Context context = config.getContext();

            ComponentRef<Provider<TestComponent>> componentRef = new ComponentRef<>() {};

            Provider<TestComponent> provider = context.get(componentRef).get();
            assertSame(instance, provider.get());
        }

        @Test
        public void should_not_retrieve_bind_type_as_unsupported_container() {
            TestComponent instance = new TestComponent() {};
            config.bind(TestComponent.class, instance);

            Context context = config.getContext();

            assertFalse(context.get(new ComponentRef<List<TestComponent>>() {}).isPresent());
        }

        @Nested
        public class WithQualifier {
            @Test
            public void should_bind_instance_with_multi_qualifier() {
                TestComponent instance = new TestComponent() {};
                config.bind(TestComponent.class, instance, new NameLiteral("chosenOne"), new SkywalkerLiteral());

                Context context = config.getContext();
                TestComponent chosenOne = context.get(
                                ComponentRef.of(TestComponent.class, new NameLiteral("chosenOne")))
                        .get();
                TestComponent skywalker = context.get(ComponentRef.of(TestComponent.class, new SkywalkerLiteral()))
                        .get();

                assertSame(instance, chosenOne);
                assertSame(instance, skywalker);
            }

            @Test
            public void should_bind_component_with_multi_qualifier() {
                Dependency dependency = new Dependency() {};
                config.bind(Dependency.class, dependency);
                config.bind(
                        InjectConstructor.class,
                        InjectConstructor.class,
                        new NameLiteral("chosenOne"),
                        new SkywalkerLiteral());

                Context context = config.getContext();
                InjectConstructor chosenOne = context.get(
                                ComponentRef.of(InjectConstructor.class, new NameLiteral("chosenOne")))
                        .get();
                InjectConstructor skywalker = context.get(
                                ComponentRef.of(InjectConstructor.class, new SkywalkerLiteral()))
                        .get();

                assertSame(dependency, chosenOne.getDependency());
                assertSame(dependency, skywalker.getDependency());
            }

            @Test
            public void should_throw_exception_if_illegal_qualifier_given_to_instance() {
                TestComponent instance = new TestComponent() {};
                assertThrows(
                        IllegalComponentException.class,
                        () -> config.bind(TestComponent.class, instance, new TestLiteral()));
            }

            @Test
            public void should_throw_exception_if_illegal_qualifier_given_to_component() {
                assertThrows(
                        IllegalComponentException.class,
                        () -> config.bind(InjectConstructor.class, InjectConstructor.class, new TestLiteral()));
            }

            @Test
            public void should_retrieve_bind_type_as_provider() {
                TestComponent instance = new TestComponent() {};
                config.bind(TestComponent.class, instance, new NameLiteral("chosenOne"), new SkywalkerLiteral());

                Optional<Provider<TestComponent>> provider =
                        config.getContext().get(new ComponentRef<>(new SkywalkerLiteral()) {});
                assertTrue(provider.isPresent());
            }

            @Test
            public void should_retrieve_empty_if_no_matched_qualifiers() {
                TestComponent instance = new TestComponent() {};
                config.bind(TestComponent.class, instance);

                Optional<TestComponent> component =
                        config.getContext().get(ComponentRef.of(TestComponent.class, new SkywalkerLiteral()));
                assertTrue(component.isEmpty());
            }
        }

        @Nested
        public class WithScope {
            static class NotSingleton {}

            @Test
            public void should_not_be_singleton_scope_by_default() {
                config.bind(NotSingleton.class, NotSingleton.class);

                Context context = config.getContext();

                assertNotSame(
                        context.get(ComponentRef.of(NotSingleton.class)).get(),
                        context.get(ComponentRef.of(NotSingleton.class)).get());
            }

            @Test
            public void should_bind_component_as_singleton_scoped() {
                config.bind(NotSingleton.class, NotSingleton.class, new SingletonLiteral());

                Context context = config.getContext();

                assertSame(
                        context.get(ComponentRef.of(NotSingleton.class)).get(),
                        context.get(ComponentRef.of(NotSingleton.class)).get());
            }

            @Singleton
            static class SingletonAnnotated implements Dependency {}

            @Test
            public void should_retrieve_scope_annotation_from_component() {
                config.bind(Dependency.class, SingletonAnnotated.class);

                Context context = config.getContext();

                assertSame(
                        context.get(ComponentRef.of(Dependency.class)).get(),
                        context.get(ComponentRef.of(Dependency.class)).get());
            }

            @Test
            public void should_bind_component_as_customized_scope() {
                config.scope(Pooled.class, PooledProvider::new);
                config.bind(NotSingleton.class, NotSingleton.class, new PooledLiteral());

                Context context = config.getContext();

                List<NotSingleton> instances = IntStream.range(0, 5)
                        .mapToObj(i ->
                                context.get(ComponentRef.of(NotSingleton.class)).get())
                        .toList();

                assertEquals(PooledProvider.MAX, new HashSet<>(instances).size());
            }

            @Test
            public void should_throw_exception_if_multi_scope_provided() {
                assertThrows(
                        IllegalComponentException.class,
                        () -> config.bind(
                                NotSingleton.class, NotSingleton.class, new SingletonLiteral(), new PooledLiteral()));
            }

            @Singleton
            @Pooled
            static class MultiScopeAnnotated {}

            @Test
            public void should_throw_exception_if_multi_scope_annotated() {
                assertThrows(
                        IllegalComponentException.class,
                        () -> config.bind(MultiScopeAnnotated.class, MultiScopeAnnotated.class));
            }

            @Test
            public void should_throw_exception_if_scope_undefined() {
                assertThrows(
                        IllegalComponentException.class,
                        () -> config.bind(NotSingleton.class, NotSingleton.class, new PooledLiteral()));
            }

            @Nested
            public class WithQualifier {
                @Test
                public void should_not_be_singleton_scope_by_default() {
                    config.bind(NotSingleton.class, NotSingleton.class, new SkywalkerLiteral());
                    Context context = config.getContext();
                    assertNotSame(
                            context.get(ComponentRef.of(NotSingleton.class, new SkywalkerLiteral()))
                                    .get(),
                            context.get(ComponentRef.of(NotSingleton.class, new SkywalkerLiteral()))
                                    .get());
                }

                @Test
                public void should_bind_component_as_singleton_scoped() {
                    config.bind(NotSingleton.class, NotSingleton.class, new SingletonLiteral(), new SkywalkerLiteral());

                    Context context = config.getContext();

                    assertSame(
                            context.get(ComponentRef.of(NotSingleton.class, new SkywalkerLiteral()))
                                    .get(),
                            context.get(ComponentRef.of(NotSingleton.class, new SkywalkerLiteral()))
                                    .get());
                }

                @Test
                public void should_retrieve_scope_annotation_from_component() {
                    config.bind(Dependency.class, SingletonAnnotated.class, new SkywalkerLiteral());

                    Context context = config.getContext();

                    assertSame(
                            context.get(ComponentRef.of(Dependency.class, new SkywalkerLiteral()))
                                    .get(),
                            context.get(ComponentRef.of(Dependency.class, new SkywalkerLiteral()))
                                    .get());
                }
            }
        }
    }

    @Nested
    public class DependencyCheck {

        @ParameterizedTest
        @MethodSource
        public void should_throw_an_exception_if_dependency_not_found(Class<? extends TestComponent> component) {
            config.bind(TestComponent.class, component);

            DependencyNotFoundException exception =
                    assertThrows(DependencyNotFoundException.class, () -> config.getContext());

            assertEquals(Dependency.class, exception.getDependency().type());
            assertEquals(TestComponent.class, exception.getComponent().type());
        }

        public static Stream<Arguments> should_throw_an_exception_if_dependency_not_found() {
            return Stream.of(
                    Arguments.of(Named.of("Inject Constructor", MissingDependencyConstructor.class)),
                    Arguments.of(Named.of("Inject Field", MissingDependencyField.class)),
                    Arguments.of(Named.of("Inject Method", MissingDependencyMethod.class)),
                    Arguments.of(
                            Named.of("Provider in Inject Constructor", MissingDependencyProviderConstructor.class)),
                    Arguments.of(Named.of("Provider in Inject Field", MissingDependencyProviderField.class)),
                    Arguments.of(Named.of("Provider in Inject Method", MissingDependencyProviderMethod.class)),
                    Arguments.of(Named.of("Scoped", MissingDependencyScoped.class)),
                    Arguments.of(Named.of("Scoped Provider", MissingDependencyProviderScoped.class)));
        }

        static class MissingDependencyConstructor implements TestComponent {
            @Inject
            public MissingDependencyConstructor(Dependency dependency) {}
        }

        static class MissingDependencyField implements TestComponent {
            @Inject
            Dependency dependency;
        }

        static class MissingDependencyMethod implements TestComponent {
            @Inject
            void install(Dependency dependency) {}
        }

        static class MissingDependencyProviderConstructor implements TestComponent {
            @Inject
            public MissingDependencyProviderConstructor(Provider<Dependency> dependency) {}
        }

        static class MissingDependencyProviderField implements TestComponent {
            @Inject
            Provider<Dependency> dependency;
        }

        static class MissingDependencyProviderMethod implements TestComponent {
            @Inject
            void install(Provider<Dependency> dependency) {}
        }

        @Singleton
        static class MissingDependencyScoped implements TestComponent {
            @Inject
            Dependency dependency;
        }

        @Singleton
        static class MissingDependencyProviderScoped implements TestComponent {
            @Inject
            Provider<Dependency> dependency;
        }

        @ParameterizedTest(name = "cyclic dependency between {0} and {1}")
        @MethodSource
        public void should_throw_an_exception_if_cycli_dependency_found(
                Class<? extends TestComponent> component, Class<? extends Dependency> dependency) {
            config.bind(TestComponent.class, component);
            config.bind(Dependency.class, dependency);

            CycliDependencyFoundException exception =
                    assertThrows(CycliDependencyFoundException.class, () -> config.getContext());

            Set<Class> components = Arrays.stream(exception.getComponents()).collect(Collectors.toSet());
            assertEquals(2, components.size());
            assertTrue(components.contains(TestComponent.class));
            assertTrue(components.contains(Dependency.class));
        }

        public static Stream<Arguments> should_throw_an_exception_if_cycli_dependency_found() {
            List<Arguments> arguments = new ArrayList<>();
            List<Named> namedOfCyclicComponentInjects = List.of(
                    Named.of("Inject Constructor", CyclicTestComponentInjectConstructor.class),
                    Named.of("Inject Field", CyclicTestComponentInjectField.class),
                    Named.of("Inject Method", CyclicTestComponentInjectMethod.class));
            List<Named> namedOfCyclicDependencyInjects = List.of(
                    Named.of("Inject Constructor", CyclicDependencyInjectConstructor.class),
                    Named.of("Inject Field", CyclicDependencyInjectField.class),
                    Named.of("Inject Method", CyclicDependencyInjectMethod.class));
            for (Named component : namedOfCyclicComponentInjects) {
                for (Named dependency : namedOfCyclicDependencyInjects) {
                    arguments.add(Arguments.of(component, dependency));
                }
            }
            return arguments.stream();
        }

        @ParameterizedTest(name = "indirect cyclic dependency between {0}, {1} and {2}")
        @MethodSource
        public void should_throw_an_exception_if_transitive_cycli_dependencies_found(
                Class<? extends TestComponent> component,
                Class<? extends Dependency> dependency,
                Class<? extends AnotherDependency> anotherDependency) {
            config.bind(TestComponent.class, component);
            config.bind(Dependency.class, dependency);
            config.bind(AnotherDependency.class, anotherDependency);

            CycliDependencyFoundException exception =
                    assertThrows(CycliDependencyFoundException.class, () -> config.getContext());

            Set<Class> components = Arrays.stream(exception.getComponents()).collect(Collectors.toSet());
            assertEquals(3, components.size());
            assertTrue(components.contains(TestComponent.class));
            assertTrue(components.contains(Dependency.class));
            assertTrue(components.contains(AnotherDependency.class));
        }

        public static Stream<Arguments> should_throw_an_exception_if_transitive_cycli_dependencies_found() {
            List<Arguments> arguments = new ArrayList<>();
            List<Named> componentInjectsNamed = List.of(
                    Named.of("Inject Constructor", CyclicTestComponentInjectConstructor.class),
                    Named.of("Inject Field", CyclicTestComponentInjectField.class),
                    Named.of("Inject Method", CyclicTestComponentInjectMethod.class));
            List<Named> cyclicDependencyInjectsNamed = List.of(
                    Named.of("Inject Constructor", IndirectCyclicDependencyInjectConstructor.class),
                    Named.of("Inject Field", IndirectCyclicDependencyInjectField.class),
                    Named.of("Inject Method", IndirectCyclicDependencyInjectMethod.class));
            List<Named> cyclicAnotherDependencyInjectsNamed = List.of(
                    Named.of("Inject Constructor", IndirectCyclicAnotherDependencyInjectConstructor.class),
                    Named.of("Inject Field", IndirectCyclicAnotherDependencyInjectField.class),
                    Named.of("Inject Method", IndirectCyclicAnotherDependencyInjectMethod.class));

            for (Named component : componentInjectsNamed) {
                for (Named dependency : cyclicDependencyInjectsNamed) {
                    for (Named anotherDependency : cyclicAnotherDependencyInjectsNamed) {
                        arguments.add(Arguments.of(component, dependency, anotherDependency));
                    }
                }
            }
            return arguments.stream();
        }

        static class CyclicTestComponentInjectConstructor implements TestComponent {
            @Inject
            public CyclicTestComponentInjectConstructor(Dependency dependency) {}
        }

        static class CyclicTestComponentInjectField implements TestComponent {
            @Inject
            Dependency dependency;
        }

        static class CyclicTestComponentInjectMethod implements TestComponent {
            @Inject
            void install(Dependency dependency) {}
        }

        static class CyclicDependencyInjectConstructor implements Dependency {
            @Inject
            public CyclicDependencyInjectConstructor(TestComponent testComponent) {}
        }

        static class CyclicDependencyInjectField implements Dependency {
            @Inject
            TestComponent testComponent;
        }

        static class CyclicDependencyInjectMethod implements Dependency {
            @Inject
            void install(TestComponent testComponent) {}
        }

        static class IndirectCyclicDependencyInjectConstructor implements Dependency {
            @Inject
            public IndirectCyclicDependencyInjectConstructor(AnotherDependency anotherDependency) {}
        }

        static class IndirectCyclicDependencyInjectField implements Dependency {
            @Inject
            AnotherDependency anotherDependency;
        }

        static class IndirectCyclicDependencyInjectMethod implements Dependency {
            @Inject
            void install(AnotherDependency anotherDependency) {}
        }

        static class IndirectCyclicAnotherDependencyInjectConstructor implements AnotherDependency {
            @Inject
            public IndirectCyclicAnotherDependencyInjectConstructor(TestComponent testComponent) {}
        }

        static class IndirectCyclicAnotherDependencyInjectField implements AnotherDependency {
            @Inject
            TestComponent testComponent;
        }

        static class IndirectCyclicAnotherDependencyInjectMethod implements AnotherDependency {
            @Inject
            void install(TestComponent testComponent) {}
        }

        static class CyclicDependencyProviderConstructor implements Dependency {
            @Inject
            public CyclicDependencyProviderConstructor(Provider<TestComponent> component) {}
        }

        @Test
        public void should_not_throw_exception_if_cyclic_dependency_via_provider() {
            config.bind(TestComponent.class, CyclicTestComponentInjectConstructor.class);
            config.bind(Dependency.class, CyclicDependencyProviderConstructor.class);

            Context context = config.getContext();
            assertTrue(context.get(ComponentRef.of(TestComponent.class)).isPresent());
            assertTrue(context.get(ComponentRef.of(Dependency.class)).isPresent());
        }

        @Nested
        public class WithQualifier {
            @ParameterizedTest
            @MethodSource
            public void should_throw_exception_if_dependency_with_qualifier_not_found(
                    Class<? extends TestComponent> component) {
                Dependency dependency = new Dependency() {};
                config.bind(Dependency.class, dependency);
                config.bind(TestComponent.class, component, new NameLiteral("Owner"));

                DependencyNotFoundException exception =
                        assertThrows(DependencyNotFoundException.class, () -> config.getContext());

                assertEquals(new Component(TestComponent.class, new NameLiteral("Owner")), exception.getComponent());
                assertEquals(new Component(Dependency.class, new SkywalkerLiteral()), exception.getDependency());
            }

            public static Stream<Arguments> should_throw_exception_if_dependency_with_qualifier_not_found() {
                return Stream.of(
                                Named.of("Inject Constructor with Qualifier", InjectConstructor.class),
                                Named.of("Inject Field with Qualifier", InjectField.class),
                                Named.of("Inject Method with Qualifier", InjectMethod.class),
                                Named.of(
                                        "Provider in Inject Constructor with Qualifier",
                                        InjectConstructorProvider.class),
                                Named.of("Provider in Inject Field with Qualifier", InjectFieldProvider.class),
                                Named.of("Provider in Inject Method with Qualifier", InjectMethodProvider.class))
                        .map(Arguments::of);
            }

            static class InjectConstructor implements TestComponent {
                @Inject
                public InjectConstructor(@Skywalker Dependency dependency) {}
            }

            static class InjectField implements TestComponent {
                @Inject
                @Skywalker
                Dependency dependency;
            }

            static class InjectMethod implements TestComponent {
                @Inject
                void install(@Skywalker Dependency dependency) {}
            }

            static class InjectConstructorProvider implements TestComponent {
                @Inject
                public InjectConstructorProvider(@Skywalker Provider<Dependency> dependency) {}
            }

            static class InjectFieldProvider implements TestComponent {
                @Inject
                @Skywalker
                Provider<Dependency> dependency;
            }

            static class InjectMethodProvider implements TestComponent {
                @Inject
                void install(@Skywalker Provider<Dependency> dependency) {}
            }

            static class SkywalkerDependency implements Dependency {
                @Inject
                public SkywalkerDependency(@jakarta.inject.Named("ChosenOne") Dependency dependency) {}
            }

            static class NotCyclicDependency implements Dependency {
                @Inject
                public NotCyclicDependency(@Skywalker Dependency dependency) {}
            }

            @ParameterizedTest(name = "{1} -> @Skywalker({0}) -> @Named(\"ChosenOne\") not cyclic dependencies")
            @MethodSource
            public void should_not_throw_cyclic_exception_if_component_with_same_type_tagged_with_different_qualifier(
                    Class skywalker, Class notCyclic) {
                Dependency dependency = new Dependency() {};
                config.bind(Dependency.class, dependency, new NameLiteral("ChosenOne"));
                config.bind(Dependency.class, skywalker, new SkywalkerLiteral());
                config.bind(Dependency.class, notCyclic);

                assertDoesNotThrow(() -> config.getContext());
            }

            public static Stream<Arguments>
                    should_not_throw_cyclic_exception_if_component_with_same_type_tagged_with_different_qualifier() {
                List<Arguments> arguments = new ArrayList<>();
                for (Named skywalker : List.of(
                        Named.of("Inject Constructor", SkywalkerInjectConstructor.class),
                        Named.of("Inject Field", SkywalkerInjectField.class),
                        Named.of("Inject Method", SkywalkerInjectMethod.class))) {
                    for (Named notCyclic : List.of(
                            Named.of("Inject Constructor", NotCyclicInjectConstructor.class),
                            Named.of("Inject Field", NotCyclicInjectField.class),
                            Named.of("Inject Method", NotCyclicInjectMethod.class))) {
                        arguments.add(Arguments.of(skywalker, notCyclic));
                    }
                }
                return arguments.stream();
            }

            static class SkywalkerInjectConstructor implements Dependency {
                @Inject
                public SkywalkerInjectConstructor(@jakarta.inject.Named("ChosenOne") Dependency dependency) {}
            }

            static class NotCyclicInjectConstructor implements Dependency {
                @Inject
                public NotCyclicInjectConstructor(@Skywalker Dependency dependency) {}
            }

            static class SkywalkerInjectField implements Dependency {
                @Inject
                @jakarta.inject.Named("ChosenOne")
                Dependency dependency;
            }

            static class NotCyclicInjectField implements Dependency {
                @Inject
                @Skywalker
                Dependency dependency;
            }

            static class SkywalkerInjectMethod implements Dependency {
                @Inject
                void install(@jakarta.inject.Named("ChosenOne") Dependency dependency) {}
            }

            static class NotCyclicInjectMethod implements Dependency {
                @Inject
                void install(@Skywalker Dependency dependency) {}
            }
        }
    }
}
