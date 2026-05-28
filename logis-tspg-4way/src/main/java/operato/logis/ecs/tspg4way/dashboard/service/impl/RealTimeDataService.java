package operato.logis.ecs.tspg4way.dashboard.service.impl;

import java.util.*;

import operato.logis.wcs.entity.TbWcsShuttleOrder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import operato.logis.ecs.tspg4way.dashboard.entity.TbEcs2dPage;
import operato.logis.ecs.tspg4way.entity.TbEqCarMst;
import operato.logis.ecs.tspg4way.entity.TbEqCvMst;
import operato.logis.ecs.tspg4way.entity.TbEqMst;
import operato.logis.ecs.tspg4way.entity.TbEqRackMst;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.orm.OrmConstants;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

@Service
public class RealTimeDataService extends AbstractQueryService {

    public static final int EQ_TYPE_RACK = 11;
    public static final int EQ_TYPE_CONVEYOR = 21;
    public static final int EQ_TYPE_SHUTTLE_CAR = 22;

    // 공통 조회

    public List<TbEqMst> findEqMstByGroupAndType(String eqGroupId, int type) {
        if (!StringUtils.hasText(eqGroupId)) {
            return Collections.emptyList();
        }

        String sql = """
                SELECT id, eq_group_id, name, type, plc_id
                  FROM tb_eq_mst
                 WHERE eq_group_id = :eqGroupId
                   AND type = :type
                 ORDER BY id ASC
                """;

        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,type",
                eqGroupId, type
        );

