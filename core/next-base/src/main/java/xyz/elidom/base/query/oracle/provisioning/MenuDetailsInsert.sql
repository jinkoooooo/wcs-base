INSERT INTO MENU_DETAILS (
    ID, MENU_ID, NAME, VIEW_SECTION, ENTITY_ID, DATA_PROP,
    ASSOCIATION, SEARCH_URL, SAVE_URL, MASTER_FIELD, CUSTOM_VIEW, DOMAIN_ID
)
SELECT
    LOWER(
	    REGEXP_REPLACE(
	        RAWTOHEX(SYS_GUID()),
	        '(.{8})(.{4})(.{4})(.{4})(.{12})',
	        '\1-\2-\3-\4-\5'
	    )
	) AS ID, B.ID AS MENU_ID, C.NAME, C.VIEW_SECTION, C.ENTITY_ID,
    C.DATA_PROP, C.ASSOCIATION, C.SEARCH_URL, C.SAVE_URL, C.MASTER_FIELD, C.CUSTOM_VIEW,
    b.DOMAIN_ID AS DOMAIN_ID
FROM
    (SELECT * FROM menus WHERE domain_id = :sourceDomainId) a,
    (SELECT * FROM menus WHERE domain_id = :targetDomainId) b,
    (SELECT * FROM MENU_DETAILS WHERE domain_id = :sourceDomainId) C
WHERE
    a.name = b.name AND a.id = c.menu_id