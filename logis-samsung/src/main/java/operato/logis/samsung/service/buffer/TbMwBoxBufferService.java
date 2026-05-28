package operato.logis.samsung.service.buffer;

import lombok.RequiredArgsConstructor;
import operato.logis.samsung.consts.BufferStockStatus;
import operato.logis.samsung.entity.buffer.TbMwBoxBuffer;
import operato.logis.samsung.entity.mw.TbMwInboundDelivery;
import operato.logis.samsung.service.mw.TbMwInboundDeliveryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * - 시퀀스버퍼 내 box 조회
 * - 시퀀스버퍼 관리 box 생성
 * - 중복 검사
 * - 출고 여부 검사
 * - sku 집계
 */
@Service
@Transactional
@RequiredArgsConstructor
public class TbMwBoxBufferService extends AbstractQueryService {

    private final TbMwInboundDeliveryService inboundDeliveryService;

    /**
     * 당일 입고 예정 및 시퀀스 버퍼 내에 존재하는 박스 수량 조회
     * 1. 기존 박스 조회 - 보관중, 재정리 중 상태
     * 2. 신규 입고 예정 박스 조회
     *
     * @return key: itemCode(SKU), value: 총 예상 수량
     */
    public Map<String, Integer> aggregateSku() {

        Map<String, Integer> currentStock = aggregateCurrentStock();
        Map<String, Integer> todayInbound = aggregateTodayInbound();
        Map<String, Integer> skuQtyMap = mergeSkuQty(currentStock, todayInbound);

        return skuQtyMap;
    }

    /**
     * 시퀀스버퍼 내 SKU 별 수량 조회
     *
     * @return
     */
    private Map<String, Integer> aggregateCurrentStock() {
        Map<String, Integer> result = new HashMap<>();

        // todo: item_code 기반 그룹화하여 sku 계산
        Query condition = new Query();
        condition.addFilter("stock_status", ">=", BufferStockStatus.INBOUND.getValue());
        condition.addFilter("stock_status", "<", BufferStockStatus.IN_PROGRESS_OUTBOUND.getValue());
        List<TbMwBoxBuffer> existingBoxes = queryManager.selectList(TbMwBoxBuffer.class, condition);

        for (TbMwBoxBuffer box : existingBoxes) {
            result.put(box.getItemCode(), result.getOrDefault(box.getItemCode(), 0) + 1);
        }
        return result;
    }

    /**
     * 오늘 입고 예정 SKU별 수량 조회
     */
    private Map<String, Integer> aggregateTodayInbound() {
        Map<String, Integer> result = new HashMap<>();

        // inbound_date 형식 : yyyy-MM-dd HH:mm:ss.ffffff
        LocalDateTime startDateTime = LocalDate.now().atStartOfDay();
        Date targetDate = Timestamp.valueOf(startDateTime);
        List<TbMwInboundDelivery> newBoxes = inboundDeliveryService.getInboundDeliveryByDate(targetDate);

        for (TbMwInboundDelivery box : newBoxes) {
            result.put(box.getItemCode(), result.getOrDefault(box.getItemCode(), 0) + box.getItemQty());
        }
        return result;
    }

    /**
     * SKU 수량 병합
     * - 현재 시퀀스버퍼 재고 + 당일 입고 예정 수량 합산
     *
     * @param currentStock 현재 시퀀스버퍼 재고
     * @param todayInbound 당일 입고 예정 수량
     * @return SKU 별 총 예상 수량
     */
    private Map<String, Integer> mergeSkuQty(Map<String, Integer> currentStock, Map<String, Integer> todayInbound) {
        Map<String, Integer> result = new HashMap<>();

        for (Map.Entry<String, Integer> entry : currentStock.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, Integer> entry : todayInbound.entrySet()) {
            String itemCode = entry.getKey();
            Integer inboundQty = entry.getValue();

            result.put(itemCode, result.getOrDefault(itemCode, 0) + inboundQty);
        }

        return result;
    }
}
