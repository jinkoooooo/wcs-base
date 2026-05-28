package operato.logis.kmat_2026.biz.ecs.sineva.service;

import operato.logis.kmat_2026.biz.ecs.sineva.consts.CbkStatus;
import operato.logis.kmat_2026.biz.ecs.sineva.consts.CommandType;
import operato.logis.kmat_2026.biz.ecs.sineva.consts.DataTransmitStatus;
import operato.logis.kmat_2026.biz.ecs.sineva.consts.EquipTaskType;
import operato.logis.kmat_2026.biz.ecs.sineva.consts.ProcessStatus;
import operato.logis.kmat_2026.entity.TbWcsOrder;
import operato.logis.kmat_2026.service.impl.TbEcsLocMstService;
import operato.logis.kmat_2026.service.impl.TbWcsOrderService;
import operato.logis.kmat_2026.util.InventoryConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Date;
import java.util.Map;

/**
 * ============================================================================
 * Order Command Service
 * ============================================================================
 *
 * [역할]
 * - WCS Order 신규 생성/초기 지시
 * - 기존 WCS Order release 처리
 * - complete / error callback 처리
 *
 * [핵심 변경]
 * - 신규 생성과 release를 명확히 분리
 * - orderId가 "이미 있다"는 이유만으로 release로 간주하지 않음
 * - 외부에서 미리 생성한 orderId를 가진 신규 오더도 정상 생성 가능
 */
