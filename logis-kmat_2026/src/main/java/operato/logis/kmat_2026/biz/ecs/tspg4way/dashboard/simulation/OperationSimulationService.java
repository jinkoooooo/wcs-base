package operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.simulation;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PreDestroy;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.BeanUtil;
import xyz.elidom.util.ValueUtil;

/**
 * 실운영 기반 가상 시나리오 시뮬레이션 서비스
 *
 * 실제 테이블 구조 기준:
 * - tb_eq_group_mst: 설비 그룹 (예: K_MAT_TSPG)
 * - tb_eq_mst: 설비 마스터 (RACK_1, CV_1, SHUTTLE_CAR_1, SHUTTLE_CAR_2)
 * - tb_eq_car_mst: 셔틀 상태 (위치, 상태, 배터리, 화물)
 * - tb_eq_rack_mst: 랙 셀 (재고, 타입)
 * - tb_eq_cv_mst: 컨베이어 상태
 * - tb_wcs_shuttle_order: WCS 작업
 * - tb_ecs_rack_order: ECS 랙 작업
 */
@Service
public class OperationSimulationService extends AbstractQueryService {

    private static final Logger log = LoggerFactory.getLogger(OperationSimulationService.class);

    // ===== 설비 타입 (EcsDBConsts.EqType 기준) =====
    public static final int EQ_TYPE_RACK = 11;
    public static final int EQ_TYPE_CONVEYOR = 21;
    public static final int EQ_TYPE_SHUTTLE_CAR = 22;

    // ===== 랙 타입 (EcsDBConsts.RackType 기준) =====
    public static final int RACK_TYPE_CELL = 11;
    public static final int RACK_TYPE_INBOUND_PORT = 21;
    public static final int RACK_TYPE_OUTBOUND_PORT = 22;
    public static final int RACK_TYPE_IN_OUTBOUND_PORT = 23;
    public static final int RACK_TYPE_CHARGE_PORT = 31;
    public static final int RACK_TYPE_CHARGE_ENTER_PORT = 32;

    // ===== 상태 코드 =====
    public static final int STATUS_IDLE = 0;
    public static final int STATUS_RESERVED = 1;
    public static final int STATUS_WORKING = 2;
    public static final int STATUS_ERROR = 8;
    public static final int STATUS_COMPLETE = 9;

    // ===== WCS 오더 상태 =====
    public static final int WCS_CREATED = 10;
    public static final int WCS_ECS_SENT = 20;
    public static final int WCS_WORKING = 30;
    public static final int WCS_COMPLETE = 90;
    public static final int WCS_CANCEL = 99;

    // ===== ECS 오더 상태 =====
    public static final int ECS_READY = 10;
    public static final int ECS_EQ_SEND = 20;
    public static final int ECS_WORKING = 30;
    public static final int ECS_COMPLETE = 90;

    private ScheduledExecutorService scheduler;
    private volatile boolean running = false;

    // 시뮬레이션 컨텍스트
    private String eqGroupId;
    private String rackEqId;  // 실운영: "RACK_1"
    private final AtomicLong orderSeq = new AtomicLong(1);
    private final AtomicLong barcodeSeq = new AtomicLong(1);

    // 내부 상태 추적 (셔틀별 현재 작업)
    private final Map<String, SimCarState> carStates = new ConcurrentHashMap<>();

    @PreDestroy
    public void destroy() {
        stop();
    }

    // ============================================
    // 1. 실운영 Seed 데이터 복사/생성
    // ============================================

