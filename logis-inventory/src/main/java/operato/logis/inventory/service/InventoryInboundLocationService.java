package operato.logis.inventory.service;

import lombok.RequiredArgsConstructor;
import operato.logis.inventory.consts.InventoryConstants;
import operato.logis.inventory.dto.InboundLocationRequestDto;
import operato.logis.inventory.dto.ItemIdentifierDto;
import operato.logis.inventory.dto.StockMetricsDto;
import operato.logis.inventory.entity.TbInventoryLocation;
import operato.logis.inventory.query.InventoryQueryStore;
import operato.logis.inventory.util.InventoryUtils;
import org.springframework.stereotype.Service;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.*;

@Service
@RequiredArgsConstructor
public class InventoryInboundLocationService extends AbstractQueryService {

    private final InventoryQueryStore inventoryQueryStore;
    private final InventorySettingService inventorySettingService;
    private final InventoryItemGroupService inventoryItemGroupService;
    private final InventoryItemMasterService inventoryItemMasterService;
    private final InventoryStockService inventoryStockService;
    private final InventoryLocationService inventoryLocationService;

    /**
     * 입고 로케이션 산출
     * 각 기능들에 대한 우선순위는 CalculateInboundLocation.sql의 ORDER BY 부분을 참고해 주세요.
     * 고정된 기능 목록
     * - 각 Cell 무게 제한
     * - 각 Cell 높이 제한
     * - 재고 유형에 맞는 위치 조회(Pallet, Bucket, ...)
     * - 품목 등급에 따른 구역 지정
     * - 사용 불가 지역 설정
     * - 각 Cell 재고 / 작업 유무 확인
     * - 특정 로케이션까지의 거리 기준 정렬
     * - MultiDeep 환경에서 안쪽 Deep 우선 사용
     * - 특정 품번 전용 구역 설정
     * - 특정 품번 금지 구역 설정
     * - 동일 품번 집중 보관 or Location Group 기준 부하 방지
     * 설정 가능한 기능 목록
     * - 설비 고장 시 해당 구역 입고 제외
     * - MultiDeep 환경에서 목적지까지 장애물이 없는 Cell 조회
     * - 전용 공간이 없을 경우 공용 공간 사용
     * - 할당된 작업이 적은 설비 우선 사용
     * - 동일 SKU 집약적 관리
     * - 설비 Way 정보 설정
     *
     * @param requestParam 입고하는 재고의 전체 품목 정보 / 품목 유형(Pallet, Bucket, ...) / 합산 무게(kg) / 총 높이(cm) / 특정 로케이션 Group(옵션)
     * @return 최종 입고 로케이션 (조건에 맞는 로케이션이 없는 경우 null) <결과에 대한 ROW는 DB LOCK이 걸립니다.>
     */
    public TbInventoryLocation calculateInboundLocation(InboundLocationRequestDto requestParam) {
        // 0. 입력 파라미터에 대한 유효성 검사
        if (!validateInboundLocationRequest(requestParam)) throw new ElidomRuntimeException("입력 파라미터가 유효하지 않습니다.");
        if (ValueUtil.isEmpty(requestParam.getReservedLocCodeList())) {
            requestParam.setReservedLocCodeList(new ArrayList<>());
        }

        // 1. 특정 품번 전용 그룹 리스트 조회
        List<String> dedicatedGroupList = inventoryItemGroupService.getDedicatedGroupList(requestParam.getItemList());
        if (ValueUtil.isEmpty(dedicatedGroupList)) {
            dedicatedGroupList = new ArrayList<>();
        }

        // 2. Overflow 옵션 활성화 여부 확인 (전용 공간 부족 시 공용 공간 사용)
        boolean isOverflowAllowed = InventoryConstants.OPTION_VALUE_TRUE.equals(
                inventorySettingService.getOptionValue(InventoryConstants.DEDICATED_OVERFLOW)
        );

        // 3. 조건 충족 시 공용 공간(PUBLIC) 추가
        if (dedicatedGroupList.isEmpty() || isOverflowAllowed) {
            dedicatedGroupList.add(InventoryConstants.PUBLIC_ITEM_GROUP);
        }

        // 4. 특정 품번 금지 그룹 리스트 조회
        List<String> forbiddenGroupList = inventoryItemGroupService.getForbiddenGroupList(requestParam.getItemList());
        if (ValueUtil.isEmpty(forbiddenGroupList)) {
            forbiddenGroupList = new ArrayList<>();
        }

        // 5. 전체 품목 중 가장 높은 등급 조회
        int highestItemGrade = inventoryItemMasterService.getHighestItemGrade(requestParam.getItemList());

        // 6. SKU Concentrated 옵션 활성화 여부 확인
        boolean isSkuConcentrated = InventoryConstants.OPTION_VALUE_TRUE.equals(
                inventorySettingService.getOptionValue(InventoryConstants.SKU_CONCENTRATED)
        );

        // 7. SKU Concentrated 옵션이 활성화된 경우 다중 SKU의 중심 Location 조회
        TbInventoryLocation centerLocation = null;
        if (isSkuConcentrated) {
            centerLocation = findReferenceLocation(requestParam.getItemList());
        }
        if (ValueUtil.isEmpty(centerLocation)) {
            centerLocation = new TbInventoryLocation();
        }

        // 8. 조회 후 결과 반환
        String sql = inventoryQueryStore.getCalculateInboundLocationSql();
        Map<String, Object> param = ValueUtil.newMap("locGroup,itemType,stockHeight,stockWeight,dedicatedGroupList,forbiddenGroupList,itemGrade,centerLocation,reservedLocCodeList",
                requestParam.getLocGroup(), requestParam.getItemType(), requestParam.getTotalHeight(), requestParam.getTotalWeight(), String.join(",", dedicatedGroupList),
                String.join(",", forbiddenGroupList), highestItemGrade, centerLocation.getLocCode(), String.join(",", requestParam.getReservedLocCodeList()));

        return this.queryManager.selectBySql(sql, param, TbInventoryLocation.class);
    }

