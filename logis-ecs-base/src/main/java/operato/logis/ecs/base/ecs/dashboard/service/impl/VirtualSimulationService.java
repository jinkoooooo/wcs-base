package operato.logis.ecs.base.ecs.dashboard.service.impl;

import jakarta.annotation.PreDestroy;
import operato.logis.ecs.base.ecs.dashboard.entity.TbEcs2dItem;
import operato.logis.ecs.base.ecs.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 가상 운영 시뮬레이션 서비스 (확정된 tspg4way.entity.* 기준)
 *
 * 최종 규칙(중요) - RACK 모델 수정 반영
 * - tb_eq_mst.id === (tb_eq_car_mst.eq_id / tb_eq_cv_mst.eq_id / tb_eq_rack_mst.eq_id)
 * - CAR/CV는 기존과 동일: tb_eq_mst.id == tb_eq_*_mst.eq_id
 * - RACK은 "층당 1개 랙"만 tb_eq_mst에 생성:
 * - tb_eq_mst.id = RACK_{eqGroupId}_F{floor}
 * - tb_eq_rack_mst.eq_id = (해당 층 rackId)로 고정
 * - tb_eq_rack_mst.id = 셀(=cellId)로 사용: RACKCELL_{eqGroupId}_F{floor}_R{row}_C{col}
 * - 즉, 레이아웃 매핑(real_eq_id)은 "셀 ID(tb_eq_rack_mst.id)"를 바라본다.
 *
 * - Seed:
 * - tb_eq_mst: CAR / CONVEYOR / RACK(층 단위) 생성
 * - tb_eq_car_mst / tb_eq_cv_mst / tb_eq_rack_mst 생성
 * (CAR/CV: eq_id = tb_eq_mst.id, RACK: eq_id = rackId, rack row는 id=cellId로 구분)
 *
 * - Layout auto-map:
 * - tb_ecs_2d_item.real_eq_id / real_eq_type 채움
 * (RACK은 cellId를 매핑)
 *
 * - Simulation:
 * - 셔틀 이동(500ms): tb_eq_car_mst row/bay/level/status/cargo_yn/battery_status 업데이트
 * - 작업 생성(5s): tb_wcs_shuttle_order insert + 일정 시간 후 완료 처리
 * - WS 브로드캐스트:
 * /topic/shuttle/positions/{lcId},
 * /topic/shuttle/cargos/{lcId},
 * /topic/shuttle/wcs/order/{lcId}
 */
@Service
public class VirtualSimulationService {

    private static final Logger logger = LoggerFactory.getLogger(VirtualSimulationService.class);

    // ===== Status Codes (int) =====
    private static final int CAR_STATUS_IDLE = 0;
    private static final int CAR_STATUS_MOVING = 1;

    private static final int ORDER_STATUS_CREATED = 10;
    private static final int ORDER_STATUS_COMPLETED = 90;
    private static final int ORDER_STATUS_CANCELLED = 99;

    // 화물 상태 코드
    private static final int CARGO_STATUS_PENDING = 0;   // 대기
    private static final int CARGO_STATUS_MOVING = 1;    // 이동중
    private static final int CARGO_STATUS_STORED = 2;    // 보관중
    private static final int CARGO_STATUS_PICKING = 3;   // 피킹중
    private static final int CARGO_STATUS_ERROR = 9;     // 에러

    private IQueryManager queryManager;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private TbEcs2dItemService layoutService;

    private ScheduledExecutorService scheduler;
    private volatile boolean isSimulating = false;

    // 시뮬레이션 상태
    private final Map<String, VirtualShuttle> virtualShuttles = new ConcurrentHashMap<>();
    private final Map<String, VirtualConveyor> virtualConveyors = new ConcurrentHashMap<>();
    private final Map<String, VirtualCargo> virtualCargos = new ConcurrentHashMap<>();
    private final List<VirtualOrder> virtualOrders = new CopyOnWriteArrayList<>();
    private final AtomicLong orderSequence = new AtomicLong(1);
    private final AtomicLong cargoSequence = new AtomicLong(1);

    // 레이아웃 좌표 캐시: cellId(realEqId) -> LayoutPos(centerX, centerY, floor)
    private final Map<String, LayoutPos> layoutPositionCache = new ConcurrentHashMap<>();
    private final Map<Integer, List<String>> rackCellIdsByFloor = new ConcurrentHashMap<>();
    private final Map<Integer, List<String>> conveyorIdsByFloor = new ConcurrentHashMap<>();

    private String currentLcId;
    private String currentEqGroupId;

    private IQueryManager getQueryManager() {
        if (this.queryManager == null) {
            this.queryManager = BeanUtil.get(IQueryManager.class);
        }
        return this.queryManager;
    }

    @PreDestroy
    public void destroy() {
        stopSimulation();
    }