    /**
     * 실운영 Seed 데이터 생성 (K_MAT_TSPG 기준)
     *
     * 실제 운영 테이블 구조에 맞게 데이터 생성
     */
    @Transactional
    public Map<String, Object> seed(String eqGroupId, int floors, int shuttlesPerFloor,
                                    int rackRows, int rackBays) {
        log.info("========================================");
        log.info("[Seed] Creating production-like equipment data");
        log.info("  eqGroupId: {}", eqGroupId);
        log.info("  floors: {}, shuttles/floor: {}", floors, shuttlesPerFloor);
        log.info("  rack: {}x{}", rackRows, rackBays);
        log.info("========================================");

        this.eqGroupId = eqGroupId;
        this.rackEqId = "RACK_1";

        // 기존 시뮬레이션 데이터만 삭제 (실운영 데이터는 보존)
        clearSimulationOrders(eqGroupId);

        // 1. 설비 그룹 확인/생성
        ensureEqGroup(eqGroupId);

        // 2. 설비 마스터 확인/생성
        ensureEqMst(rackEqId, eqGroupId, "셔틀랙", EQ_TYPE_RACK);
        ensureEqMst("CV_1", eqGroupId, "컨베이어", EQ_TYPE_CONVEYOR);

        // 3. 셔틀 카 생성
        int carCount = 0;
        for (int floor = 1; floor <= floors; floor++) {
            for (int i = 1; i <= shuttlesPerFloor; i++) {
                String carId = String.format("SHUTTLE_CAR_%d", (floor - 1) * shuttlesPerFloor + i);
                ensureEqMst(carId, eqGroupId, String.format("셔틀카 %d단", floor), EQ_TYPE_SHUTTLE_CAR);
                ensureCarMst(carId, rackEqId, floor, 1, 1);
                carCount++;
            }
        }

        // 4. 랙 셀 생성
        int rackCount = createRackCells(rackEqId, floors, rackRows, rackBays);

        // 5. 컨베이어 생성
        int cvCount = createConveyors("CV_1", floors);

        log.info("[Seed] Created: {} cars, {} rack cells, {} conveyors", carCount, rackCount, cvCount);

        return ValueUtil.newMap(
            "success,carCount,rackCount,cvCount,eqGroupId",
            true, carCount, rackCount, cvCount, eqGroupId
        );
    }

    private void ensureEqGroup(String eqGroupId) {
        String sql = "SELECT COUNT(*) FROM tb_eq_group_mst WHERE id = :id";
        Long count = this.queryManager.selectBySql(sql, ValueUtil.newMap("id", eqGroupId), Long.class);

        if (count == null || count == 0) {
            String insertSql = """
                INSERT INTO tb_eq_group_mst (id, name, type, domain_id, creator_id, updater_id, created_at, updated_at)
                VALUES (:id, :name, :type, 7, 'system', 'system', now(), now())
                """;
            this.queryManager.executeBySql(insertSql, ValueUtil.newMap(
                "id,name,type",
                eqGroupId,
                eqGroupId + " TSPG 4-Way Shuttle",
                eqGroupId
            ));
            log.info("[Seed] Created eq_group: {}", eqGroupId);
        }
    }

    private void ensureEqMst(String id, String eqGroupId, String name, int type) {
        String sql = "SELECT COUNT(*) FROM tb_eq_mst WHERE id = :id";
        Long count = this.queryManager.selectBySql(sql, ValueUtil.newMap("id", id), Long.class);

        if (count == null || count == 0) {
            String insertSql = """
                INSERT INTO tb_eq_mst (id, eq_group_id, name, type, plc_id, domain_id, created_at, updated_at)
                VALUES (:id, :eqGroupId, :name, :type, :plcId, 7, now(), now())
                """;
            this.queryManager.executeBySql(insertSql, ValueUtil.newMap(
                "id,eqGroupId,name,type,plcId",
                id, eqGroupId, name, type, id
            ));
            log.info("[Seed] Created eq_mst: {} (type={})", id, type);
        }
    }

