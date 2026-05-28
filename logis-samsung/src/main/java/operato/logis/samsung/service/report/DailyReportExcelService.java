package operato.logis.samsung.service.report;

import lombok.RequiredArgsConstructor;
import operato.logis.samsung.dto.report.request.DailyReportRawRequest;
import operato.logis.samsung.dto.report.request.DailyReportSearchRequest;
import operato.logis.samsung.dto.report.response.DailyHourlyUphRowDto;
import operato.logis.samsung.dto.report.response.DailyNgDetailRowDto;
import operato.logis.samsung.dto.report.response.DailyPalletExchangeRowDto;
import operato.logis.samsung.dto.report.response.DailyRawRowDto;
import operato.logis.samsung.dto.report.response.DailyReportSummaryDto;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DailyReportExcelService {

    private final DailyReportQueryService dailyReportQueryService;

    /**
     * 일별 리포트 엑셀 추출
     */
    public byte[] exportDailyReport(DailyReportSearchRequest request) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            // -----------------------------------------------------------------
            // 데이터 조회
            // -----------------------------------------------------------------
            DailyReportSummaryDto summary = dailyReportQueryService.getDailySummary(request);
            List<DailyHourlyUphRowDto> hourlyRows = dailyReportQueryService.getDailyHourlyUphRows(request);
            List<DailyPalletExchangeRowDto> exchangeRows = dailyReportQueryService.getDailyPalletExchangeRows(request);
            List<DailyNgDetailRowDto> ngRows = dailyReportQueryService.getDailyNgDetailRows(request);

            DailyReportRawRequest bcrReq = createRawRequest(request, "BCR");
            DailyReportRawRequest sorterReq = createRawRequest(request, "SORTER");
            DailyReportRawRequest palletizedReq = createRawRequest(request, "PALLETIZED");

            List<DailyRawRowDto> bcrRows = dailyReportQueryService.getDailyRawRows(bcrReq);
            List<DailyRawRowDto> sorterRows = dailyReportQueryService.getDailyRawRows(sorterReq);
            List<DailyRawRowDto> palletizedRows = dailyReportQueryService.getDailyRawRows(palletizedReq);

            // -----------------------------------------------------------------
            // 스타일 생성
            // -----------------------------------------------------------------
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle textStyle = createTextStyle(workbook);
            CellStyle wrapStyle = createWrapStyle(workbook);

            // -----------------------------------------------------------------
            // 시트 생성 순서
            // -----------------------------------------------------------------
            Sheet hourlySheet = workbook.createSheet("시간대별 UPH");
            writeHourlySheet(hourlySheet, hourlyRows, headerStyle, textStyle);

            Sheet palletExchangeSheet = workbook.createSheet("파렛트별운영시간");
            writePalletExchangeSheet(palletExchangeSheet, exchangeRows, headerStyle, textStyle);

            Sheet ngDetailSheet = workbook.createSheet("NG 상세");
            writeNgDetailSheet(ngDetailSheet, ngRows, headerStyle, textStyle);

            Sheet summarySheet = workbook.createSheet("최종요약");
            writeSummarySheet(summarySheet, summary, headerStyle, textStyle, wrapStyle);

            Sheet bcrSheet = workbook.createSheet("BCR_Rows");
            writeRawSheet(bcrSheet, bcrRows, headerStyle, textStyle);

            Sheet sorterSheet = workbook.createSheet("SORTED_Rows");
            writeRawSheet(sorterSheet, sorterRows, headerStyle, textStyle);

            Sheet palletizedSheet = workbook.createSheet("PALLETIZED_Rows");
            writeRawSheet(palletizedSheet, palletizedRows, headerStyle, textStyle);

            workbook.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("일별 리포트 엑셀 생성 실패", e);
        }
    }

    /**
     * Raw 조회용 request 생성
     */
    private DailyReportRawRequest createRawRequest(DailyReportSearchRequest request, String processType) {
        DailyReportRawRequest rawRequest = new DailyReportRawRequest();
        rawRequest.setTodayDate(request.getTodayDate());
        rawRequest.setBlNo(request.getBlNo());
        rawRequest.setCntrNo(request.getCntrNo());
        rawRequest.setProcessType(processType);
        return rawRequest;
    }

    /**
     * 시간대별 UPH 시트
     */
    private void writeHourlySheet(
            Sheet sheet,
            List<DailyHourlyUphRowDto> rows,
            CellStyle headerStyle,
            CellStyle textStyle
    ) {
        String[] headers = {
                "hour_slot",
                "시간내첫박스시작시간",
                "시간내마지막박스종료시간",
                "시간내박스처리량",
                "ok_qty",
                "ng_qty",
                "실수행시간별uph"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (DailyHourlyUphRowDto row : rows) {
            Row dataRow = sheet.createRow(rowIdx++);
            setCell(dataRow, 0, row.getHourSlot(), textStyle);
            setCell(dataRow, 1, row.getFirstBoxStartAtInHour(), textStyle);
            setCell(dataRow, 2, row.getLastBoxEndAtInHour(), textStyle);
            setCell(dataRow, 3, row.getBoxQtyInHour(), textStyle);
            setCell(dataRow, 4, row.getOkQty(), textStyle);
            setCell(dataRow, 5, row.getNgQty(), textStyle);
            setCell(dataRow, 6, row.getEffectiveUph(), textStyle);
        }

        setWidths(sheet, 22, 24, 26, 18, 10, 10, 16);
    }

    /**
     * 파렛트교체시간 시트
     */
    private void writePalletExchangeSheet(
            Sheet sheet,
            List<DailyPalletExchangeRowDto> rows,
            CellStyle headerStyle,
            CellStyle textStyle
    ) {
        String[] headers = {
                "pallet_id",
                "start_pallet_sequence",
                "start_at",
                "end_pallet_sequence",
                "end_at",
                "operating_seconds",
                "operating_time"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (DailyPalletExchangeRowDto row : rows) {
            Row dataRow = sheet.createRow(rowIdx++);
            setCell(dataRow, 0, row.getPalletId(), textStyle);
            setCell(dataRow, 1, row.getExchangePalletSequence(), textStyle);
            setCell(dataRow, 2, row.getExchangeAt(), textStyle);
            setCell(dataRow, 3, row.getEmissionPalletSequence(), textStyle);
            setCell(dataRow, 4, row.getEmissionAt(), textStyle);
            setCell(dataRow, 5, row.getExchangeSeconds(), textStyle);
            setCell(dataRow, 6, row.getExchangeTime(), textStyle);
        }

        setWidths(sheet, 14, 32, 22, 32, 22, 18, 16);
    }

    /**
     * NG 상세 시트
     */
    private void writeNgDetailSheet(
            Sheet sheet,
            List<DailyNgDetailRowDto> rows,
            CellStyle headerStyle,
            CellStyle textStyle
    ) {
        String[] headers = {
                "박스시리얼코드",
                "컨테이너번호",
                "최초입고시간",
                "작업자처리시간",
                "최종완료시간",
                "reject_type",
                "final_remark",
                "result_type"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (DailyNgDetailRowDto row : rows) {
            Row dataRow = sheet.createRow(rowIdx++);
            setCell(dataRow, 0, row.getBoxSerialCode(), textStyle);
            setCell(dataRow, 1, row.getContainerNo(), textStyle);
            setCell(dataRow, 2, row.getFirstReceivedAt(), textStyle);
            setCell(dataRow, 3, row.getManualProcessedAt(), textStyle);
            setCell(dataRow, 4, row.getFinalCompletedAt(), textStyle);
            setCell(dataRow, 5, row.getRejectType(), textStyle);
            setCell(dataRow, 6, row.getFinalRemark(), textStyle);
            setCell(dataRow, 7, row.getResultType(), textStyle);
        }

        setWidths(sheet, 26, 18, 22, 22, 22, 16, 26, 14);
    }

    /**
     * 최종요약 시트
     */
    private void writeSummarySheet(
            Sheet sheet,
            DailyReportSummaryDto summary,
            CellStyle headerStyle,
            CellStyle textStyle,
            CellStyle wrapStyle
    ) {
        Map<String, Object> summaryMap = new LinkedHashMap<>();

        summaryMap.put("total_box_qty", summary == null ? null : summary.getTotalBoxQty());
        summaryMap.put("job_sku_qty", summary == null ? null : summary.getJobSkuQty());
        summaryMap.put("actual_sku_qty", summary == null ? null : summary.getActualSkuQty());
        summaryMap.put("ok_box_qty", summary == null ? null : summary.getOkBoxQty());
        summaryMap.put("ng_box_qty", summary == null ? null : summary.getNgBoxQty());
        summaryMap.put("pending_box_qty", summary == null ? null : summary.getPendingBoxQty());

        summaryMap.put("manual_box_qty", summary == null ? null : summary.getManualBoxQty());
        summaryMap.put("job_start_dt", summary == null ? null : summary.getJobStartDt());
        summaryMap.put("job_end_dt", summary == null ? null : summary.getJobEndDt());

        summaryMap.put("first_received_at", summary == null ? null : summary.getFirstReceivedAt());
        summaryMap.put("last_palletized_at", summary == null ? null : summary.getLastPalletizedAt());

        summaryMap.put("total_operating_time", summary == null ? null : summary.getTotalOperatingTime());
        summaryMap.put("idle_time", summary == null ? null : summary.getIdleTime());
        summaryMap.put("pallet_operating_time", summary == null ? null : summary.getPalletOperatingTime());

        summaryMap.put("total_time_uph", summary == null ? null : summary.getTotalTimeUph());
        summaryMap.put("pallet_time_uph", summary == null ? null : summary.getPalletTimeUph());
        summaryMap.put("ng_rate_pct", summary == null ? null : summary.getNgRatePct());

        summaryMap.put("avg_all_time", summary == null ? null : summary.getAvgAllTime());
        summaryMap.put("median_time", summary == null ? null : summary.getMedianTime());
        summaryMap.put("p95_time", summary == null ? null : summary.getP95Time());
        summaryMap.put("avg_excl_p95_time", summary == null ? null : summary.getAvgExclP95Time());
        summaryMap.put("min_time", summary == null ? null : summary.getMinTime());
        summaryMap.put("max_time", summary == null ? null : summary.getMaxTime());

        Row headerRow = sheet.createRow(0);
        int colIdx = 0;
        for (String key : summaryMap.keySet()) {
            Cell cell = headerRow.createCell(colIdx++);
            cell.setCellValue(key);
            cell.setCellStyle(headerStyle);
        }

        Row valueRow = sheet.createRow(1);
        colIdx = 0;
        for (Object value : summaryMap.values()) {
            Cell cell = valueRow.createCell(colIdx++);
            setCellValue(cell, value);
            cell.setCellStyle(textStyle);
        }

        Row summaryTextRow = sheet.createRow(3);
        summaryTextRow.setHeightInPoints(320);

        Cell summaryTextCell = summaryTextRow.createCell(0);
        summaryTextCell.setCellValue(summary == null || summary.getSummaryText() == null ? "" : summary.getSummaryText());
        summaryTextCell.setCellStyle(wrapStyle);

        sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 10));

        for (int i = 0; i < summaryMap.size(); i++) {
            sheet.setColumnWidth(i, 16 * 256);
        }
        sheet.setColumnWidth(0, 36 * 256);
    }

    /**
     * 원본 데이터 시트
     */
    private void writeRawSheet(
            Sheet sheet,
            List<DailyRawRowDto> rows,
            CellStyle headerStyle,
            CellStyle textStyle
    ) {
        if (rows == null || rows.isEmpty()) {
            Row row = sheet.createRow(0);
            Cell cell = row.createCell(0);
            cell.setCellValue("데이터 없음");
            cell.setCellStyle(textStyle);
            sheet.setColumnWidth(0, 20 * 256);
            return;
        }

        Map<String, Object> firstRowMap = convertRawRowToMap(rows.get(0));
        List<String> headers = new ArrayList<>(firstRowMap.keySet());

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (DailyRawRowDto dto : rows) {
            Map<String, Object> rowMap = convertRawRowToMap(dto);
            Row row = sheet.createRow(rowIdx++);

            for (int i = 0; i < headers.size(); i++) {
                Cell cell = row.createCell(i);
                setCellValue(cell, rowMap.get(headers.get(i)));
                cell.setCellStyle(textStyle);
            }
        }

        for (int i = 0; i < headers.size(); i++) {
            sheet.setColumnWidth(i, 18 * 256);
        }
    }

    /**
     * DailyRawRowDto -> Map 변환
     * DTO 구조에 맞게 필요시 수정
     */
    private Map<String, Object> convertRawRowToMap(DailyRawRowDto dto) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("date", dto.getDate());
        map.put("bl_no", dto.getBlNo());
        map.put("cntr_no", dto.getCntrNo());
        map.put("box_id", dto.getBoxId());
        map.put("parcel_id", dto.getParcelId());
        map.put("box_width", dto.getBoxWidth());
        map.put("box_length", dto.getBoxLength());
        map.put("box_height", dto.getBoxHeight());
        map.put("plc_seq_no", dto.getPlcSeqNo());
        map.put("item_code", dto.getItemCode());
        map.put("process_type", dto.getProcessType());
        map.put("tracking_status", dto.getTrackingStatus());
        map.put("tracking_desc", dto.getTrackingDesc());
        map.put("event_time", dto.getEventTime());
        map.put("line_id", dto.getLineId());
        map.put("equip_id", dto.getEquipId());
        map.put("final_status", dto.getFinalStatus());
        map.put("reject_type", dto.getRejectType());
        return map;
    }

    /**
     * 헤더 스타일
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }

    /**
     * 일반 텍스트 스타일
     */
    private CellStyle createTextStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * 줄바꿈 텍스트 스타일
     */
    private CellStyle createWrapStyle(Workbook workbook) {
        CellStyle style = createTextStyle(workbook);
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        return style;
    }

    /**
     * Row + Cell 값 세팅
     */
    private void setCell(Row row, int colIdx, Object value, CellStyle style) {
        Cell cell = row.createCell(colIdx);
        setCellValue(cell, value);
        cell.setCellStyle(style);
    }

    /**
     * Cell 값 세팅
     */
    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
            return;
        }

        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
            return;
        }

        cell.setCellValue(String.valueOf(value));
    }

    /**
     * 시트 폭 일괄 세팅
     */
    private void setWidths(Sheet sheet, int... widths) {
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
    }
}