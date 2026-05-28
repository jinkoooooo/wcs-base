WITH PREFERRED_CRANES AS (
    /*
     * 1단계: itemOwner가 현재 재고를 보관 중인 구역(크레인)을 찾습니다.
     */
    SELECT DISTINCT
        sa.CRANE_NO
    FROM
        WCS_STOCK_INFO si
            JOIN
        WCS_STOCK_AUTO sa ON si.STOCK_ID = sa.STOCK_ID
    WHERE
        si.ITEM_OWNER = :itemOwner
      AND sa.STOCK_ID IS NOT NULL
      AND sa.STOCK_ID != ''
),
     AVAILABLE_SLOTS_PER_CRANE AS (
         /*
          * 2단계 (일부): 모든 크레인별로 '입고 가능한' 슬롯의 총 개수를 집계합니다.
          */
         SELECT
             CRANE_NO,
             COUNT(*) AS AVAILABLE_COUNT
         FROM
             WCS_STOCK_AUTO
         WHERE
               RACK_LOCKED = 0
           AND RACK_DISABLED = 0
           AND (STOCK_ID IS NULL OR STOCK_ID = '')
           AND (TASK_ID IS NULL OR TASK_ID = '')
           AND CRANE_DISABLED = 0
           AND NOT (
             (CRANE_NO % 2 = 1 AND LOC_SIDE = 'RIGHT' AND LOC_ROW = 1) /* 홀수 크레인 (201, 203, 205) */
                 OR (CRANE_NO % 2 = 0 AND LOC_SIDE = 'LEFT'  AND LOC_ROW = 1) /* 짝수 크레인 (202, 204, 206) */
           )
         GROUP BY
             CRANE_NO
     ),
     RANKED_CRANES AS (
         /*
          * 3단계: 크레인의 우선순위를 매깁니다.
          * 1순위: 같은 화주사의 재고가 있는 크레인 (priority_group = 1)
          * 2순위: 그 외 크레인 (priority_group = 2)
          *
          * 각 그룹 내에서는 입고 가능 슬롯이 많은 순(available_count DESC)으로 정렬합니다.
          */
         SELECT
             av.CRANE_NO,
             av.AVAILABLE_COUNT,
             /* 화주사 크레인(PREFERRED_CRANES)에 포함되어 있으면 1, 아니면 2 */
             CASE
                 WHEN pc.CRANE_NO IS NOT NULL THEN 1
                 ELSE 2
                 END AS PRIORITY_GROUP
         FROM
             AVAILABLE_SLOTS_PER_CRANE av
                 LEFT JOIN
             PREFERRED_CRANES pc ON av.CRANE_NO = pc.CRANE_NO
     ),
     BEST_CRANE AS (
         /*
          * 우선순위(PRIORITY_GROUP)가 가장 높고,
          * 그중 입고 가능 슬롯(AVAILABLE_COUNT)이 가장 많은 크레인을 하나 선택합니다.
          */
         SELECT
             CRANE_NO
         FROM
             RANKED_CRANES
         WHERE
             AVAILABLE_COUNT >= 5 /* 안전 슬롯 */
         ORDER BY
             PRIORITY_GROUP ASC,  /* 1순위(선호) 그룹 먼저 */
             AVAILABLE_COUNT DESC /* 가능한 슬롯 많은 순 */
         LIMIT 1
     )
/*
 * 4단계 (최종 선택):
 * 위에서 선택된 'BEST_CRANE'의 입고 가능 슬롯 중에서
 * 정렬 조건(LOC_DEEP DESC, LOC_ROW ASC)에 맞는 최적의 위치 1개를 반환합니다.
 */
SELECT
    *
FROM
    WCS_STOCK_AUTO sa
        JOIN
    BEST_CRANE bc ON sa.CRANE_NO = bc.CRANE_NO
WHERE
    /* 입고 가능 슬롯 조건을 다시 한번 적용 (JOIN된 크레인 내에서 필터링) */
    sa.RACK_LOCKED = 0
  AND sa.RACK_DISABLED = 0
  AND (sa.STOCK_ID IS NULL OR sa.STOCK_ID = '')
  AND (sa.TASK_ID IS NULL OR sa.TASK_ID = '')
  AND sa.CRANE_DISABLED = 0
  AND NOT (
    (sa.CRANE_NO % 2 = 1 AND sa.LOC_SIDE = 'RIGHT' AND sa.LOC_ROW = 1) /* 홀수 크레인 */
        OR (sa.CRANE_NO % 2 = 0 AND sa.LOC_SIDE = 'LEFT'  AND sa.LOC_ROW = 1) /* 짝수 크레인 */
  )
ORDER BY
    sa.LOC_DEEP DESC,   /* 1. 깊은 곳 우선 */
    sa.LOC_ROW ASC      /* 2. 앞쪽 행 우선 */
LIMIT 1