    private void ensureCarMst(String carId, String rackEqId, int level, int row, int bay) {
        String sql = "SELECT COUNT(*) FROM tb_eq_car_mst WHERE eq_id = :eqId";
        Long count = this.queryManager.selectBySql(sql, ValueUtil.newMap("eqId", carId), Long.class);

        if (count == null || count == 0) {
            String rackId = String.format("%d%02d%02d", level, row, bay);
            String insertSql = """
                INSERT INTO tb_eq_car_mst (id, eq_id, rack_eq_id, level, row, bay, rack_id,
                    status, battery_status, cargo_yn, auto_yn, use_yn, domain_id, created_at, updated_at)
                VALUES (:id, :eqId, :rackEqId, :level, :row, :bay, :rackId,
                    0, 100, false, true, true, 7, now(), now())
                """;
            this.queryManager.executeBySql(insertSql, ValueUtil.newMap(
                "id,eqId,rackEqId,level,row,bay,rackId",
                carId, carId, rackEqId, level, row, bay, rackId
            ));
            log.info("[Seed] Created car_mst: {} at F{}-R{}-B{}", carId, level, row, bay);
        } else {
            // 기존 데이터 초기화
            String updateSql = """
                UPDATE tb_eq_car_mst
                SET level = :level, row = :row, bay = :bay, rack_id = :rackId,
                    status = 0, battery_status = 100, cargo_yn = false, auto_yn = true, use_yn = true,
                    error_id = null, error_desc = null, updated_at = now()
                WHERE eq_id = :eqId
                """;
            String rackId = String.format("%d%02d%02d", level, row, bay);
            this.queryManager.executeBySql(updateSql, ValueUtil.newMap(
                "eqId,level,row,bay,rackId",
                carId, level, row, bay, rackId
            ));
        }
    }

    private int createRackCells(String rackEqId, int floors, int rackRows, int rackBays) {
        int count = 0;

        for (int level = 1; level <= floors; level++) {
            for (int row = 1; row <= rackRows; row++) {
                for (int bay = 0; bay <= rackBays; bay++) {
                    // ID 형식: level(1자리) + row(2자리) + bay(2자리) = "10101"
                    String cellId = String.format("%d%02d%02d", level, row, bay);

                    // 셀 타입 결정
                    int cellType = RACK_TYPE_CELL;
                    boolean driveOnlyYn = false;
                    boolean useYn = true;

                    if (bay == 0) {
                        // bay=0: 충전포트
                        cellType = RACK_TYPE_CHARGE_PORT;
                        driveOnlyYn = true;
                        useYn = false;  // 충전포트는 사용 안함
                    } else if (row == 1 || row == rackRows) {
                        // 첫번째/마지막 row: 주행 전용
                        driveOnlyYn = true;
                        if (row == rackRows && bay <= 2) {
                            // 마지막 row의 앞쪽: 출고포트
                            cellType = RACK_TYPE_OUTBOUND_PORT;
                        } else if (row == rackRows - 1 && bay == 1) {
                            // 충전진입포트
                            cellType = RACK_TYPE_CHARGE_ENTER_PORT;
                        }
                    }

                    ensureRackCell(cellId, rackEqId, level, row, bay, cellType, driveOnlyYn, useYn);
                    count++;
                }
            }
        }

        return count;
    }

    private void ensureRackCell(String cellId, String rackEqId, int level, int row, int bay,
                                int type, boolean driveOnlyYn, boolean useYn) {
        String sql = "SELECT COUNT(*) FROM tb_eq_rack_mst WHERE id = :id";
        Long count = this.queryManager.selectBySql(sql, ValueUtil.newMap("id", cellId), Long.class);

        if (count == null || count == 0) {
            String insertSql = """
                INSERT INTO tb_eq_rack_mst (id, eq_id, level, row, bay, type, drive_only_yn, use_yn,
                    status, domain_id, created_at, updated_at)
                VALUES (:id, :eqId, :level, :row, :bay, :type, :driveOnlyYn, :useYn,
                    0, 7, now(), now())
                """;
            this.queryManager.executeBySql(insertSql, ValueUtil.newMap(
                "id,eqId,level,row,bay,type,driveOnlyYn,useYn",
                cellId, rackEqId, level, row, bay, type, driveOnlyYn, useYn
            ));
        }
    }

    private int createConveyors(String cvEqId, int floors) {
        int count = 0;

        // 층별 컨베이어
        for (int level = 1; level <= floors; level++) {
            for (int idx = 1; idx <= 4; idx++) {
                String cvId = String.format("%d%02d", level, idx);
                ensureConveyor(cvId, cvEqId, level, idx == 1 ? 4 : 11);  // 첫번째는 특수 타입
                count++;
            }
        }

        return count;
    }

