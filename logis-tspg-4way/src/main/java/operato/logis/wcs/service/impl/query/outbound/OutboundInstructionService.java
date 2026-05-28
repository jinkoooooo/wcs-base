package operato.logis.wcs.service.impl.query.outbound;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.UomType;
import operato.logis.wcs.dto.HostOrderApi;
import operato.logis.wcs.facade.Tspg4WayShuttleWcsFacade;
import operato.logis.wcs.service.impl.query.common.AbstractFlattenedPagedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.util.ValueUtil;

import static operato.logis.wcs.common.util.lang.CommonUtils.strOr;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 출고 지시(UI) 페이징 조회 + 다건 host_order 발행 + 삭제 서비스.
 *
 * 정책: 행 단위 1 host_order 생성, 행별 독립 트랜잭션 (REQUIRES_NEW), 부분 성공 허용.
 */
@Service
@RequiredArgsConstructor
public class OutboundInstructionService extends AbstractFlattenedPagedService {

    private final Tspg4WayShuttleWcsFacade wcsFacade;

    /**
     * 셀프 주입 — 같은 클래스 내 @Transactional 메서드 호출 시 프록시 우회 방지.
     * 생성자 주입 시 BeanCurrentlyInCreationException 회피 위해 field-injected 로 유지.
     */
    @Autowired
    private OutboundInstructionService self;

    private static final String INNER_SQL = """
        SELECT o.host_order_key                  AS order_key,
               o.owner_code                      AS owner_code,
               i.item_code                        AS item_code,
               m.item_name                       AS item_name,
               i.lot_no                          AS lot_no,
               i.qty                             AS order_qty,
               i.uom                             AS uom,
               COALESCE(picked.qty_sum, 0)       AS picked_qty,
               COALESCE(completed.qty_sum, 0)    AS complete_qty,
               o.order_status                    AS order_status,
               o.eq_group_id                     AS eq_group_id,
               o.to_loc_code                     AS request_port_code,
               o.id                              AS tbl_id,
               o.created_at::date                AS created_at,
               o.created_at                      AS created_at_ts
          FROM tb_wcs_host_order o
          JOIN tb_wcs_host_order_item i
            ON i.host_order_key = o.host_order_key
          LEFT JOIN tb_inventory_item_mst m
            ON  m.item_code  = i.item_code
            AND m.item_owner = o.owner_code
          LEFT JOIN (
              SELECT soi.order_key, SUM(soi.qty) AS qty_sum
                FROM tb_wcs_shuttle_order so
                JOIN tb_wcs_shuttle_order_item soi ON soi.order_key = so.order_key
               WHERE so.order_type = 'OUTBOUND'
                 AND so.order_status >= 20
               GROUP BY soi.order_key
          ) picked
            ON picked.order_key = o.wcs_order_key
          LEFT JOIN (
              SELECT soi.order_key, SUM(soi.qty) AS qty_sum
                FROM tb_wcs_shuttle_order so
                JOIN tb_wcs_shuttle_order_item soi ON soi.order_key = so.order_key
               WHERE so.order_type = 'OUTBOUND'
                 AND so.order_status >= 90
               GROUP BY soi.order_key
          ) completed
            ON completed.order_key = o.wcs_order_key
         WHERE o.order_type = 'OUTBOUND' and o.host_system_code in ('WCS_UI','WMS-SIM')
        """;

