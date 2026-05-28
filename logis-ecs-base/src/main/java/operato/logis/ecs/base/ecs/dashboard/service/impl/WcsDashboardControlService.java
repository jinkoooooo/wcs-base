package operato.logis.ecs.base.ecs.dashboard.service.impl;

import operato.logis.ecs.base.ecs.dashboard.dto.DashboardControlInfo;
import operato.logis.ecs.base.ecs.dashboard.dto.DashboardControlResponse;
import operato.logis.ecs.base.ecs.dashboard.realtime.util.RowConverter;
import operato.logis.ecs.base.ecs.entity.TbEqCraneMst;
import operato.logis.ecs.base.ecs.entity.TbEqCvMst;
import operato.logis.ecs.base.ecs.entity.TbWcsCraneOrder;
import operato.logis.ecs.base.wcs.consts.WcsDomainEnums;
import operato.logis.ecs.base.wcs.entity.ExtTbInventoryLocation;
import operato.logis.ecs.base.wcs.entity.ExtTbInventoryStock;
import operato.logis.ecs.base.wcs.service.impl.ExtTbInventoryLocationService;
import operato.logis.ecs.base.wcs.service.impl.ExtTbInventoryStockService;
import operato.logis.ecs.base.wcs.service.impl.TbWcsCraneOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * WCS 대시보드 팝업 수동 제어 서비스
 *
 * [교체 완료] TbWcsLocMst → ExtTbInventoryLocation, TbWcsInventory → ExtTbInventoryStock
 * - lockYn/lockBy → taskId
 * - status → stockId IS NULL/NOT NULL
 * - useYn → isEnabled
 * - eqGroupId → locGroup
 */
