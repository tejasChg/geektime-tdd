package geektime.tdd.di;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Optional;

public interface Context {
    <Type> Optional<Type> get(Class<Type> type);

    Optional get(ParameterizedType type);
}
