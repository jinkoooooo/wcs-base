package operato.logis.ecs.tspg4way.dashboard.service.impl;

import operato.logis.ecs.tspg4way.dashboard.dto.DashboardControlInfo;
import operato.logis.ecs.tspg4way.dashboard.dto.DashboardControlResponse;
import operato.logis.ecs.tspg4way.dashboard.realtime.util.RowConverter;
import operato.logis.ecs.tspg4way.entity.TbEqCarMst;
import operato.logis.ecs.tspg4way.entity.TbEqCvMst;
import operato.logis.inventory.consts.StockStatus;
import operato.logis.wcs.consts.*;
import operato.logis.wcs.entity.ExtTbInventoryLocation;
import operato.logis.wcs.entity.ExtTbInventoryStock;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.service.impl.inventory.state.InventoryStockStateWriter;
import operato.logis.wcs.service.repository.InventoryItemMasterRepository;
import operato.logis.wcs.service.repository.InventoryLocationRepository;
import operato.logis.wcs.service.repository.InventoryStockRepository;
import operato.logis.wcs.service.repository.ShuttleOrderRepository;
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
 * WCS 대시보드 팝업 수동 제어 서비스. 로케이션/설비 use 토글, 수동 lock/unlock, 재고 정합성(Empty Pick/Double Entry) 복구, 오더 흐름 제어.
 * 저장소: ExtTbInventoryLocation(잠금=taskId, 사용=isEnabled, 그룹=locGroup) + ExtTbInventoryStock(stockId 유무로 점유 판정).
 */
