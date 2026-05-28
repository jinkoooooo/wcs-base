package operato.logis.connector.gtr.service;

import operato.logis.connector.gtr.entity.*;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Map;

@Service
public class TokenService extends AbstractQueryService {

    public GtrToken getToken() {
        Domain.setCurrentDomain(new Domain("7"));

        String sql = "select * from samsung_mw.tb_mw_gtr_token";
        return this.queryManager.selectBySql(sql, null, GtrToken.class);
    }

    public VisionValue getVisionData(String seqno) {
        Domain.setCurrentDomain(new Domain("7"));

        String sql = "SELECT A2.seqno, A1.seqno AS dev1_seqno, A2.barcodedata, " +
                "       B.filenametop, B.filenamefront, B.filenameback, " +
                "       B.filenameleft, B.filenameright, " +
                "       B.filenamebottomleft, B.filenamebottomright " +
                "FROM samsung_mw.tb_mw_bcr_Data A2 " +
                "JOIN ( " +
                "    SELECT seqno, barcodedata, " +
                "           ROW_NUMBER() OVER(PARTITION BY barcodedata ORDER BY reg_dt DESC) as rn " +
                "    FROM samsung_mw.tb_mw_bcr_Data " +
                "    WHERE device_name = 'BCR-01' " +
                ") A1 ON A2.barcodedata = A1.barcodedata AND A1.rn = 1 " +
                "JOIN samsung_mw.tb_mw_vision_data B ON A1.seqno = B.seqno " +
                "WHERE A2.device_name = 'BCR-02' " +
                "  AND A2.seqno = :seqno " +
                "ORDER BY B.reg_dt DESC " +
                "LIMIT 1";

        Map<String, Object> param = ValueUtil.newMap("seqno", seqno);
        return this.queryManager.selectBySql(sql, param, VisionValue.class);
    }

    public GtrInspectionResult getResultInspection(String transactionId) {
        Domain.setCurrentDomain(new Domain("7"));

        String sql = "select * from samsung_mw.tb_mw_gtr_inspection_results where transaction_id = :transactionId";

        Map<String, Object> param = ValueUtil.newMap("transactionId", transactionId);

        return this.queryManager.selectBySql(sql, param, GtrInspectionResult.class);
    }

}