    /**
     * 다중 칸 입고 로케이션 계산
     *
     * @param requestParam 입고 요청 파라미터
     * @param requiredCellCount 요구되는 로케이션 칸 수
     * @return 할당된 로케이션 리스트
     */
    public List<TbInventoryLocation> calculateMultipleInboundLocations(InboundLocationRequestDto requestParam, int requiredCellCount) {
        List<TbInventoryLocation> allocatedLocations = new ArrayList<>();

        // 1. 기존 파라미터의 예약 리스트를 복제 (원본 리스트 오염 방지 및 누적용)
        List<String> reservedList = new ArrayList<>();
        if (ValueUtil.isNotEmpty(requestParam.getReservedLocCodeList())) {
            reservedList.addAll(requestParam.getReservedLocCodeList());
        }
        requestParam.setReservedLocCodeList(reservedList);

        // 2. 환경 설정(4-Way 여부) 조회 (확장 로직 분기용)
        boolean is4WayShuttle = InventoryConstants.OPTION_VALUE_TRUE.equals(
                inventorySettingService.getOptionValue(InventoryConstants.SHUTTLE_WAY_INFO));

        // 3. 요구 칸 수를 채울 때까지 루프 반복
        while (allocatedLocations.size() < requiredCellCount) {

            // Step A: 단일 최적 로케이션(시작점) 획득
            TbInventoryLocation startLoc = calculateInboundLocation(requestParam);

            // 창고가 꽉 차서 더 이상 할당할 공간이 없다면 루프 중단
            if (ValueUtil.isEmpty(startLoc)) {
                break;
            }

            // Step B: 시작점을 기준으로 해당 라인의 연속된 빈 공간(경로) 확보
            List<TbInventoryLocation> contiguousCells = getContiguousEmptyCells(startLoc, is4WayShuttle);

            // Step C: 확보한 셀들을 결과에 담고, 다음 탐색에서 제외되도록 처리
            for (TbInventoryLocation cell : contiguousCells) {
                if (allocatedLocations.size() < requiredCellCount) {
                    allocatedLocations.add(cell);
                    // 핵심: 이 셀은 이미 차지했으므로, 다음 루프의 SQL 쿼리에서 잡히지 않도록 예약 리스트에 추가
                    reservedList.add(cell.getLocCode());
                } else {
                    break; // 요구 칸 수를 모두 채웠으면 중단
                }
            }
        }

        return allocatedLocations;
    }

