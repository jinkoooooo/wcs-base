package operato.logis.samsung.cognex.service;

import operato.logis.connector.gtr.dto.InspectionRequestDto;
import operato.logis.connector.gtr.service.InspectionService;
import operato.logis.samsung.cognex.core.DataReceiver;
import operato.logis.samsung.cognex.core.SocketClient;
import operato.logis.samsung.cognex.util.CreateUuid;
import operato.logis.samsung.cognex.util.FieldStatus;
import operato.logis.samsung.consts.BoxTrackingEventType;
import operato.logis.samsung.event.BoxTrackingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.util.*;

/**
 * SocketClient의 DataReceiver 역할을 수행하며,
 * Heartbeat 메시지 필터링, BCR/Vision 데이터 파싱 및 DB에 저장하는 핵심 비즈니스 로직을 처리합니다.
 *
 * ⭐️ 수정: DataReceiver 인터페이스의 handleReceivedData(Object, String) 시그니처를 구현합니다.
 */
@Service
public class DataParserService extends AbstractQueryService implements DataReceiver {

    private static final Logger log = LoggerFactory.getLogger(DataParserService.class);

    private final ApplicationEventPublisher publisher;
    private final InspectionService inspectionService;

    public DataParserService(ApplicationEventPublisher publisher, InspectionService inspectionService) {
        this.publisher = publisher;
        this.inspectionService = inspectionService;
    }

    // 통신 규격 정의
    private static final String STX = "\u0002"; // Start of Text (0x02)
    private static final String ETX = "\u0003"; // End of Text (0x03)
    private static final String DELIMITER = ",";
    private static final String HEARTBEAT_CONTENT = "Heartbeat"; // Heartbeat 메시지 본문
    // 예: DeviceName, FieldName, Status (String)
    // 현재 메모리에 저장된 최종 상태 (예: "0"(ACTIVE) 또는 "1"(INACTIVE))
    private Map<String, Map<String, String>> deviceFieldCache = new HashMap<>();

    // (SQL 쿼리 상수들은 이전 코드와 동일하게 유지됩니다)
    // Heartbeat 상태 업데이트 DB 쿼리 정의
    private static final String UPDATE_HEARTBEAT_SQL = """
            UPDATE samsung_mw.tb_mw_unit_heartbeat 
                SET status = :status, 
                    msg = :msg, 
                    updated_at = CURRENT_TIMESTAMP, 
                    updater_id = 'WCS' 
                WHERE unit_code = :unitCode 
                    AND instance_id = :instanceId;
            """;

    // Heartbeat 상태 업데이트 DB 쿼리 정의
    private static final String UPDATE_HEARTBEAT_SQL_ALL = """
            UPDATE samsung_mw.tb_mw_unit_heartbeat 
                SET status = :status, 
                    msg = :msg, 
                    updated_at = CURRENT_TIMESTAMP, 
                    updater_id = 'WCS' 
                WHERE unit_code = :unitCode;
            """;

    // BCR 데이터 삽입 DB 쿼리 정의
    private static final String INSERT_BCR_DATA_SQL =
            "INSERT INTO tb_mw_bcr_data (" +
                    "id, " +
                    "seqno, " +
                    "length, " +
                    "width, " +
                    "height, " +
                    "angle, " +
                    "barcodedata, " +
                    "reg_dt, " +
                    "reg_no," +
                    "item_code," +
                    "device_name" +
                    ") VALUES (" +
                    ":uuid, " +
                    ":seqNo, " +
                    ":length, " +
                    ":width, " +
                    ":height, " +
                    ":angle, " +
                    ":rawBarcodeData, " +
                    "NOW(), " +
                    "'WCS'," +
                    ":itemCode," +
                    ":deviceName" +
                    ")";

