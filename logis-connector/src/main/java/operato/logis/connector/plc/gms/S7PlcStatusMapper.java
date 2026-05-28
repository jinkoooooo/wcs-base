package operato.logis.connector.plc.gms;

import lombok.AllArgsConstructor;
import lombok.Getter;
import operato.logis.connector.plc.gms.dto.S7PlcStatusDto;
import operato.logis.connector.plc.s7.S7Client;
import operato.logis.connector.plc.s7.type.DataType;
import operato.logis.connector.plc.s7.util.S7;

import java.nio.charset.StandardCharsets;

public class S7PlcStatusMapper {

    // ECS -> PLC 명령 블록
    @Getter
    @AllArgsConstructor
    public enum ScWriteAddress {
        OPERATION_TYPE(0, 0, DataType.WORD, "동작모드"), // 0:작업없음, 1:입고, 2:출고, 3:홈복귀, 4:자재정리
        START_TASK_FLAG(2, 2, DataType.WORD, "작업 시작 플래그"), // 0:대기, 1:시작
        ECS_TASK_ID(4, 4, DataType.REAL, "ECS 작업 번호 (yyyyMMdd + 일련번호 6자리 + 작업구분 2자리"),
        ECS_TASK_ID1(4, 4, DataType.WORD, "ECS 작업 번호 (yyyy)"),
        ECS_TASK_ID2(6, 6, DataType.WORD, "ECS 작업 번호 (MMdd)"),
        ECS_TASK_ID3(8, 8, DataType.WORD, "ECS 작업 번호 (일련번호 4자리)"),
        ECS_TASK_ID4(10, 10, DataType.WORD, "ECS 작업 번호 (일련번호 2자리 + 입/출/정리 구분 2자리)"),
        SRT_COL(12, 12, DataType.WORD, "작업 시작 열"), // 1,3:left, 2,4:right
        SRT_ROW(14, 14, DataType.WORD, "작업 시작 행"),
        SRT_LEVEL(16, 16, DataType.WORD, "작업 시작 단"),
        SRT_STATION_NO(18, 18, DataType.WORD, "입고대"),
        END_COL(20, 20, DataType.WORD, "작업 종료 열"), // 1,3:left, 2,4:right
        END_ROW(22, 22, DataType.WORD, "작업 종료 행"),
        END_LEVEL(24, 24, DataType.WORD, "작업 종료 단"),
        END_STATION_NO(26, 26, DataType.WORD, "출고대"),
        TASK_CLEAR_FLAG(28, 28, DataType.WORD, "작업 완료/취소 플래그"),
        ERROR_RESET_FLAG(30, 30, DataType.WORD, "에러 리셋 플래그"),
        EMG_STOP(32, 32, DataType.WORD, "S/W 비상정지 플래그"),
        FORK_DEPTH(34, 34, DataType.WORD, "포크 깊이"); // 1:1단, 2:2단

        private final int dbNumber;
        private final int offset;
        private final DataType dataType;
        private final String description;
    }

