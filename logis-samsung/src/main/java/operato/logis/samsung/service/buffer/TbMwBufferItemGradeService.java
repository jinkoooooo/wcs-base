package operato.logis.samsung.service.buffer;

import lombok.RequiredArgsConstructor;
import operato.logis.samsung.consts.BufferItemGrade;
import operato.logis.samsung.entity.buffer.TbMwBufferItemGrade;
import operato.logis.samsung.entity.buffer.TbMwBufferItemGradeHist;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * - 상품 별 등급 계산
 * - 상품 별 이력 저장
 */
@Service
@Transactional
@RequiredArgsConstructor
public class TbMwBufferItemGradeService extends AbstractQueryService {

    private final int CLASS_C_QTY_STANDARD = 5; // 초저빈도 기준 수량

    /**
     * item 등급 설정
     * 1. 초저빈도 : CLASS_C_QTY_STANDARD 이하 수량
     * 2. 저빈도 : 전체 수량 중 하위 50%
     * 3. 고빈도 : 전체 수량 중 상위 50%
     * TODO: INSERT 로직 추가
     *
     * @param skuQtyMap SKU 별 입고 예정 수량
     * @return
     */
    public Map<String, Integer> calculateGrade(Map<String, Integer> skuQtyMap) {

        Map<String, Integer> result = new HashMap<>();

        List<Integer> sortedQty = skuQtyMap.values().stream()
                .sorted(Comparator.reverseOrder())
                .toList();

        //int medianIndex = sortedQty.size() / 2;
        int medianIndex = (sortedQty.size() - 1) / 2;
        int threshold = sortedQty.get(medianIndex);

        for (Map.Entry<String, Integer> entry : skuQtyMap.entrySet()) {

            int qty = entry.getValue();
            if (qty <= CLASS_C_QTY_STANDARD) {
                result.put(entry.getKey(), BufferItemGrade.CLASS_C.getValue());
                saveGrade(entry.getKey(), BufferItemGrade.CLASS_C);
            } else if (qty >= threshold) {
                result.put(entry.getKey(), BufferItemGrade.CLASS_A.getValue());
                saveGrade(entry.getKey(), BufferItemGrade.CLASS_A);
            } else {
                result.put(entry.getKey(), BufferItemGrade.CLASS_B.getValue());
                saveGrade(entry.getKey(), BufferItemGrade.CLASS_B);
            }
        }

        return result;
    }

    private void saveGrade(String itemCode, BufferItemGrade newGrade) {
        // todo: 한꺼번에 insert
        Query condition = new Query();
        condition.addFilter("item_code", itemCode);
        TbMwBufferItemGrade prev = queryManager.selectByCondition(TbMwBufferItemGrade.class, condition);

        if (prev == null) {
            TbMwBufferItemGrade item = new TbMwBufferItemGrade();
            item.setItem(null); // todo: TbMwItemMasterRef - tb_mw_item_master의 id
            item.setItemCode(itemCode);
            item.setInnerItemCode(null); // todo: tb_mw_inbound_delivery의 inner_item_code
            item.setGrade(newGrade.getValue());
            item.setInboundQty(0); // todo: sku맵의 수량
            queryManager.insert(TbMwBufferItemGrade.class, item);
        } else{
            TbMwBufferItemGradeHist hist = new  TbMwBufferItemGradeHist();
            hist.setItemId(prev.getItemId());
            hist.setItemCode(itemCode);
            hist.setInnerItemCode(prev.getInnerItemCode());
            hist.setBeforeGrade(prev.getGrade());
            hist.setAfterGrade(newGrade.getValue());
            hist.setBeforeInboundQty(prev.getInboundQty());
            hist.setAfterInboundQty(0); // todo: sku맵의 수량
            queryManager.insert(TbMwBufferItemGradeHist.class, hist);
        }
    }
}