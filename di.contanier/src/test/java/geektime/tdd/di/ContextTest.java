package geektime.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
            Component instance = new Component() {
            };

            config.bind(Component.class, instance);

            Context context = config.getContext();
            assertSame(instance, context.get(Context.Ref.of(Component.class)).get());
        }

        @ParameterizedTest(name = "supporting {0}")
        @MethodSource
        public void should_bing_type_to_an_injectable_component(Class<? extends Component> componentType) {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(Component.class, componentType);

            Context context = config.getContext();
            Optional<Component> component = context.get(Context.Ref.of(Component.class));

            assertTrue(component.isPresent());
            assertSame(dependency, component.get().dependency());
        }

        public static Stream<Arguments> should_bing_type_to_an_injectable_component() {
            return Stream.of(Arguments.of(Named.of("Constructor Injection", ConstructorInjection.class)), Arguments.of(Named.of("Field Injection", FieldInjection.class)),
                Arguments.of(Named.of("Method Injection", MethodInjection.class)));
        }

        static class ConstructorInjection implements Component {
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

        static class FieldInjection implements Component {
            @Inject
            Dependency dependency;

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class MethodInjection implements Component {
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
            Optional<Component> component = context.get(Context.Ref.of(Component.class));
            assertTrue(component.isEmpty());
        }

        @Test
        public void should_retrieve_bind_type_as_provider() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);

            Context context = config.getContext();

            Context.Ref<Provider<Component>> ref = new Context.Ref<>() {
            };

            Provider<Component> provider = context.get(ref).get();
            assertSame(instance, provider.get());
        }

        @Test
        public void should_not_retrieve_bind_type_as_unsupported_container() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);

            Context context = config.getContext();

            assertFalse(context.get(new Context.Ref<List<Component>>() {
            }).isPresent());
        }

        @Nested
        public class WithQualifier {
            //TODO binding component with qualifier
            @Test
            public void should_bind_instance_with_qualifier() {
                Component instance = new Component() {
                };
                config.bind(Component.class, instance, new NameLiteral("chosenOne"));

                Context context = config.getContext();
                Component chosenOne = context.get(Context.Ref.of(Component.class, new NameLiteral("chosenOne"))).get();

                assertSame(instance, chosenOne);
            }

            @Test
            public void should_bind_component_with_qualifier() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(InjectionTest.ConstructorInjection.Injection.InjectConstructor.class,
                    InjectionTest.ConstructorInjection.Injection.InjectConstructor.class, new NameLiteral("chosenOne"));

                Context context = config.getContext();
                InjectionTest.ConstructorInjection.Injection.InjectConstructor chosenOne =
                    context.get(Context.Ref.of(InjectionTest.ConstructorInjection.Injection.InjectConstructor.class, new NameLiteral("chosenOne"))).get();

                assertSame(dependency, chosenOne.getDependency());
            }
            //TODO binding component with multi qualifiers
            //TODO throw illegal component of illegal qualifier
        }
    }

    @Nested
    public class DependencyCheck {

        @ParameterizedTest
        @MethodSource
        public void should_throw_an_exception_if_dependency_not_found(Class<? extends Component> component) {
            config.bind(Component.class, component);

            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext());

            assertEquals(Dependency.class, exception.getDependency());
            assertEquals(Component.class, exception.getComponent());
        }

        public static Stream<Arguments> should_throw_an_exception_if_dependency_not_found() {
            return Stream.of(
                Arguments.of(Named.of("Inject Constructor", MissingDependencyConstructor.class)),
                Arguments.of(Named.of("Inject Field", MissingDependencyField.class)),
                Arguments.of(Named.of("Inject Method", MissingDependencyMethod.class)),
                Arguments.of(Named.of("Provider in Inject Constructor", MissingDependencyProviderConstructor.class)),
                Arguments.of(Named.of("Provider in Inject Field", MissingDependencyProviderField.class)),
                Arguments.of(Named.of("Provider in Inject Method", MissingDependencyProviderMethod.class))
            );
        }

        static class MissingDependencyConstructor implements Component {
            @Inject
            public MissingDependencyConstructor(Dependency dependency) {
            }
        }

        static class MissingDependencyField implements Component {
            @Inject
            Dependency dependency;
        }

        static class MissingDependencyMethod implements Component {
            @Inject
            void install(Dependency dependency) {
            }
        }

        static class MissingDependencyProviderConstructor implements Component {
            @Inject
            public MissingDependencyProviderConstructor(Provider<Dependency> dependency) {
            }
        }

        static class MissingDependencyProviderField implements Component {
            @Inject
            Provider<Dependency> dependency;
        }

        static class MissingDependencyProviderMethod implements Component {
            @Inject
            void install(Provider<Dependency> dependency) {
            }
        }

        @ParameterizedTest(name = "cyclic dependency between {0} and {1}")
        @MethodSource
        public void should_throw_an_exception_if_cycli_dependency_found(Class<? extends Component> component, Class<? extends Dependency> dependency) {
            config.bind(Component.class, component);
            config.bind(Dependency.class, dependency);

            CycliDependencyFoundException exception = assertThrows(CycliDependencyFoundException.class, () -> config.getContext());

            Set<Class<?>> components = exception.getComponents();
            assertEquals(2, components.size());
            assertTrue(components.contains(Component.class));
            assertTrue(components.contains(Dependency.class));
        }

        public static Stream<Arguments> should_throw_an_exception_if_cycli_dependency_found() {
            List<Arguments> arguments = new ArrayList<>();
            List<Named> namedOfCyclicComponentInjects =
                List.of(Named.of("Inject Constructor", CyclicComponentInjectConstructor.class), Named.of("Inject Field", CyclicComponentInjectField.class),
                    Named.of("Inject Method", CyclicComponentInjectMethod.class));
            List<Named> namedOfCyclicDependencyInjects =
                List.of(Named.of("Inject Constructor", CyclicDependencyInjectConstructor.class), Named.of("Inject Field", CyclicDependencyInjectField.class),
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
        public void should_throw_an_exception_if_transitive_cycli_dependencies_found(Class<? extends Component> component, Class<? extends Dependency> dependency,
                                                                                     Class<? extends AnotherDependency> anotherDependency) {
            config.bind(Component.class, component);
            config.bind(Dependency.class, dependency);
            config.bind(AnotherDependency.class, anotherDependency);

            CycliDependencyFoundException exception = assertThrows(CycliDependencyFoundException.class, () -> config.getContext());

            Set<Class<?>> components = exception.getComponents();
            assertEquals(3, components.size());
            assertTrue(components.contains(Component.class));
            assertTrue(components.contains(Dependency.class));
            assertTrue(components.contains(AnotherDependency.class));
        }

        public static Stream<Arguments> should_throw_an_exception_if_transitive_cycli_dependencies_found() {
            List<Arguments> arguments = new ArrayList<>();
            List<Named> componentInjectsNamed =
                List.of(Named.of("Inject Constructor", CyclicComponentInjectConstructor.class), Named.of("Inject Field", CyclicComponentInjectField.class),
                    Named.of("Inject Method", CyclicComponentInjectMethod.class));
            List<Named> cyclicDependencyInjectsNamed =
                List.of(Named.of("Inject Constructor", IndirectCyclicDependencyInjectConstructor.class), Named.of("Inject Field", IndirectCyclicDependencyInjectField.class),
                    Named.of("Inject Method", IndirectCyclicDependencyInjectMethod.class));
            List<Named> cyclicAnotherDependencyInjectsNamed = List.of(Named.of("Inject Constructor", IndirectCyclicAnotherDependencyInjectConstructor.class),
                Named.of("Inject Field", IndirectCyclicAnotherDependencyInjectField.class), Named.of("Inject Method", IndirectCyclicAnotherDependencyInjectMethod.class)

            );
            for (Named component : componentInjectsNamed) {
                for (Named dependency : cyclicDependencyInjectsNamed) {
                    for (Named anotherDependency : cyclicAnotherDependencyInjectsNamed) {
                        arguments.add(Arguments.of(component, dependency, anotherDependency));
                    }
                }
            }
            return arguments.stream();
        }

        static class CyclicComponentInjectConstructor implements Component {
            @Inject
            public CyclicComponentInjectConstructor(Dependency dependency) {
            }
        }

        static class CyclicComponentInjectField implements Component {
            @Inject
            Dependency dependency;
        }

        static class CyclicComponentInjectMethod implements Component {
            @Inject
            void install(Dependency dependency) {
            }
        }

        static class CyclicDependencyInjectConstructor implements Dependency {
            @Inject
            public CyclicDependencyInjectConstructor(Component component) {
            }
        }

        static class CyclicDependencyInjectField implements Dependency {
            @Inject
            Component component;
        }

        static class CyclicDependencyInjectMethod implements Dependency {
            @Inject
            void install(Component component) {
            }
        }

        static class IndirectCyclicDependencyInjectConstructor implements Dependency {
            @Inject
            public IndirectCyclicDependencyInjectConstructor(AnotherDependency anotherDependency) {
            }
        }

        static class IndirectCyclicDependencyInjectField implements Dependency {
            @Inject
            AnotherDependency anotherDependency;
        }

        static class IndirectCyclicDependencyInjectMethod implements Dependency {
            @Inject
            void install(AnotherDependency anotherDependency) {
            }
        }

        static class IndirectCyclicAnotherDependencyInjectConstructor implements AnotherDependency {
            @Inject
            public IndirectCyclicAnotherDependencyInjectConstructor(Component component) {
            }
        }

        static class IndirectCyclicAnotherDependencyInjectField implements AnotherDependency {
            @Inject
            Component component;
        }

        static class IndirectCyclicAnotherDependencyInjectMethod implements AnotherDependency {
            @Inject
            void install(Component component) {
            }
        }

        static class CyclicDependencyProviderConstructor implements Dependency {
            @Inject
            public CyclicDependencyProviderConstructor(Provider<Component> component) {
            }
        }

        @Test
        public void should_not_throw_exception_if_cyclic_dependency_via_provider() {
            config.bind(Component.class, CyclicComponentInjectConstructor.class);
            config.bind(Dependency.class, CyclicDependencyProviderConstructor.class);

            Context context = config.getContext();
            assertTrue(context.get(Context.Ref.of(Component.class)).isPresent());
            assertTrue(context.get(Context.Ref.of(Dependency.class)).isPresent());
        }
    }
}

record NameLiteral(String value) implements jakarta.inject.Named {


    @Override
    public Class<? extends Annotation> annotationType() {
        return null;
    }

    @Override
    public String value() {
        return value;
    }
}