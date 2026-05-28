package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Shuttle Order Key 생성 유틸
 *
 * [포맷]
 * {PREFIX}-{yyyyMMddHHmmss}-{2자리 랜덤}
 * 예) INB-20260309151623-a3
 *
 * [사용처]
 * - InboundOrderHandler.createShuttleOrder()
 * - OutboundOrderHandler.createShuttleOrder()
 * - MoveOrderHandler.createShuttleOrder()
 */
public final class OrderKeyGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private OrderKeyGenerator() {}

    public static String inbound() {
        return generate("INB");
    }

    public static String outbound() {
        return generate("OUT");
    }

    public static String move() {
        return generate("MOV");
    }

    private static String generate(String prefix) {
        String datetime = LocalDateTime.now().format(FORMATTER);
        String suffix = UUID.randomUUID().toString().replace("_", "").substring(0, 2);
        return prefix + "_" + datetime + "_" + suffix;
    }
}