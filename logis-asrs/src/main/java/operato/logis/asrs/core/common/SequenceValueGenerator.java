package operato.logis.asrs.core.common;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import xyz.elidom.sys.system.service.AbstractQueryService;

@Component
public class SequenceValueGenerator extends AbstractQueryService {

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyyMMdd");
    private static final String STOCK_UNIT_NO_DEFAULT_PREFIX = "SU";
    private static final String REF_DOC_NO_DEFAULT_PREFIX = "INB";

    public String refDocNoGenerate(String refDocType) {
        if (refDocType == null || refDocType.isBlank()) {
            return REF_DOC_NO_DEFAULT_PREFIX;
        }

        String normalized = refDocType.trim()
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase();

        String prefix = normalized.isEmpty() ? REF_DOC_NO_DEFAULT_PREFIX : normalized;

        long seq = nextSequenceValue("logis_asrs.seq_ac_ref_doc_no");
        String ymd = DATE_FMT.format(new Date());

        return String.format("%s-%s-%09d", prefix, ymd, seq);
    }


    public String stockUnitNoGenerate(String areaCode) {
        if (areaCode == null || areaCode.isBlank()) {
            return "AREA";
        }

        String normalized = areaCode.trim()
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase();

        if (normalized.isEmpty()) {
            return "AREA";
        }

        String areaPart = normalized.length() > 12 ? normalized.substring(0, 12) : normalized;

        long seq = nextSequenceValue("logis_asrs.seq_ac_stock_unit_no");
        String ymd = DATE_FMT.format(new Date());

        return String.format("%s-%s-%s-%09d", STOCK_UNIT_NO_DEFAULT_PREFIX, areaPart, ymd, seq);
    }


    private long nextSequenceValue(String sequenceName) {
        String sql = "select nextval('" + sequenceName + "') as seq";

        Map<String, Object> param = new HashMap<String, Object>();

        List<SequenceValueRow> rows =
                this.queryManager.selectListBySql(sql, param, SequenceValueRow.class, 0, 0);

        if (rows == null || rows.isEmpty() || rows.get(0) == null || rows.get(0).getSeq() == null) {
            throw new IllegalStateException("Failed to get next sequence value. sequenceName=" + sequenceName);
        }

        return rows.get(0).getSeq();
    }
}