    // VISION 결과 삽입 DB 쿼리 정의
    private static final String INSERT_VISION_DATA_SQL =
            "INSERT INTO tb_mw_vision_data (" +
                    "id, " +
                    "seqno, " +
                    "receive_time, " +
                    "fileNameTop, " +
                    "fileNameFront, " +
                    "fileNameBack, " +
                    "fileNameLeft, " +
                    "fileNameRight, " +
                    "fileNameBottomLeft, " +
                    "fileNameBottomRight, " +
                    "result, " +
                    "reg_dt, " +
                    "reg_no, " +
                    "result_code" +
                    ") VALUES (" +
                    ":uuid, " +
                    ":seqNo, " +
                    ":receiveTime, " +
                    ":fileNameTop, " +
                    ":fileNameFront, " +
                    ":fileNameBack, " +
                    ":fileNameLeft, " +
                    ":fileNameRight, " +
                    ":fileNameBottomLeft, " +
                    ":fileNameBottomRight, " +
                    ":visionResult, " +
                    "NOW(), " +
                    "'WCS', " +
                    ":result_code" +
                    ")";

    /**
     * DataReceiver 인터페이스 구현: StreamAssembler로부터 완전한 메시지를 전달받아 처리합니다.
     * ⭐️ 수정: (Object clientContext, String rawData) 시그니처로 변경
     *
     * @param clientContext 메시지를 수신한 SocketClient 인스턴스 (Object 타입)
     * @param rawData StreamAssembler가 조립한 원본 메시지 (STX/ETX 포함)
     */
    @Override
    public void handleReceivedData(Object clientContext, String rawData) {

        // ========================================================================
        // 1. ⭐️ 컴파일 오류 수정: Object 타입을 String deviceName으로 캐스팅
        // ========================================================================
        final String deviceName;

        // StreamAssembler가 SocketClient 인스턴스 자체를 전달했다고 가정
        if (clientContext instanceof SocketClient) {
            deviceName = ((SocketClient) clientContext).getName();
        }
        // StreamAssembler가 clientContext로 이름(String)을 전달한 경우 (이전 버전 호환)
        else if (clientContext instanceof String) {
            deviceName = (String) clientContext;
        } else {
            // String 타입이 아닐 경우 오류 처리 후 메서드 종료
            log.error("클라이언트 컨텍스트 타입 오류: 예상된 String 또는 SocketClient가 아닌 {} 타입입니다. 처리를 중단합니다.",
                    clientContext != null ? clientContext.getClass().getName() : "null");
            return;
        }

        // ------------------------------------------------------------------------

        // log.info("[{}] 데이터 수신 완료: {}", deviceName, rawData);

        // 1. STX/ETX 제거 및 Heartbeat 메시지 필터링
        String innerData = stripStxEtxAndCheckHeartbeat(rawData);

        if (innerData == null) {
            // Heartbeat 메시지이거나, 잘못된 포맷으로 처리되었으므로 즉시 종료
            return;
        }

        // 2. 데이터 유형별 분기 처리 (쉼표로 분리)
        String[] parts = innerData.split(DELIMITER);

        // ⭐️ 수정: 캐스팅된 deviceName 변수를 사용하여 startsWith 호출
        if (deviceName.startsWith("BCR")) {
            if (parts.length <= 7) {
                handleBcrData(deviceName, parts, innerData);
            } else if(parts.length == 22) {
                handleHeartbeat(deviceName, parts, innerData);
            }else {
                log.error("[{}] BCR 데이터 포맷 오류 (요소 개수: {}): {}", deviceName, parts.length, innerData);
            }
        } else if (deviceName.startsWith("VISION")) {
            if (parts.length <= 10) {
                handleVisionData(deviceName, parts, innerData);
            } else if(parts.length == 14) {
                handleHeartbeat(deviceName, parts, innerData);
            }else {
                log.error("[{}] VISION 데이터 포맷 오류 (요소 개수: {}): {}", deviceName, parts.length, innerData);
            }
        } else {
            log.error("알 수 없는 장치 이름 [{}] 또는 데이터 포맷: {}", deviceName, innerData);
        }
    }