    // PLC -> ECS 상태 보고 블록
    @Getter
    @AllArgsConstructor
    public enum ScReadAddress {
        OPERATION_MODE(50, 50, DataType.WORD, "운전 모드"), // 1:manual, 2:semi-auto, 3:ecs
        OPERATION_STATUS(52, 52, DataType.WORD, "운전 상태"), // 1:ready, 2:in-progress, 3:error
        ECS_TASK_ID(54, 54, DataType.REAL, "ECS 작업 번호 (yyyyMMdd + 일련번호 6자리 + 작업구분 2자리"),
        ECS_TASK_ID1(54, 54, DataType.WORD, "ECS 작업 번호 (yyyy)"),
        ECS_TASK_ID2(56, 56, DataType.WORD, "ECS 작업 번호 (MMdd)"),
        ECS_TASK_ID3(58, 58, DataType.WORD, "ECS 작업 번호 (일련번호 4자리)"),
        ECS_TASK_ID4(60, 60, DataType.WORD, "ECS 작업 번호 (일련번호 2자리 + 입/출/정리 구분 2자리)"),
        SRT_POINT_COL(62, 62, DataType.WORD, "작업 시작 열"), // 1,3:left, 2,4:right
        SRT_POINT_ROW(64, 64, DataType.WORD, "작업 시작 행"),
        SRT_POINT_LEVEL(66, 66, DataType.WORD, "작업 시작 단"),
        SRT_STATION_NO(68, 68, DataType.WORD, "입고대"),
        END_POINT_COL(70, 70, DataType.WORD, "작업 종료 열"), // 1,3:left, 2,4:right
        END_POINT_ROW(72, 72, DataType.WORD, "작업 종료 행"),
        END_POINT_LEVEL(74, 74, DataType.WORD, "작업 종료 단"),
        END_STATION_NO(76, 76, DataType.WORD, "출고대"),
        CURR_POINT_COL(78, 78, DataType.WORD, "현재 위치 열"), // 1,3:left, 2,4:right
        CURR_POINT_ROW(80, 80, DataType.WORD, "현재 위치 행"),
        CURR_POINT_LEVEL(82, 82, DataType.WORD, "현재 위치 단"),
        CURR_X_POS(84, 84, DataType.WORD, "현재 위치 X"),
        CURR_Y_POS(86, 86, DataType.WORD, "현재 위치 Y"),
        COMPLETE_YN(88, 88, DataType.WORD, "작업 완료 여부"),
        LOAD_YN(90, 90, DataType.WORD, "화물 유무"),
        FORK_STATUS(92, 92, DataType.WORD, "포크 상태"),
        ERROR_PLC_CD(94, 94, DataType.WORD, "PLC 에러 코드"),
        ERROR_WMS_CD(96, 96, DataType.WORD, "WMS 에러 코드"),
        REORDER_REQ_YN(98, 98, DataType.WORD, "재지시 요청 여부"),
        EMG_STOP_YN(100, 100, DataType.WORD, "S/W 비상정지 여부"),
        CYCLE_TIME(200, 200, DataType.DINT, "사이클 타임");

        private final int dbNumber;
        private final int offset;
        private final DataType dataType;
        private final String description;
    }

    // Read buffer size
    public static final int SIZE_READ_BYTE = 204;

    // 작업번호 길이 (8 bytes)
    private static final int TASK_ID_BYTE_LEN = 8;

    private S7PlcStatusMapper() {}

