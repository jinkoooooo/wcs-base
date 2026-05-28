package operato.logis.wcs.service.impl.label;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import operato.logis.wcs.entity.ExtTbInventoryItemMaster;
import operato.logis.wcs.entity.TbWcsHostOrder;
import operato.logis.wcs.entity.TbWcsHostOrderItem;
import operato.logis.wcs.entity.TbWcsPalletBox;
import operato.logis.wcs.service.impl.label.renderer.PdfLabelRenderer;
import operato.logis.wcs.service.impl.label.renderer.ZplLabelRenderer;
import operato.logis.wcs.service.impl.pallet.PalletProgressService;
import operato.logis.wcs.service.repository.HostOrderItemRepository;
import operato.logis.wcs.service.repository.HostOrderRepository;
import operato.logis.wcs.service.repository.InventoryItemMasterRepository;
import operato.logis.wcs.service.repository.PalletBoxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import xyz.elidom.exception.server.ElidomRuntimeException;

import static operato.logis.wcs.common.util.check.Validator.requireFound;
import static operato.logis.wcs.common.util.check.Validator.requireNotEmpty;
import xyz.elidom.util.ValueUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 라벨 데이터(JSON Map) 생성 + ZPL/PDF 렌더러 진입점.
 *
 * 박스 라벨의 수량 표기는 total_qty (박스 처음 수량) 를 사용한다.
 * 박스 바코드도 불변이므로 부분 출고 후에도 라벨 내용은 그대로 유효하다.
 *
 * 읽기 전용 — 트랜잭션 없음, 외부 I/O 없음.
 */
@Service
@RequiredArgsConstructor
public class LabelGenerator {

    private static final Logger logger = LoggerFactory.getLogger(LabelGenerator.class);
    private static final SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final HostOrderRepository hostOrderRepository;
    private final HostOrderItemRepository hostOrderItemRepository;
    private final InventoryItemMasterRepository itemMasterRepository;
    private final PalletBoxRepository palletBoxRepository;
    private final PalletProgressService palletProgressService;

    /**
     * 파렛트 라벨 페이로드 — 호스트 오더 메타 + 아이템 내역 + 파렛트 바코드.
     */
    public Map<String, Object> buildPalletLabel(String pallet) {
        // 입력 검증
        requireNotEmpty(pallet, "INVALID_PARAMETER", "pallet required");

        // 박스·호스트오더·아이템 로드
        List<TbWcsPalletBox> boxes = palletProgressService.listByPallet(pallet);
        requireFound(boxes, "PALLET_NOT_FOUND", pallet + " 파렛트 정보를 찾을 수 없습니다.");

        TbWcsHostOrder h = hostOrderRepository.findByHostOrderKey(boxes.get(0).getHostOrderKey());
        List<TbWcsHostOrderItem> items = hostOrderItemRepository.findByHostOrderKey(
                ValueUtil.isEmpty(h) ? null : h.getHostSystemCode(),
                boxes.get(0).getHostOrderKey());

        // 파렛트 메타
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("type", "PALLET");
        p.put("palletBarcode", pallet);
        p.put("hostOrderKey", boxes.get(0).getHostOrderKey());
        p.put("eqGroupId", ValueUtil.isEmpty(h) ? null : h.getEqGroupId());
        p.put("ownerCode", ValueUtil.isEmpty(h) ? null : h.getOwnerCode());
        p.put("testRequired", ValueUtil.isNotEmpty(h) && Boolean.TRUE.equals(h.getTestRequired()));
        p.put("inboundDate", inboundDate(h));
        p.put("boxCount", boxes.size());

        // 아이템 내역
        List<Map<String, Object>> il = new ArrayList<>();
        if (ValueUtil.isNotEmpty(items)) {
            for (TbWcsHostOrderItem it : items) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("itemCode", it.getItemCode());
                m.put("itemName", itemName(ValueUtil.isEmpty(h) ? null : h.getOwnerCode(), it.getItemCode()));
                m.put("lotNo", it.getLotNo());
                m.put("qty", it.getQty());
                m.put("uom", it.getUom());
                m.put("produceDate", d(it.getProduceDate()));
                m.put("expiryDate", d(it.getExpiryDate()));
                m.put("testRequestNo", it.getTestRequestNo());
                m.put("testNo", it.getTestNo());
                il.add(m);
            }
        }
        p.put("items", il);

