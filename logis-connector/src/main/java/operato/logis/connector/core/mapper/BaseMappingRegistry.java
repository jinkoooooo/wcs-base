package operato.logis.connector.core.mapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import operato.logis.connector.core.loader.MappingLoader;

public abstract class BaseMappingRegistry<T> implements IMappingRegistry<T> {
    private final MappingLoader<T> loader;
    private Map<String, T> registry = new HashMap<>();
    private final Function<T, String> keyExtractor;

    public BaseMappingRegistry(MappingLoader<T> loader, Function<T, String> keyExtractor) {
        this.loader = loader;
        this.keyExtractor = keyExtractor;
        this.reload();
    }

    @Override
    public void reload() {
        this.registry = loader.loadAll(keyExtractor);
    }

    @Override
    public Optional<T> getByKey(String key) {
        return Optional.ofNullable(registry.get(key));
    }
}