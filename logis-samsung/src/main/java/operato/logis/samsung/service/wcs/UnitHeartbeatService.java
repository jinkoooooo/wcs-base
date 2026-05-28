package operato.logis.samsung.service.wcs;

import operato.logis.samsung.entity.wcs.TbMwUnitHeartbeat;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Map;

@Service
public class UnitHeartbeatService extends AbstractQueryService {

    /**
     * 특정 유닛의 상태를 업데이트하는 메소드
     *
     * @param unitType 유닛의 유형 (SYS, AGV, AGF, ...)
     * @param unitCode 시스템 이름 혹은 설비 번호 (RCS, ACS, 1, 2, ...)
     * @param status   유닛의 상태 코드
     * @param msg      상태 코드에 대한 메시지
     */
    public void updateUnitStatus(String unitType, String unitCode, String status, String msg) {
        // 이미 존재하는 객체인지 조회
        String sql = "select * from tb_mw_unit_heartbeat where unit_type = :unitType and unit_code = :unitCode";
        Map<String, Object> param = ValueUtil.newMap("unitType,unitCode", unitType, unitCode);
        TbMwUnitHeartbeat unit = this.queryManager.selectBySql(sql, param, TbMwUnitHeartbeat.class);

        // 새로운 객체로 등록
        if (ValueUtil.isEmpty(unit)) {
            TbMwUnitHeartbeat newUnit = new TbMwUnitHeartbeat();
            newUnit.setUnitType(unitType);
            newUnit.setUnitCode(unitCode);
            newUnit.setStatus(status);
            newUnit.setMsg(msg);
            this.queryManager.insert(newUnit);
        }
        // 기존 객체 상태 업데이트
        else {
            // 상태가 변하지 않았다면 종료
            if (unit.getStatus().equals(status)) {
                return;
            }

            unit.setStatus(status);
            unit.setMsg(msg);
            this.queryManager.update(unit, "status", "msg");
        }
    }

    /**
     * 전체 유닛 상태 조회
     *
     * @return 전체 UnitStatus 리스트
     */
    public List<TbMwUnitHeartbeat> getAllUnitStatus() {
        String sql = "select unit_code unit_type, msg,\n" +
                "    coalesce(instance_id, unit_code) unit_code,\n" +
                "    CASE\n" +
                "        WHEN status  = '0' THEN '0'\n" +
                "        WHEN status  IN ('9', '99') THEN '9'\n" +
                "        ELSE '9'\n" +
                "        END AS status\n" +
                "from tb_mw_unit_heartbeat\n" +
                "order by unit_type desc, unit_code asc";
        return this.queryManager.selectListBySql(sql, null, TbMwUnitHeartbeat.class, 0, 0);
    }
}