package operato.logis.wcs.service.impl.query.inbound;

import operato.logis.wcs.service.impl.query.common.AbstractAggregatePagedService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * 입고집계현황 서비스.
 *
 * 품목 + Lot + 날짜 기준으로 SUM 집계된 입고 현황을 반환한다.
 * 공통 페이징/필터/정렬 로직은 AbstractAggregatePagedService 참고.
 */
@Service
public class InboundSummaryService extends AbstractAggregatePagedService {

    /**
     * 집계 내부 SQL — SELECT alias 가 메뉴 메타 컬럼 name 과 일치해야 한다.
     */
    private static final String INNER_SQL = """
            SELECT m.item_code,
                   m.item_name,
                   m.item_unit,
                   i.lot_no,
                   SUM(i.qty)              AS inbound_qty,
                   o.updated_at::date      AS inbound_date
              FROM tb_wcs_shuttle_order o
              JOIN tb_wcs_shuttle_order_item i
                ON i.order_key = o.order_key
              LEFT JOIN tb_inventory_item_mst m
                ON m.item_code = i.item_code
               AND m.item_owner = o.owner_code
             WHERE o.order_type = 'INBOUND'
               AND o.order_status = 90
             /*__DYN_WHERE__*/
             GROUP BY m.item_code, m.item_name, m.item_unit,
                      i.lot_no, o.updated_at::date
        """;

    private static final String DEFAULT_ORDER = " ORDER BY t.inbound_date DESC, t.item_code, t.lot_no ";

    /** INNER_SQL SELECT alias 화이트리스트. */
    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            "item_code", "item_name", "item_unit",
            "lot_no", "inbound_qty", "inbound_date"
    );

    @Override protected String getInnerSql()         { return INNER_SQL; }
    @Override protected String getDefaultOrder()     { return DEFAULT_ORDER; }
    @Override protected Set<String> allowedColumns() { return ALLOWED_COLUMNS; }

    // 집계 전 푸시. inbound_qty(SUM)는 집계결과라 바깥 필터.
    @Override protected Map<String, PushdownColumn> pushdownColumns() {
        return Map.of(
                "item_code",    new PushdownColumn("i.item_code",  ColumnKind.TEXT),
                "item_name",    new PushdownColumn("m.item_name",  ColumnKind.TEXT),
                "item_unit",    new PushdownColumn("m.item_unit",  ColumnKind.TEXT),
                "lot_no",       new PushdownColumn("i.lot_no",      ColumnKind.TEXT),
                "inbound_date", new PushdownColumn("o.updated_at", ColumnKind.TIMESTAMP_AS_DATE));
    }
}
