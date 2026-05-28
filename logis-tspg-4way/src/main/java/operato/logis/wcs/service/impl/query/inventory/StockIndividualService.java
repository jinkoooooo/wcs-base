package operato.logis.wcs.service.impl.query.inventory;

import operato.logis.wcs.service.impl.query.common.AbstractFlattenedPagedService;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 개별 재고 현황 (BUSINESS304) 서비스.
 *
 * tb_inventory_stock 기준 stock 단위 개별 행을 그대로 조회한다.
 * LATERAL JOIN 으로 가장 최근 셔틀 작업의 order_key 를 task_id 로 노출.
 */
@Service
public class StockIndividualService extends AbstractFlattenedPagedService {

    private static final String INNER_SQL = """
        SELECT loc.loc_id                                              AS stor_loc,
               stk.item_code                                             AS item_code,
               itm.item_name                                             AS item_name,
               COALESCE(itm.item_spec_detail, itm.item_spec)             AS item_spec,
               itm.remarks                                               AS item_memo,
               stk.lot_no                                                AS lot_no,
               COALESCE(itm.item_unit, 'BOX')                            AS unit,
               stk.item_qty                                              AS item_qty,
               TO_CHAR(stk.inb_datetime, 'YYYY-MM-DD')                   AS produce_date,
               TO_CHAR(
                   COALESCE(
                       stk.expired_datetime,
                       stk.inb_datetime + (itm.expiry_days || ' days')::interval
                   ),
                   'YYYY-MM-DD'
               )                                                         AS expire_date,
               stk.stock_status                                          AS stock_status,
               stk.stock_type                                            AS stock_type,
               so.order_key                                              AS task_id,
               TO_CHAR(stk.inb_datetime, 'YYYY-MM-DD')                   AS inbound_date,
               stk.eq_group_id                                           AS eq_group_id,
               stk.item_owner                                            AS item_owner,
               stk.stock_id                                              AS stock_id
          FROM tb_inventory_stock stk
          LEFT JOIN tb_inventory_location loc
            ON loc.stock_id = stk.stock_id
          LEFT JOIN tb_inventory_item_mst itm
            ON itm.item_code  = stk.item_code
           AND itm.item_owner = stk.item_owner
          LEFT JOIN LATERAL (
              SELECT s.order_key
                FROM tb_wcs_shuttle_order s
               WHERE s.carrying_stock_id = stk.stock_id
               ORDER BY s.order_status DESC, s.updated_at DESC
               LIMIT 1
          ) so ON true
         WHERE stk.item_qty > 0
           AND stk.stock_status = 0
           AND stk.stock_type   = 'NORMAL'
        """;

    private static final String DEFAULT_ORDER = " ORDER BY t.stor_loc ASC, t.inbound_date DESC, t.item_code ";

    /** INNER_SQL SELECT alias 화이트리스트. */
    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            "stor_loc", "item_code", "item_name", "item_spec", "item_memo",
            "lot_no", "unit", "item_qty", "produce_date", "expire_date",
            "stock_status", "stock_type", "task_id", "inbound_date",
            "eq_group_id", "item_owner", "stock_id"
    );

    @Override protected String getInnerSql()         { return INNER_SQL; }
    @Override protected String getDefaultOrder()     { return DEFAULT_ORDER; }
    @Override protected Set<String> allowedColumns() { return ALLOWED_COLUMNS; }
}