@Service
public class WcsDashboardControlService extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(WcsDashboardControlService.class);

    /** 수동 잠금 시 taskId 필드에 기록하는 식별자 */
    private static final String MANUAL_LOCK_BY = "MANUAL";

    /** 포트 류 로케이션 타입 (LocType.code() 와 일치) */
    private static final java.util.Set<String> PORT_LOC_TYPES =
            java.util.Set.of("INBOUND_PORT", "OUTBOUND_PORT", "IN_OUTBOUND_PORT");

    /** DISPATCH_LOCK sentinel (PortService.tryLockForDispatch 의 fallback 값) */
    private static final String DISPATCH_LOCK_SENTINEL = "DISPATCH_LOCK";

    @Autowired
    private InventoryLocationRepository inventoryLocationRepository;

    @Autowired
    private operato.logis.wcs.service.impl.allocation.location.LocationService locationService;

    @Autowired
    private InventoryStockRepository inventoryStockRepository;

    @Autowired
    private InventoryStockStateWriter stockStateWriter;

    @Autowired
    private ShuttleOrderRepository shuttleOrderRepository;

    // [1] 제어 정보 조회 (팝업 오픈 시 호출)

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
            logger.warn("[ Dashboard ][ Control ] unmapped rack cell: rackCellId={}", rackCellId);
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
        TbWcsShuttleOrder activeOrder = this.queryManager.selectBySql(
                orderSql, orderParams, TbWcsShuttleOrder.class
        );

        boolean isPortLoc = PORT_LOC_TYPES.contains(loc.getLocType());
        String portLockTaskId = (isPortLoc && loc.getTaskId() != null) ? loc.getTaskId() : null;
        String portLockBarcode = null;
        Integer portLockOrderStatus = null;
        if (portLockTaskId != null) {
            Object[] holder = fetchPortLockHolder(loc.getLocGroup(), portLockTaskId);
            if (holder != null) {
                portLockBarcode = (String) holder[0];
                portLockOrderStatus = (Integer) holder[1];
            }
        }

        return DashboardControlInfo.builder()
                .locId(loc.getLocId())
                .eqGroupId(loc.getLocGroup())
                .locUseYn(Boolean.TRUE.equals(loc.getIsEnabled()) ? 1 : 0)
                .locLockYn(loc.getTaskId() != null ? 1 : 0)
                .locLockBy(loc.getTaskId())
                .locStatus(loc.getStockId() != null
                        ? LocStatus.OCCUPIED.codeAsIntOrNull()
                        : LocStatus.EMPTY.codeAsIntOrNull())
                .activeOrderKey(activeOrder != null ? activeOrder.getOrderKey() : null)
                .activeOrderStatus(activeOrder != null ? activeOrder.getOrderStatus() : null)
                .activeOrderType(activeOrder != null ? activeOrder.getOrderType() : null)
                .portLockTaskId(portLockTaskId)
                .portLockBarcode(portLockBarcode)
                .portLockOrderStatus(portLockOrderStatus)
                .inventory(inventoryItems)
                .build();
    }

    /**
     * 포트 락 holder 셔틀 오더 조회.
     * task_id 는 PortService.tryLockForDispatch 에서 parent host key 또는 "DISPATCH_LOCK" 으로 설정됨.
     * order_key 우선, 못 찾으면 host_order_key 로 fallback.
     *
     * @return [barcode(String), orderStatus(Integer)] — sentinel 이거나 매칭 오더 없으면 null
     */
    @SuppressWarnings("unchecked")
    private Object[] fetchPortLockHolder(String eqGroupId, String taskId) {
        if (!StringUtils.hasText(taskId) || DISPATCH_LOCK_SENTINEL.equals(taskId)
                || MANUAL_LOCK_BY.equals(taskId)) {
            return null;
        }
        String sql = """
                SELECT barcode, order_status
                  FROM tb_wcs_shuttle_order
                 WHERE eq_group_id = :eqGroupId
                   AND (order_key = :taskId OR host_order_key = :taskId)
                   AND order_status = :arrivedStatus
                 ORDER BY created_at DESC
                """;
        Map<String, Object> params = ValueUtil.newMap("eqGroupId,taskId,arrivedStatus", eqGroupId, taskId,ShuttleOrderStatus.ARRIVED.code());
        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 1);
        if (rows == null || rows.isEmpty()) return null;
        Map row = rows.get(0);
        Object barcode = row.get("barcode");
        Object status = row.get("order_status");
        return new Object[]{
                barcode != null ? barcode.toString() : null,
                status instanceof Number ? ((Number) status).intValue() : null
        };
    }

    /**
     * 설비 제어 정보 조회 (CONVEYOR / LIFTER / SHUTTLE — 비랙 타입)
     */
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

    // [2] Use/Disable 제어 (설비 가동/비가동)

    /**
     * 랙 로케이션 사용 여부 토글
     */
    @Transactional(rollbackFor = Exception.class)
    public DashboardControlResponse toggleLocUse(String eqGroupId, String locId) {
        ExtTbInventoryLocation loc = inventoryLocationRepository.findByEqGroupIdAndLocId(eqGroupId, locId);
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

        logger.info("[ Dashboard ][ Control ] loc use toggled: eqGroupId={}, locId={}, isEnabled={}", eqGroupId, locId, newIsEnabled);
        return DashboardControlResponse.ok(actionMsg,
                Map.of("locUseYn", newIsEnabled ? 1 : 0));
    }

    /**
     * 설비 마스터 사용 여부 토글 (CONVEYOR / LIFTER / SHUTTLE)
     */
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
        logger.info("[ Dashboard ][ Control ] equipment use toggled: eqType={}, eqId={}, useYn={}", eqType, eqId, newUseYn);
        return DashboardControlResponse.ok(msg, Map.of("eqUseYn", newUseYn));
    }

    // [3] 수동 Lock/Unlock 제어

    /**
     * 로케이션 수동 잠금 (taskId = "MANUAL" 세팅)
     */
    @Transactional(rollbackFor = Exception.class)
    public DashboardControlResponse manualLock(String eqGroupId, String locId) {
        ExtTbInventoryLocation loc = inventoryLocationRepository.findByEqGroupIdAndLocId(eqGroupId, locId);
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

        boolean locked = inventoryLocationRepository.lock(eqGroupId, locId, MANUAL_LOCK_BY);
        if (!locked) {
            return DashboardControlResponse.fail("잠금 처리에 실패했습니다. 다시 시도해주세요.");
        }

        logger.info("[ Dashboard ][ Control ] manual lock: eqGroupId={}, locId={}", eqGroupId, locId);
        return DashboardControlResponse.ok("로케이션 수동 잠금 완료: " + locId,
                Map.of("locLockYn", 1, "locLockBy", MANUAL_LOCK_BY));
    }

    /**
     * 로케이션 수동 잠금 해제 (taskId = NULL)
     */
    @Transactional(rollbackFor = Exception.class)
    public DashboardControlResponse manualUnlock(String eqGroupId, String locId) {
        ExtTbInventoryLocation loc = inventoryLocationRepository.findByEqGroupIdAndLocId(eqGroupId, locId);
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

        locationService.unlock(eqGroupId, locId);

        logger.info("[ Dashboard ][ Control ] manual unlock: eqGroupId={}, locId={}", eqGroupId, locId);
        return DashboardControlResponse.ok("로케이션 수동 잠금 해제 완료: " + locId,
                Map.of("locLockYn", 0));
    }

    // [4] 재고 정합성 제어 (Empty Pick / Double Entry 복구)

    /**
     * 수동 재고 삭제 — 공출고(Empty Pick) 복구
     */
    @Transactional(rollbackFor = Exception.class)
    public DashboardControlResponse deleteInventory(String eqGroupId, String locId) {
        List<ExtTbInventoryStock> invList = inventoryStockRepository.findByEqGroupIdAndLocId(eqGroupId, locId);

        if (invList == null || invList.isEmpty()) {
            return DashboardControlResponse.fail("삭제할 재고가 없습니다. (이미 EMPTY 상태)");
        }

        // 물리삭제 대신 라인별 비활성 (audit, 되돌림 가능 / StateWriter 단일창구 경유)
        int disabled = 0;
        for (ExtTbInventoryStock inv : invList) {
            disabled += stockStateWriter.disableStock(inv.getId(), "DASHBOARD_EMPTY_PICK_DISABLE");
        }

        // 로케이션 stockId 초기화 (EMPTY 상태로 복원, audit 경유)
        inventoryLocationRepository.clearStockId(eqGroupId, locId);

        logger.warn("[ Dashboard ][ Control ] inventory disabled (empty pick): eqGroupId={}, locId={}, count={}", eqGroupId, locId, disabled);
        return DashboardControlResponse.ok(
                "재고 " + disabled + "건 비활성 완료. 로케이션 상태 → EMPTY");
    }

    /**
     * 수동 재고 생성 — 이중입고(Double Entry) 복구
     */
    @Transactional(rollbackFor = Exception.class)
    public DashboardControlResponse createInventory(
            String eqGroupId, String locId, String skuCode, String palletId, int qty, String ownerCode) {

        if (!StringUtils.hasText(skuCode) || qty <= 0) {
            return DashboardControlResponse.fail("SKU 코드와 수량(1 이상)은 필수입니다.");
        }

        String stockId = StringUtils.hasText(palletId) ? palletId : locId;
        String owner = StringUtils.hasText(ownerCode) ? ownerCode : "";

        // raw insert 대신 단일 생성 창구(audit + status=IDLE/type=NORMAL/enabled/inbDatetime 일관)
        ExtTbInventoryStock inv = inventoryStockRepository.createOrIncreaseQty(eqGroupId, stockId, owner, skuCode, "", qty);

        // 로케이션 stockId 갱신 (OCCUPIED 상태, audit 경유)
        inventoryLocationRepository.updateStockId(eqGroupId, locId, stockId);

        logger.warn("[ Dashboard ][ Control ] inventory created (double entry): eqGroupId={}, locId={}, skuCode={}, stockId={}, qty={}",
                eqGroupId, locId, skuCode, stockId, qty);
        return DashboardControlResponse.ok("재고 수동 생성 완료. 로케이션 상태 → OCCUPIED",
                Map.of("inventoryId", inv.getId()));
    }

    /**
     * 라인 단위 수량 보정 — 채취/박스출고 오기입 보정.
     * newQty=0 이면 비활성 처리하고, 셀에 활성 라인이 없으면 위치 EMPTY 복원.
     * 이 한 동작이 수량보정 + 이중입고 라인 비활성 + 공출고 부분/전량 차감을 모두 커버한다.
     */
    @Transactional(rollbackFor = Exception.class)
    public DashboardControlResponse adjustInventoryQty(
            String eqGroupId, String locId, String stockRowId, int newQty, String comment) {
        // 입력 검증
        if (newQty < 0) {
            return DashboardControlResponse.fail("수량은 0 이상이어야 합니다.");
        }
        ExtTbInventoryStock stock = inventoryStockRepository.findById(stockRowId);
        if (stock == null) {
            return DashboardControlResponse.fail("재고를 찾을 수 없습니다: " + stockRowId);
        }
        // 가드 — 진행 중(입출고/이동/예약)인 재고는 보정 금지
        if (!isCorrectableStatus(stock.getStockStatus())) {
            logger.warn("[ Dashboard ][ Control ] adjustQty rejected, in-flight - id={}, status={}", stockRowId, stock.getStockStatus());
            return DashboardControlResponse.fail("진행 중인 재고는 수량 보정할 수 없습니다. (상태=" + stock.getStockStatus() + ")");
        }

        // 수량 절대값 보정 (StateWriter 단일창구 경유, audit)
        String reason = StringUtils.hasText(comment) ? comment : "(사유 미입력)";
        stockStateWriter.adjustStockQty(stockRowId, newQty, reason);

        // 0 이면 비활성 + 셀에 활성 라인이 없으면 EMPTY 복원
        if (newQty == 0) {
            stockStateWriter.disableStock(stockRowId, reason);
            List<ExtTbInventoryStock> remain = inventoryStockRepository.findByEqGroupIdAndLocId(eqGroupId, locId);
            boolean anyEnabled = remain != null && remain.stream()
                    .anyMatch(s -> Boolean.TRUE.equals(s.getIsEnabled()) && s.getItemQty() != null && s.getItemQty() > 0);
            if (!anyEnabled) inventoryLocationRepository.clearStockId(eqGroupId, locId);
        }

        logger.warn("[ Dashboard ][ Control ] adjustQty - eqGroupId={}, locId={}, id={}, newQty={}", eqGroupId, locId, stockRowId, newQty);
        return DashboardControlResponse.ok("재고 수량 보정 완료: " + newQty + " EA",
                Map.of("inventoryId", stockRowId, "itemQty", newQty));
    }

    /** 보정 허용 상태 — 가용(IDLE=0) / 보류(HOLD=7) 만. 진행 중 재고는 보정 불가. */
    private boolean isCorrectableStatus(Integer status) {
        return status != null && (status == StockStatus.IDLE.value() || status == StockStatus.HOLD.value());
    }

    // [5] 작업(Order) 흐름 제어

    /**
     * 작업 강제 종료 (Force Complete)
     */
    @Transactional(rollbackFor = Exception.class)
    public DashboardControlResponse forceCompleteOrder(String orderKey) {
        TbWcsShuttleOrder order = shuttleOrderRepository.findByOrderKey(orderKey);
        if (order == null) {
            return DashboardControlResponse.fail("작업을 찾을 수 없습니다: " + orderKey);
        }

        if (ShuttleOrderStatus.isFinalStatus(order.getOrderStatus())) {
            return DashboardControlResponse.fail(
                    "이미 종료된 작업입니다. (현재 상태: " + order.getOrderStatus() + ")");
        }

        order.setOrderStatus(ShuttleOrderStatus.COMPLETED.codeAsIntOrNull());
        order.setRemark("[수동 강제완료] " + new Date());
        shuttleOrderRepository.update(order,"orderStatus","remark");

        // 로케이션 잠금(taskId) 해제
        unlockOrderLocations(order, true);

        logger.info("[ Dashboard ][ Control ] force complete: orderKey={}", orderKey);
        return DashboardControlResponse.ok("작업 강제 완료 처리됨: " + orderKey);
    }

    /**
     * 작업 취소 (Cancel)
     */
    @Transactional(rollbackFor = Exception.class)
    public DashboardControlResponse cancelOrder(String orderKey, String reason) {
        TbWcsShuttleOrder order = shuttleOrderRepository.findByOrderKey(orderKey);
        if (order == null) {
            return DashboardControlResponse.fail("작업을 찾을 수 없습니다: " + orderKey);
        }

        if (ShuttleOrderStatus.isFinalStatus(order.getOrderStatus())) {
            return DashboardControlResponse.fail(
                    "이미 종료된 작업입니다. (현재 상태: " + order.getOrderStatus() + ")");
        }

        order.setOrderStatus(ShuttleOrderStatus.CANCELLED.codeAsIntOrNull());
        order.setRemark("[수동 취소] " + (StringUtils.hasText(reason) ? reason : "사유 없음") + " | " + new Date());
        shuttleOrderRepository.update(order,"orderStatus","remark");

        // 로케이션 잠금(taskId) 해제
        unlockOrderLocations(order, false);

        logger.info("[ Dashboard ][ Control ] order cancel: orderKey={}, reason={}", orderKey, reason);
        return DashboardControlResponse.ok("작업 취소 완료: " + orderKey);
    }

    /**
     * 작업 재개 (Resume)
     */
    @Transactional(rollbackFor = Exception.class)
    public DashboardControlResponse resumeOrder(String orderKey) {
        TbWcsShuttleOrder order = shuttleOrderRepository.findByOrderKey(orderKey);
        if (order == null) {
            return DashboardControlResponse.fail("작업을 찾을 수 없습니다: " + orderKey);
        }

        if (ShuttleOrderStatus.isFinalStatus(order.getOrderStatus())) {
            return DashboardControlResponse.fail(
                    "이미 종료된 작업은 재개할 수 없습니다. (현재 상태: " + order.getOrderStatus() + ")");
        }

        int currentStatus = order.getOrderStatus();
        if (currentStatus < 100) {
            return DashboardControlResponse.fail(
                    "현재 진행 중인 작업입니다. 에러 상태의 작업만 재개할 수 있습니다. (현재 상태: " + currentStatus + ")");
        }

        order.setOrderStatus(ShuttleOrderStatus.SENT.codeAsIntOrNull());
        order.setRemark("[수동 재개] " + new Date());
        shuttleOrderRepository.update(order,"orderStatus");

        logger.info("[ Dashboard ][ Control ] order resume: orderKey={}, prevStatus={}", orderKey, currentStatus);
        return DashboardControlResponse.ok("작업 재개 요청 완료. ECS 재전송 대기 중: " + orderKey);
    }

    // [6] 내부 헬퍼 메서드

    /**
     * 해당 로케이션에 재고가 존재하는지 확인
     */
    private boolean hasInventory(String eqGroupId, String locId) {
        List<ExtTbInventoryStock> invList = inventoryStockRepository.findByEqGroupIdAndLocId(eqGroupId, locId);
        return invList != null && !invList.isEmpty();
    }

    /**
     * 랙 셀의 재고 상세 조회 (팝업 전용).
     * tb_inventory_location + tb_inventory_stock 2-way JOIN.
     *
     * 주의사항:
     * - JOIN 키: stk.stock_id = loc.stock_id (stk.id 가 아님)
     *   stk.id 는 UUID PK, loc.stock_id 는 팔레트 그룹 키
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
                stk.stock_type        AS stock_type,
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
                        .stockType(RowConverter.toStringValue(row.get("stock_type")))
                        .stockId(RowConverter.toStringValue(row.get("stock_id")))
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
        if (v instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean fetchEquipmentUseYn(String eqType, String eqId) {
        String upper = eqType.toUpperCase();
        if ("SHUTTLE".equals(upper)) {
            TbEqCarMst car = this.queryManager.select(TbEqCarMst.class, eqId);
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

    /**
     * 오더 취소/완료 시 로케이션 잠금(taskId) 해제
     */
    private void unlockOrderLocations(TbWcsShuttleOrder order, boolean isComplete) {
        String eqGroupId = order.getEqGroupId();
        String fromLoc = order.getFromLocCode();
        String toLoc = order.getToLocCode();
        String orderType = order.getOrderType();

        if (!StringUtils.hasText(eqGroupId)) return;

        try {
            if ("INBOUND".equalsIgnoreCase(orderType)) {
                if (StringUtils.hasText(toLoc)) locationService.unlock(eqGroupId, toLoc);

            } else if ("OUTBOUND".equalsIgnoreCase(orderType)) {
                if (StringUtils.hasText(fromLoc)) locationService.unlock(eqGroupId, fromLoc);

            } else if ("MOVE".equalsIgnoreCase(orderType)) {
                if (StringUtils.hasText(fromLoc)) locationService.unlock(eqGroupId, fromLoc);
                if (StringUtils.hasText(toLoc)) locationService.unlock(eqGroupId, toLoc);
            }
        } catch (Exception e) {
            logger.error("[ Dashboard ][ Control ] unlock failed: orderKey={}, eqGroupId={}",
                    order.getOrderKey(), eqGroupId, e);
        }
    }
}
