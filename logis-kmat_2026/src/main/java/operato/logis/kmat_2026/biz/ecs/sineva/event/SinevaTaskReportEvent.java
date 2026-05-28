package operato.logis.kmat_2026.biz.ecs.sineva.event;

import operato.logis.kmat_2026.biz.ecs.sineva.consts.CbkStatus;
import operato.logis.kmat_2026.biz.ecs.sineva.consts.CommandType;
import operato.logis.kmat_2026.entity.TbWcsOrder;
import org.springframework.context.ApplicationEvent;

/**
 * AGF 콜백 이벤트
 *
 * ECS 모듈에서 AGF 콜백 수신 시 발행하여
 * KMAT 등 다른 모듈에서 수신할 수 있도록 함
 *
 * [사용 가능한 콜백 상태]
 * - IN_PROGRESS: AGF가 출발지에서 파렛트를 집어 올림
 * - SUCCESS: AGF 작업 완료
 * - END: AGF 작업 종료
 * - ERROR: AGF 작업 에러
 */
public class SinevaTaskReportEvent extends ApplicationEvent {

    private final TbWcsOrder order;
    private final CbkStatus cbkStatus;
    private final CommandType commandType;
    private final String errorCode;

    public SinevaTaskReportEvent(Object source, TbWcsOrder order, CbkStatus cbkStatus, String errorCode) {
        super(source);
        this.order = order;
        this.cbkStatus = cbkStatus;
        this.commandType = CommandType.fromCode(order.getCommandType());
        this.errorCode = errorCode;
    }

    public SinevaTaskReportEvent(Object source, TbWcsOrder order, CbkStatus cbkStatus) {
        this(source, order, cbkStatus, null);
    }

    public TbWcsOrder getOrder() {
        return order;
    }

    public CbkStatus getCbkStatus() {
        return cbkStatus;
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getOrderId() {
        return order != null ? order.getOrderId() : null;
    }

    public String getFromSide() {
        return order != null ? order.getFromSide() : null;
    }

    public String getToSide() {
        return order != null ? order.getToSide() : null;
    }

    public boolean isInProgress() {
        return cbkStatus == CbkStatus.IN_PROGRESS;
    }

    public boolean isSuccess() {
        return cbkStatus == CbkStatus.SUCCESS;
    }

    public boolean isEnd() {
        return cbkStatus == CbkStatus.END;
    }

    public boolean isError() {
        return cbkStatus == CbkStatus.ERROR;
    }

    @Override
    public String toString() {
        return String.format("AgfCallbackEvent{orderId=%s, cbkStatus=%s, commandType=%s, from=%s, to=%s}",
                getOrderId(), cbkStatus, commandType, getFromSide(), getToSide());
    }
}
