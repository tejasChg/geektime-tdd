package geektime.tdd.di;

public interface ScopeProvider {
    ComponentProvider<?> create(ComponentProvider<?> provider);
}
