package operato.logis.connector.sap.event;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import operato.logis.connector.core.event.EventDirection;
import operato.logis.connector.core.event.IntegrationEvent;
import operato.logis.connector.core.event.SystemType;
import operato.logis.connector.sap.mapper.SapMappingMetaData;
import operato.logis.connector.sap.model.SapPayload;
import operato.logis.connector.sap.service.RfcDataProcessorService;
import operato.logis.connector.sap.util.RfcFieldLoader;

@Component
public class SapTestRunner implements CommandLineRunner {

	@Autowired
	private RfcFieldLoader loader;

	@Autowired
	private RfcDataProcessorService rfcDataProcessorService;

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	/**
	 * SAP 자체 통신 시뮬레이션 테스트를 위한 클레스 함수
	 * 
	 * @param args
	 * @throws Exception
	 */
	@Override
	public void run(String... args) throws Exception {
		try {
			// 이벤트 방식 테스트
			 callZwmWesMaraSend();

			// 이벤트 방식 테스트
			// EventZwmWesBinTransferIoTest();
			// EventZwmWesBinTransferIoResultTest();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 테스트 필요시 활성화
	 */
	public void callZwmWesMaraSend() {
		// Import 파라미터 정의
		Map<String, String> importParams = new HashMap<>();
		importParams.put("I_ZWERKS", "2009");
//		SapPayload payload = new SapPayload(true, importParams, null);
//		IntegrationEvent<SapPayload> event = new IntegrationEvent<SapPayload>(7L, SystemType.SAP, EventDirection.SEND,
//				"ZWM_WES_MARA_SEND", payload);
//		eventPublisher.publishEvent(event);
//		event.getFutureResponse().thenAccept(res -> System.out.printf("SAP 응답: {}", res.getMessage()));
	}

	private void EventZwmWesBinTransferIoTest() {
		// 테이블명
		String tableName = "GT_TAB";

		String[][] raw = { { "110358", "S존" }, { "110369", "S존" }, { "112623", "S존" }, { "113458", "S존" },
				{ "113647", "S존" }, { "3100213", "A존" }, { "3100865", "A존" }, { "3101859", "A존" }, { "3102754", "A존" },
				{ "3107318", "A존" }, { "3107698", "A존" }, { "3109216", "A존" }, { "3111291", "A존" }, { "3112407", "A존" },
				{ "3115602", "A존" }, { "3115685", "A존" }, { "3302788", "A존" }, { "3303357", "A존" } };

		List<Map<String, Object>> tableRows = new ArrayList<>();
		for (String[] pair : raw) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("COM_CD", "2009");
			row.put("SKU_CD", pair[0]);
			row.put("INOUT_MODE", "1");
			row.put("REQ_DATE", "20250403");
			row.put("REQ_NO", "W002025040300003");
			row.put("TO_STATION", pair[1]);
			row.put("REQ_QTY", 10);
			tableRows.add(row);
		}
		// 호출 이벤트 생성
		Map<String, String> importParams = Map.of("I_MODE", "R", "I_ZWERKS", "2009", "I_RSDAT", "20250403");
		List<Map<String, String>> resultHolder = new ArrayList<>();
		SapPayload payload = new SapPayload(true, importParams, null);
		IntegrationEvent<SapPayload> event = new IntegrationEvent<SapPayload>(7L, SystemType.SAP, EventDirection.SEND,
				"ZWM_WES_BIN_TRANSFER_IO", payload);
		eventPublisher.publishEvent(event);
		event.getFutureResponse().thenAccept(res -> System.out.printf("SAP 응답: {}", res.getMessage()));
//		
//		// 호출 이벤트 생성
//		SapRfcCallEvent event = new SapRfcCallEvent("ZWM_WES_BIN_TRANSFER_IO", true,
//				Map.of("I_MODE", "R", "I_ZWERKS", "2009", "I_RSDAT", "20250403"), Map.of(tableName, tableRows),
//				meta -> {
//					// 결과 콜백
//					System.out.println("RFC 호출 완료");
//					System.out.println("▶ exportParams: " + meta.getExportParams());
//					System.out.println("▶ 결과 데이터: ");
//					meta.getTableDataMap().forEach((tbl, rows) -> {
//						System.out.println("테이블: " + tableName + " / 건수: " + rows.size());
//					});
//
////                    if(meta.getExportParamResult().equals("S")){
////                        // 재고이동 처리 성공일 경우 확정결과 모드"F" 함수 호출 테스트.
////                        EventZwmWesBinTransferIoResultTest();
////                    }
//				});
//
//		// 이벤트 발행
//		eventPublisher.publishEvent(event);
	}

	private void EventZwmWesBinTransferIoResultTest() {

		// 호출 이벤트 생성
		Map<String, String> importParams = Map.of("I_MODE", "F", "I_ZWERKS", "2009", "I_RSDAT", "20250403");
		List<Map<String, String>> resultHolder = new ArrayList<>();
		SapPayload payload = new SapPayload(true, importParams, null);
		IntegrationEvent<SapPayload> event = new IntegrationEvent<SapPayload>(7L, SystemType.SAP, EventDirection.SEND,
				"ZWM_WES_BIN_TRANSFER_IO", payload);
		eventPublisher.publishEvent(event);
		event.getFutureResponse().thenAccept(res -> System.out.printf("SAP 응답: {}", res.getMessage()));
//        SapRfcCallEvent event = new SapRfcCallEvent(
//                "ZWM_WES_BIN_TRANSFER_IO",
//                false,
//                Map.of(
//                        "I_MODE", "F",
//                        "I_ZWERKS", "2009",
//                        "I_RSDAT", "20250403"
//                ),
//                null,
//                result -> {
//                    // 🎯 결과 콜백 처리
//                    Map<String, List<Map<String, Object>>> tableDataMap = result.getTableDataMap();
//
//                    for (Map.Entry<String, List<Map<String, Object>>> entry : tableDataMap.entrySet()) {
//                        String tableName = entry.getKey();
//                        List<Map<String, Object>> rows = entry.getValue();
//
//                        System.out.println("테이블: " + tableName + " / 건수: " + rows.size());
//                    }
//                }
//        );

		// 이벤트 발행
		eventPublisher.publishEvent(event);
	}
/**
 * service call 방식으로 할 경우 결과에 대한 converting작업은 별도 수행 해야 함
 * @param filePath
 * @return
 * @throws Exception
 */
//	private void zvmWesMaraSendTest() throws IOException {
//		SapMappingMetaData meta = loader.loadMetaData("ZWM_WES_MARA_SEND");
//
//		Map<String, String> importParams = new HashMap<>();
//		importParams.put("I_ZWERKS", "2009");
////		importParams.put("I_MATNR", "111693"); // null 허용됨
//
//		meta.setImportParams(importParams);
//
//		rfcDataProcessorService.handleRfcCall(meta);
//
//		// 수신 데이터 저장.
//		//rfcDataProcessorService.saveResultToJson(meta);
//	}
//
//	private void zvmWesSerialSendTest() throws IOException {
//		SapMappingMetaData meta = loader.loadMetaData("ZWM_WES_SIRIAL_SEND");
//
//		Map<String, String> importParams = new HashMap<>();
//		importParams.put("I_WERKS", "2009");
//
//		meta.setImportParams(importParams);
//
//		rfcDataProcessorService.handleRfcCall(meta);
//
//		// 수신 데이터 저장.
//		rfcDataProcessorService.saveResultToJson(meta);
//	}
//
//	private void zvmWesOrderTotSumSendTest() throws IOException {
//		SapMappingMetaData meta = loader.loadMetaData("ZWM_WES_ORDER_TOTSUM_SEND");
//
//		Map<String, String> importParams = new HashMap<>();
//		// * Import parameter : 1.I_ZWERKS, 2. I_ERDAT, I_RSDAT
//		importParams.put("I_ZWERKS", "2009");
//		importParams.put("I_ERDAT", "20250402");
//		importParams.put("I_RSDAT", "20250407");
//
//		meta.setImportParams(importParams);
//
//		rfcDataProcessorService.handleRfcCall(meta);
//
//		// 수신 데이터 저장.
//		rfcDataProcessorService.saveResultToJson(meta);
//	}
//
//	private void zvmWesOrderSendTest() throws IOException {
//		SapMappingMetaData meta = loader.loadMetaData("ZWM_WES_ORDER_SEND");
//
//		Map<String, String> importParams = new HashMap<>();
//		// * Import parameter : 1.I_ZWERKS, 2. I_RSDAT, I_SEQ
//		importParams.put("I_ZWERKS", "2009");
//		importParams.put("I_RSDAT", "20250407");
//		importParams.put("I_SEQ", "01");
//
//		meta.setImportParams(importParams);
//
//		rfcDataProcessorService.handleRfcCall(meta);
//
//		// 수신 데이터 저장.
//		rfcDataProcessorService.saveResultToJson(meta);
//	}
//
//	private void zvmWesOrderConfirmTest() throws IOException {
//		SapMappingMetaData meta = loader.loadMetaData("ZWM_WES_ORDER_CONFIRM");
//
//		Map<String, String> importParams = new HashMap<>();
//		// * Import parameter : 1.I_ZWERKS, 2. I_RSDAT, I_SEQ
//		importParams.put("I_ZWERKS", "2009");
//		importParams.put("I_RSDAT", "20250407");
//		importParams.put("I_SEQ", "01");
//
//		meta.setImportParams(importParams);
//
//		rfcDataProcessorService.handleRfcCall(meta);
//
//		// 수신 데이터 저장.
//		rfcDataProcessorService.saveResultToJson(meta);
//	}
//
//	private void zvmWesToConfirmReceive() throws Exception {
//		SapMappingMetaData meta = loader.loadMetaData("ZWM_WES_TO_CONFIRM_RECEIVE");
//
//		Map<String, String> importParams = new HashMap<>();
//		// * Import parameter : 1.I_ZWERKS, 2. I_RSDAT, I_SEQ
//		importParams.put("I_ZWERKS", "2009");
//		importParams.put("I_RSDAT", "20250407");
//		// importParams.put("I_SEQ", "3");
//
//		meta.setImportParams(importParams);
//
//		Map<String, List<Map<String, Object>>> tableDataMap = new HashMap<>();
//
//		// T_HEADER
//		Map<String, Object> headerRow = new HashMap<>();
//		headerRow.put("COM_CD", "2009");
//		headerRow.put("ORDER_DATE", "20250407");
//		headerRow.put("WMS_BATCH_NO", "3");
//		headerRow.put("CUST_ORDER_NO", "18341373");
//		headerRow.put("ORDERER_ID", "20386146");
//		tableDataMap.put("T_HEADER", Collections.singletonList(headerRow));
//
//		// T_ITEM
//		String excelFilePath = "C:/project/coway/near-wcs/logis-connector/logs/T_ITEM.xlsx";
//		List<Map<String, Object>> T_ITEM = loadTItemFromExcel(excelFilePath);
//		tableDataMap.put("T_ITEM", T_ITEM);
//
//		// T_SIRIAL
//		String sirialFilePath = "C:/project/coway/near-wcs/logis-connector/logs/T_SIRIAL.xlsx";
//		List<Map<String, Object>> T_SIRIAL = loadTItemFromExcel(sirialFilePath);
//		tableDataMap.put("T_SIRIAL", T_SIRIAL);
//
//		meta.setTableDataMap(tableDataMap);
//
//		rfcDataProcessorService.handleRfcCall(meta);
//
//		// 수신 데이터 저장.
//		rfcDataProcessorService.saveResultToJson(meta);
//	}
//
//	private void zvmWesToConfirmReceiveDif() throws Exception {
//		SapMappingMetaData meta = loader.loadMetaData("ZWM_WES_TO_CONFIRM_RECEIVE_DIF");
//
//		Map<String, String> importParams = new HashMap<>();
//		// * Import parameter : 1.I_ZWERKS, 2. I_RSDAT, I_SEQ
//		importParams.put("I_ZWERKS", "2009");
//		importParams.put("I_RSDAT", "20250407");
//		// importParams.put("I_SEQ", "3");
//
//		meta.setImportParams(importParams);
//
//		Map<String, List<Map<String, Object>>> tableDataMap = new HashMap<>();
//
//		// T_HEADER
//		Map<String, Object> headerRow = new HashMap<>();
//		headerRow.put("COM_CD", "2009");
//		headerRow.put("ORDER_DATE", "20250407");
//		headerRow.put("WMS_BATCH_NO", "3");
//		headerRow.put("CUST_ORDER_NO", "18342263");
//		headerRow.put("ORDERER_ID", "20386146");
//		tableDataMap.put("T_HEADER", Collections.singletonList(headerRow));
//
//		// T_ITEM
//		String excelFilePath = "C:/project/coway/near-wcs/logis-connector/logs/T_ITEM.xlsx";
//		List<Map<String, Object>> T_ITEM = loadTItemFromExcel(excelFilePath);
//		tableDataMap.put("T_ITEM", T_ITEM);
//
//		// T_SIRIAL
//		String sirialFilePath = "C:/project/coway/near-wcs/logis-connector/logs/T_SIRIAL.xlsx";
//		List<Map<String, Object>> T_SIRIAL = loadTItemFromExcel(sirialFilePath);
//		tableDataMap.put("T_SIRIAL", T_SIRIAL);
//
//		meta.setTableDataMap(tableDataMap);
//
//		rfcDataProcessorService.handleRfcCall(meta);
//
//		// 수신 데이터 저장.
//		rfcDataProcessorService.saveResultToJson(meta);
//	}

	public static List<Map<String, Object>> loadTItemFromExcel(String filePath) throws Exception {
		List<Map<String, Object>> T_TABLE = new ArrayList<>();

		try (InputStream inp = new FileInputStream(filePath); Workbook workbook = new XSSFWorkbook(inp)) {

			Sheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rowIterator = sheet.iterator();

			// 헤더 추출
			Row headerRow = rowIterator.next();
			List<String> headers = new ArrayList<>();
			for (Cell cell : headerRow) {
				headers.add(cell.getStringCellValue().trim());
			}

			DataFormatter formatter = new DataFormatter(); // ✅ 여기!

			// 데이터 추출
			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();
				Map<String, Object> item = new HashMap<>();

				for (int i = 0; i < headers.size(); i++) {
					Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
					Object value = formatter.formatCellValue(cell).trim(); // ✅ 포맷팅된 문자열로 가져오기
					item.put(headers.get(i), value);
				}

				T_TABLE.add(item);
			}
		}

		return T_TABLE;
	}
}