        // 바코드 (code128 + QR)
        Map<String, Object> barcodes = new LinkedHashMap<>();
        barcodes.put("code128", pallet);
        barcodes.put("qr", buildQrUrl(pallet));
        p.put("barcodes", barcodes);
        return p;
    }

    /**
     * 박스 라벨 페이로드 — 박스 메타 + 수량(total/picked/remaining) + 포장 분해 + 바코드.
     * 라벨에는 박스 입고 시 수량(total_qty) 을 표기하므로 부분 출고 후에도 동일하게 출력 가능.
     */
    public Map<String, Object> buildBoxLabel(String boxId) {
        TbWcsPalletBox b = palletBoxRepository.findById(boxId);
        requireFound(b, "BOX_NOT_FOUND", boxId);

        // 호스트 오더 + 마스터 로드
        TbWcsHostOrder h = hostOrderRepository.findByHostOrderKey(b.getHostOrderKey());
        ExtTbInventoryItemMaster m = (ValueUtil.isEmpty(h) || ValueUtil.isEmpty(b.getItemCode()))
                ? null
                : itemMasterRepository.findByOwnerAndCode(h.getOwnerCode(), b.getItemCode());

        // 수량 — total/picked/remaining 셋 다 노출. 라벨 인쇄 수량은 total_qty
        int totalQty  = ValueUtil.isEmpty(b.getTotalQty())  ? 0 : b.getTotalQty();
        int pickedQty = ValueUtil.isEmpty(b.getPickedQty()) ? 0 : b.getPickedQty();
        int remainingQty = b.calcRemainingQty();

        // 박스 메타
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("type", "BOX");
        p.put("boxId", b.getId());
        p.put("boxBarcode", b.getBoxBarcode());
        p.put("palletBarcode", b.getPalletBarcode());
        p.put("boxSeq", b.getBoxSeq());
        p.put("itemCode", b.getItemCode());
        p.put("itemName", ValueUtil.isEmpty(m) ? null : m.getItemName());
        p.put("lotNo", b.getLotNo());
        p.put("totalQty", totalQty);
        p.put("pickedQty", pickedQty);
        p.put("remainingQty", remainingQty);
        p.put("qty", totalQty);
        p.put("uom", b.getUom());
        p.put("produceDate", d(b.getProduceDate()));
        p.put("expiryDate", d(b.getExpiryDate()));
        p.put("testRequestNo", b.getTestRequestNo());
        p.put("testNo", b.getTestNo());
        p.put("inboundDate", inboundDate(h));
        p.put("boxStatus", b.getBoxStatus());

        // 포장 단위 분해 — total_qty 기준
        int[] decomp = decomposeQty(totalQty, ValueUtil.isEmpty(m) ? null : m.getBoxQty());
        Map<String, Object> decompMap = new LinkedHashMap<>();
        decompMap.put("box", decomp[0]);
        decompMap.put("ea",  decomp[1]);
        decompMap.put("perBox", ValueUtil.isEmpty(m) ? null : m.getBoxQty());
        decompMap.put("text", formatDecompositionText(decomp));
        p.put("decomposition", decompMap);

        // 바코드 — code128 + QR. 라벨의 양쪽 모서리에 배치되어 동시 인식 방지. QR 콘텐츠는 박스 바코드 텍스트만
        Map<String, Object> barcodes = new LinkedHashMap<>();
        barcodes.put("code128", b.getBoxBarcode());
        barcodes.put("qr",      buildQrUrl(b.getBoxBarcode()));
        p.put("barcodes", barcodes);
        return p;
    }

    /**
     * QR 콘텐츠 = vue-ui-operator 화면 URL.
     */
    private String buildQrUrl(String code) {
        ServletRequestAttributes a = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (a == null) return code;
        HttpServletRequest r = a.getRequest();
        return r.getScheme() + "://" + r.getServerName() + ":" + r.getServerPort() + "/operator/#/qr/" + code;
    }

    /**
     * 파렛트의 모든 박스 라벨 페이로드.
     */
    public Map<String, Object> buildAllBoxLabels(String pallet) {
        List<Map<String, Object>> labels = new ArrayList<>();
        for (TbWcsPalletBox b : palletProgressService.listByPallet(pallet)) {
            labels.add(buildBoxLabel(b.getId()));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("palletBarcode", pallet);
        out.put("boxCount", labels.size());
        out.put("labels", labels);
        return out;
    }

    /**
     * 파렛트 ZPL.
     */
    public String buildPalletZpl(String pallet) {
        return ZplLabelRenderer.renderPallet(buildPalletLabel(pallet), pallet);
    }

    /**
     * 박스 ZPL.
     */
    public String buildBoxZpl(String boxId) {
        return ZplLabelRenderer.renderBox(buildBoxLabel(boxId));
    }

    /**
     * 파렛트 PDF.
     */
    public byte[] buildPalletPdf(String pallet) {
        return PdfLabelRenderer.renderPallet(buildPalletLabel(pallet), pallet);
    }

    /**
     * 박스 PDF.
     */
    public byte[] buildBoxPdf(String boxId) {
        return PdfLabelRenderer.renderBox(buildBoxLabel(boxId));
    }

    /**
     * 박스 PDF — 포장 단위(박스/낱개) 분리 출력.
     */
    public byte[] buildBoxUnitPdf(String boxId) {
        return PdfLabelRenderer.renderBoxUnits(buildBoxLabel(boxId));
    }

    /**
     * 파렛트의 모든 박스 PDF (한 파일).
     */
    public byte[] buildAllBoxesPdf(String pallet) {
        List<TbWcsPalletBox> boxes = palletProgressService.listByPallet(pallet);
        requireFound(boxes, "PALLET_NOT_FOUND", pallet);
        List<Map<String, Object>> labels = new ArrayList<>(boxes.size());
        for (TbWcsPalletBox b : boxes) labels.add(buildBoxLabel(b.getId()));
        return PdfLabelRenderer.renderAllBoxes(labels);
    }

    /**
     * 수량을 박스/낱개로 그리디 분해.
     * perBox 가 비거나 0 이하이면 전부 낱개로 처리.
     */
    public static int[] decomposeQty(int totalQty, Integer perBox) {
        if (totalQty <= 0) return new int[]{0, 0};
        int rem = totalQty;
        int box = 0;
        int pB = (ValueUtil.isEmpty(perBox) || perBox <= 0) ? 0 : perBox;
        if (pB > 0) {
            box = rem / pB;
            rem -= box * pB;
        }
        return new int[]{ box, rem };
    }

    /**
     * 분해 결과를 사람이 읽는 텍스트로.
     */
    public static String formatDecompositionText(int[] decomp) {
        if (ValueUtil.isEmpty(decomp) || decomp.length < 2) return "";
        return String.format("박스 %d개, 낱개 %d개", decomp[0], decomp[1]);
    }

    /**
     * PdfLabelRenderer 의 단위 라벨용 직렬화 헬퍼.
     */
    static String toJson(Map<String, Object> payload) {
        try {
            Map<String, Object> snap = new LinkedHashMap<>(payload);
            snap.remove("barcodes");
            return JSON.writeValueAsString(snap);
        } catch (Exception e) {
            logger.warn("[ Label ] toJson fallback", e);
            return String.valueOf(payload.get("boxBarcode"));
        }
    }

    /**
     * 입고일자 텍스트 — receivedAt 비면 오늘 날짜.
     */
    private String inboundDate(TbWcsHostOrder h) {
        if (ValueUtil.isEmpty(h) || ValueUtil.isEmpty(h.getReceivedAt())) return d(new Date());
        return d(Date.from(h.getReceivedAt().toInstant()));
    }

    /**
     * (owner, sku) 로 마스터 조회 → 품명 반환.
     */
    private String itemName(String owner, String sku) {
        if (ValueUtil.isEmpty(sku)) return null;
        ExtTbInventoryItemMaster m = itemMasterRepository.findByOwnerAndCode(owner, sku);
        return ValueUtil.isEmpty(m) ? null : m.getItemName();
    }

    /**
     * Date → yyyy-MM-dd 문자열.
     */
    static String d(Date x) { return ValueUtil.isEmpty(x) ? null : DF.format(x); }
}
