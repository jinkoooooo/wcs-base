WITH box_base AS (
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
    AND (:excludeManualYn <> 'Y' OR b.manual_result_at IS NULL)
    ),
    proc AS (
SELECT
    bb.*,
    m110.tracking_at AS bcr_measured_at,
    m120.tracking_at AS vision_measured_at,
    d501.tracking_at AS report_dvrt_at,
    p531.tracking_at AS report_pltz_at
FROM box_base bb
    LEFT JOIN LATERAL (
    SELECT t.tracking_at
    FROM samsung_mw.tb_mw_box_track t
    WHERE t.box_id = bb.box_id
    AND t.tracking_status = :bcrMeasuredStatus
    ORDER BY t.tracking_at ASC
    LIMIT 1
    ) m110 ON TRUE
    LEFT JOIN LATERAL (
    SELECT t.tracking_at
    FROM samsung_mw.tb_mw_box_track t
    WHERE t.box_id = bb.box_id
    AND t.tracking_status = :visionMeasuredStatus
    ORDER BY t.tracking_at ASC
    LIMIT 1
    ) m120 ON TRUE
    LEFT JOIN LATERAL (
    SELECT t.tracking_at
    FROM samsung_mw.tb_mw_box_track t
    WHERE t.box_id = bb.box_id
    AND t.tracking_status = :reportDvrtStatus
    ORDER BY t.tracking_at ASC
    LIMIT 1
    ) d501 ON TRUE
    LEFT JOIN LATERAL (
    SELECT t.tracking_at
    FROM samsung_mw.tb_mw_box_track t
    WHERE t.box_id = bb.box_id
    AND t.tracking_status = :reportPltzStatus
    ORDER BY t.tracking_at ASC
    LIMIT 1
    ) p531 ON TRUE
    ),
    container_summary AS (
SELECT
    'CONTAINER' AS row_type,
    cntr_no AS row_group,
    'CONTAINER' AS process_type,
    bl_no,
    cntr_no,
    NULL AS box_id,
    NULL AS parcel_id,
    NULL AS plc_seq_no,
    NULL AS item_code,
    TO_CHAR(MIN(received_at), 'YYYY-MM-DD HH24:MI:SS') AS start_at,
    TO_CHAR(MAX(final_at), 'YYYY-MM-DD HH24:MI:SS') AS end_at,
    EXTRACT(EPOCH FROM (MAX(final_at) - MIN(received_at)))::bigint AS duration_sec,
    'CONTAINER SUMMARY' AS tooltip_title,
    cntr_no AS tooltip_sub1,
    bl_no AS tooltip_sub2
FROM proc
WHERE received_at IS NOT NULL
  AND final_at IS NOT NULL
GROUP BY bl_no, cntr_no
    ),
    bcr_rows AS (
SELECT
    'BOX' AS row_type,
    cntr_no AS row_group,
    'BCR' AS process_type,
    bl_no,
    cntr_no,
    box_id,
    parcel_id,
    plc_seq_no,
    item_code,
    TO_CHAR(received_at, 'YYYY-MM-DD HH24:MI:SS') AS start_at,
    TO_CHAR(COALESCE(vision_measured_at, bcr_measured_at, received_at), 'YYYY-MM-DD HH24:MI:SS') AS end_at,
    EXTRACT(EPOCH FROM (COALESCE(vision_measured_at, bcr_measured_at, received_at) - received_at))::bigint AS duration_sec,
    'BCR READING' AS tooltip_title,
    item_code AS tooltip_sub1,
    box_id AS tooltip_sub2
FROM proc
    ),
    sorter_rows AS (
SELECT
    'BOX' AS row_type,
    cntr_no AS row_group,
    'SORTER' AS process_type,
    bl_no,
    cntr_no,
    box_id,
    parcel_id,
    plc_seq_no,
    item_code,
    TO_CHAR(COALESCE(vision_measured_at, bcr_measured_at, received_at), 'YYYY-MM-DD HH24:MI:SS') AS start_at,
    TO_CHAR(COALESCE(report_dvrt_at, report_pltz_at, final_at, COALESCE(vision_measured_at, bcr_measured_at, received_at)), 'YYYY-MM-DD HH24:MI:SS') AS end_at,
    EXTRACT(EPOCH FROM (COALESCE(report_dvrt_at, report_pltz_at, final_at, COALESCE(vision_measured_at, bcr_measured_at, received_at)) - COALESCE(vision_measured_at, bcr_measured_at, received_at)))::bigint AS duration_sec,
    'SORTER' AS tooltip_title,
    item_code AS tooltip_sub1,
    box_id AS tooltip_sub2
FROM proc
    ),
    palletized_rows AS (
SELECT
    'BOX' AS row_type,
    cntr_no AS row_group,
    'PALLETIZED' AS process_type,
    bl_no,
    cntr_no,
    box_id,
    parcel_id,
    plc_seq_no,
    item_code,
    TO_CHAR(COALESCE(report_dvrt_at, report_pltz_at, final_at), 'YYYY-MM-DD HH24:MI:SS') AS start_at,
    TO_CHAR(COALESCE(final_at, report_pltz_at, report_dvrt_at), 'YYYY-MM-DD HH24:MI:SS') AS end_at,
    EXTRACT(EPOCH FROM (COALESCE(final_at, report_pltz_at, report_dvrt_at) - COALESCE(report_dvrt_at, report_pltz_at, final_at)))::bigint AS duration_sec,
    'PALLETIZED' AS tooltip_title,
    item_code AS tooltip_sub1,
    box_id AS tooltip_sub2
FROM proc
    )
SELECT * FROM container_summary
UNION ALL
SELECT * FROM bcr_rows
UNION ALL
SELECT * FROM sorter_rows
UNION ALL
SELECT * FROM palletized_rows
ORDER BY
    CASE row_type
        WHEN 'CONTAINER' THEN 0
        WHEN 'BOX' THEN 1
        ELSE 9
        END,
    row_group,
    CASE process_type
        WHEN 'CONTAINER' THEN 0
        WHEN 'BCR' THEN 1
        WHEN 'SORTER' THEN 2
        WHEN 'PALLETIZED' THEN 3
        ELSE 9
        END,
    start_at,
    box_id
;