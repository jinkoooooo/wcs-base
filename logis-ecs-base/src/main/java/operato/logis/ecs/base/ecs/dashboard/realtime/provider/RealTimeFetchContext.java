package operato.logis.ecs.base.ecs.dashboard.realtime.provider;

import operato.logis.ecs.base.ecs.dashboard.realtime.model.BroadcastKey;

/**
 * Provider가 데이터를 조회할 때 필요한 컨텍스트
 * - eqGroupId로 DB 필터
 * - lcId/pageId로 좌표캐시/페이지 매핑 필터
 */
public final class RealTimeFetchContext {

    private final BroadcastKey key;

    public RealTimeFetchContext(BroadcastKey key) {
        this.key = key;
    }

    public BroadcastKey getKey() { return key; }

    public String getEqGroupId() { return key.getEqGroupId(); }

    public String getLcId() { return key.getLcId(); }

    public String getPageId() { return key.getPageId(); }
}