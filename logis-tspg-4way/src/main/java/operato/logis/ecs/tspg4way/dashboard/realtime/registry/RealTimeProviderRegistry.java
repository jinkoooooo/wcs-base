package operato.logis.ecs.tspg4way.dashboard.realtime.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import operato.logis.ecs.tspg4way.dashboard.realtime.provider.RealTimeDataProvider;

import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 실시간 데이터 Provider 레지스트리. 모든 RealTimeDataProvider 빈을 @PostConstruct 에서 타입별로 등록하고 조회 제공.
 */
@Component
public class RealTimeProviderRegistry {

    private static final Logger logger = LoggerFactory.getLogger(RealTimeProviderRegistry.class);

    /**
     * Provider 타입 → Provider 인스턴스 매핑
     */
    private final Map<String, RealTimeDataProvider<?>> providers = new ConcurrentHashMap<>();

    /**
     * Spring이 모든 RealTimeDataProvider 구현체를 자동 주입
     */
    private final List<RealTimeDataProvider<?>> providerList;

    @Autowired
    public RealTimeProviderRegistry(List<RealTimeDataProvider<?>> providerList) {
        this.providerList = providerList;
    }

    /**
     * 빈 초기화 후 모든 Provider 등록
     */
    @PostConstruct
    public void init() {
        for (RealTimeDataProvider<?> provider : providerList) {
            String type = provider.getProviderType();
            providers.put(type, provider);
            logger.debug("[ Realtime ][ Registry ] provider registered: type={}, class={}, interval={}ms, enabled={}",
                    type,
                    provider.getClass().getSimpleName(),
                    provider.getIntervalMs(),
                    provider.isEnabled());
        }
        logger.info("[ Realtime ][ Registry ] providers registered: count={}", providers.size());
    }

    /**
     * 모든 Provider 반환
     */
    public Collection<RealTimeDataProvider<?>> getAllProviders() {
        return providers.values();
    }

    /**
     * 타입으로 Provider 조회
     *
     * @param type Provider 타입 (예: "shuttle", "conveyor")
     * @return Provider 인스턴스 (없으면 null)
     */
    public RealTimeDataProvider<?> getProvider(String type) {
        return providers.get(type);
    }

    /**
     * 활성화된 Provider만 반환
     */
    public List<RealTimeDataProvider<?>> getEnabledProviders() {
        return providers.values().stream()
                .filter(RealTimeDataProvider::isEnabled)
                .collect(Collectors.toList());
    }

    /**
     * 특정 주기의 Provider만 반환
     *
     * @param intervalMs 브로드캐스트 주기 (ms)
     * @return 해당 주기의 Provider 목록
     */
    public List<RealTimeDataProvider<?>> getProvidersByInterval(long intervalMs) {
        return providers.values().stream()
                .filter(p -> p.isEnabled() && p.getIntervalMs() == intervalMs)
                .collect(Collectors.toList());
    }

    /**
     * 등록된 Provider 수
     */
    public int getProviderCount() {
        return providers.size();
    }

    /**
     * Provider 존재 여부 확인
     */
    public boolean hasProvider(String type) {
        return providers.containsKey(type);
    }
}
