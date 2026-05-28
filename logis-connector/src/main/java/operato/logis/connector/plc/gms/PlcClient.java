package operato.logis.connector.plc.gms;

import operato.logis.connector.plc.s7.AbstractS7PlcConnector;
import operato.logis.connector.plc.s7.exception.S7Exception;
import operato.logis.connector.plc.s7.type.AreaType;
import operato.logis.connector.plc.s7.type.DataType;
import operato.logis.connector.plc.s7.util.S7;

/**
 * todo: Connect로 health check 이관 후 삭제
 * S7 PLC 클라이언트: 헬스체크
 */
public class PlcClient extends AbstractS7PlcConnector {

    private static final int HEARTBEAT_DB     = 200;
    private static final int HEARTBEAT_OFFSET = 0;
    private static final int HEARTBEAT_SIZE   = 4; // DINT

    private int lastHeartBeat = -1;
    private boolean healthy   = false;

    public PlcClient(String host, int rack, int slot) {
        initClient(host, rack, slot);
    }

    public boolean isHealthy() {
        return healthy;
    }

    /**
     * PLC 통신 상태 확인 (heartbeat DB 읽기)
     * 직전 값과 동일하면 unhealthy, 다르면 healthy로 판정
     */
    public void checkHealth() {
        if (!isConnected()) {
            healthy = false;
            logger.warn("[PlcClient] checkHealth skipped. not connected");
            return;
        }

        byte[] buffer = new byte[HEARTBEAT_SIZE];
        try {
            int result = client.readArea(AreaType.DB, HEARTBEAT_DB, HEARTBEAT_OFFSET, HEARTBEAT_SIZE, DataType.BYTE, buffer);
            if (result == 0) {
                int currHB = S7.getDIntAt(buffer, HEARTBEAT_OFFSET);
                healthy = currHB != lastHeartBeat;
                lastHeartBeat = currHB;
                logger.debug("[PlcClient] heartbeat = {} healthy = {}", currHB, healthy);
            }
        } catch (S7Exception e) {
            logger.error("[PlcClient] checkHealth failed: {}", e.getMessage());
            healthy = false;
        }
    }
}
