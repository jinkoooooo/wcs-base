package operato.logis.kmat_2026.biz.ecs.sineva.processor;

import operato.logis.kmat_2026.biz.ecs.sineva.consts.EquipType;
import operato.logis.kmat_2026.biz.ecs.sineva.consts.ProcessStatus;
import operato.logis.kmat_2026.biz.ecs.sineva.service.EcsCommandService;
import operato.logis.kmat_2026.biz.ecs.sineva.service.LocationLockService;
import operato.logis.kmat_2026.biz.ecs.sineva.service.LocationStateService;
import operato.logis.kmat_2026.biz.ecs.sineva.service.OrderCommandService;
import operato.logis.kmat_2026.entity.TbWcsOrder;
import operato.logis.kmat_2026.service.impl.TbWcsOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * Order Cancel Processor
 * ============================================================================
 *
 * [역할]
 * - 취소 요청 처리 전담
 * - 기존 daehwa CancelProcessor 정책을 kmat_2026 구조에 맞게 반영
 *
 * [정책]
 * 1. COMPLETE / CANCEL / ERROR 상태는 취소 불가
 * 2. 취소 시작 시 remark에 IGNORE_4000 마킹
 * 3. AGF는 "취소 전송 + WCS CANCEL + 내 락 해제" 중심으로 최소 처리
 * 4. AMR이며 POD/PALLET 로딩 상태면 필요 시 빈 버퍼 이동 오더를 선행 전송
 * 5. 취소 성공 후 orderId가 소유한 락만 해제
 * 6. AMR은 취소 후 running type 전환 가능
 */
@Component
public class OrderCancelProcessor extends AbstractQueryService implements OrderProcessor<TbWcsOrder> {

    private static final Logger logger = LoggerFactory.getLogger(OrderCancelProcessor.class);

    @Autowired
    protected TbWcsOrderService tbWcsOrderService;

    @Autowired
    protected EcsCommandService ecsCommandService;

    @Autowired
    protected LocationStateService locationStateService;

    @Autowired
    protected LocationLockService locationLockService;

    @Autowired
    protected OrderCommandService orderCommandService;

    /**
     * 취소 실행
     *
     * @param cancelOrderId 취소 대상 orderId
     * @return 취소 완료 order
     */
    @Override
    @Transactional
    public TbWcsOrder execute(String cancelOrderId) {
        logger.info("[WCS → ECS][CANCEL] start - orderId={}", cancelOrderId);

        // 0) 오더 조회
        TbWcsOrder cancelOrder = tbWcsOrderService.findOrderByOrderId(cancelOrderId);
        if (ValueUtil.isEmpty(cancelOrder)) {
            throw new ElidomRuntimeException("취소 대상 오더가 없습니다. orderId=" + cancelOrderId);
        }

        logger.info("[WCS → ECS][CANCEL] order found - orderId={}, taskId={}, processStatus={}, equipType={}, equipId={}, from={}, to={}",
                cancelOrder.getOrderId(),
                cancelOrder.getTaskId(),
                cancelOrder.getProcessStatus(),
                cancelOrder.getEquipType(),
                cancelOrder.getEquipId(),
                cancelOrder.getFromPositionCod(),
                cancelOrder.getToPositionCod());

        // 1) 취소 가능 여부 검증
        validateCancellable(cancelOrder);

        // 2) 취소 전 상태 저장
        Integer beforeProcessStatus = cancelOrder.getProcessStatus();

        // 3) IGNORE_4000 마킹
        markCancelInProgressIgnore4000(cancelOrder);

        // 4) 장비 타입 및 로딩 상태 기준 분기
        boolean isAgf = isEquipType(cancelOrder, EquipType.AGF);
        boolean nextOrderSent = false;

        if (!isAgf) {
            if (ValueUtil.isNotEmpty(cancelOrder.getEquipId())
                    && isPodOrPalletLoading(beforeProcessStatus)) {
//                nextOrderSent = trySendNextMoveToEmptyBuffer(cancelOrder);
            }
        }

        // 5) ECS 취소 전송 + WCS 취소 상태 반영
        sendCancelTaskAndMarkWcsCancel(cancelOrder, nextOrderSent);

        // 6) 취소 후 내 락만 해제
        int released = releaseOwnedLocks(cancelOrder);

        // 7) AGF는 최소 처리 후 종료
        if (isAgf) {
            appendCancelResultRemark(cancelOrder, nextOrderSent, released);

            logger.info("[WCS → ECS][CANCEL][AGF] done - orderId={}, releasedLocks={}, remark={}",
                    cancelOrder.getOrderId(), released, cancelOrder.getRemark());
            return cancelOrder;
        }

        // 8) 위치 상태 복구
        locationStateService.updateLocationCancelTaskMng(cancelOrder, beforeProcessStatus);

        // 9) AMR running type 전환
        if (ValueUtil.isNotEmpty(cancelOrder.getEquipId()) && isEquipType(cancelOrder, EquipType.AMR)) {
            ecsCommandService.setRobotRunningType(Arrays.asList(cancelOrder.getEquipId()));
            logger.info("[WCS → ECS][CANCEL] setRobotRunningType done - orderId={}, equipId={}",
                    cancelOrder.getOrderId(), cancelOrder.getEquipId());
        } else {
            logger.info("[WCS → ECS][CANCEL] setRobotRunningType skipped - orderId={}, equipType={}, equipId={}",
                    cancelOrder.getOrderId(), cancelOrder.getEquipType(), cancelOrder.getEquipId());
        }

        appendCancelResultRemark(cancelOrder, nextOrderSent, released);

        logger.info("[WCS → ECS][CANCEL] done - orderId={}, taskId={}, processStatus={}, releasedLocks={}, remark={}",
                cancelOrder.getOrderId(),
                cancelOrder.getTaskId(),
                cancelOrder.getProcessStatus(),
                released,
                cancelOrder.getRemark());

        return cancelOrder;
    }

