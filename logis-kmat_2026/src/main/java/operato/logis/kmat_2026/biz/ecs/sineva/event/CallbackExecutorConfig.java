package operato.logis.kmat_2026.biz.ecs.sineva.event;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class CallbackExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService callbackWorkerPool() {
        return Executors.newFixedThreadPool(
                Math.max(4, Runtime.getRuntime().availableProcessors())
        );
    }

    @Bean
    public KeyedSerialExecutor keyedSerialExecutor(
            ExecutorService callbackWorkerPool) {
        return new KeyedSerialExecutor(callbackWorkerPool);
    }
}