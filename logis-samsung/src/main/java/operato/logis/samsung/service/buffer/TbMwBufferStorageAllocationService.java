package operato.logis.samsung.service.buffer;

import lombok.RequiredArgsConstructor;
import operato.logis.samsung.consts.BufferItemGrade;
import operato.logis.samsung.dto.buffer.BufferSkuAllocationDto;
import operato.logis.samsung.entity.buffer.TbMwBufferStorageArea;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.*;

/**
 * - 할당 유무 조회
 * - 상품 별 area 할당
 */
@Service
@Transactional
@RequiredArgsConstructor
public class TbMwBufferStorageAllocationService extends AbstractQueryService {

    private final int CLASS_C_QTY_STANDARD = 5; // 초저빈도 기준 수량

    /**
     * SKU 별 Area 할당
     * <p>
     * 1. 초저빈도 (CLASS_C)
     * - 다중 Area 우선 사용
     * - aisle 내림차순 / level 오름차순
     * - SKU 당 최소 1칸 보장
     * - 기준 : CLASS_C_QTY_STANDARD 수량 이하
     * <p>
     * 2. 고빈도 (CLASS_A)
     * - 단일 Area 우선 사용
     * - aisle 오름차순 / level 오름차순
     * - 기준 : 전체 수량 중 상위 50%
     * <p>
     * 3. 저빈도 (CLASS_B)
     * - 단일 Area 우선 사용
     * - aisle 내림차순 / level 오름차순
     * - 기준 : 전체 수량 중 하위 50%
     * <p>
     * 4. 비중 기반 추가 할당
     * - 남은 수량 비율 기준
     * - 다중 area capacity 고령
     * - 단일 area 중복 할당 불가 - todo : 재확인
     * - 다중 area는 maxSkuCnt 까지 허용
     * *
     *
     * @param skuList            SKU 별 총 수량 (qty 내림차순)
     * @param areas              Area목록 (aisle 오름차순, level 오름차순)
     *                           //* @param singleAreas        단일 SKU만 사용 가능한 Area 목록 (aisle 오름차순, level 오름차순)
     *                           //* @param multiAreas         다중 SKU가 사용 가능한 Area 목록 (aisle 오름차순, level 오름차순)
     * @param existingAllocation 기존 버퍼 할당 정보
     * @return SKU 별 할당 Area 목록
     */
    public Map<String, List<TbMwBufferStorageArea>> allocateAreaCore(
            List<BufferSkuAllocationDto> skuList,
            List<TbMwBufferStorageArea> areas,
            List<BufferSkuAllocationDto> existingAllocation
    ) {
        Map<String, List<TbMwBufferStorageArea>> result = new HashMap<>();

        List<TbMwBufferStorageArea> singleAreas = areas.stream().filter(a -> a.getMaxSkuCnt() == 1).toList();
        List<TbMwBufferStorageArea> multiAreas = areas.stream().filter(a -> a.getMaxSkuCnt() > 1)
                .sorted(Comparator
                        .comparing(TbMwBufferStorageArea::getAisleNo).reversed()
                        .thenComparing(TbMwBufferStorageArea::getLevelNo))
                .toList();

        // todo: existingAllocation 상태를 areas, singleAreas, multiAreas에 반영

        // 다중 area에서 현재 사용 중인 SKU 수
        Map<String, Integer> multiAreaUsageMap = new HashMap<>();
        // 이미 사용된 single area
        Set<String> usedSingleAreaIds = new HashSet<>();

        // 고빈도 area
        int half = (int) Math.ceil(singleAreas.size() / 2.0);
        List<TbMwBufferStorageArea> highAreas = singleAreas.subList(0, half);

        // 저빈도 area
        List<TbMwBufferStorageArea> lowAreas = singleAreas.subList(half, singleAreas.size())
                .stream()
                .sorted(Comparator
                        .comparing(TbMwBufferStorageArea::getAisleNo).reversed()
                        .thenComparing(TbMwBufferStorageArea::getLevelNo))
                .toList();

        // 1. 1차 할당 - SKU 별 최소 1 Area 보장
        for (BufferSkuAllocationDto sku : skuList) {
            String itemCode = sku.getItemCode();
            Integer qty = sku.getQty();
            Integer grade = sku.getGrade();

            if (hasExistingAllocation(itemCode, existingAllocation)) {
                continue;
            }

            // 초저빈도
            if (qty <= CLASS_C_QTY_STANDARD) {
                for (TbMwBufferStorageArea area : multiAreas) {
                    int currentUsage = multiAreaUsageMap.getOrDefault(area.getId(), 0);
                    if (currentUsage >= area.getMaxSkuCnt()) {
                        continue;
                    }

                    result.computeIfAbsent(itemCode, k -> new ArrayList<>()).add(area);
                    multiAreaUsageMap.put(area.getId(), currentUsage + 1);
                    break;
                }
                continue; // todo: 언제 continue 되는지 확인 - 예상) 초저빈도인 경우 다음 로직 타지 않도록
                //todo: 할당되지 않은 sku는 어떻게 처리할 것인지
            }

            // 고빈도
            if (grade == BufferItemGrade.CLASS_A.getValue()) {
                for (TbMwBufferStorageArea area : highAreas) {
                    if (usedSingleAreaIds.contains(area.getId())) {
                        continue;
                    }

                    result.computeIfAbsent(itemCode, k -> new ArrayList<>()).add(area);
                    usedSingleAreaIds.add(area.getId());
                    break;
                }
                continue; // todo: 언제 continue 되는지 확인 - 예상) 고빈도인 겨우 다음 로직 타지 않도록
                //todo: 할당되지 않은 sku는 어떻게 처리할 것인지
            }

            // 저빈도
            if (grade == BufferItemGrade.CLASS_B.getValue()) {
                for (TbMwBufferStorageArea area : lowAreas) {
                    if (usedSingleAreaIds.contains(area.getId())) {
                        continue;
                    }

                    result.computeIfAbsent(itemCode, k -> new ArrayList<>()).add(area);
                    usedSingleAreaIds.add(area.getId());
                    break;
                }
                //todo: 할당되지 않은 sku는 어떻게 처리할 것인지
            }
        }

        // 2. 2차 할당 - 비중 기반 추가 할당
        int totalQty = skuList.stream().mapToInt(BufferSkuAllocationDto::getQty).sum();

        for (BufferSkuAllocationDto sku : skuList) {
            String itemCode = sku.getItemCode();
            int qty = sku.getQty();
            int grade = sku.getGrade();

            int currentAllocated = result.getOrDefault(sku, List.of()).size();

            double ratio = (double) qty / totalQty;
            int targetCnt = (int) Math.ceil(ratio * (singleAreas.size() + multiAreas.size())); // todo: ceil/floor 비교
            int needCnt = targetCnt - currentAllocated;

            if (needCnt <= 0) {
                continue;
            }

            List<TbMwBufferStorageArea> targetAreas = grade == BufferItemGrade.CLASS_A.getValue() ? highAreas : lowAreas;

            for (TbMwBufferStorageArea area : targetAreas) {
                if (needCnt <= 0) {
                    break;
                }
                if (usedSingleAreaIds.contains(area.getId())) {
                    continue;
                }

                result.computeIfAbsent(itemCode, k -> new ArrayList<>()).add(area);
                usedSingleAreaIds.add(area.getId());
                needCnt--;
            }

            for (TbMwBufferStorageArea area : multiAreas) {
                if (needCnt <= 0) {
                    break;
                }
                int currentUsage = multiAreaUsageMap.getOrDefault(area.getId(), 0);
                if (currentUsage >= area.getMaxSkuCnt()) {
                    continue;
                }

                result.computeIfAbsent(itemCode, k -> new ArrayList<>()).add(area);
                multiAreaUsageMap.put(area.getId(), currentUsage + 1);
                needCnt--;
            }
        }
        return result;
    }

