package operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.realtime.model;

import java.util.Objects;

/**
 * 브로드캐스트/구독/좌표캐시를 완전히 분리하기 위한 Key
 * - eqGroupId, lcId, pageId 가 달라지면 절대 섞이면 안 된다.
 */
public final class BroadcastKey {
    private final String eqGroupId;
    private final String lcId;
    private final String pageId;

    public BroadcastKey(String eqGroupId, String lcId, String pageId) {
        this.eqGroupId = safe(eqGroupId);
        this.lcId = safe(lcId);
        this.pageId = safe(pageId);
    }

    public String getEqGroupId() { return eqGroupId; }
    public String getLcId() { return lcId; }
    public String getPageId() { return pageId; }

    public String asKeyString() {
        return eqGroupId + ":" + lcId + ":" + pageId;
    }

    private String safe(String v) { return v == null ? "" : v.trim(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BroadcastKey)) return false;
        BroadcastKey that = (BroadcastKey) o;
        return Objects.equals(eqGroupId, that.eqGroupId)
                && Objects.equals(lcId, that.lcId)
                && Objects.equals(pageId, that.pageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eqGroupId, lcId, pageId);
    }

    @Override
    public String toString() {
        return "BroadcastKey{" + asKeyString() + "}";
    }
}