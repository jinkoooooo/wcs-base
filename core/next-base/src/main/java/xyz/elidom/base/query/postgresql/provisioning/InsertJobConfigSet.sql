INSERT INTO job_config_set
SELECT 
    uuid_generate_v4() AS id,
    com_cd,
    stage_cd,
    job_type,
    equip_type,
    equip_cd,
    conf_set_cd,
    conf_set_nm,
    default_flag,
    remark,
    :targetDomainId AS domain_id,
    creator_id,
    updater_id,
    created_at,
    updated_at
FROM job_config_set
WHERE domain_id = :sourceDomainId
  AND default_flag = true