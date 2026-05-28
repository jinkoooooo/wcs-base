INSERT INTO MENU_DETAIL_BUTTONS (
    ID, MENU_DETAIL_ID, NAME, ICON, STYLE, LOGIC, DOMAIN_ID
)
SELECT
    LOWER(
	    REGEXP_REPLACE(
	        RAWTOHEX(SYS_GUID()),
	        '(.{8})(.{4})(.{4})(.{4})(.{12})',
	        '\1-\2-\3-\4-\5'
	    )
	) AS ID, B.ID AS MENU_DETAIL_ID, C.NAME, C.ICON, C.STYLE, C.LOGIC,
    b.DOMAIN_ID AS DOMAIN_ID
FROM
    (SELECT * FROM MENU_DETAILS WHERE domain_id = :sourceDomainId) a,
    (SELECT * FROM MENU_DETAILS WHERE domain_id = :targetDomainId) b,
    (SELECT * FROM MENU_DETAIL_BUTTONS WHERE domain_id = :sourceDomainId) C
WHERE
    a.name = b.name AND a.id = c.MENU_DETAIL_ID