    /**
     * STX/ETX를 제거하고 Heartbeat 메시지인지 확인합니다.
     * (이전 코드와 동일)
     */
    private String stripStxEtxAndCheckHeartbeat(String rawData) {
        String strippedData = rawData;
        int maxAttempts = 2; // 최대 이중 캡슐화 처리

        for (int i = 0; i < maxAttempts; i++) {
            if (strippedData.startsWith(STX) && strippedData.endsWith(ETX)) {
                // STX/ETX 제거
                strippedData = strippedData.substring(STX.length(), strippedData.length() - ETX.length());
            } else {
                // 더 이상 STX/ETX 캡슐화가 없으면 루프 종료
                break;
            }
        }

        // Heartbeat 체크는 STX/ETX가 모두 제거된 최종 문자열에서 수행
        if (HEARTBEAT_CONTENT.equals(strippedData)) {
            log.debug("Heartbeat 메시지를 수신했습니다. 데이터 파싱을 건너뜁니다.");
            return null; // Heartbeat 메시지이므로 null 반환하여 처리 종료
        }

        // 최종적으로 Heartbeat가 아니며, 비어있지 않다면 순수 데이터 반환
        if (strippedData.length() > 0) {
            return strippedData;
        } else {
            // Heartbeat가 아닌데 데이터가 비어있거나, 잘못된 STX/ETX 포맷인 경우
            log.error("잘못된 데이터 포맷 또는 빈 메시지 수신 후 STX/ETX 제거 완료. 원본: {}", rawData);
            return null;
        }
    }


    /**
     * BCR 데이터 (Seq, L, W, H, Barcode)를 처리하고 DB에 저장합니다.
     * ⭐️ 수정: deviceName 파라미터 추가
     */
    private void handleBcrData(String deviceName, String[] parts, String innerData) {
        try {
            // 필드 추출 및 변환
            String seqNo = parts[0];
            int length = Integer.parseInt(parts[1]);
            int width = Integer.parseInt(parts[2]);
            int height = Integer.parseInt(parts[3]);
            int angle = Integer.parseInt(parts[4]);
            String barcodeData = parts[5]; // Barcode 또는 NoRead
            String itemCode = (parts.length > 6) ? parts[6] : "";
            String bcrNo = deviceName;
            String lineId = deviceName.equals("BCR-01") ? "P101" : "P102";
            String parcelId = "";
            String equipId = deviceName;

            Domain newDomain = new Domain("7");

            Domain.setCurrentDomain(newDomain);
            if ("0000".equals(seqNo) || "000".equals(seqNo)){
                log.info("[{}], seq:{} 값이 0000 or 000은 BoxTrackingEvent를 실행시키지 않습니다. ", deviceName, seqNo);
            }else{
                log.info("[{}] BoxTrackingEvent 시작", deviceName);

                BoxTrackingEvent event = BoxTrackingEvent.builder()
                        .bcrNo(bcrNo)
                        .eventType(BoxTrackingEventType.BCR_EVENT)
                        .plcSeqNo(seqNo)
                        .barcode(barcodeData)
                        .parcelId(parcelId)
                        .itemCode(itemCode)
                        .lineId(lineId)
                        .equipId(equipId)
                        .boxLength(length)
                        .boxWidth(width)
                        .boxHeight(height)
                        .boxAngle(angle)
                        .measuredAt(new Date())
                        .build();

                publisher.publishEvent(event);
            }

            String uuid = CreateUuid.generateUUID();

            log.info("[{}] [BCR Parsed] Seq: {}, Volume: {}x{}x{}, Angle : {}, Barcode: {},itemCode: {}",
                    deviceName, seqNo, length, width, height, angle,barcodeData, itemCode);

            // DB 저장 로직 호출
            saveBcrDataToDatabase(uuid, seqNo, length, width, height, angle, barcodeData, itemCode, deviceName);


//            if ("BCR-02".equals(bcrNo)){
//
//            // 2. InspectionService 호출을 위한 DTO 구성
//            InspectionRequestDto requestDto = new InspectionRequestDto();
//            // transactionId는 seqNo 값과 동일하게 설정하여 서비스에서 VisionValue를 조회할 키로 사용합니다.
//            requestDto.setTransactionId(seqNo);
//
//            String siteId = "hwaseong/damage-detection"; // 고정 사이트 ID (필요에 따라 설정)
//
//            // 3. InspectionService 호출 및 비동기 실행 시작
//            // 삼성생기연 API 호출
//            inspectionService.requestInspection(siteId, requestDto)
//                    .subscribe(
//                            resultMap -> log.info("[GTR_CONN] Inspection API 호출 성공. Seq: {}", seqNo),
//                            error -> log.error("[GTR_CONN] Inspection API 호출 실패. Seq: {}", seqNo, error)
//                    );
//
//            }

        } catch (NumberFormatException e) {
            log.error("[{}] BCR 데이터: 부피 정보 (L, W, H) 변환 실패. 데이터: {}", deviceName, innerData, e);
        } catch (Exception e) {
            log.error("[{}] BCR 데이터 처리 중 알 수 없는 오류 발생. 데이터: {}", deviceName, innerData, e);
        }
    }

