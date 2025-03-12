package geektime.tdd.di;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {
    interface Components {

    }

    static class ComponentWithDefaultConstructor implements Components {
        public ComponentWithDefaultConstructor() {
        }
    }

    @Nested
    public class ComponentConstruction {
        //TODO: instance
        @Test
        public void should_bind_type_to_a_specific_instance() {
            Context context = new Context();

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
                Context context = new Context();

                context.bind(Components.class, ComponentWithDefaultConstructor.class);
                Components instance = context.get(Components.class);

                assertNotNull(instance);
                assertTrue(instance instanceof ComponentWithDefaultConstructor);
            }
            //TODO: with dependencies
            //TODO: A->B-C
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
