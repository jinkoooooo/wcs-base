package operato.logis.wcs.service.impl.query.inbound;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.entity.TbWcsQcTestRequest;
import operato.logis.wcs.service.impl.pallet.PalletBoxFactory;
import operato.logis.wcs.service.impl.qctest.QcRequestService;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.dto.HostOrderApi;
import operato.logis.wcs.facade.Tspg4WayShuttleWcsFacade;
import operato.logis.wcs.service.impl.query.common.AbstractFlattenedPagedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.util.ValueUtil;

import static operato.logis.wcs.common.util.lang.CommonUtils.strOr;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 입고 등록(UI) 페이징 조회 + 다중 파렛트 일괄 등록 + 삭제 서비스.
 *
 * 정책:
 *   - 한 파렛트(eqGroupId + barcode) = 하나의 host_order
 *   - 파렛트별 독립 트랜잭션, 일부 실패해도 성공 파렛트는 커밋
 *   - 결과는 파렛트별 results 배열로 반환
 */
@Service
@RequiredArgsConstructor
public class InboundRegistService extends AbstractFlattenedPagedService {

    private final Tspg4WayShuttleWcsFacade wcsFacade;
    private final PalletBoxFactory palletBoxFactory;
    private final QcRequestService qcRequestService;
    private final InboundPlanService inboundPlanService;

    /**
     * 셀프 주입 — 같은 클래스 내 @Transactional 메서드 호출 시 프록시 우회 방지.
     * 생성자 주입 시 BeanCurrentlyInCreationException 회피 위해 field-injected 로 유지.
     */
    @Autowired
    private InboundRegistService self;

    private static final String INNER_SQL = """
    SELECT i.id,
           o.host_order_key                    AS host_order_key,
           i.item_code                          AS item_code,
           m.item_name                         AS item_name,
           i.lot_no                            AS lot_no,
           o.order_type                        AS order_type,
           o.owner_code                        AS owner_code,
           i.qty                               AS item_qty,
           i.uom                               AS uom,
           o.order_status                      AS order_status,
           o.eq_group_id                       AS eq_group_id,
           o.barcode                           AS barcode,
           o.test_required                     AS test_required,
           i.test_request_no                   AS test_request_no,
           i.test_no                           AS test_no,
           i.produce_date                      AS produce_date,
           i.expiry_date                       AS expiry_date,
           o.created_at::date                  AS created_at,
           o.updated_at::date                  AS updated_at,
           o.created_at                        AS created_at_ts,
           o.updated_at                        AS updated_at_ts
      FROM tb_wcs_host_order o
      JOIN tb_wcs_host_order_item i
        ON i.host_order_key = o.host_order_key
      LEFT JOIN tb_inventory_item_mst m
        ON m.item_code = i.item_code
     WHERE o.order_type = 'INBOUND'
    """;

