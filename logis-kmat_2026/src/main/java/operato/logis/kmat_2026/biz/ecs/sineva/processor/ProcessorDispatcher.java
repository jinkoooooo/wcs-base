package operato.logis.kmat_2026.biz.ecs.sineva.processor;

import operato.logis.kmat_2026.entity.TbWcsOrder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * Processor Dispatcher
 * ============================================================================
 *
 * [역할]
 * - processorType 문자열로 Processor를 찾는다.
 * - 기존 PodProcessorDispatcher 정책을 유지하되,
 *   더 일반적인 이름으로 정리하였다.
 */
@Component
public class ProcessorDispatcher {

    private final Map<String, OrderProcessor<?>> processorMap = new HashMap<>();

    public ProcessorDispatcher(List<OrderProcessor<?>> processors) {
        for (OrderProcessor<?> processor : processors) {
            processorMap.put(processor.getProcessorType(), processor);
        }
    }

    @SuppressWarnings("unchecked")
    public <R> R execute(String processorType, String param) {
        OrderProcessor<R> processor = (OrderProcessor<R>) processorMap.get(processorType);
        if (processor == null) {
            return null;
        }
        return processor.execute(param);
    }

    public void callback(String processorType, TbWcsOrder order) throws InterruptedException {
        OrderProcessor<?> processor = processorMap.get(processorType);
        if (processor != null) {
            processor.callback(order);
        }
    }

    public OrderProcessor<?> getProcessor(String processorType) {
        return processorMap.get(processorType);
    }
}