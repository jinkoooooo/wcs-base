package operato.logis.wcs.service.impl.query.inbound;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.common.util.lang.ParseUtils;
import operato.logis.wcs.common.util.time.LocalDateUtils;
import operato.logis.wcs.consts.WcsError;
import operato.logis.wcs.entity.TbWcsInboundPlan;
import operato.logis.wcs.service.impl.qctest.QcRequestService;
import operato.logis.wcs.service.impl.query.common.AbstractFlattenedPagedService;
import operato.logis.wcs.service.repository.InboundPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.util.SettingUtil;
import xyz.elidom.util.ValueUtil;

import static operato.logis.wcs.common.util.lang.CommonUtils.strOr;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 입고 예정 마스터 조회/저장/삭제 + 입고 주문 연계(수량 가/감산) 서비스.
 *
 * 정책:
 * - 한 행 = (입고예정일, SKU, LOT) 예정 1건
 * - test_required=true 면 저장 시 qc_test_request 를 멱등 생성
 * - 입고 주문 발행 시 planned_qty 한도 내에서만 ordered_qty 가산
 */
@Service
@RequiredArgsConstructor
public class InboundPlanService extends AbstractFlattenedPagedService {

    private final InboundPlanRepository repository;
    private final QcRequestService qcRequestService;

    /** 입고 예정 필수 설정 키 — "true" 면 입고 주문을 예정 기반으로만 허용(plan_id 없는 행 거부). 기본 false. */
    private static final String SETTING_INBOUND_PLAN_REQUIRED = "wcs.inbound.plan.required";

    // 잔여수량(remaining_qty) = 예정수량 - 누적주문수량 을 함께 내려 화면/팝업에서 활용
    // test_required = true 인 경우에 한하여 QC 의뢰 정보를 함께 조회 (plan_date, item_code, lot_no 기준 조인)
    private static final String INNER_SQL = """
    SELECT p.id,
           p.plan_date::date                   AS plan_date,
           p.item_code                          AS item_code,
           m.item_name                          AS item_name,
           p.lot_no                             AS lot_no,
           p.item_owner                         AS item_owner,
           p.planned_qty                        AS planned_qty,
           p.ordered_qty                        AS ordered_qty,
           (p.planned_qty - p.ordered_qty)      AS remaining_qty,
           p.uom                                AS uom,
           p.produce_date::date                 AS produce_date,
           p.expiry_date::date                  AS expiry_date,
           p.test_required                      AS test_required,
           p.nia_required                       AS nia_required,
           p.plan_date                          AS plan_date_ts,
           p.created_at::date                   AS created_at,
           p.created_at                         AS created_at_ts,
           q.test_request_no                    AS test_request_no,
           q.test_no                            AS test_no,
           q.status                             AS status
      FROM tb_wcs_inbound_plan p
      LEFT JOIN tb_inventory_item_mst m
        ON m.item_code = p.item_code
       AND m.item_owner = p.item_owner
      LEFT JOIN tb_wcs_qc_test_request q
        ON p.test_required = true 
       AND q.inbound_date::date = p.plan_date::date
       AND q.item_code = p.item_code
       AND q.lot_no = p.lot_no
    """;