    private static final String DEFAULT_ORDER =
            " ORDER BY t.created_at_ts DESC, t.order_key ";

    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            "order_key", "owner_code", "item_code", "item_name", "lot_no",
            "order_qty", "uom", "picked_qty", "complete_qty",
            "order_status", "eq_group_id", "request_port_code",
            "tbl_id", "created_at", "created_at_ts"
    );

    @Override protected String getInnerSql()         { return INNER_SQL; }
    @Override protected String getDefaultOrder()     { return DEFAULT_ORDER; }
    @Override protected Set<String> allowedColumns() { return ALLOWED_COLUMNS; }

    // 표시용 created_at(::date) → 원본 timestamp created_at_ts (날짜 필터 sargable)
    @Override protected Map<String, String> dateColumns() {
        return Map.of("created_at", "created_at_ts");
    }

    /**
     * 다건 출고 지시 등록.
     *
     * body 형태:
     *   { portCode(선택), scheduledDate(선택),
     *     orders: [{ eqGroupId, ownerCode, itemCode, lotNo, qty, uom, clientRowSeq }, ...] }
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> register(Map<String, Object> requestData) {
        logger.info("[ Outbound ][ Instruction ] register - data={}", requestData);

        List<Map<String, Object>> orders = (List<Map<String, Object>>) requestData.get("orders");
        if (ValueUtil.isEmpty(orders)) {
            return Map.of("success", false, "message", "등록할 출고 주문이 없습니다.");
        }

        String portCode = (String) requestData.get("portCode");
        LocalDate scheduledDate = parseLocalDate(requestData.get("scheduledDate"));

        List<Map<String, Object>> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        // 동일 timestamp 충돌 방지를 위해 baseSeq 와 함께 사용
        long baseTs = System.currentTimeMillis();
        int seq = 0;

        for (Map<String, Object> row : orders) {
            seq++;
            Map<String, Object> rowResult;
            try {
                // 셀프 프록시 호출 — 행별 독립 트랜잭션 보장
                rowResult = self.registerSingleOrder(row, portCode, scheduledDate, baseTs, seq);
            } catch (Exception e) {
                logger.error("[ Outbound ][ Instruction ] register failed - seq={}, sku={}",
                        seq, row.get("itemCode"), e);
                rowResult = failResult(row, "처리 중 오류: " + e.getMessage());
            }

            results.add(rowResult);
            if (Boolean.TRUE.equals(rowResult.get("success"))) successCount++;
            else failCount++;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", failCount == 0);
        response.put("totalCount", orders.size());
        response.put("successCount", successCount);
        response.put("failCount", failCount);
        response.put("results", results);
        response.put("message", String.format(
                "전체 %d건 중 성공 %d / 실패 %d",
                orders.size(), successCount, failCount));

        return response;
    }

    /**
     * 단일 출고 주문 등록 — 트랜잭션 경계. Facade 실패 시 RuntimeException 으로 롤백.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Map<String, Object> registerSingleOrder(
            Map<String, Object> row,
            String portCode,
            LocalDate scheduledDate,
            long baseTs,
            int seq) {

        String eqGroupId = strOr(row.get("eqGroupId"), null);
        String ownerCode = strOr(row.get("ownerCode"), null);
        String itemCode = (String) row.get("itemCode");
        String lotNo = (String) row.getOrDefault("lotNo", "");
        String uom = strOr(row.get("uom"), UomType.EA.code());
        int qty = ValueUtil.isNotEmpty(row.get("qty")) ? Integer.parseInt(row.get("qty").toString()) : 0;

        // 필수값 검증
        if (ValueUtil.isEmpty(eqGroupId)) {
            return failResult(row, "창고(eqGroupId)가 비어있습니다.");
        }
        if (ValueUtil.isEmpty(ownerCode)) {
            return failResult(row, "화주(ownerCode)가 비어있습니다.");
        }
        if (ValueUtil.isEmpty(itemCode)) {
            return failResult(row, "품목코드가 비어있습니다.");
        }
        if (qty <= 0) {
            return failResult(row, "출고수량은 1 이상이어야 합니다.");
        }

        // Host Order 요청 조립
        HostOrderApi.Request request = new HostOrderApi.Request();
        request.setHostSystemCode("WCS_UI");
        request.setHostOrderKey("UI-OUT-" + baseTs + "-" + seq);
        request.setOrderType("OUTBOUND");
        request.setOwnerCode(ownerCode);
        request.setEqGroupId(eqGroupId);
        request.setPriority(5);
        request.setScheduledDate(scheduledDate);

        if (ValueUtil.isNotEmpty(portCode)) {
            request.setToLocId(portCode);
        }

        // 아이템 1건
        List<HostOrderApi.Item> items = new ArrayList<>();
        HostOrderApi.Item item = new HostOrderApi.Item();
        item.setLineNo(1);
        item.setItemCode(itemCode);
        item.setLotNo(lotNo);
        item.setQty(qty);
        item.setUom(uom);
        items.add(item);
        request.setItems(items);

        // Facade 호출
        HostOrderApi.Response response = wcsFacade.receiveHostOrder(request);

        if (!response.isSuccess()) {
            // 트랜잭션 롤백 + 결과 메시지 반환
            throw new RuntimeException(ValueUtil.isNotEmpty(response.getErrorDesc())
                    ? response.getErrorDesc()
                    : "Facade 실패");
        }

        // 성공 응답
        Map<String, Object> r = new HashMap<>();
        r.put("success", true);
        r.put("clientRowSeq", row.get("clientRowSeq"));
        r.put("eqGroupId", eqGroupId);
        r.put("ownerCode", ownerCode);
        r.put("itemCode", itemCode);
        r.put("qty", qty);
        r.put("uom", uom);
        r.put("hostOrderKey", request.getHostOrderKey());
        r.put("wcsOrderKey", ValueUtil.isNotEmpty(response.getWcsOrderKey()) ? response.getWcsOrderKey() : "");
        r.put("message", "등록 완료");
        return r;
    }

    /**
     * 실패 응답 빌더.
     */
    private Map<String, Object> failResult(Map<String, Object> row, String message) {
        Map<String, Object> r = new HashMap<>();
        r.put("success", false);
        r.put("clientRowSeq", row.get("clientRowSeq"));
        r.put("eqGroupId", strOr(row.get("eqGroupId"), ""));
        r.put("ownerCode", strOr(row.get("ownerCode"), ""));
        r.put("itemCode", strOr(row.get("itemCode"), ""));
        r.put("message", message);
        return r;
    }

    /**
     * 프론트 DatePicker 값(yyyy-MM-dd) → LocalDate. 잘못된 값은 null.
     */
    private LocalDate parseLocalDate(Object value) {
        if (ValueUtil.isEmpty(value)) return null;
        String s = value.toString().trim();
        if (ValueUtil.isEmpty(s)) return null;
        if (s.length() >= 10) s = s.substring(0, 10);
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            logger.warn("[ Outbound ][ Instruction ] parseLocalDate ignored - value={}", value);
            return null;
        }
    }


    /**
     * 여러 host_order_key 단위로 출고 지시 삭제 (item → header 순).
     */
    public Map<String, Object> deleteByHostOrderKeys(List<String> hostOrderKeys) {
        if (ValueUtil.isEmpty(hostOrderKeys)) {
            return Map.of("success", false, "message", "삭제할 항목이 없습니다.");
        }

        int deletedOrders = 0;
        for (String hostOrderKey : hostOrderKeys) {
            Map<String, Object> params = ValueUtil.newMap("hostOrderKey", hostOrderKey);

            this.queryManager.executeBySql(
                    "DELETE FROM tb_wcs_host_order_item WHERE host_order_key = :hostOrderKey", params);

            this.queryManager.executeBySql(
                    "DELETE FROM tb_wcs_host_order WHERE host_order_key = :hostOrderKey", params);

            deletedOrders++;
        }

        logger.info("[ Outbound ][ Instruction ] deleted - orderCount={}", deletedOrders);
        return Map.of("success", true, "message", deletedOrders + "건 삭제되었습니다.");
    }
}
