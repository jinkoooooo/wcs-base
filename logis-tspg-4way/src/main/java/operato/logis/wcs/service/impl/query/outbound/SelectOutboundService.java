package operato.logis.wcs.service.impl.query.outbound;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.dto.HostOrderApi;
import operato.logis.wcs.facade.Tspg4WayShuttleWcsFacade;
import operato.logis.wcs.service.impl.query.common.AbstractFlattenedPagedService;
import org.springframework.stereotype.Service;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 선택출고 서비스.
 *
 *   1) 재고 목록 조회 — tb_inventory_stock + tb_inventory_location + tb_inventory_item_mst 조인
 *      (ZONE/보관위치/품목/Lot/수량/생산일자/사용기한/입고일시/작업ID)
 *   2) 선택 재고에 대해 사용자가 지정한 포트(출고대) 로 출고 지시 생성
 *      → Tspg4WayShuttleWcsFacade.receiveHostOrder() 호출
 */
@Service
@RequiredArgsConstructor
public class SelectOutboundService extends AbstractFlattenedPagedService {

    private final Tspg4WayShuttleWcsFacade wcsFacade;

    /**
     * 재고현황 INNER SQL — 예약되지 않은 출고 가능 재고만.
     * stock_type 은 :stockType 파라미터로 제어 (기본 NORMAL, RETURN/DISPOSAL 등 카테고리 전환 가능).
     */
    private static final String INNER_SQL = """
    SELECT s.stock_id                        AS stock_id,
           l.loc_group                       AS loc_zone,
           l.loc_id                        AS loc_id,
           s.item_code                       AS item_code,
           m.item_name                       AS item_name,
           s.lot_no                          AS lot_no,
           m.item_unit                       AS item_unit,
           s.item_qty                        AS item_qty,
           s.inb_datetime::date              AS inbound_date,
           s.expired_datetime::date          AS expired_date,
           s.inb_datetime                    AS inbound_at,
           s.expired_datetime                AS expired_at,
           l.task_id                         AS task_id,
           s.item_owner                      AS item_owner,
           s.eq_group_id                     AS eq_group_id,
           s.stock_type                      AS stock_type
      FROM tb_inventory_stock s
      LEFT JOIN tb_inventory_location l
        ON l.stock_id = s.stock_id
       AND l.loc_group = s.eq_group_id
      LEFT JOIN tb_inventory_item_mst m
        ON m.item_code = s.item_code
       AND m.item_owner = s.item_owner
     WHERE s.is_enabled = TRUE
       AND COALESCE(s.item_qty, 0) > 0
       AND s.stock_status = 0
       AND s.stock_type   = :stockType
    """;

    private static final String DEFAULT_ORDER = " ORDER BY t.loc_id, t.item_code ";

