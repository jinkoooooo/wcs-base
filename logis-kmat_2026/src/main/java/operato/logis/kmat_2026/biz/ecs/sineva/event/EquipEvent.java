package operato.logis.kmat_2026.biz.ecs.sineva.event;


import operato.logis.kmat_2026.entity.TbWcsOrder;
import xyz.elidom.sys.event.SysEvent;
import xyz.elidom.util.ValueUtil;

import java.util.Map;

/**
 * ============================================================================
 * Equip Event
 * ============================================================================
 *
 * [역할]
 * - callback 적용 후 실제 장비/오더 후속 처리 이벤트
 *
 * [중요]
 * - 기존 event 형식 유지
 */
public class EquipEvent extends SysEvent {

    private TbWcsOrder order;
    private String equipType;
    private String commandType;
    private Integer callbackTaskStatus;
    private String errorCode;
    private Map<String, Object> returnValue;

    public EquipEvent(String equipType, TbWcsOrder order) {
        this.equipType = equipType;
        this.order = order;
    }

    public EquipEvent(String equipType, TbWcsOrder order, String errorCode) {
        this.equipType = equipType;
        this.order = order;
        this.errorCode = errorCode;
    }

    public TbWcsOrder getOrder() {
        return order;
    }

    public void setOrder(TbWcsOrder order) {
        this.order = order;
    }

    public String getEquipType() {
        return equipType;
    }

    public void setEquipType(String equipType) {
        this.equipType = equipType;
    }

    public String getCommandType() {
        return commandType;
    }

    public void setCommandType(String commandType) {
        this.commandType = commandType;
    }

    public Integer getCallbackTaskStatus() {
        return callbackTaskStatus;
    }

    public void setCallbackTaskStatus(Integer callbackTaskStatus) {
        this.callbackTaskStatus = callbackTaskStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setReturnValue(String code, String message, String taskId) {
        this.returnValue = ValueUtil.newMap("code,message,reqCode", code, message, taskId);
    }

    public Map<String, Object> getReturnValue() {
        return returnValue;
    }
}