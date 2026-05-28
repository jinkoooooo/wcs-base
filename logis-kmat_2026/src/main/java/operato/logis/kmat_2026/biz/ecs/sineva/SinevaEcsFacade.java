package operato.logis.kmat_2026.biz.ecs.sineva;

import operato.logis.kmat_2026.biz.ecs.sineva.processor.ProcessorDispatcher;
import operato.logis.kmat_2026.entity.TbWcsOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ============================================================================
 * Sineva ECS Facade
 * ============================================================================
 *
 * [역할]
 * - 외부에서 사용하는 진입점
 * - 기존 EcsSinevaService 스타일 유지
 * - processor dispatcher 호출용 facade
 */
@Service
public class SinevaEcsFacade {

    private static final Logger logger = LoggerFactory.getLogger(SinevaEcsFacade.class);

    @Autowired
    protected ProcessorDispatcher processorDispatcher;

    /**
     * AGF -> TSPG 입고 excute
     */
    @Transactional
    public TbWcsOrder handleTspgConveyorInboundExecute(String fromLocationCd) {
        logger.info("[SinevaEcsFacade] handleTspgConveyorInboundExecute({})", fromLocationCd);
        return processorDispatcher.execute("TspgConveyorInboundProcessor", fromLocationCd);
    }

    /**
     * AGF -> TSPG 입고 callback
     */
    public void handleTspgConveyorInboundCallback(TbWcsOrder order) throws InterruptedException {
        logger.info("SinevaEcsFacade Call [handleTspgConveyorInboundCallback]({})", order);
        processorDispatcher.callback("TspgConveyorInboundProcessor", order);
    }

    /**
     * TSPG -> AGF 출고 excute
     */
    @Transactional
    public TbWcsOrder handleTspgConveyorOutboundExecute(String fromLocationCd) {
        logger.info("[SinevaEcsFacade] handleTspgConveyorOutboundExecute({})", fromLocationCd);
        return processorDispatcher.execute("TspgConveyorOutboundProcessor", fromLocationCd);
    }

    /**
     * TSPG -> AGF 출고 callback
     */
    public void handleTspgConveyorOutboundCallback(TbWcsOrder order) throws InterruptedException {
        logger.info("SinevaEcsFacade Call [handleTspgConveyorOutboundCallback]({})", order);
        processorDispatcher.callback("TspgConveyorOutboundProcessor", order);
    }

    /**
     * TSPG 입고 Execute
     */

    public TbWcsOrder handleTspgConveyorInboundRefillExecute(String inboundLocationCd) {
        logger.info("[SinevaEcsFacade] handleTspgConveyorInboundRefillExecute({})", inboundLocationCd);
        return processorDispatcher.execute("TspgConveyorInboundRefillProcessor", inboundLocationCd);
    }

    /**
     * TSPG 입고 Callback
     */
    public void handleTspgConveyorInboundRefillCallback(TbWcsOrder order) throws InterruptedException {
        logger.info("SinevaEcsFacade Call [handleTspgConveyorInboundRefillCallback]({})", order);
        processorDispatcher.callback("TspgConveyorInboundRefillProcessor", order);
    }

    /**
     * order 삭제
     */
    @Transactional
    public TbWcsOrder handleCancelTaskExecute(String orderId) {
        logger.info("[SinevaEcsFacade] handleCancelTaskExecute({})", orderId);
        return processorDispatcher.execute("OrderCancelProcessor", orderId);
    }
}