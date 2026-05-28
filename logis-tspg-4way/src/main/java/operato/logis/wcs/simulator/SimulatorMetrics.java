package operato.logis.wcs.simulator;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

/**
 * 시뮬레이터 메트릭 — 카운터 + 이벤트 로그 + 분 단위 시계열.
 */
@Component
public class SimulatorMetrics {

    private static final int MAX_EVENTS = 100;
    private static final int MAX_TIMESERIES_MINUTES = 30;

    // HOST 카운터
    private final AtomicLong hostSubmitted = new AtomicLong();
    private final AtomicLong hostFailed    = new AtomicLong();
    private final ConcurrentHashMap<String, AtomicLong> hostByType = new ConcurrentHashMap<>();

    // PLC 카운터
    private final AtomicLong plcStarted   = new AtomicLong();
    private final AtomicLong plcCompleted = new AtomicLong();
    private final AtomicLong plcFailed    = new AtomicLong();

    // 이벤트 로그 (최신 100건)
    private final Deque<Map<String, Object>> recentEvents = new ConcurrentLinkedDeque<>();

    // 분 단위 시계열 (key = epochMillis / 60000)
    private final ConcurrentSkipListMap<Long, MinuteBucket> timeSeries = new ConcurrentSkipListMap<>();

    /**
     * HOST 주문 발행 기록.
     */
    public void recordHostSubmit(String type, String key) {
        hostSubmitted.incrementAndGet();
        hostByType.computeIfAbsent(type, k -> new AtomicLong()).incrementAndGet();
        addEvent("HOST_SUBMIT", Map.of("type", type, "key", key));
        bucketNow().submitted.incrementAndGet();
    }

    /**
     * HOST 주문 발행 실패 기록.
     */
    public void recordHostFail(String type, String key, String code, String desc) {
        hostFailed.incrementAndGet();
        addEvent("HOST_FAIL", Map.of(
                "type", String.valueOf(type),
                "key",  String.valueOf(key),
                "code", String.valueOf(code),
                "desc", String.valueOf(desc)));
        bucketNow().failed.incrementAndGet();
    }

    /**
     * PLC 콜백 시퀀스 시작 기록.
     */
    public void recordPlcStart(String key, String type) {
        plcStarted.incrementAndGet();
        addEvent("PLC_START", Map.of("key", key, "type", type));
    }

    /**
     * PLC 콜백 시퀀스 완료 기록.
     */
    public void recordPlcComplete(String key, String type) {
        plcCompleted.incrementAndGet();
        addEvent("PLC_COMPLETE", Map.of("key", key, "type", type));
        bucketNow().completed.incrementAndGet();
    }

    /**
     * PLC 콜백 시퀀스 실패 기록.
     */
    public void recordPlcFail(String key, String type, String reason) {
        plcFailed.incrementAndGet();
        addEvent("PLC_FAIL", Map.of(
                "key",    String.valueOf(key),
                "type",   String.valueOf(type),
                "reason", String.valueOf(reason)));
    }

    /**
     * 이벤트 큐에 추가 + 최대 100건 유지.
     */
    private void addEvent(String type, Map<String, Object> payload) {
        Map<String, Object> evt = new LinkedHashMap<>();
        evt.put("ts", System.currentTimeMillis());
        evt.put("type", type);
        evt.putAll(payload);
        recentEvents.addFirst(evt);
        while (recentEvents.size() > MAX_EVENTS) recentEvents.pollLast();
    }

    /**
     * 현재 분 버킷 — 30분 초과 분은 head 부터 제거.
     */
    private MinuteBucket bucketNow() {
        long minute = System.currentTimeMillis() / 60_000L;
        MinuteBucket b = timeSeries.computeIfAbsent(minute, m -> new MinuteBucket());
        long cutoff = minute - MAX_TIMESERIES_MINUTES;
        timeSeries.headMap(cutoff, true).clear();
        return b;
    }

    /**
     * 카운터 + 이벤트 통합 스냅샷.
     */
    public Map<String, Object> snapshot() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("hostSubmitted", hostSubmitted.get());
        m.put("hostFailed",    hostFailed.get());
        Map<String, Long> byType = new LinkedHashMap<>();
        hostByType.forEach((k, v) -> byType.put(k, v.get()));
        m.put("hostByType", byType);
        m.put("plcStarted",   plcStarted.get());
        m.put("plcCompleted", plcCompleted.get());
        m.put("plcFailed",    plcFailed.get());
        m.put("recentEvents", new ArrayList<>(recentEvents));
        return m;
    }

    /**
     * 분 단위 시계열 (오래된 것부터).
     */
    public List<Map<String, Object>> timeseries() {
        List<Map<String, Object>> out = new ArrayList<>();
        timeSeries.forEach((minute, b) -> {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("ts",        minute * 60_000L);
            p.put("submitted", b.submitted.get());
            p.put("completed", b.completed.get());
            p.put("failed",    b.failed.get());
            out.add(p);
        });
        return out;
    }

    /**
     * 분 단위 카운터 버킷.
     */
    private static final class MinuteBucket {
        final AtomicLong submitted = new AtomicLong();
        final AtomicLong completed = new AtomicLong();
        final AtomicLong failed    = new AtomicLong();
    }
}
