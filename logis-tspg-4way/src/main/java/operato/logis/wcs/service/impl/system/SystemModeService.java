package operato.logis.wcs.service.impl.system;

import operato.logis.wcs.consts.HostOrderType;
import operato.logis.wcs.consts.LocType;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.PortMode;
import operato.logis.wcs.consts.WcsError;
import operato.logis.wcs.consts.WcsOperationMode;
import lombok.RequiredArgsConstructor;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.entity.TbWcsSystemMode;
import operato.logis.wcs.service.impl.event.RealtimeEventPublisher;
import operato.logis.wcs.service.repository.SystemModeRepository;
import operato.logis.wcs.common.util.time.LocalDateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 전역/센터별 운영 모드 + 기능 플래그 + 산출 게이팅 통합 서비스.
 *
 * 3단 폴백: eqGroupId row → GLOBAL row → 하드코딩 기본값.
 * 5초 캐시로 스케줄러/배차 루프의 DB 부하를 차단한다.
 */
@Service
@RequiredArgsConstructor
public class SystemModeService extends AbstractQueryService {

    private static final boolean DEFAULT_DISPATCH_LOCK_ENABLED  = false;
    private static final boolean DEFAULT_OPERATION_MODE_ENABLED = false;
    private static final boolean DEFAULT_INSPECTION_ENABLED     = false;

    public static final String FLAG_DISPATCH_LOCK  = "dispatchLock";
    public static final String FLAG_OPERATION_MODE = "operationMode";
    public static final String FLAG_INSPECTION     = "inspection";

    private static final long CACHE_TTL_MS = 5_000L;

    private final SystemModeRepository repo;
    /** Optional — STOMP 브로커가 없는 환경(테스트/임베디드)에서도 동작해야 하므로 setter 주입 유지. */
    @Autowired(required = false) private RealtimeEventPublisher eventPublisher;

    private final ConcurrentHashMap<String, Snapshot> cache = new ConcurrentHashMap<>();

    /**
     * 캐시 단위 스냅샷 — 3단 폴백 결과를 createdAt 시각과 함께 보존.
     */
    private record Snapshot(WcsOperationMode mode,
                            boolean operationModeEnabled,
                            boolean dispatchLockEnabled,
                            boolean inspectionEnabled,
                            long createdAt) { }

    /**
     * eqGroupId 기준 스냅샷 조회. TTL 만료 시 DB 재로드.
     */
    private Snapshot getSnapshot(String eqGroupId) {
        String key = ValueUtil.isEmpty(eqGroupId) ? "__GLOBAL__" : eqGroupId;
        Snapshot s = cache.get(key);
        if (ValueUtil.isNotEmpty(s) && System.currentTimeMillis() - s.createdAt < CACHE_TTL_MS) return s;
        Snapshot fresh = loadFromDb(eqGroupId);
        cache.put(key, fresh);
        return fresh;
    }

    /**
     * DB 에서 3단 폴백 적용한 스냅샷 빌드.
     */
    private Snapshot loadFromDb(String eqGroupId) {
        WcsOperationMode mode = computeModeFromDb(eqGroupId);
        boolean om = resolveFlag(eqGroupId, TbWcsSystemMode::getIsOperationModeEnabled, DEFAULT_OPERATION_MODE_ENABLED);
        boolean dl = resolveFlag(eqGroupId, TbWcsSystemMode::getIsDispatchLockEnabled,   DEFAULT_DISPATCH_LOCK_ENABLED);
        boolean ins= resolveFlag(eqGroupId, TbWcsSystemMode::getIsInspectionEnabled,     DEFAULT_INSPECTION_ENABLED);
        return new Snapshot(mode, om, dl, ins, System.currentTimeMillis());
    }

