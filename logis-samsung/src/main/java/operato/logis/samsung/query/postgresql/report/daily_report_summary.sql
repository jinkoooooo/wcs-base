WITH base AS (
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
        b.final_remark,
        CASE
            WHEN b.reject_type IN ('체적불량', '외관불량') THEN 'NG'
            WHEN b.final_status = :storedStatus THEN 'OK'
            ELSE 'PENDING'
            END AS result_type,
        CASE
            WHEN b.reject_type IN ('체적불량', '외관불량')
                THEN COALESCE(b.final_at, b.tracking_at, b.manual_result_at)
            WHEN b.final_status = :storedStatus
                THEN b.final_at
            ELSE NULL
            END AS completed_at,
        CASE
            WHEN b.final_status = :storedStatus
                AND b.received_at IS NOT NULL
                AND b.final_at IS NOT NULL
                THEN EXTRACT(EPOCH FROM (b.final_at - b.received_at))::numeric
    ELSE NULL
END AS elapsed_seconds
    FROM samsung_mw.tb_mw_box b
    WHERE b.received_at::date = :targetDate
      AND (:blNo IS NULL OR :blNo = '' OR b.bl_no = :blNo)
      AND (:cntrNo IS NULL OR :cntrNo = '' OR b.cntr_no = :cntrNo)
      AND (:excludeManualYn <> 'Y' OR b.manual_result_at IS NULL)
),
job_info AS (
    SELECT
        j.cntr_no,
        j.bl_no,
        MIN(j.job_start_dt) AS job_start_dt,
        MAX(j.job_end_dt) AS job_end_dt,
        MAX(j.sku_qty) AS job_sku_qty
    FROM samsung_mw.tb_mw_inbound_job j
    WHERE j.inbound_date::date = :targetDate
      AND (:blNo IS NULL OR :blNo = '' OR j.bl_no = :blNo)
      AND (:cntrNo IS NULL OR :cntrNo = '' OR j.cntr_no = :cntrNo)
    GROUP BY j.cntr_no, j.bl_no
),
inbound_box_base AS (
    SELECT
        b.bl_no,
        b.cntr_no,
        b.received_at
    FROM samsung_mw.tb_mw_box b
    WHERE b.received_at::date = :targetDate
      AND (:blNo IS NULL OR :blNo = '' OR b.bl_no = :blNo)
      AND (:cntrNo IS NULL OR :cntrNo = '' OR b.cntr_no = :cntrNo)
      AND b.received_at IS NOT NULL
      AND (:excludeManualYn <> 'Y' OR b.manual_result_at IS NULL)
),
inbound_box_seq AS (
    SELECT
        ibb.received_at,
        LEAD(ibb.received_at) OVER (ORDER BY ibb.received_at) AS next_received_at
    FROM inbound_box_base ibb
),
inbound_gap AS (
    SELECT
        EXTRACT(EPOCH FROM (ibs.next_received_at - ibs.received_at))::numeric AS gap_seconds,
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
pallet_event_base AS (
    SELECT
        e.pallet_id,
        e.method,
        e.pallet_sequence,
        e.created_at,
        CAST(NULLIF(regexp_replace(e.pallet_sequence, '^.*-', ''), '') AS integer) AS seq_no
    FROM samsung_mw.tb_mw_if_xyz_pallet_exchange e
    CROSS JOIN (
        SELECT
            MIN(b.received_at) AS process_start_dt,
            MAX(b.final_at) FILTER (WHERE b.final_status = :storedStatus AND b.final_at IS NOT NULL) AS process_end_dt
        FROM base b
    ) t
    WHERE e.method IN ('emission', 'exchange')
      AND t.process_start_dt IS NOT NULL
      AND t.process_end_dt IS NOT NULL
      AND e.created_at >= t.process_start_dt
      AND e.created_at <= t.process_end_dt
      AND e.pallet_sequence IS NOT NULL
),
pallet_exchange_event AS (
    SELECT
        peb.pallet_id,
        peb.pallet_sequence AS exchange_pallet_sequence,
        peb.seq_no AS exchange_seq_no,
        peb.created_at AS exchange_at
    FROM pallet_event_base peb
    WHERE peb.method = 'exchange'
),
pallet_emission_event AS (
    SELECT
        peb.pallet_id,
        peb.pallet_sequence AS emission_pallet_sequence,
        peb.seq_no AS emission_seq_no,
        peb.created_at AS emission_at
    FROM pallet_event_base peb
    WHERE peb.method = 'emission'
),
pallet_idle_time AS (
    SELECT
        COALESCE(SUM(idle_seconds), 0)::numeric AS idle_seconds
    FROM (
        SELECT
            e.pallet_id,
            e.emission_seq_no,
            e.emission_at,
            CASE
                WHEN nx.next_exchange_at IS NOT NULL
                     AND nx.next_exchange_at <= t.process_end_dt
                    THEN EXTRACT(EPOCH FROM (nx.next_exchange_at - e.emission_at))
                WHEN t.process_end_dt > e.emission_at
                    THEN EXTRACT(EPOCH FROM (t.process_end_dt - e.emission_at))
                ELSE 0
            END AS idle_seconds
        FROM pallet_emission_event e
        CROSS JOIN (
            SELECT
                MAX(b.final_at) FILTER (WHERE b.final_status = :storedStatus AND b.final_at IS NOT NULL) AS process_end_dt
            FROM base b
        ) t
        LEFT JOIN LATERAL (
            SELECT
                x.exchange_at AS next_exchange_at
            FROM pallet_exchange_event x
            WHERE x.pallet_id = e.pallet_id
              AND x.exchange_seq_no = e.emission_seq_no + 1
              AND x.exchange_at > e.emission_at
            ORDER BY x.exchange_at
            LIMIT 1
        ) nx ON TRUE
        WHERE t.process_end_dt IS NOT NULL
          AND e.emission_at < t.process_end_dt
    ) z
),
pallet_operating_time AS (
    SELECT
        COALESCE(SUM(EXTRACT(EPOCH FROM (e.emission_at - x.exchange_at))), 0)::numeric AS operating_seconds
    FROM pallet_exchange_event x
    JOIN pallet_emission_event e
      ON e.pallet_id = x.pallet_id
     AND e.emission_seq_no = x.exchange_seq_no
     AND e.emission_at > x.exchange_at
),
agg AS (
    SELECT
        COUNT(*) AS total_box_qty,
        COUNT(*) FILTER (WHERE b.final_status = :storedStatus) AS done_box_qty,
        COUNT(*) FILTER (WHERE b.result_type = 'OK') AS ok_box_qty,
        COUNT(*) FILTER (WHERE b.result_type = 'NG') AS ng_box_qty,
        COUNT(*) FILTER (WHERE b.result_type = 'PENDING') AS pending_box_qty,
        COUNT(DISTINCT b.item_code) FILTER (WHERE b.item_code IS NOT NULL AND b.item_code <> '') AS actual_sku_qty,
        MIN(b.received_at) FILTER (WHERE b.received_at IS NOT NULL) AS first_received_at,
        MAX(b.final_at) FILTER (WHERE b.final_status = :storedStatus AND b.final_at IS NOT NULL) AS last_palletized_at,
        MIN(b.completed_at) FILTER (WHERE b.completed_at IS NOT NULL) AS first_completed_at,
        MAX(b.completed_at) FILTER (WHERE b.completed_at IS NOT NULL) AS last_completed_at,
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
        COUNT(*) AS normal_box_qty,
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
        pit.idle_seconds,
        pot.operating_seconds,
        ROUND(a.avg_all_sec)::bigint AS avg_all_sec_i,
        ROUND(a.min_sec)::bigint AS min_sec_i,
        ROUND(a.max_sec)::bigint AS max_sec_i,
        ROUND(p.p50_sec)::bigint AS p50_sec_i,
        ROUND(p.p95_sec)::bigint AS p95_sec_i,
        ROUND(f.avg_excl_p95_sec)::bigint AS avg_excl_p95_sec_i,
        f.normal_box_qty
    FROM agg a
    CROSS JOIN pct p
    CROSS JOIN filtered f
    LEFT JOIN job_info j ON 1 = 1
    LEFT JOIN pallet_idle_time pit ON 1 = 1
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
        sf.job_start_dt,
        sf.job_end_dt,
        sf.first_received_at,
        sf.last_palletized_at,
        sf.first_completed_at,
        sf.last_completed_at,
        sf.idle_seconds,
        sf.operating_seconds,
        CASE
            WHEN sf.first_received_at IS NOT NULL
             AND sf.last_palletized_at IS NOT NULL
                THEN ROUND(EXTRACT(EPOCH FROM (sf.last_palletized_at - sf.first_received_at)))::bigint
            ELSE NULL
        END AS total_operating_seconds_i,
        CASE
            WHEN sf.operating_seconds IS NOT NULL
                THEN ROUND(sf.operating_seconds)::bigint
            ELSE NULL
        END AS pallet_operating_seconds_i,
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
    TO_CHAR(:targetDate, 'YYYY-MM-DD') AS today_date,
    :blNo AS bl_no,
    :cntrNo AS cntr_no,
    fd.total_box_qty,
    fd.done_box_qty,
    fd.ok_box_qty,
    fd.ng_box_qty,
    fd.pending_box_qty,
    fd.job_sku_qty,
    fd.actual_sku_qty,
    COALESCE(to_char(fd.first_received_at, 'YYYY-MM-DD HH24:MI:SS'), '-') AS first_received_at,
    COALESCE(to_char(fd.last_palletized_at, 'YYYY-MM-DD HH24:MI:SS'), '-') AS last_palletized_at,
    CASE
        WHEN fd.total_operating_seconds_i IS NOT NULL
            THEN LPAD((fd.total_operating_seconds_i / 3600)::text, 2, '0') || ':' ||
                 LPAD(((fd.total_operating_seconds_i % 3600) / 60)::text, 2, '0') || ':' ||
                 LPAD((fd.total_operating_seconds_i % 60)::text, 2, '0')
        ELSE NULL
        END AS total_operating_time,
    CASE
        WHEN fd.idle_seconds IS NOT NULL
            THEN LPAD((ROUND(fd.idle_seconds)::bigint / 3600)::text, 2, '0') || ':' ||
                 LPAD((((ROUND(fd.idle_seconds)::bigint) % 3600) / 60)::text, 2, '0') || ':' ||
                 LPAD(((ROUND(fd.idle_seconds)::bigint) % 60)::text, 2, '0')
        ELSE NULL
        END AS idle_time,
    CASE
        WHEN fd.pallet_operating_seconds_i IS NOT NULL
            THEN LPAD((fd.pallet_operating_seconds_i / 3600)::text, 2, '0') || ':' ||
                 LPAD(((fd.pallet_operating_seconds_i % 3600) / 60)::text, 2, '0') || ':' ||
                 LPAD((fd.pallet_operating_seconds_i % 60)::text, 2, '0')
        ELSE NULL
        END AS pallet_operating_time,
    LPAD((fd.avg_all_sec_i / 3600)::text, 2, '0') || ':' ||
    LPAD(((fd.avg_all_sec_i % 3600) / 60)::text, 2, '0') || ':' ||
    LPAD((fd.avg_all_sec_i % 60)::text, 2, '0') AS avg_all_time,
    LPAD((fd.p50_sec_i / 3600)::text, 2, '0') || ':' ||
    LPAD(((fd.p50_sec_i % 3600) / 60)::text, 2, '0') || ':' ||
    LPAD((fd.p50_sec_i % 60)::text, 2, '0') AS median_time,
    LPAD((fd.p95_sec_i / 3600)::text, 2, '0') || ':' ||
    LPAD(((fd.p95_sec_i % 3600) / 60)::text, 2, '0') || ':' ||
    LPAD((fd.p95_sec_i % 60)::text, 2, '0') AS p95_time,
    LPAD((fd.avg_excl_p95_sec_i / 3600)::text, 2, '0') || ':' ||
    LPAD(((fd.avg_excl_p95_sec_i % 3600) / 60)::text, 2, '0') || ':' ||
    LPAD((fd.avg_excl_p95_sec_i % 60)::text, 2, '0') AS avg_excl_p95_time,
    LPAD((fd.min_sec_i / 3600)::text, 2, '0') || ':' ||
    LPAD(((fd.min_sec_i % 3600) / 60)::text, 2, '0') || ':' ||
    LPAD((fd.min_sec_i % 60)::text, 2, '0') AS min_time,
    LPAD((fd.max_sec_i / 3600)::text, 2, '0') || ':' ||
    LPAD(((fd.max_sec_i % 3600) / 60)::text, 2, '0') || ':' ||
    LPAD((fd.max_sec_i % 60)::text, 2, '0') AS max_time,
    CASE
        WHEN fd.total_operating_seconds_i IS NOT NULL
            AND fd.total_operating_seconds_i > 0
            THEN ROUND(fd.done_box_qty::numeric / (fd.total_operating_seconds_i / 3600.0), 2)
        ELSE NULL
        END AS total_time_uph,
    CASE
        WHEN fd.pallet_operating_seconds_i IS NOT NULL
            AND fd.pallet_operating_seconds_i > 0
            THEN ROUND(fd.done_box_qty::numeric / (fd.pallet_operating_seconds_i / 3600.0), 2)
        ELSE NULL
        END AS pallet_time_uph,
    CASE
        WHEN fd.inbound_net_operating_seconds_i IS NOT NULL
            AND fd.inbound_net_operating_seconds_i > 0
            THEN ROUND(fd.inbound_total_box_qty::numeric / (fd.inbound_net_operating_seconds_i / 3600.0), 2)
        ELSE NULL
        END AS inbound_net_time_uph,
    ROUND(
            CASE
                WHEN fd.total_box_qty > 0
                    THEN (fd.ng_box_qty::numeric / fd.total_box_qty) * 100
                ELSE 0
                END,
            2
    ) AS ng_rate_pct,
    '총 박스수 : ' || COALESCE(fd.total_box_qty::text, '0') || E'\n' ||
    '작업지시 SKU 수량 : ' || COALESCE(fd.job_sku_qty::text, '0') || E'\n' ||
    '실처리 SKU 수량 : ' || COALESCE(fd.actual_sku_qty::text, '0') || E'\n' ||
    '정상완료 : ' || COALESCE(fd.ok_box_qty::text, '0') || E'\n' ||
    '최종 NG : ' || COALESCE(fd.ng_box_qty::text, '0') || E'\n' ||
    '미완료 : ' || COALESCE(fd.pending_box_qty::text, '0') || E'\n' ||
    '입고 첫 박스 시작시간 : ' || COALESCE(to_char(fd.first_received_at, 'YYYY-MM-DD HH24:MI:SS'), '-') || E'\n' ||
    '마지막 박스 적재시간 : ' || COALESCE(to_char(fd.last_palletized_at, 'YYYY-MM-DD HH24:MI:SS'), '-') || E'\n' ||
    '전체 운영 시간 : ' || COALESCE(
        LPAD((fd.total_operating_seconds_i / 3600)::text, 2, '0') || ':' ||
        LPAD(((fd.total_operating_seconds_i % 3600) / 60)::text, 2, '0') || ':' ||
        LPAD((fd.total_operating_seconds_i % 60)::text, 2, '0'),
        '-'
    ) || E'\n' ||
    '전체운영 UPH : ' || COALESCE(
        CASE
            WHEN fd.total_operating_seconds_i > 0
                THEN ROUND(fd.done_box_qty::numeric / (fd.total_operating_seconds_i / 3600.0), 2)::text
            ELSE NULL
        END,
        '0'
    ) || E'\n' ||
    '파렛타이저 유휴시간 : ' || COALESCE(
        LPAD((ROUND(fd.idle_seconds)::bigint / 3600)::text, 2, '0') || ':' ||
        LPAD((((ROUND(fd.idle_seconds)::bigint) % 3600) / 60)::text, 2, '0') || ':' ||
        LPAD(((ROUND(fd.idle_seconds)::bigint) % 60)::text, 2, '0'),
        '-'
    ) || E'\n' ||
    '파렛타이저 운영시간 : ' || COALESCE(
        LPAD((fd.pallet_operating_seconds_i / 3600)::text, 2, '0') || ':' ||
        LPAD(((fd.pallet_operating_seconds_i % 3600) / 60)::text, 2, '0') || ':' ||
        LPAD((fd.pallet_operating_seconds_i % 60)::text, 2, '0'),
        '-'
    ) || E'\n' ||
    '파렛타이저 UPH : ' || COALESCE(
        CASE
            WHEN fd.pallet_operating_seconds_i > 0
                THEN ROUND(fd.done_box_qty::numeric / (fd.pallet_operating_seconds_i / 3600.0), 2)::text
            ELSE NULL
        END,
        '0'
    ) || E'\n' ||
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
    ) || '%' || E'\n' ||
    '박스입고 평균 소요시간(이상치제외) : ' ||
    (LPAD((fd.avg_excl_p95_sec_i / 3600)::text, 2, '0') || ':' ||
     LPAD(((fd.avg_excl_p95_sec_i % 3600) / 60)::text, 2, '0') || ':' ||
     LPAD((fd.avg_excl_p95_sec_i % 60)::text, 2, '0')) || E'\n' ||
    '평균 : ' ||
    (LPAD((fd.avg_all_sec_i / 3600)::text, 2, '0') || ':' ||
     LPAD(((fd.avg_all_sec_i % 3600) / 60)::text, 2, '0') || ':' ||
     LPAD((fd.avg_all_sec_i % 60)::text, 2, '0')) || E'\n' ||
    '중앙값 : ' ||
    (LPAD((fd.p50_sec_i / 3600)::text, 2, '0') || ':' ||
     LPAD(((fd.p50_sec_i % 3600) / 60)::text, 2, '0') || ':' ||
     LPAD((fd.p50_sec_i % 60)::text, 2, '0')) || E'\n' ||
    'P95 : ' ||
    (LPAD((fd.p95_sec_i / 3600)::text, 2, '0') || ':' ||
     LPAD(((fd.p95_sec_i % 3600) / 60)::text, 2, '0') || ':' ||
     LPAD((fd.p95_sec_i % 60)::text, 2, '0')) || E'\n' ||
    '박스입고 최소시간 : ' ||
    (LPAD((fd.min_sec_i / 3600)::text, 2, '0') || ':' ||
     LPAD(((fd.min_sec_i % 3600) / 60)::text, 2, '0') || ':' ||
     LPAD((fd.min_sec_i % 60)::text, 2, '0')) || E'\n' ||
    '박스입고 최대시간 : ' ||
    (LPAD((fd.max_sec_i / 3600)::text, 2, '0') || ':' ||
     LPAD(((fd.max_sec_i % 3600) / 60)::text, 2, '0') || ':' ||
     LPAD((fd.max_sec_i % 60)::text, 2, '0')) AS summary_text
FROM final_data fd;