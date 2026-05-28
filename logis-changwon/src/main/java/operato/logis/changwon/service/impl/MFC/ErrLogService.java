package operato.logis.changwon.service.impl.MFC;

import operato.logis.changwon.dto.DashboardErrorAlarm;
import operato.logis.changwon.entity.MFC.ErrLog;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.List;

@Service
public class ErrLogService extends AbstractQueryService {

    public List<DashboardErrorAlarm> getErrorAlarm() {
        String sql = """
                SELECT
                    EL.ID,
                    EL.IS_CHECKED,
                    WT.TASK_NO,
                    WT.TASK_ID,
                    EL.ERROR_MACHINE,
                    EL.ERROR_CODE,
                    DEF.ERROR_NAME,
                    CASE WT.ORDER_KIND
                        WHEN '1' THEN '입고'
                        WHEN '2' THEN '출고'
                        WHEN '3' THEN '재고 이동'
                        WHEN '4' THEN '작업 삭제'
                        WHEN '8' THEN '강제 입고'
                        WHEN '9' THEN '재고 이동'
                        ELSE '알 수 없음'
                    END AS ORDER_KIND,
                    WT.START_POINT_CD,
                    WT.END_POINT_CD
                FROM
                    C_ERR_LOG EL
                JOIN
                    C_JOB_ODR JO ON (EL.ORDER_ID = JO.ORDER_ID AND EL.JOB_NO = JO.JOB_NO)
                JOIN
                    WCS_TASK WT ON (JO.WMS_ORD_NO = WT.TASK_ID AND JO.ORDER_ID = WT.TASK_NO)
                LEFT JOIN (
                    SELECT DISTINCT ON (ERROR_CODE)
                        ERROR_CODE,
                        ERROR_NAME
                    FROM
                        C_ERR_DEF
                    ORDER BY
                        ERROR_CODE ASC,
                        ERROR_MACHINE ASC
                ) DEF ON EL.ERROR_CODE = DEF.ERROR_CODE
                WHERE
                    WT.PROCESS_STATUS < 33
                    AND EL.IS_CHECKED = FALSE
                    AND NOT EXISTS (
                        SELECT 1
                        FROM EXCEPTION_ERROR_CODE EXC
                        WHERE EXC.ERROR_CODE = EL.ERROR_CODE
                    )
                """;
        return this.queryManager.selectListBySql(sql, null, DashboardErrorAlarm.class, 0, 0);
    }

    public Boolean checkErrorAlarm(List<ErrLog> errorList) {
        for (ErrLog error : errorList) {
            error.setIsChecked(true);
            this.queryManager.update(error, "isChecked");
        }

        return true;
    }
}