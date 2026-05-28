package operato.logis.wcs.scheduler;

import operato.logis.inventory.consts.StockStatus;
import operato.logis.wcs.consts.LocType;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.ShuttleOrderStatus;
import operato.logis.wcs.consts.StockType;
import operato.logis.wcs.dto.HostOrderApi;
import operato.logis.wcs.dto.WcsOrderCommand;
import operato.logis.wcs.entity.ExtTbInventoryLocation;
import operato.logis.wcs.service.impl.inventory.state.InventoryStockStateWriter;
import operato.logis.wcs.service.impl.order.intake.OrderIntakeService;
import operato.logis.wcs.service.impl.allocation.port.PortService;
import operato.logis.wcs.service.repository.InventoryLocationRepository;
import operato.logis.wcs.service.repository.InventoryStockRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WCS 유지보수성 잡 묶음 (10s 주기, 단일 잡으로 통합 실행).
 *
 * 역할별 실행 빈도 (사이클 카운터 기반):
 *   - runPortModeAutoSwitch()  — 매 사이클 (≈10s)
 *   - runStockRelocation()     — 현재 비활성 (필요 시 3사이클 ≈30s 로 활성화)
 *   - runPortReconciliation()  — 6사이클마다 (≈60s)
 *   - runExpiryCheck()         — 30사이클마다 (≈5분, §11 사용기한 만료 검사)
 *
 * WcsJobLauncher 가 10초 주기로 runMaintenanceCycle() 을 호출.
 * 각 작업은 try-catch 로 격리되어 한 작업 실패가 다른 작업을 막지 않는다.
 */
