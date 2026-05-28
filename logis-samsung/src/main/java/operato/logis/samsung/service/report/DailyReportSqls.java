package operato.logis.samsung.service.report;

/**
 * 일별 리포트 SQL 모음
 */
public class DailyReportSqls {

    private DailyReportSqls() {
    }

    public static String summarySql() {
        return """
        WITH all_boxes AS (
            SELECT
                b.box_id,
                b.bl_no,
                b.cntr_no,
                b.item_code,
                b.received_at,
                b.final_at,
                b.tracking_at,
                b.manual_result_at,
                b.final_status,
                b.reject_type,
                b.reject_desc,
                b.final_remark
            FROM samsung_mw.tb_mw_box b
            WHERE b.received_at::date = :targetDate
              AND (:blNo IS NULL OR :blNo = '' OR b.bl_no = :blNo)
              AND (:cntrNo IS NULL OR :cntrNo = '' OR b.cntr_no = :cntrNo)
        ),

        /* ------------------------------------------------------------------ */
        /* 수동처리 대상 상품코드 목록                                         */
        /* box.item_code = delivery.inner_item_code 매핑                      */
        /* ------------------------------------------------------------------ */
        manual_delivery_items AS (
            SELECT DISTINCT
                d.bl_no,
                d.cntr_no,
                d.inner_item_code
            FROM samsung_mw.tb_mw_inbound_delivery d
            WHERE d.manual_flag = true
              AND d.inner_item_code IS NOT NULL
              AND d.inner_item_code <> ''
              AND (:blNo IS NULL OR :blNo = '' OR d.bl_no = :blNo)
              AND (:cntrNo IS NULL OR :cntrNo = '' OR d.cntr_no = :cntrNo)
        ),

        /* ------------------------------------------------------------------ */
        /* 수동처리 박스 수                                                    */
        /* ------------------------------------------------------------------ */
        manual_stats AS (
            SELECT
                COUNT(*) AS manual_box_qty
            FROM all_boxes ab
            JOIN manual_delivery_items mdi
              ON mdi.bl_no = ab.bl_no
             AND mdi.cntr_no = ab.cntr_no
             AND mdi.inner_item_code = ab.item_code
        ),

        /* ------------------------------------------------------------------ */
        /* 일반 집계 대상 박스 = 수동처리 대상 제외                            */
        /* ------------------------------------------------------------------ */
        base AS (
            SELECT
                ab.box_id,
                ab.bl_no,
                ab.cntr_no,
                ab.item_code,
                ab.received_at,
                ab.final_at,
                ab.tracking_at,
                ab.manual_result_at,
                ab.final_status,
                ab.reject_type,
                ab.reject_desc,
                ab.final_remark,
                CASE
                    WHEN ab.reject_type IN ('체적불량', '외관불량') THEN 'NG'
                    WHEN ab.final_status = :storedStatus THEN 'OK'
                    ELSE 'PENDING'
                END AS result_type,
                CASE
                    WHEN ab.reject_type IN ('체적불량', '외관불량')
                        THEN COALESCE(ab.final_at, ab.tracking_at, ab.manual_result_at)
                    WHEN ab.final_status = :storedStatus
                        THEN ab.final_at
                    ELSE NULL
                END AS completed_at,
                CASE
                    WHEN ab.final_status = :storedStatus
                         AND ab.received_at IS NOT NULL
                         AND ab.final_at IS NOT NULL
                        THEN EXTRACT(EPOCH FROM (ab.final_at - ab.received_at))::numeric
                    ELSE NULL
                END AS elapsed_seconds
            FROM all_boxes ab
            LEFT JOIN manual_delivery_items mdi
              ON mdi.bl_no = ab.bl_no
             AND mdi.cntr_no = ab.cntr_no
             AND mdi.inner_item_code = ab.item_code
            WHERE mdi.inner_item_code IS NULL
        ),

        /* ------------------------------------------------------------------ */
        /* 작업 정보                                                           */
        /* 반드시 1 row 만 나오도록 집계                                       */
        /* ------------------------------------------------------------------ */
        job_time_info AS (
            SELECT
                MIN(j.job_start_dt) AS job_start_dt,
                MAX(j.job_end_dt) AS job_end_dt
            FROM samsung_mw.tb_mw_inbound_job j
            WHERE j.inbound_date::date = :targetDate
              AND (:blNo IS NULL OR :blNo = '' OR j.bl_no = :blNo)
              AND (:cntrNo IS NULL OR :cntrNo = '' OR j.cntr_no = :cntrNo)
        ),
        job_sku_info AS (
            SELECT
                COUNT(DISTINCT d.inner_item_code) AS job_sku_qty
            FROM samsung_mw.tb_mw_inbound_delivery d
            WHERE d.inner_item_code IS NOT NULL
              AND d.inner_item_code <> ''
              AND COALESCE(d.manual_flag, false) = false
              AND (:blNo IS NULL OR :blNo = '' OR d.bl_no = :blNo)
              AND (:cntrNo IS NULL OR :cntrNo = '' OR d.cntr_no = :cntrNo)
        ),
        job_info AS (
            SELECT
                jt.job_start_dt,
                jt.job_end_dt,
                COALESCE(js.job_sku_qty, 0) AS job_sku_qty
            FROM job_time_info jt
            CROSS JOIN job_sku_info js
        ),

        /* ------------------------------------------------------------------ */
        /* 입고 박스 기준 UPH 계산용                                          */
        /* ------------------------------------------------------------------ */
        inbound_box_base AS (
            SELECT
                b.received_at
            FROM base b
            WHERE b.received_at IS NOT NULL
        ),
        inbound_box_seq AS (
            SELECT
                ibb.received_at,
                LEAD(ibb.received_at) OVER (ORDER BY ibb.received_at) AS next_received_at
            FROM inbound_box_base ibb
        ),
        inbound_gap AS (
            SELECT
                CASE
                    WHEN EXTRACT(EPOCH FROM (ibs.next_received_at - ibs.received_at))::numeric > :gapThresholdSec
                        THEN EXTRACT(EPOCH FROM (ibs.next_received_at - ibs.received_at))::numeric - :gapThresholdSec
                    ELSE 0
                END AS idle_excess_seconds
            FROM inbound_box_seq ibs
            WHERE ibs.next_received_at IS NOT NULL
        ),
        inbound_uph AS (
            SELECT
                COUNT(*) AS inbound_total_box_qty,
                MIN(ibb.received_at) AS inbound_first_at,
                MAX(ibb.received_at) AS inbound_last_at,
                COALESCE((SELECT SUM(ig.idle_excess_seconds) FROM inbound_gap ig), 0) AS inbound_idle_excess_seconds
            FROM inbound_box_base ibb
        ),

        /* ------------------------------------------------------------------ */
        /* 전체 운영 시간 기준                                                 */
        /* ------------------------------------------------------------------ */
        process_window AS (
            SELECT
                MIN(b.received_at) AS process_start_dt,
                MAX(b.final_at) FILTER (
                    WHERE b.final_status = :storedStatus
                      AND b.final_at IS NOT NULL
                ) AS process_end_dt
            FROM base b
        ),

        /* ------------------------------------------------------------------ */
        /* 파렛타이저 실 운영시간 계산                                        */
        /* exchange seq N -> emission seq N                                   */
        /* ------------------------------------------------------------------ */
        pallet_event_base AS (
            SELECT
                e.pallet_id,
                e.method,
                e.pallet_sequence,
                e.created_at,
                CAST(NULLIF(regexp_replace(e.pallet_sequence, '^.*-', ''), '') AS integer) AS seq_no
            FROM samsung_mw.tb_mw_if_xyz_pallet_exchange e
            CROSS JOIN process_window pw
            WHERE e.method IN ('exchange', 'emission')
              AND e.pallet_sequence IS NOT NULL
              AND pw.process_start_dt IS NOT NULL
              AND pw.process_end_dt IS NOT NULL
              AND e.created_at >= pw.process_start_dt
              AND e.created_at <= pw.process_end_dt
        ),
        pallet_exchange_event AS (
            SELECT
                eb.pallet_id,
                eb.pallet_sequence AS exchange_pallet_sequence,
                eb.seq_no AS exchange_seq_no,
                eb.created_at AS exchange_at
            FROM pallet_event_base eb
            WHERE eb.method = 'exchange'
        ),
        pallet_emission_event AS (
            SELECT
                eb.pallet_id,
                eb.pallet_sequence AS emission_pallet_sequence,
                eb.seq_no AS emission_seq_no,
                eb.created_at AS emission_at
            FROM pallet_event_base eb
            WHERE eb.method = 'emission'
        ),
        pallet_operating_interval_raw AS (
            SELECT
                x.pallet_id,
                x.exchange_pallet_sequence,
                x.exchange_seq_no,
                x.exchange_at AS operating_start_at,
                e.emission_pallet_sequence,
                e.emission_seq_no,
                e.emission_at AS operating_end_at
            FROM pallet_exchange_event x
            JOIN pallet_emission_event e
              ON e.pallet_id = x.pallet_id
             AND e.emission_seq_no = x.exchange_seq_no
             AND e.emission_at > x.exchange_at
        ),
        pallet_operating_interval_valid AS (
            SELECT
                por.operating_start_at,
                por.operating_end_at
            FROM pallet_operating_interval_raw por
            CROSS JOIN process_window pw
            WHERE pw.process_start_dt IS NOT NULL
              AND pw.process_end_dt IS NOT NULL
              AND por.operating_end_at > por.operating_start_at
              AND por.operating_start_at < pw.process_end_dt
              AND por.operating_end_at > pw.process_start_dt
        ),
        pallet_operating_interval_clamped AS (
            SELECT
                GREATEST(poiv.operating_start_at, pw.process_start_dt) AS operating_start_at,
                LEAST(poiv.operating_end_at, pw.process_end_dt) AS operating_end_at
            FROM pallet_operating_interval_valid poiv
            CROSS JOIN process_window pw
            WHERE LEAST(poiv.operating_end_at, pw.process_end_dt) >
                  GREATEST(poiv.operating_start_at, pw.process_start_dt)
        ),
        pallet_operating_ordered AS (
            SELECT
                poic.operating_start_at,
                poic.operating_end_at,
                MAX(poic.operating_end_at) OVER (
                    ORDER BY poic.operating_start_at, poic.operating_end_at
                    ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
                ) AS prev_max_end
            FROM pallet_operating_interval_clamped poic
        ),
        pallet_operating_grouped AS (
            SELECT
                poo.operating_start_at,
                poo.operating_end_at,
                SUM(
                    CASE
                        WHEN poo.prev_max_end IS NULL OR poo.operating_start_at > poo.prev_max_end THEN 1
                        ELSE 0
                    END
                ) OVER (ORDER BY poo.operating_start_at, poo.operating_end_at) AS grp
            FROM pallet_operating_ordered poo
        ),
        pallet_operating_merged AS (
            SELECT
                pog.grp,
                MIN(pog.operating_start_at) AS operating_start_at,
                MAX(pog.operating_end_at) AS operating_end_at
            FROM pallet_operating_grouped pog
            GROUP BY pog.grp
        ),
        pallet_operating_time AS (
            SELECT
                COALESCE(SUM(EXTRACT(EPOCH FROM (pom.operating_end_at - pom.operating_start_at))), 0)::numeric AS operating_seconds
            FROM pallet_operating_merged pom
        ),

        /* ------------------------------------------------------------------ */
        /* 요약 집계                                                           */
        /* ------------------------------------------------------------------ */
        agg AS (
            SELECT
                COUNT(*) AS total_box_qty,
                COUNT(*) FILTER (WHERE b.final_status = :storedStatus) AS done_box_qty,
                COUNT(*) FILTER (WHERE b.result_type = 'OK') AS ok_box_qty,
                COUNT(*) FILTER (WHERE b.result_type = 'NG') AS ng_box_qty,
                COUNT(*) FILTER (WHERE b.result_type = 'PENDING') AS pending_box_qty,
                COUNT(DISTINCT b.item_code) FILTER (
                    WHERE b.item_code IS NOT NULL
                      AND b.item_code <> ''
                ) AS actual_sku_qty,
                MIN(b.received_at) FILTER (WHERE b.received_at IS NOT NULL) AS first_received_at,
                MAX(b.final_at) FILTER (
                    WHERE b.final_status = :storedStatus
                      AND b.final_at IS NOT NULL
                ) AS last_palletized_at,
                AVG(b.elapsed_seconds) FILTER (WHERE b.elapsed_seconds IS NOT NULL) AS avg_all_sec,
                MIN(b.elapsed_seconds) FILTER (WHERE b.elapsed_seconds IS NOT NULL) AS min_sec,
                MAX(b.elapsed_seconds) FILTER (WHERE b.elapsed_seconds IS NOT NULL) AS max_sec
            FROM base b
        ),
        pct AS (
            SELECT
                percentile_cont(0.50) WITHIN GROUP (ORDER BY b.elapsed_seconds) AS p50_sec,
                percentile_cont(0.95) WITHIN GROUP (ORDER BY b.elapsed_seconds) AS p95_sec
            FROM base b
            WHERE b.elapsed_seconds IS NOT NULL
        ),
        filtered AS (
            SELECT
                AVG(b.elapsed_seconds) AS avg_excl_p95_sec
            FROM base b
            CROSS JOIN pct p
            WHERE b.elapsed_seconds IS NOT NULL
              AND b.elapsed_seconds <= p.p95_sec
        ),
        sec_fmt AS (
            SELECT
                a.*,
                j.job_start_dt,
                j.job_end_dt,
                j.job_sku_qty,
                ms.manual_box_qty,
                pot.operating_seconds,
                ROUND(a.avg_all_sec)::bigint AS avg_all_sec_i,
                ROUND(a.min_sec)::bigint AS min_sec_i,
                ROUND(a.max_sec)::bigint AS max_sec_i,
                ROUND(p.p50_sec)::bigint AS p50_sec_i,
                ROUND(p.p95_sec)::bigint AS p95_sec_i,
                ROUND(f.avg_excl_p95_sec)::bigint AS avg_excl_p95_sec_i
            FROM agg a
            CROSS JOIN pct p
            CROSS JOIN filtered f
            CROSS JOIN manual_stats ms
            CROSS JOIN job_info j
            LEFT JOIN pallet_operating_time pot ON 1 = 1
        ),
        final_data AS (
            SELECT
                sf.total_box_qty,
                sf.done_box_qty,
                sf.ok_box_qty,
                sf.ng_box_qty,
                sf.pending_box_qty,
                sf.job_sku_qty,
                sf.actual_sku_qty,
                sf.manual_box_qty,
                sf.job_start_dt,
                sf.job_end_dt,
                sf.first_received_at,
                sf.last_palletized_at,
                CASE
                    WHEN sf.first_received_at IS NOT NULL
                     AND sf.last_palletized_at IS NOT NULL
                        THEN ROUND(EXTRACT(EPOCH FROM (sf.last_palletized_at - sf.first_received_at)))::bigint
                    ELSE NULL
                END AS total_operating_seconds_i,
                COALESCE(ROUND(sf.operating_seconds)::bigint, 0) AS pallet_operating_seconds_i,
                iu.inbound_total_box_qty,
                iu.inbound_first_at,
                iu.inbound_last_at,
                iu.inbound_idle_excess_seconds,
                CASE
                    WHEN iu.inbound_first_at IS NOT NULL
                     AND iu.inbound_last_at IS NOT NULL
                        THEN GREATEST(
                            ROUND(EXTRACT(EPOCH FROM (iu.inbound_last_at - iu.inbound_first_at)))::bigint
                            - COALESCE(ROUND(iu.inbound_idle_excess_seconds)::bigint, 0),
                            0
                        )
                    ELSE NULL
                END AS inbound_net_operating_seconds_i,
                sf.avg_all_sec_i,
                sf.p50_sec_i,
                sf.p95_sec_i,
                sf.avg_excl_p95_sec_i,
                sf.min_sec_i,
                sf.max_sec_i
            FROM sec_fmt sf
            CROSS JOIN inbound_uph iu
        )
        SELECT
            CAST(:targetDate AS DATE) AS "todayDate",
            :blNo AS "blNo",
            :cntrNo AS "cntrNo",

            fd.total_box_qty AS "totalBoxQty",
            fd.job_sku_qty AS "jobSkuQty",
            fd.actual_sku_qty AS "actualSkuQty",
            fd.ok_box_qty AS "okBoxQty",
            fd.ng_box_qty AS "ngBoxQty",
            fd.pending_box_qty AS "pendingBoxQty",
            fd.manual_box_qty AS "manualBoxQty",

            TO_CHAR(fd.job_start_dt, 'YYYY-MM-DD HH24:MI:SS') AS "jobStartDt",
            TO_CHAR(fd.job_end_dt, 'YYYY-MM-DD HH24:MI:SS') AS "jobEndDt",

            COALESCE(TO_CHAR(fd.first_received_at, 'YYYY-MM-DD HH24:MI:SS'), '-') AS "firstReceivedAt",
            COALESCE(TO_CHAR(fd.last_palletized_at, 'YYYY-MM-DD HH24:MI:SS'), '-') AS "lastPalletizedAt",

            CASE
                WHEN fd.total_operating_seconds_i IS NOT NULL
                    THEN LPAD((fd.total_operating_seconds_i / 3600)::text, 2, '0') || ':' ||
                         LPAD(((fd.total_operating_seconds_i % 3600) / 60)::text, 2, '0') || ':' ||
                         LPAD((fd.total_operating_seconds_i % 60)::text, 2, '0')
                ELSE NULL
            END AS "totalOperatingTime",

            CASE
                WHEN fd.total_operating_seconds_i IS NOT NULL
                    THEN LPAD((GREATEST(fd.total_operating_seconds_i - fd.pallet_operating_seconds_i, 0) / 3600)::text, 2, '0') || ':' ||
                         LPAD(((GREATEST(fd.total_operating_seconds_i - fd.pallet_operating_seconds_i, 0) % 3600) / 60)::text, 2, '0') || ':' ||
                         LPAD((GREATEST(fd.total_operating_seconds_i - fd.pallet_operating_seconds_i, 0) % 60)::text, 2, '0')
                ELSE NULL
            END AS "idleTime",

            CASE
                WHEN fd.pallet_operating_seconds_i IS NOT NULL
                    THEN LPAD((fd.pallet_operating_seconds_i / 3600)::text, 2, '0') || ':' ||
                         LPAD(((fd.pallet_operating_seconds_i % 3600) / 60)::text, 2, '0') || ':' ||
                         LPAD((fd.pallet_operating_seconds_i % 60)::text, 2, '0')
                ELSE NULL
            END AS "palletOperatingTime",

            LPAD((fd.avg_all_sec_i / 3600)::text, 2, '0') || ':' ||
            LPAD(((fd.avg_all_sec_i % 3600) / 60)::text, 2, '0') || ':' ||
            LPAD((fd.avg_all_sec_i % 60)::text, 2, '0') AS "avgAllTime",

            LPAD((fd.p50_sec_i / 3600)::text, 2, '0') || ':' ||
            LPAD(((fd.p50_sec_i % 3600) / 60)::text, 2, '0') || ':' ||
            LPAD((fd.p50_sec_i % 60)::text, 2, '0') AS "medianTime",

            LPAD((fd.p95_sec_i / 3600)::text, 2, '0') || ':' ||
            LPAD(((fd.p95_sec_i % 3600) / 60)::text, 2, '0') || ':' ||
            LPAD((fd.p95_sec_i % 60)::text, 2, '0') AS "p95Time",

            LPAD((fd.avg_excl_p95_sec_i / 3600)::text, 2, '0') || ':' ||
            LPAD(((fd.avg_excl_p95_sec_i % 3600) / 60)::text, 2, '0') || ':' ||
            LPAD((fd.avg_excl_p95_sec_i % 60)::text, 2, '0') AS "avgExclP95Time",

            LPAD((fd.min_sec_i / 3600)::text, 2, '0') || ':' ||
            LPAD(((fd.min_sec_i % 3600) / 60)::text, 2, '0') || ':' ||
            LPAD((fd.min_sec_i % 60)::text, 2, '0') AS "minTime",

            LPAD((fd.max_sec_i / 3600)::text, 2, '0') || ':' ||
            LPAD(((fd.max_sec_i % 3600) / 60)::text, 2, '0') || ':' ||
            LPAD((fd.max_sec_i % 60)::text, 2, '0') AS "maxTime",

            CASE
                WHEN fd.total_operating_seconds_i IS NOT NULL
                     AND fd.total_operating_seconds_i > 0
                    THEN ROUND(fd.done_box_qty::numeric / (fd.total_operating_seconds_i / 3600.0), 2)
                ELSE NULL
            END AS "totalTimeUph",

            CASE
                WHEN fd.pallet_operating_seconds_i IS NOT NULL
                     AND fd.pallet_operating_seconds_i > 0
                    THEN ROUND(fd.done_box_qty::numeric / (fd.pallet_operating_seconds_i / 3600.0), 2)
                ELSE NULL
            END AS "palletTimeUph",

            CASE
                WHEN fd.inbound_net_operating_seconds_i IS NOT NULL
                     AND fd.inbound_net_operating_seconds_i > 0
                    THEN ROUND(fd.inbound_total_box_qty::numeric / (fd.inbound_net_operating_seconds_i / 3600.0), 2)
                ELSE NULL
            END AS "inboundNetTimeUph",

            ROUND(
                CASE
                    WHEN fd.total_box_qty > 0
                        THEN (fd.ng_box_qty::numeric / fd.total_box_qty) * 100
                    ELSE 0
                END,
                2
            ) AS "ngRatePct",

            '총 박스수 : ' || COALESCE(fd.total_box_qty::text, '0') || E'\\n' ||
            '작업지시 SKU 수량 : ' || COALESCE(fd.job_sku_qty::text, '0') || E'\\n' ||
            '실처리 SKU 수량 : ' || COALESCE(fd.actual_sku_qty::text, '0') || E'\\n' ||
            '수동처리 : ' || COALESCE(fd.manual_box_qty::text, '0') || E'\\n' ||
            '정상완료 : ' || COALESCE(fd.ok_box_qty::text, '0') || E'\\n' ||
            '최종 NG : ' || COALESCE(fd.ng_box_qty::text, '0') || E'\\n' ||
            '미완료 : ' || COALESCE(fd.pending_box_qty::text, '0') || E'\\n' ||
            '입고 첫 박스 시작시간 : ' || COALESCE(TO_CHAR(fd.first_received_at, 'YYYY-MM-DD HH24:MI:SS'), '-') || E'\\n' ||
            '마지막 박스 적재시간 : ' || COALESCE(TO_CHAR(fd.last_palletized_at, 'YYYY-MM-DD HH24:MI:SS'), '-') || E'\\n' ||
            '전체 운영 시간 : ' || COALESCE(
                LPAD((fd.total_operating_seconds_i / 3600)::text, 2, '0') || ':' ||
                LPAD(((fd.total_operating_seconds_i % 3600) / 60)::text, 2, '0') || ':' ||
                LPAD((fd.total_operating_seconds_i % 60)::text, 2, '0'),
                '-'
            ) || E'\\n' ||
            '전체운영 UPH : ' || COALESCE(
                CASE
                    WHEN fd.total_operating_seconds_i > 0
                        THEN ROUND(fd.done_box_qty::numeric / (fd.total_operating_seconds_i / 3600.0), 2)::text
                    ELSE NULL
                END,
                '0'
            ) || E'\\n' ||
            '파렛타이저 유휴시간 : ' || COALESCE(
                LPAD((GREATEST(fd.total_operating_seconds_i - fd.pallet_operating_seconds_i, 0) / 3600)::text, 2, '0') || ':' ||
                LPAD(((GREATEST(fd.total_operating_seconds_i - fd.pallet_operating_seconds_i, 0) % 3600) / 60)::text, 2, '0') || ':' ||
                LPAD((GREATEST(fd.total_operating_seconds_i - fd.pallet_operating_seconds_i, 0) % 60)::text, 2, '0'),
                '-'
            ) || E'\\n' ||
            '파렛타이저 운영시간 : ' || COALESCE(
                LPAD((fd.pallet_operating_seconds_i / 3600)::text, 2, '0') || ':' ||
                LPAD(((fd.pallet_operating_seconds_i % 3600) / 60)::text, 2, '0') || ':' ||
                LPAD((fd.pallet_operating_seconds_i % 60)::text, 2, '0'),
                '-'
            ) || E'\\n' ||
            '파렛타이저 UPH : ' || COALESCE(
                CASE
                    WHEN fd.pallet_operating_seconds_i > 0
                        THEN ROUND(fd.done_box_qty::numeric / (fd.pallet_operating_seconds_i / 3600.0), 2)::text
                    ELSE NULL
                END,
                '0'
            ) || E'\\n' ||
            'NG율 : ' || COALESCE(
                ROUND(
                    CASE
                        WHEN fd.total_box_qty > 0
                            THEN (fd.ng_box_qty::numeric / fd.total_box_qty) * 100
                        ELSE 0
                    END,
                    2
                )::text,
                '0'
            ) || '%' || E'\\n' ||
            '박스입고 평균 소요시간(이상치제외) : ' ||
            (LPAD((fd.avg_excl_p95_sec_i / 3600)::text, 2, '0') || ':' ||
             LPAD(((fd.avg_excl_p95_sec_i % 3600) / 60)::text, 2, '0') || ':' ||
             LPAD((fd.avg_excl_p95_sec_i % 60)::text, 2, '0')) || E'\\n' ||
            '평균 : ' ||
            (LPAD((fd.avg_all_sec_i / 3600)::text, 2, '0') || ':' ||
             LPAD(((fd.avg_all_sec_i % 3600) / 60)::text, 2, '0') || ':' ||
             LPAD((fd.avg_all_sec_i % 60)::text, 2, '0')) || E'\\n' ||
            '중앙값 : ' ||
            (LPAD((fd.p50_sec_i / 3600)::text, 2, '0') || ':' ||
             LPAD(((fd.p50_sec_i % 3600) / 60)::text, 2, '0') || ':' ||
             LPAD((fd.p50_sec_i % 60)::text, 2, '0')) || E'\\n' ||
            'P95 : ' ||
            (LPAD((fd.p95_sec_i / 3600)::text, 2, '0') || ':' ||
             LPAD(((fd.p95_sec_i % 3600) / 60)::text, 2, '0') || ':' ||
             LPAD((fd.p95_sec_i % 60)::text, 2, '0')) AS "summaryText"
        FROM final_data fd
        """;
    }

