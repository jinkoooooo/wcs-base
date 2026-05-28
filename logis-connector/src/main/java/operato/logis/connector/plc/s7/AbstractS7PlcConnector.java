package operato.logis.connector.plc.s7;

import operato.logis.connector.plc.s7.exception.S7Exception;
import operato.logis.connector.plc.s7.type.ConnectionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * S7 PLC 연결 관리 추상 클래스
 * - 연결 초기화 / 재연결 / 종료 등 공통 생명주기 로직
 */
public abstract class AbstractS7PlcConnector {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected S7Client client;

    /**
     * PLC 연결 초기화
     * - 하위 클래스의 @PostConstruct 메서드에서 호출
     *
     * @param host PLC IP 주소
     * @param rack PLC 랙 번호
     * @param slot PLC 슬롯 번호
     */
    protected void initClient(String host, int rack, int slot) {
        client = new S7Client();

        S7Config config = new S7Config();
        config.setType(ConnectionType.OP);
        config.setHost(host);
        config.setRack(rack);
        config.setSlot(slot);
        client.setConfig(config);

        try {
            boolean connected = client.connect();
            if (connected) {
                logger.info("Connected to PLC {} (rack = {}, slot = {})", host, rack, slot);
            } else {
                logger.warn("connect() returned false for {}", host);
            }
        } catch (S7Exception e) {
            logger.error("Failed to connect to PLC {}: {}", host, e.getMessage());
        }
    }

    /**
     * PLC 연결 상태
     */
    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    /**
     * PLC 재연결 시도
     *
     * @return 재연결 성공여부
     */
    public boolean ensureConnected() {
        if (isConnected()) return true;
        try {
            boolean connected = client.connect();
            if (connected) {
                logger.info("[{}] Reconnected to PLC", getClass().getSimpleName());
            } else {
                logger.warn("[{}] Reconnection attempt failed", getClass().getSimpleName());
            }
            return connected;
        } catch (S7Exception e) {
            logger.error("[{}] Reconnect error: {}", getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    /**
     * PLC 연결 종료
     * - 하위 클래스의 @PreDestroy 메서드에서 호출
     */
    public void disconnect() {
        if (client != null) {
            client.disconnect();
            logger.info("[{}] Disconnected from PLC", getClass().getSimpleName());
        }
    }
}
