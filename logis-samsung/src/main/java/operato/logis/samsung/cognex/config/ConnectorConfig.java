package operato.logis.samsung.cognex.config;

import operato.logis.samsung.cognex.core.DataReceiver;
import operato.logis.samsung.cognex.core.SocketClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Cognex Connector에 필요한 SocketClient Bean 정의를 담당합니다.
 * BCR 2대와 Vision 1대, 총 3개의 독립적인 클라이언트를 설정합니다.
 */
@Configuration
public class ConnectorConfig {


    @Value("${cognex.bcr01.ip:192.168.100.181}")
    private String bcr01Ip;
    @Value("${cognex.bcr01.port:6001}")
    private int bcr01Port;
    @Value("${cognex.bcr02.ip:192.168.100.191}") // BCR-01과 IP가 다르다는 요구사항을 반영할 수 있도록 분리
    private String bcr02Ip;
    @Value("${cognex.bcr02.port:6001}")
    private int bcr02Port;
    @Value("${cognex.vision.ip:192.168.100.151}")
    private String visionIp;
    @Value("${cognex.vision.port:6001}")
    private int visionPort;


    /**
     * BCR-01 클라이언트 정의 (이름: BCR-01)
     */
    @Bean
    public SocketClient bcr01Client(DataReceiver dataReceiver) {
        return new SocketClient("BCR-01", bcr01Ip, bcr01Port, dataReceiver);
    }

    /**
     * BCR-02 클라이언트 정의 (이름: BCR-02, 별도 IP/Port)
     */
    @Bean
    public SocketClient bcr02Client(DataReceiver dataReceiver) {
        return new SocketClient("BCR-02", bcr02Ip, bcr02Port, dataReceiver);
    }

    /**
     * VISION-01 클라이언트 정의 (이름: VISION-01)
     */
    @Bean
    public SocketClient visionClient(DataReceiver dataReceiver) {
        return new SocketClient("VISION-01", visionIp, visionPort, dataReceiver);
    }

    // 모든 SocketClient 빈들을 리스트로 모아 HeartbeatScheduler에 주입하기 위한 Bean
    // 이 Bean을 사용하면 HeartbeatScheduler가 특정 클라이언트 개수에 묶이지 않습니다.
    @Bean
    public List<SocketClient> allSocketClients(SocketClient bcr01Client, SocketClient bcr02Client, SocketClient visionClient) {
        return List.of(bcr01Client, bcr02Client, visionClient);
    }
}