    // ============================================
    // 가상 설비 Seed 데이터 생성
    // ============================================

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generateVirtualEquipment(
            String lcId,
            String eqGroupId,
            int floors,
            int shuttlesPerFloor,
            int rackRows,
            int rackCols) {

        logger.info("Generating virtual equipment for lcId: {}, eqGroupId: {}", lcId, eqGroupId);

        this.currentLcId = lcId;
        this.currentEqGroupId = eqGroupId;

        ensureEqGroupMstExists(eqGroupId);

        // 기존 가상 데이터 삭제
        clearVirtualData(eqGroupId);

        int carCount = 0;
        int rackCellCount = 0;
        int cvCount = 0;
        int rackCount = 0;

        // (선택) eq_mst에 매핑용 plc_id 값
        String plcId = "PLC_" + eqGroupId;

        // 0) 층별 RACK(층 단위 1개) - tb_eq_mst에만 생성
        for (int floor = 1; floor <= floors; floor++) {
            String rackEqId = buildRackEqId(eqGroupId, floor);
            String rackName = String.format("랙 %d층", floor);
            ensureEqMstExists(rackEqId, eqGroupId, rackName, "RACK", plcId);
            rackCount++;
        }

        // 1) 셔틀카 생성(층별) - tb_eq_car_mst
        for (int floor = 1; floor <= floors; floor++) {
            for (int i = 1; i <= shuttlesPerFloor; i++) {

                String carEqId = String.format("CAR_%s_F%d_%02d", eqGroupId, floor, i);
                String carName = String.format("셔틀카 %d층-%d", floor, i);

                // tb_eq_mst
                ensureEqMstExists(carEqId, eqGroupId, carName, "CAR", plcId);

                // tb_eq_car_mst (eq_id = tb_eq_mst.id)
                TbEqCraneMst car = new TbEqCraneMst();
                car.setEqId(carEqId);
                car.setType("4WAY_SHUTTLE");

                //car.setRow(1);
                car.setBay(i);
                car.setLevel(floor);

                car.setRackId(null);
                car.setAutoYn(false);
                car.setPlcCmdId(0);
                car.setStatus(CAR_STATUS_IDLE);
                //car.setBatteryStatus(100);
                car.setCargoYn(false);
                //car.setMinRow(1);
                //car.setMaxRow(Math.max(rackRows, 1));
                car.setErrorId(null);
                car.setErrorDesc(null);
                car.setUseYn(true);

                this.getQueryManager().insert(car);
                carCount++;
            }
        }

        // 2) 랙 셀 생성(층별) - tb_eq_rack_mst
        // ✅ tb_eq_rack_mst.eq_id = (층 rackId)
        // ✅ tb_eq_rack_mst.id    = cellId (실제 셀 식별자)
        for (int floor = 1; floor <= floors; floor++) {
            String rackEqId = buildRackEqId(eqGroupId, floor);

            for (int row = 1; row <= rackRows; row++) {
                for (int col = 1; col <= rackCols; col++) {

                    String cellId = buildRackCellId(eqGroupId, floor, row, col);

                    TbEqRackMst rackCell = new TbEqRackMst();
                    rackCell.setRackId(cellId);          // ✅ 셀 식별자
                    rackCell.setEqId(rackEqId);      // ✅ 층 rackId 참조
                    // rackCell.setType("RACK_CELL");
                    //rackCell.setRow(row);
                    rackCell.setBay(col);
                    rackCell.setLevel(floor);

                    // 랜덤 재고
                    if (Math.random() < 0.30) {
                        rackCell.setSkuId("SKU_" + String.format("%06d", (int) (Math.random() * 100000)));
                        rackCell.setSkuQty((int) (Math.random() * 100) + 1);
                    } else {
                        rackCell.setSkuId(null);
                        rackCell.setSkuQty(0);
                    }

                    rackCell.setStatus(0);
                    rackCell.setErrorId(null);
                    rackCell.setErrorDesc(null);
                    rackCell.setUseYn(true);

                    this.getQueryManager().insert(rackCell);
                    rackCellCount++;
                }
            }
        }

        // 3) 컨베이어 생성(6개) - tb_eq_cv_mst
        String[] cvTypes = { "INBOUND", "OUTBOUND", "TRANSFER", "BUFFER", "INBOUND2", "OUTBOUND2" };
        for (int i = 0; i < 6; i++) {

            String cvEqId = String.format("CV_%s_%02d", eqGroupId, i + 1);
            String cvName = String.format("컨베이어 %d (%s)", i + 1, cvTypes[i]);

            // tb_eq_mst
            ensureEqMstExists(cvEqId, eqGroupId, cvName, "CONVEYOR", plcId);

            // tb_eq_cv_mst (eq_id = tb_eq_mst.id)
            TbEqCvMst cv = new TbEqCvMst();
            cv.setEqId(cvEqId);
            // cv.setType(cvTypes[i]);
            cv.setAutoYn(true);
            cv.setCargoYn(false);
            //cv.setLevel(1);
            cv.setPlcCmdId(0);
            cv.setStatus(0);
            cv.setErrorId(null);
            cv.setErrorDesc(null);
            cv.setUseYn(true);

            this.getQueryManager().insert(cv);
            cvCount++;
        }

        logger.info("Generated virtual equipment: racks={}, cars={}, rackCells={}, conveyors={}",
                rackCount, carCount, rackCellCount, cvCount);

        return ValueUtil.newMap(
                "success,rackCount,carCount,rackCellCount,cvCount",
                true, rackCount, carCount, rackCellCount, cvCount
        );
    }

    private String buildRackEqId(String eqGroupId, int floor) {
        return String.format("RACK_%s_F%d", eqGroupId, floor);
    }

    private String buildRackCellId(String eqGroupId, int floor, int row, int col) {
        return String.format("RACKCELL_%s_F%d_R%02d_C%02d", eqGroupId, floor, row, col);
    }

    private void ensureEqGroupMstExists(String eqGroupId) {
        TbEqGroupMst existing = this.getQueryManager()
                .selectByCondition(TbEqGroupMst.class, ValueUtil.newMap("id", eqGroupId));

        if (!ValueUtil.isEmpty(existing)) return;

        TbEqGroupMst g = new TbEqGroupMst();
        g.setId(eqGroupId);
        g.setName(eqGroupId);
        g.setType("TSPG_AMBIENT");

        this.getQueryManager().insert(g);
        logger.info("Inserted TbEqGroupMst: id={}, name={}, type={}", g.getId(), g.getName(), g.getType());
    }

    /**
     * tb_eq_mst.id = eqId
     * (CAR/CV/RACK(층단위))만 tb_eq_mst에 들어감
     */
    private void ensureEqMstExists(String eqId, String eqGroupId, String name, String type, String plcId) {
        if (ValueUtil.isEmpty(eqId)) return;

        TbEqMst existing = this.getQueryManager().selectByCondition(TbEqMst.class, ValueUtil.newMap("id", eqId));
        if (!ValueUtil.isEmpty(existing)) return;

        TbEqMst eq = new TbEqMst();
        eq.setId(eqId);
        eq.setEqGroupId(eqGroupId);
        eq.setName(name);
        // eq.setType(type);
        eq.setPlcId(plcId);

        this.getQueryManager().insert(eq);
    }

