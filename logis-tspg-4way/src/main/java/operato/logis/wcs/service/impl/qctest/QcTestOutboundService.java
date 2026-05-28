package operato.logis.wcs.service.impl.qctest;

import operato.logis.wcs.consts.BoxStatus;
import operato.logis.wcs.service.impl.order.issuer.QcTestOutboundIssuer;
import operato.logis.wcs.service.impl.order.lookup.OrderLookupUtils;
import operato.logis.wcs.service.impl.query.common.AbstractFlattenedPagedService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.util.ValueUtil;

import static operato.logis.wcs.common.util.lang.CommonUtils.stringOf;

import java.util.*;

/**
 * 시험 대상 재고 출고 서비스 (QcTest 흐름 일부).
 *
 * 동작:
 *   - stock_type=QC_PENDING 인 재고만 조회 (시험 대기 카테고리)
 *   - issueOutbound: 선택 stock 1건당 OUTBOUND(SAMPLE_OUT) shuttle 만 발행
 *     재입고 동시 발행은 하지 않으며 사용자가 PalletWorkstation 채취 후
 *     ReinboundIssuer.issueSampleReinbound 로 트리거한다
 *   - 별도 host_order 발행 안 함 — 원본 입고 host_order(origin_host_order_key) 에 attach
 *
 * 시험 정보(testRequired / testRequestNo / testNo / testStatus) 는 모두 ITEM 단위.
 * 셔틀 헤더에는 testRequired 집계 캐시만 둔다.
 */
@Service
@RequiredArgsConstructor
public class QcTestOutboundService extends AbstractFlattenedPagedService {

    private final OrderLookupUtils orderLookup;
    private final QcTestOutboundIssuer qcTestOutboundIssuer;

    private static final String INNER_SQL = """
    SELECT s.stock_id                        AS stock_id,
           l.loc_group                       AS loc_group,
           l.loc_id                          AS loc_id,
           s.item_code                       AS item_code,
           m.item_name                       AS item_name,
           s.lot_no                          AS lot_no,
           m.item_unit                       AS item_unit,
           s.item_qty                        AS item_qty,
           s.inb_datetime::date              AS inb_datetime,
           s.expired_datetime::date          AS expired_datetime,
           s.inb_datetime                    AS inb_datetime_ts,
           s.expired_datetime                AS expired_datetime_ts,
           l.task_id                         AS task_id,
           s.item_owner                      AS item_owner,
           s.eq_group_id                     AS eq_group_id,
           s.origin_host_order_key           AS origin_host_order_key,
           hoi.test_required                 AS test_required,
           hoi.test_request_no               AS test_request_no,
           hoi.test_no                       AS test_no,
           hoi.test_status                   AS test_status
      FROM tb_inventory_stock s
      LEFT JOIN tb_inventory_location l
        ON l.stock_id = s.stock_id
       AND l.loc_group = s.eq_group_id
      LEFT JOIN tb_inventory_item_mst m
        ON m.item_code = s.item_code
       AND m.item_owner = s.item_owner
      LEFT JOIN tb_wcs_host_order_item hoi
        ON hoi.host_order_key = s.origin_host_order_key
       AND hoi.item_code       = s.item_code
       AND COALESCE(hoi.lot_no, '') = COALESCE(s.lot_no, '')
     WHERE s.is_enabled = TRUE
       AND COALESCE(s.item_qty, 0) > 0
       AND s.stock_type = 'QC_PENDING'
    """;

    private static final String DEFAULT_ORDER = " ORDER BY t.loc_id, t.item_code ";

    @Override protected String getInnerSql() { return INNER_SQL; }
    @Override protected String getDefaultOrder() { return DEFAULT_ORDER; }

    private static final Set<String> ALLOWED_COLUMNS = Set.of(
            "stock_id", "loc_group", "loc_id",
            "item_code", "item_name", "lot_no", "item_unit", "item_qty",
            "inb_datetime", "expired_datetime", "inb_datetime_ts", "expired_datetime_ts",
            "task_id", "item_owner", "eq_group_id",
            "origin_host_order_key",
            "test_required", "test_request_no", "test_no", "test_status"
    );

    @Override protected Set<String> allowedColumns() { return ALLOWED_COLUMNS; }