    /**
     * 운영 모드 3단 폴백 — eqGroupId → GLOBAL → NORMAL.
     */
    private WcsOperationMode computeModeFromDb(String eqGroupId) {
        TbWcsSystemMode row = findPreferredRow(eqGroupId);
        if (ValueUtil.isNotEmpty(row)) {
            WcsOperationMode m = WcsOperationMode.from(row.getOperationMode());
            if (ValueUtil.isNotEmpty(m)) return m;
        }
        TbWcsSystemMode global = repo.findGlobal();
        if (ValueUtil.isNotEmpty(global)) {
            WcsOperationMode m = WcsOperationMode.from(global.getOperationMode());
            if (ValueUtil.isNotEmpty(m)) return m;
        }
        return WcsOperationMode.NORMAL;
    }

    /**
     * 기능 플래그 3단 폴백 — eqGroupId → GLOBAL → defaultValue.
     */
    private boolean resolveFlag(String eqGroupId,
                                Function<TbWcsSystemMode, Boolean> getter,
                                boolean defaultValue) {
        TbWcsSystemMode row = findPreferredRow(eqGroupId);
        if (ValueUtil.isNotEmpty(row)) {
            Boolean v = getter.apply(row);
            if (ValueUtil.isNotEmpty(v)) return v;
        }
        TbWcsSystemMode global = repo.findGlobal();
        if (ValueUtil.isNotEmpty(global)) {
            Boolean v = getter.apply(global);
            if (ValueUtil.isNotEmpty(v)) return v;
        }
        return defaultValue;
    }

    /**
     * 전체 캐시 무효화. UPDATE 후 호출.
     */
    private void invalidateCache() { cache.clear(); }

    /**
     * 현재 운영 모드.
     */
    public WcsOperationMode getCurrentMode(String eqGroupId) {
        return getSnapshot(eqGroupId).mode;
    }

    /**
     * 운영 모드 기준 OrderType 진행 가능 여부.
     */
    public boolean isAllowed(String eqGroupId, OrderType type) {
        if (ValueUtil.isEmpty(type)) return true;
        return getCurrentMode(eqGroupId).allows(type);
    }

    /**
     * 운영 모드 변경 — row 없으면 INSERT, 있으면 UPDATE. 캐시 무효화 + STOMP 이벤트 발행.
     */
    @Transactional(rollbackFor = Exception.class)
    public TbWcsSystemMode changeMode(String eqGroupId, WcsOperationMode newMode, String operator, String reason) {
        if (ValueUtil.isEmpty(newMode)) {
            throw new ElidomRuntimeException("INVALID_PARAMETER", "newMode is required");
        }
        String pk = resolveRowId(eqGroupId);
        TbWcsSystemMode row = repo.findById(pk);
        boolean isNew = ValueUtil.isEmpty(row);
        if (isNew) {
            row = new TbWcsSystemMode();
            row.setId(pk);
            row.setEqGroupId(SystemModeRepository.GLOBAL_ID.equals(pk) ? null : eqGroupId);
        }
        String previousMode = row.getOperationMode();
        row.setOperationMode(newMode.code());
        row.setChangedBy(operator);
        row.setChangedAt(OffsetDateTime.now());
        row.setReason(reason);
        TbWcsSystemMode saved = isNew ? repo.insert(row) : repo.update(row);
        invalidateCache();
        if (ValueUtil.isNotEmpty(eventPublisher)) {
            eventPublisher.publishModeChanged(eqGroupId, previousMode, newMode.code(), operator, reason);
        }
        return saved;
    }

    /**
     * 배차 락 ON/OFF.
     */
    public boolean isDispatchLockEnabled(String eqGroupId)  { return getSnapshot(eqGroupId).dispatchLockEnabled; }

    /**
     * 운영 모드 게이팅 ON/OFF.
     */
    public boolean isOperationModeEnabled(String eqGroupId) { return getSnapshot(eqGroupId).operationModeEnabled; }

    /**
     * 검사(시험) 게이팅 ON/OFF.
     */
    public boolean isInspectionEnabled(String eqGroupId)    { return getSnapshot(eqGroupId).inspectionEnabled; }

