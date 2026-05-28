SELECT b.name, a.id AS fromid, b.id AS toid
FROM menus a, menus b
WHERE a.DOMAIN_ID = :sourceDomainId
AND b.DOMAIN_ID = :targetDomainId
AND a.name = b.name