package operato.logis.connector.core.loader;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public interface IMappingLoader<T> {
    Map<String, T> loadAll(Function<T, String> keyFunc);

    Optional<T> loadOne(String key);
}