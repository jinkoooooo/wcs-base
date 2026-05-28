package operato.logis.wcs.service.impl.query.inventory;

import operato.logis.wcs.service.impl.query.common.AbstractAggregatePagedService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.util.ValueUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 재고 집계 현황 (BUSINESS303) 서비스.
 *
 * tb_inventory_stock 을 기준으로 아이템/Lot/생산일자 단위 수량 집계.
 * InboundResult/InboundSummary 와 동일한 페이징/필터/정렬 규약을 따른다.
 *
 * SELECT alias → 프론트 검색 필드 매핑:
 *   - item_code / item_name / lot_no: like 필터
 *   - produce_date: between/eq 필터 — date 타입으로 노출
 */
@Service
public class InventoryAggregateService extends AbstractAggregatePagedService {

    /**
     * 도메인 SQL — SELECT alias 가 메뉴 메타의 그리드 컬럼 name 과 일치해야 한다.
     * produce_date 는 BETWEEN 비교 시 date CAST 가 필요하므로 alias 에서 date 로 노출.
     */
    private static final String INNER_SQL = """
             SELECT COALESCE(loc.loc_group, '') AS zone,
                    stk.item_code,
                    itm.item_name,
                    itm.item_type AS item_spec,
                    stk.lot_no,
                    stk.inb_datetime::date AS produce_date,
                    'EA' AS base_unit,
                    COALESCE(SUM(stk.item_qty), 0) AS total_qty,
                    COUNT(DISTINCT stk.stock_id) AS pallet_qty
               FROM tb_inventory_stock stk
               LEFT JOIN tb_inventory_item_mst itm
                      ON itm.item_code = stk.item_code
               -- loc.stock_id 는 비즈니스 키(stk.stock_id). PK 매칭하면 zone=''
               LEFT JOIN tb_inventory_location loc
                      ON loc.stock_id = stk.stock_id
              WHERE (stk.is_enabled IS NULL OR stk.is_enabled = true)
              /*__DYN_WHERE__*/
              GROUP BY loc.loc_group, stk.item_code, itm.item_name, itm.item_type, stk.lot_no,
                       stk.inb_datetime::date
            """;

    private static final String DEFAULT_ORDER = " ORDER BY t.item_code ASC, t.lot_no ASC ";

    /** INNER_SQL SELECT alias 화이트리스트. */
    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            "zone", "item_code", "item_name", "item_spec", "lot_no",
            "produce_date", "base_unit", "total_qty", "pallet_qty"
    );

    @Override protected String getInnerSql()         { return INNER_SQL; }
    @Override protected String getDefaultOrder()     { return DEFAULT_ORDER; }
    @Override protected Set<String> allowedColumns() { return ALLOWED_COLUMNS; }

    // 집계 전 푸시 가능한 베이스 컬럼. 집계결과(total_qty/pallet_qty/base_unit)는 바깥 필터.
    @Override protected Map<String, PushdownColumn> pushdownColumns() {
        return Map.of(
                "item_code",    new PushdownColumn("stk.item_code",   ColumnKind.TEXT),
                "item_name",    new PushdownColumn("itm.item_name",   ColumnKind.TEXT),
                "lot_no",       new PushdownColumn("stk.lot_no",       ColumnKind.TEXT),
                "zone",         new PushdownColumn("loc.loc_group",    ColumnKind.TEXT),
                "item_spec",    new PushdownColumn("itm.item_type",    ColumnKind.TEXT),
                "produce_date", new PushdownColumn("stk.inb_datetime", ColumnKind.TIMESTAMP_AS_DATE));
    }

    /**
     * 품목 자동완성용 lookup. keyword 가 비면 전체 200건.
     */
    @Transactional(readOnly = true)
    public List<Map> lookupItems(String keyword) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT item_code, item_name, item_type, item_owner ")
           .append("  FROM tb_inventory_item_mst ")
           .append(" WHERE 1=1 ");
        Map<String, Object> params = new HashMap<>();
        if (ValueUtil.isNotEmpty(keyword)) {
            sql.append("   AND (item_code ILIKE :kw OR item_name ILIKE :kw) ");
            params.put("kw", "%" + keyword + "%");
        }
        sql.append(" ORDER BY item_code ASC LIMIT 200 ");
        return this.queryManager.selectListBySql(sql.toString(), params, Map.class, 0, 0);
    }
}
