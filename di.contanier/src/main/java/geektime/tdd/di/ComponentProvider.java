package geektime.tdd.di;

import java.util.List;

public interface ComponentProvider<T> {
    T get(Context context);

    default void statics(Context context) {}

    default List<ComponentRef<?>> getDependencies() {
        return List.of();
    }
}
