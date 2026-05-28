package operato.logis.posco.service;

import operato.logis.posco.consts.PoscoConstants;
import operato.logis.posco.dto.McsMonitoringDetailDto;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Map;

@Service
public class McsMonitoringService extends AbstractQueryService {

    public McsMonitoringDetailDto getMonitoringDetail(String equipType, String equipCode) {
        if (PoscoConstants.AGV.equals(equipType) || PoscoConstants.AGF.equals(equipType)) {
            return getEquipDetail(equipType, equipCode);
        }

        return null;
    }

    private McsMonitoringDetailDto getEquipDetail(String equipType, String equipCode) {
        String sql = """
                SELECT
                    e.equip_type,
                    e.equip_code,
                    e.status,
                    e.position_x,
                    e.position_y,
                    e.rotation,
                    e.battery,
                    e.pod_code,
                    e.pod_type,
                    t.task_id,
                    t.task_type,
                    t.task_priority,
                    t.stock_id,
                    t.start_point_cd,
                    t.end_point_cd,
                    t.process_status,
                    t.accept_datetime,
                    t.start_datetime,
                    t.loading_datetime,
                    t.complete_datetime,
                    t.error_code,
                    t.error_msg
                FROM tb_mcs_equip e
                LEFT JOIN tb_mcs_task t ON e.task_id = t.task_id
                WHERE e.equip_type = :equipType
                  AND e.equip_code = :equipCode
                """;
        Map<String, Object> param = ValueUtil.newMap("equipType,equipCode", equipType, equipCode);
        return this.queryManager.selectBySql(sql, param, McsMonitoringDetailDto.class);
    }
}