INSERT INTO device_profiles
SELECT 
    uuid_generate_v4() AS id,
    a.stage_cd,
    a.device_type,
    a.com_cd,
    a.job_type,
    a.equip_type,
    a.equip_cd,
    a.profile_cd,
    a.profile_nm,
    a.default_flag,
    :targetDomainId AS domain_id,
    a.creator_id,
    a.updater_id,
    a.created_at,
    a.updated_at
FROM device_profiles a
WHERE a.domain_id = :sourceDomainId
  AND a.default_flag = true;