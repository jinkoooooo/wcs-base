package operato.logis.inventory.service;

import lombok.RequiredArgsConstructor;
import operato.logis.inventory.consts.InventoryConstants;
import operato.logis.inventory.consts.RetrevalPlanTaskType;
import operato.logis.inventory.dto.RelocationTaskDto;
import operato.logis.inventory.entity.TbInventoryLocation;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MultiDeepSortService extends AbstractQueryService {

    private final InventoryInboundLocationService inventoryInboundLocationService;
    private final InventoryLocationService inventoryLocationService;
    private final InventorySettingService inventorySettingService;

    /**
     * Target 로케이션에 접근하기 위한 경로에 존재하는 장애물을 재정렬하는 작업 목록 반환
     * 
     * @param targetLocCode 접근할 로케이션 코드
     * @return 재정렬 작업 목록 (stepOrder 오름차순으로 실행해야하며 taskType이 WAIT인 경우 작업을 진행하지 않고 대기해야 합니다.)
     */
    public List<RelocationTaskDto> createRetrievalPlan(String targetLocCode) {
        boolean isRowStandard = InventoryConstants.ROW_DIRECTION.equals(inventorySettingService.getOptionValue(InventoryConstants.PATH_STANDARD));
        TbInventoryLocation targetLoc = inventoryLocationService.findByLocCode(targetLocCode);
        List<RelocationTaskDto> planList = new ArrayList<>();
        Set<String> lockedLocations = new HashSet<>(); // 순환 참조 및 중복 예약 방지용

        // 1. 타겟 로케이션 앞을 막고 있는 장애물 조회 (통로쪽부터 순서대로)
        String wayInfo = inventorySettingService.getOptionValue(InventoryConstants.SHUTTLE_WAY_INFO);
        List<TbInventoryLocation> blockers;
        if (InventoryConstants.OPTION_VALUE_TRUE.equals(wayInfo)) {
            blockers = findBlockers4Way(targetLoc, isRowStandard);
        } else {
            blockers = findBlockers2Way(targetLoc, isRowStandard);
        }

        // 2. 출고 경로에 작업이 진행 중이면 대기
        for (TbInventoryLocation blocker : blockers) {
            if (ValueUtil.isNotEmpty(blocker.getTaskId())) {
                planList.add(RelocationTaskDto.builder()
                        .taskType(RetrevalPlanTaskType.WAIT.value())
                        .fromLocCode(blocker.getLocCode())  // ← 이 한 줄 추가
                        .build());

                return planList;
            }
        }

        // 3. 통로 쪽에 가장 가까운 장애물부터 해결
        for (TbInventoryLocation blocker : blockers) {
            resolveBlocker(blocker, planList, lockedLocations);
        }

        return planList;
    }

    /**
     * blocker에 있는 재고를 다른 로케이션에 재배치하는 계획 생성
     */
    private void resolveBlocker(TbInventoryLocation blocker, List<RelocationTaskDto> planList, Set<String> lockedLocations) {
        // 최적의 빈 로케이션 탐색
        TbInventoryLocation destLoc = inventoryInboundLocationService.findOptimalEmptyLocation(blocker, lockedLocations);
        if (ValueUtil.isEmpty(destLoc)) {
            throw new RuntimeException("재배치를 위한 가용 공간이 없습니다.");
        }

        // 목적지를 예약 상태로 등록 (다른 로직에서 사용 못하게)
        lockedLocations.add(destLoc.getLocCode());

        // 목적지가 확보되었으므로 현재 블로커 이동 계획 추가
        planList.add(RelocationTaskDto.builder()
                .stepOrder(planList.size() + 1)
                .stockId(blocker.getStockId())
                .fromLocCode(blocker.getLocCode())
                .toLocCode(destLoc.getLocCode())
                .taskType(RetrevalPlanTaskType.RELOCATION.value())
                .build());
    }

    /**
     * Target 로케이션부터 중앙 통로까지 장애물 목록 반환
     */
    private List<TbInventoryLocation> findBlockers2Way(TbInventoryLocation targetLoc, boolean isRowStandard) {
        // 설정에 따라 동일 터널(라인)을 묶는 기준 축 결정
        String fixedAxis = isRowStandard ? "loc_col" : "loc_row";
        int fixedValue = isRowStandard ? targetLoc.getLocCol() : targetLoc.getLocRow();

        String sql = String.format("""
                SELECT *
                FROM tb_inventory_location
                WHERE (
                    (stock_id IS NOT NULL AND stock_id != '') OR
                    (task_id IS NOT NULL AND task_id != '')
                  )
                  AND loc_group = :locGroup
                  AND loc_side = :locSide
                  AND loc_level = :locLevel
                  AND %s = :fixedValue
                  AND loc_deep < :locDeep
                ORDER BY loc_deep ASC
                """, fixedAxis);

        Map<String, Object> param = ValueUtil.newMap(
                "locGroup,locSide,locLevel,fixedValue,locDeep",
                targetLoc.getLocGroup(),
                targetLoc.getLocSide(),
                targetLoc.getLocLevel(),
                fixedValue,
                targetLoc.getLocDeep()
        );

        return this.queryManager.selectListBySql(sql, param, TbInventoryLocation.class, 0, 0);
    }

    /**
     * Target 로케이션의 설정된 축(Col/Row) 양방향 경로를 검사하여 장애물이 적은 쪽의 장애물 목록 반환
     */
    private List<TbInventoryLocation> findBlockers4Way(TbInventoryLocation targetLoc, boolean isRowStandard) {
        // 1. 탐색 축과 고정 축 동적 결정
        String fixedAxis = isRowStandard ? "loc_col" : "loc_row";
        String searchAxis = isRowStandard ? "loc_row" : "loc_col";

        int fixedValue = isRowStandard ? targetLoc.getLocCol() : targetLoc.getLocRow();
        int searchTargetValue = isRowStandard ? targetLoc.getLocRow() : targetLoc.getLocCol();

        String sql = String.format("""
                -- 양(+) 방향에 설비 통로가 있는지 조회 (단, 타겟과 통로 사이에 기둥이 있으면 경로 없음 처리)
                WITH PathPos AS (
                    SELECT P.%2$s FROM TB_INVENTORY_LOCATION P
                    WHERE P.LOC_GROUP = :locGroup AND P.LOC_LEVEL = :locLevel
                      AND P.%1$s = :fixedValue AND P.%2$s > :searchTargetValue AND P.IS_PATH = TRUE
                      AND NOT EXISTS (
                          SELECT 1 FROM TB_INVENTORY_LOCATION OBS
                          WHERE OBS.LOC_GROUP = P.LOC_GROUP AND OBS.LOC_LEVEL = P.LOC_LEVEL AND OBS.LOC_SIDE = :locSide
                            AND OBS.%1$s = :fixedValue AND OBS.%2$s > :searchTargetValue AND OBS.%2$s < P.%2$s
                            AND OBS.LOC_TYPE = 'Pillar'
                      )
                    ORDER BY P.%2$s ASC LIMIT 1
                ),
                -- 음(-) 방향에 설비 통로가 있는지 조회 (단, 타겟과 통로 사이에 기둥이 있으면 경로 없음 처리)
                PathNeg AS (
                    SELECT P.%2$s FROM TB_INVENTORY_LOCATION P
                    WHERE P.LOC_GROUP = :locGroup AND P.LOC_LEVEL = :locLevel
                      AND P.%1$s = :fixedValue AND P.%2$s < :searchTargetValue AND P.IS_PATH = TRUE
                      AND NOT EXISTS (
                          SELECT 1 FROM TB_INVENTORY_LOCATION OBS
                          WHERE OBS.LOC_GROUP = P.LOC_GROUP AND OBS.LOC_LEVEL = P.LOC_LEVEL AND OBS.LOC_SIDE = :locSide
                            AND OBS.%1$s = :fixedValue AND OBS.%2$s < :searchTargetValue AND OBS.%2$s > P.%2$s
                            AND OBS.LOC_TYPE = 'Pillar'
                      )
                    ORDER BY P.%2$s DESC LIMIT 1
                ),
                -- 양(+) 방향 경로에 재고와 작업이 있는지 조회
                BlockersPos AS (
                    SELECT T.* FROM TB_INVENTORY_LOCATION T, PathPos P
                    WHERE T.LOC_GROUP = :locGroup AND T.LOC_LEVEL = :locLevel AND T.LOC_SIDE = :locSide
                      AND T.%1$s = :fixedValue AND T.%2$s > :searchTargetValue AND T.%2$s < P.%2$s
                      AND ((T.STOCK_ID IS NOT NULL AND T.STOCK_ID != '') OR (T.TASK_ID IS NOT NULL AND T.TASK_ID != ''))
                ),
                -- 음(-) 방향 경로에 재고와 작업이 있는지 조회
                BlockersNeg AS (
                    SELECT T.* FROM TB_INVENTORY_LOCATION T, PathNeg P
                    WHERE T.LOC_GROUP = :locGroup AND T.LOC_LEVEL = :locLevel AND T.LOC_SIDE = :locSide
                      AND T.%1$s = :fixedValue AND T.%2$s < :searchTargetValue AND T.%2$s > P.%2$s
                      AND ((T.STOCK_ID IS NOT NULL AND T.STOCK_ID != '') OR (T.TASK_ID IS NOT NULL AND T.TASK_ID != ''))
                ),
                Stats AS (
                    SELECT
                        (SELECT COUNT(*) FROM PathPos) AS p_path,
                        (SELECT COUNT(*) FROM PathNeg) AS n_path,
                        (SELECT COUNT(*) FROM BlockersPos) AS p_blk,
                        (SELECT COUNT(*) FROM BlockersNeg) AS n_blk
                )
                SELECT * FROM (
                    -- 1. 양(+) 방향 경로 조건 (장애물이 더 적거나 같을 때)
                    SELECT * FROM BlockersPos
                    WHERE (SELECT p_path FROM Stats) = 1
                      AND (
                          (SELECT n_path FROM Stats) = 0
                          OR (SELECT p_blk FROM Stats) <= (SELECT n_blk FROM Stats)
                      )
                    UNION ALL
                    -- 2. 음(-) 방향 경로 조건 (장애물이 더 적을 때)
                    SELECT * FROM BlockersNeg
                    WHERE (SELECT n_path FROM Stats) = 1
                      AND (
                          (SELECT p_path FROM Stats) = 0
                          OR (SELECT n_blk FROM Stats) < (SELECT p_blk FROM Stats)
                      )
                ) AS FinalResult
                ORDER BY ABS(%2$s - :searchTargetValue) DESC
                """, fixedAxis, searchAxis);

        Map<String, Object> param = ValueUtil.newMap(
                "locGroup,locLevel,locSide,fixedValue,searchTargetValue",
                targetLoc.getLocGroup(), targetLoc.getLocLevel(), targetLoc.getLocSide(), fixedValue, searchTargetValue
        );

        return this.queryManager.selectListBySql(sql, param, TbInventoryLocation.class, 0, 0);
    }
}