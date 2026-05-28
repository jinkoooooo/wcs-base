UPDATE menus
SET PARENT_ID = :toid
WHERE PARENT_ID = :fromid AND DOMAIN_ID = :domainId