    /**
     * 기능 플래그 변경.
     *
     * @param flagName  "dispatchLock" | "operationMode" | "inspection"
     * @param value     true=ON, false=OFF, null=상속(초기화)
     */
    @Transactional(rollbackFor = Exception.class)
    public TbWcsSystemMode updateFeatureFlag(String eqGroupId, String flagName, Boolean value, String operator, String reason) {
        if (!StringUtils.hasText(flagName)) {
            throw new ElidomRuntimeException("INVALID_PARAMETER", "flagName is required");
        }
        String pk = resolveRowId(eqGroupId);
        TbWcsSystemMode row = repo.findById(pk);
        boolean isNew = ValueUtil.isEmpty(row);
        if (isNew) {
            row = new TbWcsSystemMode();
            row.setId(pk);
            row.setOperationMode(WcsOperationMode.NORMAL.code());
            row.setEqGroupId(SystemModeRepository.GLOBAL_ID.equals(pk) ? null : eqGroupId);
        }
        // 플래그명별 setter 분기
        switch (flagName) {
            case FLAG_DISPATCH_LOCK  -> row.setIsDispatchLockEnabled(value);
            case FLAG_OPERATION_MODE -> row.setIsOperationModeEnabled(value);
            case FLAG_INSPECTION     -> row.setIsInspectionEnabled(value);
            default -> throw new ElidomRuntimeException("INVALID_PARAMETER",
                    "Unknown flagName: " + flagName);
        }
        row.setChangedBy(operator);
        row.setChangedAt(OffsetDateTime.now());
        row.setReason(reason);
        TbWcsSystemMode saved = isNew ? repo.insert(row) : repo.update(row);
        invalidateCache();
        if (ValueUtil.isNotEmpty(eventPublisher)) {
            eventPublisher.publishFlagChanged(eqGroupId, flagName, value, operator, reason);
        }
        return saved;
    }

    /**
     * host_order 단위 산출 게이팅 — 운영 모드 + scheduledDate + 포트 모드 정합성 일괄 검사.
     */
    public GatingResult check(TbWcsHostOrder host) {
        if (ValueUtil.isEmpty(host)) return GatingResult.ok();

        HostOrderType hostType = HostOrderType.from(host.getOrderType());
        OrderType type = ValueUtil.isNotEmpty(hostType) ? hostType.baseOrderType()
                : OrderType.from(host.getOrderType());
        String eqGroupId = host.getEqGroupId();

        // 운영 모드 게이팅
        GatingResult r = check(eqGroupId, type);
        if (!r.allowed()) return r;

        // 예약일 미도래
        LocalDate sched = LocalDateUtils.toLocalDate(host.getScheduledDate());
        if (ValueUtil.isNotEmpty(sched) && sched.isAfter(LocalDate.now())) {
            return GatingResult.blocked(WcsError.SCHEDULED_DATE_NOT_REACHED);
        }

        // 포트 모드 정합성
        if (!isPortModeReadyForType(eqGroupId, type)) {
            return GatingResult.blocked(WcsError.PORT_MODE_NOT_READY);
        }
        return GatingResult.ok();
    }

