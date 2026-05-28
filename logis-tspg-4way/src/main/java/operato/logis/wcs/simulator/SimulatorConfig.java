package operato.logis.wcs.simulator;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 시뮬레이터 통합 설정 — MOVE 페이즈 없음, INBOUND ↔ OUTBOUND 순환만 지원.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class SimulatorConfig {

    // 마스터 스위치
    public static volatile boolean ENABLED    = true;
    public static volatile boolean AUTO_START = false;

    // 도메인 고정값
    public static final String OWNER_CODE       = "OWNER001";
    public static final String HOST_SYSTEM_CODE = "WMS-SIM";
    public static final long   DOMAIN_ID        = 7L;

    // 시뮬 데이터 식별 접두사 (운영 데이터와 분리)
    public static final String SIM_LOT_PREFIX      = "LOT-SIM-";
    public static final String SIM_BARCODE_PREFIX  = "SIM-BC-";
    public static final String SIM_HOST_KEY_PREFIX = "SIM-";

    // HOST 주문 생성 (전 그룹 공통, 런타임 토글)
    public static volatile long HOST_INTERVAL_MS = 3000L;
    public static volatile int  HOST_MAX_PENDING = 100;

    /** BCR 스캔 시뮬레이션 폴링 주기 (ms). 입고 산출 완료 후 BCR 흉내까지의 지연. */
    public static final long BCR_INTERVAL_MS = 500L;

    /** BCR tick 1회당 최대 스캔 건수 — ECS 큐 과적 방지. */
    public static final int BCR_MAX_PER_TICK = 10;

    /**
     * 페이즈 카운트 (글로벌 fallback).
     * 그룹별 tb_wcs_simulator_state.target_inbound/outbound 가 우선.
     * 0 이면 해당 페이즈 즉시 스킵.
     */
    public static volatile int  RATIO_INBOUND    = 50;
    public static volatile int  RATIO_OUTBOUND   = 50;

    public static volatile int  QTY_MIN          = 50;
    public static volatile int  QTY_MAX          = 50;

    // ECS+PLC 콜백
    public static volatile long PLC_POLL_MS       = 1000L;
    public static volatile long PLC_STEP_DELAY_MS = 500L;
    public static volatile int  PLC_BATCH_SIZE    = 10;

    public static volatile long PORT_MODE_CYCLE_MS = 5_000L;

    /**
     * 시드 SKU — 시뮬 초기화 시 자동 등록되는 품목 목록.
     */
    public record SeedSku(String code, String name, int boxQty, int palletQty, double weight, String lotNo) {}

    public static final List<SeedSku> SEED_SKUS = List.of(
            new SeedSku("SIM-SKU-APPLE-001",  "사과 1박스",   10, 100, 0.5, "LOT-APPLE-001"),
            new SeedSku("SIM-SKU-BANANA-002", "바나나 1박스", 10, 100, 0.3, "LOT-SIM-002"),
            new SeedSku("SIM-SKU-CHERRY-003", "체리 1박스",   10, 100, 0.2, "LOT-SIM-003"),
            new SeedSku("SIM-SKU-GRAPE-004",  "포도 1박스",   10, 100, 0.4, "LOT-SIM-004"),
            new SeedSku("SIM-SKU-ORANGE-005", "오렌지 1박스", 10, 100, 0.4, "LOT-SIM-005" ),
            new SeedSku("SIM-SKU-MELON-006",  "멜론 1박스",   10, 100, 0.6, "LOT-SIM-006")
    );

    /**
     * 시뮬레이터 전용 비동기 태스크 풀.
     */
    @Bean(name = "simulatorTaskExecutor")
    public TaskExecutor simulatorTaskExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(5);
        ex.setMaxPoolSize(20);
        ex.setQueueCapacity(100);
        ex.setThreadNamePrefix("sim-");
        ex.initialize();
        return ex;
    }
}
