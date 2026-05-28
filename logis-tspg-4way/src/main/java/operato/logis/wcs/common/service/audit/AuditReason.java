package operato.logis.wcs.common.service.audit;

import java.util.function.Supplier;

/**
 * 현재 쓰기에 붙일 변경 사유(reason)를 스레드 로컬로 전달.
 * 감사 프록시가 기록 시점에 get() 으로 읽는다. leaf-level(단일 쓰기) 래핑 용도.
 */
public final class AuditReason {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private AuditReason() {}

    /** 사유 설정. */
    public static void set(String reason) {
        HOLDER.set(reason);
    }

    /** 현재 사유 반환. 없으면 null. */
    public static String get() {
        return HOLDER.get();
    }

    /** 사유 제거. */
    public static void clear() {
        HOLDER.remove();
    }

    /** 사유를 건 채 body 실행 후 항상 정리(반환 없음). */
    public static void run(String reason, Runnable body) {
        set(reason);
        try {
            body.run();
        } finally {
            clear();
        }
    }

    /** 사유를 건 채 body 실행 후 항상 정리(값 반환). */
    public static <T> T call(String reason, Supplier<T> body) {
        set(reason);
        try {
            return body.get();
        } finally {
            clear();
        }
    }
}
