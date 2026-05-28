SELECT
    TO_CHAR(t.tracking_at, 'YYYY-MM-DD') AS date,
    b.bl_no,
    b.cntr_no,
    t.box_id,
    t.parcel_id,
    t.plc_seq_no,
    b.item_code,
    'SORTER' AS process_type,
    CAST(t.tracking_status AS varchar) AS tracking_status,
    t.tracking_desc,
    TO_CHAR(t.tracking_at, 'YYYY-MM-DD HH24:MI:SS') AS event_time,
    t.line_id,
    t.equip_id,
    CAST(b.final_status AS varchar) AS final_status,
    b.reject_type
FROM samsung_mw.tb_mw_box_track t
    JOIN samsung_mw.tb_mw_box b ON b.box_id = t.box_id
WHERE t.tracking_at::date = :targetDate
  AND t.tracking_status = :reportDvrtStatus
  AND (:blNo IS NULL OR :blNo = '' OR b.bl_no = :blNo)
  AND (:cntrNo IS NULL OR :cntrNo = '' OR b.cntr_no = :cntrNo)
  AND (:excludeManualYn <> 'Y' OR b.manual_result_at IS NULL)
ORDER BY t.tracking_at;