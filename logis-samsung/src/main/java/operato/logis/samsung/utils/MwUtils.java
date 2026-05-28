package operato.logis.samsung.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Component
public class MwUtils {
    private static final Logger logger = LoggerFactory.getLogger(MwUtils.class);

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules(); // LocalDateTime 등 자동 변환

    /**
     * 요청 바디를 JSON 형태로 로그 출력
     */
    public String logRequestBody(Object request) {
        try {
            String jsonPretty = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(request);

            logger.info("[HOKUSHO] 오더 지시 Body :\n{}", jsonPretty);

            return jsonPretty;

        } catch (Exception e) {
            logger.warn("[HOKUSHO] 요청 Body 로그 변환 실패: {}", e.getMessage());
            logger.info("[HOKUSHO] 실적 보고 Body 수신:\n{}", request);

            return null;
        }
    }
    /**
     * 실적 바디를 JSON 형태로 로그 출력
     */
    public String logResponseBody(Object request) {
        try {
            String jsonPretty = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(request);

            logger.info("[HOKUSHO] 실적 보고 Body 수신:\n{}", jsonPretty);

            return jsonPretty;

        } catch (Exception e) {
            logger.warn("[HOKUSHO] 요청 Body 로그 변환 실패: {}", e.getMessage());
            logger.info("[HOKUSHO] 실적 보고 Body 수신:\n{}", request);

            return null;
        }
    }

    public static boolean hasMissingField(Object request) {
        try {
            for (Field field : request.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(request);

                // null 또는 빈 문자열이면 실패
                if (value == null || (value instanceof String && StringUtils.isBlank((String) value))) {
                    System.out.println("[HOKUSHO] 누락된 필드: " + field.getName());
                    return true;
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false; // 모든 필드 존재하면 false
    }

    /**
     * null 또는 빈 문자열 체크 유틸
     */
    public boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * 수신된 JSON 요청을 프로젝트 실행 경로의 /inoutdata 폴더에 날짜별 파일로 저장
     */
    public void saveRequestToFile(Object request) {
        try {
            // 1️⃣ 현재 실행 중인 경로 기준으로 inoutdata 폴더 설정
            String baseDir = System.getProperty("user.dir") + File.separator + "inoutdata";
            File folder = new File(baseDir);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            // 2️⃣ 날짜별 파일명 생성
            String dateStr = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
            File file = new File(folder, dateStr + ".json");

            // 3️⃣ 파일 용량 5MB 초과 시 자동 분할
            long maxSize = 5 * 1024 * 1024; // 5MB
            int index = 1;
            while (file.exists() && file.length() > maxSize) {
                file = new File(folder, dateStr + "_" + index + ".json");
                index++;
            }

            // 4️⃣ JSON 문자열로 변환
            ObjectMapper mapper = new ObjectMapper()
                    .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .enable(SerializationFeature.INDENT_OUTPUT);
            String json = mapper.writeValueAsString(request);

            // 5️⃣ 파일에 append 방식으로 저장
            try (FileWriter fw = new FileWriter(file, true);
                 BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write("==== [" + new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()) + "] ====\n");
                bw.write(json);
                bw.write("\n\n");
            }

            logger.info("[HOKUSHO] 수신 JSON 저장 완료: {}", file.getAbsolutePath());
        } catch (Exception e) {
            logger.error("[HOKUSHO] JSON 저장 실패: {}", e.getMessage(), e);
        }
    }

    public static boolean isInvalidJsonFormat(String jsonBody, Class<?> dtoClass) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(jsonBody);

            // DTO의 필드명 (JsonProperty 반영 포함)
            Set<String> dtoFields = new HashSet<>();
            for (Field f : dtoClass.getDeclaredFields()) {
                com.fasterxml.jackson.annotation.JsonProperty prop = f.getAnnotation(com.fasterxml.jackson.annotation.JsonProperty.class);
                dtoFields.add(prop != null ? prop.value() : f.getName());
            }

            // JSON의 필드명
            Set<String> jsonFields = new HashSet<>();
            Iterator<String> it = node.fieldNames();
            while (it.hasNext()) jsonFields.add(it.next());

            // DTO에 없는 필드가 JSON에 있으면 invalid
            for (String field : jsonFields) {
                if (!dtoFields.contains(field)) {
                    System.out.println("[HOKUSHO] DTO에 정의되지 않은 필드: " + field);
                    return true;
                }
            }

            // DTO 필드 중 JSON에 누락된 게 있으면 invalid
            for (String field : dtoFields) {
                if (!jsonFields.contains(field)) {
                    System.out.println("[HOKUSHO] JSON에 누락된 필드: " + field);
                    return true;
                }
            }

            return false; // ✅ 일치할 경우 정상
        } catch (Exception e) {
            System.out.println("[HOKUSHO] JSON 파싱 오류: " + e.getMessage());
            return true;
        }
    }

}