    /**
     * Vision 데이터 (ReceiveTime, SeqNo, Result)를 처리하고 DB에 삽입합니다.
     * ⭐️ 수정: deviceName 파라미터 추가
     */
    private void handleVisionData(String deviceName, String[] parts, String innerData) {
        try {

            String receiveTime = parts[0]; // YYYYMMDDhhmmss
            String seqNo = parts[1]; // 001
            String visionResult = parts[2]; // 1: OK, 2: NG
            String fileNameTop = "";
            String fileNameFront = "";
            String fileNameBack = "";
            String fileNameLeft = "";
            String fileNameRight = "";
            String fileNameBottomLeft = "";
            String fileNameBottomRight = "";
            String bcrNo = deviceName;
            String lineId = "P101";
            String parcelId = "";
            String equipId = deviceName;
            int resultValue = Integer.parseInt(visionResult);

            for(int i = 0; i < parts.length; i++  ) {
                String currentFileName = parts[i].trim(); // 공백 제거

                if (currentFileName.contains("_TP_")) {
                    // 인덱스 3번에 해당하는 변수에 할당 (Top View)
                    fileNameTop = currentFileName;
                } else if (currentFileName.contains("_FR_")) {
                    // 인덱스 4번에 해당하는 변수에 할당 (Front View)
                    fileNameFront = currentFileName;
                } else if (currentFileName.contains("_RE_")) {
                    fileNameBack = currentFileName;
                } else if (currentFileName.contains("_LE_")) {
                    fileNameLeft = currentFileName;
                } else if (currentFileName.contains("_RI_")) {
                    fileNameRight = currentFileName;
                } else if (currentFileName.contains("_BR_")) {
                    fileNameBottomLeft = currentFileName;
                } else if (currentFileName.contains("_BL_")) {
                    fileNameBottomRight = currentFileName;
                }
            }

            Domain newDomain = new Domain("7");

            Domain.setCurrentDomain(newDomain);

            if ("0000".equals(seqNo) || "000".equals(seqNo)){
                log.info("[{}], seq:{} 값이 0000 or 000은 BoxTrackingEvent를 실행시키지 않습니다. ", deviceName, seqNo);
            }else{
                log.info("[{}] BoxTrackingEvent 시작", deviceName);

                BoxTrackingEvent event = BoxTrackingEvent.builder()
                        .bcrNo(bcrNo)
                        .eventType(BoxTrackingEventType.VISION_EVENT) // 여기서 에러나
                        .plcSeqNo(seqNo)
                        .parcelId(parcelId)
                        .lineId(lineId)
                        .equipId(equipId)
                        .cognexVisionResult(resultValue)
                        .fileNameTop(fileNameTop)
                        .fileNameFront(fileNameFront)
                        .fileNameBack(fileNameBack)
                        .fileNameLeft(fileNameLeft)
                        .fileNameRight(fileNameRight)
                        .fileNameBottomLeft(fileNameBottomLeft)
                        .fileNameBottomRight(fileNameBottomRight)
                        .measuredAt(new Date())
                        .build();

                publisher.publishEvent(event);
            }

            String uuid = CreateUuid.generateUUID(); // Primary Key

            String resultText = "1".equals(visionResult) ? "OK" : ("2".equals(visionResult) ? "NG" : "UNKNOWN");

            log.info("[{}] [Vision Parsed] Seq: {}, Time: {}, Result: {} ({})",
                    deviceName, seqNo, receiveTime, visionResult, resultText);

            // Vision 결과를 DB에 삽입하는 로직
            saveVisionResultToDatabase(uuid, seqNo, receiveTime, visionResult, resultText,fileNameTop,
                    fileNameFront, fileNameBack, fileNameLeft, fileNameRight, fileNameBottomLeft, fileNameBottomRight) ;

        } catch (Exception e) {
            log.error("[{}] Vision 데이터 처리 중 오류 발생. 데이터: {}", deviceName, innerData, e);
        }
    }

