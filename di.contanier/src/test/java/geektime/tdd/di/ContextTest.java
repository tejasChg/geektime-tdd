package geektime.tdd.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

            assertSame(instance, config.getContext().get(Component.class).get());
        }

        @ParameterizedTest(name = "supporting {0}")
        @MethodSource
        public void should_bing_type_to_an_injectable_component(Class<? extends Component> componentType) {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(Component.class, componentType);

            Optional<Component> component = config.getContext().get(Component.class);

            assertTrue(component.isPresent());
            assertSame(dependency, component.get().dependency());
        }

        public static Stream<Arguments> should_bing_type_to_an_injectable_component() {
            return Stream.of(
                Arguments.of(Named.of("Constructor Injection", ConstructorInjection.class)),
                Arguments.of(Named.of("Field Injection", FieldInjection.class)),
                Arguments.of(Named.of("Method Injection", MethodInjection.class))
            );
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
        public void should_retrieve_empty_for_unbind_type(){
            Optional<Component> component = config.getContext().get(Component.class);
            assertTrue(component.isEmpty());
        }
    }

    @Nested
    public class DependencyCheck {

        @Test
        public void should_throw_an_exception_if_dependency_not_found() {
            config.bind(Component.class, ComponentWithInjectConstructor.class);

            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext());

            assertEquals(Dependency.class, exception.getDependency());
            assertEquals(Component.class, exception.getComponent());

        }

        static class ComponentWithInjectConstructor implements Component {
            private Dependency dependency;

            @Inject
            public ComponentWithInjectConstructor(Dependency dependency) {
                this.dependency = dependency;
            }

            public Dependency getDependency() {
                return dependency;
            }
        }

        static class DependencyDependedOnComponent implements Dependency {
            private Component component;

            @Inject
            public DependencyDependedOnComponent(Component component) {
                this.component = component;
            }

            public Component getComponents() {
                return component;
            }
        }

        static class DependencyDependedOnAnotherDependency implements Dependency {
            private AnotherDependency anotherDependency;

            @Inject
            public DependencyDependedOnAnotherDependency(AnotherDependency anotherDependency) {
                this.anotherDependency = anotherDependency;
            }

        }

        static class AnotherDependencyDependedOnComponent implements AnotherDependency {
            private Component component;

            @Inject
            public AnotherDependencyDependedOnComponent(Component component) {
                this.component = component;
            }
        }

        @Test
        public void should_throw_an_exception_if_cycli_dependency_found() {
            config.bind(Component.class, ComponentWithInjectConstructor.class);
            config.bind(Dependency.class, DependencyDependedOnComponent.class);

            CycliDependencyFoundException exception = assertThrows(CycliDependencyFoundException.class, () -> config.getContext());

            Set<Class<?>> components = exception.getComponents();
            assertEquals(2, components.size());
            assertTrue(components.contains(Component.class));
            assertTrue(components.contains(Dependency.class));
        }

        @Test
        public void should_throw_an_exception_if_transitive_cycli_dependencies_found() {
            config.bind(Component.class, ComponentWithInjectConstructor.class);
            config.bind(Dependency.class, DependencyDependedOnAnotherDependency.class);
            config.bind(AnotherDependency.class, AnotherDependencyDependedOnComponent.class);

            CycliDependencyFoundException exception = assertThrows(CycliDependencyFoundException.class, () -> config.getContext());

            Set<Class<?>> components = exception.getComponents();
            assertEquals(3, components.size());
            assertTrue(components.contains(Component.class));
            assertTrue(components.contains(Dependency.class));
            assertTrue(components.contains(AnotherDependency.class));
        }
    }
}
