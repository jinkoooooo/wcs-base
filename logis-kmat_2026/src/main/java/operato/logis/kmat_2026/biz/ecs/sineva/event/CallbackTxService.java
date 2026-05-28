package operato.logis.kmat_2026.biz.ecs.sineva.event;

import operato.logis.kmat_2026.biz.ecs.sineva.consts.CbkStatus;
import operato.logis.kmat_2026.entity.TbEcsTaskProcess;
import operato.logis.kmat_2026.entity.TbWcsOrder;
import operato.logis.kmat_2026.service.impl.TbWcsOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

@Service
public class CallbackTxService extends AbstractQueryService {

    private static final Logger logger =
            LoggerFactory.getLogger(CallbackTxService.class);

    private final TbWcsOrderService tbWcsOrderService;
    private final ApplicationEventPublisher eventPublisher;

    public CallbackTxService(TbWcsOrderService tbWcsOrderService,
                             ApplicationEventPublisher eventPublisher) {
        this.tbWcsOrderService = tbWcsOrderService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * ECS 콜백 반영 트랜잭션 서비스
     *
     * 보장 사항
     * 1. order update는 반드시 트랜잭션 내에서 반영된다.
     * 2. 트랜잭션 커밋이 성공한 경우에만 EquipEvent를 발행한다.
     * 3. 같은 orderId는 외부 KeyedSerialExecutor에서 순차 처리된다.
     */
    @Transactional
    public void apply(Domain domain, TbEcsTaskProcess callbackTask) {
        Domain.setCurrentDomain(domain);

        String orderId = callbackTask.getOrderId();
        String status = callbackTask.getCbkStatus();

        TbWcsOrder order = tbWcsOrderService.findOrderByOrderId(orderId);
        if (ValueUtil.isEmpty(order)) {
            throw new ElidomRuntimeException("Order not found: " + orderId);
        }

        order.setEquipId(callbackTask.getEquipId());
        order.setCbkStatus(status);

        boolean errorStatus =
                CbkStatus.ERROR.getCode().equals(status)
                        || CbkStatus.ERROR_RECOVERY.getCode().equals(status);

        if (!errorStatus) {
            order.setCurrentPositionCod(callbackTask.getCurrentPositionCod());
        }

        this.queryManager.update(
                order,
                "equipId",
                "cbkStatus",
                "currentPositionCod"
        );

        final String equipType = order.getEquipType();
        final String errorCode = CbkStatus.ERROR.getCode().equals(status)
                ? callbackTask.getErrorCode()
                : null;

        logger.info("[CBK][TX] order update 완료 orderId={}, status={}, equipId={}",
                orderId, status, order.getEquipId());

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        logger.info("[CBK][AFTER_COMMIT] EquipEvent 발행 orderId={}, status={}",
                                orderId, status);

                        if (ValueUtil.isNotEmpty(errorCode)) {
                            eventPublisher.publishEvent(
                                    new EquipEvent(equipType, order, errorCode)
                            );
                        } else {
                            eventPublisher.publishEvent(
                                    new EquipEvent(equipType, order)
                            );
                        }
                    }
                }
        );
    }
}