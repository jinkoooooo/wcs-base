package operato.logis.kmat_2026.biz.ecs.sineva.event;

import operato.logis.kmat_2026.entity.TbEcsTaskProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import xyz.elidom.util.ValueUtil;

import java.util.Map;

@Component
public class CallbackEventListener {

    private static final Logger logger =
            LoggerFactory.getLogger(CallbackEventListener.class);

    private final KeyedSerialExecutor keyedSerialExecutor;
    private final CallbackTxService callbackTxService;

    public CallbackEventListener(KeyedSerialExecutor keyedSerialExecutor,
                                 CallbackTxService callbackTxService) {
        this.keyedSerialExecutor = keyedSerialExecutor;
        this.callbackTxService = callbackTxService;
    }

    @EventListener
    public void handleCallbackEvent(CallbackEvent event) {
        Map<String, Object> data = event.getRequestData();
        TbEcsTaskProcess callbackTask =
                (TbEcsTaskProcess) data.get("callbackTask");

        if (ValueUtil.isEmpty(callbackTask)) {
            logger.warn("[CBK] callbackTask is empty");
            return;
        }

        String orderId = callbackTask.getOrderId();
        String status = callbackTask.getCbkStatus();

        if (ValueUtil.isEmpty(orderId)) {
            logger.warn("[CBK] orderId is empty");
            return;
        }

        logger.info("[CBK][ENQUEUE] orderId={}, status={}, thread={}",
                orderId, status, Thread.currentThread().getName());

        keyedSerialExecutor.execute(orderId, () -> {
            logger.info("[CBK][RUN] orderId={}, status={}, thread={}",
                    orderId, status, Thread.currentThread().getName());

            callbackTxService.apply(event.getDomain(), callbackTask);

            logger.info("[CBK][DONE] orderId={}, status={}",
                    orderId, status);
        });
    }
}