    // ============================================
    // Layout ↔ Virtual Auto Mapping
    // ============================================

    /**
     * Layout과 가상 설비 자동 매핑
     * - SHUTTLE -> tb_eq_car_mst.eq_id
     * - RACK    -> tb_eq_rack_mst.id (cellId)
     * - CONVEYOR-> tb_eq_cv_mst.eq_id
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> autoMapLayoutsToVirtualEquipment(String lcId, String pageId) {
        logger.info("Auto-mapping layouts to virtual equipment for pageId: {}", pageId);

        List<TbEcs2dItem> layouts = layoutService.getLayoutsByPageId(lcId, pageId);
        int mappedCount = 0;

        // 현재 페이지 층 조회
        String floorSql = "SELECT floor_level FROM tb_ecs_2d_page WHERE id = :pageId";
        Integer floorLevel = this.getQueryManager().selectBySql(
                floorSql,
                ValueUtil.newMap("pageId", pageId),
                Integer.class
        );
        int floor = floorLevel != null ? floorLevel : 1;

        String eqGroupId = ValueUtil.isEmpty(this.currentEqGroupId) ? "UNKNOWN" : this.currentEqGroupId;
        String pattern = "%" + eqGroupId + "%";

        // CAR 목록(eq_id)
        String carListSql = """
                SELECT eq_id
                  FROM tb_eq_car_mst
                 WHERE level = :floor
                   AND eq_id LIKE :pattern
                 ORDER BY eq_id
                """;
        List<String> carEqIds = this.getQueryManager().selectListBySql(
                carListSql,
                ValueUtil.newMap("floor,pattern", floor, pattern),
                String.class, 0, 0
        );

        // RACK CELL 목록(id = cellId)  ※ eq_id는 rackId(층 단위)라서 floor로만 필터
        String rackCellListSql = """
                SELECT id
                  FROM tb_eq_rack_mst
                 WHERE level = :floor
                   AND id LIKE :pattern
                 ORDER BY id
                """;
        List<String> rackCellIds = this.getQueryManager().selectListBySql(
                rackCellListSql,
                ValueUtil.newMap("floor,pattern", floor, pattern),
                String.class, 0, 0
        );

        // CV 목록(eq_id)
        String cvListSql = """
                SELECT eq_id
                  FROM tb_eq_cv_mst
                 WHERE eq_id LIKE :pattern
                 ORDER BY eq_id
                """;
        List<String> cvEqIds = this.getQueryManager().selectListBySql(
                cvListSql,
                ValueUtil.newMap("pattern", pattern),
                String.class, 0, 0
        );

        int shuttleIndex = 0;
        int rackIndex = 0;
        int cvIndex = 0;

        for (TbEcs2dItem layout : layouts) {
            String typeCode = layout.getEquipmentTypeCode();
            if (typeCode == null) continue;

            String realEqId = null;
            String realEqType = null;

            switch (typeCode) {
                case "SHUTTLE" -> {
                    if (shuttleIndex < carEqIds.size()) {
                        realEqId = carEqIds.get(shuttleIndex++);
                        realEqType = "CAR";
                    }
                }
                case "RACK" -> {
                    if (rackIndex < rackCellIds.size()) {
                        realEqId = rackCellIds.get(rackIndex++); // ✅ cellId
                        realEqType = "RACK";
                    }
                }
                case "CONVEYOR" -> {
                    if (cvIndex < cvEqIds.size()) {
                        realEqId = cvEqIds.get(cvIndex++);
                        realEqType = "CONVEYOR";
                    }
                }
                case "LIFTER" -> {
                    realEqId = String.format("LIFTER_%s_F%d", eqGroupId, floor);
                    realEqType = "LIFTER";
                }
            }

            if (realEqId != null) {
                layout.setRealEqId(realEqId);
                layout.setRealEqType(realEqType);
                this.getQueryManager().update(layout, "realEqId,realEqType");
                mappedCount++;
            }
        }

        logger.info("Auto-mapped {} layouts to virtual equipment", mappedCount);
        return ValueUtil.newMap("success,mappedCount", true, mappedCount);
    }

    // ============================================
    // 가상 데이터 삭제
    // ============================================

    @Transactional(rollbackFor = Exception.class)
    public void clearVirtualData(String eqGroupId) {
        if (ValueUtil.isEmpty(eqGroupId)) return;

        String pattern = "%" + eqGroupId + "%";

        // car
        this.getQueryManager().executeBySql(
                "DELETE FROM tb_eq_car_mst WHERE eq_id LIKE :pattern",
                ValueUtil.newMap("pattern", pattern)
        );

        // rack cell (cell rows)
        this.getQueryManager().executeBySql(
                "DELETE FROM tb_eq_rack_mst WHERE id LIKE :pattern OR eq_id LIKE :pattern",
                ValueUtil.newMap("pattern", pattern)
        );

        // conveyor
        this.getQueryManager().executeBySql(
                "DELETE FROM tb_eq_cv_mst WHERE eq_id LIKE :pattern",
                ValueUtil.newMap("pattern", pattern)
        );

        // orders
        this.getQueryManager().executeBySql(
                "DELETE FROM tb_wcs_shuttle_order WHERE order_key LIKE :pattern",
                ValueUtil.newMap("pattern", pattern)
        );

        // eq mst (eq_group_id 기준 + id 패턴 제한)
        this.getQueryManager().executeBySql(
                "DELETE FROM tb_eq_mst WHERE eq_group_id = :eqGroupId AND id LIKE :pattern",
                ValueUtil.newMap("eqGroupId,pattern", eqGroupId, pattern)
        );

        logger.info("Cleared virtual data for eqGroupId: {}", eqGroupId);
    }

    // ============================================
    // 실시간 시뮬레이션
    // ============================================

    public synchronized void startSimulation(String lcId, String eqGroupId) {
        if (isSimulating) {
            logger.warn("Simulation already running");
            return;
        }

        logger.info("========================================");
        logger.info("[Simulation] Starting for lcId: '{}', eqGroupId: '{}'", lcId, eqGroupId);
        logger.info("========================================");

        this.currentLcId = lcId;
        this.currentEqGroupId = eqGroupId;

        initializeLayoutPositionCache(lcId, eqGroupId);
        initializeVirtualShuttles(eqGroupId);

        logger.info("[Simulation] Initialized {} shuttles, {} layout positions",
                virtualShuttles.size(), layoutPositionCache.size());
        logger.info("[Simulation] WebSocket topics will be:");
        logger.info("  - /topic/shuttle/positions/{}", lcId);
        logger.info("  - /topic/shuttle/car/status/{}", eqGroupId);
        logger.info("  - /topic/shuttle/cargos/{}", lcId);

        if (virtualShuttles.isEmpty()) {
            logger.error("[Simulation] WARNING: No shuttles found! Check tb_eq_car_mst for eqGroupId pattern: %{}%", eqGroupId);
        }

        isSimulating = true;
        scheduler = Executors.newScheduledThreadPool(2);

        scheduler.scheduleAtFixedRate(() -> {
            if (!isSimulating) return;
            try {
                simulateShuttleMovement(lcId, eqGroupId);
            } catch (Exception e) {
                logger.error("Shuttle simulation error: {}", e.getMessage(), e);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);

        scheduler.scheduleAtFixedRate(() -> {
            if (!isSimulating) return;
            try {
                simulateOrderCreation(lcId, eqGroupId);
            } catch (Exception e) {
                logger.error("Order simulation error: {}", e.getMessage(), e);
            }
        }, 2000, 5000, TimeUnit.MILLISECONDS);

        logger.info("Virtual simulation started");
    }

    public synchronized void stopSimulation() {
        if (!isSimulating) return;

        isSimulating = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }

        virtualShuttles.clear();
        virtualOrders.clear();
        virtualCargos.clear();

        logger.info("Virtual simulation stopped");
    }

    public boolean isSimulating() {
        return isSimulating;
    }

    /**
     * 레이아웃 좌표 캐시 초기화
     * RACK 타입 레이아웃의 realEqId(cellId) -> (centerX, centerY, floor) 매핑
     */
    private void initializeLayoutPositionCache(String lcId, String eqGroupId) {
        layoutPositionCache.clear();
        rackCellIdsByFloor.clear();
        conveyorIdsByFloor.clear();

        // RACK과 CONVEYOR 모두 로드 (셔틀이 두 곳 모두 이동 가능)
        String sql = """
                SELECT
                    sel.real_eq_id,
                    sel.pos_x,
                    sel.pos_y,
                    sel.width,
                    sel.height,
                    sel.equipment_type_code,
                    slp.floor_level
                FROM tb_ecs_2d_item sel
                INNER JOIN tb_ecs_2d_page slp ON sel.page_id = slp.id
                WHERE slp.lc_id = :lcId
                  AND slp.eq_group_id = :eqGroupId
                  AND sel.real_eq_id IS NOT NULL
                  AND sel.equipment_type_code IN ('RACK', 'CONVEYOR')
                """;

        Map<String, Object> params = ValueUtil.newMap("lcId,eqGroupId", lcId, eqGroupId);
        List<Map> rows = this.getQueryManager().selectListBySql(sql, params, Map.class, 0, 0);

        int rackCount = 0;
        int conveyorCount = 0;

        for (Map row : rows) {
            String cellId = (String) row.get("real_eq_id");
            if (cellId == null) continue;

            int posX = row.get("pos_x") != null ? ((Number) row.get("pos_x")).intValue() : 0;
            int posY = row.get("pos_y") != null ? ((Number) row.get("pos_y")).intValue() : 0;
            int width = row.get("width") != null ? ((Number) row.get("width")).intValue() : 50;
            int height = row.get("height") != null ? ((Number) row.get("height")).intValue() : 50;
            int floor = row.get("floor_level") != null ? ((Number) row.get("floor_level")).intValue() : 1;
            String eqTypeCode = (String) row.get("equipment_type_code");

            int centerX = posX + width / 2;
            int centerY = posY + height / 2;

            layoutPositionCache.put(cellId, new LayoutPos(centerX, centerY, floor));

            // 타입별로 분류
            if ("RACK".equals(eqTypeCode)) {
                rackCellIdsByFloor.computeIfAbsent(floor, k -> new ArrayList<>()).add(cellId);
                rackCount++;
            } else if ("CONVEYOR".equals(eqTypeCode)) {
                conveyorIdsByFloor.computeIfAbsent(floor, k -> new ArrayList<>()).add(cellId);
                conveyorCount++;
            }
        }

        for (List<String> list : rackCellIdsByFloor.values()) {
            Collections.shuffle(list);
        }
        for (List<String> list : conveyorIdsByFloor.values()) {
            Collections.shuffle(list);
        }

        logger.info("Initialized layout position cache: {} total ({} rack cells, {} conveyors), floors={}",
                layoutPositionCache.size(),
                rackCount,
                conveyorCount,
                rackCellIdsByFloor.keySet()
        );
    }

