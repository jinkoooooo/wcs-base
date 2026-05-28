package operato.logis.wcs.common.service.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.manager.DbistQueryManager;

import java.lang.reflect.Proxy;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 자동 감사(Audit) 시스템을 켜기 위한 스프링 설정 파일
 * 1. 기존 DB 작업을 가로채는 '프록시'
 * 2. 가로챈 로그를 백그라운드에서 조용히 DB에 저장할 '전담 스레드 풀'
 * 이 두 가지를 스프링 빈(Bean)으로 등록하는 역할
 */
@Configuration
public class AuditingQueryManagerConfig {

    private static final Logger logger = LoggerFactory.getLogger(AuditingQueryManagerConfig.class);

    /**
     * 기존 서비스 코드들을 전혀 건드리지 않고 감사 로그를 남기기 위한 핵심 프록시
     * * @Primary가 붙어 있어서, 다른 서비스들이 "IQueryManager 좀 주세요!"라고 스프링에 요청할 때
     * 진짜가 아닌 '이 프록시(가짜)'가 최우선으로 주입(@Autowired)
     * * @param delegate 가짜 프록시가 몰래 품고 있을 '진짜 DB 실행기' (DbistQueryManager)
     * @param worker   가로챈 전후 데이터를 비교해 로그를 만들어 줄 작업자
     * @return DB 쓰기 작업을 중간에 낚아챌 수 있도록 세팅된 프록시 객체
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "wcs.audit.enabled", havingValue = "true", matchIfMissing = true)
    public IQueryManager auditingQueryManager(DbistQueryManager delegate, AuditWorker worker) {
        logger.info("[ Audit ][ Proxy ] auditing IQueryManager registered as @Primary");

        // IQueryManager의 껍데기를 쓴 프록시를 생성해서 던져줍니다.
        return (IQueryManager) Proxy.newProxyInstance(
                IQueryManager.class.getClassLoader(),
                new Class<?>[]{ IQueryManager.class },
                new AuditingInvocationHandler(delegate, worker));
    }

    /**
     * 감사 로그를 DB에 저장하는 일을 전담할 비동기 작업반(스레드 풀)
     * * 물류 비즈니스 로직(출고, 재고 변경 등)이 "로그 저장 대기" 때문에 느려지면 안 되므로,
     * 메인 스레드는 일만 시켜놓고 빠지고, 실제 저장은 이 스레드들이 백그라운드에서 알아서 처리
     */
    @Bean("auditExecutor")
    public TaskExecutor auditExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(2); // 평소에 대기하는 기본 스레드 수
        exec.setMaxPoolSize(4);  // 바쁠 때 늘어날 수 있는 최대 스레드 수
        exec.setQueueCapacity(10000); // 작업이 밀리면 최대 10,000개까지 줄을 세워둠
        exec.setKeepAliveSeconds(60);
        exec.setThreadNamePrefix("audit-");
        exec.setWaitForTasksToCompleteOnShutdown(true);
        exec.setAwaitTerminationSeconds(10);

        // [안전 장치] 만약 로그가 너무 많이 밀려서 큐(10,000개)가 꽉 차면 어떻게 할까?
        exec.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy() {
            private long discardedCount = 0L;
            private long lastLogAt = 0L;

            // "시스템 중단보다는 차라리 감사 로그를 포기하는 게 낫다!"
            // 에러를 던져서 시스템을 터뜨리지 않고(운영 최우선), 꽉 찬 이후의 로그는 조용히 버립니다.
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                synchronized (this) {
                    discardedCount++;
                    long now = System.currentTimeMillis();
                    // 로그를 버릴 때마다 에러를 찍으면 서버가 더 힘들어지니 5초에 한 번만 경고를 남김
                    if (now - lastLogAt > 5000L) {
                        logger.warn("[ Audit ][ Executor ] queue full - discarded={}", discardedCount);
                        lastLogAt = now;
                    }
                }
            }
        });

        exec.initialize();
        logger.info("[ Audit ][ Executor ] initialized - core={}, max={}, queue={}, policy=Discard", 2, 4, 10000);
        return exec;
    }
}