package operato.logis.wcs.service.impl.query.inventory;

import operato.logis.inventory.consts.StockStatus;
import xyz.elidom.util.ValueUtil;

import java.util.EnumMap;
import java.util.Map;

/**
 * 셀 상태(state_code) 결정 로직의 단일 출처 (SSOT).
 *
 * 사용처:
 *   - Dashboard2D: TbEcs2dItemService.getLayoutsWithRealStatus()
 *   - CellState2D: CellStateService.getCellsByGroup()
 *
 * 두 화면이 동일한 룰로 셀을 표시하도록 SQL CASE 문을 한 곳에서 생성한다.
 *
 * 새 상태 추가 시:
 *   1) STATE_PRIORITY 에 한 줄 추가 (StockStatus enum 값 + 우선순위)
 *   2) stateCodeCaseSql() 의 WHEN 절에 매핑 한 줄 추가
 *
 * StockStatus enum 은 외부 모듈(operato.logis.inventory.consts) 이므로
 * 우선순위 메타데이터를 EnumMap 으로 별도 관리한다.
 */
public final class CellStateClassifier {

    private CellStateClassifier() {}

    /**
     * 셀 대표 상태 우선순위.
     *
     * 한 stock_id 아래 여러 tb_inventory_stock row 가 있을 때(혼적 파렛트 등)
     * 어느 상태를 셀 대표로 쓸지 결정. priority 값이 작을수록 우선 (1 = 최우선).
     * 등록되지 않은 상태(IDLE 등)는 셀 대표 상태 후보가 아님.
     */
    public static final Map<StockStatus, Integer> STATE_PRIORITY;
    static {
        Map<StockStatus, Integer> m = new EnumMap<>(StockStatus.class);
        m.put(StockStatus.HOLD,          1);   // 최우선 (입출고 불가 — 시험/국검/반품/폐기 격리)
        m.put(StockStatus.INBOUND_READY, 3);
        m.put(StockStatus.INBOUND,       4);
        m.put(StockStatus.OUTBOUND,      5);
        m.put(StockStatus.RELOCATION,    6);
        m.put(StockStatus.HOST_PENDING,  7);   // HOST 예약 — 셔틀 미생성, 진행 중 작업보다는 약함
        // IDLE 미등록 → 셀 대표 상태 후보 아님
        STATE_PRIORITY = m;
    }

    /**
     * "stock_status → 우선순위 정수" 매핑 CASE 문. LATERAL 서브쿼리의 ORDER BY 에서 사용.
     */
    public static String priorityCaseSql() {
        StringBuilder sb = new StringBuilder("CASE s.stock_status");
        STATE_PRIORITY.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(e -> sb
                        .append(" WHEN ").append(e.getKey().value())
                        .append(" THEN ").append(e.getValue()));
        sb.append(" ELSE 999 END");
        return sb.toString();
    }

    /**
     * LATERAL 서브쿼리 — 같은 stock_id 의 여러 row 중 우선순위 1위 1개만 가져옴.
     * 셀당 tb_inventory_stock 스캔 1회로 끝남 (기존 EXISTS 다회 → 1회).
     *
     * @param locAlias 호출 측 SQL 에서 tb_inventory_location 의 별칭 (예: "wcs_loc" / "loc")
     * @param stkAlias 결과 별칭 (예: "stk")
     */
    public static String lateralStockSubquerySql(String locAlias, String stkAlias) {
        return String.format("""
            LEFT JOIN LATERAL (
                SELECT s.stock_status, s.stock_type, s.expired_datetime
                  FROM tb_inventory_stock s
                 WHERE s.stock_id    = %s.stock_id
                   AND s.eq_group_id = %s.loc_group
                 ORDER BY %s
                 LIMIT 1
            ) %s ON %s.stock_id IS NOT NULL
            """,
                locAlias, locAlias, priorityCaseSql(), stkAlias, locAlias);
    }

    /**
     * 셀 state_code 결정 CASE 문 — legend-spec.ts RACK_STATES 의 code 와 1:1 매핑.
     *
     * 주요 분기:
     *   - ABNORMAL: QC 부적합(stock_type=QC_FAIL) 또는 사용기한 만료(expired_datetime < NOW()) — 한 색상, 사유는 상세에서 분리
     *   - HOST_PENDING: host_order 단계 예약. location.task_id 는 아직 NULL (셔틀 미생성) 이므로 stock_status 단독 판정, PRODUCT 폴백 직전에 가로채야 함
     */
    public static String stateCodeCaseSql(
            String rackAlias,
            String locAlias,
            String stkAlias,
            String prefixCondition) {

        StringBuilder sb = new StringBuilder("CASE ");

        if (ValueUtil.isNotEmpty(prefixCondition)) {
            sb.append(" WHEN ").append(prefixCondition).append(" THEN NULL ");
        }

        sb.append(String.format("""
          WHEN %s.drive_only_yn = true                                THEN 'DRIVE'
          WHEN NULLIF(%s.stock_id, '') = 'DOUBLE_IN'                  THEN 'DOUBLE_IN'
          WHEN NULLIF(%s.stock_id, '') = 'EMPTY_OUT'                  THEN 'EMPTY_OUT'
          WHEN NULLIF(%s.task_id, '')  IS NOT NULL
           AND NULLIF(%s.stock_id, '') IS NULL                        THEN 'INBOUND'
          WHEN %s.stock_type = 'QC_FAIL'                              THEN 'ABNORMAL'
          WHEN %s.stock_type = 'NIA_FAIL'                             THEN 'ABNORMAL'
          WHEN %s.expired_datetime IS NOT NULL
           AND %s.expired_datetime < NOW()                            THEN 'ABNORMAL'
          WHEN %s.stock_type = 'QC_PENDING'                           THEN 'QC_PENDING'
          WHEN %s.stock_type = 'NIA_PENDING'                          THEN 'NIA_PENDING'
          WHEN %s.stock_type = 'RETURN'                               THEN 'RETURN'
          WHEN %s.stock_type = 'DISPOSAL'                             THEN 'DISPOSAL'
          WHEN NULLIF(%s.task_id, '')  IS NOT NULL
           AND %s.stock_status = %d                                   THEN 'INBOUND_READY'
          WHEN NULLIF(%s.task_id, '')  IS NOT NULL
           AND %s.stock_status = %d                                   THEN 'INBOUND'
          WHEN NULLIF(%s.task_id, '')  IS NOT NULL
           AND NULLIF(%s.stock_id, '') IS NOT NULL                    THEN 'OUTBOUND'
          WHEN %s.stock_status = %d                                   THEN 'HOST_PENDING'
          WHEN NULLIF(%s.stock_id, '') IS NOT NULL                    THEN 'PRODUCT'
          ELSE 'EMPTY'
        END
        """,
                rackAlias,
                locAlias, locAlias,
                locAlias, locAlias,
                stkAlias,
                stkAlias,
                stkAlias, stkAlias,
                stkAlias,
                stkAlias,
                stkAlias,
                stkAlias,
                locAlias, stkAlias, StockStatus.INBOUND_READY.value(),
                locAlias, stkAlias, StockStatus.INBOUND.value(),
                locAlias, locAlias,
                stkAlias, StockStatus.HOST_PENDING.value(),
                locAlias
        ));

        return sb.toString();
    }
}