    // 표시용 ::date alias → 원본 timestamp alias (날짜 필터 sargable)
    @Override protected Map<String, String> dateColumns() {
        return Map.of(
                "inb_datetime",     "inb_datetime_ts",
                "expired_datetime", "expired_datetime_ts");
    }

    /**
     * 선택된 stock 들에 대해 SAMPLE_OUT 셔틀을 1건씩 발급. 실패해도 다른 stock 진행.
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> issueOutbound(Map<String, Object> requestData) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> stocks = (List<Map<String, Object>>) requestData.get("stocks");
        if (ValueUtil.isEmpty(stocks)) {
            return Map.of("success", false, "message", "출고할 재고가 선택되지 않았습니다.");
        }

        String portCode = (String) requestData.getOrDefault("portCode", "");
        String eqGroupIdParam = (String) requestData.getOrDefault("eqGroupId", "");

        List<Map<String, Object>> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        // stock 1건당 1셔틀 발급 시도. 실패해도 진행
        for (Map<String, Object> stock : stocks) {
            Map<String, Object> one = new HashMap<>();
            try {
                String stockId = stringOf(stock.getOrDefault("stockId", stock.get("stock_id")));
                String eqGroupId = ValueUtil.isNotEmpty(eqGroupIdParam)
                        ? eqGroupIdParam
                        : stringOf(stock.getOrDefault("eq_group_id", "EQ01"));
                String originHostKey = stringOf(stock.get("origin_host_order_key"));

                if (ValueUtil.isEmpty(originHostKey)) {
                    throw new IllegalStateException("origin_host_order_key 누락 — 시험 대상 출고 불가");
                }

                // origin 존재 검증
                orderLookup.getHostOrderOrThrow(originHostKey);

                // SAMPLE_OUT 발급
                Map<String, Object> issued = qcTestOutboundIssuer.issue(
                        eqGroupId,
                        stockId,
                        portCode,
                        stringOrNull(stock.get("test_request_no")),
                        stringOrNull(stock.get("test_no")));

                one.put("success", true);
                one.put("stockId", stockId);
                one.put("hostOrderKey", issued.get("hostOrderKey"));
                successCount++;
            } catch (Exception e) {
                logger.error("[ Qctest ][ Outbound ] issue failed - stock={}", stock, e);
                one.put("success", false);
                one.put("errorDesc", e.getMessage());
                failCount++;
            }
            results.add(one);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", failCount == 0);
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("results", results);
        result.put("message", String.format("시험 출고 지시 완료: 성공 %d건, 실패 %d건", successCount, failCount));
        return result;
    }

    /**
     * 출고 가능 포트 목록 — OUTBOUND_PORT, IN_OUTBOUND_PORT.
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

    /**
     * 의뢰번호 단위 출고 파렛트 추천 — 가장 작은 box_seq 를 보유한 QC_PENDING 파렛트 1건.
     * 후보 박스 = SCANNED + remaining_qty>0 + 미출고(outbound_order_key NULL).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> recommendPallet(String testRequestNo) {
        // 의뢰번호 필수
        if (ValueUtil.isEmpty(testRequestNo)) {
            return Map.of("found", false, "message", "의뢰번호가 비어 있습니다.");
        }

        // 추천 파렛트 1건 — 최소 box_seq 보유 QC_PENDING 파렛트 (box→stock 은 origin_host_order_key 로 연계)
        String palletSql = """
            SELECT s.stock_id                  AS stock_id,
                   s.eq_group_id               AS eq_group_id,
                   s.origin_host_order_key     AS host_order_key,
                   s.test_no                   AS test_no,
                   b.pallet_barcode            AS pallet_barcode,
                   l.loc_id                    AS loc_id,
                   MIN(b.box_seq)              AS min_box_seq
              FROM tb_wcs_pallet_box b
              JOIN tb_inventory_stock s
                ON s.origin_host_order_key = b.host_order_key
               AND s.stock_type = 'QC_PENDING'
               AND s.is_enabled = TRUE
               AND COALESCE(s.item_qty, 0) > 0
              LEFT JOIN tb_inventory_location l
                ON l.stock_id = s.stock_id
               AND l.loc_group = s.eq_group_id
             WHERE b.test_request_no = :reqNo
               AND b.box_status = :scanned
               AND COALESCE(b.remaining_qty, 0) > 0
               AND b.outbound_order_key IS NULL
             GROUP BY s.stock_id, s.eq_group_id, s.origin_host_order_key, s.test_no, b.pallet_barcode, l.loc_id
             ORDER BY MIN(b.box_seq) ASC
            """;
        Map<String, Object> params = new HashMap<>();
        params.put("reqNo", testRequestNo);
        params.put("scanned", BoxStatus.SCANNED.code());

        @SuppressWarnings("unchecked")
        List<Map> palletRows = this.queryManager.selectListBySql(palletSql, params, Map.class, 0, 0);
        if (ValueUtil.isEmpty(palletRows)) {
            logger.debug("[ Qctest ][ Recommend ] no candidate pallet - reqNo={}", testRequestNo);
            return Map.of("found", false, "message", "추천 가능한 파렛트가 없습니다.");
        }

        // 정렬 첫 행 = 추천 파렛트
        @SuppressWarnings("unchecked")
        Map<String, Object> pallet = (Map<String, Object>) palletRows.get(0);

        // 추천 파렛트의 해당 의뢰 박스 목록 (box_seq 순)
        String boxSql = """
            SELECT box_seq        AS box_seq,
                   box_barcode    AS box_barcode,
                   remaining_qty  AS remaining_qty
              FROM tb_wcs_pallet_box
             WHERE test_request_no = :reqNo
               AND pallet_barcode = :pallet
               AND box_status = :scanned
               AND COALESCE(remaining_qty, 0) > 0
               AND outbound_order_key IS NULL
             ORDER BY box_seq
            """;
        params.put("pallet", pallet.get("pallet_barcode"));
        @SuppressWarnings("unchecked")
        List<Map> boxes = this.queryManager.selectListBySql(boxSql, params, Map.class, 0, 0);

        logger.debug("[ Qctest ][ Recommend ] pallet recommended - reqNo={}, pallet={}, stockId={}, minBoxSeq={}, boxCount={}",
                testRequestNo, pallet.get("pallet_barcode"), pallet.get("stock_id"),
                pallet.get("min_box_seq"), boxes == null ? 0 : boxes.size());

        // pallet 행 + found/의뢰번호/박스목록 합쳐 반환
        Map<String, Object> result = new HashMap<>(pallet);
        result.put("found", true);
        result.put("test_request_no", testRequestNo);
        result.put("boxes", boxes);
        return result;
    }

    /**
     * 의뢰번호 추천 파렛트 출고 — 서버에서 추천을 재계산(클라이언트 stock_id 미신뢰·동시성 안전) 후
     * 기존 issuer 로 SAMPLE_OUTBOUND 발행. issuer 의 ALREADY_RESERVED 예외는 그대로 전파.
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> issueRecommended(String testRequestNo, String portCode) {
        // 추천 재계산
        Map<String, Object> rec = recommendPallet(testRequestNo);
        if (!Boolean.TRUE.equals(rec.get("found"))) {
            return Map.of("success", false,
                    "message", stringOf(rec.getOrDefault("message", "추천 파렛트가 없습니다.")));
        }

        // 추천 파렛트의 stock 으로 기존 issuer 재사용
        String stockId = stringOf(rec.get("stock_id"));
        String eqGroupId = stringOf(rec.get("eq_group_id"));
        String testNo = stringOrNull(rec.get("test_no"));
        Map<String, Object> issued =
                qcTestOutboundIssuer.issue(eqGroupId, stockId, portCode, testRequestNo, testNo);

        logger.info("[ Qctest ][ Recommend ] recommended outbound issued - reqNo={}, pallet={}, stockId={}, port={}",
                testRequestNo, rec.get("pallet_barcode"), stockId, portCode);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("palletBarcode", rec.get("pallet_barcode"));
        result.put("stockId", stockId);
        result.put("hostOrderKey", issued.get("hostOrderKey"));
        result.put("message", "추천 파렛트 출고 지시 완료");
        return result;
    }

    /**
     * Object → 비면 null, 아니면 trim 한 문자열.
     */
    private String stringOrNull(Object v) {
        if (ValueUtil.isEmpty(v)) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }
}
