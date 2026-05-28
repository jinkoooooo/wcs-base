package operato.logis.ecs.base.ecs.service.common;

import lombok.AllArgsConstructor;
import operato.logis.ecs.base.ecs.domain.crane.StackerCraneContext;
import operato.logis.ecs.base.ecs.domain.enums.EcsDBConsts;
import operato.logis.ecs.base.ecs.entity.TbEqMst;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Map;

@Service
@AllArgsConstructor
public class TbEqMstService extends AbstractQueryService {

    /** 검토중) 현재 랙 그룹의 컨베이어 설비 정보 조회 */
    public TbEqMst selectEqMst(StackerCraneContext context) {
        String eqId = context.getRackEqId();
        int type = EcsDBConsts.EqType.CONVEYOR.getValue();
        String sql = """
                SELECT * 
                FROM tb_eq_mst
                WHERE eq_group_id IN (
                        SELECT eq_group_id
                        FROM tb_eq_mst
                        WHERE id = :eqId
                    )
                    AND type = :type;
                """;
        Map<String, Object> params = ValueUtil.newMap("eqId,type", eqId, type);
        return queryManager.selectBySql(sql, params, TbEqMst.class);
    }
}