    /**
     * 그룹의 포트 상태가 OrderType 을 지금 받을 수 있는지 검사.
     * 모드가 명시적으로 INBOUND/OUTBOUND 일 때만 산출 허용 — 입출고 동시 진행 방지.
     */
    public boolean isPortModeReadyForType(String eqGroupId, OrderType type) {
        if (!StringUtils.hasText(eqGroupId)) return true;
        if (ValueUtil.isEmpty(type)) return true;
        if (type == OrderType.MOVE) return true;

        boolean isInboundDirection = (type == OrderType.INBOUND);

        // 방향별 SQL + 파라미터 빌드
        String sql;
        Map<String, Object> params;
        if (isInboundDirection) {
            sql = """
            SELECT COUNT(*) AS cnt FROM tb_inventory_location
             WHERE loc_group  = :eqGroupId
               AND is_enabled = TRUE
               AND (
                    loc_type = :inboundPort
                 OR (loc_type = :ioPort AND port_mode = :inboundMode)
               )
            """;
            params = ValueUtil.newMap(
                    "eqGroupId,inboundPort,ioPort,inboundMode",
                    eqGroupId,
                    LocType.INBOUND_PORT.code(),
                    LocType.IN_OUTBOUND_PORT.code(),
                    PortMode.INBOUND.code());
        } else {
            sql = """
            SELECT COUNT(*) AS cnt FROM tb_inventory_location
             WHERE loc_group  = :eqGroupId
               AND is_enabled = TRUE
               AND (
                    loc_type = :outboundPort
                 OR (loc_type = :ioPort
                     AND port_mode IN (:outboundMode, :outboundPriority))
               )
            """;
            params = ValueUtil.newMap(
                    "eqGroupId,outboundPort,ioPort,outboundMode,outboundPriority",
                    eqGroupId,
                    LocType.OUTBOUND_PORT.code(),
                    LocType.IN_OUTBOUND_PORT.code(),
                    PortMode.OUTBOUND.code(),
                    PortMode.OUTBOUND_PRIORITY.code());
        }

        // 카운트 결과로 가용 포트 존재 여부 판정
        @SuppressWarnings("rawtypes")
        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 1);
        if (ValueUtil.isEmpty(rows)) return false;
        Object cnt = rows.get(0).get("cnt");
        return cnt instanceof Number n && n.intValue() > 0;
    }

    /**
     * 운영 모드 게이팅만 — 플래그 OFF 또는 type 없으면 통과.
     */
    public GatingResult check(String eqGroupId, OrderType type) {
        if (!isOperationModeEnabled(eqGroupId)) return GatingResult.ok();
        if (ValueUtil.isEmpty(type)) return GatingResult.ok();
        if (!getCurrentMode(eqGroupId).allows(type)) {
            return GatingResult.blocked(WcsError.OPERATION_MODE_BLOCKED);
        }
        return GatingResult.ok();
    }

    /**
     * 스케줄러 tick 단위 그룹 차단 — MAINTENANCE 시 전체 skip.
     */
    public GatingResult checkGroup(String eqGroupId) {
        if (!isOperationModeEnabled(eqGroupId)) return GatingResult.ok();
        if (getCurrentMode(eqGroupId) == WcsOperationMode.MAINTENANCE) {
            return GatingResult.blocked(WcsError.OPERATION_MODE_BLOCKED);
        }
        return GatingResult.ok();
    }

    /**
     * 게이팅 결과 — allowed=true 면 통과, false 면 reason 으로 사유 표시.
     */
    public record GatingResult(boolean allowed, WcsError reason) {
        public static GatingResult ok() { return new GatingResult(true, null); }
        public static GatingResult blocked(WcsError r) { return new GatingResult(false, r); }
    }

    /**
     * eqGroupId 우선 row 검색 — id 일치 → eqGroupId 매칭 순.
     */
    private TbWcsSystemMode findPreferredRow(String eqGroupId) {
        if (!StringUtils.hasText(eqGroupId)) return null;
        if (SystemModeRepository.GLOBAL_ID.equals(eqGroupId)) return null;
        TbWcsSystemMode byId = repo.findById(eqGroupId);
        if (ValueUtil.isNotEmpty(byId)) return byId;
        return repo.findByEqGroupId(eqGroupId);
    }

    /**
     * 저장 대상 row id — eqGroupId 있으면 그것을, 없으면 GLOBAL_ID.
     */
    private static String resolveRowId(String eqGroupId) {
        return StringUtils.hasText(eqGroupId) ? eqGroupId : SystemModeRepository.GLOBAL_ID;
    }
}
