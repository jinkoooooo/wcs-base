package operato.logis.inventory.service;

import lombok.RequiredArgsConstructor;
import operato.logis.inventory.consts.OutboundCalculateStrategy;
import operato.logis.inventory.dto.ItemIdentifierDto;
import operato.logis.inventory.dto.OutboundStockRequestDto;
import operato.logis.inventory.dto.RelocationTaskDto;
import operato.logis.inventory.entity.TbInventoryLocation;
import operato.logis.inventory.entity.TbInventoryStock;
import operato.logis.wcs.entity.TbWcsHostOrderItem;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockRelocationService extends AbstractQueryService {

    private final InventoryOutboundStockService inventoryOutboundStockService;
    private final InventoryLocationService inventoryLocationService;

    public RelocationTaskDto getNextOutboundRelocation(Date scheduledDate) {
        // 1. 출고 예정 품목 조회
        String sql = """
                SELECT
                    I.ITEM_CODE,
                    I.LOT_NO,
                    SUM(I.QTY) AS QTY
                FROM
                    TB_WCS_HOST_ORDER O
                JOIN
                    TB_WCS_HOST_ORDER_ITEM I
                    ON O.HOST_ORDER_KEY = I.HOST_ORDER_KEY
                WHERE
                    O.ORDER_TYPE = 'OUTBOUND'
                    AND O.SCHEDULED_DATE = :scheduledDate
                GROUP BY
                    I.ITEM_CODE,
                    I.LOT_NO
                ORDER BY
                    I.ITEM_CODE,
                    I.LOT_NO
                """;
        Map<String, Object> param = ValueUtil.newMap("scheduledDate", scheduledDate);
        List<TbWcsHostOrderItem> nextOutboundRequestList = this.queryManager.selectListBySql(sql, param, TbWcsHostOrderItem.class, 0, 0);

        List<TbInventoryStock> totalOutboundStockList = new ArrayList<>();
        if (ValueUtil.isNotEmpty(nextOutboundRequestList)) {
            try {
                // 2. LOT_NO 기준으로 그룹화 (LOT_NO가 null인 경우 "EMPTY_LOT"으로 묶어 통합)
                Map<String, List<TbWcsHostOrderItem>> groupedByLotNo = nextOutboundRequestList.stream()
                        .collect(Collectors.groupingBy(item -> item.getLotNo() != null ? item.getLotNo() : "EMPTY_LOT"));

                // 3. 그룹별 OutboundStockRequestDto 생성 및 연산
                for (Map.Entry<String, List<TbWcsHostOrderItem>> entry : groupedByLotNo.entrySet()) {
                    String currentLotNo = entry.getKey();
                    List<TbWcsHostOrderItem> groupItems = entry.getValue();

                    // ItemIdentifierDto 리스트 구성
                    List<ItemIdentifierDto> itemList = groupItems.stream().map(orderItem -> {
                        ItemIdentifierDto itemDto = new ItemIdentifierDto();
                        itemDto.setItemCode(orderItem.getItemCode());
                        itemDto.setItemOwner("OWNER001"); // TODO : 테스트 코드 -> 현장 코드
                        itemDto.setItemQty(orderItem.getQty());
                        return itemDto;
                    }).collect(Collectors.toList());

                    // OutboundStockRequestDto 생성 및 데이터 세팅
                    OutboundStockRequestDto requestDto = new OutboundStockRequestDto();
                    requestDto.setOutboundCalculateStrategy(OutboundCalculateStrategy.FIFO);
                    requestDto.setItemList(itemList);

                    // 실제 LOT_NO가 존재하는 그룹에만 lotNo 필드 할당
                    if (!"EMPTY_LOT".equals(currentLotNo)) {
                        requestDto.setLotNo(currentLotNo);
                    }

                    // 4. 출고 대상 재고 목록 조회 및 전체 결과에 병합
                    List<TbInventoryStock> outboundStockList = inventoryOutboundStockService.calculateOutboundStock(requestDto);
                    if (ValueUtil.isEmpty(outboundStockList)) {
                        continue;
                    }

                    for (TbInventoryStock stock : outboundStockList) {
                        stock.setAttributeA("computed");
                        totalOutboundStockList.add(stock);
                    }
                    this.queryManager.updateBatch(outboundStockList, "attributeA");
                }
            }
            finally {
                // 5. 출고 목록 산출 후 속성 원복
                for (TbInventoryStock stock : totalOutboundStockList) {
                    stock.setAttributeA(null);
                }
                this.queryManager.updateBatch(totalOutboundStockList, "attributeA");
            }
        }

        // 6. 최종 조회된 전체 재고 리스트에서 중복 제거된 Stock ID 추출
        List<String> distinctStockIdList = totalOutboundStockList.stream()
                .map(TbInventoryStock::getStockId)
                .distinct()
                .toList();

        if (ValueUtil.isNotEmpty(distinctStockIdList)) {
            // 6-1. stockId 리스트에 해당하는 로케이션 목록 조회
            List<TbInventoryLocation> locationList = inventoryLocationService.getLocationListByStockIdList(distinctStockIdList);

            // 6-2. List를 Map<String, Integer> 형태로 변환 (Key: stockId, Value: locDeep)
            Map<String, Integer> stockIdToLocDeepMap = locationList.stream()
                    .collect(Collectors.toMap(
                            TbInventoryLocation::getStockId,
                            loc -> loc.getLocDeep() != null ? loc.getLocDeep() : 0,
                            (existing, replacement) -> existing // 중복 방어
                    ));

            // 6-3. 메모리에 올려둔 Map의 Value(locDeep)를 참조하여 오름차순 정렬 적용 (locDeep 값이 작을수록 앞에 위치)
            List<String> stockIdList = distinctStockIdList.stream()
                    .sorted(Comparator.comparingInt(id -> stockIdToLocDeepMap.getOrDefault(id, 0)))
                    .toList();

            // 7. 다음 정렬 작업 반환
            for (String stockId : stockIdList) {
                TbInventoryLocation originLocation = inventoryLocationService.findByStockId(stockId);
                TbInventoryLocation targetLocation = calculateFrontLocation(originLocation);
                if (ValueUtil.isEmpty(targetLocation) || isFrontLocation(originLocation, targetLocation)) {
                    continue;
                }

                return RelocationTaskDto.builder()
                        .stepOrder(0)
                        .stockId(stockId)
                        .fromLocCode(originLocation.getLocCode())
                        .toLocCode(targetLocation.getLocCode())
                        .build();
            }
        }

        return null;
    }

    private boolean isFrontLocation(TbInventoryLocation originLocation, TbInventoryLocation targetLocation) {
        String locGroup = originLocation.getLocGroup();
        int originCol = originLocation.getLocCol();
        int targetCol = targetLocation.getLocCol();

        // 그룹별 비교 로직 적용
        if ("BMI_COLD".equals(locGroup)) {
            // "BMI_COLD"는 loc_col이 낮을수록 가깝다.
            // originLocation의 loc_col이 더 낮으면 중앙 통로에 더 가까우므로 true 반환
            return originCol <= targetCol;

        } else if ("BMI_ORDINARY".equals(locGroup)) {
            // "BMI_ORDINARY"는 loc_col이 높을수록 가깝다.
            // originLocation의 loc_col이 더 높으면 중앙 통로에 더 가까우므로 true 반환
            return originCol >= targetCol;
        }

        // 조건에 명시되지 않은 기타 그룹이 들어올 경우의 기본 반환값
        return true;
    }

    private TbInventoryLocation calculateFrontLocation(TbInventoryLocation originLocation) {
        String locGroup = originLocation.getLocGroup();

        // 1. locGroup에 따른 동적 정렬 기준 설정
        String orderByClause = "";
        if ("BMI_COLD".equals(locGroup)) {
            orderByClause = " ORDER BY loc_col ASC, loc_deep DESC";
        } else if ("BMI_ORDINARY".equals(locGroup)) {
            orderByClause = " ORDER BY loc_col DESC, loc_deep DESC";
        }

        // 2. SQL 쿼리 작성
        String sql = """
            SELECT *
            FROM tb_inventory_location
            WHERE loc_group = :locGroup
              AND loc_type = :locType
              AND item_type = :itemType
              AND item_group = :itemGroup
              AND is_enabled = true
              AND is_inbound_enabled = true
              AND is_outbound_enabled = true
              AND is_path = false
              AND (stock_id IS NULL OR stock_id = '')
              AND (task_id IS NULL OR task_id = '')
            """ + orderByClause + " LIMIT 1";

        // 3. 파라미터 매핑
        Map<String, Object> param = ValueUtil.newMap(
                "locGroup,locType,itemType,itemGroup", locGroup, originLocation.getLocType(), originLocation.getItemType(), originLocation.getItemGroup());

        // 4. 데이터 조회
        return this.queryManager.selectBySql(sql, param, TbInventoryLocation.class);
    }
}