    private void handleHeartbeat(String deviceName, String[] parts, String innerData) {
        try {
            List<FieldStatus> changedFieldList = new ArrayList<>();

            // 1. 해당 디바이스의 현재 필드 캐시 상태를 가져옵니다.
            Map<String, String> fieldCache =
                    deviceFieldCache.computeIfAbsent(deviceName, k -> new HashMap<>());

            // parts 배열을 2칸씩 건너뛰며 반복 (필드명, 값 순서)
            for (int i = 0; i < parts.length; i += 2) {
                String fieldName = parts[i];
                int statusCode = Integer.parseInt(parts[i + 1]);
                String newStatus = convertToStatus(statusCode);

                // 2. 변경 감지: 캐시된 상태(oldStatus)와 새로 들어온 상태(newStatus) 비교
                String oldStatus = fieldCache.get(fieldName);

                if (!newStatus.equals(oldStatus)) {
                    // 상태가 변경됨! -> DB에 저장할 리스트에 추가
                    changedFieldList.add(new FieldStatus(fieldName, newStatus));
                    // 3. 캐시 업데이트
                    fieldCache.put(fieldName, newStatus);
                }
            }

            // 4. 변경된 데이터가 있을 경우에만 DB 저장 로직 호출
            if (!changedFieldList.isEmpty()) {
                log.info("[{}] {}개 필드 상태 변경 감지. DB 업데이트 실행.", deviceName, changedFieldList.size());
                updateHeartbeatStatus(deviceName, changedFieldList); // 기존 DB 업데이트 메서드 사용
            }

        } catch (NumberFormatException e) {
            log.error("[{}] 필드 상태 값 변환 실패. 데이터: {}", deviceName, innerData, e);
        } catch (Exception e) {
            log.error("[{}] 필드 상태 처리 중 알 수 없는 오류 발생. 데이터: {}", deviceName, innerData, e);
        }
    }


    /**
     * BCR 데이터를 DB에 저장하는 실제 로직
     * (이전 코드와 동일)
     */
    private void saveBcrDataToDatabase(String uuid, String seqNo, int length, int width, int height, int angle, String rawBarcodeData, String itemCode, String deviceName) {
        String sql = INSERT_BCR_DATA_SQL;
        Map<String, Object> params = new HashMap<>();
        params.put("uuid", uuid);
        params.put("seqNo", seqNo);
        params.put("length", length);
        params.put("width", width);
        params.put("height", height);
        params.put("angle", angle);
        params.put("rawBarcodeData", rawBarcodeData);
        params.put("itemCode", itemCode);
        params.put("deviceName", deviceName);

        this.queryManager.executeBySql(sql,params);
        log.info("BCR 결과 DB 삽입 완료. Seq: {}, Barcode: {},  itemCode: {}", seqNo, rawBarcodeData, itemCode);
    }