    private LayoutPos getLayoutPos(String cellId) {
        return layoutPositionCache.get(cellId);
    }

    private String getRandomRackCellId(int floor) {
        List<String> list = rackCellIdsByFloor.get(floor);
        if (list == null || list.isEmpty()) return null;
        return list.get((int) (Math.random() * list.size()));
    }

    private String getRandomConveyorId(int floor) {
        List<String> list = conveyorIdsByFloor.get(floor);
        if (list == null || list.isEmpty()) return null;
        return list.get((int) (Math.random() * list.size()));
    }

    /** 셔틀이 이동할 다음 위치 선택 (랙 셀 또는 컨베이어) */
    private String getRandomTargetId(int floor, boolean preferConveyor) {
        if (preferConveyor) {
            String cvId = getRandomConveyorId(floor);
            if (cvId != null) return cvId;
        }
        return getRandomRackCellId(floor);
    }

    private void initializeVirtualShuttles(String eqGroupId) {
        virtualShuttles.clear();

        String sql = "SELECT * FROM tb_eq_car_mst WHERE eq_id LIKE :pattern ORDER BY eq_id";
        Map<String, Object> params = ValueUtil.newMap("pattern", "%" + eqGroupId + "%");
        List<TbEqCraneMst> cars = this.getQueryManager().selectListBySql(sql, params, TbEqCraneMst.class, 0, 0);

        int idx = 0;

        for (TbEqCraneMst car : cars) {
            VirtualShuttle vs = new VirtualShuttle();
            vs.eqId = car.getEqId();
            vs.floor = car.getLevel() > 0 ? car.getLevel() : 1;

            String startCellId = null;
            List<String> floorCells = rackCellIdsByFloor.get(vs.floor);
            if (floorCells != null && !floorCells.isEmpty()) {
                startCellId = floorCells.get(idx % floorCells.size());
            }

            // 레이아웃에 정의된 셀이 없으면 셔틀을 생성하지 않음
            if (startCellId == null) {
                logger.warn("[Shuttle Init] Skipping shuttle {} - no layout cells for floor {}",
                        car.getEqId(), vs.floor);
                continue;
            }

            LayoutPos lp = getLayoutPos(startCellId);
            if (lp == null) {
                logger.warn("[Shuttle Init] Skipping shuttle {} - no position for cell {}",
                        car.getEqId(), startCellId);
                continue;
            }

            vs.posX = lp.x;
            vs.posY = lp.y;
            vs.targetX = lp.x;
            vs.targetY = lp.y;
            vs.currentCellId = startCellId;
            vs.targetCellId = startCellId;

            ParsedCell parsed = ParsedCell.tryParse(startCellId);
            if (parsed != null) {
                vs.row = parsed.row;
                vs.bay = parsed.col;
                vs.floor = parsed.floor > 0 ? parsed.floor : vs.floor;
            } else {
                //vs.row = car.getRow();
                vs.bay = car.getBay();
            }

            vs.statusCode = car.getStatus();
            vs.hasCargo = car.isCargoYn();
            //vs.battery = car.getBatteryStatus() > 0 ? car.getBatteryStatus() : 100;

            virtualShuttles.put(vs.eqId, vs);
            idx++;
        }

        logger.info("Initialized {} virtual shuttles", virtualShuttles.size());
    }

