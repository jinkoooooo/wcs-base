package operato.logis.samsung.service.wcs;

import operato.logis.samsung.entity.wcs.TbMwUnitErrorLog;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class UnitErrorLogService extends AbstractQueryService {

    /**
     * 특정 유닛의 에러 이력을 저장하는 메소드
     *
     * @param unitType 유닛의 유형 (SYS, AGV, AGF, ...)
     * @param unitCode 시스템 이름 혹은 설비 번호 (RCS, ACS, 1, 2, ...)
     * @param errorCode 유닛의 에러 코드
     * @param errorMsg 에러 코드에 대한 메시지
     */
    public void createErrorLog(String unitType, String unitCode, String errorCode, String errorMsg) {
        TbMwUnitErrorLog errorLog = new TbMwUnitErrorLog();
        errorLog.setUnitType(unitType);
        errorLog.setUnitCode(unitCode);
        errorLog.setErrorCode(errorCode);
        errorLog.setErrorMsg(errorMsg);
        this.queryManager.insert(errorLog);
    }

    public List<TbMwUnitErrorLog> getErrorLogByDate(Date chooseDate) {
        String sql = "select * from tb_mw_unit_error_log where created_at::date = :chooseDate order by created_at asc";
        Map<String, Object> param = ValueUtil.newMap("chooseDate", chooseDate);
        return this.queryManager.selectListBySql(sql, param, TbMwUnitErrorLog.class, 0, 0);
    }
}