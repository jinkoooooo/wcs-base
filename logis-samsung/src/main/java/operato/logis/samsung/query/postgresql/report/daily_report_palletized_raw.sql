SELECT
    TO_CHAR(b.final_at, 'YYYY-MM-DD') AS date,
    b.bl_no,
    b.cntr_no,
    b.box_id,
    b.parcel_id,
    b.plc_seq_no,
    b.item_code,
    'PALLETIZED' AS process_type,
    CAST(b.final_status AS varchar) AS tracking_status,
    b.final_remark AS tracking_desc,
    TO_CHAR(b.final_at, 'YYYY-MM-DD HH24:MI:SS') AS event_time,
    CAST(NULL AS varchar) AS line_id,
    b.end_point_cd AS equip_id,
    CAST(b.final_status AS varchar) AS final_status,
    b.reject_type
FROM samsung_mw.tb_mw_box b
WHERE b.final_at::date = :targetDate
  AND (:blNo IS NULL OR :blNo = '' OR b.bl_no = :blNo)
  AND (:cntrNo IS NULL OR :cntrNo = '' OR b.cntr_no = :cntrNo)
  AND (:excludeManualYn <> 'Y' OR b.manual_result_at IS NULL)
ORDER BY b.final_at;