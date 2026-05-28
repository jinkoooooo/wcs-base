package operato.logis.wcs.service.impl.query.inventory;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.entity.TbWcsPalletBox;
import operato.logis.wcs.service.impl.label.ExpiryColorCalculator;
import operato.logis.wcs.service.impl.label.LabelGenerator;
import operato.logis.wcs.service.repository.InventoryStockRepository;
import operato.logis.wcs.service.repository.PalletBoxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;

import static operato.logis.wcs.common.util.check.Validator.requireFound;
import xyz.elidom.util.ValueUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * QR 태블릿 조회 서비스.
 *
 * 박스 바코드(B- prefix) / 파렛트 바코드 모두 단일 진입점에서 분기.
 * 응답에 라벨 메타 + 잔량 + 사용기한 색상 + SKU+lot 집계를 포함한다.
 *
 * 읽기 전용 — 외부 I/O 없음.
 */
@Service
@RequiredArgsConstructor
public class QrTabletService {

    private static final String BOX_BARCODE_PREFIX = "B-";

    private final PalletBoxRepository palletBoxRepository;
    private final LabelGenerator labelGenerator;
    private final ExpiryColorCalculator expiryColorCalculator;
    private final InventoryStockRepository stockRepository;

    /**
     * QR/바코드 스캔 진입점. "B-" prefix 면 박스, 아니면 파렛트로 분기.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> scan(String code) {
        if (ValueUtil.isEmpty(code)) {
            throw new ElidomRuntimeException("code 는 필수입니다");
        }
        String trimmed = code.trim();
        if (trimmed.startsWith(BOX_BARCODE_PREFIX)) {
            return lookupBox(trimmed);
        }
        return lookupPallet(trimmed);
    }

    /**
     * 박스 바코드로 박스 정보 + 사용기한 색상 + 동일 SKU+lot 집계 조회.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> lookupBox(String boxBarcode) {
        TbWcsPalletBox box = palletBoxRepository.findByBoxBarcode(boxBarcode);
        requireFound(box, "BOX_NOT_FOUND", boxBarcode);
        Map<String, Object> payload = new LinkedHashMap<>(labelGenerator.buildBoxLabel(box.getId()));
        // 사용기한 색상 — LabelGenerator 응답의 expiryDate 와 동일하게 계산
        payload.putAll(expiryColorCalculator.computeFromExpiryDate(box.getExpiryDate()));
        // 동일 SKU + lot 집계
        payload.put("aggregateBySkuLot",
                stockRepository.aggregateBySkuAndLot(box.getItemCode(), box.getLotNo()));
        return payload;
    }

    /**
     * 파렛트 바코드로 파렛트 + 박스 리스트 조회. 박스별 잔량과 사용기한 색상을 함께 노출.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> lookupPallet(String palletBarcode) {
        List<TbWcsPalletBox> boxes = palletBoxRepository.findByPalletBarcode(palletBarcode);
        requireFound(boxes, "PALLET_NOT_FOUND", palletBarcode);
        Map<String, Object> payload = new LinkedHashMap<>(labelGenerator.buildPalletLabel(palletBarcode));

        // 박스별 요약 + 잔량 합계
        int totalRemaining = 0;
        List<Map<String, Object>> boxSummaries = new ArrayList<>();
        for (TbWcsPalletBox b : boxes) {
            Map<String, Object> bs = new LinkedHashMap<>();
            bs.put("boxId", b.getId());
            bs.put("boxBarcode", b.getBoxBarcode());
            bs.put("boxSeq", b.getBoxSeq());
            bs.put("itemCode", b.getItemCode());
            bs.put("lotNo", b.getLotNo());
            bs.put("totalQty", b.getTotalQty());
            bs.put("pickedQty", b.getPickedQty());
            int rem = b.calcRemainingQty();
            bs.put("remainingQty", rem);
            bs.put("boxStatus", b.getBoxStatus());
            bs.put("testNo", b.getTestNo());
            bs.putAll(expiryColorCalculator.computeFromExpiryDate(b.getExpiryDate()));
            totalRemaining += rem;
            boxSummaries.add(bs);
        }
        payload.put("totalRemaining", totalRemaining);
        payload.put("boxes", boxSummaries);
        return payload;
    }
}
