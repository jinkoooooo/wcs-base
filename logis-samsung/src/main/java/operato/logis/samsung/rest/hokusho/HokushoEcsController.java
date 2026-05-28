package operato.logis.samsung.rest.hokusho;

import lombok.RequiredArgsConstructor;
import operato.logis.connector.hokusho.consts.HokushoResponseCode;
import operato.logis.connector.hokusho.dto.HokushoPerformanceReportRequest;
import operato.logis.connector.hokusho.dto.HokushoResponse;
import operato.logis.samsung.consts.BoxTrackingEventType;
import operato.logis.samsung.event.BoxTrackingEvent;
import operato.logis.samsung.utils.MwUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/hokusho")
public class HokushoEcsController {

    /**
     * Logger
     */
    private Logger logger = LoggerFactory.getLogger(HokushoEcsController.class);

    private final MwUtils mwUtils;

    private final ApplicationEventPublisher publisher;

    /**
     * ECS → WCS 실적 수신 콜백
     */
    @PostMapping("/performance")
    public ResponseEntity<HokushoResponse> receivePerformance(@RequestBody HokushoPerformanceReportRequest request) {

        String jsonRow = mwUtils.logResponseBody(request); // ✅ JSON 로그만 출력

        logger.info("[HOKUSHO] 실적 보고 도착 commandId={}, equipId={}, getResultType={}, message={}",
                request.getCommandId(), request.getEquipId(), request.getResultType(), request.getMessage());

        // JSON 저장
        mwUtils.saveRequestToFile(request);

        // 기본값
        HokushoResponseCode codeEnum = HokushoResponseCode.SUCCESS;

        try {
            // 1️⃣ 포맷 검증
            if (MwUtils.isInvalidJsonFormat(jsonRow, HokushoPerformanceReportRequest.class)) {
                codeEnum = HokushoResponseCode.INVALID_FORMAT;
            }
            // 2️⃣ 필수 파라미터
            else if (MwUtils.hasMissingField(request)) {
                codeEnum = HokushoResponseCode.MISSING_PARAM;
            }
            // 3️⃣ 명령 타입 검증 (예: MOVE, STORE, RETRIEVE 허용)
            else if (!Arrays.asList("DVRT", "TURN", "PMOV", "PLTZ").contains(request.getResultType())) {
                codeEnum = HokushoResponseCode.UNSUPPORTED_TYPE;
            }
            // 4️⃣ 설비ID 검증
            else if (mwUtils.isEmpty(request.getEquipId())) {
                codeEnum = HokushoResponseCode.INVALID_LINE_OR_EQUIP;
            }

        } catch (Exception e) {
            logger.error("❌ 실적 콜백 처리 중 예외 발생", e);
            codeEnum = HokushoResponseCode.SERVER_ERROR;
        }

        // ✅ 응답 객체 구성
        HokushoResponse response = HokushoResponse.builder()
                .code(codeEnum.getCode())
                .message(codeEnum.getMessage())
                .commandId(request.getCommandId())
                .build();

        logger.info("[HOKUSHO] 콜백 응답: {}", response);


        BoxTrackingEvent event = BoxTrackingEvent.builder()
                .eventType(BoxTrackingEventType.HOKUSHO_EVENT)
                .plcSeqNo(request.getPlcSeqNo())
                .parcelId(request.getParcelId())
                .lineId(request.getLineId())
                .equipId(request.getEquipId())
                .hokushoData(request)
                .build();

        publisher.publishEvent(event);
        logger.info("[DEV][UPDATE] published: {}", event);

        return ResponseEntity.ok(response);
    }



    /* ===== 작은 파서 ===== */
    private static String str(Map<String, Object> m, String k, boolean required) {
        return str(m, k, required, null);
    }
    private static String str(Map<String, Object> m, String k, boolean required, String def) {
        Object v = m.get(k);
        if (v == null) {
            if (required) throw new IllegalArgumentException("missing required field: " + k);
            return def;
        }
        return String.valueOf(v);
    }
    private static Integer intOrNull(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception ignore) { return null; }
    }
}