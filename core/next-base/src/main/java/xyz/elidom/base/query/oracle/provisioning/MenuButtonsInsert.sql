INSERT INTO MENU_BUTTONS (
    ID, MENU_ID, RANK, STYLE, ICON, TEXT, AUTH, LOGIC, BUTTON_TYPE, DOMAIN_ID
)
SELECT
    LOWER(
	    REGEXP_REPLACE(
	        RAWTOHEX(SYS_GUID()),
	        '(.{8})(.{4})(.{4})(.{4})(.{12})',
	        '\1-\2-\3-\4-\5'
	    )
	) AS ID, B.ID AS MENU_ID, C.RANK, C.STYLE, C.ICON, C.TEXT, C.AUTH, C.LOGIC, C.BUTTON_TYPE, b.DOMAIN_ID AS DOMAIN_ID
FROM
    (SELECT * FROM menus WHERE domain_id = :sourceDomainId) a,
    (SELECT * FROM menus WHERE domain_id = :targetDomainId) b,
    (SELECT * FROM MENU_BUTTONS WHERE domain_id = :sourceDomainId) C
WHERE
    a.name = b.name AND a.id = c.menu_id