@Service
public class WcsDashboardControlService extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(WcsDashboardControlService.class);

    /** 수동 잠금 시 taskId 필드에 기록하는 식별자 */
    private static final String MANUAL_LOCK_BY = "MANUAL";

    @Autowired
    private ExtTbInventoryLocationService locMstService;

    @Autowired
    private ExtTbInventoryStockService inventoryService;

    @Autowired
    private TbWcsCraneOrderService craneOrderService;

    // =========================================================================
    // [1] 제어 정보 조회 (팝업 오픈 시 호출)
    // =========================================================================

    /**
     * 랙 셀 제어 정보 조회
     *
     * @param rackCellId TbEqRackMst.id (= TbEcs2dItem.realEqId for RACK type)
     */
    @Transactional(readOnly = true)
    public DashboardControlInfo getRackControlInfo(String eqGroupId, String rackCellId) {
        if (!StringUtils.hasText(rackCellId)) {
            return DashboardControlInfo.builder().build();
        }

        // rackCellId = ExtTbInventoryLocation.locId — 이것이 ECS ↔ WCS 연결 핵심 조인 키
        String sql = "SELECT * FROM tb_inventory_location WHERE loc_group = :eqGroupId AND loc_id = :locId LIMIT 1";
        Map<String, Object> params = ValueUtil.newMap("eqGroupId,locId", eqGroupId, rackCellId);
        ExtTbInventoryLocation loc = this.queryManager.selectBySql(sql, params, ExtTbInventoryLocation.class);

        if (loc == null) {
            logger.warn("[CONTROL_INFO] WCS 로케이션 미매핑 랙 셀: rackCellId={}", rackCellId);
            return DashboardControlInfo.builder().build();
        }

        // 해당 로케이션의 재고 상세 조회 (혼적 팔레트 N 행 대응, 상세 필드 포함)
        List<DashboardControlInfo.InventoryItem> inventoryItems = fetchInventoryDetail(
                loc.getLocGroup(), loc.getLocId()
        );

        // 현재 이 로케이션을 점유 중인 활성 WCS 오더 조회
        String orderSql = """
                SELECT * FROM tb_wcs_shuttle_order
                WHERE eq_group_id   = :eqGroupId
                  AND (from_loc_code = :locId OR to_loc_code = :locId)
                  AND order_status NOT IN (90, 91, 95)
                ORDER BY created_at DESC
                LIMIT 1
                """;
        Map<String, Object> orderParams = ValueUtil.newMap(
                "eqGroupId,locId", loc.getLocGroup(), loc.getLocId()
        );
        TbWcsCraneOrder activeOrder = this.queryManager.selectBySql(
                orderSql, orderParams, TbWcsCraneOrder.class
        );

        return DashboardControlInfo.builder()
                .locId(loc.getLocId())
                .eqGroupId(loc.getLocGroup())
                .locUseYn(Boolean.TRUE.equals(loc.getIsEnabled()) ? 1 : 0)
                .locLockYn(loc.getTaskId() != null ? 1 : 0)
                .locLockBy(loc.getTaskId())
                .locStatus(loc.getStockId() != null
                        ? WcsDomainEnums.LocStatus.OCCUPIED.codeAsIntOrNull()
                        : WcsDomainEnums.LocStatus.EMPTY.codeAsIntOrNull())
                .activeOrderKey(activeOrder != null ? activeOrder.getOrderKey() : null)
                .activeOrderStatus(activeOrder != null ? activeOrder.getOrderStatus() : null)
                .activeOrderType(activeOrder != null ? activeOrder.getOrderType() : null)
                .inventory(inventoryItems)
                .build();
    }

    /** 설비 제어 정보 조회 (CONVEYOR / LIFTER / SHUTTLE — 비랙 타입) */
    @Transactional(readOnly = true)
    public DashboardControlInfo getEquipmentControlInfo(String eqType, String eqId) {
        if (!StringUtils.hasText(eqType) || !StringUtils.hasText(eqId)) {
            return DashboardControlInfo.builder().build();
        }

        boolean useYn = fetchEquipmentUseYn(eqType, eqId);
        return DashboardControlInfo.builder()
                .eqUseYn(useYn)
                .inventory(Collections.emptyList())
                .build();
    }

    // =========================================================================
    // [2] Use/Disable 제어 (설비 가동/비가동)
    // =========================================================================

    /** 랙 로케이션 사용 여부 토글 */
    @Transactional(rollbackFor = Exception.class)
    public DashboardControlResponse toggleLocUse(String eqGroupId, String locId) {
        ExtTbInventoryLocation loc = locMstService.findByEqGroupIdAndLocId(eqGroupId, locId);
        if (loc == null) {
            return DashboardControlResponse.fail("로케이션을 찾을 수 없습니다: " + locId);
        }

        // 잠금 상태(taskId 존재)에서 use 토글은 위험 — 잠금 해제 후에만 허용
        if (loc.getTaskId() != null) {
            return DashboardControlResponse.fail("수동 잠금 상태입니다. 먼저 Lock을 해제하세요.");
        }

        boolean newIsEnabled;
        String actionMsg;

        if (Boolean.TRUE.equals(loc.getIsEnabled())) {
            // 사용 → 비가동
            newIsEnabled = false;
            actionMsg = "로케이션 비가동 처리 완료: " + locId;
        } else {
            // 비가동 → 사용
            newIsEnabled = true;
            actionMsg = "로케이션 가동 처리 완료: " + locId;
        }

        String updateSql = "UPDATE tb_inventory_location SET is_enabled = :isEnabled " +
                "WHERE loc_group = :locGroup AND loc_id = :locId";
        this.queryManager.executeBySql(updateSql,
                ValueUtil.newMap("isEnabled,locGroup,locId", newIsEnabled, eqGroupId, locId));

        logger.info("[LOC_USE_TOGGLE] eqGroupId={}, locId={}, isEnabled={}", eqGroupId, locId, newIsEnabled);
        return DashboardControlResponse.ok(actionMsg,
                Map.of("locUseYn", newIsEnabled ? 1 : 0));
    }

    /** 설비 마스터 사용 여부 토글 (CONVEYOR / LIFTER / SHUTTLE) */
    @Transactional(rollbackFor = Exception.class)
    public DashboardControlResponse toggleEquipmentUse(String eqType, String eqId) {
        if (!StringUtils.hasText(eqType) || !StringUtils.hasText(eqId)) {
            return DashboardControlResponse.fail("설비 타입 또는 ID가 누락되었습니다.");
        }

        boolean currentUseYn = fetchEquipmentUseYn(eqType, eqId);
        boolean newUseYn = !currentUseYn;

        String tableName = resolveEquipmentTable(eqType);
        if (tableName == null) {
            return DashboardControlResponse.fail("지원하지 않는 설비 타입입니다: " + eqType);
        }

        String updateSql = "UPDATE " + tableName + " SET use_yn = :useYn WHERE id = :eqId";
        this.queryManager.executeBySql(updateSql, ValueUtil.newMap("useYn,eqId", newUseYn, eqId));

        String msg = newUseYn ? "설비 가동 처리 완료" : "설비 비가동 처리 완료";
        logger.info("[EQ_USE_TOGGLE] eqType={}, eqId={}, useYn={}", eqType, eqId, newUseYn);
        return DashboardControlResponse.ok(msg, Map.of("eqUseYn", newUseYn));
    }

    // =========================================================================
    // [3] 수동 Lock/Unlock 제어
    // =========================================================================

    /** 로케이션 수동 잠금 (taskId = "MANUAL" 세팅) */
    @Transactional(rollbackFor = Exception.class)
    public DashboardControlResponse manualLock(String eqGroupId, String locId) {
        ExtTbInventoryLocation loc = locMstService.findByEqGroupIdAndLocId(eqGroupId, locId);
        if (loc == null) {
            return DashboardControlResponse.fail("로케이션을 찾을 수 없습니다: " + locId);
        }
        if (loc.getTaskId() != null) {
            String lockedBy = loc.getTaskId();
            // 오더가 선점한 락은 수동으로 풀면 안 된다. 오더 제어를 통해 처리해야 한다.
            if (!MANUAL_LOCK_BY.equals(lockedBy)) {
                return DashboardControlResponse.fail(
                        "작업(" + lockedBy + ")이 선점한 Lock입니다. 해당 작업을 먼저 처리하세요.");
            }
            return DashboardControlResponse.fail("이미 수동 잠금 상태입니다.");
        }

        boolean locked = locMstService.lock(eqGroupId, locId, MANUAL_LOCK_BY);
        if (!locked) {
            return DashboardControlResponse.fail("잠금 처리에 실패했습니다. 다시 시도해주세요.");
        }

        logger.info("[MANUAL_LOCK] eqGroupId={}, locId={}", eqGroupId, locId);
        return DashboardControlResponse.ok("로케이션 수동 잠금 완료: " + locId,
                Map.of("locLockYn", 1, "locLockBy", MANUAL_LOCK_BY));
    }

    /** 로케이션 수동 잠금 해제 (taskId = NULL) */
    @Transactional(rollbackFor = Exception.class)
    public DashboardControlResponse manualUnlock(String eqGroupId, String locId) {
        ExtTbInventoryLocation loc = locMstService.findByEqGroupIdAndLocId(eqGroupId, locId);
        if (loc == null) {
            return DashboardControlResponse.fail("로케이션을 찾을 수 없습니다: " + locId);
        }
        if (loc.getTaskId() == null) {
            return DashboardControlResponse.fail("잠금 상태가 아닙니다.");
        }
        // 오더 선점 락은 수동 해제 금지
        if (!MANUAL_LOCK_BY.equals(loc.getTaskId())) {
            return DashboardControlResponse.fail(
                    "작업(" + loc.getTaskId() + ")이 선점한 Lock입니다. 해당 작업을 먼저 처리하세요.");
        }

        locMstService.unlock(eqGroupId, locId);

        logger.info("[MANUAL_UNLOCK] eqGroupId={}, locId={}", eqGroupId, locId);
        return DashboardControlResponse.ok("로케이션 수동 잠금 해제 완료: " + locId,
                Map.of("locLockYn", 0));
    }

    // =========================================================================
    // [4] 재고 정합성 제어 (Empty Pick / Double Entry 복구)
    // =========================================================================

    /** 수동 재고 삭제 — 공출고(Empty Pick) 복구 */
    @Transactional(rollbackFor = Exception.class)
    public DashboardControlResponse deleteInventory(String eqGroupId, String locId) {
        List<ExtTbInventoryStock> invList = inventoryService.findByEqGroupIdAndLocId(eqGroupId, locId);

        if (invList == null || invList.isEmpty()) {
            return DashboardControlResponse.fail("삭제할 재고가 없습니다. (이미 EMPTY 상태)");
        }

        // 재고 삭제 (JOIN 기반이므로 stock_id를 통해 삭제)
        for (ExtTbInventoryStock inv : invList) {
            String deleteSql = "DELETE FROM tb_inventory_stock WHERE id = :id";
            this.queryManager.executeBySql(deleteSql, ValueUtil.newMap("id", inv.getId()));
        }

        // 로케이션 stockId 초기화 (EMPTY 상태로 복원)
        String updateLocSql = "UPDATE tb_inventory_location SET stock_id = NULL " +
                "WHERE loc_group = :locGroup AND loc_id = :locId";
        this.queryManager.executeBySql(updateLocSql,
                ValueUtil.newMap("locGroup,locId", eqGroupId, locId));

        logger.info("[INVENTORY_DELETE] eqGroupId={}, locId={}, count={}", eqGroupId, locId, invList.size());
        return DashboardControlResponse.ok(
                "재고 " + invList.size() + "건 삭제 완료. 로케이션 상태 → EMPTY");
    }

    /** 수동 재고 생성 — 이중입고(Double Entry) 복구 */
    @Transactional(rollbackFor = Exception.class)
    public DashboardControlResponse createInventory(
            String eqGroupId, String locId, String skuCode, String palletId, int qty, String ownerCode) {

        if (!StringUtils.hasText(skuCode) || qty <= 0) {
            return DashboardControlResponse.fail("SKU 코드와 수량(1 이상)은 필수입니다.");
        }

        String stockId = StringUtils.hasText(palletId) ? palletId : locId;

        ExtTbInventoryStock inv = new ExtTbInventoryStock();
        inv.setEqGroupId(eqGroupId);
        inv.setStockId(stockId);
        inv.setSku(skuCode);
        inv.setItemOwner(StringUtils.hasText(ownerCode) ? ownerCode : "");
        inv.setItemQty(qty);
        inv.setStockStatus(0); // 0 = 정상
        inv.setIsEnabled(true);
        inv.setLotNo("");
        inv.setInbDatetime(new Date());

        this.queryManager.insert(inv);

        // 로케이션 stockId 갱신 (OCCUPIED 상태)
        String updateLocSql = "UPDATE tb_inventory_location SET stock_id = :stockId " +
                "WHERE loc_group = :locGroup AND loc_id = :locId";
        this.queryManager.executeBySql(updateLocSql,
                ValueUtil.newMap("stockId,locGroup,locId", stockId, eqGroupId, locId));

        logger.info("[INVENTORY_CREATE] eqGroupId={}, locId={}, skuCode={}, stockId={}, qty={}",
                eqGroupId, locId, skuCode, stockId, qty);
        return DashboardControlResponse.ok("재고 수동 생성 완료. 로케이션 상태 → OCCUPIED",
                Map.of("inventoryId", inv.getId()));
    }

    // =========================================================================
    // [5] 작업(Order) 흐름 제어
    // =========================================================================

    /** 작업 강제 종료 (Force Complete) */
    @Transactional(rollbackFor = Exception.class)
    public DashboardControlResponse forceCompleteOrder(String orderKey) {
        TbWcsCraneOrder order = craneOrderService.findByOrderKey(orderKey);
        if (order == null) {
            return DashboardControlResponse.fail("작업을 찾을 수 없습니다: " + orderKey);
        }

        if (WcsDomainEnums.ShuttleOrderStatus.isFinalStatus(order.getOrderStatus())) {
            return DashboardControlResponse.fail(
                    "이미 종료된 작업입니다. (현재 상태: " + order.getOrderStatus() + ")");
        }

        order.setOrderStatus(WcsDomainEnums.ShuttleOrderStatus.COMPLETED.codeAsIntOrNull());
        //todo: 기존. order.setRemark("[수동 강제완료] " + new Date());
        craneOrderService.update(order, "orderStatus", "remark");

        // 로케이션 잠금(taskId) 해제
        unlockOrderLocations(order, true);

        logger.info("[FORCE_COMPLETE] orderKey={}", orderKey);
        return DashboardControlResponse.ok("작업 강제 완료 처리됨: " + orderKey);
    }

    /** 작업 취소 (Cancel) */
    @Transactional(rollbackFor = Exception.class)
    public DashboardControlResponse cancelOrder(String orderKey, String reason) {
        TbWcsCraneOrder order = craneOrderService.findByOrderKey(orderKey);
        if (order == null) {
            return DashboardControlResponse.fail("작업을 찾을 수 없습니다: " + orderKey);
        }

        if (WcsDomainEnums.ShuttleOrderStatus.isFinalStatus(order.getOrderStatus())) {
            return DashboardControlResponse.fail(
                    "이미 종료된 작업입니다. (현재 상태: " + order.getOrderStatus() + ")");
        }

        order.setOrderStatus(WcsDomainEnums.ShuttleOrderStatus.CANCELLED.codeAsIntOrNull());
        // todo: 기존. order.setRemark("[수동 취소] " + (StringUtils.hasText(reason) ? reason : "사유 없음") + " | " + new Date());
        craneOrderService.update(order, "orderStatus", "remark");

        // 로케이션 잠금(taskId) 해제
        unlockOrderLocations(order, false);

        logger.info("[ORDER_CANCEL] orderKey={}, reason={}", orderKey, reason);
        return DashboardControlResponse.ok("작업 취소 완료: " + orderKey);
    }

    /** 작업 재개 (Resume) */
    @Transactional(rollbackFor = Exception.class)
    public DashboardControlResponse resumeOrder(String orderKey) {
        TbWcsCraneOrder order = craneOrderService.findByOrderKey(orderKey);
        if (order == null) {
            return DashboardControlResponse.fail("작업을 찾을 수 없습니다: " + orderKey);
        }

        if (WcsDomainEnums.ShuttleOrderStatus.isFinalStatus(order.getOrderStatus())) {
            return DashboardControlResponse.fail(
                    "이미 종료된 작업은 재개할 수 없습니다. (현재 상태: " + order.getOrderStatus() + ")");
        }

        int currentStatus = order.getOrderStatus();
        if (currentStatus < 100) {
            return DashboardControlResponse.fail(
                    "현재 진행 중인 작업입니다. 에러 상태의 작업만 재개할 수 있습니다. (현재 상태: " + currentStatus + ")");
        }

        order.setOrderStatus(WcsDomainEnums.ShuttleOrderStatus.SENT.codeAsIntOrNull());
        // todo: 기존. order.setRemark("[수동 재개] " + new Date());
        craneOrderService.update(order, "orderStatus");

        logger.info("[ORDER_RESUME] orderKey={}, prevStatus={}", orderKey, currentStatus);
        return DashboardControlResponse.ok("작업 재개 요청 완료. ECS 재전송 대기 중: " + orderKey);
    }

    // =========================================================================
    // [6] 내부 헬퍼 메서드
    // =========================================================================

    /** 해당 로케이션에 재고가 존재하는지 확인 */
    private boolean hasInventory(String eqGroupId, String locId) {
        List<ExtTbInventoryStock> invList = inventoryService.findByEqGroupIdAndLocId(eqGroupId, locId);
        return invList != null && !invList.isEmpty();
    }

    /**
     * 랙 셀의 재고 상세 조회 (팝업 전용).
     * tb_inventory_location + tb_inventory_stock 2-way JOIN.
     *
     * 주의사항:
     * - JOIN 키: stk.stock_id = loc.stock_id (stk.id 가 아님)
     * stk.id 는 UUID PK, loc.stock_id 는 팔레트 그룹 키
     * - 한 팔레트(stock_id)에 여러 SKU/Lot 행 가능 → List 반환 (혼적 팔레트)
     * - DOUBLE_IN / EMPTY_OUT 마커는 실제 재고 아님 → JOIN 조건에서 제외
     * - 로케이션만 있고 재고 없는 경우(빈 랙) → 빈 List 반환
     */
    @SuppressWarnings("unchecked")
    private List<DashboardControlInfo.InventoryItem> fetchInventoryDetail(
            String eqGroupId, String locId) {

        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(locId)) {
            return Collections.emptyList();
        }

        String sql = """
                SELECT
                    loc.loc_type          AS loc_type,
                    loc.port_mode         AS port_mode,
                    loc.barcode           AS scanned_barcode,
                
                    stk.id                AS stock_row_id,
                    stk.stock_id          AS stock_id,
                    stk.sku               AS sku,
                    stk.item_owner        AS item_owner,
                    stk.item_code         AS item_code,
                    stk.item_qty          AS item_qty,
                    stk.lot_no            AS lot_no,
                    stk.stock_status      AS stock_status,
                    stk.item_priority     AS item_priority,
                    stk.stock_height      AS stock_height,
                    stk.attribute_a       AS attribute_a,
                
                    TO_CHAR(stk.inb_datetime,     'YYYY-MM-DD HH24:MI:SS') AS inb_datetime,
                    TO_CHAR(stk.expired_datetime, 'YYYY-MM-DD')            AS expired_date,
                    TO_CHAR(stk.produce_date,     'YYYY-MM-DD')            AS produce_date
                
                  FROM tb_inventory_location loc
                  LEFT JOIN tb_inventory_stock stk
                         ON stk.stock_id = loc.stock_id
                        AND loc.stock_id NOT IN ('DOUBLE_IN', 'EMPTY_OUT')
                 WHERE loc.loc_group = :eqGroupId
                   AND loc.loc_id  = :locId
                 ORDER BY stk.inb_datetime DESC NULLS LAST,
                          stk.sku ASC NULLS LAST
                """;

        Map<String, Object> params = ValueUtil.newMap("eqGroupId,locId", eqGroupId, locId);
        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);

        return ((List<Map<String, Object>>) (List<?>) rows).stream()
                // stock 이 null 이면 (로케이션만 있고 재고 없음) 제외
                .filter(row -> row.get("stock_row_id") != null)
                .map(row -> DashboardControlInfo.InventoryItem.builder()
                        // 기존 필드
                        .id(RowConverter.toStringValue(row.get("stock_row_id")))
                        .skuCode(RowConverter.toStringValue(row.get("sku")))
                        .palletId(RowConverter.toStringValue(row.get("stock_id")))
                        .qty(RowConverter.toInt(row.get("item_qty"), 0))
                        .allocQty(0)
                        .stockStatus(RowConverter.toInt(row.get("stock_status"), 0))
                        // 신규: Stock 상세
                        .itemCode(RowConverter.toStringValue(row.get("item_code")))
                        .lotNo(RowConverter.toStringValue(row.get("lot_no")))
                        .itemOwner(RowConverter.toStringValue(row.get("item_owner")))
                        .inbDatetime(RowConverter.toStringValue(row.get("inb_datetime")))
                        .expiredDate(RowConverter.toStringValue(row.get("expired_date")))
                        .produceDate(RowConverter.toStringValue(row.get("produce_date")))
                        .itemPriority(toIntegerOrNull(row.get("item_priority")))
                        .stockHeight(toIntegerOrNull(row.get("stock_height")))
                        .attributeA(RowConverter.toStringValue(row.get("attribute_a")))
                        // 신규: Location 메타
                        .locType(RowConverter.toStringValue(row.get("loc_type")))
                        .portMode(RowConverter.toStringValue(row.get("port_mode")))
                        .scannedBarcode(RowConverter.toStringValue(row.get("scanned_barcode")))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Object → Integer (null 유지). RowConverter.toInt 는 default 를 강제하므로
     * nullable Integer 필드에는 부적합 → 이 헬퍼로 null 보존.
     */
    private static Integer toIntegerOrNull(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean fetchEquipmentUseYn(String eqType, String eqId) {
        String upper = eqType.toUpperCase();
        if ("SHUTTLE".equals(upper)) {
            TbEqCraneMst car = this.queryManager.select(TbEqCraneMst.class, eqId);
            return car != null && car.isUseYn();
        } else if ("CONVEYOR".equals(upper) || "LIFTER".equals(upper)) {
            TbEqCvMst cv = this.queryManager.select(TbEqCvMst.class, eqId);
            return cv != null && cv.isUseYn();
        }
        return false;
    }

    private String resolveEquipmentTable(String eqType) {
        String upper = eqType.toUpperCase();
        if ("SHUTTLE".equals(upper)) return "tb_eq_car_mst";
        if ("CONVEYOR".equals(upper) || "LIFTER".equals(upper)) return "tb_eq_cv_mst";
        return null;
    }

    /** 오더 취소/완료 시 로케이션 잠금(taskId) 해제 */
    private void unlockOrderLocations(TbWcsCraneOrder order, boolean isComplete) {
        String eqGroupId = order.getEqGroupId();
        String fromLoc = order.getFromLocId();
        String toLoc = order.getToLocId();
        String orderType = order.getOrderType();

        if (!StringUtils.hasText(eqGroupId)) return;

        try {
            if ("INBOUND".equalsIgnoreCase(orderType)) {
                if (StringUtils.hasText(toLoc)) locMstService.unlock(eqGroupId, toLoc);

            } else if ("OUTBOUND".equalsIgnoreCase(orderType)) {
                if (StringUtils.hasText(fromLoc)) locMstService.unlock(eqGroupId, fromLoc);

            } else if ("MOVE".equalsIgnoreCase(orderType)) {
                if (StringUtils.hasText(fromLoc)) locMstService.unlock(eqGroupId, fromLoc);
                if (StringUtils.hasText(toLoc)) locMstService.unlock(eqGroupId, toLoc);
            }
        } catch (Exception e) {
            logger.error("[UNLOCK_FAIL] orderKey={}, eqGroupId={}, error={}",
                    order.getOrderKey(), eqGroupId, e.getMessage());
        }
    }
}
