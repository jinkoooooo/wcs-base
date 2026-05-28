package operato.logis.connector.plc.gms;

import operato.logis.connector.plc.s7.AbstractS7PlcConnector;
import operato.logis.connector.plc.gms.dto.S7PlcStatusDto;
import operato.logis.connector.plc.s7.exception.S7Exception;
import operato.logis.connector.plc.s7.type.AreaType;
import operato.logis.connector.plc.s7.type.DataType;
import operato.logis.connector.plc.s7.util.S7;

/**
 * S7 PLC 통신 커넥터
 * - 설비별로 인스턴스 분리
 */
public class S7PlcConnector extends AbstractS7PlcConnector {

    private static final int SIZE_WRITE_BYTE = 2;

    public S7PlcConnector(String host, int rack, int slot) {
        initClient(host, rack, slot);
    }

    /**
     * PLC 상태 읽기
     * - DB 블록 READ -> S7PlcStatusDto 파싱
     *
     * @param statusDb  상태 DB 번호
     * @param equipId   설비 ID
     * @param ecsTaskId 폴백 ECS 작업 번호
     * @return 파싱된 PLC 상태 DTO, 읽기 실패 시 null
     */
    public S7PlcStatusDto readStatus(int statusDb, String equipId, String ecsTaskId) {
        byte[] buffer = new byte[S7PlcStatusMapper.SIZE_READ_BYTE];
        try {
            int result = client.readArea(AreaType.DB, statusDb, 0, S7PlcStatusMapper.SIZE_READ_BYTE, DataType.BYTE, buffer);
            if (result <= 0) {
                logger.warn("readStatus returned 0 bytes (equip = {}, DB{})", equipId, statusDb);
                return null;
            }
            return S7PlcStatusMapper.map(buffer, equipId, ecsTaskId);
        } catch (S7Exception e) {
            logger.error("readStatus failed (equip={}, DB{}): {}", equipId, statusDb, e.getMessage());
            return null;
        }
    }

    /**
     * ECS -> PLC 쓰기
     *
     * @param taskDb DB 번호
     * @param value  쓸 값
     */
    public void writeTaskCommand(int taskDb, short value) {
        byte[] buffer = new byte[SIZE_WRITE_BYTE];
        S7.setShortAt(buffer, 0, value);
        try {
            boolean result = client.writeArea(AreaType.DB, taskDb, 0, SIZE_WRITE_BYTE, DataType.BYTE, buffer);
            logger.debug("writeTaskCommand DB{} value = {} result = {}", taskDb, value, result);
        } catch (S7Exception e) {
            logger.error("writeTaskCommand failed (DB{}): {}", taskDb, e.getMessage());
        }
    }

    /**
     * ECS -> PLC 쓰기
     * @param dbNumber PLC DB 번호
     * @param offset
     * @param value 쓸 값
     */
    public void writeCommand(int dbNumber, int offset, short value) {
        byte[] buffer = new byte[SIZE_WRITE_BYTE];
        S7.setShortAt(buffer, 0, value);
        try {
            boolean result = client.writeArea(AreaType.DB, dbNumber, offset, SIZE_WRITE_BYTE, DataType.BYTE, buffer);
            logger.debug("writeTaskCommand DB{} offset = {} value = {} result = {}", dbNumber, offset, value, result);
        } catch (S7Exception e) {
            logger.error("writeTaskCommand failed (DB{}): {}", dbNumber, e.getMessage());
        }
    }
}
