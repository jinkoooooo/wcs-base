INSERT INTO ind_configs
SELECT 
    uuid_generate_v4() AS id,
    CASE 
        WHEN b.job_type = 'DAS' THEN c.id
        WHEN b.job_type = 'DPS' THEN d.id
    END AS ind_config_set_id,
    a.category,
    a.name,
    a.description,
    a.value,
    a.remark,
    a.config,
    :targetDomainId AS domain_id,
    a.creator_id,
    a.updater_id,
    a.created_at,
    a.updated_at
FROM 
    ind_configs a
    LEFT JOIN (
        SELECT * 
        FROM ind_config_set
        WHERE domain_id = :sourceDomainId
          AND default_flag = true
    ) b ON a.ind_config_set_id = b.id
    LEFT JOIN (
        SELECT id, job_type, domain_id
        FROM ind_config_set
        WHERE domain_id = :targetDomainId
          AND job_type = 'DAS'
    ) c ON b.job_type = c.job_type
    LEFT JOIN (
        SELECT id, job_type, domain_id
        FROM ind_config_set
        WHERE domain_id = :targetDomainId
          AND job_type = 'DPS'
    ) d ON b.job_type = d.job_type
WHERE 
    a.domain_id = :sourceDomainId
    AND b.default_flag = true;