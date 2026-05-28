package operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.realtime.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import operato.logis.kmat_2026.biz.ecs.tspg4way.entity.TbEqCarMst;
import operato.logis.kmat_2026.biz.ecs.tspg4way.entity.TbEqRackMst;
import operato.logis.kmat_2026.biz.ecs.tspg4way.entity.TbWcsShuttleOrder;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * 실시간 상태 조회 Service (대시보드용)
 *
 * 원칙
 * 1) 단일 테이블 조회/필터링은 Query(condition) + addFilter(...) 로 처리
 * 2) JOIN이 필요하면 SQL String( selectListBySql / selectBySql / executeBySql )로 처리
 * 3) selectOne 같은 건 없으므로 "하나만" 필요하면 setMaxResultSize(1) + selectByCondition 사용
 *
 * 전제(너가 말한 DB 구성)
 * - tb_eq_mst.id 를 자식 테이블들이 그대로 참조
 *   - tb_eq_car_mst.eq_id = tb_eq_mst.id
 *   - tb_eq_rack_mst.eq_id = tb_eq_mst.id   (rack "부모" id)
 *   - tb_eq_cv_mst.eq_id  = tb_eq_mst.id
 */
@Service
public class RealTimeStatusService extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(RealTimeStatusService.class);

    // =========================================================
    // CAR (셔틀카)
    // =========================================================

    /**
     * 전체 셔틀카 상태 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TbEqCarMst> getAllCarStatus() {
        Query condition = new Query();
        condition.addOrder("eqId", true);
        return this.queryManager.selectList(TbEqCarMst.class, condition);
    }

    /**
     * 설비 그룹별 셔틀카 상태 목록 조회
     * - JOIN 필요 -> SQL string 사용
     */
    @Transactional(readOnly = true)
    public List<TbEqCarMst> getCarStatusByGroup(String eqGroupId) {
        String sql = """
            SELECT car.*
              FROM tb_eq_car_mst car
              JOIN tb_eq_mst m
                ON m.id = car.eq_id
             WHERE m.eq_group_id = :eqGroupId
             ORDER BY car.eq_id ASC
            """;
        Map<String, Object> params = ValueUtil.newMap("eqGroupId", eqGroupId);
        return this.queryManager.selectListBySql(sql, params, TbEqCarMst.class, 0, 0);
    }

    /**
     * 단일 셔틀카 상태 조회 (eq_id)
     * - selectOne 없음 -> setMaxResultSize(1) + selectByCondition
     */
    @Transactional(readOnly = true)
    public TbEqCarMst getCarStatus(String eqId) {
        Query condition = new Query();
        condition.addFilter("eqId", eqId);
        condition.setMaxResultSize(1);
        return this.queryManager.selectByCondition(TbEqCarMst.class, condition);
    }

    // =========================================================
    // RACK (랙 셀)
    // =========================================================

    /**
     * 전체 랙 셀 상태 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TbEqRackMst> getAllRackStatus() {
        Query condition = new Query();
        condition.addOrder("eqId", true);
        condition.addOrder("level", true);
        condition.addOrder("row", true);
        condition.addOrder("bay", true);
        return this.queryManager.selectList(TbEqRackMst.class, condition);
    }

    /**
     * 층(level)별 랙 셀 상태 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TbEqRackMst> getRackStatusByFloor(Integer floor) {
        Query condition = new Query();
        condition.addFilter("level", floor);
        condition.addOrder("eqId", true);
        condition.addOrder("row", true);
        condition.addOrder("bay", true);
        return this.queryManager.selectList(TbEqRackMst.class, condition);
    }

    /**
     * 랙(부모 eq_mst.id)별 랙 셀 목록
     * - rackId == tb_eq_mst.id
     * - tb_eq_rack_mst.eq_id == rackId
     */
    @Transactional(readOnly = true)
    public List<TbEqRackMst> getRackStatusByRackId(String rackId) {
        Query condition = new Query();
        condition.addFilter("eqId", rackId);
        condition.addOrder("level", true);
        condition.addOrder("row", true);
        condition.addOrder("bay", true);
        return this.queryManager.selectList(TbEqRackMst.class, condition);
    }

    /**
     * 단일 셀 조회 (rackId + floor + row + bay)
     * - "cell_id" 같은 별도 컬럼 없이 rackId(eq_id) + 좌표로 한 칸을 특정
     */
    @Transactional(readOnly = true)
    public TbEqRackMst getCellStatus(String cellId) {
        Query condition = new Query();
        condition.addFilter("id", cellId);
        condition.setMaxResultSize(1);
        return this.queryManager.selectByCondition(TbEqRackMst.class, condition);
    }

    /**
     * 재고가 있는 셀만 조회
     * - Query/Filter만으로 "IS NOT NULL" + ">" 가 애매하면 SQL이 안전
     */
    @Transactional(readOnly = true)
    public List<TbEqRackMst> getOccupiedCells() {
        String sql = """
            SELECT *
              FROM tb_eq_rack_mst
             WHERE sku_id IS NOT NULL
               AND sku_qty > 0
             ORDER BY eq_id, level, row, bay
            """;
        return this.queryManager.selectListBySql(sql, null, TbEqRackMst.class, 0, 0);
    }

    /**
     * 설비그룹 + 층(level)별 랙 셀 목록 조회 (매핑 드롭다운용)
     * - JOIN 필요 -> SQL string 사용
     *
     * - tb_eq_mst(type='RACK')의 id가 rackId(부모)
     * - tb_eq_rack_mst.eq_id = tb_eq_mst.id
     * - floor는 tb_eq_rack_mst.level
     */
    @Transactional(readOnly = true)
    public List<TbEqRackMst> getRackCellsByGroupAndFloor(String eqGroupId, Integer floor) {
        String sql = """
            SELECT r.*
              FROM tb_eq_rack_mst r
              JOIN tb_eq_mst m
                ON m.id = r.eq_id
             WHERE m.type = 'RACK'
               AND m.eq_group_id = :eqGroupId
               AND r.level = :floor
             ORDER BY r.row, r.bay, r.level
            """;
        Map<String, Object> params = ValueUtil.newMap("eqGroupId,floor", eqGroupId, floor);
        return this.queryManager.selectListBySql(sql, params, TbEqRackMst.class, 0, 0);
    }

    /**
     * 설비그룹의 모든 랙 셀 목록 조회 (매핑 드롭다운용)
     * - JOIN 필요 -> SQL string 사용
     */
    @Transactional(readOnly = true)
    public List<TbEqRackMst> getRackCellsByGroup(String eqGroupId) {
        String sql = """
            SELECT r.*
              FROM tb_eq_rack_mst r
              JOIN tb_eq_mst m
                ON m.id = r.eq_id
             WHERE m.type = 'RACK'
               AND m.eq_group_id = :eqGroupId
             ORDER BY r.level, r.row, r.bay
            """;
        Map<String, Object> params = ValueUtil.newMap("eqGroupId", eqGroupId);
        return this.queryManager.selectListBySql(sql, params, TbEqRackMst.class, 0, 0);
    }

    // =========================================================
    // WCS ORDER
    // =========================================================

    /**
     * 활성 오더 조회
     * - order_status int 기준으로 조회
     */
    @Transactional(readOnly = true)
    public List<TbWcsShuttleOrder> getActiveOrders() {
        // TODO: 실제 코드값으로 바꿔
        final int STATUS_PENDING = 10;
        final int STATUS_RUNNING = 20;

        String sql = """
            SELECT *
              FROM tb_wcs_shuttle_order
             WHERE order_status IN (:pending, :running)
             ORDER BY priority ASC, id ASC
            """;
        Map<String, Object> params = ValueUtil.newMap("pending,running", STATUS_PENDING, STATUS_RUNNING);
        return this.queryManager.selectListBySql(sql, params, TbWcsShuttleOrder.class, 0, 0);
    }

    /**
     * 상태별 WCS 작업 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TbWcsShuttleOrder> getOrdersByStatus(Integer orderStatus) {
        Query condition = new Query();
        condition.addFilter("orderStatus", orderStatus);
        condition.addOrder("priority", true);
        condition.addOrder("id", true);
        return this.queryManager.selectList(TbWcsShuttleOrder.class, condition);
    }

    /**
     * 셔틀별 WCS 작업 조회
     * - tb_wcs_shuttle_order에 shuttle_id 필드/컬럼이 실제로 있을 때만 정상 동작
     * - 없으면 이 메서드는 제거/대체 필요
     */
    @Transactional(readOnly = true)
    public List<TbWcsShuttleOrder> getOrdersByShuttle(String shuttleId) {
        Query condition = new Query();
        condition.addFilter("shuttleId", shuttleId);
        condition.addOrder("priority", true);
        condition.addOrder("id", true);
        return this.queryManager.selectList(TbWcsShuttleOrder.class, condition);
    }

    /**
     * 단일 WCS 작업 조회 (orderKey)
     */
    @Transactional(readOnly = true)
    public TbWcsShuttleOrder getOrder(String orderKey) {
        Query condition = new Query();
        condition.addFilter("orderKey", orderKey);
        condition.setMaxResultSize(1);
        return this.queryManager.selectByCondition(TbWcsShuttleOrder.class, condition);
    }

    /**
     * 에러 상태 작업 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TbWcsShuttleOrder> getErrorOrders() {
        // TODO: 실제 FAILED 코드로 바꿔
        final int STATUS_FAILED = 80;

        Query condition = new Query();
        condition.addFilter("orderStatus", STATUS_FAILED);
        condition.addOrder("id", false);
        return this.queryManager.selectList(TbWcsShuttleOrder.class, condition);
    }

    /**
     * 최근 완료된 작업 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TbWcsShuttleOrder> getRecentCompletedOrders(int limit) {
        // TODO: 실제 COMPLETED 코드로 바꿔
        final int STATUS_COMPLETED = 90;

        String sql = """
            SELECT *
              FROM tb_wcs_shuttle_order
             WHERE order_status = :status
             ORDER BY finished_at DESC
            """;
        Map<String, Object> params = ValueUtil.newMap("status", STATUS_COMPLETED);
        return this.queryManager.selectListBySql(sql, params, TbWcsShuttleOrder.class, 0, limit);
    }

    // =========================================================
    // 대시보드 초기 데이터 (셔틀+화물+작업 통합)
    // =========================================================

    /**
     * 대시보드 초기 데이터 조회 (셔틀, 화물, 작업 통합)
     * 시뮬레이션 없이도 DB에 있는 데이터를 화면에 표시하기 위함
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardInitialData(String lcId, String pageId) {
        logger.info("Loading dashboard initial data for lcId={}, pageId={}", lcId, pageId);

        List<Map<String, Object>> shuttles = getDashboardShuttles(lcId, pageId);
        List<Map<String, Object>> cargos = getDashboardCargos(lcId, pageId);
        List<TbWcsShuttleOrder> orders = getActiveOrders();

        return ValueUtil.newMap(
                "shuttles,cargos,orders,shuttleCount,cargoCount,orderCount",
                shuttles, cargos, orders,
                shuttles.size(), cargos.size(), orders.size()
        );
    }

    /**
     * 대시보드 셔틀 위치 조회 (레이아웃 좌표 포함)
     * - 셔틀 레이아웃(SHUTTLE 타입)이 있으면 해당 좌표 사용
     * - 없으면 셔틀의 현재 row/bay/level로 해당 랙셀 레이아웃 좌표 참조
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getDashboardShuttles(String lcId, String pageId) {
        // 1. 셔틀 레이아웃이 직접 매핑된 경우
        String sql = """
            SELECT
                car.eq_id as equipmentId,
                car.eq_id as equipmentCode,
                COALESCE(item.pos_x, 0) + COALESCE(item.width, 50) / 2 as posX,
                COALESCE(item.pos_y, 0) + COALESCE(item.height, 50) / 2 as posY,
                COALESCE(car.level, 1) as posZ,
                COALESCE(car.status, 0) as status,
                CASE WHEN car.status = 1 THEN 1 ELSE 2 END as movementStatus,
                COALESCE(car.cargo_yn, false) as hasCargo,
                COALESCE(car.battery_status, 100) as batteryLevel,
                car.row as carRow,
                car.bay as carBay
            FROM tb_eq_car_mst car
            LEFT JOIN tb_ecs_2d_item item ON item.real_eq_id = car.eq_id
            LEFT JOIN tb_ecs_2d_page page ON item.page_id = page.id
            WHERE car.use_yn = true
              AND (page.lc_id = :lcId OR item.id IS NULL)
              AND (page.id = :pageId OR item.id IS NULL)
            """;

        Map<String, Object> params = ValueUtil.newMap("lcId,pageId", lcId, pageId);
        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Map row : rows) {
            result.add(row);
        }

        logger.debug("Found {} shuttles for lcId={}, pageId={}", result.size(), lcId, pageId);
        return result;
    }

    /**
     * 대시보드 화물 위치 조회 (레이아웃 좌표 포함)
     * - 재고가 있는 랙 셀의 레이아웃 좌표 사용
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getDashboardCargos(String lcId, String pageId) {
        String sql = """
            SELECT
                rack.id as cargoId,
                rack.sku_id as barcode,
                COALESCE(item.pos_x, 0) + COALESCE(item.width, 50) / 2 as posX,
                COALESCE(item.pos_y, 0) + COALESCE(item.height, 50) / 2 as posY,
                2 as cargoStatus,
                NULL as carriedByShuttleId,
                rack.id as storedCellId,
                COALESCE(rack.level, 1) as floor,
                rack.sku_qty as skuQty
            FROM tb_eq_rack_mst rack
            INNER JOIN tb_ecs_2d_item item ON item.real_eq_id = rack.id
            INNER JOIN tb_ecs_2d_page page ON item.page_id = page.id
            WHERE page.lc_id = :lcId
              AND page.id = :pageId
              AND rack.use_yn = true
              AND rack.sku_id IS NOT NULL
              AND rack.sku_qty > 0
            """;

        Map<String, Object> params = ValueUtil.newMap("lcId,pageId", lcId, pageId);
        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Map row : rows) {
            result.add(row);
        }

        logger.debug("Found {} cargos for lcId={}, pageId={}", result.size(), lcId, pageId);
        return result;
    }

    // =========================================================
    // 작업 제어 (뼈대)
    // =========================================================

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> requestCancelOrder(String orderKey, String reason) {
        logger.info("Request cancel order: orderKey={}, reason={}", orderKey, reason);

        TbWcsShuttleOrder order = this.getOrder(orderKey);
        if (order == null) {
            return ValueUtil.newMap("success,message", false, "Order not found: " + orderKey);
        }

        // TODO: 실제 ECS 연동
        logger.info("Order cancel requested (ECS integration pending): orderKey={}", orderKey);

        return ValueUtil.newMap(
                "success,message,orderKey",
                true, "Cancel request sent (ECS integration pending)", orderKey
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> requestResumeOrder(String orderKey) {
        logger.info("Request resume order: orderKey={}", orderKey);

        TbWcsShuttleOrder order = this.getOrder(orderKey);
        if (order == null) {
            return ValueUtil.newMap("success,message", false, "Order not found: " + orderKey);
        }

        // TODO: 실제 ECS 연동
        logger.info("Order resume requested (ECS integration pending): orderKey={}", orderKey);

        return ValueUtil.newMap(
                "success,message,orderKey",
                true, "Resume request sent (ECS integration pending)", orderKey
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> requestChangePriority(String orderKey, Integer newPriority) {
        logger.info("Request change priority: orderKey={}, newPriority={}", orderKey, newPriority);

        TbWcsShuttleOrder order = this.getOrder(orderKey);
        if (order == null) {
            return ValueUtil.newMap("success,message", false, "Order not found: " + orderKey);
        }

        // TODO: 실제 ECS 연동
        logger.info("Priority change requested (ECS integration pending): orderKey={}, newPriority={}", orderKey, newPriority);

        return ValueUtil.newMap(
                "success,message,orderKey,newPriority",
                true, "Priority change request sent (ECS integration pending)", orderKey, newPriority
        );
    }
}