    private static final String DEFAULT_ORDER = " ORDER BY t.created_at_ts DESC, t.item_code ";

    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            "id", "host_order_key", "item_code", "item_name", "lot_no",
            "order_type", "owner_code", "item_qty", "uom", "order_status", "eq_group_id",
            "barcode", "test_required", "test_request_no", "test_no",
            "produce_date",
            "expiry_date", "created_at", "updated_at", "created_at_ts", "updated_at_ts"
    );

    @Override protected String getInnerSql()         { return INNER_SQL; }
    @Override protected String getDefaultOrder()     { return DEFAULT_ORDER; }
    @Override protected Set<String> allowedColumns() { return ALLOWED_COLUMNS; }

    // 표시용 ::date alias → 원본 timestamp alias (날짜 필터 sargable)
    @Override protected Map<String, String> dateColumns() {
        return Map.of(
                "created_at", "created_at_ts",
                "updated_at", "updated_at_ts");
    }

    /**
     * 다중 파렛트 입고 등록 진입점.
     *
     * 요청 body 형태:
     *   { ownerCode, testRequired, items: [...rows] }
     *   또는 단건 호환: { eqGroupId, barcode, ownerCode, testRequired, items }
     *
     * items[] 의 각 row 는 자체 eqGroupId / barcode / ownerCode / testRequired 를
     * 가질 수 있고, 동일 (eqGroupId, barcode) 끼리 묶어서 파렛트별로 처리한다.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> register(Map<String, Object> requestData) {
        logger.info("[ Inbound ][ Regist ] register - data={}", requestData);

        List<Map<String, Object>> items = (List<Map<String, Object>>) requestData.get("items");
        if (ValueUtil.isEmpty(items)) {
            return Map.of("success", false, "message", "등록할 품목이 없습니다.");
        }

        // (eqGroupId, barcode) 별 그룹핑
        Map<String, List<Map<String, Object>>> groups = new LinkedHashMap<>();
        for (Map<String, Object> row : items) {
            String eqGroupId = strOr(row.get("eqGroupId"), strOr(requestData.get("eqGroupId"), "EQ01"));
            String barcode   = strOr(row.get("barcode"),   strOr(requestData.get("barcode"),   null));

            if (ValueUtil.isEmpty(barcode)) {
                // barcode 누락 행은 별도 키로 그룹핑하되 register 시점에 실패 처리됨
                barcode = "__NO_BARCODE__" + row.hashCode();
            }
            String key = eqGroupId + "|" + barcode;
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }

        // 파렛트별 처리
        List<Map<String, Object>> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (Map.Entry<String, List<Map<String, Object>>> entry : groups.entrySet()) {
            List<Map<String, Object>> palletRows = entry.getValue();
            Map<String, Object> first = palletRows.get(0);

            String eqGroupId    = strOr(first.get("eqGroupId"),    strOr(requestData.get("eqGroupId"),    "EQ01"));
            String barcode      = strOr(first.get("barcode"),      strOr(requestData.get("barcode"),      null));
            String ownerCode    = strOr(first.get("ownerCode"),    strOr(requestData.get("ownerCode"),    "OWN001"));
            Boolean testRequired = (Boolean) firstNonNull(first.get("testRequired"), requestData.get("testRequired"));
            Boolean niaRequired = (Boolean) firstNonNull(first.get("niaRequired"), requestData.get("niaRequired"));
            // host_order.order_type 으로 카테고리 표현 (INBOUND / RETURN_IN). 기본 INBOUND
            String orderType     = strOr(first.get("orderType"),    strOr(requestData.get("orderType"),
                    OrderType.INBOUND.codeAsString()));

            Map<String, Object> palletResult;
            try {
                // 셀프 프록시 호출 — 파렛트 단위 트랜잭션 보장
                palletResult = self.registerSinglePallet(
                        eqGroupId, barcode, ownerCode, testRequired, niaRequired, orderType, palletRows);
            } catch (Exception e) {
                logger.error("[ Inbound ][ Regist ] pallet failed - barcode={}", barcode, e);
                palletResult = Map.of(
                        "success", false,
                        "barcode", ValueUtil.isEmpty(barcode) ? "" : barcode,
                        "eqGroupId", eqGroupId,
                        "message", "처리 중 오류: " + e.getMessage()
                );
            }

            results.add(palletResult);
            if (Boolean.TRUE.equals(palletResult.get("success"))) successCount++;
            else failCount++;
        }

        // 전체 응답
        Map<String, Object> response = new HashMap<>();
        response.put("success", failCount == 0);
        response.put("totalCount", groups.size());
        response.put("successCount", successCount);
        response.put("failCount", failCount);
        response.put("results", results);
        response.put("message", String.format(
                "전체 %d 파렛트 중 성공 %d / 실패 %d",
                groups.size(), successCount, failCount));

        return response;
    }

    /**
     * 단일 파렛트(host_order 1건) 등록 — 트랜잭션 경계.
     * 같은 파렛트 내 item 등록은 모두 성공하거나 모두 롤백.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Map<String, Object> registerSinglePallet(
            String eqGroupId,
            String barcode,
            String ownerCode,
            Boolean testRequired,
            Boolean niaRequired,
            String orderType,
            List<Map<String, Object>> palletRows) {

        // 기본 검증
        if (ValueUtil.isEmpty(barcode) || barcode.startsWith("__NO_BARCODE__")) {
            return failResult(eqGroupId, barcode, "파렛트 바코드가 비어있습니다.");
        }

        // SKU 목록 추출 (distinct)
        List<String> skuCodes = palletRows.stream()
                .map(it -> (String) it.get("itemCode"))
                .filter(ValueUtil::isNotEmpty)
                .distinct()
                .toList();

        if (ValueUtil.isEmpty(skuCodes)) {
            return failResult(eqGroupId, barcode, "품목코드가 비어있습니다.");
        }

        // (SKU, LOT) 중복 차단 — 한 파렛트 내 동일 (itemCode, lotNo) 두 줄 금지 (4.9)
        Set<String> seenSkuLot = new HashSet<>();
        for (Map<String, Object> row : palletRows) {
            String ic = strOr(row.get("itemCode"), null);
            if (ValueUtil.isEmpty(ic)) continue;
            String ln = strOr(row.get("lotNo"), "");
            if (!seenSkuLot.add(ic + "|" + ln)) {
                return failResult(eqGroupId, barcode,
                        "동일 파렛트에 같은 (SKU, LOT) 가 중복되었습니다: itemCode=" + ic + ", lot=" + ln);
            }
        }

        // 품목 마스터 존재 검증
        List<String> missingCodes = findMissingItemCodes(skuCodes, ownerCode);
        if (ValueUtil.isNotEmpty(missingCodes)) {
            Map<String, Object> result = new HashMap<>(failResult(eqGroupId, barcode, "품목 마스터 미등록: " + String.join(", ", missingCodes)));
            result.put("missingCodes", missingCodes);
            return result;
        }

        // 입고 예정 필수 설정 — true 면 plan_id 없는 행 등록 거부(예정 기반 입고만 허용)
        if (inboundPlanService.isPlanRequired()) {
            boolean anyMissingPlan = palletRows.stream()
                    .anyMatch(r -> ValueUtil.isEmpty(strOr(r.get("planId"), null)));
            if (anyMissingPlan) {
                logger.warn("[ Inbound ][ Regist ] plan required but missing - barcode={}", barcode);
                return failResult(eqGroupId, barcode,
                        "입고 예정 필수 설정이 켜져 있습니다. '입고 예정 조회'로 예정을 선택해 등록하세요.");
            }
        }

        // 입고 예정 연계 — 예정별 요청 수량 합산 후 원자 차감(초과 시 예외 → 파렛트 롤백). 예정 미연결 행은 무시.
        Map<String, Integer> planQty = new LinkedHashMap<>();
        for (Map<String, Object> row : palletRows) {
            String planId = strOr(row.get("planId"), null);
            if (ValueUtil.isEmpty(planId)) continue;
            int q = ValueUtil.isNotEmpty(row.get("qty")) ? Integer.parseInt(row.get("qty").toString()) : 0;
            planQty.merge(planId, q, Integer::sum);
        }
        for (Map.Entry<String, Integer> planEntry : planQty.entrySet()) {
            inboundPlanService.consumeQty(planEntry.getKey(), planEntry.getValue());
        }

        // WCS 호스트 오더 요청 조립
        HostOrderApi.Request request = new HostOrderApi.Request();
        request.setHostSystemCode("WCS_UI");
        request.setHostOrderKey("UI-INB-" + System.currentTimeMillis() + "-" + barcode);
        request.setOrderType(ValueUtil.isNotEmpty(orderType)
                ? orderType
                : OrderType.INBOUND.codeAsString());
        request.setBarcode(barcode);
        request.setOwnerCode(ownerCode);
        request.setEqGroupId(eqGroupId);
        request.setPriority(5);
        request.setTestRequired(testRequired);
        request.setNiaRequired(niaRequired);

        boolean defaultTestReq = Boolean.TRUE.equals(testRequired);
        AtomicInteger lineNo = new AtomicInteger(1);
        LocalDate inboundDate = LocalDate.now();

        // row → HostOrderApi.Item 매핑
        request.setItems(palletRows.stream().map(item -> {
            HostOrderApi.Item req = new HostOrderApi.Item();
            req.setLineNo(lineNo.getAndIncrement());
            req.setItemCode((String) item.get("itemCode"));
            req.setLotNo((String) item.get("lotNo"));
            req.setQty(ValueUtil.isNotEmpty(item.get("qty")) ? Integer.parseInt(item.get("qty").toString()) : 0);
            req.setUom((String) item.getOrDefault("uom", "BOX"));
            req.setProduceDate(parseDate(item.get("produceDate")));
            req.setExpiryDate(parseDate(item.get("expiryDate")));

            // row 별 testRequired (없으면 헤더 값 사용)
            Object rowTr = item.get("testRequired");
            boolean rowTestRequired = ValueUtil.isNotEmpty(rowTr) ? Boolean.valueOf(rowTr.toString()) : defaultTestReq;
            req.setTestRequired(rowTestRequired);

            // QC 의뢰 마스터 lookup (시험 대상이면 마스터 필수)
            String userReqNo = (String) item.get("testRequestNo");
            String userTestNo = (String) item.get("testNo");
            QcResolved resolved = rowTestRequired
                    ? resolveQcRequest(inboundDate, req.getItemCode(), req.getLotNo(), userReqNo, userTestNo)
                    : new QcResolved(userReqNo, userTestNo);
            req.setTestRequestNo(resolved.requestNo);
            req.setTestNo(resolved.testNo);
            req.setNiaRequired((Boolean) item.get("niaRequired"));
            return req;
        }).toList());

        // Facade 호출
        HostOrderApi.Response response = wcsFacade.receiveHostOrder(request);

        if (!response.isSuccess()) {
            throw new RuntimeException("Facade 실패: " + response.getErrorDesc());
        }

        // 입고 예정 키를 host_order 에 기록 (단일 예정 기준; 혼재 시 첫 예정)
        if (!planQty.isEmpty()) {
            String planId = planQty.keySet().iterator().next();
            if (planQty.size() > 1) {
                logger.warn("[ Inbound ][ Regist ] multiple plans in one pallet - barcode={}, plans={}", barcode, planQty.keySet());
            }
            this.queryManager.executeBySql(
                    "UPDATE tb_wcs_host_order SET parent_host_order_key = :planId WHERE host_order_key = :hostOrderKey",
                    Map.of("planId", planId, "hostOrderKey", request.getHostOrderKey()));
        }

        // 박스 생성 (실패해도 host_order 는 유지)
        int boxCount = 0;
        try {
            boxCount = palletBoxFactory.generateBoxes(request.getHostOrderKey());
        } catch (Exception e) {
            logger.error("[ Inbound ][ Regist ] generateBoxes failed - hostOrderKey={}",
                    request.getHostOrderKey(), e);
        }

        // 성공 응답
        Map<String, Object> r = new HashMap<>();
        r.put("success", true);
        r.put("eqGroupId", eqGroupId);
        r.put("barcode", barcode);
        r.put("itemCount", palletRows.size());
        r.put("hostOrderKey", request.getHostOrderKey());
        r.put("wcsOrderKey", ValueUtil.isNotEmpty(response.getWcsOrderKey()) ? response.getWcsOrderKey() : "");
        r.put("boxCount", boxCount);
        r.put("message", "등록 완료 (" + palletRows.size() + "건, 박스 " + boxCount + "개)");
        return r;
    }

    /**
     * 입고 row 의 QC 의뢰 결정 — lookup-only.
     *
     * 정책: 시험 의뢰 발행은 UI 측에서 [등록] 직전에 별도 모달 + PDF 동반으로 사전 처리한다.
     * 본 메서드는 lookup 만 수행하며 마스터를 새로 만들지 않는다.
     *   - 마스터 존재 → 그 의뢰 정보로 채움
     *   - 마스터 부재 → 에러 (사용자가 PDF 첨부 후 사전 발행 필요)
     */
    private QcResolved resolveQcRequest(LocalDate inboundDate, String itemCode, String lotNo,
                                        String userReqNo, String userTestNo) {
        if (ValueUtil.isEmpty(itemCode)) {
            return new QcResolved(userReqNo, userTestNo);
        }
        TbWcsQcTestRequest master = qcRequestService.lookup(inboundDate, itemCode, lotNo);
        if (ValueUtil.isEmpty(master)) {
            throw new IllegalStateException(
                    "시험 의뢰 마스터 미발행 - 입고 전에 PDF 첨부로 의뢰를 먼저 발행하세요. itemCode=%s, lot=%s, date=%s"
                            .formatted(itemCode, ValueUtil.isEmpty(lotNo) ? "" : lotNo, inboundDate));
        }
        String testNo = ValueUtil.isNotEmpty(userTestNo) ? userTestNo : master.getTestNo();
        return new QcResolved(master.getTestRequestNo(), testNo);
    }

    /**
     * resolveQcRequest 결과 record.
     */
    private record QcResolved(String requestNo, String testNo) {}

    /**
     * 실패 응답 빌더.
     */
    private Map<String, Object> failResult(String eqGroupId, String barcode, String message) {
        return Map.of(
                "success", false,
                "eqGroupId", ValueUtil.isEmpty(eqGroupId) ? "" : eqGroupId,
                "barcode", ValueUtil.isEmpty(barcode) ? "" : barcode,
                "message", message
        );
    }


    /**
     * varargs 중 첫 non-null 값. 없으면 null.
     */
    private static Object firstNonNull(Object... values) {
        for (Object v : values) if (ValueUtil.isNotEmpty(v)) return v;
        return null;
    }

    /**
     * "yyyy-MM-dd" 문자열 → Date. 파싱 실패 시 null + 에러 로그.
     */
    private Date parseDate(Object value) {
        if (ValueUtil.isEmpty(value)) return null;
        String s = value.toString().trim();
        if (ValueUtil.isEmpty(s)) return null;
        if (s.length() >= 10) s = s.substring(0, 10);
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setLenient(false);
            return sdf.parse(s);
        } catch (Exception e) {
            logger.error("[ Inbound ][ Regist ] parseDate failed - value={}", value, e);
            return null;
        }
    }

    /**
     * 품목 코드 리스트 중 마스터에 없는 것만 반환. UNNEST 로 LEFT JOIN 1회로 처리.
     */
    private List<String> findMissingItemCodes(List<String> itemCodes, String itemOwner) {
        String sql = """
        SELECT item.code AS item_code
          FROM UNNEST(CAST(:itemCodes AS text[])) AS item(code)
          LEFT JOIN tb_inventory_item_mst m
            ON m.item_code  = item.code
           AND m.item_owner = :itemOwner
         WHERE m.id IS NULL
        """;
        Map<String, Object> params = new HashMap<>();
        params.put("itemCodes", itemCodes.toArray(new String[0]));
        params.put("itemOwner", itemOwner);
        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);
        return rows.stream().map(r -> String.valueOf(r.get("item_code"))).toList();
    }

    /**
     * 여러 host_order_key 단위로 입고 등록 삭제 (예정 수량 환원 → item → header 순).
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> deleteByHostOrderKeys(List<String> hostOrderKeys) {
        if (ValueUtil.isEmpty(hostOrderKeys)) {
            return Map.of("success", false, "message", "삭제할 항목이 없습니다.");
        }
        int deletedOrders = 0;
        for (String hostOrderKey : hostOrderKeys) {
            // 연계된 입고 예정 수량 환원 (item 삭제 전에 수량 합산)
            releasePlanQty(hostOrderKey);

            Map<String, Object> params = ValueUtil.newMap("hostOrderKey", hostOrderKey);
            this.queryManager.executeBySql(
                    "DELETE FROM tb_wcs_host_order_item WHERE host_order_key = :hostOrderKey", params);
            this.queryManager.executeBySql(
                    "DELETE FROM tb_wcs_host_order WHERE host_order_key = :hostOrderKey", params);
            deletedOrders++;
        }
        logger.info("[ Inbound ][ Regist ] deleted - orderCount={}", deletedOrders);
        return Map.of("success", true, "message", deletedOrders + "건 삭제되었습니다.");
    }

    /**
     * 입고 주문 삭제 시 연계 입고 예정의 누적 수량 환원. 예정 미연계면 no-op.
     */
    @SuppressWarnings("rawtypes")
    private void releasePlanQty(String hostOrderKey) {
        String sql = """
                SELECT o.parent_host_order_key       AS plan_id,
                       COALESCE(SUM(i.qty), 0)        AS total_qty
                  FROM tb_wcs_host_order o
                  LEFT JOIN tb_wcs_host_order_item i
                    ON i.host_order_key = o.host_order_key
                 WHERE o.host_order_key = :hostOrderKey
                 GROUP BY o.parent_host_order_key
                """;
        List<Map> rows = this.queryManager.selectListBySql(
                sql, ValueUtil.newMap("hostOrderKey", hostOrderKey), Map.class, 0, 0);
        if (ValueUtil.isEmpty(rows)) return;

        Object planId = rows.get(0).get("plan_id");
        if (ValueUtil.isEmpty(planId)) return;
        Object totalQty = rows.get(0).get("total_qty");
        int qty = (totalQty instanceof Number n) ? n.intValue() : 0;
        inboundPlanService.releaseQty(planId.toString(), qty);
    }
}
