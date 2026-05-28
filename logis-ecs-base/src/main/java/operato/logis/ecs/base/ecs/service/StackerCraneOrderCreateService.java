package operato.logis.ecs.base.ecs.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import operato.logis.ecs.base.ecs.domain.enums.EcsDBConsts;
import operato.logis.ecs.base.ecs.entity.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.util.ValueUtil;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class StackerCraneOrderCreateService {

    private final IQueryManager iQueryManager;
    private Map<String, String> cvEqIdMap = new HashMap<>();
    private Map<String, String> rackEqIdMap = new HashMap<>();
    private Map<String, List<TbEqCvMst>> eqCvMstMap = new HashMap<>();
    private Map<String, List<TbEqRackMst>> eqRackMstMap = new HashMap<>();

    public StackerCraneOrderCreateService(IQueryManager iQueryManager) {
        this.iQueryManager = iQueryManager;
    }

    /** 필요 설비 정보 조회 */
    private void init() {
        List<TbEqGroupMst> eqGroupList = selectEqGroupMst();
        // size = 2, 저온 상온
        eqGroupList.forEach(eqGroupMst -> {
            String eqGroudpId = eqGroupMst.getId();
            String rackEqId = selectEqId(eqGroudpId, EcsDBConsts.EqType.RACK);
            String cvEqId = selectEqId(eqGroudpId, EcsDBConsts.EqType.CONVEYOR);
            List<TbEqCvMst> eqCvMstList = selectEqCvMst(cvEqId);
            List<TbEqRackMst> eqRackMstList = selectEqRackMst(rackEqId);

            rackEqIdMap.put(eqGroudpId, rackEqId);
            cvEqIdMap.put(eqGroudpId, cvEqId);
            eqRackMstMap.put(eqGroudpId, eqRackMstList);
            eqCvMstMap.put(eqGroudpId, eqCvMstList);
        });
        // log.info("MovexOrderCreateService init complete");
    }

    public void createOrder() {
        createWcsOrder();
        createCraneMoveHomeOrder();
    }

    @Transactional(rollbackFor = Exception.class)
    public void createWcsOrder() {
        init();
        createCraneMoveHomeOrder();
        List<TbWcsCraneOrder> createOrderList = selectWcsCraneOrder();
        if (!createOrderList.isEmpty()) {
            log.info("MovexOrderCreateService - createOrder, select wcs order : " + createOrderList.size());
            createOrderList.sort(Comparator.comparing(TbWcsCraneOrder::getPriority));
            createOrderList.forEach(wcsOrder -> {
                String eqGroupId = wcsOrder.getEqGroupId();
                String rackEqId = rackEqIdMap.get(eqGroupId);
                String cvEqId = cvEqIdMap.get(eqGroupId);
                List<TbEqRackMst> eqRackMstList = eqRackMstMap.get(eqGroupId);
                List<TbEqCvMst> eqCvMstList = eqCvMstMap.get(eqGroupId);

                if (eqRackMstList.isEmpty() || eqCvMstList.isEmpty()) {
                    log.warn("MovexOrderCreateService - eqRackMstList eqCvMstList eq group id FAIL");
                    return;
                }
                // TODO : 설비 지시 생성시 현장의 입출고 포트의 개수에 따라 변경 필요
                List<TbEcsRackOrder> rackOrderList = TbEcsRackOrder.onlyOnePortOf(wcsOrder, rackEqId, eqRackMstList);
                if (rackOrderList.isEmpty()) {
                    log.warn("MovexOrderCreateService - create Rack Order FAIL");
                }
                TbEcsRouteOrder routeOrder = TbEcsRouteOrder.onlyOnePortOf(wcsOrder, cvEqId, eqCvMstList);
                if (routeOrder == null)
                    log.warn("MovexOrderCreateService - create Route Order FAIL");

                wcsOrder.setEcsIfStatus(EcsDBConsts.EcsIfStatus.RECEIVE.getValue());
                try {
                    createEcsOrderAndUpdateWcsOrder(rackOrderList, routeOrder, wcsOrder);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void createCraneMoveHomeOrder() {
        List<TbEqCraneMst> craneMstList = selectCraneStatus();
        craneMstList.forEach(craneMst -> {
            boolean needCreateOrder = craneMst.isReqMoveHomeYn();
            if (needCreateOrder) {
                List<TbEqRackMst> rackMstList = selectEqRackMst(craneMst.getRackEqId());
                TbEqRackMst bufferRackMst = rackMstList.stream().filter(mst -> mst.isBufferYn()).findFirst().orElse(null);
                if (bufferRackMst != null) {
                    TbEcsRackOrder rackOrder = TbEcsRackOrder.craneHomeMoveOrder(bufferRackMst.getRackId(), craneMst.getLevel(), craneMst.getRackEqId(), craneMst.getId());
                    log.info("MovexOrderCreateService - create CraneMoveHomeOrder : " + craneMst.getId());
                    iQueryManager.insert(rackOrder);
                    craneMst.setReqMoveHomeYn(false);
                    iQueryManager.update(craneMst);
                }
            }
        });
    }

    protected void createEcsOrderAndUpdateWcsOrder(List<TbEcsRackOrder> rackOrderList, TbEcsRouteOrder routeOrder, TbWcsCraneOrder wcsOrder) throws JsonProcessingException {
        if (!rackOrderList.isEmpty())
            rackOrderList.stream().forEach(rackOrder -> iQueryManager.insert(rackOrder));
        if (routeOrder != null)
            iQueryManager.insert(routeOrder);
        if (wcsOrder != null)
            iQueryManager.update(wcsOrder);
    }

    public List<TbEqGroupMst> selectEqGroupMst() {
        String sql = "SELECT * FROM tb_eq_group_mst;";
        return iQueryManager.selectListBySql(sql, null, TbEqGroupMst.class, 0, 0);
    }

    private List<TbEqCraneMst> selectCraneStatus() {
        String sql = "SELECT * FROM tb_eq_crane_mst;";
        return iQueryManager.selectListBySql(sql, null, TbEqCraneMst.class, 0, 0);
    }

    private List<TbWcsCraneOrder> selectWcsCraneOrder() {
        int status = EcsDBConsts.EcsIfStatus.READY.getValue();
        String sql = """
                SELECT * 
                FROM tb_wcs_crane_order 
                WHERE ecs_if_status = :status;
                """;
        Map<String, Object> params = ValueUtil.newMap("status", status);
        return iQueryManager.selectListBySql(sql, params, TbWcsCraneOrder.class, 0, 0);
    }

    private List<TbEqRackMst> selectEqRackMst(String rackEqId) {
        String sql = """
                SELECT * 
                FROM tb_eq_rack_mst 
                WHERE eq_id = :eqId;
                """;
        Map<String, Object> params = ValueUtil.newMap("eqId", rackEqId);
        return iQueryManager.selectListBySql(sql, params, TbEqRackMst.class, 0, 0);
    }

    private List<TbEqCvMst> selectEqCvMst(String eqId) {
        String sql = """
                SELECT * 
                FROM tb_eq_cv_mst 
                WHERE eq_id = :eqId;
                """;
        Map<String, Object> params = ValueUtil.newMap("eqId", eqId);
        return iQueryManager.selectListBySql(sql, params, TbEqCvMst.class, 0, 0);
    }

    private String selectEqId(String eqGroupId, EcsDBConsts.EqType eqType) {
        int type = eqType.getValue();
        String sql = """
                SELECT id 
                FROM tb_eq_mst 
                WHERE eq_group_id = :eqGroupId 
                    AND type = :type;
                """;
        Map<String, Object> params = ValueUtil.newMap("eqGroupId,type", eqGroupId, type);
        return iQueryManager.selectBySql(sql, params, String.class);
    }
}
