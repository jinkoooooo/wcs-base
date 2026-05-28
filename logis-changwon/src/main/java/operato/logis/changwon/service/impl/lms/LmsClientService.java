package operato.logis.changwon.service.impl.lms;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import operato.logis.changwon.dto.lms.AlarmRequestDto;
import operato.logis.changwon.dto.lms.EquipmentRequestDto;
import operato.logis.changwon.entity.MFC.ErrLog;
import operato.logis.changwon.entity.MFC.PrsJobSts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class LmsClientService {

    private final LmsQueryService lmsQueryService; // QueryService 주입

    private RestTemplate restTemplate;

    @Value("${lms.server.url}")
    private String baseUrl;

    @Value("${lms.center.id:KR_Changwon}")
    private String centerId;

    @Value("${lms.center.line:MFC_LINE}")
    private String lineId;

    private static final String API_STATUS = "/rest/lms/status";
    private static final String API_ALARM = "/rest/lms/alarm";

    // 중복 전송 방지용 캐시 (Key: 설비ID)
    private final Map<String, EquipmentRequestDto> prevStatusMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 1. 설비 상태 전송 프로세스 (스케줄러 호출용)
     */
    public void processAndSendConveyorStatus() {
        try {
            // 1. DB 조회 (QueryService 이용)
            List<PrsJobSts> entities = lmsQueryService.getConveyorStatusList();
            if (entities == null || entities.isEmpty()) return;

            List<EquipmentRequestDto> toSendList = new ArrayList<>();

            for (PrsJobSts entity : entities) {
                EquipmentRequestDto dto = convertStatusToDto(entity);

                // 2. 상태 변경 확인 (중복 제거)
                if (isStatusChanged(dto)) {
                    toSendList.add(dto);
                    prevStatusMap.put(dto.getEquipId(), dto); // 캐시 갱신
                }
            }

            // 3. 전송
            if (!toSendList.isEmpty()) {
                sendData(baseUrl + API_STATUS, toSendList);
            }

        } catch (Exception e) {
            log.error("컨베이어 상태 전송 중 오류", e);
        }
    }

    /**
     * 2. 에러 알람 전송 프로세스 (스케줄러 호출용)
     */
    public void processAndSendConveyorAlarms() {
        try {
            // 1. 미전송 알람 조회 (QueryService 이용)
            List<ErrLog> errLogs = lmsQueryService.getAgvStatusList();
            if (errLogs == null || errLogs.isEmpty()) return;

            List<AlarmRequestDto> toSendList = new ArrayList<>();
            for (ErrLog logEntity : errLogs) {
                toSendList.add(convertAlarmToDto(logEntity));
            }

            // 2. 전송 시도
            boolean isSent = sendData(baseUrl + API_ALARM, toSendList);

            // 3. 성공 시 DB 업데이트 (전송 완료 처리)
            if (isSent) {
                for (ErrLog logEntity : errLogs) {
                    //lmsQueryService.getAgvStatusList(logEntity);
                }
                log.info("알람 {}건 전송 및 업데이트 완료", errLogs.size());
            }

        } catch (Exception e) {
            log.error("컨베이어 알람 전송 중 오류", e);
        }
    }

    // ==========================================
    // 매핑 로직 (Entity -> DTO)
    // ==========================================

    private EquipmentRequestDto convertStatusToDto(PrsJobSts entity) {
        // ID 생성 (예: CV_01)
        String equipId = "CV_" + String.format("%02d", entity.getMachineId());

        // 상태값 변환 (현장 코드에 맞게 매핑 필요)
        String status = "UNKNOWN";
        if (entity.getJobStatus() != null) {
            switch (entity.getJobStatus()) {
                case 1: status = "IDLE"; break;
                case 2: status = "RUN"; break;
                case 3: status = "ERROR"; break;
                default: status = String.valueOf(entity.getJobStatus());
            }
        }

        // 에러 코드
        String errCode = (entity.getErrorCode() != null && entity.getErrorCode() > 0)
                ? String.valueOf(entity.getErrorCode()) : "0";
        String errMsg = (!"0".equals(errCode)) ? "Error Code: " + errCode : null;

        return EquipmentRequestDto.builder()
                .lcId(this.centerId)
                .equipId(equipId)
                .lineId(this.lineId)
                .orderId(entity.getOrderId())
                .currentStatus(status)
                .errCd(errCode)
                .errMsg(errMsg)
                .sensorValue(BigDecimal.valueOf(entity.getBattery() != null ? entity.getBattery() : 0))
                .sensorUnit("%")
                .statusUpdatedAt(entity.getUpdateTime() != null ? entity.getUpdateTime() : new Date())
                .dataUpdatedAt(new Date())
                .sourceSystem("MFC_DB")
                .build();
    }

    private AlarmRequestDto convertAlarmToDto(ErrLog entity) {
        String equipId = entity.getErrorMachine() != null ? entity.getErrorMachine() : "UNKNOWN";
        String alarmCode = entity.getErrorCode() != null ? String.valueOf(entity.getErrorCode()) : "UNKNOWN";
        boolean isCleared = entity.getResetDatetime() != null;

        return AlarmRequestDto.builder()
                .lcId(this.centerId)
                .equipId(equipId)
                .lineId(this.lineId)
                .orderId(entity.getOrderId())
                .alarmId(alarmCode)
                .alarmType("ERROR")
                .alarmMsg("Rack No: " + entity.getRackNo()) // 필요한 정보 매핑
                .description(String.format("Bank:%d Bay:%d Tier:%d", entity.getErrorBank(), entity.getErrorBay(), entity.getErrorTier()))
                .isCleared(isCleared)
                .occurredAt(entity.getErrorDatetime())
                .clearedAt(entity.getResetDatetime())
                .sourceSystem("MFC_DB")
                .build();
    }

    // ==========================================
    // 유틸리티
    // ==========================================
    private boolean sendData(String url, List<?> dataList) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<?>> request = new HttpEntity<>(dataList, headers);
            restTemplate.postForObject(url, request, String.class);
            return true;
        } catch (Exception e) {
            log.error("LMS 전송 실패 [URL: {}]: {}", url, e.getMessage());
            return false;
        }
    }

    private boolean isStatusChanged(EquipmentRequestDto current) {
        if (!prevStatusMap.containsKey(current.getEquipId())) return true;
        EquipmentRequestDto prev = prevStatusMap.get(current.getEquipId());

        return !(Objects.equals(current.getCurrentStatus(), prev.getCurrentStatus()) &&
                Objects.equals(current.getErrCd(), prev.getErrCd()) &&
                Objects.equals(current.getOrderId(), prev.getOrderId()));
    }
}