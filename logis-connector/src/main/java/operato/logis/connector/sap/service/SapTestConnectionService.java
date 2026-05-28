package operato.logis.connector.sap.service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import operato.logis.connector.core.event.IntegrationEvent;
import operato.logis.connector.sap.mapper.SapMappingMetaData;
import operato.logis.connector.sap.model.SapPayload;

@Component
public class SapTestConnectionService {

	private final ObjectMapper objectMapper = new ObjectMapper();

//    public SapRfcFieldConfig handleRfcTestCall(SapRfcCallEvent event) throws IOException {
//        // JSON 파일 이름 설정
//        StringBuilder fileName = new StringBuilder();
//        Map<String, String> params = event.getImportParams();
//        if (params != null) {
//            for (Map.Entry<String, String> entry : params.entrySet()) {
//                if (entry.getValue() != null && !entry.getValue().isBlank()) {
//                    fileName.append(entry.getKey().replace("I_", ""))
//                            .append("-")
//                            .append(entry.getValue())
//                            .append("_");
//                }
//            }
//        }
//        fileName.append(event.getFunctionName());
//
//        // prefix로 시작하는 파일 필터링
//        File logsDir = new File("logis-connector/logs");
//        System.out.println("Looking in: " + logsDir.getAbsolutePath());
//        File[] matchingFiles = logsDir.listFiles((dir, name) -> name.startsWith(fileName.toString()) && name.endsWith(".json"));
//
//        if (matchingFiles == null || matchingFiles.length == 0) {
//            throw new IOException("No matching JSON file found with prefix: " + fileName);
//        }
//
//        // 최신순 정렬
//        Optional<File> latestFile = Arrays.stream(matchingFiles).min((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
//
//        return objectMapper.readValue(latestFile.get(), SapRfcFieldConfig.class);
//    }

	public SapMappingMetaData handleRfcTestCall(IntegrationEvent<SapPayload> event) throws IOException {
		// 1. 파일 이름 Prefix 구성
		StringBuilder fileName = new StringBuilder();
		Map<String, String> params = event.getPayload().getImportParams();
		if (params != null) {
			for (Map.Entry<String, String> entry : params.entrySet()) {
				if (entry.getValue() != null && !entry.getValue().isBlank()) {
					fileName.append(entry.getKey().replace("I_", "")).append("-").append(entry.getValue()).append("_");
				}
			}
		}

		fileName.append(event.getEndpoint()); // FunctionName

		// 2. 디렉토리 조회 및 파일 필터링
		File logsDir = new File("logis-connector/logs");
		File[] matchingFiles = logsDir
				.listFiles((dir, name) -> name.startsWith(fileName.toString()) && name.endsWith(".json"));

		if (matchingFiles == null || matchingFiles.length == 0) {
			throw new IOException("No matching JSON file found with prefix: " + fileName);
		}

		// 3. 최신 파일 가져오기
		Optional<File> latestFile = Arrays.stream(matchingFiles)
				.min((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

		return objectMapper.readValue(latestFile.get(), SapMappingMetaData.class);
	}
}