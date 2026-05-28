WITH SYS_CONFIG AS (
    -- 1. 환경 설정값을 조회하여 단일 행으로 구성
    SELECT
        COALESCE(MAX(CASE WHEN OPTION_NAME = 'EQUIP_STATUS' THEN OPTION_VALUE END) = 'true', false) AS is_equip_check_on,
        COALESCE(MAX(CASE WHEN OPTION_NAME = 'PATH_CLEARANCE_CHECK' THEN OPTION_VALUE END) = 'true', false) AS is_path_check_on,
        COALESCE(MAX(CASE WHEN OPTION_NAME = 'SKU_CONCENTRATED' THEN OPTION_VALUE END) = 'true', false) AS is_sku_concentrated,
        COALESCE(MAX(CASE WHEN OPTION_NAME = 'PRIORITIZE_IDLE_EQUIPMENT' THEN OPTION_VALUE END) = 'true', false) AS is_prioritize_idle_equipment,
        COALESCE(MAX(CASE WHEN OPTION_NAME = '4_WAY_SHUTTLE' THEN OPTION_VALUE END) = 'true', false) AS is_4_way_shuttle,
        COALESCE(MAX(CASE WHEN OPTION_NAME = 'DISTANCE_STANDARD' THEN OPTION_VALUE END), 'EUCLID') AS distance_standard,
        COALESCE(MAX(CASE WHEN OPTION_NAME = 'PATH_STANDARD' THEN OPTION_VALUE END), 'COLUMN') AS path_standard
    FROM TB_INVENTORY_SETTING
    WHERE OPTION_NAME IN (
        'EQUIP_STATUS',
        'PATH_CLEARANCE_CHECK',
        'SKU_CONCENTRATED',
        'PRIORITIZE_IDLE_EQUIPMENT',
        '4_WAY_SHUTTLE',
        'DISTANCE_STANDARD',
        'PATH_STANDARD'
    )
),
PATH_STATUS AS (
    -- 2. 경로 상태 확인용 Function
    -- 목적지까지 경로 중 재고나 작업이 있는 Cell이 있는지 검사
    SELECT
        LOC_CODE,
        SUM(
            CASE
                WHEN TASK_ID IS NOT NULL AND TASK_ID <> '' THEN 1
                ELSE 0
            END
        ) OVER (
            PARTITION BY
                LOC_GROUP,
                LOC_LEVEL,
                LOC_SIDE,
                -- PATH_STANDARD 설정에 따라 고정되는 축을 동적으로 변경
                CASE
                    WHEN (SELECT path_standard FROM SYS_CONFIG) = 'ROW' THEN LOC_COL
                    ELSE LOC_ROW
                END
        ) AS BLOCKING_CNT
    FROM TB_INVENTORY_LOCATION
    -- is_path_check_on이 false라면 연산을 수행하지 않음
    WHERE (SELECT is_path_check_on FROM SYS_CONFIG) = true
      AND (SELECT is_4_way_shuttle FROM SYS_CONFIG) = false
),
EQUIP_TASK_STATUS AS (
    -- 3. 장비별 현재 할당된 작업 수 계산
    SELECT
        EQUIP_TYPE,
        EQUIP_CODE,
        SUM(
            CASE
                WHEN TASK_ID IS NOT NULL AND TASK_ID <> '' THEN 1
                ELSE 0
            END
        ) AS ACTIVE_TASK_CNT
    FROM TB_INVENTORY_LOCATION
    -- is_prioritize_idle_equipment이 false라면 연산을 수행하지 않음
    WHERE (SELECT is_prioritize_idle_equipment FROM SYS_CONFIG) = true
    GROUP BY EQUIP_TYPE, EQUIP_CODE
),
AVAILABLE_CANDIDATES AS (
    -- 4. 전체 조건 적용
    SELECT
        L.LOC_CODE,
        COUNT(*) OVER (PARTITION BY L.LOC_GROUP) AS GROUP_EMPTY_CNT
    FROM TB_INVENTORY_LOCATION L
    CROSS JOIN SYS_CONFIG S
    WHERE L.IS_INBOUND_ENABLED = TRUE                                         -- 입고 허용 Cell
      AND L.IS_ENABLED = TRUE                                                 -- 사용 가능한 Cell
      AND L.IS_PATH = FALSE                                                   -- 경로가 아닌 로케이션
      AND L.LOC_TYPE != 'Pillar'                                              -- 기둥이 아닌 로케이션
      AND (L.STOCK_ID IS NULL OR L.STOCK_ID = '')                             -- 재고가 없는 Cell
      AND (L.TASK_ID IS NULL OR L.TASK_ID = '')                               -- 작업이 없는 Cell
      AND (
        :locGroup::varchar IS NULL OR :locGroup::varchar = ''
        OR L.LOC_GROUP = :locGroup::varchar                                   -- 특정 로케이션 Group (선택)
      )
      AND L.ITEM_TYPE = :itemType                                             -- 품목 유형 적용(Pallet, Bucket, ...)
      AND L.MAX_HEIGHT >= :stockHeight                                        -- 최대 높이 제한
      AND L.MAX_WEIGHT >= :stockWeight                                        -- 최대 무게 제한
      AND L.ITEM_GROUP = ANY(string_to_array(:dedicatedGroupList::text, ',')) -- 전용 로케이션 적용
      AND (
        :forbiddenGroupList::text = ''
        OR
        L.ITEM_GROUP != ALL(string_to_array(:forbiddenGroupList::text, ','))  -- 금지 로케이션 적용
      )
      AND (
        :reservedLocCodeList::text = ''
            OR
        L.LOC_CODE != ALL(string_to_array(:reservedLocCodeList::text, ','))  -- 예약 로케이션 적용
      )
      AND (
        S.is_equip_check_on = false                                           -- Error 설비 구역 제외
        OR EXISTS (
            SELECT 1
            FROM TB_INVENTORY_EQUIPMENT E
            WHERE E.EQUIP_TYPE = L.EQUIP_TYPE
              AND E.EQUIP_CODE = L.EQUIP_CODE
              AND E.EQUIP_STATUS != 'ERROR'
        )
      )
      AND (
        S.is_path_check_on = false                                             -- 경로 비어있음 검사 분기 로직 (1-Way vs 4-Way)
        OR (
            -- s_4_way_shuttle = false 일 때: LOC_DEEP 기준 앞쪽 장애물 검사
            S.is_4_way_shuttle = false
            AND EXISTS (
                SELECT 1
                FROM PATH_STATUS P
                WHERE P.LOC_CODE = L.LOC_CODE
                  AND P.BLOCKING_CNT = 0
            )
        )
        OR (
            -- is_4_way_shuttle = true 일 때: PATH_STANDARD 설정에 따라 축(Axis) 탐색 분기
            S.is_4_way_shuttle = true
            AND (
                -- [COLUMN 기준 탐색] path_standard가 'COLUMN'일 때만 양옆(Col)으로 통로 확인
                (
                    S.path_standard = 'COLUMN'
                    AND (
                        -- 방향 1: LOC_COL 양(+)의 방향으로 탐색
                        EXISTS (
                            SELECT 1 FROM TB_INVENTORY_LOCATION P
                            WHERE P.LOC_GROUP = L.LOC_GROUP AND P.LOC_LEVEL = L.LOC_LEVEL
                              AND P.LOC_ROW = L.LOC_ROW AND P.LOC_COL > L.LOC_COL AND P.IS_PATH = TRUE
                              AND NOT EXISTS (
                                SELECT 1 FROM TB_INVENTORY_LOCATION OBS
                                WHERE OBS.LOC_GROUP = L.LOC_GROUP AND OBS.LOC_LEVEL = L.LOC_LEVEL AND OBS.LOC_SIDE = L.LOC_SIDE
                                  AND OBS.LOC_ROW = L.LOC_ROW AND OBS.LOC_COL > L.LOC_COL AND OBS.LOC_COL < P.LOC_COL
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
                            WHERE P.LOC_GROUP = L.LOC_GROUP AND P.LOC_LEVEL = L.LOC_LEVEL
                              AND P.LOC_ROW = L.LOC_ROW AND P.LOC_COL < L.LOC_COL AND P.IS_PATH = TRUE
                              AND NOT EXISTS (
                                SELECT 1 FROM TB_INVENTORY_LOCATION OBS
                                WHERE OBS.LOC_GROUP = L.LOC_GROUP AND OBS.LOC_LEVEL = L.LOC_LEVEL AND OBS.LOC_SIDE = L.LOC_SIDE
                                  AND OBS.LOC_ROW = L.LOC_ROW AND OBS.LOC_COL < L.LOC_COL AND OBS.LOC_COL > P.LOC_COL
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
                            WHERE P.LOC_GROUP = L.LOC_GROUP AND P.LOC_LEVEL = L.LOC_LEVEL
                              AND P.LOC_COL = L.LOC_COL AND P.LOC_ROW > L.LOC_ROW AND P.IS_PATH = TRUE
                              AND NOT EXISTS (
                                SELECT 1 FROM TB_INVENTORY_LOCATION OBS
                                WHERE OBS.LOC_GROUP = L.LOC_GROUP AND OBS.LOC_LEVEL = L.LOC_LEVEL AND OBS.LOC_SIDE = L.LOC_SIDE
                                  AND OBS.LOC_COL = L.LOC_COL AND OBS.LOC_ROW > L.LOC_ROW AND OBS.LOC_ROW < P.LOC_ROW
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
                            WHERE P.LOC_GROUP = L.LOC_GROUP AND P.LOC_LEVEL = L.LOC_LEVEL
                              AND P.LOC_COL = L.LOC_COL AND P.LOC_ROW < L.LOC_ROW AND P.IS_PATH = TRUE
                              AND NOT EXISTS (
                                SELECT 1 FROM TB_INVENTORY_LOCATION OBS
                                WHERE OBS.LOC_GROUP = L.LOC_GROUP AND OBS.LOC_LEVEL = L.LOC_LEVEL AND OBS.LOC_SIDE = L.LOC_SIDE
                                  AND OBS.LOC_COL = L.LOC_COL AND OBS.LOC_ROW < L.LOC_ROW AND OBS.LOC_ROW > P.LOC_ROW
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
        )
      )
      -- L에 짐을 넣었을 때, 다른 예정된 작업(W)의 '유일한 퇴로'를 막는지 검사 (Deadlock Prevention)
      AND (
          S.is_path_check_on = false
          OR S.is_4_way_shuttle = false -- 1-Way는 앞서 PATH_STATUS로 방어되므로 패스
          OR NOT EXISTS (
              SELECT 1
              FROM TB_INVENTORY_LOCATION W
              WHERE W.LOC_GROUP = L.LOC_GROUP AND W.LOC_LEVEL = L.LOC_LEVEL AND W.LOC_SIDE = L.LOC_SIDE
                AND W.LOC_CODE != L.LOC_CODE
                AND (W.TASK_ID IS NOT NULL AND W.TASK_ID != '') -- 다른 입출고 작업이 걸려있는 셀
                AND (
                    (
                        -- [COLUMN 기준] 통로가 양옆(Col)에 있는 경우: 같은 Row에 있는 작업끼리 간섭
                        S.path_standard = 'COLUMN'
                        AND W.LOC_ROW = L.LOC_ROW
                        AND (
                            -- Case 1: L이 W의 우측(Col > W)을 막는 경우 -> W의 좌측(Col < W)이 막혀있는지 확인 (막혀있다면 L 기각)
                            (L.LOC_COL > W.LOC_COL AND NOT EXISTS (
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
                            -- Case 2: L이 W의 좌측(Col < W)을 막는 경우 -> W의 우측(Col > W)이 막혀있는지 확인 (막혀있다면 L 기각)
                            (L.LOC_COL < W.LOC_COL AND NOT EXISTS (
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
                    (
                        -- [ROW 기준] 통로가 위아래(Row)에 있는 경우: 같은 Col에 있는 작업끼리 간섭
                        S.path_standard = 'ROW'
                        AND W.LOC_COL = L.LOC_COL
                        AND (
                            -- Case 3: L이 W의 위쪽(Row > W)을 막는 경우 -> W의 아래쪽(Row < W)이 막혀있는지 확인 (막혀있다면 L 기각)
                            (L.LOC_ROW > W.LOC_ROW AND NOT EXISTS (
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
                            -- Case 4: L이 W의 아래쪽(Row < W)을 막는 경우 -> W의 위쪽(Row > W)이 막혀있는지 확인 (막혀있다면 L 기각)
                            (L.LOC_ROW < W.LOC_ROW AND NOT EXISTS (
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
      )
)

SELECT L.*
FROM TB_INVENTORY_LOCATION L
-- 사용 가능한 Cell 목록
JOIN AVAILABLE_CANDIDATES A
  ON L.LOC_CODE = A.LOC_CODE
-- 출고대까지의 거리 계산
JOIN TB_INVENTORY_LOCATION D
  ON L.DEST_NODE_CODE = D.LOC_CODE
-- 환경 설정값
CROSS JOIN SYS_CONFIG S
-- 동일 SKU 집약 관리
LEFT JOIN TB_INVENTORY_LOCATION C
  ON :centerLocation::varchar IS NOT NULL
 AND :centerLocation::varchar <> ''
 AND C.LOC_CODE = :centerLocation::varchar
-- 장비별 할당된 작업 수
LEFT JOIN EQUIP_TASK_STATUS ET
  ON L.EQUIP_TYPE = ET.EQUIP_TYPE
  AND L.EQUIP_CODE = ET.EQUIP_CODE
ORDER BY
    -- [1차 정렬] 전용 구역 중 우선순위 적용
    array_position(string_to_array(:dedicatedGroupList::text, ','), L.ITEM_GROUP) ASC,
    -- [2차 정렬] 입력받은 등급 이하의 등급 우선 사용
    CASE
        WHEN L.ITEM_GRADE >= :itemGrade THEN 0
        ELSE 1
    END ASC,
    -- [3차 정렬] 입력받은 등급 이상은 오름차순(+), 미만은 부호를 반전(-)시켜 내림차순 효과 적용
    CASE
        WHEN L.ITEM_GRADE >= :itemGrade THEN L.ITEM_GRADE
        ELSE -L.ITEM_GRADE
    END ASC,
    -- [4차 정렬] 유휴 장비 우선 할당
    CASE
        WHEN S.is_prioritize_idle_equipment = true THEN COALESCE(ET.ACTIVE_TASK_CNT, 0)
        ELSE 0
    END ASC,
    -- [5차 정렬] 양옆(LOC_COL +- 1)에 재고가 있는 '접촉 셀' 최우선 할당
    CASE
        WHEN S.is_4_way_shuttle = true THEN (
            SELECT COUNT(1)
            FROM TB_INVENTORY_LOCATION ADJ
            WHERE ADJ.LOC_GROUP = L.LOC_GROUP
              AND ADJ.LOC_LEVEL = L.LOC_LEVEL
              AND ADJ.LOC_SIDE = L.LOC_SIDE
              -- 설정에 따라 Row를 고정할지 Col을 고정할지 동적으로 스위칭
              AND (
                  (S.path_standard = 'COLUMN' AND ADJ.LOC_ROW = L.LOC_ROW AND ABS(ADJ.LOC_COL - L.LOC_COL) = 1)
                  OR
                  (S.path_standard = 'ROW' AND ADJ.LOC_COL = L.LOC_COL AND ABS(ADJ.LOC_ROW - L.LOC_ROW) = 1)
              )
              AND (
                ADJ.LOC_TYPE = 'Pillar' OR
                (ADJ.STOCK_ID IS NOT NULL AND ADJ.STOCK_ID != '') OR
                (ADJ.TASK_ID IS NOT NULL AND ADJ.TASK_ID != '')
              )
        )
        ELSE 0
    END DESC,
    -- [6차 정렬] 동일 등급 내에서는 안쪽 Deep 우선
    L.LOC_DEEP DESC,
    -- [7차 정렬] 보관 전략(집중 vs 분산) 및 NULL 안전 처리
    CASE
        -- 집중 보관 설정 활성화 & Center 좌표를 정상적으로 찾은 경우에만 거리 계산
        WHEN S.is_sku_concentrated = true AND (C.LOC_CODE IS NOT NULL AND C.LOC_CODE != '') THEN
            SQRT(
                POWER(L.LOC_COL - C.LOC_COL, 2) +
                POWER(L.LOC_ROW - C.LOC_ROW, 2) +
                POWER(L.LOC_LEVEL - C.LOC_LEVEL, 2)
            )
        -- 설정이 비활성화이거나, Center 좌표를 못 찾은 경우 분산 보관으로 자동 우회
        ELSE
            -(A.GROUP_EMPTY_CNT)
    END ASC,
    -- [8차 정렬] 동일 Deep 내에서는 설정된 기준에 따라 거리가 가까운 순으로 정렬
    CASE S.distance_standard
        -- [맨해튼 거리] 격자 이동 거리
        WHEN 'MANHATTAN' THEN
            ABS(L.LOC_COL - D.LOC_COL) +
            ABS(L.LOC_ROW - D.LOC_ROW) +
            ABS(L.LOC_LEVEL - D.LOC_LEVEL)
        -- [체비쇼프 거리] 가장 오래 걸리는 단일 축 기준
        WHEN 'CHEBYSHEV' THEN
            GREATEST(
                ABS(L.LOC_COL - D.LOC_COL),
                ABS(L.LOC_ROW - D.LOC_ROW),
                ABS(L.LOC_LEVEL - D.LOC_LEVEL)
            )
        -- [유클리드 거리] 직선 거리 (기본값)
        ELSE
            SQRT(
                POWER(L.LOC_COL - D.LOC_COL, 2) +
                POWER(L.LOC_ROW - D.LOC_ROW, 2) +
                POWER(L.LOC_LEVEL - D.LOC_LEVEL, 2)
            )
    END ASC
LIMIT 1
FOR UPDATE OF L SKIP LOCKED