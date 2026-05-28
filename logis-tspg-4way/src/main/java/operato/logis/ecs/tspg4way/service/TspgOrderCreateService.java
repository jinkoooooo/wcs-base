package operato.logis.ecs.tspg4way.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import operato.logis.ecs.tspg4way.domain.enums.EcsDBConsts;
import operato.logis.ecs.tspg4way.entity.*;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.util.ValueUtil;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@Slf4j
public class TspgOrderCreateService {
    private final IQueryManager iQueryManager;
    private Map<String, String> cvEqIdMap = new HashMap<>();
    private Map<String, String> rackEqIdMap = new HashMap<>();
    private Map<String, List<TbEqCvMst>> eqCvMstMap =  new HashMap<>();
    private Map<String, List<TbEqRackMst>> eqRackMstMap =  new HashMap<>();

    public TspgOrderCreateService(IQueryManager iQueryManager) {
        this.iQueryManager = iQueryManager;
    }

    /**
     * 필요 설비 정보 조회
     */
    private void init(){
        List<TbEqGroupMst> eqGroupList = selectEqGroupMst();
        // size = 2, 저온 상온
        eqGroupList.forEach(eqGroupMst -> {
            String eqGroudpId = eqGroupMst.getId();
            String rackEqId = selectEqId(eqGroudpId, EcsDBConsts.EqType.RACK);
            String cvEqId = selectEqId(eqGroudpId, EcsDBConsts.EqType.CONVEYOR);
            List<TbEqCvMst> eqCvMstList =  selectEqCvMst(cvEqId);
            List<TbEqRackMst> eqRackMstList = selectEqRackMst(rackEqId);

            rackEqIdMap.put(eqGroudpId, rackEqId);
            cvEqIdMap.put(eqGroudpId, cvEqId);
            eqRackMstMap.put(eqGroudpId, eqRackMstList);
            eqCvMstMap.put(eqGroudpId, eqCvMstList);
        });
        // log.info("TspgOrderCreateService init complete");
    }

    public void createOrder(){
        createWcsOrder();
        createCarMoveHomeOrder();
    }