    /**
     * 취소 processor는 별도 callback 후속 흐름 없음
     */
    @Override
    public TbWcsOrder callback(TbWcsOrder order) {
        logger.info("[OrderCancelProcessor][callback] orderId={}, processStatus={}",
                order.getOrderId(), order.getProcessStatus());
        return order;
    }

    @Override
    public String getProcessorType() {
        return getClass().getSimpleName();
    }

    // =====================================================================
    // private helpers
    // =====================================================================

    /**
     * 취소 불가능 상태 방어
     */
    private void validateCancellable(TbWcsOrder order) {
        if (order == null) {
            throw new ElidomRuntimeException("취소할 오더가 없습니다.");
        }

        List<Integer> nonCancellableStatuses = Arrays.asList(
                ProcessStatus.COMPLETE.getCode(),
                ProcessStatus.CANCEL.getCode(),
                ProcessStatus.ERROR.getCode()
        );

        if (nonCancellableStatuses.contains(order.getProcessStatus())) {
            throw new ElidomRuntimeException(
                    "완료되었거나 이미 취소된 작업, 또는 복구 불가능한 에러 상태의 작업은 취소할 수 없습니다. orderId="
                            + order.getOrderId() + ", processStatus=" + order.getProcessStatus()
            );
        }
    }

    /**
     * CANCEL 진입 시 IGNORE_4000 마킹
     */
    private void markCancelInProgressIgnore4000(TbWcsOrder cancelOrder) {
        String prevRemark = ValueUtil.isEmpty(cancelOrder.getRemark()) ? "" : cancelOrder.getRemark();

        if (!prevRemark.contains("IGNORE_4000")) {
            cancelOrder.setRemark("[CANCEL_IN_PROGRESS][IGNORE_4000] " + prevRemark);
            this.queryManager.update(cancelOrder, "remark");
        }
    }

