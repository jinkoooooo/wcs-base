package operato.logis.wcs.event;

import operato.logis.wcs.entity.TbWcsShuttleOrder;

/**
 * WCS → HOST 콜백 통보 이벤트 (트랜잭션 후처리용 도메인 이벤트).
 *
 * EcsCallbackProcessor 의 각 handleXxx() 가 DB 상태 변경 직후 발행하고,
 * @TransactionalEventListener(AFTER_COMMIT) 과 결합되어 커밋 완료 후에만 처리된다.
 *
 * 불변 객체. wcsOrderKey 만 필수이며 TbWcsShuttleOrder 는 메모리 참조로 전달해
 * 추가 DB 조회를 피한다.
 */
public class WcsHostCallbackEvent {

    /** 콜백 이벤트 종류. */
    public enum Type {
        STARTED,
        COMPLETED,
        RACK_CONVEYOR_ARRIVED,
        FAILED,
        CANCELLED
    }

    private final Type eventType;

    // 셔틀 오더 키 — TbWcsHostOrder.wcsOrderKey = TbWcsShuttleOrder.orderKey 로 HOST 주문 조회
    private final String wcsOrderKey;

    // 상태 변경 이벤트(STARTED, CANCELLED) 에서 새 HOST 주문 상태 코드
    private final Integer newHostStatus;

    // COMPLETED/FAILED/CANCELLED 에서 셔틀 오더 참조. 이벤트 타입에 따라 null 가능
    private final TbWcsShuttleOrder shuttleOrder;

    // FAILED 이벤트 전용 에러 코드/설명
    private final String errorCode;
    private final String errorDesc;

    private WcsHostCallbackEvent(Type eventType,
                                  String wcsOrderKey,
                                  Integer newHostStatus,
                                  TbWcsShuttleOrder shuttleOrder,
                                  String errorCode,
                                  String errorDesc) {
        this.eventType     = eventType;
        this.wcsOrderKey   = wcsOrderKey;
        this.newHostStatus = newHostStatus;
        this.shuttleOrder  = shuttleOrder;
        this.errorCode     = errorCode;
        this.errorDesc     = errorDesc;
    }

    /** ECS → 실행 시작(STARTED) 이벤트 */
    public static WcsHostCallbackEvent ofStarted(String wcsOrderKey, int newHostStatus) {
        return new WcsHostCallbackEvent(Type.STARTED, wcsOrderKey, newHostStatus,
                null, null, null);
    }

    /** ECS → 완료(COMPLETED) 이벤트 */
    public static WcsHostCallbackEvent ofCompleted(String wcsOrderKey, TbWcsShuttleOrder shuttleOrder) {
        return new WcsHostCallbackEvent(Type.COMPLETED, wcsOrderKey, null,
                shuttleOrder, null, null);
    }

    /** ECS → 랙단 컨베이어 도착(RACK_CONVEYOR_ARRIVED) 이벤트 */
    public static WcsHostCallbackEvent ofRackConveyorArrived(String wcsOrderKey) {
        return new WcsHostCallbackEvent(Type.RACK_CONVEYOR_ARRIVED, wcsOrderKey,
                null, null, null, null);
    }

    /** ECS → 설비 에러(FAILED) 이벤트 */
    public static WcsHostCallbackEvent ofFailed(String wcsOrderKey,
                                                 String errorCode,
                                                 String errorDesc) {
        return new WcsHostCallbackEvent(Type.FAILED, wcsOrderKey, null,
                null, errorCode, errorDesc);
    }

    /** ECS → 취소(CANCELLED) 이벤트 */
    public static WcsHostCallbackEvent ofCancelled(String wcsOrderKey, int newHostStatus) {
        return new WcsHostCallbackEvent(Type.CANCELLED, wcsOrderKey, newHostStatus,
                null, null, null);
    }

    public Type            getEventType()     { return eventType;     }
    public String          getWcsOrderKey()   { return wcsOrderKey;   }
    public Integer         getNewHostStatus() { return newHostStatus; }
    public TbWcsShuttleOrder getShuttleOrder(){ return shuttleOrder;  }
    public String          getErrorCode()     { return errorCode;     }
    public String          getErrorDesc()     { return errorDesc;     }

    @Override
    public String toString() {
        return "WcsHostCallbackEvent{type=" + eventType +
                ", wcsOrderKey='" + wcsOrderKey + "'}";
    }
}