    private void simulateShuttleMovement(String lcId, String eqGroupId) {
        final int MOVE_SPEED = 15;

        for (VirtualShuttle vs : virtualShuttles.values()) {
            boolean moved = false;

            int dx = vs.targetX - vs.posX;
            int dy = vs.targetY - vs.posY;

            if (Math.abs(dx) > MOVE_SPEED) {
                vs.posX += (dx > 0) ? MOVE_SPEED : -MOVE_SPEED;
                moved = true;
            } else if (dx != 0) {
                vs.posX = vs.targetX;
                moved = true;
            }

            if (Math.abs(dy) > MOVE_SPEED) {
                vs.posY += (dy > 0) ? MOVE_SPEED : -MOVE_SPEED;
                moved = true;
            } else if (dy != 0) {
                vs.posY = vs.targetY;
                moved = true;
            }

            if (!moved && vs.statusCode == CAR_STATUS_MOVING) {
                vs.statusCode = CAR_STATUS_IDLE;
                vs.currentCellId = vs.targetCellId;

                ParsedCell parsed = ParsedCell.tryParse(vs.currentCellId);
                if (parsed != null) {
                    vs.row = parsed.row;
                    vs.bay = parsed.col;
                    vs.floor = parsed.floor > 0 ? parsed.floor : vs.floor;
                }

                // hasCargo는 화물 시뮬레이션에서 관리 (무작위 변경 제거)
            }

            if (vs.statusCode == CAR_STATUS_IDLE && Math.random() < 0.15) {
                // 화물이 있으면 랙 셀로, 없으면 컨베이어나 랙 셀로 이동
                boolean preferConveyor = !vs.hasCargo && Math.random() < 0.3;
                String nextCellId = getRandomTargetId(vs.floor, preferConveyor);

                if (nextCellId != null && !nextCellId.equals(vs.currentCellId)) {
                    LayoutPos nextPos = getLayoutPos(nextCellId);
                    if (nextPos != null) {
                        vs.targetX = nextPos.x;
                        vs.targetY = nextPos.y;
                        vs.targetCellId = nextCellId;
                        vs.statusCode = CAR_STATUS_MOVING;
                    }
                }
            }

            if (vs.statusCode == CAR_STATUS_MOVING && vs.battery > 10 && Math.random() < 0.2) {
                vs.battery -= 1;
            }

            // DB 업데이트 (tb_eq_car_mst)
            String updateSql = """
                    UPDATE tb_eq_car_mst
                       SET row = :row,
                           bay = :bay,
                           level = :level,
                           status = :status,
                           cargo_yn = :cargoYn,
                           battery_status = :battery
                     WHERE eq_id = :eqId
                    """;
            Map<String, Object> updateParams = ValueUtil.newMap(
                    "row,bay,level,status,cargoYn,battery,eqId",
                    vs.row, vs.bay, vs.floor, vs.statusCode, vs.hasCargo, vs.battery, vs.eqId
            );
            this.getQueryManager().executeBySql(updateSql, updateParams);
        }

        broadcastShuttlePositions(lcId, eqGroupId);
        simulateCargoMovement(lcId, eqGroupId);
    }