    private void ensureConveyor(String cvId, String cvEqId, int level, int type) {
        String sql = "SELECT COUNT(*) FROM tb_eq_cv_mst WHERE id = :id";
        Long count = this.queryManager.selectBySql(sql, ValueUtil.newMap("id", cvId), Long.class);

        if (count == null || count == 0) {
            String insertSql = """
                INSERT INTO tb_eq_cv_mst (id, eq_id, level, type, status, cargo_yn, auto_yn, use_yn,
                    stopper_open_yn, run_yn, domain_id, created_at, updated_at)
                VALUES (:id, :eqId, :level, :type, 0, false, true, true, false, false, 7, now(), now())
                """;
            this.queryManager.executeBySql(insertSql, ValueUtil.newMap(
                "id,eqId,level,type",
                cvId, cvEqId, level, type
            ));
        }
    }

    private void clearSimulationOrders(String eqGroupId) {
        // 시뮬레이션에서 생성된 오더만 삭제 (SIM_ 접두사)
        this.queryManager.executeBySql("DELETE FROM tb_ecs_rack_order WHERE order_key LIKE 'SIM_%'", null);
        this.queryManager.executeBySql("DELETE FROM tb_wcs_shuttle_order WHERE order_key LIKE 'SIM_%'", null);
        log.info("[Clear] Deleted simulation orders");
    }

    // ============================================
    // 2. 시뮬레이션 시작/중지/리셋
    // ============================================

    public synchronized Map<String, Object> start(String eqGroupId) {
        if (running) {
            return result(false, "Already running");
        }

        this.eqGroupId = eqGroupId;

        // rackEqId 조회
        String sql = "SELECT id FROM tb_eq_mst WHERE eq_group_id = :g AND type = :t LIMIT 1";
        Map rackInfo = this.queryManager.selectBySql(sql, ValueUtil.newMap("g,t", eqGroupId, EQ_TYPE_RACK), Map.class);
        this.rackEqId = rackInfo != null ? (String) rackInfo.get("id") : "RACK_1";

        initCarStates();

        running = true;
        scheduler = Executors.newScheduledThreadPool(2);

        // 셔틀 이동 (500ms)
        scheduler.scheduleAtFixedRate(this::tickShuttleMovement, 0, 500, TimeUnit.MILLISECONDS);

        // 작업 생성 및 처리 (2초)
        scheduler.scheduleAtFixedRate(this::tickOrderProcess, 1000, 2000, TimeUnit.MILLISECONDS);

        log.info("[Simulation] Started for eqGroupId: {}, rackEqId: {}", eqGroupId, rackEqId);
        return result(true, "Simulation started");
    }

    public synchronized Map<String, Object> stop() {
        if (!running) {
            return result(false, "Not running");
        }

        running = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }

