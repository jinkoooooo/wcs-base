package operato.logis.wcs.service.impl.query.outbound;

import operato.logis.wcs.service.impl.query.common.AbstractFlattenedPagedService;
import org.springframework.stereotype.Service;
import xyz.elidom.util.ValueUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 출고실적현황 서비스.
 *
 * Master: tb_wcs_host_order 기준 OUTBOUND 주문 전체 이력.
 *   - 상태 무관 (진행중/완료/실패/취소 모두 포함)
 *   - 주문 1건 = 그리드 1행
 *   - 하단 셔틀 오더 목록을 드릴다운하기 위한 키: host_order_key
 *
 * Detail: 특정 host_order_key 에 연결된 tb_wcs_shuttle_order N건 + 각 오더의 Item 정보.
 *   - OUTBOUND 본 오더 + 방해물 MOVE 오더(parent_order_key 로 연결)
 *   - 한 주문에서 셔틀이 실제로 몇 번 움직였는지 표시
 */
@Service
public class OutboundResultService extends AbstractFlattenedPagedService {

    private static final String INNER_SQL = """
        SELECT ho.host_order_key                    AS history_no,
               ho.order_type                        AS order_type,
               ho.order_status                      AS order_status,
               ho.eq_group_id                       AS eq_group_id,
               ho.owner_code                        AS owner_code,
               ow.owner_name                        AS owner_name,
               ho.to_loc_code                       AS request_port_code,
               ho.wcs_order_key                     AS wcs_order_key,
               ho.priority                          AS priority,
               ho.error_code                        AS error_code,
               ho.error_desc                        AS error_desc,
               item_agg.item_codes                  AS item_code,
               item_agg.item_names                  AS item_name,
               item_agg.lot_nos                     AS lot_no,
               item_agg.total_qty                   AS item_qty,
               item_agg.line_count                  AS line_count,
               shuttle_agg.shuttle_count            AS shuttle_count,
               ho.created_at::date                  AS created_at,
               ho.updated_at::date                  AS outbound_at,
               ho.created_at                        AS created_at_ts,
               ho.updated_at                        AS outbound_at_ts,
               ho.id                                AS tbl_id
          FROM tb_wcs_host_order ho
          LEFT JOIN tb_wcs_item_owner ow
            ON ow.owner_code = ho.owner_code
          LEFT JOIN (
              SELECT hoi.host_order_key,
                     STRING_AGG(DISTINCT hoi.item_code, ', ')       AS item_codes,
                     STRING_AGG(DISTINCT m.item_name, ', ')        AS item_names,
                     STRING_AGG(DISTINCT hoi.lot_no,  ', ')        AS lot_nos,
                     SUM(hoi.qty)                                  AS total_qty,
                     COUNT(*)                                      AS line_count
                FROM tb_wcs_host_order_item hoi
                LEFT JOIN tb_inventory_item_mst m
                  ON  m.item_code  = hoi.item_code
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
         WHERE ho.order_type = 'OUTBOUND'
        """;

    private static final String DEFAULT_ORDER =
            " ORDER BY t.created_at_ts DESC, t.history_no ";

    /** INNER_SQL SELECT alias 화이트리스트 (Master 그리드 검색/정렬용). */
    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            "history_no", "order_type", "order_status", "eq_group_id",
            "owner_code", "owner_name", "request_port_code", "wcs_order_key",
            "priority", "error_code", "error_desc",
            "item_code", "item_name", "lot_no", "item_qty",
            "line_count", "shuttle_count",
            "created_at", "outbound_at", "created_at_ts", "outbound_at_ts", "tbl_id"
    );

    @Override protected String getInnerSql()         { return INNER_SQL; }
    @Override protected String getDefaultOrder()     { return DEFAULT_ORDER; }
    @Override protected Set<String> allowedColumns() { return ALLOWED_COLUMNS; }

    // 표시용 ::date alias → 원본 timestamp alias (날짜 필터 sargable)
    @Override protected Map<String, String> dateColumns() {
        return Map.of(
                "created_at",  "created_at_ts",
                "outbound_at", "outbound_at_ts");
    }

    /**
     * 특정 host_order_key 에 연결된 셔틀 오더 + item 상세.
     *
     * 포함 범위:
     *   - 본 OUTBOUND 셔틀 오더 (host_order_key 직접 매칭)
     *   - 해당 본 오더의 방해물 MOVE 오더 (parent_order_key 로 연결)
     *
     * 정렬:
     *   - 본 오더(OUTBOUND) 우선, 방해물(MOVE) 뒤
     *   - 본 오더 안에서는 priority ASC, created_at ASC
     *   - 각 오더의 item 은 line_no ASC
     */
    public Map<String, Object> searchShuttleOrders(String hostOrderKey) {
        if (ValueUtil.isEmpty(hostOrderKey)) {
            return Map.of("total", 0, "items", Collections.emptyList());
        }

        String sql = """
            SELECT so.order_key                AS order_key,
                   so.order_type               AS order_type,
                   so.order_status             AS order_status,
                   so.from_loc_code            AS from_loc_code,
                   so.to_loc_code              AS to_loc_code,
                   so.eq_group_id              AS eq_group_id,
                   so.level                    AS level,
                   so.barcode                  AS barcode,
                   so.ecs_if_status            AS ecs_if_status,
                   so.parent_order_key         AS parent_order_key,
                   so.aging_count              AS aging_count,
                   so.remark                   AS remark,
                   so.carrying_stock_id        AS carrying_stock_id,
                   so.host_order_key           AS host_order_key,
                   so.priority                 AS so_priority,
                   so.created_at               AS so_created_at,
                   so.updated_at               AS so_updated_at,
                   soi.id                      AS item_id,
                   soi.item_code                AS item_code,
                   m.item_name                 AS item_name,
                   soi.lot_no                  AS lot_no,
                   soi.qty                     AS qty,
                   soi.uom                     AS uom,
                   soi.produce_date            AS produce_date,
                   soi.expiry_date             AS expiry_date,
                   soi.line_status             AS line_status
              FROM tb_wcs_shuttle_order so
              LEFT JOIN tb_wcs_shuttle_order_item soi
                ON soi.order_key = so.order_key
              LEFT JOIN tb_inventory_item_mst m
                ON  m.item_code  = soi.item_code
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
             ORDER BY CASE WHEN so.order_type = 'OUTBOUND' THEN 0 ELSE 1 END,
                      so.parent_order_key ASC NULLS FIRST,
                      so.priority ASC,
                      so.created_at ASC,
                      soi.id ASC
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("hostOrderKey", hostOrderKey);

        List<Map> records = this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);
        List<Map> result = ValueUtil.isNotEmpty(records) ? records : Collections.emptyList();

        logger.info("[ Outbound ][ Result ] searchShuttleOrders - hostOrderKey={}, count={}",
                hostOrderKey, result.size());

        return Map.of("total", result.size(), "items", result);
    }
}
