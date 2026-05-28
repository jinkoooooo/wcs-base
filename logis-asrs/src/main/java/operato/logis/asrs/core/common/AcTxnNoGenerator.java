package operato.logis.asrs.core.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * AisleCore 업무번호 생성기.
 *
 * <p>
 * 재고 트랜잭션 번호, 할당 번호, 전략 실행 번호 등
 * 업무성 식별자 생성을 담당한다.
 * </p>
 *
 * <p>
 * 1차 구현은 단순/안전성을 우선하여
 * "구분코드 + 시각 + 짧은 UUID" 조합으로 생성한다.
 * </p>
 */
@Component
public class AcTxnNoGenerator {

    /** 밀리초 단위까지 포함한 시각 포맷 */
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /**
     * 재고 트랜잭션 번호 생성.
     *
     * <p>
     * 예: MOVE-20260320143030123-1A2B3C4D
     * </p>
     *
     * @param txnType 트랜잭션 유형
     * @return 생성된 트랜잭션 번호
     */
    public String generateTxnNo(String txnType) {
        // txnType 미지정 시 TXN 공통 prefix 사용
        String prefix = txnType == null || txnType.isBlank() ? "TXN" : txnType.trim().toUpperCase();
        return prefix + "-" + nowString() + "-" + shortUuid();
    }

    /**
     * 할당번호 생성.
     */
    public String generateAllocationNo() {
        return "ALLOC-" + nowString() + "-" + shortUuid();
    }

    /**
     * 전략 실행번호 생성.
     */
    public String generateStrategyRunNo() {
        return "STRUN-" + nowString() + "-" + shortUuid();
    }

    /**
     * 현재 시각 문자열 생성.
     */
    private String nowString() {
        return LocalDateTime.now().format(TS_FORMAT);
    }

    /**
     * UUID 앞 8자리 축약값 생성.
     *
     * <p>
     * 1차에서는 간단한 구분값 용도로 충분하다.
     * </p>
     */
    private String shortUuid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}