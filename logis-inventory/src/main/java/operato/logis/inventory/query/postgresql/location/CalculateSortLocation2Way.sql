WITH SYS_CONFIG AS (
    -- 1. 환경 설정값을 조회하여 단일 행으로 구성
    SELECT
        COALESCE(MAX(CASE WHEN OPTION_NAME = 'PATH_STANDARD' THEN OPTION_VALUE END), 'COLUMN') AS path_standard
    FROM TB_INVENTORY_SETTING
    WHERE OPTION_NAME = 'PATH_STANDARD'
)

SELECT T.* FROM TB_INVENTORY_LOCATION T
CROSS JOIN SYS_CONFIG S
WHERE T.LOC_GROUP = :locGroup
  AND T.LOC_LEVEL = :locLevel
  AND T.ITEM_TYPE = :itemType
  AND T.ITEM_GROUP = :itemGroup
  AND T.IS_ENABLED = TRUE
  AND T.IS_PATH = FALSE
  AND T.LOC_TYPE != 'Pillar'
  AND (T.STOCK_ID IS NULL OR T.STOCK_ID = '') -- 재고가 없는 Cell
  AND (T.TASK_ID IS NULL OR T.TASK_ID = '')   -- 작업이 없는 Cell
  AND T.MAX_HEIGHT >= :stockHeight            -- 최대 높이 제한
  AND T.MAX_WEIGHT >= :stockWeight            -- 최대 무게 제한
  AND (
    :reservedLocCodes::text = ''
    OR
    T.LOC_CODE != ALL(string_to_array(:reservedLocCodes::text, ','))
  )
  -- 1. 중앙 통로부터 목적지까지 경로에 재고, 작업이 있는지 검사
  AND NOT EXISTS (
    SELECT 1
    FROM TB_INVENTORY_LOCATION P
    WHERE P.LOC_GROUP = T.LOC_GROUP
      AND P.LOC_SIDE = T.LOC_SIDE
      AND P.LOC_COL = T.LOC_COL
      AND P.LOC_ROW = T.LOC_ROW
      AND P.LOC_LEVEL = T.LOC_LEVEL
      AND P.LOC_DEEP <= T.LOC_DEEP
      AND P.LOC_CODE != T.LOC_CODE
      AND (
        P.LOC_TYPE = 'Pillar' OR
        (P.STOCK_ID IS NOT NULL AND P.STOCK_ID != '') OR
        (P.TASK_ID IS NOT NULL AND P.TASK_ID != '')
      )
  )
  -- 2. 목적지부터 벽쪽까지 작업이 있는지 검사
  AND NOT EXISTS (
    SELECT 1
    FROM TB_INVENTORY_LOCATION W
    WHERE W.LOC_GROUP = T.LOC_GROUP
      AND W.LOC_SIDE = T.LOC_SIDE
      AND W.LOC_COL = T.LOC_COL
      AND W.LOC_ROW = T.LOC_ROW
      AND W.LOC_LEVEL = T.LOC_LEVEL
      AND W.LOC_DEEP > T.LOC_DEEP
      AND (W.TASK_ID IS NOT NULL AND W.TASK_ID != '')
)
ORDER BY
    -- 설정(PATH_STANDARD)에 따라 설비의 주행 축(거리 계산 축)을 동적으로 변경
    CASE
        WHEN S.path_standard = 'COLUMN' THEN ABS(T.LOC_COL - :locCol)
        ELSE ABS(T.LOC_ROW - :locRow)
    END ASC,
    -- 안쪽 Deep 우선 사용
    T.LOC_DEEP DESC
LIMIT 1
FOR UPDATE SKIP LOCKED