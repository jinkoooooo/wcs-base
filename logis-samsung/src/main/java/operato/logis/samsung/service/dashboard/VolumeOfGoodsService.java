package operato.logis.samsung.service.dashboard;

import lombok.RequiredArgsConstructor;
import operato.logis.samsung.dto.dashboard.DashboardVolumeOfGoods;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VolumeOfGoodsService extends AbstractQueryService {

    public List<DashboardVolumeOfGoods> getVolumeOfGoodsGroupByDay(Date chooseDate) {
        Map<String, Object> param = ValueUtil.newMap("chooseDate", chooseDate);

        // 1. 입고 데이터 (deliverySql): 최근 7일 (일별 집계)
        String deliverySql = """
            WITH date_series AS (
                SELECT
                    generate_series(
                        :chooseDate::DATE - INTERVAL '6 days',
                        :chooseDate::DATE,
                        '1 day'::interval
                    )::DATE AS date_key
            ),
            delivery_data AS (
                SELECT
                    inbound_date::DATE AS date_key,
                    COUNT(DISTINCT cntr_no) AS cntr_qty,
                    SUM(item_qty) AS plan_qty
                FROM
                    tb_mw_inbound_delivery
                WHERE
                    inbound_date >= (:chooseDate::DATE - INTERVAL '6 days')
                    AND inbound_date <= :chooseDate::DATE
                GROUP BY
                    inbound_date::DATE
            )
            SELECT
                TO_CHAR(ds.date_key, 'MM/DD') AS inbound_date,
                COALESCE(dd.cntr_qty, 0) AS cntr_qty,
                COALESCE(dd.plan_qty, 0) AS plan_qty
            FROM
                date_series ds
            LEFT JOIN
                delivery_data dd ON ds.date_key = dd.date_key
            ORDER BY
                ds.date_key ASC
        """;
        List<DashboardVolumeOfGoods> deliveryResult = this.queryManager.selectListBySql(deliverySql, param, DashboardVolumeOfGoods.class, 0, 0);

        // 2. 피킹 데이터 (palletizingSql): 최근 7일 (일별 집계)
        String palletizingSql = """
            WITH date_series AS (
                SELECT
                    generate_series(
                        :chooseDate::DATE - INTERVAL '6 days',
                        :chooseDate::DATE,
                        '1 day'::interval
                    )::DATE AS date_key
            ),
            palletizing_data AS (
                SELECT
                    created_at::DATE AS date_key,
                    COUNT(*) AS pass_qty
                FROM
                    tb_mw_box_conveyor_info
                WHERE
                    is_picked = TRUE
                    AND created_at::DATE >= (:chooseDate::DATE - INTERVAL '6 days')
                    AND created_at::DATE <= :chooseDate::DATE
                GROUP BY
                    created_at::DATE
            )
            SELECT
                TO_CHAR(ds.date_key, 'MM/DD') AS inbound_date,
                COALESCE(pd.pass_qty, 0) AS pass_qty
            FROM
                date_series ds
            LEFT JOIN
                palletizing_data pd ON ds.date_key = pd.date_key
            ORDER BY
                ds.date_key ASC
        """;
        List<DashboardVolumeOfGoods> palletizingResult = this.queryManager.selectListBySql(palletizingSql, param, DashboardVolumeOfGoods.class, 0, 0);

        // 3. 두 결과를 병합
        return mergeVolumeOfGoodsResults(deliveryResult, palletizingResult);
    }

    public List<DashboardVolumeOfGoods> getVolumeOfGoodsGroupByWeek(Date chooseDate) {
        Map<String, Object> param = ValueUtil.newMap("chooseDate", chooseDate);

        // 1. 입고 데이터 (deliverySql): 최근 12주 (주간 집계)
        String deliverySql = """
            WITH date_series AS (
                SELECT
                    generate_series(
                        DATE_TRUNC('week', :chooseDate::DATE) - INTERVAL '11 weeks',
                        DATE_TRUNC('week', :chooseDate::DATE),
                        '1 week'::interval
                    )::DATE AS date_key
            ),
            delivery_data AS (
                SELECT
                    DATE_TRUNC('week', inbound_date)::DATE AS date_key,
                    COUNT(DISTINCT cntr_no) AS cntr_qty,
                    SUM(item_qty) AS plan_qty
                FROM
                    tb_mw_inbound_delivery
                WHERE
                    inbound_date >= (DATE_TRUNC('week', :chooseDate::DATE) - INTERVAL '11 weeks')
                    AND inbound_date <= :chooseDate::DATE
                GROUP BY
                    DATE_TRUNC('week', inbound_date)::DATE
            )
            SELECT
                TO_CHAR(ds.date_key, 'MM/DD') AS inbound_date,
                COALESCE(dd.cntr_qty, 0) AS cntr_qty,
                COALESCE(dd.plan_qty, 0) AS plan_qty
            FROM
                date_series ds
            LEFT JOIN
                delivery_data dd ON ds.date_key = dd.date_key
            ORDER BY
                ds.date_key ASC
        """;
        List<DashboardVolumeOfGoods> deliveryResult = this.queryManager.selectListBySql(deliverySql, param, DashboardVolumeOfGoods.class, 0, 0);

        // 2. 피킹 데이터 (palletizingSql): 최근 12주 (주간 집계)
        String palletizingSql = """
            WITH date_series AS (
                SELECT
                    generate_series(
                        DATE_TRUNC('week', :chooseDate::DATE) - INTERVAL '11 weeks',
                        DATE_TRUNC('week', :chooseDate::DATE),
                        '1 week'::interval
                    )::DATE AS date_key
            ),
            palletizing_data AS (
                SELECT
                    DATE_TRUNC('week', created_at)::DATE AS date_key,
                    COUNT(*) AS pass_qty
                FROM
                    tb_mw_box_conveyor_info
                WHERE
                    is_picked = TRUE
                    AND created_at >= (DATE_TRUNC('week', :chooseDate::DATE) - INTERVAL '11 weeks')
                    AND created_at::DATE <= :chooseDate::DATE
                GROUP BY
                    DATE_TRUNC('week', created_at)::DATE
            )
            SELECT
                TO_CHAR(ds.date_key, 'MM/DD') AS inbound_date,
                COALESCE(pd.pass_qty, 0) AS pass_qty
            FROM
                date_series ds
            LEFT JOIN
                palletizing_data pd ON ds.date_key = pd.date_key
            ORDER BY
                ds.date_key ASC
        """;
        List<DashboardVolumeOfGoods> palletizingResult = this.queryManager.selectListBySql(palletizingSql, param, DashboardVolumeOfGoods.class, 0, 0);

        // 3. 두 결과를 병합
        return mergeVolumeOfGoodsResults(deliveryResult, palletizingResult);
    }

    public List<DashboardVolumeOfGoods> getVolumeOfGoodsGroupByMonth(Date chooseDate) {
        Map<String, Object> param = ValueUtil.newMap("chooseDate", chooseDate);

        // 1. 입고 데이터 (deliverySql): 최근 12개월 (월별 집계)
        String deliverySql = """
        WITH date_series AS (
            SELECT
                generate_series(
                    DATE_TRUNC('month', :chooseDate::DATE) - INTERVAL '11 months',
                    DATE_TRUNC('month', :chooseDate::DATE),
                    '1 month'::interval
                )::DATE AS date_key
        ),
        delivery_data AS (
            SELECT
                DATE_TRUNC('month', inbound_date)::DATE AS date_key,
                COUNT(DISTINCT cntr_no) AS cntr_qty,
                SUM(item_qty) AS plan_qty
            FROM
                tb_mw_inbound_delivery
            WHERE
                inbound_date >= (DATE_TRUNC('month', :chooseDate::DATE) - INTERVAL '11 months')
                AND inbound_date < (DATE_TRUNC('month', :chooseDate::DATE) + INTERVAL '1 month')
            GROUP BY
                DATE_TRUNC('month', inbound_date)::DATE
        )
        SELECT
            -- FMMM: 앞의 0을 제거 (01월 -> 1월)
            TO_CHAR(ds.date_key, 'FMMM"월"') AS inbound_date,
            COALESCE(dd.cntr_qty, 0) AS cntr_qty,
            COALESCE(dd.plan_qty, 0) AS plan_qty
        FROM
            date_series ds
        LEFT JOIN
            delivery_data dd ON ds.date_key = dd.date_key
        ORDER BY
            ds.date_key ASC -- 시간 순서대로 정렬 (매우 중요)
        """;
        List<DashboardVolumeOfGoods> deliveryResult = this.queryManager.selectListBySql(deliverySql, param, DashboardVolumeOfGoods.class, 0, 0);

        // 2. 피킹 데이터 (palletizingSql): 최근 12개월 (월별 집계)
        String palletizingSql = """
        WITH date_series AS (
            SELECT
                generate_series(
                    DATE_TRUNC('month', :chooseDate::DATE) - INTERVAL '11 months',
                    DATE_TRUNC('month', :chooseDate::DATE),
                    '1 month'::interval
                )::DATE AS date_key
        ),
        palletizing_data AS (
            SELECT
                DATE_TRUNC('month', created_at)::DATE AS date_key,
                COUNT(*) AS pass_qty
            FROM
                tb_mw_box_conveyor_info
            WHERE
                is_picked = TRUE
                AND created_at >= (DATE_TRUNC('month', :chooseDate::DATE) - INTERVAL '11 months')
                AND created_at < (DATE_TRUNC('month', :chooseDate::DATE) + INTERVAL '1 month')
            GROUP BY
                DATE_TRUNC('month', created_at)::DATE
        )
        SELECT
            TO_CHAR(ds.date_key, 'FMMM"월"') AS inbound_date,
            COALESCE(pd.pass_qty, 0) AS pass_qty
        FROM
            date_series ds
        LEFT JOIN
            palletizing_data pd ON ds.date_key = pd.date_key
        ORDER BY
            ds.date_key ASC
        """;
        List<DashboardVolumeOfGoods> palletizingResult = this.queryManager.selectListBySql(palletizingSql, param, DashboardVolumeOfGoods.class, 0, 0);

        // 3. 두 결과를 병합
        return mergeVolumeOfGoodsResults(deliveryResult, palletizingResult);
    }

    public List<DashboardVolumeOfGoods> getVolumeOfGoodsGroupByHour(Date chooseDate) {
        Map<String, Object> param = ValueUtil.newMap("chooseDate", chooseDate);

        // 1. 입고 데이터 (deliverySql): 시간 정보가 없으므로 하루 총량을 모든 구간에 반복 표시
        String deliverySql = """
            WITH time_series AS (
                SELECT generate_series(
                    :chooseDate::TIMESTAMP,
                    :chooseDate::TIMESTAMP + INTERVAL '22 hours',
                    '2 hours'::interval
                ) AS time_key
            ),
            delivery_data AS (
                SELECT
                    DATE_TRUNC('day', start_datetime) + (FLOOR(EXTRACT(HOUR FROM start_datetime) / 2) * 2 * INTERVAL '1 hour') AS bucket,
                    COUNT(DISTINCT cntr_no) AS cntr_qty,
                    SUM(item_qty) AS plan_qty
                FROM
                    tb_mw_inbound_delivery
                WHERE
                    start_datetime >= :chooseDate::TIMESTAMP
                    AND start_datetime < :chooseDate::TIMESTAMP + INTERVAL '1 day'
                GROUP BY
                    1
            )
            SELECT
                TO_CHAR(ts.time_key, 'FMHH24"시"') AS inbound_date,
                COALESCE(dd.cntr_qty, 0) AS cntr_qty,
                COALESCE(dd.plan_qty, 0) AS plan_qty
            FROM
                time_series ts
            LEFT JOIN
                delivery_data dd ON ts.time_key = dd.bucket
            ORDER BY
                ts.time_key ASC
        """;
        List<DashboardVolumeOfGoods> deliveryResult = this.queryManager.selectListBySql(deliverySql, param, DashboardVolumeOfGoods.class, 0, 0);

        // 2. 피킹 데이터 (palletizingSql): 2시간 단위로 그룹화
        String palletizingSql = """
            WITH time_series AS (
                SELECT generate_series(
                    :chooseDate::TIMESTAMP,
                    :chooseDate::TIMESTAMP + INTERVAL '22 hours',
                    '2 hours'::interval
                ) AS time_key
            ),
            palletizing_data AS (
                SELECT
                    (
                        DATE_TRUNC('day', created_at) + 
                        (FLOOR(EXTRACT(HOUR FROM created_at) / 2) * 2) * INTERVAL '1 hour'
                    ) AS time_slot,
                    COUNT(*) AS pass_qty
                FROM
                    tb_mw_box_conveyor_info
                WHERE
                    is_picked = TRUE
                    AND created_at >= :chooseDate::TIMESTAMP
                    AND created_at < :chooseDate::TIMESTAMP + INTERVAL '1 day'
                GROUP BY
                    time_slot
            )
            SELECT
                TO_CHAR(ts.time_key, 'FMHH24"시"') AS inbound_date,
                COALESCE(pd.pass_qty, 0) AS pass_qty
            FROM
                time_series ts
            LEFT JOIN
                palletizing_data pd ON ts.time_key = pd.time_slot
            ORDER BY
                ts.time_key ASC
        """;
        List<DashboardVolumeOfGoods> palletizingResult = this.queryManager.selectListBySql(palletizingSql, param, DashboardVolumeOfGoods.class, 0, 0);

        // 3. 두 결과를 병합
        return mergeVolumeOfGoodsResults(deliveryResult, palletizingResult);
    }

    private List<DashboardVolumeOfGoods> mergeVolumeOfGoodsResults(
            List<DashboardVolumeOfGoods> deliveryResult,
            List<DashboardVolumeOfGoods> palletizingResult) {

        // 3-1. 첫 번째 리스트를 LinkedHashMap으로 변환 (입력된 순서 = 시간 순서 유지)
        Map<String, DashboardVolumeOfGoods> resultMap = deliveryResult.stream()
                .collect(Collectors.toMap(
                        DashboardVolumeOfGoods::getInboundDate, // 키: "1월", "2월" 등
                        item -> item,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        // 3-2. 두 번째 리스트 병합
        for (DashboardVolumeOfGoods palletizingItem : palletizingResult) {
            String dateKey = palletizingItem.getInboundDate();

            DashboardVolumeOfGoods mergedItem = resultMap.computeIfAbsent(dateKey, k -> {
                DashboardVolumeOfGoods newItem = new DashboardVolumeOfGoods();
                newItem.setInboundDate(k);
                return newItem;
            });

            mergedItem.setPassQty(palletizingItem.getPassQty());
        }

        // 3-3. 순서가 유지된 값들을 List로 반환
        return new ArrayList<>(resultMap.values());
    }
}