    @Transactional(rollbackFor = Exception.class)
    public void createWcsOrder(){
        init();
        createCarMoveHomeOrder();
        List<TbWcsShuttleOrder> createOrderList = selectWcsShuttleOrder();
        if(!createOrderList.isEmpty()){
            log.info("TspgOrderCreateService - createOrder, select wcs order : " + createOrderList.size());
            createOrderList.sort(Comparator.comparing(TbWcsShuttleOrder::getPriority));
            createOrderList.forEach(wcsOrder -> {
                String eqGroupId = wcsOrder.getEqGroupId();
                String rackEqId = rackEqIdMap.get(eqGroupId);
                String cvEqId = cvEqIdMap.get(eqGroupId);
                List<TbEqRackMst>  eqRackMstList =  eqRackMstMap.get(eqGroupId);
                List<TbEqCvMst>  eqCvMstList =  eqCvMstMap.get(eqGroupId);

                if(eqRackMstList.isEmpty() ||  eqCvMstList.isEmpty()){
                    log.warn("TspgOrderCreateService - eqRackMstList eqCvMstList eq group id FAIL");
                    return;
                }
                // TODO : 설비 지시 생성시 현장의 입출고 포트의 개수에 따라 변경 필요
                List<TbEcsRackOrder> rackOrderList = TbEcsRackOrder.onlyOnePortOf(wcsOrder, rackEqId, eqRackMstList);
                if(rackOrderList.isEmpty()){
                    log.warn("TspgOrderCreateService - create Rack Order FAIL");
                }
                TbEcsRouteOrder routeOrder = TbEcsRouteOrder.onlyOnePortOf(wcsOrder, cvEqId, eqCvMstList);
                if(routeOrder == null)
                    log.warn("TspgOrderCreateService - create Route Order FAIL");

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
    public void createCarMoveHomeOrder(){
        List<TbEqCarMst> carMstList = selectCarStatus();
        carMstList.forEach(carMst -> {
            boolean needCreateOrder = carMst.isReqMoveHomeYn();
            if(needCreateOrder){
                List<TbEqRackMst> rackMstList = selectEqRackMst(carMst.getRackEqId());
                TbEqRackMst bufferRackMst = rackMstList.stream().filter(mst -> mst.isBufferYn()).findFirst().orElse(null);
                if(bufferRackMst != null){
                    TbEcsRackOrder rackOrder = TbEcsRackOrder.carHomeMoveOrder(bufferRackMst.getRackId(), carMst.getLevel(), carMst.getRackEqId(), carMst.getId());
                    log.info("TspgOrderCreateService - create CarMoveHomeOrder : "+carMst.getId());
                    iQueryManager.insert(rackOrder);
                    carMst.setReqMoveHomeYn(false);
                    iQueryManager.update(carMst);
                }
            }
        });
    }
    protected void createEcsOrderAndUpdateWcsOrder(List<TbEcsRackOrder> rackOrderList, TbEcsRouteOrder routeOrder, TbWcsShuttleOrder wcsOrder) throws JsonProcessingException {
//        ObjectMapper mapper = new ObjectMapper();
//        log.info(mapper.writeValueAsString(rackOrderList));
//        log.info("=========");
//        log.info(mapper.writeValueAsString(routeOrder));
//        log.info("=========");
//        log.info(mapper.writeValueAsString(wcsOrder));
        if(!rackOrderList.isEmpty())
            rackOrderList.stream().forEach(rackOrder->
                    iQueryManager.insert(rackOrder)
            );
        if(routeOrder != null)
            iQueryManager.insert(routeOrder);
        if(wcsOrder != null)
            iQueryManager.update(wcsOrder);
    }


    public List<TbEqGroupMst> selectEqGroupMst() {
        String sql = """
                SELECT *
                FROM tb_eq_group_mst
                """;
        return iQueryManager.selectListBySql(sql, null, TbEqGroupMst.class, 0, 0);
    }

    private List<TbEqCarMst> selectCarStatus(){
        String sql = """
                select * from tb_eq_car_mst
                """;
        return iQueryManager.selectListBySql(sql, null, TbEqCarMst.class, 0,0);
    }

    private List<TbWcsShuttleOrder> selectWcsShuttleOrder(){
        int status = EcsDBConsts.EcsIfStatus.READY.getValue();
        String sql = """
                SELECT *
                FROM tb_wcs_shuttle_order
                WHERE ecs_if_status = :status
                """;
        Map<String, Object> params = ValueUtil.newMap("status", status);
        return iQueryManager.selectListBySql(sql, params, TbWcsShuttleOrder.class, 0,0);
    }


    private List<TbEqRackMst> selectEqRackMst(String rackEqId) {
        String sql = """
                SELECT *
                FROM tb_eq_rack_mst
                WHERE eq_id = :eqId
                """;
        Map<String, Object> params = ValueUtil.newMap("eqId", rackEqId);
        return iQueryManager.selectListBySql(sql, params, TbEqRackMst.class, 0,0);
    }

    private List<TbEqCvMst> selectEqCvMst(String eqId){
        String sql = """
                SELECT *
                FROM tb_eq_cv_mst
                WHERE eq_id = :eqId
                """;
        Map<String, Object> params = ValueUtil.newMap("eqId", eqId);
        return iQueryManager.selectListBySql(sql, params, TbEqCvMst.class, 0,0);
    }

    private String selectEqId(String eqGroupId, EcsDBConsts.EqType eqType){
        int type = eqType.getValue();
        String sql = """
                SELECT id
                FROM tb_eq_mst
                WHERE eq_group_id = :eqGroupId
                AND type = :type
                """;
        Map<String, Object> params = ValueUtil.newMap("eqGroupId,type", eqGroupId, type);
        return iQueryManager.selectBySql(sql, params, String.class);
    }

}
