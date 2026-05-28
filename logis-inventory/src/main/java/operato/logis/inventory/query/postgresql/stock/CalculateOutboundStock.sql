WITH SYS_CONFIG AS (
    -- 1. 환경 설정값을 조회하여 단일 행으로 구성
    SELECT
        COALESCE(MAX(CASE WHEN OPTION_NAME = 'PATH_STANDARD' THEN OPTION_VALUE END), 'COLUMN') AS path_standard
    FROM TB_INVENTORY_SETTING
    WHERE OPTION_NAME = 'PATH_STANDARD'
),
REQUEST_ITEMS AS (
    -- 2. 입력받은 다중 품목/수량 배열을 테이블 형태로 병합 (Index 순서대로 매핑됨)
    SELECT
        OWNER AS REQ_OWNER,
        CODE AS REQ_CODE,
        QTY AS REQ_QTY
    FROM UNNEST(
        :itemOwnerList::varchar[],
        :itemCodeList::varchar[],
        :itemQtyList::int[]
    ) AS T(OWNER, CODE, QTY)
),
AVAILABLE_CANDIDATES AS (
    -- 3. 조건에 맞는 출고 가능 가용 재고 1차 필터링
    SELECT
        S.*,
        L.LOC_GROUP, L.LOC_LEVEL, L.LOC_SIDE, L.LOC_ROW, L.LOC_COL, L.LOC_DEEP,
        R.REQ_QTY
    FROM TB_INVENTORY_STOCK S
    JOIN TB_INVENTORY_LOCATION L ON S.STOCK_ID = L.STOCK_ID
    JOIN REQUEST_ITEMS R ON S.ITEM_OWNER = R.REQ_OWNER AND S.ITEM_CODE = R.REQ_CODE
    WHERE S.ITEM_QTY > 0
      AND S.IS_ENABLED = TRUE
      AND S.STOCK_STATUS IN (0, 3)
      AND (S.ATTRIBUTE_A IS NULL OR S.ATTRIBUTE_A = '')
      AND (:lotNo::varchar IS NULL OR :lotNo::varchar = '' OR S.LOT_NO = :lotNo::varchar)
      AND L.IS_OUTBOUND_ENABLED = TRUE
      AND L.IS_ENABLED = TRUE
      AND (L.TASK_ID IS NULL OR L.TASK_ID = '')
      AND (:locGroup::varchar IS NULL OR :locGroup::varchar = '' OR L.LOC_GROUP = :locGroup::varchar)
    FOR UPDATE OF S
),
OBSTACLE_CALC AS (
    -- 4. 전략 0 (MIN_MOVEMENT)을 위한 앞단 장애물 계산
    SELECT
        A.ID,
        CASE
            -- 전략이 0이 아닐 때는 연산 비용을 아끼기 위해 0으로 바이패스
            WHEN :outboundCalculateStrategy != 0 THEN 0

            -- [4Way 모드] 양방향 경로 중 장애물이 적은 쪽의 개수를 산출
            WHEN (SELECT OPTION_VALUE FROM TB_INVENTORY_SETTING WHERE OPTION_NAME = '4_WAY_SHUTTLE') = 'true' THEN
                CASE
                    -- [COLUMN 기준] 좌/우(Col) 방향 확인
                    WHEN S.path_standard = 'COLUMN' THEN
                        LEAST(
                            -- 우측(Col 증가) 장애물 수 (경로가 아예 없으면 무한대 999999 처리)
                            COALESCE(
                                (SELECT COUNT(1) FROM TB_INVENTORY_LOCATION OBS
                                WHERE OBS.LOC_GROUP = A.LOC_GROUP AND OBS.LOC_LEVEL = A.LOC_LEVEL AND OBS.LOC_SIDE = A.LOC_SIDE AND OBS.LOC_ROW = A.LOC_ROW
                                  AND OBS.LOC_COL > A.LOC_COL
                                  AND OBS.LOC_COL < (SELECT MIN(P.LOC_COL) FROM TB_INVENTORY_LOCATION P WHERE P.LOC_GROUP = A.LOC_GROUP AND P.LOC_LEVEL = A.LOC_LEVEL AND P.LOC_SIDE = A.LOC_SIDE AND P.LOC_ROW = A.LOC_ROW AND P.LOC_COL > A.LOC_COL AND P.IS_PATH = TRUE)
                                  AND (
                                    OBS.LOC_TYPE = 'Pillar' OR
                                    (OBS.STOCK_ID IS NOT NULL AND OBS.STOCK_ID != '') OR
                                    (OBS.TASK_ID IS NOT NULL AND OBS.TASK_ID != '')
                                  )
                                ), 999999
                            ),
                            -- 좌측(Col 감소) 장애물 수
                            COALESCE(
                                (SELECT COUNT(1) FROM TB_INVENTORY_LOCATION OBS
                                WHERE OBS.LOC_GROUP = A.LOC_GROUP AND OBS.LOC_LEVEL = A.LOC_LEVEL AND OBS.LOC_SIDE = A.LOC_SIDE AND OBS.LOC_ROW = A.LOC_ROW
                                  AND OBS.LOC_COL < A.LOC_COL
                                  AND OBS.LOC_COL > (SELECT MAX(P.LOC_COL) FROM TB_INVENTORY_LOCATION P WHERE P.LOC_GROUP = A.LOC_GROUP AND P.LOC_LEVEL = A.LOC_LEVEL AND P.LOC_SIDE = A.LOC_SIDE AND P.LOC_ROW = A.LOC_ROW AND P.LOC_COL < A.LOC_COL AND P.IS_PATH = TRUE)
                                  AND (
                                    OBS.LOC_TYPE = 'Pillar' OR
                                    (OBS.STOCK_ID IS NOT NULL AND OBS.STOCK_ID != '') OR
                                    (OBS.TASK_ID IS NOT NULL AND OBS.TASK_ID != '')
                                  )
                                ), 999999
                            )
                        )
                    -- [ROW 기준] 상/하(Row) 방향 확인
                    WHEN S.path_standard = 'ROW' THEN
                        LEAST(
                            -- 위쪽(Row 증가) 장애물 수
                            COALESCE(
                                (SELECT COUNT(1) FROM TB_INVENTORY_LOCATION OBS
                                WHERE OBS.LOC_GROUP = A.LOC_GROUP AND OBS.LOC_LEVEL = A.LOC_LEVEL AND OBS.LOC_SIDE = A.LOC_SIDE AND OBS.LOC_COL = A.LOC_COL
                                  AND OBS.LOC_ROW > A.LOC_ROW
                                  AND OBS.LOC_ROW < (SELECT MIN(P.LOC_ROW) FROM TB_INVENTORY_LOCATION P WHERE P.LOC_GROUP = A.LOC_GROUP AND P.LOC_LEVEL = A.LOC_LEVEL AND P.LOC_SIDE = A.LOC_SIDE AND P.LOC_COL = A.LOC_COL AND P.LOC_ROW > A.LOC_ROW AND P.IS_PATH = TRUE)
                                  AND (
                                    OBS.LOC_TYPE = 'Pillar' OR
                                    (OBS.STOCK_ID IS NOT NULL AND OBS.STOCK_ID != '') OR
                                    (OBS.TASK_ID IS NOT NULL AND OBS.TASK_ID != '')
                                  )
                                ), 999999
                            ),
                            -- 아래쪽(Row 감소) 장애물 수
                            COALESCE(
                                (SELECT COUNT(1) FROM TB_INVENTORY_LOCATION OBS
                                WHERE OBS.LOC_GROUP = A.LOC_GROUP AND OBS.LOC_LEVEL = A.LOC_LEVEL AND OBS.LOC_SIDE = A.LOC_SIDE AND OBS.LOC_COL = A.LOC_COL
                                  AND OBS.LOC_ROW < A.LOC_ROW
                                  AND OBS.LOC_ROW > (SELECT MAX(P.LOC_ROW) FROM TB_INVENTORY_LOCATION P WHERE P.LOC_GROUP = A.LOC_GROUP AND P.LOC_LEVEL = A.LOC_LEVEL AND P.LOC_SIDE = A.LOC_SIDE AND P.LOC_COL = A.LOC_COL AND P.LOC_ROW < A.LOC_ROW AND P.IS_PATH = TRUE)
                                  AND (
                                    OBS.LOC_TYPE = 'Pillar' OR
                                    (OBS.STOCK_ID IS NOT NULL AND OBS.STOCK_ID != '') OR
                                    (OBS.TASK_ID IS NOT NULL AND OBS.TASK_ID != '')
                                  )
                                ), 999999
                            )
                        )
                    ELSE 0
                END

            -- [2Way 모드] 중앙 통로(LOC_DEEP 감소) 방향 장애물 개수
            ELSE (
                SELECT COUNT(1) FROM TB_INVENTORY_LOCATION OBS
                WHERE OBS.LOC_GROUP = A.LOC_GROUP AND OBS.LOC_LEVEL = A.LOC_LEVEL AND OBS.LOC_SIDE = A.LOC_SIDE
                -- [2-Way 동적 축 스위칭] 기준 축이 바뀌면 동일 라인을 판단하는 기준도 바뀜
                  AND (
                      (S.path_standard = 'COLUMN' AND OBS.LOC_ROW = A.LOC_ROW)
                      OR
                      (S.path_standard = 'ROW' AND OBS.LOC_COL = A.LOC_COL)
                  )
                  AND OBS.LOC_DEEP < A.LOC_DEEP
                  AND ((OBS.STOCK_ID IS NOT NULL AND OBS.STOCK_ID != '') OR (OBS.TASK_ID IS NOT NULL AND OBS.TASK_ID != ''))
            )
        END AS OBSTACLE_COUNT
        FROM AVAILABLE_CANDIDATES A
        CROSS JOIN SYS_CONFIG S
    )

SELECT
    A.ID,
    A.STOCK_ID,
    A.SKU,
    A.ITEM_OWNER,
    A.ITEM_CODE,
    A.ITEM_QTY,
    A.LOT_NO,
    A.STOCK_STATUS,
    A.IS_ENABLED,
    A.ITEM_PRIORITY,
    A.INB_DATETIME,
    A.EXPIRED_DATETIME,
    A.STOCK_HEIGHT,
    O.OBSTACLE_COUNT::varchar AS ATTRIBUTE_A,
    A.DOMAIN_ID,
    A.CREATOR_ID,
    A.UPDATER_ID,
    A.CREATED_AT,
    A.UPDATED_AT
FROM AVAILABLE_CANDIDATES A
JOIN OBSTACLE_CALC O ON A.ID = O.ID
ORDER BY
    A.STOCK_ID ASC,
    A.ITEM_OWNER ASC,
    A.ITEM_CODE ASC