        log.info("[Simulation] Stopped");
        return result(true, "Simulation stopped");
    }

    public synchronized Map<String, Object> reset(String eqGroupId) {
        stop();
        clearSimulationOrders(eqGroupId);
        carStates.clear();
        orderSeq.set(1);
        barcodeSeq.set(1);

        // 셔틀 위치 초기화
        String updateSql = """
            UPDATE tb_eq_car_mst
            SET status = 0, cargo_yn = false, error_id = null, error_desc = null
            WHERE eq_id IN (SELECT id FROM tb_eq_mst WHERE eq_group_id = :g AND type = :t)
            """;
        this.queryManager.executeBySql(updateSql, ValueUtil.newMap("g,t", eqGroupId, EQ_TYPE_SHUTTLE_CAR));

        log.info("[Simulation] Reset complete");
        return result(true, "Simulation reset");
    }

    public Map<String, Object> getStatus() {
        long activeOrders = 0;
        if (eqGroupId != null) {
            String sql = "SELECT COUNT(*) FROM tb_wcs_shuttle_order WHERE order_key LIKE 'SIM_%' AND order_status < :s";
            Long count = this.queryManager.selectBySql(sql, ValueUtil.newMap("s", WCS_COMPLETE), Long.class);
            activeOrders = count != null ? count : 0;
        }

        return ValueUtil.newMap(
            "running,eqGroupId,rackEqId,carCount,activeOrders",
            running, eqGroupId, rackEqId, carStates.size(), activeOrders
        );
    }

    // ============================================
    // 3. 시뮬레이션 틱
    // ============================================

    private void initCarStates() {
        carStates.clear();

        String sql = """
            SELECT c.* FROM tb_eq_car_mst c
            JOIN tb_eq_mst e ON c.eq_id = e.id
            WHERE e.eq_group_id = :g AND e.type = :t
            """;
        List<Map> cars = this.queryManager.selectListBySql(sql, ValueUtil.newMap("g,t", eqGroupId, EQ_TYPE_SHUTTLE_CAR), Map.class, 0, 0);

        for (Map car : cars) {
            SimCarState state = new SimCarState();
            state.eqId = (String) car.get("eq_id");
            state.rackEqId = (String) car.get("rack_eq_id");
            state.floor = getInt(car, "level", 1);
            state.row = getInt(car, "row", 1);
            state.bay = getInt(car, "bay", 1);
            state.targetRow = state.row;
            state.targetBay = state.bay;
            state.status = getInt(car, "status", 0);
            state.hasCargo = getBool(car, "cargo_yn");
            state.battery = getInt(car, "battery_status", 100);
            carStates.put(state.eqId, state);
        }

        log.info("[Init] Loaded {} shuttles", carStates.size());
    }

    private void tickShuttleMovement() {
        if (!running) return;

        try {
            for (SimCarState car : carStates.values()) {
                boolean moved = false;

                // Row 이동
                if (car.row < car.targetRow) {
                    car.row++;
                    moved = true;
                } else if (car.row > car.targetRow) {
                    car.row--;
                    moved = true;
                }

                // Bay 이동
                if (car.bay < car.targetBay) {
                    car.bay++;
                    moved = true;
                } else if (car.bay > car.targetBay) {
                    car.bay--;
                    moved = true;
                }

                if (moved) {
                    car.status = STATUS_WORKING;
                    // 배터리 소모
                    if (car.battery > 10 && Math.random() < 0.3) {
                        car.battery--;
                    }
                } else {
                    // 도착
                    if (car.status == STATUS_WORKING) {
                        car.status = STATUS_IDLE;

                        // 작업 완료 처리
                        if (car.currentOrderKey != null) {
                            completeCarTask(car);
                        }
                    }

                    // IDLE 상태에서 새 목적지 할당 (10% 확률)
                    if (car.status == STATUS_IDLE && car.currentOrderKey == null && Math.random() < 0.1) {
                        assignRandomMove(car);
                    }
                }

                // DB 업데이트
                updateCarInDb(car);
            }
        } catch (Exception e) {
            log.error("[Shuttle] Movement tick error", e);
        }
    }

    private void tickOrderProcess() {
        if (!running) return;

        try {
            // 활성 작업 수 확인
            String countSql = "SELECT COUNT(*) FROM tb_wcs_shuttle_order WHERE order_key LIKE 'SIM_%' AND order_status < :s";
            Long activeCount = this.queryManager.selectBySql(countSql, ValueUtil.newMap("s", WCS_COMPLETE), Long.class);

            // 작업이 5개 미만이면 새 작업 생성
            if (activeCount != null && activeCount < 5) {
                createNewOrder();
            }

            // 대기 중인 ECS Rack Order 처리
            processEcsRackOrders();

        } catch (Exception e) {
            log.error("[Order] Process tick error", e);
        }
    }

    // ============================================
    // 4. 작업 처리
    // ============================================

    @Transactional
    public void createNewOrder() {
        String fromSql = """
        SELECT id, level, row, bay
        FROM tb_eq_rack_mst
        WHERE eq_id = :rackEqId
          AND type = :t
          AND use_yn = true
          AND row BETWEEN 1 AND 6
          AND bay BETWEEN 1 AND 4
        ORDER BY RANDOM()
        LIMIT 1
        """;

        Map fromCell = this.queryManager.selectBySql(
                fromSql,
                ValueUtil.newMap("rackEqId,t", rackEqId, RACK_TYPE_CELL),
                Map.class
        );

        if (fromCell == null) {
            log.debug("[Order] No from cell found");
            return;
        }

        int level = getInt(fromCell, "level", 1);
        String fromId = (String) fromCell.get("id");
        int fromRow = getInt(fromCell, "row", 1);
        int fromBay = getInt(fromCell, "bay", 1);

        String toSql = """
        SELECT id, level, row, bay
        FROM tb_eq_rack_mst
        WHERE eq_id = :rackEqId
          AND type = :t
          AND use_yn = true
          AND level = :level
          AND row BETWEEN 1 AND 6
          AND bay BETWEEN 1 AND 4
          AND id <> :fromId
        ORDER BY RANDOM()
        LIMIT 1
        """;

        Map toCell = this.queryManager.selectBySql(
                toSql,
                ValueUtil.newMap("rackEqId,t,level,fromId", rackEqId, RACK_TYPE_CELL, level, fromId),
                Map.class
        );

        if (toCell == null) {
            log.debug("[Order] No to cell found");
            return;
        }

        String toId = (String) toCell.get("id");
        int toRow = getInt(toCell, "row", 1);
        int toBay = getInt(toCell, "bay", 1);

        String wcsOrderId = UUID.randomUUID().toString();
        String orderKey = String.format("SIM_ORD_%06d", orderSeq.getAndIncrement());
        String barcode = String.format("SIM_BOX%08d", barcodeSeq.getAndIncrement());

        String insertWcsSql = """
        INSERT INTO tb_wcs_shuttle_order (
            id, order_key, order_type, order_status, priority, barcode,
            from_loc_code, to_loc_code, eq_group_id, domain_id, created_at, updated_at
        )
        VALUES (
            :id, :orderKey, :orderType, :orderStatus, 50, :barcode,
            :fromLoc, :toLoc, :eqGroupId, 7, now(), now()
        )
        """;

        this.queryManager.executeBySql(insertWcsSql, ValueUtil.newMap(
                "id,orderKey,orderType,orderStatus,barcode,fromLoc,toLoc,eqGroupId",
                wcsOrderId, orderKey, "MOVE", WCS_CREATED, barcode, fromId, toId, eqGroupId
        ));

        String insertEcsSql = """
        INSERT INTO tb_ecs_rack_order (
            order_key, order_type, order_status, priority, barcode,
            eq_id, level, from_loc_code, to_loc_code,
            from_row, from_bay, to_row, to_bay,
            cmd_status, domain_id, created_at, updated_at
        )
        VALUES (
            :orderKey, :orderType, :orderStatus, 50, :barcode,
            :eqId, :level, :fromLoc, :toLoc,
            :fromRow, :fromBay, :toRow, :toBay,
            0, 7, now(), now()
        )
        """;

        this.queryManager.executeBySql(insertEcsSql, ValueUtil.newMap(
                "orderKey,orderType,orderStatus,barcode,eqId,level,fromLoc,toLoc,fromRow,fromBay,toRow,toBay",
                orderKey, 99, ECS_READY, barcode, rackEqId, level, fromId, toId, fromRow, fromBay, toRow, toBay
        ));

        this.queryManager.executeBySql(
                "UPDATE tb_wcs_shuttle_order SET order_status = :s, updated_at = now() WHERE order_key = :k",
                ValueUtil.newMap("s,k", WCS_ECS_SENT, orderKey)
        );

        log.info("[Order] Created MOVE order: {} (F{} R{}B{} -> R{}B{})",
                orderKey, level, fromRow, fromBay, toRow, toBay);
    }

    private void createEcsRackOrder(String orderKey, String orderType, String barcode,
                                    String fromLoc, String toLoc, int level) {
        int ecsOrderType = "INBOUND".equals(orderType) ? 1 : 2;

        String insertSql = """
            INSERT INTO tb_ecs_rack_order (order_key, order_type, order_status, priority, barcode,
                eq_id, level, from_loc_code, to_loc_code, from_row, from_bay, to_row, to_bay,
                cmd_status, domain_id, created_at, updated_at)
            VALUES (:orderKey, :orderType, :orderStatus, 50, :barcode,
                :eqId, :level, :fromLoc, :toLoc, :fromRow, :fromBay, :toRow, :toBay,
                0, 7, now(), now())
            """;

        this.queryManager.executeBySql(insertSql, ValueUtil.newMap(
            "orderKey,orderType,orderStatus,barcode,eqId,level,fromLoc,toLoc,fromRow,fromBay,toRow,toBay",
            orderKey, ecsOrderType, ECS_READY, barcode, rackEqId, level, fromLoc, toLoc,
            parseRow(fromLoc), parseBay(fromLoc), parseRow(toLoc), parseBay(toLoc)
        ));

        // WCS Order 상태 업데이트
        this.queryManager.executeBySql(
            "UPDATE tb_wcs_shuttle_order SET order_status = :s, updated_at = now() WHERE order_key = :k",
            ValueUtil.newMap("s,k", WCS_ECS_SENT, orderKey)
        );
    }

    private void processEcsRackOrders() {
        // READY 상태의 ECS Rack Order 조회
        String sql = """
            SELECT * FROM tb_ecs_rack_order
            WHERE eq_id = :rackEqId AND order_status = :s AND order_key LIKE 'SIM_%'
            ORDER BY priority DESC, id ASC
            LIMIT 5
            """;
        List<Map> orders = this.queryManager.selectListBySql(sql,
            ValueUtil.newMap("rackEqId,s", rackEqId, ECS_READY), Map.class, 0, 0);

        for (Map order : orders) {
            int level = getInt(order, "level", 1);
            String orderKey = (String) order.get("order_key");

            // 해당 층의 사용 가능한 셔틀 찾기
            SimCarState availableCar = findAvailableCar(level);
            if (availableCar == null) continue;

            // 셔틀에 작업 할당
            availableCar.currentOrderKey = orderKey;
            availableCar.targetRow = getInt(order, "from_row", 1);
            availableCar.targetBay = getInt(order, "from_bay", 1);
            availableCar.status = STATUS_RESERVED;

            // ECS Order 상태 업데이트
            this.queryManager.executeBySql(
                "UPDATE tb_ecs_rack_order SET order_status = :s, eq_car_id = :c, updated_at = now() WHERE order_key = :k",
                ValueUtil.newMap("s,c,k", ECS_EQ_SEND, availableCar.eqId, orderKey)
            );

            log.debug("[ECS] Assigned order {} to car {}", orderKey, availableCar.eqId);
        }
    }

    private SimCarState findAvailableCar(int floor) {
        for (SimCarState car : carStates.values()) {
            if (car.floor == floor && car.status == STATUS_IDLE && car.currentOrderKey == null) {
                return car;
            }
        }
        return null;
    }

    private void completeCarTask(SimCarState car) {
        // ECS Order 조회
        String sql = "SELECT * FROM tb_ecs_rack_order WHERE order_key = :k AND eq_car_id = :c";
        Map ecsOrder = this.queryManager.selectBySql(sql,
            ValueUtil.newMap("k,c", car.currentOrderKey, car.eqId), Map.class);

        if (ecsOrder == null) {
            car.currentOrderKey = null;
            return;
        }

        int fromRow = getInt(ecsOrder, "from_row", 1);
        int fromBay = getInt(ecsOrder, "from_bay", 1);
        int toRow = getInt(ecsOrder, "to_row", 1);
        int toBay = getInt(ecsOrder, "to_bay", 1);
        int orderType = getInt(ecsOrder, "order_type", 1);
        String orderKey = (String) ecsOrder.get("order_key");
        String fromLoc = (String) ecsOrder.get("from_loc_code");
        String toLoc = (String) ecsOrder.get("to_loc_code");
        String barcode = (String) ecsOrder.get("barcode");

        // 현재 from 위치에 도착한 경우 -> to 위치로 이동
        if (car.row == fromRow && car.bay == fromBay && !car.hasCargo) {
            // 화물 픽업
            car.hasCargo = true;
            car.targetRow = toRow;
            car.targetBay = toBay;
            car.status = STATUS_WORKING;

            // 출고의 경우 랙 재고 제거
            if (orderType == 2) {
                this.queryManager.executeBySql(
                    "UPDATE tb_eq_rack_mst SET sku_id = NULL, sku_qty = 0, updated_at = now() WHERE id = :id",
                    ValueUtil.newMap("id", fromLoc)
                );
            }

            this.queryManager.executeBySql(
                "UPDATE tb_ecs_rack_order SET order_status = :s, updated_at = now() WHERE order_key = :k",
                ValueUtil.newMap("s,k", ECS_WORKING, orderKey)
            );

            log.debug("[Car] {} picked up cargo at R{}-B{}", car.eqId, car.row, car.bay);
        }
        // to 위치에 도착한 경우 -> 작업 완료
        else if (car.row == toRow && car.bay == toBay && car.hasCargo) {
            // 화물 하역
            car.hasCargo = false;
            car.currentOrderKey = null;

            // 입고의 경우 랙에 재고 추가
            if (orderType == 1) {
                this.queryManager.executeBySql(
                    "UPDATE tb_eq_rack_mst SET sku_id = :sku, sku_qty = 1, updated_at = now() WHERE id = :id",
                    ValueUtil.newMap("sku,id", barcode, toLoc)
                );
            }

            // ECS Order 완료
            this.queryManager.executeBySql(
                "UPDATE tb_ecs_rack_order SET order_status = :s, updated_at = now() WHERE order_key = :k",
                ValueUtil.newMap("s,k", ECS_COMPLETE, orderKey)
            );

            // WCS Order 완료
            this.queryManager.executeBySql(
                "UPDATE tb_wcs_shuttle_order SET order_status = :s, updated_at = now() WHERE order_key = :k",
                ValueUtil.newMap("s,k", WCS_COMPLETE, orderKey)
            );

            log.info("[Car] {} completed order {} at R{}-B{}", car.eqId, orderKey, car.row, car.bay);
        }
    }

    private void assignRandomMove(SimCarState car) {
        // 랜덤 위치로 이동 (주행 가능 셀로)
        String sql = """
            SELECT row, bay FROM tb_eq_rack_mst
            WHERE eq_id = :rackEqId AND level = :l AND type = :t AND use_yn = true
            ORDER BY RANDOM() LIMIT 1
            """;
        Map target = this.queryManager.selectBySql(sql,
            ValueUtil.newMap("rackEqId,l,t", rackEqId, car.floor, RACK_TYPE_CELL), Map.class);

        if (target != null) {
            car.targetRow = getInt(target, "row", car.row);
            car.targetBay = getInt(target, "bay", car.bay);
        }
    }

    private void updateCarInDb(SimCarState car) {
        String rackId = String.format("%d%02d%02d", car.floor, car.row, car.bay);
        String sql = """
            UPDATE tb_eq_car_mst
            SET row = :r, bay = :b, rack_id = :rackId, status = :s, cargo_yn = :c,
                battery_status = :bat, updated_at = now()
            WHERE eq_id = :id
            """;
        this.queryManager.executeBySql(sql, ValueUtil.newMap(
            "r,b,rackId,s,c,bat,id",
            car.row, car.bay, rackId, car.status, car.hasCargo, car.battery, car.eqId
        ));
    }

    // ============================================
    // 유틸리티
    // ============================================

    private int parseRow(String cellId) {
        if (cellId == null || cellId.length() < 3) return 1;
        try {
            return Integer.parseInt(cellId.substring(1, 3));
        } catch (Exception e) {
            return 1;
        }
    }

    private int parseBay(String cellId) {
        if (cellId == null || cellId.length() < 5) return 1;
        try {
            return Integer.parseInt(cellId.substring(3, 5));
        } catch (Exception e) {
            return 1;
        }
    }

    private int getInt(Map map, String key, int defaultValue) {
        Object val = map.get(key);
        if (val == null) return defaultValue;
        if (val instanceof Number) return ((Number) val).intValue();
        try {
            return Integer.parseInt(val.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private boolean getBool(Map map, String key) {
        Object val = map.get(key);
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        return "true".equalsIgnoreCase(val.toString()) || "1".equals(val.toString());
    }

    private Map<String, Object> result(boolean success, String message) {
        return ValueUtil.newMap("success,message", success, message);
    }

    // ============================================
    // 내부 클래스
    // ============================================

    private static class SimCarState {
        String eqId;
        String rackEqId;
        int floor;
        int row, bay;
        int targetRow, targetBay;
        int status;
        boolean hasCargo;
        int battery;
        String currentOrderKey;
    }
}