    public static String timelineSql() {
        return """
                WITH all_boxes AS (
                    SELECT
                        b.box_id,
                        b.parcel_id,
                        b.plc_seq_no,
                        b.item_code,
                        b.bl_no,
                        b.cntr_no,
                        b.received_at,
                        b.final_at,
                        b.final_status,
                        b.manual_result_at
                    FROM samsung_mw.tb_mw_box b
                    WHERE b.received_at::date = :targetDate
                      AND (:blNo IS NULL OR :blNo = '' OR b.bl_no = :blNo)
                      AND (:cntrNo IS NULL OR :cntrNo = '' OR b.cntr_no = :cntrNo)
                ),
                box_base AS (
                    SELECT *
                    FROM all_boxes
                ),
                track_agg AS (
                    SELECT
                        t.box_id,
                        MIN(CASE WHEN t.tracking_status = :bcrMeasuredStatus THEN t.tracking_at END) AS bcr_measured_at,
                        MIN(CASE WHEN t.tracking_status = :visionMeasuredStatus THEN t.tracking_at END) AS vision_measured_at,
                        MIN(CASE WHEN t.tracking_status = :reportDvrtStatus THEN t.tracking_at END) AS report_dvrt_at,
                        MIN(CASE WHEN t.tracking_status = :reportPltzStatus THEN t.tracking_at END) AS report_pltz_at
                    FROM samsung_mw.tb_mw_box_track t
                    INNER JOIN box_base bb
                        ON bb.box_id = t.box_id
                    WHERE t.tracking_status IN (
                        :bcrMeasuredStatus,
                        :visionMeasuredStatus,
                        :reportDvrtStatus,
                        :reportPltzStatus
                    )
                    GROUP BY t.box_id
                ),
                proc AS (
                    SELECT
                        bb.box_id,
                        bb.parcel_id,
                        bb.plc_seq_no,
                        bb.item_code,
                        bb.bl_no,
                        bb.cntr_no,
                        bb.received_at,
                        bb.final_at,
                        bb.final_status,
                        ta.bcr_measured_at,
                        ta.vision_measured_at,
                        ta.report_dvrt_at,
                        ta.report_pltz_at,
                        bb.received_at AS bcr_start_at,
                        COALESCE(ta.vision_measured_at, ta.bcr_measured_at, bb.received_at) AS bcr_end_at,
                        COALESCE(ta.vision_measured_at, ta.bcr_measured_at, bb.received_at) AS sorter_start_at,
                        COALESCE(
                            ta.report_dvrt_at,
                            ta.report_pltz_at,
                            bb.final_at,
                            COALESCE(ta.vision_measured_at, ta.bcr_measured_at, bb.received_at)
                        ) AS sorter_end_at,
                        COALESCE(ta.report_dvrt_at, ta.report_pltz_at, bb.final_at) AS palletized_start_at,
                        COALESCE(bb.final_at, ta.report_pltz_at, ta.report_dvrt_at) AS palletized_end_at
                    FROM box_base bb
                    LEFT JOIN track_agg ta
                        ON ta.box_id = bb.box_id
                ),
                container_summary AS (
                    SELECT
                        'CONTAINER' AS row_type,
                        p.cntr_no AS row_group,
                        'CONTAINER' AS process_type,
                        p.bl_no,
                        p.cntr_no,
                        NULL::varchar AS box_id,
                        NULL::varchar AS parcel_id,
                        NULL::varchar AS plc_seq_no,
                        NULL::varchar AS item_code,
                        MIN(p.received_at) AS start_at_ts,
                        MAX(p.final_at) AS end_at_ts,
                        EXTRACT(EPOCH FROM (MAX(p.final_at) - MIN(p.received_at)))::bigint AS duration_sec,
                        'CONTAINER SUMMARY' AS tooltip_title,
                        p.cntr_no AS tooltip_sub1,
                        p.bl_no AS tooltip_sub2
                    FROM proc p
                    WHERE p.received_at IS NOT NULL
                      AND p.final_at IS NOT NULL
                    GROUP BY p.bl_no, p.cntr_no
                ),
                bcr_rows AS (
                    SELECT
                        'BOX' AS row_type,
                        p.cntr_no AS row_group,
                        'BCR' AS process_type,
                        p.bl_no,
                        p.cntr_no,
                        p.box_id,
                        p.parcel_id,
                        p.plc_seq_no,
                        p.item_code,
                        p.bcr_start_at AS start_at_ts,
                        p.bcr_end_at AS end_at_ts,
                        CASE
                            WHEN p.bcr_start_at IS NOT NULL
                             AND p.bcr_end_at IS NOT NULL
                             AND p.bcr_end_at >= p.bcr_start_at
                                THEN EXTRACT(EPOCH FROM (p.bcr_end_at - p.bcr_start_at))::bigint
                            ELSE 0
                        END AS duration_sec,
                        'BCR READING' AS tooltip_title,
                        p.item_code AS tooltip_sub1,
                        p.box_id AS tooltip_sub2
                    FROM proc p
                ),
                sorter_rows AS (
                    SELECT
                        'BOX' AS row_type,
                        p.cntr_no AS row_group,
                        'SORTER' AS process_type,
                        p.bl_no,
                        p.cntr_no,
                        p.box_id,
                        p.parcel_id,
                        p.plc_seq_no,
                        p.item_code,
                        p.sorter_start_at AS start_at_ts,
                        p.sorter_end_at AS end_at_ts,
                        CASE
                            WHEN p.sorter_start_at IS NOT NULL
                             AND p.sorter_end_at IS NOT NULL
                             AND p.sorter_end_at >= p.sorter_start_at
                                THEN EXTRACT(EPOCH FROM (p.sorter_end_at - p.sorter_start_at))::bigint
                            ELSE 0
                        END AS duration_sec,
                        'SORTER' AS tooltip_title,
                        p.item_code AS tooltip_sub1,
                        p.box_id AS tooltip_sub2
                    FROM proc p
                ),
                palletized_rows AS (
                    SELECT
                        'BOX' AS row_type,
                        p.cntr_no AS row_group,
                        'PALLETIZED' AS process_type,
                        p.bl_no,
                        p.cntr_no,
                        p.box_id,
                        p.parcel_id,
                        p.plc_seq_no,
                        p.item_code,
                        p.palletized_start_at AS start_at_ts,
                        p.palletized_end_at AS end_at_ts,
                        CASE
                            WHEN p.palletized_start_at IS NOT NULL
                             AND p.palletized_end_at IS NOT NULL
                             AND p.palletized_end_at >= p.palletized_start_at
                                THEN EXTRACT(EPOCH FROM (p.palletized_end_at - p.palletized_start_at))::bigint
                            ELSE 0
                        END AS duration_sec,
                        'PALLETIZED' AS tooltip_title,
                        p.item_code AS tooltip_sub1,
                        p.box_id AS tooltip_sub2
                    FROM proc p
                )
                SELECT
                    t.row_type,
                    t.row_group,
                    t.process_type,
                    t.bl_no,
                    t.cntr_no,
                    t.box_id,
                    t.parcel_id,
                    t.plc_seq_no,
                    t.item_code,
                    TO_CHAR(t.start_at_ts, 'YYYY-MM-DD HH24:MI:SS') AS start_at,
                    TO_CHAR(t.end_at_ts, 'YYYY-MM-DD HH24:MI:SS') AS end_at,
                    t.duration_sec,
                    t.tooltip_title,
                    t.tooltip_sub1,
                    t.tooltip_sub2
                FROM (
                    SELECT * FROM container_summary
                    UNION ALL
                    SELECT * FROM bcr_rows
                    UNION ALL
                    SELECT * FROM sorter_rows
                    UNION ALL
                    SELECT * FROM palletized_rows
                ) t
                ORDER BY
                    CASE t.row_type
                        WHEN 'CONTAINER' THEN 0
                        WHEN 'BOX' THEN 1
                        ELSE 9
                    END,
                    t.row_group,
                    CASE t.process_type
                        WHEN 'CONTAINER' THEN 0
                        WHEN 'BCR' THEN 1
                        WHEN 'SORTER' THEN 2
                        WHEN 'PALLETIZED' THEN 3
                        ELSE 9
                    END,
                    t.start_at_ts,
                    t.box_id
                """;
    }