@Component
@RequiredArgsConstructor
public class MaintenanceJobs extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(MaintenanceJobs.class);

    private static final int COMPLETED_THRESHOLD = ShuttleOrderStatus.COMPLETED.codeAsIntOrNull();
    private static final List<String> PORT_TYPES = List.of(
            LocType.OUTBOUND_PORT.code().toString(),
            LocType.IN_OUTBOUND_PORT.code().toString());

    private static final String DEFAULT_OWNER  = "OWN001";
    private static final int    RELOC_PRIORITY = 7;

    /** 10s 주기 기준 디바이더 — cycleCount % N == 0 인 사이클에서 실행. */
    private static final int RELOC_EVERY_N_CYCLES  = 3;
    private static final int RECON_EVERY_N_CYCLES  = 6;
    private static final int EXPIRY_EVERY_N_CYCLES = 30;

    private final PortService portService;
    private final OrderIntakeService orderIntakeService;
    private final InventoryStockRepository stockRepository;
    private final InventoryLocationRepository inventoryLocationRepository;
    private final InventoryStockStateWriter stockStateWriter;

    private long cycleCount = 0L;

    /**
     * 통합 사이클 — 매 사이클 PortModeAutoSwitch, N사이클마다 Reconciliation / ExpiryCheck.
     * 단일 풀 스레드 점유 시간을 짧게 유지하면서 잡 개수를 1개로 줄인다.
     */
    public void runMaintenanceCycle() {
        long c = cycleCount++;

        // 매 사이클 — 포트 모드 자동 전환
        safeRun("PortModeAutoSwitch", this::runPortModeAutoSwitch);

        // 6사이클마다 — 포트 Ghost Count 보정
        if (c % RECON_EVERY_N_CYCLES == 0) {
            safeRun("PortReconciliation", this::runPortReconciliation);
        }

        // 30사이클마다 — 사용기한 만료 자동 격리 (§11)
        if (c % EXPIRY_EVERY_N_CYCLES == 0) {
            safeRun("ExpiryCheck", this::runExpiryCheck);
        }
    }

    /**
     * 사용기한 만료 stock 자동 격리.
     * expired_datetime < NOW() AND stock_type=NORMAL AND stock_status=IDLE → HOLD + QC_FAIL 로 전이.
     * 출고 후보에서 자동 제외되며 Dashboard2D 의 CellStateClassifier 가 ABNORMAL 로 표시.
     */
    @Transactional(rollbackFor = Exception.class)
    public void runExpiryCheck() {
        @SuppressWarnings("rawtypes")
        List<Map> rows = stockRepository.findExpiredNormalStocks();
        if (ValueUtil.isEmpty(rows)) return;

        // stock 별 상태 전이
        int transitioned = 0;
        for (Map r : rows) {
            String eqGroupId = String.valueOf(r.get("eq_group_id"));
            String stockId   = String.valueOf(r.get("stock_id"));
            int updated = stockStateWriter.markStockQcFailed(eqGroupId, stockId);
            if (updated > 0) transitioned += updated;
        }
        if (transitioned > 0) {
            logger.info("[ Scheduler ][ Maintenance ] expired stocks transitioned - count={}", transitioned);
        }
    }

    /**
     * 잡 실행 가드 — 한 잡 예외가 다른 잡을 막지 못하게.
     */
    private void safeRun(String name, Runnable r) {
        try {
            r.run();
        } catch (Exception e) {
            logger.error("[ Scheduler ][ Maintenance ] job failed - name={}", name, e);
        }
    }

    /**
     * 포트 모드 자동 전환 — 게이팅 상태에 따라 IN_OUTBOUND_PORT 의 port_mode 가 자동 토글.
     */
    public void runPortModeAutoSwitch() {
        int switched = portService.runPortModeAutoSwitch();
        if (switched > 0) {
            logger.info("[ Scheduler ][ Maintenance ] port mode auto-switched - count={}", switched);
        }
    }

    /**
     * 포트 Ghost Count 보정 — active_task_count 와 실측 카운트 동기화.
     */
    @Transactional(rollbackFor = Exception.class)
    public void runPortReconciliation() {
        long start = System.currentTimeMillis();
        int updated = 0;

        @SuppressWarnings("rawtypes")
        List<Map> realCounts = aggregateRealCounts();

        for (Map row : realCounts) {
            String eqGroupId = (String) row.get("eq_group_id");
            String locId = (String) row.get("loc_id");
            int realCount = ((Number) row.get("real_count")).intValue();

            ExtTbInventoryLocation loc = inventoryLocationRepository.findByEqGroupIdAndLocId(eqGroupId, locId);

            if (ValueUtil.isNotEmpty(loc)) {
                int currentCount = loc.getActiveTaskCount() != null ? loc.getActiveTaskCount() : 0;

                // 실측 카운트와 다를 경우에만 업데이트
                if (currentCount != realCount) {
                    loc.setActiveTaskCount(realCount);
                    inventoryLocationRepository.update(loc, "activeTaskCount");

                    logger.info("[ Scheduler ][ Maintenance ] sync - eqGroup={}, port={}, {} -> {}",
                            eqGroupId, locId, currentCount, realCount);
                    updated++;
                }
            }
        }

        if (updated > 0) {
            logger.info("[ Scheduler ][ Maintenance ] reconciliation done - updated={}, tookMs={}",
                    updated, System.currentTimeMillis() - start);
        }
    }

    /**
     * 포트별 진행 중 셔틀 오더 수 집계.
     */
    @SuppressWarnings({"rawtypes", "rawtypes"})
    private List<Map> aggregateRealCounts() {
        String sql = """
            SELECT m.loc_group AS eq_group_id,\s
                   m.loc_id,\s
                   COUNT(o.order_key) AS real_count
              FROM tb_inventory_location m
              LEFT JOIN tb_wcs_shuttle_order o
                ON (m.loc_id = o.from_loc_code OR m.loc_id = o.to_loc_code)
               AND o.order_status < :completedStatus
             WHERE m.loc_type IN (:portTypes)
               AND m.is_enabled = true
             GROUP BY m.loc_group, m.loc_id
           \s""";

        Map<String, Object> params = ValueUtil.newMap("completedStatus,portTypes", COMPLETED_THRESHOLD, PORT_TYPES);

        return this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);
    }

    /**
     * 재고 자동 재배치:
     *   - stock_type=DISPOSAL 이지만 현재 location.item_group ≠ 'DISPOSAL_ZONE'
     *   - stock_type=RETURN   이지만 현재 location.item_group ≠ 'RETURN_ZONE'
     * 이미 RELOCATION 진행 중인 stock 은 skip.
     */
    public void runStockRelocation() {
        int disposal = scanAndMove(StockType.DISPOSAL, "DISPOSAL_ZONE");
        int ret      = scanAndMove(StockType.RETURN,   "RETURN_ZONE");
        if (disposal > 0 || ret > 0) {
            logger.info("[ Scheduler ][ Maintenance ] stock relocation done - disposal={}, return={}",
                    disposal, ret);
        }
    }

    /**
     * 대상 stock 검출 → 목표 zone 의 빈 셀 선점 → MOVE 발행. 빈 셀 없으면 skip.
     */
    @SuppressWarnings("rawtypes")
    private int scanAndMove(StockType targetType, String targetZone) {
        List<Map> rows = findRelocationTargets(targetType, targetZone);
        if (ValueUtil.isEmpty(rows)) return 0;

        int issued = 0;
        for (Map row : rows) {
            String stockId   = String.valueOf(row.get("stock_id"));
            String eqGroupId = String.valueOf(row.get("eq_group_id"));
            String fromLocId = String.valueOf(row.get("loc_id"));
            String ownerCode = ValueUtil.isNotEmpty(row.get("item_owner"))
                    ? String.valueOf(row.get("item_owner"))
                    : DEFAULT_OWNER;

            // 목표 zone 의 빈 셀 선점
            String toLocId = pickEmptyLocation(eqGroupId, targetZone);
            if (ValueUtil.isEmpty(toLocId)) {
                logger.warn("[ Scheduler ][ Maintenance ] relocation skip - no empty zone={}, stockId={}, type={}",
                        targetZone, stockId, targetType.code());
                continue;
            }

            // MOVE 발행
            HostOrderApi.Response resp = orderIntakeService.execute(buildCommand(
                    targetType, eqGroupId, ownerCode, fromLocId, toLocId));

            if (resp.isSuccess()) {
                issued++;
                logger.info("[ Scheduler ][ Maintenance ] relocation issued - type={}, stockId={}, from={}, to={}, wcs={}",
                        targetType.code(), stockId, fromLocId, toLocId, resp.getWcsOrderKey());
            } else {
                logger.warn("[ Scheduler ][ Maintenance ] relocation failed - type={}, stockId={}, cause={}",
                        targetType.code(), stockId, resp.getErrorDesc());
            }
        }
        return issued;
    }

    /**
     * 재배치 대상 stock 검출 — RELOCATION 진행 중·task 진행 중은 제외.
     */
    @SuppressWarnings("rawtypes")
    private List<Map> findRelocationTargets(StockType targetType, String targetZone) {
        String sql = """
            SELECT s.stock_id, s.eq_group_id, s.item_owner, l.loc_id
              FROM tb_inventory_stock s
              JOIN tb_inventory_location l
                ON l.stock_id = s.stock_id
               AND l.loc_group = s.eq_group_id
             WHERE s.stock_type = :stockType
               AND s.stock_status <> :relocStatus
               AND COALESCE(l.item_group, '') <> :targetZone
               AND COALESCE(l.task_id, '') = ''
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "stockType,relocStatus,targetZone",
                targetType.code(), StockStatus.RELOCATION.value(), targetZone);
        return this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);
    }

    /**
     * 목표 zone 의 빈 RACK 셀 1개 picking — stock/task 모두 비어 있어야.
     */
    @SuppressWarnings("rawtypes")
    private String pickEmptyLocation(String eqGroupId, String targetZone) {
        String sql = """
            SELECT loc_id
              FROM tb_inventory_location
             WHERE loc_group   = :eqGroupId
               AND item_group  = :targetZone
               AND loc_type    = 'RACK'
               AND is_enabled  = true
               AND COALESCE(stock_id, '') = ''
               AND COALESCE(task_id, '')  = ''
             ORDER BY loc_id
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,targetZone", eqGroupId, targetZone);
        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 1);
        return ValueUtil.isEmpty(rows) ? null : String.valueOf(rows.get(0).get("loc_id"));
    }

    /**
     * 자동 재배치용 MOVE 커맨드 빌드. persistHostOrder=false 로 가상 호스트.
     */
    private WcsOrderCommand buildCommand(StockType targetType, String eqGroupId,
                                         String ownerCode, String fromLocId, String toLocId) {
        return WcsOrderCommand.builder()
                .orderType(OrderType.MOVE.codeAsString())
                .hostOrderKey("AUTO-RELOC-" + targetType.code() + "-" + System.currentTimeMillis())
                .ownerCode(ownerCode)
                .eqGroupId(eqGroupId)
                .fromLocId(fromLocId)
                .toLocId(toLocId)
                .priority(RELOC_PRIORITY)
                .persistHostOrder(false)
                .build();
    }
}