@Service
public class OrderCommandService extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(OrderCommandService.class);

    protected final TbWcsOrderService tbWcsOrderService;
    protected final EcsCommandService ecsCommandService;
    protected final TbEcsLocMstService tbEcsLocMstService;
    protected final LocationStateService locationStateService;

    public OrderCommandService(TbWcsOrderService tbWcsOrderService,
                               EcsCommandService ecsCommandService,
                               TbEcsLocMstService tbEcsLocMstService,
                               LocationStateService locationStateService) {
        this.tbWcsOrderService = tbWcsOrderService;
        this.ecsCommandService = ecsCommandService;
        this.tbEcsLocMstService = tbEcsLocMstService;
        this.locationStateService = locationStateService;
    }

    /**
     * 신규 WCS Order 생성 + ECS 초기 지시 전송
     *
     * [중요]
     * - orderId가 이미 세팅되어 있어도 "신규 생성"으로 처리한다.
     * - 단, 동일 orderId가 DB에 이미 존재하면 중복 생성으로 간주하고 예외 처리한다.
     */
    @Transactional
    public TbWcsOrder createAndSendInitialWcsOrder(TbWcsOrder order) {
        if (ValueUtil.isEmpty(order)) {
            throw new ElidomRuntimeException("생성할 order가 없습니다.");
        }

        logger.info("[createAndSendInitialWcsOrder] 시작 - fromSide={}, toSide={}, orderId={}, taskId={}",
                order.getFromSide(), order.getToSide(), order.getOrderId(), order.getTaskId());

        validateNewOrder(order);

        // 1) 외부에서 orderId를 미리 만들지 않았다면 여기서 생성
        if (ValueUtil.isEmpty(order.getOrderId())) {
            order.setOrderId(tbWcsOrderService.createOrderId());
        } else {
            // 이미 존재하는 orderId면 신규 생성 불가
            TbWcsOrder duplicated = tbWcsOrderService.findOrderByOrderId(order.getOrderId());
            if (ValueUtil.isNotEmpty(duplicated)) {
                throw new ElidomRuntimeException("이미 존재하는 orderId 입니다. orderId=" + order.getOrderId());
            }
        }

        // 2) taskId 기본값 보정
        if (ValueUtil.isEmpty(order.getTaskId())) {
            order.setTaskId(order.getOrderId());
        }

        // 3) 공통 필드 세팅
        initNewOrderFields(order);

        // 4) commandType에 따른 taskType 자동 보정
        applyTaskTypeByCommandType(order);

        // 5) 저장
        this.queryManager.insert(order);

        // 6) ECS 초기 지시 전송
        Map<String, Object> response = ecsCommandService.sendInitialTaskCommand(order);
        if (ecsCommandService.isFail(response)) {
            throw new ElidomRuntimeException("작업 지시 실패");
        }

        logger.info("[createAndSendInitialWcsOrder] 완료 - orderId={}, taskId={}, taskType={}",
                order.getOrderId(), order.getTaskId(), order.getTaskType());

        return order;
    }

    /**
     * 기존 WCS Order release 처리 진입점
     */
    @Transactional
    public TbWcsOrder releaseExistingWcsOrder(TbWcsOrder order) {
        if (ValueUtil.isEmpty(order)) {
            return null;
        }

        String taskId = ValueUtil.isNotEmpty(order.getTaskId())
                ? order.getTaskId()
                : order.getOrderId();

        return releaseWcsOrderByTaskId(taskId);
    }

    /**
     * 기존 메서드 호환용
     *
     * [주의]
     * - 신규 생성/기존 release 혼합 사용은 지양
     * - 신규 생성은 createAndSendInitialWcsOrder()
     * - release는 releaseExistingWcsOrder()/releaseWcsOrderByTaskId() 사용 권장
     */
    @Transactional
    public TbWcsOrder createOrReleaseWcsOrder(TbWcsOrder order) {
        if (ValueUtil.isEmpty(order)) {
            return null;
        }

        logger.warn("[createOrReleaseWcsOrder] deprecated method 호출 - orderId={}, taskId={}",
                order.getOrderId(), order.getTaskId());

        if (ValueUtil.isNotEmpty(order.getOrderId())) {
            TbWcsOrder dbOrder = tbWcsOrderService.findOrderByOrderId(order.getOrderId());
            if (ValueUtil.isNotEmpty(dbOrder)) {
                return releaseExistingWcsOrder(order);
            }
        }

        return createAndSendInitialWcsOrder(order);
    }

    @Transactional
    public TbWcsOrder releaseWcsOrderByTaskId(String taskId) {
        if (ValueUtil.isEmpty(taskId)) {
            return null;
        }

        TbWcsOrder dbOrder = tbWcsOrderService.findOrder(taskId);
        if (ValueUtil.isEmpty(dbOrder)) {
            return null;
        }

        Integer ps = dbOrder.getProcessStatus();
        String ct = dbOrder.getCommandType();

        if (ProcessStatus.AWAITING_FINAL_RELEASE.getCode().equals(ps)) {
            logger.warn("[releaseWcsOrderByTaskId] unsupported AWAITING_FINAL_RELEASE order. orderId={}, commandType={}",
                    dbOrder.getOrderId(), ct);
            return dbOrder;
        }

        releaseWcsOrder(dbOrder.getOrderId(), null);
        return dbOrder;
    }

    @Transactional
    public void releaseWcsOrder(String orderId, String taskId) {
        logger.info("[releaseWcsOrder] 시작 - orderId={}, taskId={}", orderId, taskId);

        TbWcsOrder order = resolveOrder(orderId, taskId);

        ecsCommandService.sendReleaseCommand(order);

        if (ProcessStatus.NEXT_TASK_READY.getCode().equals(order.getProcessStatus())) {
            order.setProcessStatus(ProcessStatus.FINISH_FROM_SIDE_LOADING.getCode());
            order.setCbkStatus(CbkStatus.FINISH_LOADING.getCode());
            order.setRemark("releaseCode 전송됨 - 다음 작업 시작됨");
            this.queryManager.update(order, "processStatus", "cbkStatus", "remark");
        }

        // 현재 위치 equip Id null 처리 (AMR 다음 task 이동 or 대기 포인트 복귀)
        tbEcsLocMstService.clearEquipIdByLocationCd(order.getCurrentPositionCod());

        logger.info("[releaseWcsOrder] 완료 - orderId={}, taskId={}", order.getOrderId(), order.getTaskId());
    }

    @Transactional
    public void completeTask(TbWcsOrder order) {
        logger.info("[completeTask] 호출 - orderId={}, taskType={}, cbkStatus={}",
                order != null ? order.getOrderId() : null,
                order != null ? order.getTaskType() : null,
                order != null ? order.getCbkStatus() : null);

        if (ValueUtil.isEmpty(order)) {
            return;
        }

        if (CbkStatus.END.getCode().equals(order.getCbkStatus())
                && EquipTaskType.MULTI_FREIGHT_MOVE.getCode().equals(order.getTaskType())) {

            order.setProcessStatus(ProcessStatus.AWAITING_FINAL_RELEASE.getCode());
            this.queryManager.update(order, "processStatus");
            return;
        }

        completeTaskOnly(order);
    }

    @Transactional
    public void completeTaskOnly(TbWcsOrder order) {
        if (ValueUtil.isEmpty(order)) {
            return;
        }

        tbWcsOrderService.completeOrder(order);
        releaseWcsOrder(order.getOrderId(), null);
    }

    @Transactional
    public Boolean unskipTask(TbWcsOrder order) {
        if (ValueUtil.isEmpty(order)
                || ValueUtil.isEmpty(order.getErrorCode())
                || !"skip".equals(order.getErrorCode())) {
            return false;
        }

        order.setErrorCode(null);
        order.setErrorMessage(null);
        order.setErrorDatetime(null);

        this.queryManager.update(order, "errorCode", "errorMessage", "errorDatetime");
        return true;
    }

    @Transactional
    public void handleErrorCallback(TbWcsOrder order, String errorCode) {
        if (ValueUtil.isEmpty(order) || ValueUtil.isEmpty(errorCode)) {
            return;
        }

        tbWcsOrderService.markOrderAsError(order, errorCode);

        if (!"4000".equals(errorCode)) {
            return;
        }

        TbWcsOrder dbOrder = tbWcsOrderService.findOrderByOrderId(order.getOrderId());
        if (ValueUtil.isEmpty(dbOrder)) {
            return;
        }

        String remark = ValueUtil.isEmpty(dbOrder.getRemark()) ? "" : dbOrder.getRemark();

        boolean cancelFlowMarked =
                remark.contains("IGNORE_4000") &&
                        (remark.contains("[CANCEL_")
                                || remark.contains("CANCEL_FLOW")
                                || remark.contains("CANCEL_OK")
                                || remark.contains("CANCEL_IN_PROGRESS"));

        boolean ignore4000 =
                ProcessStatus.CANCEL.getCode().equals(dbOrder.getProcessStatus()) || cancelFlowMarked;

        if (ignore4000) {
            logger.info("[handleErrorCallback][4000] IGNORE cancel-induced 4000. orderId={}", dbOrder.getOrderId());
            return;
        }

        locationStateService.updateLocationCancelTaskMng(dbOrder, dbOrder.getProcessStatus());
        dbOrder.setProcessStatus(ProcessStatus.CANCEL.getCode());
        dbOrder.setRemark("[AUTO_CANCEL][4000] reset by callback");
        this.queryManager.update(dbOrder, "processStatus", "remark");
    }

    @Transactional
    public void handleErrorRecoveryCallback(TbWcsOrder order) {
        tbWcsOrderService.clearOrderError(order);
    }

    private void validateNewOrder(TbWcsOrder order) {
        if (ValueUtil.isEmpty(order.getFromSide())) {
            throw new ElidomRuntimeException("fromSide가 비어 있습니다.");
        }

        if (ValueUtil.isEmpty(order.getToSide())) {
            throw new ElidomRuntimeException("toSide가 비어 있습니다.");
        }

        if (ValueUtil.isEmpty(order.getCommandType())) {
            throw new ElidomRuntimeException("commandType이 비어 있습니다.");
        }

        if (ValueUtil.isEmpty(order.getEquipType())) {
            throw new ElidomRuntimeException("equipType이 비어 있습니다.");
        }
    }

    private void initNewOrderFields(TbWcsOrder order) {
        order.setCurrentPositionCod(order.getFromSide());
        order.setProcessStatus(ProcessStatus.READY.getCode());
        order.setCurrentStep(ValueUtil.isEmpty(order.getCurrentStep()) ? 1 : order.getCurrentStep());
        order.setPriority(ValueUtil.isEmpty(order.getPriority()) ? 1 : order.getPriority());
        order.setFromPositionCod(order.getFromSide());
        order.setToPositionCod(tbWcsOrderService.getFirstToSide(order));

        order.setAcceptDatetime(new Date());
        order.setLcId(InventoryConstants.LC_ID);
        order.setCompleted(false);
        order.setDataTransmitStatus(DataTransmitStatus.CREATED.getCode());
    }

    private void applyTaskTypeByCommandType(TbWcsOrder order) {
        if(ValueUtil.isNotEmpty(order.getTaskType())){
            return;
        }

        CommandType commandType = CommandType.fromCode(order.getCommandType());

        order.setTaskType(commandType.resolveTaskTypeCode());
    }

    private TbWcsOrder resolveOrder(String orderId, String taskId) {
        if (ValueUtil.isEmpty(taskId) && ValueUtil.isEmpty(orderId)) {
            throw new ElidomRuntimeException("taskId 또는 orderId가 누락되었습니다.");
        }

        TbWcsOrder order = ValueUtil.isNotEmpty(taskId)
                ? tbWcsOrderService.findOrder(taskId)
                : tbWcsOrderService.findOrderByOrderId(orderId);

        if (ValueUtil.isEmpty(order)) {
            throw new ElidomRuntimeException("해당 오더가 존재하지 않습니다. taskId=" + taskId + ", orderId=" + orderId);
        }

        if (ProcessStatus.ERROR.getCode().equals(order.getProcessStatus())) {
            throw new ElidomRuntimeException("error task 입니다.");
        }

        return order;
    }
}