package operato.logis.kmat_2026.biz.ecs.sineva.consts;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import xyz.anythings.sys.util.AnyValueUtil;
import xyz.elidom.util.ValueUtil;

import java.util.HashMap;
import java.util.Map;

public class ResponseUtil {

    /**
     * 성공 응답 생성
     *
     * @param regId 요청 ID
     * @return 성공 응답 Map
     */
    public static Map<String, Object> successResponse(String regId) {
        return createResponse(200, "successful", null,regId);
    }

    /**
     * 성공 응답 생성
     *
     * @return 성공 응답 Map
     */
    public static Map<String, Object> successResponse() {
        return createResponse(200, "successful", null,null);
    }

    /**
     * 성공 응답 생성
     *
     * @return 성공 응답 Map
     */
    public static Map<String, Object> successResponse(String regId, Map<String, Object> params) {
        Map<String, Object> successResponse = createResponse(0, "successful","",regId);

        if (ValueUtil.isNotEmpty(params)) {
            params.putAll(params);
        }

        return successResponse;
    }

    /**
     * 실패 응답 생성 (RunTypeException 개발자 기입 전용)
     *
     * @return 실패 응답 Map
     */
    public static Map<String, Object> errorResponse(String userMessage) {
        return createResponse(-1, userMessage,null, null);
    }

    /**
     * 실패 응답 생성
     *
     * @param regId 요청 ID
     * @return 실패 응답 Map
     */
    public static Map<String, Object> errorResponse(String regId, String userMessage, String detailMessage) {
        return createResponse(400, userMessage,detailMessage, regId);
    }

    /**
     * 실패 응답 생성
     *
     * @return 실패 응답 Map
     */
    public static Map<String, Object> errorResponse(String userMessage, String detailMessage) {
        return createResponse(400, userMessage,detailMessage, null);
    }

    /**
     * 응답 Map 생성
     *
     * @param code 응답 코드 (0: 성공, -1: 실패)
     * @param message 응답 메시지
     * @param regId 요청 ID
     * @return 응답 Map
     */
    private static Map<String, Object> createResponse(int code, String message, String detailMessage, String regId) {
        Map<String, Object> stringObjectMap = AnyValueUtil.newMap("code,msg,detailMessage,regId", code, message, detailMessage,regId != null ? regId : "");
        return stringObjectMap;
    }

    private static final ObjectMapper mapper = new ObjectMapper();

    public static Map<String, Object> jsonNodeToMap(JsonNode node) {
        if (node == null) {
            return new HashMap<>();
        }
        return mapper.convertValue(node, new TypeReference<Map<String,Object>>(){});
    }


}