    /**
     * Vision 결과를 DB에 삽입하는 실제 로직 (ecs_vision_data 테이블에 INSERT)
     * (이전 코드와 동일)
     */
    private void saveVisionResultToDatabase(String uuid, String seqNo, String receiveTime, String visionResult, String resultText, String fileNameTop,
           String fileNameFront, String fileNameBack, String fileNameLeft, String fileNameRight, String  fileNameBottomLeft, String fileNameBottomRight) {
        // ecs_vision_data 테이블에 새로운 Vision 결과를 삽입합니다.
        String sql = INSERT_VISION_DATA_SQL;
        Map<String, Object> params = new HashMap<>();
        params.put("uuid", uuid);
        params.put("seqNo", seqNo);
        params.put("receiveTime", receiveTime);
        params.put("visionResult", visionResult);
        params.put("result_code", resultText);
        params.put("fileNameTop", fileNameTop);
        params.put("fileNameFront", fileNameFront);
        params.put("fileNameBack", fileNameBack);
        params.put("fileNameLeft", fileNameLeft);
        params.put("fileNameRight", fileNameRight);
        params.put("fileNameBottomLeft", fileNameBottomLeft);
        params.put("fileNameBottomRight", fileNameBottomRight);

        // 쿼리 실행
        this.queryManager.executeBySql(sql,params);
        log.info("Vision 결과 DB 삽입 완료. Seq: {}, Result: {}", seqNo, visionResult);
    }


    /**
     * Heartbeat 상태를 DB에 업데이트하는 로직 (HeartbeatScheduler에서 호출)
     * (이전 코드와 동일)
     */
    public void updateHeartbeatStatus(String clientName, List<FieldStatus> dataList) {
        String sql = UPDATE_HEARTBEAT_SQL;
        Map<String, Object> params = new HashMap<>();
        // 리스트를 반복하면서 각 FieldStatus 객체를 DB에 저장합니다.
        for (FieldStatus data : dataList) {
            try {
                // 251217 JJG : message 값 업데이트 추가. 데시보드 뷰어용
                String msg = convertToMsg(data.getStatus());

                params.put("unitCode", clientName);
                params.put("instanceId", data.getFieldName());
                params.put("status", data.getStatus());
                params.put("msg", msg);

                this.queryManager.executeBySql(sql, params);

            } catch (Exception e) {
                log.error("[{}] {} 상태 저장 실패: {}", clientName, data.getFieldName(), e.getMessage());
            }
        }
    }

    /**
     * Heartbeat 상태를 DB에 업데이트하는 로직 (HeartbeatScheduler에서 호출)
     * (이전 코드와 동일)
     */
    public void updateHeartbeatStatusall(String clientName, String newStatus) {
        String sql = UPDATE_HEARTBEAT_SQL_ALL;
        Map<String, Object> params = new HashMap<>();
        // 리스트를 반복하면서 각 FieldStatus 객체를 DB에 저장합니다.
        try {

            // 251217 JJG : message 값 업데이트 추가. 데시보드 뷰어용
            String msg = convertToMsg(newStatus);

            params.put("unitCode", clientName);
            params.put("status", newStatus);
            params.put("msg", msg);

            this.queryManager.executeBySql(sql, params);

        } catch (Exception e) {
            log.error("[{}] 상태 저장 실패: {}", clientName, newStatus);
        }
    }

    private String convertToStatus(int code) {
        switch (code) {
            case 1:
                return "0"; // ACTIVE
            case 2:
                return "99"; // INACTIVE
            default:
                // 예상치 못한 값이 들어왔을 경우
                return "-1"; // UNKNOWN;
        }
    }

    private String convertToMsg(String status) {
        switch (status) {
            case "0":
                return "연결중";
            case "99":
                return "미연결";
            default:
                return "알 수 없음";
        }
    }
}