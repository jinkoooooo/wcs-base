package operato.logis.kmat_2026.biz.ecs.sineva.service;

import operato.logis.kmat_2026.biz.ecs.sineva.consts.ProcessStatus;
import operato.logis.kmat_2026.entity.TbWcsOrder;
import operato.logis.kmat_2026.service.impl.TbWcsOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

@Service
public class OrderCancelService {

    private static final Logger logger = LoggerFactory.getLogger(OrderCancelService.class);

    @Autowired
    protected TbWcsOrderService tbWcsOrderService;

    @Autowired
    protected EcsCommandService ecsCommandService;

    @Autowired
    protected LocationStateService locationStateService;

    @Autowired
    protected LocationLockService locationLockService;

    @Transactional
    public TbWcsOrder cancel(String orderId) {
        TbWcsOrder order = tbWcsOrderService.findOrderByOrderId(orderId);

        if (ValueUtil.isEmpty(order)) {
            throw new ElidomRuntimeException("취소 대상 오더가 없습니다. orderId=" + orderId);
        }

        if (ProcessStatus.COMPLETE.getCode().equals(order.getProcessStatus())
                || ProcessStatus.CANCEL.getCode().equals(order.getProcessStatus())) {
            throw new ElidomRuntimeException("취소 불가 상태입니다. orderId=" + orderId + ", processStatus=" + order.getProcessStatus());
        }

        ecsCommandService.cancelTask(order);

        // 기존 위치 복구 정책 유지
        locationStateService.updateLocationCancelTaskMng(order, order.getProcessStatus());

        // 추가: lock_order_id = orderId 인 내 락만 해제
        int released = locationLockService.releaseOrderOwnedLocks(
                order.getOrderId(),
                order.getFromPositionCod(),
                order.getToPositionCod()
        );

        tbWcsOrderService.cancelOrder(order);
        order.setRemark("[CANCEL_OK][IGNORE_4000] releasedLocks=" + released);

        logger.info("[OrderCancelService] cancel success - orderId={}, releasedLocks={}", orderId, released);
        return order;
    }
}