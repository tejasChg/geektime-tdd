package geektime.tdd.di;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

public class ContainerTest {

    ContextConfig config;

    @BeforeEach
    void setUp() {
        config = new ContextConfig();
    }

    @Nested
    public class DependenciesSelection {
        @Nested
        public class ProviderType {
            //Context


            //InjectionProvider



        }

        @Nested
        public class Qualifier{

        }
    }

    @Nested
    public class LifecycleManagement {

    }
}

interface TestComponent {
    default Dependency dependency() {
        throw new UnsupportedOperationException("Not implemented");
    }
}

interface Dependency {

}

interface AnotherDependency {

}