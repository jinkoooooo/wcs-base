package operato.logis.connector.sap.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoListMetaData;
import com.sap.conn.jco.JCoParameterList;
import com.sap.conn.jco.JCoTable;

import operato.logis.connector.sap.mapper.SapMappingMetaData;
import operato.logis.connector.sap.util.CheckedRunnable;
import operato.logis.connector.sap.util.SapFieldTypeConverter;


/**
 * SAP RFC 호출 및 데이터 송수신 처리 서비스
 */
@Component
public class RfcDataProcessorService {

	private Logger logger = LoggerFactory.getLogger(RfcDataProcessorService.class);
//	private static final String LOG_DIR = System.getProperty("user.dir") + File.separator + "logis-connector"
//			+ File.separator + "logs";
	private static final String LOG_DIR = new File("../logis-connector/logs").getAbsolutePath();

	private final SapConnectionService sapConnectionService;
	@Autowired
	private Environment environment; // 현재 Spring 프로파일 확인용

	public RfcDataProcessorService(SapConnectionService sapConnectionService) {
		this.sapConnectionService = sapConnectionService;
	}

	/**
	 * RFC 호출 및 데이터 처리
	 */
	/**
	 * SAP RFC를 호출하거나, dev 환경에서는 JSON 결과를 로드하여 처리
	 */
	public void handleRfcCall(SapMappingMetaData config) {
		long start = System.nanoTime();
		if (isDevProfile()) {
			logger.info("[{}] dev 환경 - JSON 파일 로딩으로 처리", config.getFunctionName());
			loadResultFromJson(config);
		} else {
			retryWithDelay(3, 3000, () -> {
				JCoDestination destination = sapConnectionService.getDestination();
				JCoFunction function = destination.getRepository().getFunction(config.getFunctionName());

				if (function == null) {
					throw new RuntimeException("RFC 함수 [" + config.getFunctionName() + "] 없음");
				}

				setImportParameters(function, config.getImportParams());

				if (Boolean.TRUE.equals(config.getIsSend())) {
					processSendMode(function, config);
				} else {
					processReceiveMode(function, config);
				}
			});
		}

		double seconds = (System.nanoTime() - start) / 1_000_000_000.0;
		logger.info("[{}] SAP 처리 시간: {} 초", config.getFunctionName(), String.format("%.3f", seconds));
	}
	/**
	 * 지정된 횟수만큼 재시도하면서 예외를 처리
	 */
	private void retryWithDelay(int maxRetries, int delayMillis, CheckedRunnable task) {
		for (int attempt = 1; attempt <= maxRetries; attempt++) {
			try {
				task.run();
				return; // 성공 시 종료
			} catch (Exception e) {
				if (attempt >= maxRetries) {
					logger.error("SAP RFC 처리 실패 - 최대 {}회 재시도 후에도 실패", maxRetries, e);
					throw new RuntimeException("SAP RFC 처리 실패 (최대 재시도 초과)", e);
				} else {
					logger.warn("SAP RFC 처리 실패 - 재시도 {}/{} (사유: {})", attempt, maxRetries, e.toString());
					try {
						Thread.sleep(delayMillis);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						throw new RuntimeException("재시도 대기 중 인터럽트 발생", ie);
					}
				}
			}
		}
	}

	/**
	 * RFC 수신 처리 (SAP → 외부 시스템)
	 */
	private void processReceiveMode(JCoFunction function, SapMappingMetaData config) throws JCoException {
		function.execute(sapConnectionService.getDestination());

		Map<String, List<Map<String, Object>>> tableDataMap = new HashMap<>();

		// 테이블 이름 목록을 fieldsMap 기준으로 파악
		for (String tableName : config.getFieldsMap().keySet()) {
			JCoTable table = function.getTableParameterList().getTable(tableName);
			List<String> fields = config.getFieldsMap().get(tableName);
			Map<String, String> mapping = config.getMappingMap().getOrDefault(tableName, new HashMap<>());
			Map<String, String> typeMapping = config.getTypeMap().getOrDefault(tableName, new HashMap<>());

			List<Map<String, Object>> resultList = new ArrayList<>();

			for (int i = 0; i < table.getNumRows(); i++) {
				table.setRow(i);
				Map<String, Object> row = new LinkedHashMap<>();

				for (String field : fields) {
					String sapField = mapping.getOrDefault(field, field);
					String type = typeMapping.getOrDefault(sapField, "CHAR").toUpperCase();
					Object value = table.getValue(sapField);
					row.put(field, SapFieldTypeConverter.convertValue(type, value));
				}

				resultList.add(row);
			}

			this.logger.info("[{}] 테이블 [{}] 수신 건수: {}", config.getFunctionName(), tableName, resultList.size());
			tableDataMap.put(tableName, resultList);
		}

		config.setTableDataMap(tableDataMap);
		config.setExportParams(extractExportParams(function));
	}