    /** INNER_SQL SELECT alias 화이트리스트. */
    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            "stock_id", "loc_zone", "loc_id",
            "item_code", "item_name", "lot_no", "item_unit", "item_qty",
            "inbound_date", "expired_date", "inbound_at", "expired_at",
            "task_id", "item_owner", "eq_group_id", "stock_type"
    );

    @Override protected String getInnerSql()         { return INNER_SQL; }
    @Override protected String getDefaultOrder()     { return DEFAULT_ORDER; }
    @Override protected Set<String> allowedColumns() { return ALLOWED_COLUMNS; }

    /**
     * stock_type 카테고리 파라미터 지원 오버로드. 기본 NORMAL.
     * INNER_SQL 의 :stockType 에 바인딩하기 위해 부모 search 흐름을 따르되 queryParams 에 stockType 을 미리 끼워넣는다.
     */
    public Map<String, Object> search(String queryJson, String sortJson, int page, int limit, String stockType) {
        Map<String, Object> params = new HashMap<>();
        params.put("stockType", ValueUtil.isEmpty(stockType) ? "NORMAL" : stockType);
        return searchWithParams(queryJson, sortJson, page, limit, params);
    }

    @Override
    public Map<String, Object> search(String queryJson, String sortJson, int page, int limit) {
        return search(queryJson, sortJson, page, limit, "NORMAL");
    }

    // 표시용 ::date alias → 원본 timestamp alias (날짜 필터 sargable)
    @Override protected Map<String, String> dateColumns() {
        return Map.of(
                "inbound_date", "inbound_at",
                "expired_date", "expired_at");
    }

    /**
     * 선택 재고 출고 지시. 재고 1건 = Host Order 1건 발급.
     *
     * @param requestData {
     *     eqGroupId, portCode,
     *     stocks: [{ stockId, itemCode, lotNo, qty, ownerCode, locId, itemOwner }]
     * }
     */
    public Map<String, Object> issueOutbound(Map<String, Object> requestData) {
        logger.info("[ Outbound ][ Select ] issue - data={}", requestData);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> stocks = (List<Map<String, Object>>) requestData.get("stocks");
        if (ValueUtil.isEmpty(stocks)) {
            return Map.of("success", false, "message", "출고할 재고가 선택되지 않았습니다.");
        }

        String portCode = (String) requestData.getOrDefault("portCode", "");
        String eqGroupId = (String) requestData.getOrDefault("eqGroupId", "");
        // host_order.order_type 으로 카테고리 표현 (OUTBOUND / DISPOSAL_OUT). 기본 OUTBOUND
        String orderType = (String) requestData.getOrDefault("orderType", "OUTBOUND");

        List<Map<String, Object>> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        // 재고 1건당 Host Order 1건 발급
        for (Map<String, Object> stock : stocks) {
            HostOrderApi.Request request = new HostOrderApi.Request();
            request.setHostSystemCode("WCS_UI");
            request.setHostOrderKey("UI-OUT-" + System.currentTimeMillis() + "-" + successCount);
            request.setOrderType(ValueUtil.isNotEmpty(orderType) ? orderType : "OUTBOUND");
            request.setOwnerCode((String) stock.getOrDefault("ownerCode", stock.getOrDefault("item_owner", "OWN001")));
            request.setEqGroupId(ValueUtil.isNotEmpty(eqGroupId)
                    ? eqGroupId
                    : (String) stock.getOrDefault("eq_group_id", "EQ01"));
            request.setPriority(5);

            // 사용자 선택 포트 → toLocId, 재고 위치 → fromLocId
            request.setToLocId(ValueUtil.isNotEmpty(portCode) ? portCode : null);
            request.setFromLocId((String) stock.getOrDefault("locId", stock.get("loc_id")));

            // 아이템 1건
            HostOrderApi.Item itemReq = new HostOrderApi.Item();
            itemReq.setLineNo(1);
            itemReq.setItemCode((String) stock.getOrDefault("itemCode", stock.get("item_code")));
            itemReq.setLotNo((String) stock.getOrDefault("lotNo", stock.get("lot_no")));
            Object qtyObj = stock.getOrDefault("qty", stock.get("item_qty"));
            itemReq.setQty(ValueUtil.isNotEmpty(qtyObj) ? Integer.parseInt(qtyObj.toString()) : 0);
            itemReq.setUom((String) stock.getOrDefault("uom", "BOX"));
            request.setItems(List.of(itemReq));

            // Facade 호출
            HostOrderApi.Response resp = wcsFacade.receiveHostOrder(request);

            Map<String, Object> one = new HashMap<>();
            one.put("hostOrderKey", request.getHostOrderKey());
            one.put("stockId", ValueUtil.isNotEmpty(stock.get("stockId")) ? stock.get("stockId") : stock.get("stock_id"));
            one.put("success", resp.isSuccess());
            one.put("wcsOrderKey", resp.getWcsOrderKey());
            one.put("errorDesc", resp.getErrorDesc());
            results.add(one);

            if (resp.isSuccess()) successCount++;
            else failCount++;
        }

        // 전체 결과
        Map<String, Object> result = new HashMap<>();
        result.put("success", failCount == 0);
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("results", results);
        result.put("message", String.format("출고 지시 완료: 성공 %d건, 실패 %d건", successCount, failCount));
        return result;
    }

    /**
     * 출고대(포트) 선택 옵션 조회 — 화면 우측 상단 드롭다운에서 사용.
     * tb_inventory_location 에서 OUTBOUND_PORT / IN_OUTBOUND_PORT 타입 목록을 반환한다.
     */
    public List<Map<String, Object>> listOutboundPorts(String eqGroupId) {
        StringBuilder sql = new StringBuilder("""
            SELECT loc_id AS port_code,
                   loc_id AS port_name,
                   loc_type AS port_type,
                   loc_group AS eq_group_id
              FROM tb_inventory_location
             WHERE loc_type IN ('OUTBOUND_PORT', 'IN_OUTBOUND_PORT')
               AND is_enabled = true
        """);

        Map<String, Object> params = new HashMap<>();
        if (ValueUtil.isNotEmpty(eqGroupId)) {
            sql.append(" AND loc_group = :eqGroupId");
            params.put("eqGroupId", eqGroupId);
        }
        sql.append(" ORDER BY loc_id");

        @SuppressWarnings("unchecked")
        List<Map> rows = this.queryManager.selectListBySql(sql.toString(), params, Map.class, 0, 0);
        List<Map<String, Object>> result = new ArrayList<>();
        if (ValueUtil.isNotEmpty(rows)) {
            for (Map r : rows) {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) r;
                result.add(m);
            }
        }
        return result;
    }
}