    private boolean hasExistingAllocation(String itemCode, List<BufferSkuAllocationDto> existingAllocation) {
        return existingAllocation.stream().anyMatch(e -> e.getItemCode().equals(itemCode));
    }

    /**
     * 현재 SKU 별 할당 Area 조회
     *
     * @return SKU 별 AreaId 목록
     */
    public List<BufferSkuAllocationDto> getExistingAllocation() {
        List<BufferSkuAllocationDto> result = new ArrayList<>();

        String query = """
                SELECT a.id, l.item_id
                FROM tb_mw_buffer_storage_area a 
                    JOIN tb_mw_buffer_storage_allocation l
                        ON a.id = l.area_id
                WHERE a.stock_qty > 0;
                """;
        List<Map> allocations = queryManager.selectListBySql(query, null, Map.class, 0, 0);

        for (Map allocation : allocations) {
            String sku = allocation.get("item_id").toString();
            //result.computeIfAbsent(sku, k -> new ArrayList<>()).add(allocation.get("id").toString());
            //todo: result[i].getItemCode가 sku와 동일한 값이 있는 경우, qty ++, result[i].getAllocationAreas.add(area);
            //todo: result[i].getItemcode가 sku와 동일한 값이 없는 경우, new BufferSkuAllocationdto(sku, 1, null, new ArrayList<>(area)); <- area(TbMwBufferStorageArea) 추가 필요
        }

        return result;
    }
}