        return this.queryManager.selectListBySql(sql, params, TbEqMst.class, 0, 0);
    }

    public List<TbEqRackMst> findRacksByEqId(String eqId) {
        if (!StringUtils.hasText(eqId)) {
            return Collections.emptyList();
        }

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_id", eqId);
        condition.addOrder("level", true);
        condition.addOrder("row", true);
        condition.addOrder("bay", true);

        return this.queryManager.selectList(TbEqRackMst.class, condition);
    }

    public List<TbEqRackMst> findRackCellsByEqIdAndFloor(String eqId, int floor) {
        if (!StringUtils.hasText(eqId)) {
            return Collections.emptyList();
        }

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_id", eqId);
        condition.addFilter("level", floor);
        condition.addFilter("use_yn", true);
        condition.addOrder("row", true);
        condition.addOrder("bay", true);

        return this.queryManager.selectList(TbEqRackMst.class, condition);
    }

    public List<TbEqCvMst> findConveyorsByEqId(String eqId) {
        if (!StringUtils.hasText(eqId)) {
            return Collections.emptyList();
        }

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_id", eqId);
        condition.addOrder("level", true);
        condition.addOrder("id", true);

        return this.queryManager.selectList(TbEqCvMst.class, condition);
    }

    public List<TbEqCvMst> findConveyorsByEqIdAndFloor(String eqId, int floor) {
        if (!StringUtils.hasText(eqId)) {
            return Collections.emptyList();
        }

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_id", eqId);
        condition.addFilter("level", floor);
        condition.addFilter("use_yn", true);
        condition.addOrder("id", true);

        return this.queryManager.selectList(TbEqCvMst.class, condition);
    }

    public List<TbEqCarMst> findShuttlesByEqId(String eqId) {
        if (!StringUtils.hasText(eqId)) {
            return Collections.emptyList();
        }

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_id", eqId);

        return this.queryManager.selectList(TbEqCarMst.class, condition);
    }

    public List<TbEqCarMst> findShuttlesByEqIdAndFloor(String eqId, int floor) {
        if (!StringUtils.hasText(eqId)) {
            return Collections.emptyList();
        }

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_id", eqId);
        condition.addFilter("level", floor);
        condition.addOrder("eq_id", true);

        return this.queryManager.selectList(TbEqCarMst.class, condition);
    }

    public List<TbWcsShuttleOrder> findActiveOrders() {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("order_status", OrmConstants.LESS_THAN, 90);
        condition.addOrder("priority", true);
        condition.addOrder("created_at", false);

        return this.queryManager.selectList(TbWcsShuttleOrder.class, condition);
    }

    public List<TbWcsShuttleOrder> findErrorOrders() {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("order_status", 99);
        condition.addOrder("updated_at", false);

        return this.queryManager.selectList(TbWcsShuttleOrder.class, condition);
    }

    // 설비그룹 + 타입별 설비 목록 조회

    @Transactional(readOnly = true)
    public List<?> getEquipmentsByGroupAndType(String eqGroupId, int type) {
        List<TbEqMst> eqMstList = findEqMstByGroupAndType(eqGroupId, type);
        if (eqMstList.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> eqIds = new ArrayList<>();
        for (TbEqMst eqMst : eqMstList) {
            eqIds.add(eqMst.getId());
        }

        return switch (type) {
            case EQ_TYPE_RACK -> getRacksByEqIds(eqIds);
            case EQ_TYPE_CONVEYOR -> getConveyorsByEqIds(eqIds);
            case EQ_TYPE_SHUTTLE_CAR -> getShuttlesByEqIds(eqIds);
            default -> Collections.emptyList();
        };
    }

    private List<TbEqRackMst> getRacksByEqIds(List<String> eqIds) {
        List<TbEqRackMst> result = new ArrayList<>();
        for (String eqId : eqIds) {
            result.addAll(findRacksByEqId(eqId));
        }
        return result;
    }

    private List<TbEqCvMst> getConveyorsByEqIds(List<String> eqIds) {
        List<TbEqCvMst> result = new ArrayList<>();
        for (String eqId : eqIds) {
            result.addAll(findConveyorsByEqId(eqId));
        }
        return result;
    }

    private List<TbEqCarMst> getShuttlesByEqIds(List<String> eqIds) {
        List<TbEqCarMst> result = new ArrayList<>();
        for (String eqId : eqIds) {
            result.addAll(findShuttlesByEqId(eqId));
        }
        return result;
    }

    // 랙 셀 목록 조회

    @Transactional(readOnly = true)
    public List<TbEqRackMst> getRackCellsByGroupAndFloor(String eqGroupId, int floor) {
        List<TbEqMst> rackEqList = findEqMstByGroupAndType(eqGroupId, EQ_TYPE_RACK);
        if (rackEqList.isEmpty()) {
            return Collections.emptyList();
        }

        List<TbEqRackMst> result = new ArrayList<>();
        for (TbEqMst eq : rackEqList) {
            result.addAll(findRackCellsByEqIdAndFloor(eq.getId(), floor));
        }

        return result;
    }

    // 컨베이어 목록 조회

    @Transactional(readOnly = true)
    public List<TbEqCvMst> getConveyorsByGroup(String eqGroupId) {
        List<TbEqMst> cvEqList = findEqMstByGroupAndType(eqGroupId, EQ_TYPE_CONVEYOR);
        if (cvEqList.isEmpty()) {
            return Collections.emptyList();
        }

        List<TbEqCvMst> result = new ArrayList<>();
        for (TbEqMst eq : cvEqList) {
            result.addAll(findConveyorsByEqId(eq.getId()));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<TbEqCvMst> getConveyorsByGroupAndFloor(String eqGroupId, int floor) {
        List<TbEqMst> cvEqList = findEqMstByGroupAndType(eqGroupId, EQ_TYPE_CONVEYOR);
        if (cvEqList.isEmpty()) {
            return Collections.emptyList();
        }

        List<TbEqCvMst> result = new ArrayList<>();
        for (TbEqMst eq : cvEqList) {
            result.addAll(findConveyorsByEqIdAndFloor(eq.getId(), floor));
        }

        return result;
    }

    // 셔틀 목록 조회

    @Transactional(readOnly = true)
    public List<TbEqCarMst> getShuttlesByGroup(String eqGroupId) {
        List<TbEqMst> carEqList = findEqMstByGroupAndType(eqGroupId, EQ_TYPE_SHUTTLE_CAR);
        if (carEqList.isEmpty()) {
            return Collections.emptyList();
        }

        List<TbEqCarMst> result = new ArrayList<>();
        for (TbEqMst eq : carEqList) {
            result.addAll(findShuttlesByEqId(eq.getId()));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<TbEqCarMst> getShuttlesByGroupAndFloor(String eqGroupId, int floor) {
        List<TbEqMst> carEqList = findEqMstByGroupAndType(eqGroupId, EQ_TYPE_SHUTTLE_CAR);
        if (carEqList.isEmpty()) {
            return Collections.emptyList();
        }

        List<TbEqCarMst> result = new ArrayList<>();
        for (TbEqMst eq : carEqList) {
            result.addAll(findShuttlesByEqIdAndFloor(eq.getId(), floor));
        }

        return result;
    }

    // 주문 조회

    @Transactional(readOnly = true)
    public List<TbWcsShuttleOrder> getActiveOrders() {
        return findActiveOrders();
    }

    @Transactional(readOnly = true)
    public List<TbWcsShuttleOrder> getErrorOrders() {
        return findErrorOrders();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Transactional(readOnly = true)
    public Map getDashboardInitialData(String lcId, String pageId) {
        Map<String, Object> result = new HashMap<>();

        // 기본 응답
        result.put("lcId", lcId);
        result.put("pageId", pageId);
        result.put("eqGroupId", null);
        result.put("floorLevel", 1);

        result.put("shuttles", Collections.emptyList());
        result.put("conveyors", Collections.emptyList());
        result.put("cargos", Collections.emptyList());
        result.put("orders", Collections.emptyList());

        result.put("shuttleCount", 0);
        result.put("conveyorCount", 0);
        result.put("cargoCount", 0);
        result.put("orderCount", 0);

        // pageId 없으면 기본값 반환
        if (!StringUtils.hasText(pageId)) {
            return result;
        }

        // 1. 페이지 조회
        TbEcs2dPage page = this.queryManager.selectByCondition(TbEcs2dPage.class, ValueUtil.newMap("id", pageId));
        if (page == null) {
            return result;
        }

        // 2. lcId 검증 (들어온 lcId와 page의 lcId가 다르면 잘못된 요청으로 보고 기본값 반환)
        if (StringUtils.hasText(lcId) && StringUtils.hasText(page.getLcId()) && !lcId.equals(page.getLcId())) {
            return result;
        }

        String eqGroupId = page.getEqGroupId();
        int floorLevel = page.getFloorLevel() != null ? page.getFloorLevel() : 1;

        result.put("lcId", page.getLcId());
        result.put("pageId", page.getId());
        result.put("eqGroupId", eqGroupId);
        result.put("floorLevel", floorLevel);

        // eqGroupId 없으면 페이지 정보만 반환
        if (!StringUtils.hasText(eqGroupId)) {
            return result;
        }

        // 3. 대시보드 데이터 조회
        List<TbEqCarMst> shuttles = getShuttlesByGroupAndFloor(eqGroupId, floorLevel);
        List<TbEqCvMst> conveyors = getConveyorsByGroupAndFloor(eqGroupId, floorLevel);
        List<TbWcsShuttleOrder> orders = getActiveOrders();

        // 4. 응답 구성
        result.put("shuttles", shuttles);
        result.put("conveyors", conveyors);
        result.put("cargos", Collections.emptyList());
        result.put("orders", orders);

        result.put("shuttleCount", shuttles.size());
        result.put("conveyorCount", conveyors.size());
        result.put("cargoCount", 0);
        result.put("orderCount", orders.size());

        return result;
    }
}