    public static String bcrRawSql() {
        return """
        WITH box AS (
            SELECT b.*
            FROM tb_mw_box b
            WHERE b.received_at::date = :targetDate
              AND (:blNo IS NULL OR :blNo = '' OR b.bl_no = :blNo)
              AND (:cntrNo IS NULL OR :cntrNo = '' OR b.cntr_no = :cntrNo)
        )
        SELECT
            TO_CHAR(COALESCE(t.tracking_at, b.final_at), 'YYYY-MM-DD') AS date,
            b.bl_no,
            b.cntr_no,
            b.box_id,
            b.parcel_id,
            b.box_width,
            b.box_length,
            b.box_height,
            COALESCE(t.plc_seq_no, b.plc_seq_no) AS plc_seq_no,
            b.item_code,
            CAST(t.tracking_type AS varchar) AS process_type,
            CAST(t.tracking_status AS varchar) AS tracking_status,
            t.tracking_desc AS tracking_desc,
            TO_CHAR(t.tracking_at, 'YYYY-MM-DD HH24:MI:SS') AS event_time,
            t.line_id,
            t.equip_id,
            CAST(b.final_status AS varchar) AS final_status,
            b.reject_type
        FROM box b
        JOIN tb_mw_box_track t
          ON t.box_id = b.box_id
        WHERE t.tracking_status = 110
        ORDER BY b.box_id, t.tracking_at
        """;
    }