    /**
     * 시작 로케이션을 기준으로 4-Way / 1-Way 조건에 맞게 연속된 빈 공간을 탐색합니다.
     */
    private List<TbInventoryLocation> getContiguousEmptyCells(TbInventoryLocation startLoc, boolean is4WayShuttle) {
        List<TbInventoryLocation> result = new ArrayList<>();

        // 1. DB에서 해당 라인(Group, Row or Column, Level, Side)의 '전체' 셀을 조회
        List<TbInventoryLocation> lineCells = inventoryLocationService.getLineAllCells(startLoc.getLocCode());

        // 2. 정렬 및 시작점의 인덱스 찾기
        if (!is4WayShuttle) {
            lineCells.sort(Comparator.comparing(TbInventoryLocation::getLocDeep)); // 1-Way는 Deep 기준 정렬
        }

        int startIndex = -1;
        for (int i = 0; i < lineCells.size(); i++) {
            if (lineCells.get(i).getLocCode().equals(startLoc.getLocCode())) {
                startIndex = i;
                break;
            }
        }
        if (startIndex == -1) return result;

        // 시작점은 무조건 결과에 포함 (시작점은 이미 쿼리에서 검증된 상태임)
        result.add(lineCells.get(startIndex));

        // 3. 방향에 따른 확장 로직
        if (is4WayShuttle) {
            // [4-Way 모드] 양방향(Row or Column) 통로 전까지 확장
            int leftIndex = startIndex - 1;
            int rightIndex = startIndex + 1;
            boolean canGoLeft = true;
            boolean canGoRight = true;

            while (canGoLeft || canGoRight) {
                // 왼쪽(음의 방향) 탐색
                if (canGoLeft && leftIndex >= 0) {
                    TbInventoryLocation leftCell = lineCells.get(leftIndex);
                    CellStatus status = checkCellStatus(leftCell);

                    if (status == CellStatus.IMPASSABLE) {
                        canGoLeft = false; // 장애물/통로 만나면 확산 중단
                    } else {
                        if (status == CellStatus.USABLE) {
                            result.add(0, leftCell); // 적재 가능할 때만 결과에 앞쪽 삽입
                        }
                        leftIndex--; // PASSABLE이든 USABLE이든 탐색은 이어서 진행
                    }
                } else {
                    canGoLeft = false;
                }

                // 오른쪽(양의 방향) 탐색
                if (canGoRight && rightIndex < lineCells.size()) {
                    TbInventoryLocation rightCell = lineCells.get(rightIndex);
                    CellStatus status = checkCellStatus(rightCell);

                    if (status == CellStatus.IMPASSABLE) {
                        canGoRight = false; // 장애물/통로 만나면 확산 중단
                    } else {
                        if (status == CellStatus.USABLE) {
                            result.add(rightCell); // 적재 가능할 때만 결과에 뒤쪽 삽입
                        }
                        rightIndex++; // PASSABLE이든 USABLE이든 탐색은 이어서 진행
                    }
                } else {
                    canGoRight = false;
                }
            }
        } else {
            // [1-Way 모드] 중앙 통로 방향으로 앞쪽 장애물까지만 확장
            int frontIndex = startIndex - 1;
            while (frontIndex >= 0) {
                TbInventoryLocation frontCell = lineCells.get(frontIndex);
                CellStatus status = checkCellStatus(frontCell);

                if (status == CellStatus.IMPASSABLE) {
                    break; // 1-Way 앞쪽에 물리적 장애물이 있으면 뒤쪽(Deep)을 빼낼 수 없으므로 완전 중단
                }

                if (status == CellStatus.USABLE) {
                    result.add(0, frontCell); // 적재 가능할 때만 결과에 포함
                }

                frontIndex--; // PASSABLE인 경우 결과에 안 넣고 그냥 지나감
            }
        }

        return result;
    }

    /**
     * 셀의 물리적, 논리적 상태를 판별하여 반환합니다.
     */
    private CellStatus checkCellStatus(TbInventoryLocation cell) {
        // 1. 탐색 완전 중단 (주행로이거나 이미 재고/작업이 있는 물리적 막힘)
        if (Boolean.TRUE.equals(cell.getIsPath())) return CellStatus.IMPASSABLE;
        if (ValueUtil.isNotEmpty(cell.getStockId()) || ValueUtil.isNotEmpty(cell.getTaskId())) return CellStatus.IMPASSABLE;
        if (InventoryConstants.LOCATION_TYPE_PILLAR.equals(cell.getLocType())) return CellStatus.IMPASSABLE;

        // 2. 이동(Pass)은 가능하지만 물건을 둘 수 없는 논리적 막힘 (결과에서 제외)
        if (!Boolean.TRUE.equals(cell.getIsEnabled()) || !Boolean.TRUE.equals(cell.getIsInboundEnabled())) {
            return CellStatus.PASSABLE;
        }

        // 3. 완전히 비어있고 적재 가능한 상태
        return CellStatus.USABLE;
    }

