SELECT 
    uuid_generate_v4() AS id,
    c.id AS menu_id,
    b.name,
    b.view_section,
    b.entity_id,
    b.data_prop,
    b.association,
    b.search_url,
    b.save_url,
    b.master_field,
    b.custom_view,
    c.domain_id AS domain_id,
    b.creator_id,
    b.updater_id,
    b.created_at,
    b.updated_at
FROM 
    (SELECT * 
     FROM menus 
     WHERE domain_id = :sourceDomainId) a,
    (SELECT * 
     FROM menus 
     WHERE domain_id = :targetDomainId) c,
    menu_details b
WHERE 
    a.domain_id = :sourceDomainId
    AND a.id = b.menu_id
    AND a.name = c.name;