    public static String sorterRawSql() {
        return """
        WITH box AS (
            SELECT b.*
            FROM tb_mw_box b
            WHERE b.received_at::date = :targetDate
              AND (:blNo IS NULL OR :blNo = '' OR b.bl_no = :blNo)
              AND (:cntrNo IS NULL OR :cntrNo = '' OR b.cntr_no = :cntrNo)
        )
        SELECT
            TO_CHAR(COALESCE(t.tracking_at, b.final_at), 'YYYY-MM-DD') AS date,
            b.bl_no,
            b.cntr_no,
            b.box_id,
            b.parcel_id,
            b.box_width,
            b.box_length,
            b.box_height,
            COALESCE(t.plc_seq_no, b.plc_seq_no) AS plc_seq_no,
            b.item_code,
            CAST(t.tracking_type AS varchar) AS process_type,
            CAST(t.tracking_status AS varchar) AS tracking_status,
            t.tracking_desc AS tracking_desc,
            TO_CHAR(t.tracking_at, 'YYYY-MM-DD HH24:MI:SS') AS event_time,
            t.line_id,
            t.equip_id,
            CAST(b.final_status AS varchar) AS final_status,
            b.reject_type
        FROM box b
        JOIN tb_mw_box_track t
          ON t.box_id = b.box_id
        WHERE t.tracking_status = 501
        ORDER BY b.box_id, t.tracking_at
        """;
    }