    /**
     * 여러 SKU를 종합하여 가장 근접한 기준 기존 재고 위치(Location)를 반환
     *
     * @param itemList 입고하는 재고의 전체 품목 정보
     * @return 관련 재고의 중심점과 가장 가까운 로케이션
     */
    public TbInventoryLocation findReferenceLocation(List<ItemIdentifierDto> itemList) {
        // 0. 입력 파라미터에 대한 유효성 검사
        if (!validateItemIdentifier(itemList)) throw new ElidomRuntimeException("입력 파라미터가 유효하지 않습니다.");

        // 1. DTO 리스트에 해당하는 기존 재고의 모든 Location 조회
        StringBuilder sql = new StringBuilder();
        Map<String, Object> param = new HashMap<>();

        sql.append("SELECT L.* ");
        sql.append("FROM tb_inventory_location L ");
        sql.append("JOIN tb_inventory_stock S ON L.stock_id = S.stock_id ");
        InventoryUtils.appendItemInClause(sql, param, itemList);

        List<TbInventoryLocation> existingLocations = this.queryManager.selectListBySql(sql.toString(), param, TbInventoryLocation.class, 0, 0);

        // 기존 재고가 아예 없는 경우
        if (ValueUtil.isEmpty(existingLocations)) {
            return null;
        }

        // 2. 무게 중심(Centroid) 계산 (Col, Row, Level의 평균)
        double avgCol = existingLocations.stream().mapToInt(TbInventoryLocation::getLocCol).average().orElse(0.0);
        double avgRow = existingLocations.stream().mapToInt(TbInventoryLocation::getLocRow).average().orElse(0.0);
        double avgLevel = existingLocations.stream().mapToInt(TbInventoryLocation::getLocLevel).average().orElse(0.0);

        // 3. 무게 중심 좌표와 가장 가까운 기존 Location 탐색 (맨해튼 거리 기준)
        return existingLocations.stream()
                .min(Comparator.comparingDouble(loc ->
                        InventoryUtils.calculateManhattanDistance(loc, avgCol, avgRow, avgLevel)))
                .orElse(existingLocations.get(0));
    }

    /**
     * 특정 로케이션을 재배치하기 위한 작업의 목적지 정보를 반환
     *
     * @param source 정렬 대상 로케이션 정보
     * @param reservedLocIds 이미 후보로 지정된 로케이션 목록
     * @return source 로케이션과 비슷한 환경의 작업 목적지 로케이션
     */
    public TbInventoryLocation findOptimalEmptyLocation(TbInventoryLocation source, Set<String> reservedLocIds) {
        // 현재 설정된 설비 종류 조회
        String wayInfo = inventorySettingService.getOptionValue(InventoryConstants.SHUTTLE_WAY_INFO);

        // 목적지의 뒤쪽에는 예정 작업이 없으며 앞쪽으로는 재고와 예정 작업이 없도록 조회
        String sql;
        if (InventoryConstants.OPTION_VALUE_TRUE.equals(wayInfo)) {
            sql = inventoryQueryStore.getCalculateSortLocation4WaySql();
        } else {
            sql = inventoryQueryStore.getCalculateSortLocation2WaySql();
        }

        // 재고 정보에서 Weight와 Height 조회
        StockMetricsDto stockMetricsInfo = inventoryStockService.getTotalStockMetrics(source.getStockId());

        Map<String, Object> param = ValueUtil.newMap("locGroup,locCol,locRow,locLevel,itemType,itemGroup,stockHeight,stockWeight,reservedLocCodes",
                source.getLocGroup(), source.getLocCol(), source.getLocRow(), source.getLocLevel(), source.getItemType(), source.getItemGroup(), stockMetricsInfo.getTotalHeight(), stockMetricsInfo.getTotalWeight(), String.join(",", reservedLocIds));

        return this.queryManager.selectBySql(sql, param, TbInventoryLocation.class);
    }

    private boolean validateInboundLocationRequest(InboundLocationRequestDto requestParam) {
        if (!validateItemIdentifier(requestParam.getItemList())) {
            return false;
        }

        if (ValueUtil.isEmpty(requestParam.getItemType()) || ValueUtil.isEmpty(requestParam.getTotalHeight()) || ValueUtil.isEmpty(requestParam.getTotalWeight())) {
            return false;
        }

        return true;
    }

    private boolean validateItemIdentifier(List<ItemIdentifierDto> itemList) {
        if (ValueUtil.isEmpty(itemList) || itemList.isEmpty()) {
            return false;
        }

        for (ItemIdentifierDto item : itemList) {
            if (ValueUtil.isEmpty(item.getItemOwner()) || ValueUtil.isEmpty(item.getItemCode())) {
                return false;
            }
        }

        return true;
    }

    private enum CellStatus {
        USABLE,     // 적재 가능 (결과에 포함, 탐색 계속)
        PASSABLE,   // 적재 불가 & 이동 가능 (결과에서 제외, 탐색 계속)
        IMPASSABLE  // 물리적 막힘 & 주행로 (탐색 중단)
    }
}