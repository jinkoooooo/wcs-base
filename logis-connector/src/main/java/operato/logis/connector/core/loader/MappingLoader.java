package operato.logis.connector.core.loader;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MappingLoader<T> implements IMappingLoader<T> {
    private final String externalDir; // ex) /opt/config/mappings/http
    private final String classpathDir; // ex) mappings/http
    private final Class<T> type;
    private final ObjectMapper mapper = new ObjectMapper();
    private Logger logger = LoggerFactory.getLogger(getClass());

    public MappingLoader(String externalDir, String classpathDir, Class<T> type) {
        this.externalDir = externalDir;
        this.classpathDir = classpathDir;
        this.type = type;
    }

    @Override
    public Map<String, T> loadAll(Function<T, String> keyFunc) {
        Map<String, T> result = new LinkedHashMap<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        // 1. Load from external directory first
        try {
            Resource[] ext = resolver.getResources("file:" + externalDir + "/*.json");
            for (Resource res : ext) {
                try (InputStream is = res.getInputStream()) {
                    T obj = mapper.readValue(is, type);
                    String key = keyFunc.apply(obj);
                    result.put(key, obj);
                }
            }
        } catch (Exception e) {
            logger.error("[외부 파일 로딩 실패] {}: {}", externalDir, e.toString());
        }

        // 2. Load from classpath as fallback
        try {
            Resource[] cp = resolver.getResources("classpath:" + classpathDir + "/*.json");
            for (Resource res : cp) {
                try (InputStream is = res.getInputStream()) {
                    T obj = mapper.readValue(is, type);
                    String key = keyFunc.apply(obj);
                    result.putIfAbsent(key, obj); // 외부파일에 있으면 우선
                }
            }
        } catch (Exception e) {
            logger.error("[Classpath 파일 로딩 실패] {}: {}", classpathDir, e.toString());
        }
        return result;
    }

    @Override
    public Optional<T> loadOne(String key) {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        // 1. External directory
        Resource ext = resolver.getResource("file:" + externalDir + "/" + key + ".json");
        if (ext.exists()) {
            try (InputStream is = ext.getInputStream()) {
                return Optional.of(mapper.readValue(is, type));
            } catch (Exception e) {
                logger.warn("[외부 파일 로딩 실패] {} {}: {}", key, externalDir, e.toString());
            }
        }
        // 2. Classpath
        Resource cp = resolver.getResource("classpath:" + classpathDir + "/" + key + ".json");
        if (cp.exists()) {
            try (InputStream is = cp.getInputStream()) {
                return Optional.of(mapper.readValue(is, type));
            } catch (Exception e) {
                logger.warn("[Classpath 파일 로딩 실패] {} {}: {}", key, classpathDir, e.toString());
            }
        }
        return Optional.empty();
    }
}