    public static String palletizedRawSql() {
        return """
        WITH box AS (
            SELECT b.*
            FROM tb_mw_box b
            WHERE b.received_at::date = :targetDate
              AND (:blNo IS NULL OR :blNo = '' OR b.bl_no = :blNo)
              AND (:cntrNo IS NULL OR :cntrNo = '' OR b.cntr_no = :cntrNo)
        )
        SELECT
            TO_CHAR(COALESCE(t.tracking_at, b.final_at), 'YYYY-MM-DD') AS date,
            b.bl_no,
            b.cntr_no,
            b.box_id,
            b.parcel_id,
            b.box_width,
            b.box_length,
            b.box_height,
            COALESCE(t.plc_seq_no, b.plc_seq_no) AS plc_seq_no,
            b.item_code,
            CAST(t.tracking_type AS varchar) AS process_type,
            CAST(t.tracking_status AS varchar) AS tracking_status,
            t.tracking_desc AS tracking_desc,
            TO_CHAR(t.tracking_at, 'YYYY-MM-DD HH24:MI:SS') AS event_time,
            t.line_id,
            t.equip_id,
            CAST(b.final_status AS varchar) AS final_status,
            b.reject_type
        FROM box b
        JOIN tb_mw_box_track t
          ON t.box_id = b.box_id
        WHERE t.tracking_status = 700
        ORDER BY b.box_id, t.tracking_at
        """;
    }

