package operato.logis.wcs.service.impl.scheduling;

import org.springframework.stereotype.Component;
import xyz.elidom.util.ValueUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GA 결과 캐시 — Planning 잡(30s) 갱신, Release 잡(5s) 조회.
 * TTL 만료 시 lazy invalidation, ConcurrentHashMap 기반 thread-safe.
 */
@Component
public class GaPlanCache {

    private static final long DEFAULT_TTL_MS = 60_000L;

    private final Map<String, ScoreEntry> cache = new ConcurrentHashMap<>();

    /**
     * 기본 TTL 로 저장.
     */
    public void put(String eqGroupId,
                    Map<String, Double> scoreByOrderKey,
                    Map<String, ScoreBreakdown> breakdownByOrderKey) {
        put(eqGroupId, scoreByOrderKey, breakdownByOrderKey, DEFAULT_TTL_MS);
    }

    /**
     * TTL 명시 저장. scores 가 비면 해당 eqGroupId 캐시를 삭제한다.
     */
    public void put(String eqGroupId,
                    Map<String, Double> scoreByOrderKey,
                    Map<String, ScoreBreakdown> breakdownByOrderKey,
                    long ttlMs) {
        if (ValueUtil.isEmpty(eqGroupId)) return;
        if (ValueUtil.isEmpty(scoreByOrderKey)) {
            cache.remove(eqGroupId);
            return;
        }
        cache.put(eqGroupId, new ScoreEntry(
                new HashMap<>(scoreByOrderKey),
                ValueUtil.isEmpty(breakdownByOrderKey) ? Collections.emptyMap()
                        : new HashMap<>(breakdownByOrderKey),
                System.currentTimeMillis() + ttlMs,
                System.currentTimeMillis()));
    }

    /**
     * 단건 score 조회. 만료 시 캐시에서 제거하고 빈 값 반환.
     */
    public OptionalDouble getScore(String eqGroupId, String orderKey) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(orderKey)) return OptionalDouble.empty();
        ScoreEntry entry = cache.get(eqGroupId);
        if (ValueUtil.isEmpty(entry) || entry.isExpired()) {
            if (ValueUtil.isNotEmpty(entry)) cache.remove(eqGroupId);
            return OptionalDouble.empty();
        }
        Double v = entry.scores().get(orderKey);
        return ValueUtil.isEmpty(v) ? OptionalDouble.empty() : OptionalDouble.of(v);
    }

    /**
     * 단건 breakdown 조회.
     */
    public Optional<ScoreBreakdown> getBreakdown(String eqGroupId, String orderKey) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(orderKey)) return Optional.empty();
        ScoreEntry entry = cache.get(eqGroupId);
        if (ValueUtil.isEmpty(entry) || entry.isExpired()) return Optional.empty();
        return Optional.ofNullable(entry.breakdowns().get(orderKey));
    }

    /**
     * 유효 캐시 존재 여부.
     */
    public boolean hasPlan(String eqGroupId) {
        if (ValueUtil.isEmpty(eqGroupId)) return false;
        ScoreEntry entry = cache.get(eqGroupId);
        return ValueUtil.isNotEmpty(entry) && !entry.isExpired();
    }

    /**
     * 강제 무효화.
     */
    public void invalidate(String eqGroupId) {
        if (ValueUtil.isNotEmpty(eqGroupId)) cache.remove(eqGroupId);
    }

    /**
     * 점수 전체 스냅샷 (read-only).
     */
    public Map<String, Double> snapshotScores(String eqGroupId) {
        ScoreEntry entry = cache.get(eqGroupId);
        if (ValueUtil.isEmpty(entry) || entry.isExpired()) return Collections.emptyMap();
        return Collections.unmodifiableMap(entry.scores());
    }

    /**
     * breakdown 전체 스냅샷 (read-only).
     */
    public Map<String, ScoreBreakdown> snapshotBreakdowns(String eqGroupId) {
        ScoreEntry entry = cache.get(eqGroupId);
        if (ValueUtil.isEmpty(entry) || entry.isExpired()) return Collections.emptyMap();
        return Collections.unmodifiableMap(entry.breakdowns());
    }

    /**
     * 캐시 메타 (REST 진단용) — present, expired, ageMs, ttlRemainingMs, entryCount.
     */
    public Map<String, Object> snapshotMeta(String eqGroupId) {
        ScoreEntry entry = cache.get(eqGroupId);
        Map<String, Object> meta = new HashMap<>();
        if (ValueUtil.isEmpty(entry)) {
            meta.put("present", false);
            return meta;
        }
        long now = System.currentTimeMillis();
        meta.put("present", true);
        meta.put("expired", entry.isExpired());
        meta.put("ageMs", now - entry.createdAtMs());
        meta.put("ttlRemainingMs", entry.expiresAtMs() - now);
        meta.put("entryCount", entry.scores().size());
        return meta;
    }

    // 캐시 엔트리 — 만료 시각/생성 시각 보존.
    private record ScoreEntry(Map<String, Double> scores,
                              Map<String, ScoreBreakdown> breakdowns,
                              long expiresAtMs,
                              long createdAtMs) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMs;
        }
    }
}
