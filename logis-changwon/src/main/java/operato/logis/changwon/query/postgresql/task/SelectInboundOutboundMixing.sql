-- 현재 '진행 중'인(PROCESS_STATUS = 32) 모든 작업(WCS_TASK)과
-- 해당 작업이 점유 중인 'CRANE_NO'를 식별합니다.
WITH IN_PROGRESS_TASK_CRANES AS (
    SELECT
        T.TASK_NO,
        T.ORDER_KIND,
        T.ATTRIBUTE_B,
        S.CRANE_NO
    FROM
        WCS_TASK T
    JOIN
        WCS_STOCK_AUTO S
    ON
        -- 1. 입고(1) 또는 Transfer2는 END_POINT_CD 기준
        (
            (T.ORDER_KIND = '1' OR T.ATTRIBUTE_B = 'transferBetweenEquipment2')
            AND T.END_POINT_CD = S.LOC_CD
        )
        OR
        -- 2. 출고(2) 또는 Transfer1은 START_POINT_CD 기준
        (
            (T.ORDER_KIND = '2' OR T.ATTRIBUTE_B = 'transferBetweenEquipment1')
            AND T.START_POINT_CD = S.LOC_CD
        )
    WHERE
        T.PROCESS_STATUS = 32 -- 3. 진행 중인 작업
)

-- 새로 추가할 작업(:craneNo, :orderKind)과
-- 충돌하는 작업이 IN_PROGRESS_TASK_CRANES에 몇 개 있는지 카운트합니다.
SELECT
    COUNT(T.TASK_NO) AS CONFLICTING_TASK_COUNT
FROM
    IN_PROGRESS_TASK_CRANES T
WHERE
  -- 4. 새로 추가할 작업과 '동일 그룹'의 크레인을 사용하는 작업 중에서 확인
    (
        -- 그룹 1: '201', '203', '205'
        (
            :craneNo IN ('201', '203', '205') AND T.CRANE_NO IN ('201', '203', '205')
        )
        -- 그룹 2: '202', '204', '206'
        OR
        (
            :craneNo IN ('202', '204', '206') AND T.CRANE_NO IN ('202', '204', '206')
        )
        -- 그 외 크레인 (그룹에 속하지 않은 경우, 자신만 비교)
        OR
        (
            :craneNo NOT IN ('201', '203', '205', '202', '204', '206') AND T.CRANE_NO = :craneNo
        )
    )
  AND (
    -- 5. 새 작업이 '입고(1)'일 때, 진행 중인 '출고(2)' 또는 'Transfer1' 작업 확인
    (
        :orderKind = '1'
            AND (T.ORDER_KIND = '2' OR T.ATTRIBUTE_B = 'transferBetweenEquipment1')
    )
    OR
    -- 6. 새 작업이 '출고(2)'일 때, 진행 중인 '입고(1)' 또는 'Transfer1', 'Transfer2' 작업 확인
    (
        :orderKind = '2'
        AND (
            T.ORDER_KIND = '1'
            OR T.ATTRIBUTE_B = 'transferBetweenEquipment1'
            OR T.ATTRIBUTE_B = 'transferBetweenEquipment2'
        )
    )
  )