    /**
     * 로딩 상태라면 빈 버퍼 이동 오더를 선행 전송
     */
    private boolean trySendNextMoveToEmptyBuffer(TbWcsOrder cancelOrder) {
//        TbEcsLocMst emptyBufferPoint = bufferLocationSelector.selectAmrBufferEmpty();
//
//        if (ValueUtil.isEmpty(emptyBufferPoint)) {
//            logger.warn("[WCS → ECS][CANCEL] empty buffer not found - cancelOrderId={}, equipId={}",
//                    cancelOrder.getOrderId(), cancelOrder.getEquipId());
//            return false;
//        }
//
//        TbWcsOrder nextOrder = tbWcsOrderService.createOrder(
//                null,
//                null,
//                emptyBufferPoint.getLocationCd(),
//                cancelOrder.getPodCd(),
//                CommandType.CANCEL_EMPTY_POINT_AMR_MOVE,
//                EquipType.AMR,
//                cancelOrder.getEquipId(),
//                1
//        );
//
//        logger.info("[WCS → ECS][CANCEL] next empty-buffer move sent - cancelOrderId={}, nextTo={}, equipId={}",
//                cancelOrder.getOrderId(), emptyBufferPoint.getLocationCd(), cancelOrder.getEquipId());
//
//        orderCommandService.createOrReleaseWcsOrder(nextOrder);
        return true;
    }

    /**
     * ECS 취소 전송 후 WCS 취소 확정
     */
    @SuppressWarnings("unchecked")
    private void sendCancelTaskAndMarkWcsCancel(TbWcsOrder cancelOrder, boolean nextOrderSent) {
        Map<String, Object> returnValue = ecsCommandService.cancelTask(cancelOrder);

        logger.info("[WCS → ECS][CANCEL] ECS cancelTask response - orderId={}, taskId={}, response={}",
                cancelOrder.getOrderId(), cancelOrder.getTaskId(), returnValue);

        if (ecsCommandService.isFail(returnValue)) {
            String message = ValueUtil.isEmpty(returnValue)
                    ? "ECS cancelTask 응답이 비어 있습니다."
                    : String.valueOf(returnValue.getOrDefault("message", "ECS cancelTask 실패"));
            throw new ElidomRuntimeException(message);
        }

        // 기존 프로젝트 서비스 정책 유지
        tbWcsOrderService.cancelOrder(cancelOrder);

        // cancelOrder(order) 이후 remark가 덮일 수 있으므로 다시 정리
        String remark = ValueUtil.isEmpty(cancelOrder.getRemark()) ? "" : cancelOrder.getRemark();
        String nextFlag = nextOrderSent ? "[NEXT_SENT]" : "";

        if (!remark.contains("[CANCEL_OK]")) {
            cancelOrder.setRemark("[CANCEL_OK][IGNORE_4000]" + nextFlag + " " + remark);
            this.queryManager.update(cancelOrder, "remark");
        }
    }

    /**
     * 취소 후 orderId가 소유한 락만 해제
     */
    private int releaseOwnedLocks(TbWcsOrder cancelOrder) {
        int released = locationLockService.releaseOrderOwnedLocks(
                cancelOrder.getOrderId(),
                cancelOrder.getFromPositionCod(),
                cancelOrder.getToPositionCod()
        );

        logger.info("[WCS → ECS][CANCEL] owned locks released - orderId={}, releasedLocks={}",
                cancelOrder.getOrderId(), released);

        return released;
    }

    /**
     * 최종 취소 결과 remark 보강
     */
    private void appendCancelResultRemark(TbWcsOrder cancelOrder, boolean nextOrderSent, int released) {
        String currentRemark = ValueUtil.isEmpty(cancelOrder.getRemark()) ? "" : cancelOrder.getRemark();

        String nextFlag = nextOrderSent ? "[NEXT_SENT]" : "";
        String resultRemark = "[CANCEL_OK][IGNORE_4000]" + nextFlag + "[releasedLocks=" + released + "] " + currentRemark;

        cancelOrder.setRemark(resultRemark);
        this.queryManager.update(cancelOrder, "remark");
    }

    /**
     * POD 또는 PALLET 로딩 상태 여부
     */
    private boolean isPodOrPalletLoading(Integer processStatus) {
        if (ValueUtil.isEmpty(processStatus)) {
            return false;
        }

        List<Integer> loadingStatuses = TbWcsOrderService.POD_OR_PALLET_LOADING_STATUSES;
        return loadingStatuses.contains(processStatus);
    }

    /**
     * EquipType 비교 유틸
     */
    private boolean isEquipType(TbWcsOrder order, EquipType equipType) {
        if (order == null || equipType == null) {
            return false;
        }
        if (ValueUtil.isEmpty(order.getEquipType())) {
            return false;
        }
        return order.getEquipType().equals(equipType.getCode());
    }
}