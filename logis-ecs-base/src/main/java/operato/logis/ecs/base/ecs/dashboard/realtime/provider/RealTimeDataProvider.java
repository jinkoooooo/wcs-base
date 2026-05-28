package operato.logis.ecs.base.ecs.dashboard.realtime.provider;

import java.util.List;

/**
 * Provider Pattern (완전 교체 버전)
 * - fetchData(ctx): 반드시 ctx 기반으로만 조회
 */
public interface RealTimeDataProvider<T> {

    /** 예: crane, cargo, conveyor ... */
    String getProviderType();

    /**
     * topic은 충돌 방지를 위해 lcId/eqGroupId/pageId를 포함한다.
     * 예: "/topic/realtime/crane/{lcId}/{eqGroupId}/{pageId}"
     */
    String getTopicPattern();

    /** 컨텍스트 기반 조회 */
    List<T> fetchData(RealTimeFetchContext ctx);

    default long getIntervalMs() { return 500L; }

    default boolean isEnabled() { return true; }

    default boolean retryOnError() { return true; }
}