package operato.logis.ecs.tspg4way.dashboard.realtime.provider;

import java.util.List;

/**
 * 실시간 데이터 Provider 인터페이스. fetchData(ctx) 는 반드시 ctx 기반으로만 조회.
 */
public interface RealTimeDataProvider<T> {

    /** 예: shuttle, cargo, conveyor ... */
    String getProviderType();

    /**
     * topic 은 충돌 방지를 위해 lcId/eqGroupId/pageId 를 포함한다.
     * 예: "/topic/realtime/shuttle/{lcId}/{eqGroupId}/{pageId}"
     */
    String getTopicPattern();

    /** 컨텍스트 기반 조회 */
    List<T> fetchData(RealTimeFetchContext ctx);

    default long getIntervalMs() { return 500L; }
    default boolean isEnabled() { return true; }
    default boolean retryOnError() { return true; }
}