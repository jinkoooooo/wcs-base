package operato.logis.samsung;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import operato.logis.connector.api.dto.CommonApiResponse;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.util.ValueUtil;

public class WcsUtils {

    private static final ObjectMapper logObjectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /**
     * API 응답이 성공적인지 확인하는 헬퍼 메서드
     */
    public static boolean isSuccessfulApiResponse(CommonApiResponse response) {
        return ValueUtil.isNotEmpty(response) && WcsConstants.DEFAULT_SUCCESS_CODE.equals(response.getCode());
    }

    /**
     * 도메인 설정
     */
    public static void setupDomainContext() {
        Domain domain = new Domain();
        domain.setId(WcsConstants.DOMAIN_ID);
        domain.setName(WcsConstants.DOMAIN_NAME);
        Domain.setCurrentDomain(domain);
    }

    /**
     * 요청 바디를 JSON 형태로 로그 출력
     */
    public static String logRequestBody(Object request) {
        try {
            return logObjectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(request);
        }
        catch (Exception e) {
            return null;
        }
    }
}