    private void broadcastShuttlePositions(String lcId, String eqGroupId) {
        if (virtualShuttles.isEmpty()) {
            logger.warn("[Broadcast] Skipping - virtualShuttles is empty");
            return;
        }

        List<Map<String, Object>> updates = new ArrayList<>();
        for (VirtualShuttle vs : virtualShuttles.values()) {
            updates.add(ValueUtil.newMap(
                    "eqId,posX,posY,level,statusCode,hasCargo,battery",
                    vs.eqId, vs.posX, vs.posY, vs.floor, vs.statusCode, vs.hasCargo, vs.battery
            ));
        }
        String carStatusTopic = "/topic/shuttle/car/status/" + eqGroupId;
        messagingTemplate.convertAndSend(carStatusTopic, updates);

        List<Map<String, Object>> dashboardUpdates = new ArrayList<>();
        for (VirtualShuttle vs : virtualShuttles.values()) {
            dashboardUpdates.add(ValueUtil.newMap(
                    "equipmentId,equipmentCode,posX,posY,posZ,status,movementStatus,hasCargo,batteryLevel",
                    vs.eqId, vs.eqId,
                    vs.posX, vs.posY, vs.floor,
                    vs.statusCode,
                    (vs.statusCode == CAR_STATUS_MOVING) ? 1 : 2,
                    vs.hasCargo,
                    vs.battery
            ));
        }
        String positionsTopic = "/topic/shuttle/positions/" + lcId;
        messagingTemplate.convertAndSend(positionsTopic, dashboardUpdates);

        // Debug logging - 매 10번째 브로드캐스트마다 로그 출력
        if (System.currentTimeMillis() % 5000 < 500) {
            logger.info("[Broadcast] Sent {} shuttles to topics: {} and {}",
                    dashboardUpdates.size(), carStatusTopic, positionsTopic);
        }
    }

    private void simulateCargoMovement(String lcId, String eqGroupId) {
        // 새 화물 생성 (컨베이어에서 입고 대기) - 반드시 유효한 위치에만 생성
        if (virtualCargos.size() < 15 && Math.random() < 0.03) {
            // 컨베이어 또는 랙 셀 위치 찾기
            String startLocationId = null;
            LayoutPos startPos = null;
            int startFloor = 1;

            // 1순위: 컨베이어
            for (int floor : conveyorIdsByFloor.keySet()) {
                List<String> cvList = conveyorIdsByFloor.get(floor);
                if (cvList != null && !cvList.isEmpty()) {
                    String cvId = cvList.get((int) (Math.random() * cvList.size()));
                    LayoutPos pos = getLayoutPos(cvId);
                    if (pos != null) {
                        startLocationId = cvId;
                        startPos = pos;
                        startFloor = floor;
                        break;
                    }
                }
            }

            // 2순위: 랙 셀 (컨베이어가 없으면)
            if (startPos == null) {
                for (int floor : rackCellIdsByFloor.keySet()) {
                    List<String> rackList = rackCellIdsByFloor.get(floor);
                    if (rackList != null && !rackList.isEmpty()) {
                        String cellId = rackList.get((int) (Math.random() * rackList.size()));
                        LayoutPos pos = getLayoutPos(cellId);
                        if (pos != null) {
                            startLocationId = cellId;
                            startPos = pos;
                            startFloor = floor;
                            break;
                        }
                    }
                }
            }

            // 유효한 위치가 없으면 화물 생성하지 않음
            if (startPos == null) {
                return;
            }

            long seq = cargoSequence.getAndIncrement();
            String cargoId = String.format("CARGO_%s_%06d", eqGroupId, seq);

            VirtualCargo cargo = new VirtualCargo();
            cargo.cargoId = cargoId;
            cargo.barcode = String.format("BOX%08d", seq);
            cargo.statusCode = CARGO_STATUS_PENDING;
            cargo.carriedByShuttleId = null;
            cargo.storedCellId = null;
            cargo.floor = startFloor;
            cargo.posX = startPos.x;
            cargo.posY = startPos.y;

            virtualCargos.put(cargoId, cargo);
        }

        List<Map<String, Object>> cargoUpdates = new ArrayList<>();

        for (VirtualCargo cargo : virtualCargos.values()) {
            // 1. 셔틀에 실려있는 화물 - 셔틀 위치 따라감
            if (cargo.carriedByShuttleId != null) {
                VirtualShuttle shuttle = virtualShuttles.get(cargo.carriedByShuttleId);
                if (shuttle != null) {
                    cargo.posX = shuttle.posX;
                    cargo.posY = shuttle.posY;
                    cargo.floor = shuttle.floor;
                    cargo.statusCode = CARGO_STATUS_MOVING;

                    // 셔틀이 목적지(유효한 셀)에 도착하면 화물 하역
                    if (shuttle.statusCode == CAR_STATUS_IDLE &&
                            shuttle.currentCellId != null &&
                            Math.random() < 0.2) {

                        LayoutPos cellPos = getLayoutPos(shuttle.currentCellId);
                        if (cellPos != null) {
                            // 유효한 셀 위치에서만 하역
                            cargo.carriedByShuttleId = null;
                            shuttle.hasCargo = false;
                            cargo.storedCellId = shuttle.currentCellId;
                            cargo.statusCode = CARGO_STATUS_STORED;
                            cargo.posX = cellPos.x;
                            cargo.posY = cellPos.y;
                        }
                    }
                }
            }
            // 2. 대기중인 화물 - 셔틀이 픽업
            else if (cargo.statusCode == CARGO_STATUS_PENDING) {
                if (Math.random() < 0.08) {
                    for (VirtualShuttle shuttle : virtualShuttles.values()) {
                        if (!shuttle.hasCargo && shuttle.statusCode == CAR_STATUS_IDLE && shuttle.floor == cargo.floor) {
                            cargo.carriedByShuttleId = shuttle.eqId;
                            cargo.statusCode = CARGO_STATUS_MOVING;
                            shuttle.hasCargo = true;
                            break;
                        }
                    }
                }
            }
            // 3. 보관중인 화물 - 가끔 출고 (피킹)
            else if (cargo.statusCode == CARGO_STATUS_STORED) {
                if (Math.random() < 0.01) {
                    for (VirtualShuttle shuttle : virtualShuttles.values()) {
                        if (!shuttle.hasCargo && shuttle.statusCode == CAR_STATUS_IDLE && shuttle.floor == cargo.floor) {
                            cargo.carriedByShuttleId = shuttle.eqId;
                            cargo.statusCode = CARGO_STATUS_PICKING;
                            cargo.storedCellId = null;
                            shuttle.hasCargo = true;
                            break;
                        }
                    }
                }
            }
            // 4. 피킹중인 화물 - 컨베이어로 이동 후 제거
            else if (cargo.statusCode == CARGO_STATUS_PICKING && cargo.carriedByShuttleId == null) {
                // 컨베이어에 도착하면 제거 (출고 완료)
                if (Math.random() < 0.3) {
                    continue; // cargoUpdates에 추가하지 않음 (제거됨)
                }
            }

            cargoUpdates.add(ValueUtil.newMap(
                    "cargoId,barcode,posX,posY,cargoStatus,carriedByShuttleId,storedCellId,floor",
                    cargo.cargoId, cargo.barcode, cargo.posX, cargo.posY,
                    cargo.statusCode, cargo.carriedByShuttleId, cargo.storedCellId, cargo.floor
            ));
        }

        // 피킹 완료된 화물 제거
        virtualCargos.entrySet().removeIf(e ->
                e.getValue().statusCode == CARGO_STATUS_PICKING &&
                        e.getValue().carriedByShuttleId == null &&
                        Math.random() < 0.3
        );

        if (!cargoUpdates.isEmpty()) {
            messagingTemplate.convertAndSend("/topic/shuttle/cargos/" + lcId, cargoUpdates);
        }

        // ✅ 컨베이어 cargo_yn 업데이트 및 브로드캐스트
        updateAndBroadcastConveyorStatus(eqGroupId);
    }

