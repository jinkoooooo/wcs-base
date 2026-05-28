package operato.logis.wcs.service.impl.query.common.orderResult;

import operato.logis.wcs.service.impl.query.common.AbstractFlattenedPagedService;
import org.springframework.stereotype.Service;
import xyz.elidom.util.ValueUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 입출고 실적현황 공용 서비스.
 *
 * Master: tb_wcs_host_order 전체 조회 (order_type 필터는 외부 query 에서 처리).
 * Detail: host_order_key 에 연결된 shuttle_order + item 상세.
 */
@Service
public class OrderResultService extends AbstractFlattenedPagedService {

    private static final String INNER_SQL = """
        SELECT ho.host_order_key,
               ho.order_type,
               ho.order_status,
               ho.eq_group_id,
               ho.owner_code,
               ow.owner_name,
               ho.to_loc_code,
               ho.wcs_order_key,
               ho.priority,
               ho.error_code,
               ho.error_desc,
               item_agg.item_code,
               item_agg.item_names,
               item_agg.lot_nos,
               item_agg.total_qty,
               item_agg.line_count,
               shuttle_agg.shuttle_count,
               ho.created_at,
               ho.updated_at,
               ho.id
          FROM tb_wcs_host_order ho
          LEFT JOIN tb_wcs_item_owner ow
            ON ow.owner_code = ho.owner_code
          LEFT JOIN (
              SELECT hoi.host_order_key,
                     STRING_AGG(DISTINCT hoi.item_code, ', ') AS item_code,
                     STRING_AGG(DISTINCT m.item_name, ', ')   AS item_names,
                     STRING_AGG(DISTINCT hoi.lot_no, ', ')    AS lot_nos,
                     SUM(hoi.qty)                             AS total_qty,
                     COUNT(*)                                 AS line_count
                FROM tb_wcs_host_order_item hoi
                LEFT JOIN tb_inventory_item_mst m
                  ON m.item_code = hoi.item_code
                 AND m.item_owner = (SELECT owner_code FROM tb_wcs_host_order
                                      WHERE host_order_key = hoi.host_order_key LIMIT 1)
               GROUP BY hoi.host_order_key
          ) item_agg
            ON item_agg.host_order_key = ho.host_order_key
          LEFT JOIN (
              SELECT so.host_order_key,
                     COUNT(*) AS shuttle_count
                FROM tb_wcs_shuttle_order so
               WHERE so.host_order_key IS NOT NULL
               GROUP BY so.host_order_key
          ) shuttle_agg
            ON shuttle_agg.host_order_key = ho.host_order_key
        """;

    private static final String DEFAULT_ORDER =
            " ORDER BY t.created_at DESC, t.host_order_key ";

    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            "host_order_key", "order_type", "order_status", "eq_group_id",
            "owner_code", "owner_name", "to_loc_code", "wcs_order_key",
            "priority", "error_code", "error_desc",
            "item_code", "item_names", "lot_nos", "total_qty",
            "line_count", "shuttle_count",
            "created_at", "updated_at", "id"
    );

    @Override protected String getInnerSql()         { return INNER_SQL; }
    @Override protected String getDefaultOrder()     { return DEFAULT_ORDER; }
    @Override protected Set<String> allowedColumns() { return ALLOWED_COLUMNS; }

    /**
     * host_order_key 에 연결된 shuttle_order + item 상세 조회 (입출고 공용).
     */
    public Map<String, Object> searchShuttleOrders(String hostOrderKey) {
        if (ValueUtil.isEmpty(hostOrderKey)) {
            return Map.of("total", 0, "items", Collections.emptyList());
        }

        String sql = """
            SELECT so.order_key,
                   so.order_type,
                   so.order_status,
                   so.from_loc_code,
                   so.to_loc_code,
                   so.eq_group_id,
                   so.level,
                   so.barcode,
                   so.ecs_if_status,
                   so.parent_order_key,
                   so.aging_count,
                   so.remark,
                   so.carrying_stock_id,
                   so.host_order_key,
                   so.priority         AS so_priority,
                   so.created_at       AS so_created_at,
                   so.updated_at       AS so_updated_at,
                   soi.id              AS item_id,
                   soi.item_code,
                   m.item_name,
                   soi.lot_no,
                   soi.qty,
                   soi.uom,
                   soi.produce_date,
                   soi.expiry_date,
                   soi.line_status
              FROM tb_wcs_shuttle_order so
              LEFT JOIN tb_wcs_shuttle_order_item soi
                ON soi.order_key = so.order_key
              LEFT JOIN tb_inventory_item_mst m
                ON m.item_code = soi.item_code
               AND m.item_owner = so.owner_code
             WHERE so.host_order_key = :hostOrderKey
                OR so.order_key IN (
                     SELECT child.order_key
                       FROM tb_wcs_shuttle_order child
                      WHERE child.parent_order_key IN (
                              SELECT parent.order_key
                                FROM tb_wcs_shuttle_order parent
                               WHERE parent.host_order_key = :hostOrderKey
                            )
                   )
             ORDER BY so.parent_order_key ASC NULLS FIRST,
                      so.priority ASC,
                      so.created_at ASC,
                      soi.id ASC
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("hostOrderKey", hostOrderKey);

        List<Map> records = this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);
        List<Map> result = ValueUtil.isNotEmpty(records) ? records : Collections.emptyList();

        return Map.of("total", result.size(), "items", result);
    }

    @Override
    protected Map<String, String> dateColumns() {
        // created_at(timestamp) 를 날짜 필터 대상으로 등록 → CAST + 반열림 처리됨
        return Map.of("created_at", "created_at");
    }
}