package operato.logis.kmat_2026.biz.ecs.sineva.event;

import operato.logis.kmat_2026.biz.ecs.sineva.service.EquipCallbackRoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * ============================================================================
 * Equip Event Listener
 * ============================================================================
 *
 * [역할]
 * - 기존 AgvProcessService 진입 조건 유지
 * - 실제 routing은 EquipCallbackRoutingService로 위임
 */
@Component
public class EquipEventListener {

    private static final Logger logger = LoggerFactory.getLogger(EquipEventListener.class);

    private final EquipCallbackRoutingService equipCallbackRoutingService;

    public EquipEventListener(EquipCallbackRoutingService equipCallbackRoutingService) {
        this.equipCallbackRoutingService = equipCallbackRoutingService;
    }

    @EventListener(classes = EquipEvent.class, condition = "#event.equipType == '0' or #event.equipType == '1'")
    @Transactional
    public void onAgvEquipEvent(EquipEvent event) {
        try {
            logger.info("[EquipEventListener] equipType={}, orderId={}, cbkStatus={}",
                    event.getEquipType(),
                    event.getOrder().getOrderId(),
                    event.getOrder().getCbkStatus());

            equipCallbackRoutingService.route(event.getOrder(), event.getErrorCode());
        } catch (Exception e) {
            logger.error("[EquipEventListener] processing fail - orderId={}, reason={}",
                    event.getOrder().getOrderId(), e.getMessage(), e);
        }
    }
}