    private static final String DEFAULT_ORDER = " ORDER BY t.created_at_ts DESC, t.item_code ";

    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            "id", "plan_date", "item_code", "item_name", "lot_no", "item_owner",
            "planned_qty", "ordered_qty", "remaining_qty", "uom",
            "produce_date", "expiry_date", "test_required", "nia_required",
            "plan_date_ts", "created_at", "created_at_ts",
            "test_request_no", "test_no", "status"
    );

    @Override protected String getInnerSql()         { return INNER_SQL; }
    @Override protected String getDefaultOrder()     { return DEFAULT_ORDER; }
    @Override protected Set<String> allowedColumns() { return ALLOWED_COLUMNS; }

    // 표시용 ::date alias → 원본 timestamp alias (날짜 필터 sargable)
    @Override protected Map<String, String> dateColumns() {
        return Map.of(
                "plan_date", "plan_date_ts",
                "created_at", "created_at_ts");
    }

    /**
     * 그리드 다건 저장 — 모두 신규 INSERT. test_required 행은 QC 의뢰 자동 생성.
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> save(List<Map<String, Object>> rows) {
        if (ValueUtil.isEmpty(rows)) {
            return Map.of("success", false, "message", "저장할 입고 예정이 없습니다.");
        }

        int created = 0;
        for (Map<String, Object> row : rows) {
            // 입고 예정일 — 미지정 시 오늘
            String dateStr = strOr(row.get("planDate"), null);
            LocalDate planDate = ValueUtil.isEmpty(dateStr) ? LocalDate.now() : ParseUtils.parseDate(dateStr);

            // 필수값 검증
            String itemCode = strOr(row.get("itemCode"), null);
            Integer plannedQty = ParseUtils.parseIntOrNull(row.get("plannedQty"));
            if (ValueUtil.isEmpty(itemCode) || plannedQty == null || plannedQty <= 0) {
                throw new ElidomRuntimeException(WcsError.INVALID_PARAMETER.codeAsString(),
                        "품목코드와 입고 예정 수량(>0)은 필수입니다.");
            }

            // 엔티티 구성
            TbWcsInboundPlan e = new TbWcsInboundPlan();
            e.setPlanDate(LocalDateUtils.toDate(planDate));
            e.setItemCode(itemCode);
            e.setLotNo(ValueUtil.isEmpty(row.get("lotNo")) ? "" : row.get("lotNo").toString());
            e.setItemOwner(strOr(row.get("itemOwner"), strOr(row.get("ownerCode"), null)));
            e.setPlannedQty(plannedQty);
            e.setOrderedQty(0);
            e.setUom(strOr(row.get("uom"), "EA"));
            e.setProduceDate(ParseUtils.parseDateToDate(strOr(row.get("produceDate"), null)));
            e.setExpiryDate(ParseUtils.parseDateToDate(strOr(row.get("expiryDate"), null)));
            e.setTestRequired(boolOf(row.get("testRequired")));
            e.setNiaRequired(boolOf(row.get("niaRequired")));

            repository.insert(e);
            created++;

            // QC 시험 대상이면 의뢰 마스터 즉시 생성 (멱등)
            if (Boolean.TRUE.equals(e.getTestRequired())) {
                ensureQcRequest(planDate, e);
            }
        }

        logger.info("[ Inbound ][ Plan ] saved - created={}", created);
        return Map.of("success", true, "createdCount", created);
    }

    /**
     * 입고 예정 기반 QC 의뢰 멱등 생성 — 의뢰구분/내용/제조수량은 QC 화면에서 보완(여기선 미입력).
     */
    private void ensureQcRequest(LocalDate planDate, TbWcsInboundPlan e) {
        // 이미 (입고일자, SKU, LOT) 의뢰가 있으면 재생성하지 않음
        if (ValueUtil.isNotEmpty(qcRequestService.lookup(planDate, e.getItemCode(), e.getLotNo()))) {
            logger.debug("[ Inbound ][ Plan ] qc request exists, skip - itemCode={}, lot={}",
                    e.getItemCode(), e.getLotNo());
            return;
        }
        // 입고 예정 수량을 의뢰 incoming_qty 로 승계
        qcRequestService.createWithPdfId(planDate, e.getItemCode(), e.getLotNo(),
                null,                       // pdfFileId
                null, null,                 // testWfType, testReqDesc
                e.getProduceDate(), e.getExpiryDate(),  // manufacturedDate, expiryDate
                null, e.getPlannedQty(),    // manufacturedQty, incomingQty
                null, null);                // reqDept, submitterOrder

        logger.info("[ Inbound ][ Plan ] qc request auto-created - itemCode={}, lot={}, incomingQty={}",
                e.getItemCode(), e.getLotNo(), e.getPlannedQty());
    }

    /**
     * 다건 삭제 — 입고 주문이 진행된(ordered_qty>0) 예정은 차단.
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> deleteByIds(List<String> ids) {
        if (ValueUtil.isEmpty(ids)) {
            return Map.of("success", false, "message", "삭제할 항목이 없습니다.");
        }
        int deleted = 0;
        for (String id : ids) {
            TbWcsInboundPlan e = repository.findById(id);
            if (ValueUtil.isEmpty(e)) continue;
            if (e.getOrderedQty() != null && e.getOrderedQty() > 0) {
                throw new ElidomRuntimeException(WcsError.INVALID_PARAMETER.codeAsString(),
                        "입고 주문이 진행된 예정은 삭제할 수 없습니다. itemCode=" + e.getItemCode());
            }
            repository.delete(e);
            deleted++;
        }
        logger.info("[ Inbound ][ Plan ] deleted - count={}", deleted);
        return Map.of("success", true, "message", deleted + "건 삭제되었습니다.");
    }

    /**
     * 입고 주문 발행 시 예정 수량 차감 — 누적+요청 > 예정수량이면 예외(파렛트 롤백).
     */
    @Transactional(rollbackFor = Exception.class)
    public void consumeQty(String planId, int qty) {
        if (ValueUtil.isEmpty(planId) || qty <= 0) return;
        int affected = repository.addOrderedQty(planId, qty);
        if (affected <= 0) {
            throw new IllegalStateException(
                    "입고 예정 수량 초과 또는 예정 미존재 - planId=%s, qty=%d".formatted(planId, qty));
        }
        logger.info("[ Inbound ][ Plan ] qty consumed - planId={}, qty={}", planId, qty);
    }

    /**
     * 입고 주문 삭제 시 예정 수량 환원.
     */
    @Transactional(rollbackFor = Exception.class)
    public void releaseQty(String planId, int qty) {
        if (ValueUtil.isEmpty(planId) || qty <= 0) return;
        repository.releaseOrderedQty(planId, qty);
        logger.info("[ Inbound ][ Plan ] qty released - planId={}, qty={}", planId, qty);
    }

    /**
     * 입고 예정 필수 여부 — 도메인 설정값(기본 false). 입고 등록 강제 + 프론트 버튼 게이팅 공용.
     */
    public boolean isPlanRequired() {
        String value = SettingUtil.getValue(Domain.currentDomainId(), SETTING_INBOUND_PLAN_REQUIRED, "false");
        return "true".equalsIgnoreCase(value);
    }

    /** Object → boolean (그리드 문자열 "true"/"false" 안전 변환). */
    private static boolean boolOf(Object v) {
        if (v instanceof Boolean b) return b;
        if (v == null) return false;
        return Boolean.parseBoolean(v.toString().trim());
    }
}