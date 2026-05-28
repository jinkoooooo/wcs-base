package operato.logis.connector.core.mapper;

import java.util.Optional;

public interface IMappingRegistry<T> {
    void reload();
    Optional<T> getByKey(String key);
}