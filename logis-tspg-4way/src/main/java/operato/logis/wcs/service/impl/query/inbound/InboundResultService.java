package operato.logis.wcs.service.impl.query.inbound;

import operato.logis.wcs.service.impl.query.common.AbstractFlattenedPagedService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * 입고실적현황 서비스.
 *
 * 개별 입고 건별 상세 이력(GROUP BY 없음) 을 반환한다.
 * 공통 페이징/필터/정렬 로직은 AbstractFlattenedPagedService 참고.
 */
@Service
public class InboundResultService extends AbstractFlattenedPagedService {

    /**
     * 개별 건별 SQL — SELECT alias 가 메뉴 메타 컬럼 name 과 일치해야 한다.
     */
    private static final String INNER_SQL = """
        SELECT o.order_key,
               o.order_type,
               o.to_loc_code,
               o.owner_code,
               m.item_code,
               m.item_name,
               m.item_unit,
               s.lot_no,
               s.inb_datetime::date AS inbound_date,
               s.inb_datetime       AS inbound_date_ts,
               s.item_qty,
               oi.uom                   AS uom,
               o.order_status,
               o.created_at,
               o.eq_group_id,
               o.barcode                AS barcode,
               o.test_required          AS test_required,
               oi.test_request_no       AS test_request_no,
               oi.test_no               AS test_no
          FROM tb_wcs_shuttle_order o
          JOIN tb_inventory_stock s
            ON s.stock_id = o.carrying_stock_id
           AND s.eq_group_id = o.eq_group_id
          LEFT JOIN tb_inventory_item_mst m
            ON m.item_code = s.item_code
           AND m.item_owner = s.item_owner
          LEFT JOIN tb_wcs_shuttle_order_item oi
            ON oi.order_key = o.order_key
           AND oi.item_code  = s.item_code
           AND COALESCE(oi.lot_no, '') = COALESCE(s.lot_no, '')
         WHERE o.order_type = 'INBOUND'
           AND o.order_status = 90
        """;

    private static final String DEFAULT_ORDER = " ORDER BY t.created_at DESC, t.item_code ";

    /** INNER_SQL SELECT alias 화이트리스트. */
    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            "order_key", "order_type", "to_loc_code", "owner_code",
            "item_code", "item_name", "item_unit", "lot_no",
            "inbound_date", "inbound_date_ts", "item_qty", "uom", "order_status",
            "created_at", "eq_group_id",
            "barcode", "test_required", "test_request_no", "test_no"
    );

    @Override protected String getInnerSql()         { return INNER_SQL; }
    @Override protected String getDefaultOrder()     { return DEFAULT_ORDER; }
    @Override protected Set<String> allowedColumns() { return ALLOWED_COLUMNS; }

    // 표시용 inbound_date(::date) → 원본 timestamp inbound_date_ts (날짜 필터 sargable)
    @Override protected Map<String, String> dateColumns() {
        return Map.of("inbound_date", "inbound_date_ts");
    }
}
