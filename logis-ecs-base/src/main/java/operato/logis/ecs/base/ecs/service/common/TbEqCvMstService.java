package operato.logis.ecs.base.ecs.service.common;

import lombok.extern.slf4j.Slf4j;
import operato.logis.ecs.base.ecs.domain.crane.StackerCraneContext;
import operato.logis.ecs.base.ecs.domain.enums.EcsDBConsts;
import operato.logis.ecs.base.ecs.entity.TbEqCvMst;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TbEqCvMstService extends AbstractQueryService {

    /** 검토중) 현재 랙의 연결된 컨베이어 중 화물예약 상태인 컨베이어 조회 */
    public List<TbEqCvMst> selectRackInCvMoveReserveStatus(StackerCraneContext context) {
        int type = EcsDBConsts.ConveyorType.INBOUND.getValue();
        int status = EcsDBConsts.EqConveyorStatus.MOVE_RESERVE.getValue();
        String sql = """
                SELECT * 
                FROM tb_eq_cv_mst
                WHERE eq_id = :eqCvId
                    AND type = :type
                    AND status = :status
                    AND asiel IN (:asiel1, :asiel2);
                """;
        Map<String, Object> params = ValueUtil.newMap("eqCvId,type,status,asiel1,asiel2", context.getCvEqId(), type, status, context.getAsiel1(), context.getAsiel2());
        return queryManager.selectListBySql(sql, params, TbEqCvMst.class, 0, 0);
    }

    /** 검토중) 현재 랙의 연결된 컨베이어의 화물 예약 상태인 랙단컨베이어 조회 */
    public List<TbEqCvMst> selectRackInCvStatus(StackerCraneContext context) {
        String eqId = context.getCvEqId();
        int type = EcsDBConsts.ConveyorType.INBOUND.getValue();
        String sql = """
                SELECT *
                FROM tb_eq_cv_mst
                WHERE eq_id = :eqCvId
                    AND type = :type
                    AND asiel IN (:asiel1, :asiel2);
                """;
        Map<String, Object> params = ValueUtil.newMap("eqCvId,type,asiel1,asiel2", eqId, type, context.getAsiel1(), context.getAsiel2());
        return queryManager.selectListBySql(sql, params, TbEqCvMst.class, 0, 0);
    }

    // 현재 랙의 연결된 컨베이어의 화물 예약 상태인 랙단컨베이어 조히
    public List<TbEqCvMst> selectRackInCvAllStatus(StackerCraneContext context) {
        String eqCvId = context.getCvEqId();
        String sql = """
                SELECT * 
                FROM tb_eq_cv_mst
                WHERE eq_id = :eqCvId;
                """;
        Map<String, Object> params = ValueUtil.newMap("eqCvId", eqCvId);
        return queryManager.selectListBySql(sql, params, TbEqCvMst.class, 0, 0);
    }
}