    public static String hourlyUphSql() {
        return """
            WITH base AS (
                SELECT
                    box_id,
                    cntr_no,
                    received_at,
                    tracking_at,
                    manual_result_at,
                    final_at,
                    final_status,
                    tracking_status,
                    reject_type,
                    reject_desc,
                    final_remark
                FROM samsung_mw.tb_mw_box
                WHERE received_at::date = :targetDate
                  AND (:blNo IS NULL OR :blNo = '' OR bl_no = :blNo)
                  AND (:cntrNo IS NULL OR :cntrNo = '' OR cntr_no = :cntrNo)
            ),
            classified AS (
                SELECT
                    *,
                    CASE
                        WHEN reject_type IN ('체적불량', '외관불량') THEN 'NG'
                        WHEN final_status = :storedStatus THEN 'OK'
                        ELSE 'PENDING'
                    END AS result_type,
                    CASE
                        WHEN reject_type IN ('체적불량', '외관불량')
                            THEN COALESCE(final_at, tracking_at, manual_result_at)
                        WHEN final_status = :storedStatus
                            THEN final_at
                        ELSE NULL
                    END AS completed_at
                FROM base
            ),
            hourly AS (
                SELECT
                    date_trunc('hour', completed_at) AS hour_slot,
                    MIN(completed_at) AS first_completed_at,
                    MAX(completed_at) AS last_completed_at,
                    COUNT(*) AS processed_box_qty,
                    COUNT(*) FILTER (WHERE result_type = 'OK') AS ok_qty,
                    COUNT(*) FILTER (WHERE result_type = 'NG') AS ng_qty
                FROM classified
                WHERE completed_at IS NOT NULL
                GROUP BY 1
            )
            SELECT
                to_char(hour_slot, 'YYYY-MM-DD HH24:00') AS "hourSlot",
                TO_CHAR(first_completed_at, 'YYYY-MM-DD HH24:MI:SS') AS "firstBoxStartAtInHour",
                TO_CHAR(last_completed_at, 'YYYY-MM-DD HH24:MI:SS') AS "lastBoxEndAtInHour",
                processed_box_qty AS "boxQtyInHour",
                ok_qty AS "okQty",
                ng_qty AS "ngQty",
                ROUND(
                    processed_box_qty::numeric
                        / NULLIF(EXTRACT(EPOCH FROM (last_completed_at - first_completed_at)) / 3600, 0),
                    1
                ) AS "effectiveUph"
            FROM hourly
            ORDER BY hour_slot
            """;
    }