	/**
	 * RFC 송신 처리 (외부 시스템 → SAP)
	 */
	private void processSendMode(JCoFunction function, SapMappingMetaData config) throws JCoException {
		Map<String, List<Map<String, Object>>> tableDataMap = config.getTableDataMap();
		Map<String, List<String>> fieldsMap = config.getFieldsMap();
		Map<String, Map<String, String>> mappingMap = config.getMappingMap();

		if (tableDataMap != null) {
			for (String tableName : tableDataMap.keySet()) {
				JCoTable table = function.getTableParameterList().getTable(tableName);
				List<Map<String, Object>> rows = tableDataMap.get(tableName);
				List<String> fields = fieldsMap.get(tableName);
				Map<String, String> fieldMapping = mappingMap.getOrDefault(tableName, new HashMap<>());

				for (Map<String, Object> row : rows) {
					table.appendRow();
					for (String field : fields) {
						String sapField = fieldMapping.getOrDefault(field, field);
						Object value = row.getOrDefault(field, "");
						table.setValue(sapField, value);
					}
				}
			}
		}

		function.execute(sapConnectionService.getDestination());
		config.setExportParams(extractExportParams(function));

		logger.info("[{}] 다중 테이블 송신 완료", config.getFunctionName());
	}

	/**
	 * Import 파라미터를 SAP 함수에 설정
	 */
	private void setImportParameters(JCoFunction function, Map<String, String> params) {
		if (params == null)
			return;

		JCoParameterList importList = function.getImportParameterList();
		for (Map.Entry<String, String> entry : params.entrySet()) {
			if (entry.getValue() != null) {
				importList.setValue(entry.getKey(), entry.getValue());
			}
		}
	}

	/**
	 * Export 파라미터를 Map 형식으로 추출
	 */
	private Map<String, String> extractExportParams(JCoFunction function) {
		Map<String, String> result = new LinkedHashMap<>();
		JCoParameterList exportList = function.getExportParameterList();

		if (exportList != null) {
			JCoListMetaData meta = exportList.getListMetaData();
			for (int i = 0; i < meta.getFieldCount(); i++) {
				String fieldName = meta.getName(i);
				result.put(fieldName, exportList.getString(fieldName));
			}
		}

		return result;
	}

	/**
	 * JSON 파일로 SAP 결과 저장
	 */
	public void saveResultToJson(SapMappingMetaData config) {
		try {
			String filename = buildFileName(config);
			String path = Paths.get(LOG_DIR, filename).toString();
			File file = new File(path);

			if (!file.getParentFile().exists())
				file.getParentFile().mkdirs();

			ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
					.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).enable(SerializationFeature.INDENT_OUTPUT);
			// mapper.writeValue(file, config.getTableDataMap()); // 테이블 데이터만 저장.
			mapper.writeValue(file, config);

			this.logger.info("[{}] SAP 결과 저장: {}", config.getFunctionName(), file.getAbsolutePath());

		} catch (IOException e) {
			throw new RuntimeException("SAP 결과 저장 실패", e);
		}
	}

	/**
	 * JSON 파일에서 SAP 결과 로드
	 */
	private String buildFileName(SapMappingMetaData config) {
		StringBuilder prefix = new StringBuilder();

		Map<String, String> params = config.getImportParams();
		if (params != null) {
			for (Map.Entry<String, String> entry : params.entrySet()) {
				if (entry.getValue() != null && !entry.getValue().isBlank()) {
					prefix.append(entry.getKey().replace("I_", "")).append("-").append(entry.getValue()).append("_");
				}
			}
		}

		String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(System.currentTimeMillis());

		return prefix + config.getFunctionName() + "_" + timestamp + ".json";
	}
	
	private String buildFileNamePrefix(SapMappingMetaData config) {
	    StringBuilder prefix = new StringBuilder();

	    Map<String, String> params = config.getImportParams();
	    if (params != null) {
	        for (Map.Entry<String, String> entry : params.entrySet()) {
	            if (entry.getValue() != null && !entry.getValue().isBlank()) {
	                prefix.append(entry.getKey().replace("I_", ""))
	                      .append("-")
	                      .append(entry.getValue())
	                      .append("_");
	            }
	        }
	    }

	    prefix.append(config.getFunctionName()).append("_");
	    return prefix.toString();
	}

	private void loadResultFromJson(SapMappingMetaData config) {
	    try {
	        String prefix = buildFileNamePrefix(config);
	        File logDir = new File(LOG_DIR);

	        File[] candidates = logDir.listFiles((dir, name) -> name.startsWith(prefix) && name.endsWith(".json"));

	        if (candidates == null || candidates.length == 0) {
	            throw new RuntimeException("저장된 JSON 파일이 존재하지 않습니다: " + LOG_DIR + "/" + prefix + "*");
	        }

	        // 타임스탬프가 가장 큰(최신) 파일 선정
	        File latest = Arrays.stream(candidates)
	            .max(Comparator.comparing(File::getName))
	            .orElseThrow(() -> new RuntimeException("최근 JSON 파일 없음"));

	        ObjectMapper mapper = new ObjectMapper()
	            .registerModule(new JavaTimeModule())
	            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	        SapMappingMetaData loadedConfig = mapper.readValue(latest, SapMappingMetaData.class);

	        // 필요한 정보 복사
	        config.setTableDataMap(loadedConfig.getTableDataMap());
	        config.setExportParams(loadedConfig.getExportParams());

	        this.logger.info("[{}] JSON 결과 로딩 완료: {}", config.getFunctionName(), latest.getAbsolutePath());

	    } catch (IOException e) {
	        throw new RuntimeException("JSON 로드 실패", e);
	    }
	}

	/**
	 * 현재 프로파일이 dev인지 확인
	 */
	private boolean isDevProfile() {
		return Arrays.asList(environment.getActiveProfiles()).contains("dev");
	}
}
