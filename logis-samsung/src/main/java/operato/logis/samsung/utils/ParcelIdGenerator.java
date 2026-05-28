package operato.logis.samsung.utils;

import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** parcelId = .YYYYMMDD + lineId + triggerId + barcode + 6자리 seq */
@UtilityClass
public class ParcelIdGenerator {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE; // YYYYMMDD
    private static final ConcurrentHashMap<String, AtomicInteger> COUNTERS = new ConcurrentHashMap<>();
    private static volatile LocalDate cachedDate = LocalDate.now(KST);

    public static String next(String lineId, String plcSeqNo, String barcode) {
        Objects.requireNonNull(lineId, "lineId"); Objects.requireNonNull(plcSeqNo, "triggerId"); Objects.requireNonNull(barcode, "barcode");
        LocalDate today = LocalDate.now(KST);
        if (!today.equals(cachedDate)) {
            synchronized (ParcelIdGenerator.class) {
                if (!today.equals(cachedDate)) { COUNTERS.clear(); cachedDate = today; }
            }
        }
        String key = today.format(YMD) + "|" + lineId + "|" + plcSeqNo + "|" + barcode;
        int seq = COUNTERS.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
        return today.format(YMD) + lineId + plcSeqNo + barcode + String.format("%06d", seq);
    }
}
