package operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.realtime.provider;

import java.util.List;

/**
 * Provider Pattern (완전 교체 버전)
 * - fetchData(ctx): 반드시 ctx 기반으로만 조회
 */
public interface RealTimeDataProvider<T> {

    /** 예: shuttle, cargo, conveyor ... */
    String getProviderType();

    /**
     * ✅ topic은 충돌 방지를 위해 lcId/eqGroupId/pageId를 포함한다.
     * 예: "/topic/realtime/shuttle/{lcId}/{eqGroupId}/{pageId}"
     */
    String getTopicPattern();

    /** ✅ 컨텍스트 기반 조회 */
    List<T> fetchData(RealTimeFetchContext ctx);

    default long getIntervalMs() { return 500L; }
    default boolean isEnabled() { return true; }
    default boolean retryOnError() { return true; }
}