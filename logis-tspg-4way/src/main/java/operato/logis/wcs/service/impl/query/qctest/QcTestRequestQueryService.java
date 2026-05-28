package operato.logis.wcs.service.impl.query.qctest;

import operato.logis.wcs.service.impl.query.common.AbstractFlattenedPagedService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class QcTestRequestQueryService extends AbstractFlattenedPagedService {

    private static final String INNER_SQL = """
        SELECT q.id,
               q.inbound_date,
               q.test_request_no,
               q.item_code,
               m.item_name,
               q.test_wf_type,
               q.test_req_desc,
               q.manufacturer,
               q.lot_no,
               q.manufactured_date,
               q.manufactured_qty,
               q.mfr_unit,
               q.expiry_date,
               q.incoming_qty,
               q.req_dept,
               q.submitter_order,
               q.item_owner,
               q.test_no,
               q.status,
               q.fetched,
               q.report_pdf_id,
               f.file_name AS report_pdf_name,
               q.completed_at,
               q.created_at,
               q.updated_at
          FROM tb_wcs_qc_test_request q
                LEFT JOIN tb_inventory_item_mst m ON m.item_code = q.item_code AND m.deleted_at IS NULL
                LEFT JOIN tb_wcs_file_attachment f ON f.id = q.report_pdf_id
        """;

    private static final String DEFAULT_ORDER =
            " ORDER BY t.inbound_date DESC, t.item_code, t.lot_no ";

    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            "id", "inbound_date", "test_request_no", "item_code",
            "test_wf_type", "test_req_desc", "manufacturer", "lot_no",
            "manufactured_date", "manufactured_qty", "mfr_unit",
            "expiry_date", "incoming_qty", "req_dept", "submitter_order",
            "item_owner", "test_no", "status", "fetched",
            "report_pdf_id", "report_pdf_name",
            "completed_at", "created_at", "updated_at"
    );

    @Override protected String getInnerSql()         { return INNER_SQL; }
    @Override protected String getDefaultOrder()     { return DEFAULT_ORDER; }
    @Override protected Set<String> allowedColumns() { return ALLOWED_COLUMNS; }

    @Override
    protected Map<String, String> dateColumns() {
        return Map.of(
                "inbound_date", "inbound_date",
                "manufactured_date", "manufactured_date",
                "expiry_date", "expiry_date",
                "completed_at", "completed_at"
        );
    }
}