    /**
     * PLC DB 블록 byte[] 버퍼 -> S7PlcStatusDto 변환
     *
     * @param buffer    {@link S7Client#readArea} 로 읽어온 byte 배열 (최소 {@value #SIZE_READ_BYTE} bytes)
     * @param equipId   설비 ID
     * @param ecsTaskId fallback ECS 작업 번호 (버퍼 내 두 taskId 필드가 모두 비어 있을 경우 사용)
     * @return 변환된 DTO
     */
    public static S7PlcStatusDto map(byte[] buffer, String equipId, String ecsTaskId) {
        S7PlcStatusDto dto = new S7PlcStatusDto();

        dto.setEquipId(equipId);

        //DBW0 ~ DBW34: ECS -> PLC 명령 블록
        dto.setOperationType(S7.getShortAt(buffer, ScWriteAddress.OPERATION_TYPE.getOffset()));
        dto.setStartTaskFlag(S7.getShortAt(buffer, ScWriteAddress.START_TASK_FLAG.getOffset()) != 0);
        String ecsCommandedTaskId = readAscii(buffer, ScWriteAddress.ECS_TASK_ID.getOffset(), TASK_ID_BYTE_LEN);
        dto.setSrtPointCol(S7.getShortAt(buffer, ScWriteAddress.SRT_COL.getOffset()));
        dto.setSrtPointRow(S7.getShortAt(buffer, ScWriteAddress.SRT_ROW.getOffset()));
        dto.setSrtPointLevel(S7.getShortAt(buffer, ScWriteAddress.SRT_LEVEL.getOffset()));
        dto.setSrtStationNo(S7.getShortAt(buffer, ScWriteAddress.SRT_STATION_NO.getOffset()));
        dto.setEndPointCol(S7.getShortAt(buffer, ScWriteAddress.END_COL.getOffset()));
        dto.setEndPointRow(S7.getShortAt(buffer, ScWriteAddress.END_ROW.getOffset()));
        dto.setEndPointLevel(S7.getShortAt(buffer, ScWriteAddress.END_LEVEL.getOffset()));
        dto.setEndStationNo(S7.getShortAt(buffer, ScWriteAddress.END_STATION_NO.getOffset()));
        dto.setTaskClearFlag(S7.getShortAt(buffer, ScWriteAddress.TASK_CLEAR_FLAG.getOffset()) != 0);
        dto.setErrorReset(S7.getShortAt(buffer, ScWriteAddress.ERROR_RESET_FLAG.getOffset()) != 0);
        dto.setEcsEmgStop(S7.getShortAt(buffer, ScWriteAddress.EMG_STOP.getOffset()) != 0);
        dto.setForkDepth(S7.getShortAt(buffer, ScWriteAddress.FORK_DEPTH.getOffset()));

        // DBW50 ~ DBW100: PLC -> ECS 상태 보고 블록
        dto.setOperationMode(S7.getShortAt(buffer, ScReadAddress.OPERATION_MODE.getOffset()));
        dto.setOperationStatus(S7.getShortAt(buffer, ScReadAddress.OPERATION_STATUS.getOffset()));

        // PLC 수신 확인용 작업 번호 (DBW54~DBW60, ASCII 8 bytes)
        // 우선순위: PLC echo → ECS 명령 → 파라미터 폴백
        // TODO: 에러 발생 로직 추가
        String plcEchoTaskId = readAscii(buffer, ScReadAddress.ECS_TASK_ID.getOffset(), TASK_ID_BYTE_LEN);
        dto.setEcsTaskId(!plcEchoTaskId.isEmpty() ? plcEchoTaskId
                : !ecsCommandedTaskId.isEmpty() ? ecsCommandedTaskId
                : ecsTaskId);

        // DBW62~DBW76: ECS 명령 블록 위치와 동일한 echo / DTO 에는 ECS 명령 블록 값 사용
        // TODO: 에러 발생 로직 추가
        dto.setCurrPointCol(S7.getShortAt(buffer, ScReadAddress.CURR_POINT_COL.getOffset()));
        dto.setCurrPointRow(S7.getShortAt(buffer, ScReadAddress.CURR_POINT_ROW.getOffset()));
        dto.setCurrPointLevel(S7.getShortAt(buffer, ScReadAddress.CURR_POINT_LEVEL.getOffset()));
        dto.setCurrXPos(S7.getShortAt(buffer, ScReadAddress.CURR_X_POS.getOffset()));
        dto.setCurrYPos(S7.getShortAt(buffer, ScReadAddress.CURR_Y_POS.getOffset()));

        dto.setCompleteYn(S7.getShortAt(buffer, ScReadAddress.COMPLETE_YN.getOffset()) != 0);
        dto.setLoadYn(S7.getShortAt(buffer, ScReadAddress.LOAD_YN.getOffset()) != 0);
        dto.setForkStatus(S7.getShortAt(buffer, ScReadAddress.FORK_STATUS.getOffset()));
        dto.setErrorPlcCd(S7.getShortAt(buffer, ScReadAddress.ERROR_PLC_CD.getOffset()));
        dto.setErrorWmsCd(S7.getShortAt(buffer, ScReadAddress.ERROR_WMS_CD.getOffset()));
        dto.setReorderReqYn(S7.getShortAt(buffer, ScReadAddress.REORDER_REQ_YN.getOffset()) != 0);
        dto.setEmgStopYn(S7.getShortAt(buffer, ScReadAddress.EMG_STOP_YN.getOffset()) != 0);

        dto.setCycleTime((long) S7.getDIntAt(buffer, ScReadAddress.CYCLE_TIME.getOffset()));

        return dto;
    }

    /**
     * byte[] 의 특정 범위를 US-ASCII 문자열로 읽어 null 바이트 및 공백을 제거한다.
     */
    private static String readAscii(byte[] buffer, int offset, int length) {
        return new String(S7.getBytesAt(buffer, offset, length), StandardCharsets.US_ASCII)
                .replace("\0", "").trim();
    }
}