    public static String palletExchangeSheetSql() {
        return """
        WITH all_boxes AS (
            SELECT
                b.box_id,
                b.bl_no,
                b.cntr_no,
                b.item_code,
                b.received_at,
                b.final_at,
                b.final_status
            FROM samsung_mw.tb_mw_box b
            WHERE b.received_at::date = :targetDate
              AND (:blNo IS NULL OR :blNo = '' OR b.bl_no = :blNo)
              AND (:cntrNo IS NULL OR :cntrNo = '' OR b.cntr_no = :cntrNo)
        ),
        manual_delivery_items AS (
            SELECT DISTINCT
                d.bl_no,
                d.cntr_no,
                d.inner_item_code
            FROM samsung_mw.tb_mw_inbound_delivery d
            WHERE d.manual_flag = true
              AND d.inner_item_code IS NOT NULL
              AND d.inner_item_code <> ''
              AND (:blNo IS NULL OR :blNo = '' OR d.bl_no = :blNo)
              AND (:cntrNo IS NULL OR :cntrNo = '' OR d.cntr_no = :cntrNo)
        ),
        base AS (
            SELECT
                ab.box_id,
                ab.bl_no,
                ab.cntr_no,
                ab.item_code,
                ab.received_at,
                ab.final_at,
                ab.final_status
            FROM all_boxes ab
            LEFT JOIN manual_delivery_items mdi
              ON mdi.bl_no = ab.bl_no
             AND mdi.cntr_no = ab.cntr_no
             AND mdi.inner_item_code = ab.item_code
            WHERE mdi.inner_item_code IS NULL
        ),
        process_window AS (
            SELECT
                MIN(b.received_at) AS process_start_dt,
                MAX(b.final_at) FILTER (
                    WHERE b.final_status = :storedStatus
                      AND b.final_at IS NOT NULL
                ) AS process_end_dt
            FROM base b
        ),
        event_base AS (
            SELECT
                e.pallet_id,
                e.method,
                e.pallet_sequence,
                e.created_at,
                CAST(NULLIF(regexp_replace(e.pallet_sequence, '^.*-', ''), '') AS integer) AS seq_no
            FROM samsung_mw.tb_mw_if_xyz_pallet_exchange e
            CROSS JOIN process_window pw
            WHERE e.method IN ('exchange', 'emission')
              AND e.pallet_sequence IS NOT NULL
              AND pw.process_start_dt IS NOT NULL
              AND pw.process_end_dt IS NOT NULL
              AND e.created_at >= pw.process_start_dt
              AND e.created_at <= pw.process_end_dt
        ),
        exchange_event AS (
            SELECT
                eb.pallet_id,
                eb.pallet_sequence AS exchange_pallet_sequence,
                eb.seq_no,
                eb.created_at AS exchange_at
            FROM event_base eb
            WHERE eb.method = 'exchange'
        ),
        emission_event AS (
            SELECT
                eb.pallet_id,
                eb.pallet_sequence AS emission_pallet_sequence,
                eb.seq_no,
                eb.created_at AS emission_at
            FROM event_base eb
            WHERE eb.method = 'emission'
        )
        SELECT
            x.pallet_id AS "palletId",
            x.exchange_pallet_sequence AS "exchangePalletSequence",
            TO_CHAR(x.exchange_at, 'YYYY-MM-DD HH24:MI:SS') AS "exchangeAt",
            e.emission_pallet_sequence AS "emissionPalletSequence",
            TO_CHAR(e.emission_at, 'YYYY-MM-DD HH24:MI:SS') AS "emissionAt",
            EXTRACT(EPOCH FROM (e.emission_at - x.exchange_at))::bigint AS "exchangeSeconds",
            LPAD((EXTRACT(EPOCH FROM (e.emission_at - x.exchange_at))::bigint / 3600)::text, 1, '0') || ':' ||
            LPAD((((EXTRACT(EPOCH FROM (e.emission_at - x.exchange_at))::bigint) % 3600) / 60)::text, 2, '0') || ':' ||
            LPAD(((EXTRACT(EPOCH FROM (e.emission_at - x.exchange_at))::bigint) % 60)::text, 2, '0') AS "exchangeTime"
        FROM exchange_event x
        JOIN emission_event e
          ON e.pallet_id = x.pallet_id
         AND e.seq_no = x.seq_no
         AND e.emission_at > x.exchange_at
        ORDER BY x.exchange_at
        """;
    }

    public static String ngDetailSql() {
        return """
            WITH base AS (
                SELECT
                    b.box_id,
                    b.cntr_no,
                    b.received_at,
                    b.manual_result_at,
                    CASE
                        WHEN b.reject_type IN ('체적불량', '외관불량')
                            THEN COALESCE(b.final_at, b.tracking_at, b.manual_result_at)
                        WHEN b.final_status = :storedStatus
                            THEN b.final_at
                        ELSE NULL
                    END AS completed_at,
                    b.reject_type,
                    b.final_remark,
                    CASE
                        WHEN b.reject_type IN ('체적불량', '외관불량') THEN 'NG'
                        WHEN b.final_status = :storedStatus THEN 'OK'
                        ELSE 'PENDING'
                    END AS result_type
                FROM samsung_mw.tb_mw_box b
                WHERE b.received_at::date = :targetDate
                  AND (:blNo IS NULL OR :blNo = '' OR b.bl_no = :blNo)
                  AND (:cntrNo IS NULL OR :cntrNo = '' OR b.cntr_no = :cntrNo)
            )
            SELECT
                b.box_id AS "boxSerialCode",
                b.cntr_no AS "containerNo",
                TO_CHAR(b.received_at, 'YYYY-MM-DD HH24:MI:SS') AS "firstReceivedAt",
                TO_CHAR(b.manual_result_at, 'YYYY-MM-DD HH24:MI:SS') AS "manualProcessedAt",
                TO_CHAR(b.completed_at, 'YYYY-MM-DD HH24:MI:SS') AS "finalCompletedAt",
                b.reject_type AS "rejectType",
                b.final_remark AS "finalRemark",
                b.result_type AS "resultType"
            FROM base b
            WHERE b.result_type = 'NG'
            ORDER BY b.received_at
            """;
    }

