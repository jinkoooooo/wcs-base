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
  AND (T.STOCK_ID IS NULL OR T.STOCK_ID = '')
  AND (T.TASK_ID IS NULL OR T.TASK_ID = '')
  AND T.MAX_HEIGHT >= :stockHeight
  AND T.MAX_WEIGHT >= :stockWeight
  AND (
    :reservedLocCodes::text = ''
    OR
    T.LOC_CODE != ALL(string_to_array(:reservedLocCodes::text, ','))
  )
  -- 1. 좌/우 중 적어도 한 방향은 장애물 없이 통로(IS_PATH=TRUE)까지 뚫려 있어야 함
  AND (
    -- [COLUMN 기준 탐색] path_standard가 'COLUMN'일 때만 양옆(Col)으로 통로 확인
      (
          S.path_standard = 'COLUMN'
          AND (
              -- 방향 1: LOC_COL 양(+)의 방향으로 탐색
              EXISTS (
                  SELECT 1 FROM TB_INVENTORY_LOCATION P
                  WHERE P.LOC_GROUP = T.LOC_GROUP AND P.LOC_LEVEL = T.LOC_LEVEL
                    AND P.LOC_ROW = T.LOC_ROW AND P.LOC_COL > T.LOC_COL AND P.IS_PATH = TRUE
                    AND NOT EXISTS (
                      SELECT 1 FROM TB_INVENTORY_LOCATION OBS
                      WHERE OBS.LOC_GROUP = T.LOC_GROUP AND OBS.LOC_LEVEL = T.LOC_LEVEL AND OBS.LOC_SIDE = T.LOC_SIDE
                        AND OBS.LOC_ROW = T.LOC_ROW AND OBS.LOC_COL > T.LOC_COL AND OBS.LOC_COL < P.LOC_COL
                        AND (
                          OBS.LOC_TYPE = 'Pillar' OR
                          (OBS.STOCK_ID IS NOT NULL AND OBS.STOCK_ID <> '') OR
                          (OBS.TASK_ID IS NOT NULL AND OBS.TASK_ID <> '')
                        )
                    )
              )
              OR
              -- 방향 2: LOC_COL 음(-)의 방향으로 탐색
              EXISTS (
                  SELECT 1 FROM TB_INVENTORY_LOCATION P
                  WHERE P.LOC_GROUP = T.LOC_GROUP AND P.LOC_LEVEL = T.LOC_LEVEL
                    AND P.LOC_ROW = T.LOC_ROW AND P.LOC_COL < T.LOC_COL AND P.IS_PATH = TRUE
                    AND NOT EXISTS (
                      SELECT 1 FROM TB_INVENTORY_LOCATION OBS
                      WHERE OBS.LOC_GROUP = T.LOC_GROUP AND OBS.LOC_LEVEL = T.LOC_LEVEL AND OBS.LOC_SIDE = T.LOC_SIDE
                        AND OBS.LOC_ROW = T.LOC_ROW AND OBS.LOC_COL < T.LOC_COL AND OBS.LOC_COL > P.LOC_COL
                        AND (
                          OBS.LOC_TYPE = 'Pillar' OR
                          (OBS.STOCK_ID IS NOT NULL AND OBS.STOCK_ID <> '') OR
                          (OBS.TASK_ID IS NOT NULL AND OBS.TASK_ID <> '')
                        )
                    )
              )
          )
      )
      OR
      -- [ROW 기준 탐색] path_standard가 'ROW'일 때만 상하(Row)로 통로 확인
      (
          S.path_standard = 'ROW'
          AND (
              -- 방향 3: LOC_ROW 양(+)의 방향으로 탐색
              EXISTS (
                  SELECT 1 FROM TB_INVENTORY_LOCATION P
                  WHERE P.LOC_GROUP = T.LOC_GROUP AND P.LOC_LEVEL = T.LOC_LEVEL
                    AND P.LOC_COL = T.LOC_COL AND P.LOC_ROW > T.LOC_ROW AND P.IS_PATH = TRUE
                    AND NOT EXISTS (
                      SELECT 1 FROM TB_INVENTORY_LOCATION OBS
                      WHERE OBS.LOC_GROUP = T.LOC_GROUP AND OBS.LOC_LEVEL = T.LOC_LEVEL AND OBS.LOC_SIDE = T.LOC_SIDE
                        AND OBS.LOC_COL = T.LOC_COL AND OBS.LOC_ROW > T.LOC_ROW AND OBS.LOC_ROW < P.LOC_ROW
                        AND (
                          OBS.LOC_TYPE = 'Pillar' OR
                          (OBS.STOCK_ID IS NOT NULL AND OBS.STOCK_ID <> '') OR
                          (OBS.TASK_ID IS NOT NULL AND OBS.TASK_ID <> '')
                        )
                    )
              )
              OR
              -- 방향 4: LOC_ROW 음(-)의 방향으로 탐색
              EXISTS (
                  SELECT 1 FROM TB_INVENTORY_LOCATION P
                  WHERE P.LOC_GROUP = T.LOC_GROUP AND P.LOC_LEVEL = T.LOC_LEVEL
                    AND P.LOC_COL = T.LOC_COL AND P.LOC_ROW < T.LOC_ROW AND P.IS_PATH = TRUE
                    AND NOT EXISTS (
                      SELECT 1 FROM TB_INVENTORY_LOCATION OBS
                      WHERE OBS.LOC_GROUP = T.LOC_GROUP AND OBS.LOC_LEVEL = T.LOC_LEVEL AND OBS.LOC_SIDE = T.LOC_SIDE
                        AND OBS.LOC_COL = T.LOC_COL AND OBS.LOC_ROW < T.LOC_ROW AND OBS.LOC_ROW > P.LOC_ROW
                        AND (
                          OBS.LOC_TYPE = 'Pillar' OR
                          (OBS.STOCK_ID IS NOT NULL AND OBS.STOCK_ID <> '') OR
                          (OBS.TASK_ID IS NOT NULL AND OBS.TASK_ID <> '')
                        )
                    )
              )
          )
      )
  )
  -- 2. T에 짐을 넣었을 때, 다른 예정된 작업(W)의 '유일한 퇴로'를 막는지 검사
  AND NOT EXISTS (
    SELECT 1
    FROM TB_INVENTORY_LOCATION W
    WHERE W.LOC_GROUP = T.LOC_GROUP AND W.LOC_LEVEL = T.LOC_LEVEL AND W.LOC_SIDE = T.LOC_SIDE
      AND W.LOC_CODE != T.LOC_CODE
      AND (W.TASK_ID IS NOT NULL AND W.TASK_ID != '') -- 다른 입출고 작업이 걸려있는 셀
      AND (
          -- [COLUMN 기준] 통로가 양옆(Col)에 있는 경우: 같은 Row에 있는 작업끼리 간섭
          (
              S.path_standard = 'COLUMN'
              AND W.LOC_ROW = T.LOC_ROW
              AND (
                  -- Case 1: T가 W의 우측(Col > W)을 막는 경우 -> W의 좌측(Col < W)이 막혀있는지 확인 (막혀있다면 T 기각)
                  (T.LOC_COL > W.LOC_COL AND NOT EXISTS (
                      SELECT 1 FROM TB_INVENTORY_LOCATION P_L
                      WHERE P_L.LOC_GROUP = W.LOC_GROUP AND P_L.LOC_LEVEL = W.LOC_LEVEL
                        AND P_L.LOC_ROW = W.LOC_ROW AND P_L.LOC_COL < W.LOC_COL AND P_L.IS_PATH = TRUE
                        AND NOT EXISTS (
                            SELECT 1 FROM TB_INVENTORY_LOCATION OBS_L
                            WHERE OBS_L.LOC_GROUP = W.LOC_GROUP AND OBS_L.LOC_LEVEL = W.LOC_LEVEL AND OBS_L.LOC_SIDE = W.LOC_SIDE
                              AND OBS_L.LOC_ROW = W.LOC_ROW AND OBS_L.LOC_COL < W.LOC_COL AND OBS_L.LOC_COL > P_L.LOC_COL
                              AND (
                                OBS_L.LOC_TYPE = 'Pillar' OR
                                (OBS_L.STOCK_ID IS NOT NULL AND OBS_L.STOCK_ID <> '') OR
                                (OBS_L.TASK_ID IS NOT NULL AND OBS_L.TASK_ID <> '')
                              )
                        )
                  ))
                  OR
                  -- Case 2: T가 W의 좌측(Col < W)을 막는 경우 -> W의 우측(Col > W)이 막혀있는지 확인 (막혀있다면 T 기각)
                  (T.LOC_COL < W.LOC_COL AND NOT EXISTS (
                      SELECT 1 FROM TB_INVENTORY_LOCATION P_R
                      WHERE P_R.LOC_GROUP = W.LOC_GROUP AND P_R.LOC_LEVEL = W.LOC_LEVEL
                        AND P_R.LOC_ROW = W.LOC_ROW AND P_R.LOC_COL > W.LOC_COL AND P_R.IS_PATH = TRUE
                        AND NOT EXISTS (
                            SELECT 1 FROM TB_INVENTORY_LOCATION OBS_R
                            WHERE OBS_R.LOC_GROUP = W.LOC_GROUP AND OBS_R.LOC_LEVEL = W.LOC_LEVEL AND OBS_R.LOC_SIDE = W.LOC_SIDE
                              AND OBS_R.LOC_ROW = W.LOC_ROW AND OBS_R.LOC_COL > W.LOC_COL AND OBS_R.LOC_COL < P_R.LOC_COL
                              AND (
                                OBS_R.LOC_TYPE = 'Pillar' OR
                                (OBS_R.STOCK_ID IS NOT NULL AND OBS_R.STOCK_ID <> '') OR
                                (OBS_R.TASK_ID IS NOT NULL AND OBS_R.TASK_ID <> '')
                              )
                        )
                  ))
              )
          )
          OR
          -- [ROW 기준] 통로가 위아래(Row)에 있는 경우: 같은 Col에 있는 작업끼리 간섭
          (
              S.path_standard = 'ROW'
              AND W.LOC_COL = T.LOC_COL
              AND (
                  -- Case 3: T가 W의 위쪽(Row > W)을 막는 경우 -> W의 아래쪽(Row < W)이 막혀있는지 확인 (막혀있다면 T 기각)
                  (T.LOC_ROW > W.LOC_ROW AND NOT EXISTS (
                      SELECT 1 FROM TB_INVENTORY_LOCATION P_D
                      WHERE P_D.LOC_GROUP = W.LOC_GROUP AND P_D.LOC_LEVEL = W.LOC_LEVEL
                        AND P_D.LOC_COL = W.LOC_COL AND P_D.LOC_ROW < W.LOC_ROW AND P_D.IS_PATH = TRUE
                        AND NOT EXISTS (
                            SELECT 1 FROM TB_INVENTORY_LOCATION OBS_D
                            WHERE OBS_D.LOC_GROUP = W.LOC_GROUP AND OBS_D.LOC_LEVEL = W.LOC_LEVEL AND OBS_D.LOC_SIDE = W.LOC_SIDE
                              AND OBS_D.LOC_COL = W.LOC_COL AND OBS_D.LOC_ROW < W.LOC_ROW AND OBS_D.LOC_ROW > P_D.LOC_ROW
                              AND (
                                OBS_D.LOC_TYPE = 'Pillar' OR
                                (OBS_D.STOCK_ID IS NOT NULL AND OBS_D.STOCK_ID <> '') OR
                                (OBS_D.TASK_ID IS NOT NULL AND OBS_D.TASK_ID <> '')
                              )
                        )
                  ))
                  OR
                  -- Case 4: T가 W의 아래쪽(Row < W)을 막는 경우 -> W의 위쪽(Row > W)이 막혀있는지 확인 (막혀있다면 T 기각)
                  (T.LOC_ROW < W.LOC_ROW AND NOT EXISTS (
                      SELECT 1 FROM TB_INVENTORY_LOCATION P_U
                      WHERE P_U.LOC_GROUP = W.LOC_GROUP AND P_U.LOC_LEVEL = W.LOC_LEVEL
                        AND P_U.LOC_COL = W.LOC_COL AND P_U.LOC_ROW > W.LOC_ROW AND P_U.IS_PATH = TRUE
                        AND NOT EXISTS (
                            SELECT 1 FROM TB_INVENTORY_LOCATION OBS_U
                            WHERE OBS_U.LOC_GROUP = W.LOC_GROUP AND OBS_U.LOC_LEVEL = W.LOC_LEVEL AND OBS_U.LOC_SIDE = W.LOC_SIDE
                              AND OBS_U.LOC_COL = W.LOC_COL AND OBS_U.LOC_ROW > W.LOC_ROW AND OBS_U.LOC_ROW < P_U.LOC_ROW
                              AND (
                                OBS_U.LOC_TYPE = 'Pillar' OR
                                (OBS_U.STOCK_ID IS NOT NULL AND OBS_U.STOCK_ID <> '') OR
                                (OBS_U.TASK_ID IS NOT NULL AND OBS_U.TASK_ID <> '')
                              )
                        )
                  ))
              )
          )
      )
)
ORDER BY
    -- Source와 가까운 Row 우선
    ABS(T.LOC_ROW - :locRow) ASC,
    -- 내 바로 양옆(LOC_COL +- 1)에 재고나 작업이 있는 로케이션을 1순위로 선택
    (
        SELECT COUNT(1)
        FROM TB_INVENTORY_LOCATION ADJ
        WHERE ADJ.LOC_GROUP = T.LOC_GROUP
          AND ADJ.LOC_LEVEL = T.LOC_LEVEL
          AND ADJ.LOC_SIDE = T.LOC_SIDE
          AND ADJ.LOC_ROW = T.LOC_ROW
          AND (
            (S.path_standard = 'COLUMN' AND ADJ.LOC_ROW = T.LOC_ROW AND ABS(ADJ.LOC_COL - T.LOC_COL) = 1)
            OR
            (S.path_standard = 'ROW' AND ADJ.LOC_COL = T.LOC_COL AND ABS(ADJ.LOC_ROW - T.LOC_ROW) = 1)
          )
          AND (
            ADJ.LOC_TYPE = 'Pillar' OR
            (ADJ.STOCK_ID IS NOT NULL AND ADJ.STOCK_ID != '') OR
            (ADJ.TASK_ID IS NOT NULL AND ADJ.TASK_ID != '')
          )
    ) DESC,
    -- 동일 Row라면 가장 안쪽(보통 양쪽 통로의 정중앙) 우선 사용
    T.LOC_DEEP DESC
LIMIT 1
FOR UPDATE SKIP LOCKED