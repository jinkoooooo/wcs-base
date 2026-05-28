package operato.logis.samsung.service.buffer;

import lombok.RequiredArgsConstructor;
import operato.logis.samsung.dto.buffer.BufferSkuAllocationDto;
import operato.logis.samsung.entity.buffer.TbMwBufferStorageArea;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class BufferInboundService extends AbstractQueryService {

    private final TbMwBoxBufferService boxBufferService;
    private final TbMwBufferTaskService taskService;
    private final TbMwBufferItemGradeService itemGradeService;
    private final TbMwBufferStorageAreaService storageAreaService;
    private final TbMwBufferStorageAllocationService storageAllocationService;


    public void allocateLane() {

        // SKU 집계 + 등급 산출
        Map<String, Integer> skuQtyMap = boxBufferService.aggregateSku();
        Map<String, Integer> skuGradeMap = itemGradeService.calculateGrade(skuQtyMap);
        List<BufferSkuAllocationDto> skuList = buildSkuAllocationDto(skuQtyMap, skuGradeMap);

        // 단일/다중 Area 조회
        List<TbMwBufferStorageArea> areas = storageAreaService.getAvailableAreas();

        // todo: existingAllocation 이전에 마감로직 실행하여 재고 한 곳으로 모으기
        // 기존 재고 기반 고정
        List<BufferSkuAllocationDto> existingAllocation = storageAllocationService.getExistingAllocation();

        // Area 할당
        //Map<String, List<TbMwBufferStorageArea>> allocationResult = storageAllocationService.allocateAreaCore(skuQtyMap, skuGradeMap, singleAreas, multiAreas, existingAllocation);
        Map<String, List<TbMwBufferStorageArea>> allocationResult = storageAllocationService.allocateAreaCore(skuList, areas, existingAllocation);

        // 작업 생성 (바로 DB 미반영)
        taskService.createAllocationTask(allocationResult);

        // 버퍼 내 관리 박스 생성 / todo: 바코드 스캔 후로 로직 이동
        //boxBufferService.createInboundBox();
    }

    /**
     * SKU 수량/등급 정보를 DTO로 변환
     * - 수량 기준 내림차순
     */
    private List<BufferSkuAllocationDto> buildSkuAllocationDto(Map<String, Integer> skuQtyMap, Map<String, Integer> skuGradeMap) {
        List<BufferSkuAllocationDto> result = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : skuQtyMap.entrySet()) {
            String itemCode = entry.getKey();
            Integer qty = entry.getValue();

            BufferSkuAllocationDto dto = new BufferSkuAllocationDto(itemCode, qty, skuGradeMap.get(itemCode), new ArrayList<>());
            result.add(dto);
        }

        result.sort((a, b) -> b.getQty() - a.getQty());
        return result;
    }
}