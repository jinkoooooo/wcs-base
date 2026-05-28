SELECT
    TO_CHAR(b.received_at, 'YYYY-MM-DD') AS date,
    b.bl_no,
    b.cntr_no,
    b.box_id,
    b.parcel_id,
    b.plc_seq_no,
    b.item_code,
    'BCR' AS process_type,
    CAST(NULL AS varchar) AS tracking_status,
    b.tracking_desc,
    TO_CHAR(b.received_at, 'YYYY-MM-DD HH24:MI:SS') AS event_time,
    b.first_line_id AS line_id,
    b.first_equip_id AS equip_id,
    CAST(b.final_status AS varchar) AS final_status,
    b.reject_type
FROM samsung_mw.tb_mw_box b
WHERE b.received_at::date = :targetDate
  AND (:blNo IS NULL OR :blNo = '' OR b.bl_no = :blNo)
  AND (:cntrNo IS NULL OR :cntrNo = '' OR b.cntr_no = :cntrNo)
  AND (:excludeManualYn <> 'Y' OR b.manual_result_at IS NULL)
ORDER BY b.received_at;