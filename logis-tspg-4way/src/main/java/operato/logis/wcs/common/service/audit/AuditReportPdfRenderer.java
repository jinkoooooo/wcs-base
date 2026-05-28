package operato.logis.wcs.common.service.audit;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** 감사 이력 PDF 보고서 렌더러 — OpenPDF, A4 가로, 표 형식. 입력은 조회 SQL row(Map). */
public final class AuditReportPdfRenderer {

    private static final Logger logger = LoggerFactory.getLogger(AuditReportPdfRenderer.class);

    private static final String CJK_FONT_NAME = "HYGoThic-Medium";
    private static final String CJK_FONT_ENCODING = "UniKS-UCS2-H";

    private static final Color HEAD_BG = new Color(230, 230, 230);

    private AuditReportPdfRenderer() {}

    /** 조회 결과 row 들을 PDF 바이트로 렌더. */
    public static byte[] render(List<Map<String, Object>> rows) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 24, 24, 28, 28);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont = cjkFont(16, Font.BOLD);
            Font metaFont = cjkFont(9, Font.NORMAL);
            Font headFont = cjkFont(9, Font.BOLD);
            Font cellFont = cjkFont(8, Font.NORMAL);

            // 제목
            Paragraph title = new Paragraph("감사 이력 보고서", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);
            doc.add(new Paragraph(" ", metaFont));

            // 메타 (생성시각·건수)
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            int count = rows == null ? 0 : rows.size();
            doc.add(new Paragraph("생성일시: " + fmt.format(new Date()), metaFont));
            doc.add(new Paragraph("총 건수: " + count + " 건", metaFont));
            doc.add(new Paragraph(" ", metaFont));

            // 표 — 일시 / 행위자 / 동작 / 대상 / 변경 항목 / 사유
            PdfPTable table = new PdfPTable(new float[]{2.0f, 1.8f, 1.0f, 3.0f, 2.4f, 2.0f});
            table.setWidthPercentage(100);
            table.setHeaderRows(1);
            header(table, headFont, "일시", "행위자", "동작", "대상", "변경 항목", "사유");

            if (ValueUtil.isNotEmpty(rows)) {
                for (Map<String, Object> row : rows) {
                    cell(table, cellFont, formatDate(fmt, row.get("created_at")));
                    cell(table, cellFont, actorText(row));
                    cell(table, cellFont, str(row.get("action")));
                    cell(table, cellFont, targetText(row));
                    cell(table, cellFont, str(row.get("changed_columns")));
                    cell(table, cellFont, str(row.get("reason")));
                }
            }
            doc.add(table);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            logger.error("[ Audit ][ Report ] render failed - rows={}", rows == null ? 0 : rows.size(), e);
            throw new ElidomRuntimeException("AUDIT_REPORT_PDF_FAILED", "감사 이력 PDF 생성 실패", e);
        }
    }

    /** 행위자 표시 — "유형 / 표시명". */
    private static String actorText(Map<String, Object> row) {
        String type = str(row.get("actor_type"));
        String name = ValueUtil.isNotEmpty(row.get("actor_name")) ? str(row.get("actor_name")) : str(row.get("actor_id"));
        if (ValueUtil.isEmpty(name)) return type;
        return ValueUtil.isEmpty(type) ? name : type + " / " + name;
    }

    /** 대상 표시 — "테이블 / pk". */
    private static String targetText(Map<String, Object> row) {
        String table = str(row.get("table_name"));
        String pk = str(row.get("pk_value"));
        if (ValueUtil.isEmpty(pk)) return table;
        return table + "\n" + pk;
    }

    /** 일시 포맷. Date 면 포맷, 아니면 문자열. */
    private static String formatDate(SimpleDateFormat fmt, Object v) {
        if (ValueUtil.isEmpty(v)) return "";
        if (v instanceof Date d) return fmt.format(d);
        return v.toString();
    }

    /** null/empty 안전 변환. */
    private static String str(Object v) {
        return ValueUtil.isEmpty(v) ? "" : v.toString();
    }

    /** 표 헤더 행 추가. */
    private static void header(PdfPTable t, Font f, String... cols) {
        for (String col : cols) {
            PdfPCell c = new PdfPCell(new Phrase(col, f));
            c.setPadding(4);
            c.setBackgroundColor(HEAD_BG);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            t.addCell(c);
        }
    }

    /** 표 데이터 셀 추가. */
    private static void cell(PdfPTable t, Font f, String value) {
        PdfPCell c = new PdfPCell(new Phrase(value, f));
        c.setPadding(3);
        t.addCell(c);
    }

    /** CJK 폰트 로드. 실패 시 Helvetica 로 fallback. */
    private static Font cjkFont(int size, int style) {
        try {
            BaseFont bf = BaseFont.createFont(CJK_FONT_NAME, CJK_FONT_ENCODING, BaseFont.NOT_EMBEDDED);
            return new Font(bf, size, style, Color.BLACK);
        } catch (Exception e) {
            logger.warn("[ Audit ][ Report ] CJK font fallback to Helvetica", e);
            return new Font(Font.HELVETICA, size, style, Color.BLACK);
        }
    }
}
