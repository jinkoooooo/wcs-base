INSERT INTO device_confs
SELECT 
    uuid_generate_v4() AS id,
    c.id AS device_profile_id,
    a.category,
    a.name,
    a.description,
    a.value,
    a.remark,
    a.config,
    c.domain_id AS domain_id,
    a.creator_id,
    a.updater_id,
    a.created_at,
    a.updated_at
FROM 
    device_confs a,
    (SELECT * 
     FROM device_profiles 
     WHERE domain_id = :sourceDomainId 
       AND default_flag = true) b,
    (SELECT * 
     FROM device_profiles 
     WHERE domain_id = :targetDomainId) c
WHERE 
    a.domain_id = :sourceDomainId
    AND a.device_profile_id = b.id
    AND b.profile_cd = c.profile_cd;