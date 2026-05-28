package operato.logis.ecs.base.ecs.service.common;

import lombok.extern.slf4j.Slf4j;
import operato.logis.ecs.base.ecs.domain.cell.CraneCell;
import operato.logis.ecs.base.ecs.domain.crane.StackerCraneContext;
import operato.logis.ecs.base.ecs.domain.enums.EcsDBConsts;
import operato.logis.ecs.base.ecs.entity.TbEqRackMst;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TbEqRackMstService extends AbstractQueryService {

    /** 현재랙의 셀 상태 조회 */
    public List<TbEqRackMst> selectCellStatus(StackerCraneContext context) {
        String sql = """
                SELECT * 
                FROM tb_eq_rack_mst
                WHERE eq_id = :rackEqId
                    AND asiel IN (:asiel1, :asiel2);
                """;
        Map<String, Object> params = ValueUtil.newMap("rackEqId,asiel1,asiel2", context.getRackEqId(), context.getAsiel1(), context.getAsiel2());
        return queryManager.selectListBySql(sql, params, TbEqRackMst.class, 0, 0);
    }

    /** 셀 상태 업데이트 */
    public void updateTbEqRackMstStatus(StackerCraneContext context, List<CraneCell> craneCells, EcsDBConsts.EqRackStatus eqRackCmdStatus) {
        List<TbEqRackMst> rackMstList = selectCellStatus(context);

        craneCells.stream().forEach(cell -> {

            TbEqRackMst rackMst = rackMstList.stream()
                    .filter(rack ->
                            rack.getAsiel() == cell.getAsiel()
                                    && rack.getBay() == cell.getBay()
                                    && rack.getLevel() == cell.getLevel()
                                    && rack.getSide() == cell.getSide()
                    )
                    .findFirst().orElse(null);

            if (rackMst != null) {
                log.info("update cell Status : " + rackMst.getRackId() + ", " + eqRackCmdStatus.getDescription());
                rackMst.setStatus(eqRackCmdStatus.getValue());
                this.queryManager.update(rackMst);
            }
        });
    }

    /**
     * 랙 적재상태 변경
     * - todo: cargoYn을 별도로 관리해야하는지
     */
    public void updateTbEqRackMstStatus(StackerCraneContext context, String rackId, EcsDBConsts.EqRackStatus eqRackCmdStatus) {
        List<TbEqRackMst> rackMstList = selectCellStatus(context);

        TbEqRackMst rackMst = rackMstList.stream().filter(cell -> cell.getRackId().equals(rackId)).findFirst().orElse(null);
        if (rackMst != null) {
            if (eqRackCmdStatus.getValue() == EcsDBConsts.EqRackStatus.CARGO.getValue())
                rackMst.setCargoYn(true);
            else if (eqRackCmdStatus.getValue() == EcsDBConsts.EqRackStatus.READY.getValue())
                rackMst.setCargoYn(false);

            rackMst.setStatus(eqRackCmdStatus.getValue());

            queryManager.update(rackMst);
            log.info("update cell Status" + "[" + rackId + "] : " + eqRackCmdStatus.getDescription());
        }
    }
}