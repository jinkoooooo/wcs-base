SELECT 
    uuid_generate_v4() AS id,
    c.id AS menu_detail_id,
    a.name,
    a.icon,
    a.style,
    a.logic,
    c.domain_id
FROM 
    menu_detail_buttons a,
    (SELECT * 
     FROM menu_details 
     WHERE domain_id = :sourceDomainId) b,
    (SELECT * 
     FROM menu_details 
     WHERE domain_id = :targetDomainId) c
WHERE 
    a.domain_id = :sourceDomainId
    AND a.menu_detail_id = b.id
    AND b.name = c.name;