    /**
     * 컨베이어 cargo_yn 상태 업데이트 및 브로드캐스트
     * 화물의 위치를 기반으로 컨베이어에 화물이 있는지 판단
     */
    private void updateAndBroadcastConveyorStatus(String eqGroupId) {
        String pattern = "%" + eqGroupId + "%";

        // 모든 컨베이어 조회
        String cvListSql = """
                SELECT eq_id, level
                  FROM tb_eq_cv_mst
                 WHERE eq_id LIKE :pattern
                   AND use_yn = true
                """;
        List<Map> conveyors = this.getQueryManager().selectListBySql(
                cvListSql,
                ValueUtil.newMap("pattern", pattern),
                Map.class, 0, 0
        );

        // 각 컨베이어별로 화물 존재 여부 체크
        for (Map cv : conveyors) {
            String cvEqId = (String) cv.get("eq_id");
            if (cvEqId == null) continue;

            // 해당 컨베이어의 레이아웃 위치 조회
            LayoutPos cvPos = layoutPositionCache.get(cvEqId);
            boolean hasCargo = false;

            if (cvPos != null) {
                // 해당 위치에 화물이 있는지 확인
                for (VirtualCargo cargo : virtualCargos.values()) {
                    if (cargo.carriedByShuttleId != null) continue; // 셔틀에 실린 화물 제외

                    int dx = Math.abs(cargo.posX - cvPos.x);
                    int dy = Math.abs(cargo.posY - cvPos.y);
                    if (dx < 50 && dy < 50) {
                        hasCargo = true;
                        break;
                    }
                }
            }

            // DB 업데이트
            String updateSql = "UPDATE tb_eq_cv_mst SET cargo_yn = :cargoYn WHERE eq_id = :eqId";
            this.getQueryManager().executeBySql(updateSql, ValueUtil.newMap("cargoYn,eqId", hasCargo, cvEqId));
        }

        // 컨베이어 상태 브로드캐스트
        String cvStatusSql = """
                SELECT
                    cv.id,
                    cv.eq_id as eqId,
                    cv.type,
                    cv.mode,
                    cv.cargo_yn as cargoYn,
                    cv.level,
                    cv.status,
                    cv.error_id as errorId,
                    cv.error_desc as errorDesc
                FROM tb_eq_cv_mst cv
                WHERE cv.eq_id LIKE :pattern
                  AND cv.use_yn = true
                """;
        List<Map> cvStatuses = this.getQueryManager().selectListBySql(
                cvStatusSql,
                ValueUtil.newMap("pattern", pattern),
                Map.class, 0, 0
        );

        if (!cvStatuses.isEmpty()) {
            messagingTemplate.convertAndSend("/topic/shuttle/cv/status/" + eqGroupId, cvStatuses);
        }
    }

    private void simulateOrderCreation(String lcId, String eqGroupId) {
        long activeCount = virtualOrders.stream()
                .filter(o -> o.statusCode != ORDER_STATUS_COMPLETED && o.statusCode != ORDER_STATUS_CANCELLED)
                .count();
        if (activeCount >= 5) return;

        long seq = orderSequence.getAndIncrement();

        VirtualOrder order = new VirtualOrder();
        order.orderKey = String.format("ORD_%s_%06d", eqGroupId, seq);
        order.orderType = (Math.random() < 0.5) ? "INBOUND" : "OUTBOUND";
        order.statusCode = ORDER_STATUS_CREATED;

        int floor = 1;
        if (!rackCellIdsByFloor.isEmpty()) {
            List<Integer> floors = new ArrayList<>(rackCellIdsByFloor.keySet());
            floor = floors.get((int) (Math.random() * floors.size()));
        }

        String fromCellId = getRandomRackCellId(floor);
        if (fromCellId == null) {
            fromCellId = buildRackCellId(eqGroupId, 1, 1, 1);
        }
        order.fromLoc = fromCellId;

        String toLoc = String.format("CV_%s_%02d", eqGroupId, (int) (Math.random() * 6) + 1);
        order.toLoc = toLoc;

        order.createdAt = System.currentTimeMillis();
        virtualOrders.add(order);

        TbWcsCraneOrder dbOrder = new TbWcsCraneOrder();
        dbOrder.setOrderKey(order.orderKey);
        dbOrder.setOrderType(order.orderType);
        dbOrder.setOrderStatus(order.statusCode);
        dbOrder.setPriority(50);
        dbOrder.setFromLocId(order.fromLoc);
        dbOrder.setToLocId(order.toLoc);
        dbOrder.setEcsIfStatus(0);
        dbOrder.setBarcode("BOX" + String.format("%08d", seq));
        dbOrder.setDomainId(7L);

        this.getQueryManager().insert(dbOrder);

        messagingTemplate.convertAndSend("/topic/shuttle/wcs/order/" + lcId, getAllActiveOrders(eqGroupId));
        logger.debug("Created virtual order: {}", order.orderKey);

        if (scheduler != null) {
            scheduler.schedule(() -> completeOrder(order, lcId, eqGroupId),
                    10 + (int) (Math.random() * 20),
                    TimeUnit.SECONDS
            );
        }
    }

