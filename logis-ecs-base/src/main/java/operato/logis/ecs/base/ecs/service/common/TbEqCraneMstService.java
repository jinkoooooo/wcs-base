package operato.logis.ecs.base.ecs.service.common;

import lombok.extern.slf4j.Slf4j;
import operato.logis.ecs.base.ecs.domain.crane.StackerCraneContext;
import operato.logis.ecs.base.ecs.entity.TbEqCraneMst;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TbEqCraneMstService extends AbstractQueryService {

    /** 검토완료) 설비ID기반 크레인 조회 */
    public TbEqCraneMst selectByCraneId(StackerCraneContext context, String eqId) {
        List<TbEqCraneMst> craneMsts = selectCraneStatus(context);

        List<TbEqCraneMst> curCranes = craneMsts.stream().filter(craneMst ->
                        craneMst.getRackEqId().equals(context.getRackEqId()))
                .collect(Collectors.toList());

        TbEqCraneMst craneMst = curCranes.stream().filter(crane ->
                crane.getEqId().equals(eqId)).findFirst().orElse(null);
        if (craneMst == null) {
            log.info("readyOrderManager craneMst NULL!");
            return null;
        }
        return craneMst;
    }

    /** 검토완료) 현재 랙의 스태커크레인 상태 조회 */
    public List<TbEqCraneMst> selectCraneStatus(StackerCraneContext context) {
        String sql = """
                SELECT * 
                FROM tb_eq_crane_mst
                WHERE rack_eq_id = rackEqId
                    AND asiel IN (:asiel1, :asiel2);
                """;
        Map<String, Object> params = ValueUtil.newMap("rackEqId,asiel1,asiel2", context.getRackEqId(), context.getAsiel1(), context.getAsiel2());
        return queryManager.selectListBySql(sql, params, TbEqCraneMst.class, 0, 0);
    }

    /** 검토완료) 현재 랙, 현재 asiel의 다른 스태커크레인 조회 */
    public List<TbEqCraneMst> selectOtherCraneStatus(StackerCraneContext context, String craneId) {
        String rackEqId = context.getRackEqId();
        String sql = """
                SELECT * 
                FROM tb_eq_car_mst
                WHERE where rack_eq_id = :rackEqId
                    AND asiel IN (:asiel1, :asiel2)
                    AND id <> :craneId;
                """;
        Map<String, Object> params = ValueUtil.newMap("rackEqId,asiel1,asiel2,craneId", rackEqId, context.getAsiel1(), context.getAsiel2(), craneId);
        return queryManager.selectListBySql(sql, params, TbEqCraneMst.class, 0, 0);
    }

    // 현재랙의 현재 통로의 크레인 상태 조회
    public TbEqCraneMst selectCraneStatus(StackerCraneContext context, String craneId) {
        String rackEqId = context.getRackEqId();
        String id = craneId;
        String sql = """
                SELECT * 
                FROM tb_eq_car_mst
                WHERE rack_eq_id = :rackEqId
                    AND asiel IN (:asiel1, :asiel2)
                    AND id = :id;
                """;
        Map<String, Object> params = ValueUtil.newMap("rackEqId,asiel1,asiel2,id", rackEqId, context.getAsiel1(), context.getAsiel2(), id);
        return queryManager.selectBySql(sql, params, TbEqCraneMst.class);
    }
}