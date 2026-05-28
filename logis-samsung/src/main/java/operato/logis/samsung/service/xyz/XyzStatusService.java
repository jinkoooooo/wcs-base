package operato.logis.samsung.service.xyz;

import lombok.RequiredArgsConstructor;
import operato.logis.connector.api.dto.CommonApiResponse;
import operato.logis.connector.api.service.ExternalApiService;
import operato.logis.samsung.WcsConstants;
import operato.logis.samsung.WcsUtils;
import operato.logis.samsung.service.wcs.UnitErrorLogService;
import operato.logis.samsung.service.wcs.UnitHeartbeatService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class XyzStatusService extends AbstractQueryService {

    private final ExternalApiService externalApiService;
    private final UnitHeartbeatService unitHeartbeatService;
    private final UnitErrorLogService unitErrorLogService;

    private final String UNIT_CODE_XYZ = "XYZ";
    private final String UNIT_DVN_CODE_XYZ = "XYZ_DEVANNING";

    /**
     * MW -> XYZ 상태 조회 메소드
     * - JOB(XYZ Status Scheduling)에서 1초마다 실행
     */
    //@Scheduled(fixedDelay = 1000)
    public void getXyzStatus() {
        // API URL 설정
        String xyzServerIP = WcsConstants.XYZ_SERVER_URL;
        String endPoint = "/api/status";
        String url = xyzServerIP + endPoint;

        // XYZ 응답 수신
        try {
            Mono<CommonApiResponse> responseMono = externalApiService.get(url, CommonApiResponse.class);
            CommonApiResponse response = responseMono.block();

            // XYZ Status를 Unit Status에 반영
            if (ValueUtil.isNotEmpty(response)) {
                Map<String, Object> data = response.getData();
                Integer code = (Integer) data.get("status");

                unitHeartbeatService.updateUnitStatus(WcsConstants.UNIT_TYPE_SYSTEM, UNIT_CODE_XYZ, data.get("status").toString(), convertStatusToMsg(code));
            } else {
                logger.error("[XYZ] Status 조회 중 에러 발생");
            }
        }
        catch (ElidomRuntimeException e) {
            logger.error("[XYZ] Status 조회 중 에러 발생 : {}", e.getMessage());
        }
    }

    public void getXyzDnvStatus() {
        // API URL 설정
        String xyzServerIP = WcsConstants.XYZ_DEVANNING_URL;
        String endPoint = "/api/status";
        String url = xyzServerIP + endPoint;

        // XYZ 응답 수신
        try {
            Mono<CommonApiResponse> responseMono = externalApiService.get(url, CommonApiResponse.class);
            CommonApiResponse response = responseMono.block();

            // XYZ Status를 Unit Status에 반영
            if (ValueUtil.isNotEmpty(response)) {
                Map<String, Object> data = response.getData();
                Integer code = (Integer) data.get("status");

                unitHeartbeatService.updateUnitStatus(WcsConstants.UNIT_TYPE_SYSTEM, UNIT_DVN_CODE_XYZ, data.get("status").toString(), convertStatusToMsg(code));
            } else {
                logger.error("[XYZ] Status 조회 중 에러 발생");
            }
        }
        catch (ElidomRuntimeException e) {
            logger.error("[XYZ] Status 조회 중 에러 발생 : {}", e.getMessage());
        }
    }

    /**
     * XYZ code -> msg 변환 메소드
     *
     * @param code XYZ에서 반환하는 상태 코드
     * @return 기존에 협의된 상태 코드별 메시지
     */
    private String convertStatusToMsg(int code) {
        return switch (code) {
            case 0 -> "오더 없음";
            case 1 -> "오더 진행 중";
            case 99 -> "프로그램 정지 상태";
            default -> "알 수 없음";
        };
    }

    public void createXyzErrorLog(Map<String, Object> errorInfo) {
        logger.info("[XYZ] Error Receive : {}", WcsUtils.logRequestBody(errorInfo));
        unitErrorLogService.createErrorLog(WcsConstants.UNIT_TYPE_SYSTEM, UNIT_CODE_XYZ, errorInfo.get("error_code").toString(), errorInfo.get("error_msg").toString());
    }
}