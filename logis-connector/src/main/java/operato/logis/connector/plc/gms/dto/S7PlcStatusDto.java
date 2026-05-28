package operato.logis.connector.plc.gms.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class S7PlcStatusDto {

    private String equipId; // 설비 ID

    // ECS -> PLC 명령 블록
    private String ecsTaskId; // ECS 작업 번호 (DBW4~DBW10)
    private Integer operationType; // 작업 유형
    private Boolean startTaskFlag; // 작업 시작 플래그
    private Integer srtPointCol; // 시작위치
    private Integer srtPointRow;
    private Integer srtPointLevel;
    private Integer srtStationNo;
    private Integer endPointCol; // 종료위치
    private Integer endPointRow;
    private Integer endPointLevel;
    private Integer endStationNo;
    private Boolean taskClearFlag; // 작업 취소 플래그
    private Boolean errorReset; // 에러 리셋 플래그
    private Boolean ecsEmgStop; // ECS 비상정지 플래그
    private Integer forkDepth; // 포크 깊이

    // PLC -> ECS 보고 블록
    private Integer operationMode; // 조작모드
    private Integer operationStatus; // 작업상태
    private Integer currPointCol; // 현재위치
    private Integer currPointRow;
    private Integer currPointLevel;
    private Integer currXPos;
    private Integer currYPos;
    private Boolean completeYn; // 작업 완료 여부
    private Boolean loadYn; // 하중 감지 여부
    private Integer forkStatus; // 포크 상태
    private Integer errorPlcCd; // PLC 에러 코드
    private Integer errorWmsCd; // WMS 에러 코드
    private Boolean reorderReqYn; // 재지시 요청 여부
    private Boolean emgStopYn; // PLC 비상정지 여부
    private Long cycleTime; // 사이클타임

    private String snapshotSensorData; // 센서데이터 스냅샷
}
