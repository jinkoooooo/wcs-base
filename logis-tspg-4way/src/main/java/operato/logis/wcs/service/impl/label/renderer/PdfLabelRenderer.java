package operato.logis.wcs.service.impl.label.renderer;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.Barcode128;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PDF 라벨 렌더러 — OpenPDF + ZXing.
 *
 * 4x6 인치 라벨 = 288 x 432 pt @ 72dpi.
 *
 * 라벨 디자인 — QR / Code128 분리 배치:
 *   - QR 좌상단 ~200x200pt (인식률 향상)
 *   - Code128 우하단 (QR 과 물리적으로 반대편 모서리)
 *   - 두 바코드 사이 텍스트 영역으로 BCR 동시 인식 방지
 *   - QR 콘텐츠는 박스 바코드 텍스트만 (JSON 제거 — 인식률 향상)
 */
public final class PdfLabelRenderer {

    private static final Logger logger = LoggerFactory.getLogger(PdfLabelRenderer.class);

    private static final String CJK_FONT_NAME = "HYGoThic-Medium";
    private static final String CJK_FONT_ENCODING = "UniKS-UCS2-H";

    // 라벨 크기 — 4x6 인치
    private static final float LABEL_W = 288f;
    private static final float LABEL_H = 432f;

    // 라벨 디자인 좌표 (pt)
    private static final float QR_X      = 14f;
    private static final float QR_Y      = LABEL_H - 14f - 130f;
    private static final float QR_SIZE   = 130f;

    private static final float CODE128_X = 14f;
    private static final float CODE128_Y = 14f;
    private static final float CODE128_W = 260f;
    private static final float CODE128_H = 50f;

    private static final float TEXT_X    = QR_X + QR_SIZE + 10f;
    private static final float TEXT_TOP  = LABEL_H - 14f;

    /** 포장 단위 코드 — 박스/낱개. */
    enum PackUnit { BOX, EA }

    private PdfLabelRenderer() {}