    private void completeOrder(VirtualOrder order, String lcId, String eqGroupId) {
        order.statusCode = ORDER_STATUS_COMPLETED;

        String updateSql = "UPDATE tb_wcs_shuttle_order SET order_status = :status WHERE order_key = :orderKey";
        this.getQueryManager().executeBySql(
                updateSql,
                ValueUtil.newMap("status,orderKey", ORDER_STATUS_COMPLETED, order.orderKey)
        );

        messagingTemplate.convertAndSend("/topic/shuttle/wcs/order/" + lcId, getAllActiveOrders(eqGroupId));
        logger.debug("Completed virtual order: {}", order.orderKey);
    }

    private List<Map> getAllActiveOrders(String eqGroupId) {
        String sql = """
                SELECT *
                  FROM tb_wcs_shuttle_order
                 WHERE order_key LIKE :pattern
                   AND order_status NOT IN (:completed, :cancelled)
                 ORDER BY created_at DESC
                """;
        Map<String, Object> params = ValueUtil.newMap(
                "pattern,completed,cancelled",
                "%" + eqGroupId + "%",
                ORDER_STATUS_COMPLETED,
                ORDER_STATUS_CANCELLED
        );
        return this.getQueryManager().selectListBySql(sql, params, Map.class, 0, 20);
    }

    public Map<String, Object> getSimulationStatus() {
        return ValueUtil.newMap(
                "isSimulating,shuttleCount,cargoCount,activeOrderCount,currentLcId,currentEqGroupId",
                isSimulating,
                virtualShuttles.size(),
                virtualCargos.size(),
                virtualOrders.stream().filter(o -> o.statusCode != ORDER_STATUS_COMPLETED).count(),
                currentLcId,
                currentEqGroupId
        );
    }

    /**
     * WebSocket 테스트용 브로드캐스트
     * 프론트엔드 구독이 정상 동작하는지 확인
     */
    public void testBroadcast(String lcId) {
        String topic = "/topic/crane/positions/" + lcId;
        logger.info("========================================");
        logger.info("[Test Broadcast] Sending test message to: {}", topic);
        logger.info("========================================");

        List<Map<String, Object>> testData = new ArrayList<>();
        testData.add(ValueUtil.newMap(
                "equipmentId,equipmentCode,posX,posY,posZ,status,movementStatus,hasCargo,batteryLevel",
                "TEST_SHUTTLE_001", "TEST_SHUTTLE_001",
                500, 500, 1,
                0, 1, false, 100
        ));

        messagingTemplate.convertAndSend(topic, testData);
        logger.info("[Test Broadcast] Message sent successfully");
    }

    // ============================================
    // 내부 클래스
    // ============================================

    private static class VirtualShuttle {
        String eqId;

        int posX;
        int posY;

        int row;
        int bay;
        int floor;

        int targetX;
        int targetY;

        int statusCode;
        boolean hasCargo;
        int battery;

        String currentCellId;
        String targetCellId;
    }

    private static class VirtualOrder {
        String orderKey;
        String orderType;
        int statusCode;
        String fromLoc;
        String toLoc;
        long createdAt;
    }

    private static class VirtualCargo {
        String cargoId;
        String barcode;
        int posX;
        int posY;
        int statusCode;           // 숫자 상태 코드
        String carriedByShuttleId;
        String storedCellId;      // 보관된 랙 셀 ID
        int floor;                // 층 정보
    }

    private static class VirtualConveyor {
        String eqId;
        int posX;
        int posY;
        int status;
        boolean hasCargo;
        String cargoBarcode;
    }

    private static class LayoutPos {
        final int x;
        final int y;
        final int floor;

        LayoutPos(int x, int y, int floor) {
            this.x = x;
            this.y = y;
            this.floor = floor;
        }
    }

    /** cellId 예: RACKCELL_{eqGroupId}_F{floor}_R{row}_C{col} */
    private static class ParsedCell {
        final int floor;
        final int row;
        final int col;

        ParsedCell(int floor, int row, int col) {
            this.floor = floor;
            this.row = row;
            this.col = col;
        }

        static ParsedCell tryParse(String cellId) {
            if (cellId == null) return null;
            try {
                int fIdx = cellId.indexOf("_F");
                int rIdx = cellId.indexOf("_R");
                int cIdx = cellId.indexOf("_C");
                if (fIdx < 0 || rIdx < 0 || cIdx < 0) return null;

                int floor = Integer.parseInt(readNumber(cellId, fIdx + 2));
                int row = Integer.parseInt(readNumber(cellId, rIdx + 2));
                int col = Integer.parseInt(readNumber(cellId, cIdx + 2));
                return new ParsedCell(floor, row, col);
            } catch (Exception e) {
                return null;
            }
        }

        private static String readNumber(String s, int start) {
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < s.length(); i++) {
                char ch = s.charAt(i);
                if (Character.isDigit(ch)) sb.append(ch);
                else break;
            }
            return sb.toString();
        }
    }
}