    public static String monthlySummarySql() {
        return """
        WITH day_base_all AS (
            SELECT
                b.received_at::date AS report_date,
                b.box_id,
                b.bl_no,
                b.cntr_no,
                b.item_code,
                b.received_at,
                b.final_at,
                b.tracking_at,
                b.manual_result_at,
                b.final_status,
                b.reject_type,
                CASE
                    WHEN b.reject_type IN ('체적불량', '외관불량') THEN 'NG'
                    WHEN b.final_status = :storedStatus THEN 'OK'
                    ELSE 'PENDING'
                END AS result_type
            FROM samsung_mw.tb_mw_box b
            WHERE TO_CHAR(b.received_at, 'YYYY-MM') = :month
        ),
        manual_delivery_items AS (
            SELECT DISTINCT
                d.bl_no,
                d.cntr_no,
                d.inner_item_code
            FROM samsung_mw.tb_mw_inbound_delivery d
            WHERE d.manual_flag = true
              AND d.inner_item_code IS NOT NULL
              AND d.inner_item_code <> ''
        ),
        day_base AS (
            SELECT
                dba.report_date,
                dba.box_id,
                dba.bl_no,
                dba.cntr_no,
                dba.item_code,
                dba.received_at,
                dba.final_at,
                dba.tracking_at,
                dba.manual_result_at,
                dba.final_status,
                dba.reject_type,
                dba.result_type
            FROM day_base_all dba
            LEFT JOIN manual_delivery_items mdi
              ON mdi.bl_no = dba.bl_no
             AND mdi.cntr_no = dba.cntr_no
             AND mdi.inner_item_code = dba.item_code
            WHERE mdi.inner_item_code IS NULL
        ),
        agg AS (
            SELECT
                db.report_date,
                COUNT(*) AS total_box_qty,
                COUNT(*) FILTER (WHERE db.result_type = 'OK') AS ok_box_qty,
                COUNT(*) FILTER (WHERE db.result_type = 'NG') AS ng_box_qty,
                COUNT(*) FILTER (WHERE db.result_type = 'PENDING') AS pending_box_qty,
                MIN(db.received_at) FILTER (WHERE db.received_at IS NOT NULL) AS first_received_at,
                MAX(db.final_at) FILTER (
                    WHERE db.final_status = :storedStatus
                      AND db.final_at IS NOT NULL
                ) AS last_palletized_at
            FROM day_base db
            GROUP BY db.report_date
        ),
        process_window AS (
            SELECT
                db.report_date,
                MIN(db.received_at) AS process_start_dt,
                MAX(db.final_at) FILTER (
                    WHERE db.final_status = :storedStatus
                      AND db.final_at IS NOT NULL
                ) AS process_end_dt
            FROM day_base db
            GROUP BY db.report_date
        ),
        pallet_event_base AS (
            SELECT
                pw.report_date,
                e.pallet_id,
                e.method,
                e.pallet_sequence,
                e.created_at,
                CAST(NULLIF(regexp_replace(e.pallet_sequence, '^.*-', ''), '') AS integer) AS seq_no
            FROM samsung_mw.tb_mw_if_xyz_pallet_exchange e
            JOIN process_window pw
              ON pw.process_start_dt IS NOT NULL
             AND pw.process_end_dt IS NOT NULL
             AND e.created_at >= pw.process_start_dt
             AND e.created_at <= pw.process_end_dt
            WHERE e.method IN ('exchange', 'emission')
              AND e.pallet_sequence IS NOT NULL
        ),
        pallet_exchange_event AS (
            SELECT
                peb.report_date,
                peb.pallet_id,
                peb.pallet_sequence AS exchange_pallet_sequence,
                peb.seq_no AS exchange_seq_no,
                peb.created_at AS exchange_at
            FROM pallet_event_base peb
            WHERE peb.method = 'exchange'
        ),
        pallet_emission_event AS (
            SELECT
                peb.report_date,
                peb.pallet_id,
                peb.pallet_sequence AS emission_pallet_sequence,
                peb.seq_no AS emission_seq_no,
                peb.created_at AS emission_at
            FROM pallet_event_base peb
            WHERE peb.method = 'emission'
        ),
        pallet_operating_interval_raw AS (
            SELECT
                x.report_date,
                x.pallet_id,
                x.exchange_pallet_sequence,
                x.exchange_seq_no,
                x.exchange_at AS operating_start_at,
                e.emission_pallet_sequence,
                e.emission_seq_no,
                e.emission_at AS operating_end_at
            FROM pallet_exchange_event x
            JOIN pallet_emission_event e
              ON e.report_date = x.report_date
             AND e.pallet_id = x.pallet_id
             AND e.emission_seq_no = x.exchange_seq_no
             AND e.emission_at > x.exchange_at
        ),
        pallet_operating_interval_valid AS (
            SELECT
                por.report_date,
                por.operating_start_at,
                por.operating_end_at
            FROM pallet_operating_interval_raw por
            JOIN process_window pw
              ON pw.report_date = por.report_date
            WHERE pw.process_start_dt IS NOT NULL
              AND pw.process_end_dt IS NOT NULL
              AND por.operating_end_at > por.operating_start_at
              AND por.operating_start_at < pw.process_end_dt
              AND por.operating_end_at > pw.process_start_dt
        ),
        pallet_operating_interval_clamped AS (
            SELECT
                poiv.report_date,
                GREATEST(poiv.operating_start_at, pw.process_start_dt) AS operating_start_at,
                LEAST(poiv.operating_end_at, pw.process_end_dt) AS operating_end_at
            FROM pallet_operating_interval_valid poiv
            JOIN process_window pw
              ON pw.report_date = poiv.report_date
            WHERE LEAST(poiv.operating_end_at, pw.process_end_dt) >
                  GREATEST(poiv.operating_start_at, pw.process_start_dt)
        ),
        pallet_operating_ordered AS (
            SELECT
                poic.report_date,
                poic.operating_start_at,
                poic.operating_end_at,
                MAX(poic.operating_end_at) OVER (
                    PARTITION BY poic.report_date
                    ORDER BY poic.operating_start_at, poic.operating_end_at
                    ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
                ) AS prev_max_end
            FROM pallet_operating_interval_clamped poic
        ),
        pallet_operating_grouped AS (
            SELECT
                poo.report_date,
                poo.operating_start_at,
                poo.operating_end_at,
                SUM(
                    CASE
                        WHEN poo.prev_max_end IS NULL OR poo.operating_start_at > poo.prev_max_end THEN 1
                        ELSE 0
                    END
                ) OVER (
                    PARTITION BY poo.report_date
                    ORDER BY poo.operating_start_at, poo.operating_end_at
                ) AS grp
            FROM pallet_operating_ordered poo
        ),
        pallet_operating_merged AS (
            SELECT
                pog.report_date,
                pog.grp,
                MIN(pog.operating_start_at) AS operating_start_at,
                MAX(pog.operating_end_at) AS operating_end_at
            FROM pallet_operating_grouped pog
            GROUP BY pog.report_date, pog.grp
        ),
        pallet_operating_daily AS (
            SELECT
                pom.report_date,
                COALESCE(SUM(EXTRACT(EPOCH FROM (pom.operating_end_at - pom.operating_start_at))), 0)::numeric AS operating_seconds
            FROM pallet_operating_merged pom
            GROUP BY pom.report_date
        ),
        final_data AS (
            SELECT
                a.report_date,
                a.total_box_qty,
                a.ok_box_qty,
                a.ng_box_qty,
                a.pending_box_qty,
                a.first_received_at,
                a.last_palletized_at,
                CASE
                    WHEN a.first_received_at IS NOT NULL
                     AND a.last_palletized_at IS NOT NULL
                        THEN ROUND(EXTRACT(EPOCH FROM (a.last_palletized_at - a.first_received_at)))::bigint
                    ELSE NULL
                END AS total_operating_seconds_i,
                COALESCE(ROUND(pod.operating_seconds)::bigint, 0) AS pallet_operating_seconds_i
            FROM agg a
            LEFT JOIN pallet_operating_daily pod
              ON pod.report_date = a.report_date
        )
        SELECT
            TO_CHAR(fd.report_date, 'YYYY-MM-DD') AS "reportDate",
            fd.total_box_qty AS "totalBoxQty",
            fd.ok_box_qty AS "okBoxQty",
            fd.ng_box_qty AS "ngBoxQty",
            fd.pending_box_qty AS "pendingBoxQty",
            CASE
                WHEN fd.total_operating_seconds_i IS NOT NULL
                    THEN LPAD((fd.total_operating_seconds_i / 3600)::text, 2, '0') || ':' ||
                         LPAD(((fd.total_operating_seconds_i % 3600) / 60)::text, 2, '0') || ':' ||
                         LPAD((fd.total_operating_seconds_i % 60)::text, 2, '0')
                ELSE NULL
            END AS "totalOperatingTime",
            CASE
                WHEN fd.pallet_operating_seconds_i IS NOT NULL
                    THEN LPAD((fd.pallet_operating_seconds_i / 3600)::text, 2, '0') || ':' ||
                         LPAD(((fd.pallet_operating_seconds_i % 3600) / 60)::text, 2, '0') || ':' ||
                         LPAD((fd.pallet_operating_seconds_i % 60)::text, 2, '0')
                ELSE NULL
            END AS "palletOperatingTime",
            CASE
                WHEN fd.total_operating_seconds_i IS NOT NULL
                     AND fd.total_operating_seconds_i > 0
                    THEN ROUND(fd.ok_box_qty::numeric / (fd.total_operating_seconds_i / 3600.0), 2)
                ELSE NULL
            END AS "totalTimeUph",
            CASE
                WHEN fd.pallet_operating_seconds_i IS NOT NULL
                     AND fd.pallet_operating_seconds_i > 0
                    THEN ROUND(fd.ok_box_qty::numeric / (fd.pallet_operating_seconds_i / 3600.0), 2)
                ELSE NULL
            END AS "palletTimeUph",
            COALESCE(TO_CHAR(fd.first_received_at, 'YYYY-MM-DD HH24:MI:SS'), '-') AS "firstReceivedAt",
            COALESCE(TO_CHAR(fd.last_palletized_at, 'YYYY-MM-DD HH24:MI:SS'), '-') AS "lastPalletizedAt"
        FROM final_data fd
        ORDER BY fd.report_date
        """;
    }
}