    /**
     * 파렛트 라벨 PDF 바이트 생성.
     */
    @SuppressWarnings("unchecked")
    public static byte[] renderPallet(Map<String, Object> data, String pallet) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) data.getOrDefault("items", List.of());
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Rectangle labelSize = new Rectangle(LABEL_W, LABEL_H);
            Document doc = new Document(labelSize, 14, 14, 14, 14);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont = cjkFont(14, Font.BOLD);
            Font labelFont = cjkFont(9, Font.NORMAL);
            Font valueFont = cjkFont(9, Font.BOLD);

            // 타이틀
            doc.add(centered("파렛트 라벨", titleFont));
            doc.add(new Paragraph(" ", labelFont));

            // 메타 정보 표
            PdfPTable info = new PdfPTable(2);
            info.setWidths(new float[]{1f, 2.2f});
            info.setWidthPercentage(100);
            addRow(info, "주문 키",   safe(data.get("hostOrderKey")), labelFont, valueFont);
            addRow(info, "설비 그룹", safe(data.get("eqGroupId")),    labelFont, valueFont);
            addRow(info, "입고일자",  safe(data.get("inboundDate")),  labelFont, valueFont);
            addRow(info, "박스 수",   safe(data.get("boxCount")),     labelFont, valueFont);
            addRow(info, "시험 여부",
                    Boolean.TRUE.equals(data.get("testRequired")) ? "시험 대상" : "미대상",
                    labelFont, valueFont);
            doc.add(info);

            // 품목 내역 표
            if (ValueUtil.isNotEmpty(items)) {
                doc.add(new Paragraph(" ", labelFont));
                doc.add(new Paragraph("품목 내역", titleFont));
                PdfPTable itemTbl = new PdfPTable(new float[]{1.4f, 1.4f, 0.8f, 1.2f, 1.2f, 1.4f});
                itemTbl.setWidthPercentage(100);
                addHeader(itemTbl, valueFont, "코드", "품명", "LOT", "수량", "제조일", "유효기간");
                for (Map<String, Object> it : items) {
                    addItemRow(itemTbl, labelFont,
                            safe(it.get("itemCode")),
                            safe(it.get("itemName")),
                            safe(it.get("lotNo")),
                            safe(it.get("qty")) + " " + safe(it.get("uom")),
                            safe(it.get("produceDate")),
                            safe(it.get("expiryDate")));
                }
                doc.add(itemTbl);
            }

            // Code128 (하단 flow)
            doc.add(new Paragraph(" ", labelFont));
            Barcode128 c128 = new Barcode128();
            c128.setCode(pallet);
            c128.setBarHeight(48f);
            doc.add(c128.createImageWithBarcode(writer.getDirectContent(), null, null));

            // QR (우측 정렬)
            try {
                Image qrImg = buildQrImage(pallet, 200);
                qrImg.setAlignment(Element.ALIGN_RIGHT);
                doc.add(qrImg);
            } catch (Exception e) {
                logger.warn("[ Label ][ Pdf ] pallet QR failed", e);
            }

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            logger.error("[ Label ][ Pdf ] renderPallet failed - pallet={}", pallet, e);
            throw new ElidomRuntimeException("LABEL_PDF_FAILED", "PDF 생성 실패", e);
        }
    }

    /**
     * 박스 라벨 PDF 바이트 생성 — 단일 페이지.
     */
    public static byte[] renderBox(Map<String, Object> data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Rectangle labelSize = new Rectangle(LABEL_W, LABEL_H);
            Document doc = new Document(labelSize, 0, 0, 0, 0);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            doc.open();
            renderBoxPageAbsolute(writer, data);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            logger.error("[ Label ][ Pdf ] renderBox failed - boxId={}", data.get("boxId"), e);
            throw new ElidomRuntimeException("LABEL_PDF_FAILED", "PDF 생성 실패", e);
        }
    }

    /**
     * 박스 1매를 포장 단위별로 분리 출력 — 박스 N장 + 낱개 1장.
     * 마스터 단위 미정의 시 단일 라벨로 fallback.
     */
    @SuppressWarnings("unchecked")
    public static byte[] renderBoxUnits(Map<String, Object> data) {
        Map<String, Object> decomp = (Map<String, Object>) data.getOrDefault("decomposition", Map.of());
        int box = asInt(decomp.get("box"));
        int ea  = asInt(decomp.get("ea"));
        Integer perBox = asIntOrNull(decomp.get("perBox"));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Rectangle labelSize = new Rectangle(LABEL_W, LABEL_H);
            Document doc = new Document(labelSize, 0, 0, 0, 0);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            doc.open();

            boolean any = false;

            // 박스 단위 N장
            for (int i = 0; i < box; i++) {
                if (any) doc.newPage();
                renderBoxPageAbsolute(writer, unitLabel(data, PackUnit.BOX, (i + 1) + "/" + box, perBox));
                any = true;
            }

            // 낱개 1장
            if (ea > 0) {
                if (any) doc.newPage();
                renderBoxPageAbsolute(writer, unitLabel(data, PackUnit.EA, "1/1", ea));
                any = true;
            }

            // 단위 분해 불가 — 원본 라벨로
            if (!any) renderBoxPageAbsolute(writer, data);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            logger.error("[ Label ][ Pdf ] renderBoxUnits failed - boxId={}", data.get("boxId"), e);
            throw new ElidomRuntimeException("LABEL_PDF_FAILED", "PDF 생성 실패", e);
        }
    }

    /**
     * 파렛트의 모든 박스 라벨을 한 PDF 로 결합.
     */
    public static byte[] renderAllBoxes(List<Map<String, Object>> boxLabels) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Rectangle labelSize = new Rectangle(LABEL_W, LABEL_H);
            Document doc = new Document(labelSize, 0, 0, 0, 0);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            doc.open();
            for (int i = 0; i < boxLabels.size(); i++) {
                if (i > 0) doc.newPage();
                renderBoxPageAbsolute(writer, boxLabels.get(i));
            }
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            logger.error("[ Label ][ Pdf ] renderAllBoxes failed", e);
            throw new ElidomRuntimeException("LABEL_PDF_FAILED", "PDF 생성 실패", e);
        }
    }

    /**
     * 박스 1페이지를 절대 좌표로 렌더. QR 좌상단·Code128 좌하단 분리 배치.
     */
    @SuppressWarnings("unchecked")
    private static void renderBoxPageAbsolute(PdfWriter writer, Map<String, Object> data) throws Exception {
        PdfContentByte cb = writer.getDirectContent();

        Font titleFont = cjkFont(13, Font.BOLD);
        Font labelFont = cjkFont(8, Font.NORMAL);
        Font valueFont = cjkFont(9, Font.BOLD);
        Font bigQtyFont = cjkFont(18, Font.BOLD);
        Font smallFont = cjkFont(7, Font.NORMAL);

        String boxBarcode = safe(data.get("boxBarcode"));
        String unit = safe(data.get("unit"));
        String unitSeq = safe(data.get("unitSeq"));

        // 헤더 (상단 중앙)
        String title = ValueUtil.isEmpty(unit) ? "박스 라벨" : unitTitleKo(unit) + " " + unitSeq;
        drawCenteredText(cb, title, LABEL_W / 2, LABEL_H - 18, titleFont);

        // QR 코드 (좌상단) — 콘텐츠는 박스 바코드 텍스트만
        try {
            Image qr = buildQrImage(boxBarcode, 220);
            qr.setAbsolutePosition(QR_X, QR_Y);
            qr.scaleAbsolute(QR_SIZE, QR_SIZE);
            cb.addImage(qr);
        } catch (Exception e) {
            logger.warn("[ Label ][ Pdf ] box QR failed", e);
        }

        // QR 아래 작은 텍스트
        drawText(cb, boxBarcode, QR_X, QR_Y - 10, smallFont);

        // 텍스트 정보 영역 (QR 오른쪽)
        float ty = TEXT_TOP - 28;
        float lineGap = 14f;

        // SEQ
        drawText(cb, "SEQ #" + safe(data.get("boxSeq")), TEXT_X, ty, valueFont);
        ty -= lineGap;

        // ITEM
        drawText(cb, "ITEM", TEXT_X, ty, labelFont);
        ty -= 11;
        drawText(cb, safe(data.get("itemCode")), TEXT_X, ty, valueFont);
        ty -= 12;
        String itemName = safe(data.get("itemName"));
        if (ValueUtil.isNotEmpty(itemName)) {
            drawText(cb, truncate(itemName, 18), TEXT_X, ty, labelFont);
            ty -= 13;
        }

        // LOT
        drawText(cb, "LOT", TEXT_X, ty, labelFont);
        ty -= 11;
        drawText(cb, safe(data.get("lotNo")), TEXT_X, ty, valueFont);
        ty -= 18;

        // 수량 (total/picked/remaining)
        Object totalQty     = ValueUtil.isNotEmpty(data.get("totalQty")) ? data.get("totalQty") : data.get("qty");
        Object pickedQty    = data.get("pickedQty");
        Object remainingQty = data.get("remainingQty");
        String uom = safe(data.get("uom"));

        drawText(cb, "QTY (잔여/전체)", TEXT_X, ty, labelFont);
        ty -= 18;
        String qtyText;
        if (ValueUtil.isNotEmpty(pickedQty) && ValueUtil.isNotEmpty(remainingQty) && asInt(pickedQty) > 0) {
            qtyText = safe(remainingQty) + " / " + safe(totalQty) + " " + uom;
        } else {
            qtyText = safe(totalQty) + " " + uom;
        }
        drawText(cb, qtyText, TEXT_X, ty, bigQtyFont);

        // 단위 표기 (분해 정보)
        if (ValueUtil.isNotEmpty(unit)) {
            ty -= 14;
            Object unitQty = data.get("unitQty");
            String unitLabelText = ValueUtil.isEmpty(unitQty) ? unitTitleKo(unit) : unitTitleKo(unit) + " (" + unitQty + " EA)";
            drawText(cb, unitLabelText, TEXT_X, ty, smallFont);
        }

        // 박스 분해 표시 (중단)
        Map<String, Object> decomp = (Map<String, Object>) data.getOrDefault("decomposition", Map.of());
        String decompText = safe(decomp.get("text"));
        float midY = LABEL_H - QR_SIZE - 50;
        if (ValueUtil.isNotEmpty(decompText)) {
            drawCenteredText(cb, decompText, LABEL_W / 2, midY, valueFont);
            midY -= 14;
        }

        // 박스 상세 정보 (중단 영역)
        float infoY = midY - 12;
        drawText(cb, "PALLET", QR_X, infoY, labelFont);
        drawText(cb, safe(data.get("palletBarcode")), QR_X + 50, infoY, valueFont);
        infoY -= 13;
        drawText(cb, "MFG", QR_X, infoY, labelFont);
        drawText(cb, safe(data.get("produceDate")), QR_X + 50, infoY, valueFont);
        drawText(cb, "EXP", QR_X + 140, infoY, labelFont);
        drawText(cb, safe(data.get("expiryDate")), QR_X + 175, infoY, valueFont);
        infoY -= 13;
        drawText(cb, "INBOUND", QR_X, infoY, labelFont);
        drawText(cb, safe(data.get("inboundDate")), QR_X + 50, infoY, valueFont);
        String trNo = safe(data.get("testRequestNo"));
        if (ValueUtil.isNotEmpty(trNo)) {
            infoY -= 13;
            drawText(cb, "TEST_REQ", QR_X, infoY, labelFont);
            drawText(cb, trNo, QR_X + 55, infoY, valueFont);
        }

        // 구분선 (Code128 위)
        cb.setLineWidth(0.5f);
        cb.moveTo(QR_X, CODE128_Y + CODE128_H + 18);
        cb.lineTo(LABEL_W - QR_X, CODE128_Y + CODE128_H + 18);
        cb.stroke();

        // Code128 (좌하단, QR 과 물리적 반대편)
        Barcode128 c128 = new Barcode128();
        c128.setCode(boxBarcode);
        c128.setBarHeight(CODE128_H);
        c128.setX(1.2f);
        Image c128Img = c128.createImageWithBarcode(cb, null, null);
        c128Img.setAbsolutePosition(CODE128_X, CODE128_Y);
        c128Img.scaleAbsolute(CODE128_W, CODE128_H);
        cb.addImage(c128Img);

        // Code128 아래 텍스트
        drawText(cb, boxBarcode, CODE128_X, CODE128_Y - 9, smallFont);
    }

    /**
     * 단위 라벨용 페이로드 — 원본 복제 후 unit / unitSeq / unitQty 주입.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> unitLabel(Map<String, Object> base, PackUnit unit,
                                                 String seq, Integer unitQty) {
        Map<String, Object> copy = new LinkedHashMap<>(base);
        copy.put("unit", unit.name());
        copy.put("unitSeq", seq);
        if (ValueUtil.isNotEmpty(unitQty)) copy.put("unitQty", unitQty);
        return copy;
    }

    /**
     * 단위 코드 → 한글 라벨.
     */
    private static String unitTitleKo(String unit) {
        return switch (unit) {
            case "BOX" -> "박스";
            case "EA"  -> "낱개";
            default    -> unit;
        };
    }

    /**
     * 좌측 정렬 텍스트.
     */
    private static void drawText(PdfContentByte cb, String text, float x, float y, Font font) {
        if (ValueUtil.isEmpty(text)) return;
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase(text, font), x, y, 0);
    }

    /**
     * 중앙 정렬 텍스트.
     */
    private static void drawCenteredText(PdfContentByte cb, String text, float x, float y, Font font) {
        if (ValueUtil.isEmpty(text)) return;
        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                new Phrase(text, font), x, y, 0);
    }

    /**
     * 텍스트 잘라내기 — max 길이 초과 시 …로 마무리.
     */
    private static String truncate(String s, int max) {
        if (ValueUtil.isEmpty(s) || s.length() <= max) return s;
        return s.substring(0, max - 1) + "…";
    }

    /**
     * Object → String 안전 변환. null/empty → 빈 문자열.
     */
    private static String safe(Object o) {
        if (ValueUtil.isEmpty(o)) return "";
        return String.valueOf(o);
    }

    /**
     * Object → int 안전 변환. 실패 시 0.
     */
    private static int asInt(Object v) {
        if (ValueUtil.isEmpty(v)) return 0;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return 0; }
    }

    /**
     * Object → Integer 안전 변환. 실패 시 null.
     */
    private static Integer asIntOrNull(Object v) {
        if (ValueUtil.isEmpty(v)) return null;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return null; }
    }

    /**
     * ZXing 으로 QR PNG 생성 → OpenPDF Image. ECC Level Q + margin 2 — 인식률 향상.
     */
    private static Image buildQrImage(String content, int pixelSize) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q);
        hints.put(EncodeHintType.MARGIN, 2);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        BitMatrix matrix = new QRCodeWriter().encode(
                ValueUtil.isEmpty(content) ? "" : content, BarcodeFormat.QR_CODE, pixelSize, pixelSize, hints);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", png);
        return Image.getInstance(png.toByteArray());
    }

    /**
     * CJK 폰트 로드. 실패 시 Helvetica 로 fallback.
     */
    private static Font cjkFont(int size, int style) {
        try {
            BaseFont bf = BaseFont.createFont(CJK_FONT_NAME, CJK_FONT_ENCODING, BaseFont.NOT_EMBEDDED);
            return new Font(bf, size, style, Color.BLACK);
        } catch (Exception e) {
            logger.warn("[ Label ][ Pdf ] CJK font fallback to Helvetica", e);
            return new Font(Font.HELVETICA, size, style, Color.BLACK);
        }
    }

    /**
     * 중앙 정렬 Paragraph.
     */
    private static Paragraph centered(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    /**
     * 라벨/값 한 행 추가.
     */
    private static void addRow(PdfPTable t, String label, String value, Font lf, Font vf) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, lf));
        c1.setPadding(3);
        c1.setBackgroundColor(new Color(245, 245, 245));
        t.addCell(c1);
        PdfPCell c2 = new PdfPCell(new Phrase(ValueUtil.isEmpty(value) ? "" : value, vf));
        c2.setPadding(3);
        t.addCell(c2);
    }

    /**
     * 표 헤더 행 추가.
     */
    private static void addHeader(PdfPTable t, Font f, String... cols) {
        for (String col : cols) {
            PdfPCell c = new PdfPCell(new Phrase(col, f));
            c.setPadding(3);
            c.setBackgroundColor(new Color(230, 230, 230));
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            t.addCell(c);
        }
    }

    /**
     * 표 데이터 행 추가.
     */
    private static void addItemRow(PdfPTable t, Font f, String... vals) {
        for (String v : vals) {
            PdfPCell c = new PdfPCell(new Phrase(ValueUtil.isEmpty(v) ? "" : v, f));
            c.setPadding(2);
            t.addCell(c);
        }
    }
}
