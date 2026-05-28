package operato.logis.wcs.service.impl.label.renderer;

import java.util.List;
import java.util.Map;

import xyz.elidom.util.ValueUtil;

import static operato.logis.wcs.common.util.lang.CommonUtils.toInt;

/**
 * Zebra ZPL 라벨 렌더러 — 프린터가 ^CI28(UTF-8) 인식 가정.
 *
 * 4x6 인치 (812 x 1218 dots @ 203dpi) 라벨 기준.
 *
 * 라벨 디자인 — QR / Code128 분리 배치:
 *   - QR 코드: 좌상단 (크게, magnification 8, ~200x200 dots)
 *   - Code128: 우하단 (1D 바코드)
 *   - 두 바코드 사이 텍스트 영역으로 BCR 동시 인식 방지
 *   - QR 콘텐츠는 박스 바코드 텍스트만 (JSON 제거 — 인식률 향상)
 */
public final class ZplLabelRenderer {

    private static final String ZPL_HEADER = "^XA\n^CI28\n^PW812\n^LL1218\n";
    private static final String ZPL_FOOTER = "^XZ\n";

    private ZplLabelRenderer() {}

    /**
     * 파렛트 라벨 ZPL.
     */
    @SuppressWarnings("unchecked")
    public static String renderPallet(Map<String, Object> data, String pallet) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) data.getOrDefault("items", List.of());

        StringBuilder z = new StringBuilder(ZPL_HEADER);

        // 헤더 영역 (상단)
        z.append("^FO40,30^A0N,55,55^FDPALLET LABEL^FS\n");
        z.append("^FO40,100^A0N,30,30^FDHOST: ").append(safe(data.get("hostOrderKey"))).append("^FS\n");
        z.append("^FO40,140^A0N,28,28^FDEQ:   ").append(safe(data.get("eqGroupId"))).append("^FS\n");
        z.append("^FO40,178^A0N,28,28^FDDATE: ").append(safe(data.get("inboundDate"))).append("^FS\n");
        z.append("^FO40,216^A0N,28,28^FDBOXES: ").append(safe(data.get("boxCount"))).append("^FS\n");
        z.append("^FO40,254^A0N,28,28^FDTEST: ")
                .append(Boolean.TRUE.equals(data.get("testRequired")) ? "REQUIRED" : "NONE").append("^FS\n");

        // 구분선
        z.append("^FO40,300^GB740,3,3^FS\n");

        // 아이템 라인
        int y = 320;
        for (Map<String, Object> it : items) {
            z.append("^FO40,").append(y).append("^A0N,28,28^FD")
                    .append(safe(it.get("itemCode"))).append(" / ")
                    .append(safe(it.get("itemName"))).append("^FS\n");
            y += 36;
            z.append("^FO40,").append(y).append("^A0N,26,26^FDLOT: ")
                    .append(safe(it.get("lotNo"))).append("  QTY: ").append(safe(it.get("qty")))
                    .append(" ").append(safe(it.get("uom"))).append("^FS\n");
            y += 32;
            z.append("^FO40,").append(y).append("^A0N,24,24^FDMFG: ")
                    .append(safe(it.get("produceDate"))).append("  EXP: ")
                    .append(safe(it.get("expiryDate"))).append("^FS\n");
            y += 32;
            String trNo = safe(it.get("testRequestNo"));
            if (ValueUtil.isNotEmpty(trNo)) {
                z.append("^FO40,").append(y).append("^A0N,24,24^FDTEST_REQ: ")
                        .append(trNo).append("^FS\n");
                y += 32;
            }
            y += 8;
        }

        // QR 코드 (좌하단, 크게) — BQN,2,8 = model 2, magnification 8
        int qrY = 900;
        z.append("^FO40,").append(qrY).append("^BQN,2,8^FDLA,").append(pallet).append("^FS\n");

        // Code128 (우하단, QR 과 반대편) — BY3 bar width, ^BCN 정상 방향
        int codeY = qrY + 50;
        z.append("^FO440,").append(codeY).append("^BY3,2.5,100^BCN,100,Y,N,N^FD")
                .append(pallet).append("^FS\n");

        z.append(ZPL_FOOTER);
        return z.toString();
    }

    /**
     * 박스 라벨 ZPL.
     */
    @SuppressWarnings("unchecked")
    public static String renderBox(Map<String, Object> data) {
        Map<String, Object> decomp = (Map<String, Object>) data.getOrDefault("decomposition", Map.of());
        String decompText = safe(decomp.get("text"));
        String boxBarcode = safe(data.get("boxBarcode"));

        // 수량 정보 — total/picked/remaining
        Object totalQty     = data.get("totalQty");
        Object pickedQty    = data.get("pickedQty");
        Object remainingQty = data.get("remainingQty");
        if (ValueUtil.isEmpty(totalQty)) totalQty = data.get("qty");

        StringBuilder z = new StringBuilder(ZPL_HEADER);

        // 헤더
        z.append("^FO40,30^A0N,50,50^FDBOX LABEL^FS\n");
        if (ValueUtil.isNotEmpty(decompText)) {
            z.append("^FO40,90^A0N,26,26^FD").append(decompText).append("^FS\n");
        }

        // 박스 정보 영역 — QR 오른쪽 + 하단
        int textX = 310;
        int y = 140;

        z.append("^FO").append(textX).append(",").append(y)
                .append("^A0N,30,30^FDSEQ #").append(safe(data.get("boxSeq"))).append("^FS\n");
        y += 42;
        z.append("^FO").append(textX).append(",").append(y)
                .append("^A0N,26,26^FDITEM: ").append(safe(data.get("itemCode"))).append("^FS\n");
        y += 34;
        z.append("^FO").append(textX).append(",").append(y)
                .append("^A0N,24,24^FD").append(safe(data.get("itemName"))).append("^FS\n");
        y += 34;
        z.append("^FO").append(textX).append(",").append(y)
                .append("^A0N,28,28^FDLOT: ").append(safe(data.get("lotNo"))).append("^FS\n");
        y += 38;

        // 수량 — picked > 0 이면 잔여/전체 표기
        if (ValueUtil.isNotEmpty(pickedQty) && ValueUtil.isNotEmpty(remainingQty)) {
            z.append("^FO").append(textX).append(",").append(y)
                    .append("^A0N,32,32^FDQTY: ").append(safe(remainingQty))
                    .append(" / ").append(safe(totalQty))
                    .append(" ").append(safe(data.get("uom"))).append("^FS\n");
            y += 38;
            if (toInt(pickedQty) > 0) {
                z.append("^FO").append(textX).append(",").append(y)
                        .append("^A0N,22,22^FD(picked: ").append(safe(pickedQty)).append(")^FS\n");
                y += 28;
            }
        } else {
            z.append("^FO").append(textX).append(",").append(y)
                    .append("^A0N,32,32^FDQTY: ").append(safe(totalQty))
                    .append(" ").append(safe(data.get("uom"))).append("^FS\n");
            y += 40;
        }

        // QR 코드 (좌상단, 크게) — 콘텐츠는 박스 바코드만
        z.append("^FO40,140^BQN,2,8^FDLA,").append(boxBarcode).append("^FS\n");
        z.append("^FO40,360^A0N,20,20^FD").append(boxBarcode).append("^FS\n");

        // 박스 상세 정보 (하단 좌측)
        int detailY = 460;
        z.append("^FO40,").append(detailY).append("^GB740,3,3^FS\n");
        detailY += 20;
        z.append("^FO40,").append(detailY)
                .append("^A0N,26,26^FDPALLET: ").append(safe(data.get("palletBarcode"))).append("^FS\n");
        detailY += 34;
        z.append("^FO40,").append(detailY)
                .append("^A0N,24,24^FDMFG: ").append(safe(data.get("produceDate")))
                .append("  EXP: ").append(safe(data.get("expiryDate"))).append("^FS\n");
        detailY += 32;
        z.append("^FO40,").append(detailY)
                .append("^A0N,24,24^FDINBOUND: ").append(safe(data.get("inboundDate"))).append("^FS\n");
        detailY += 32;
        String trNo = safe(data.get("testRequestNo"));
        if (ValueUtil.isNotEmpty(trNo)) {
            z.append("^FO40,").append(detailY)
                    .append("^A0N,24,24^FDTEST_REQ: ").append(trNo).append("^FS\n");
            detailY += 32;
        }

        // 구분선
        z.append("^FO40,720^GB740,3,3^FS\n");

        // Code128 (우하단, QR 과 물리적으로 반대편) — BCR 동시 인식 방지
        z.append("^FO40,760^A0N,24,24^FD").append(boxBarcode).append("^FS\n");
        z.append("^FO40,800^BY3,2.5,100^BCN,100,Y,N,N^FD").append(boxBarcode).append("^FS\n");

        z.append(ZPL_FOOTER);
        return z.toString();
    }

    /**
     * ZPL 토큰 충돌 방지용 sanitize. ^ ~ 개행을 공백으로 치환.
     */
    static String safe(Object o) {
        if (ValueUtil.isEmpty(o)) return "";
        String s = String.valueOf(o);
        return s.replace('^', ' ').replace('~', ' ').replace("\n", " ");
    }

}
