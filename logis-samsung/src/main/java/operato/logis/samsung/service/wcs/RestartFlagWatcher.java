package operato.logis.samsung.service.wcs;

import jakarta.annotation.PostConstruct;
import operato.logis.samsung.service.hokusho.proc.MeasureTrackProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class RestartFlagWatcher {
    private static final String FLAG_PATH = "C:\\server\\deploy\\restart.flag";
    private Logger logger = LoggerFactory.getLogger(RestartFlagWatcher.class);

    @PostConstruct
    public void start() {
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    File f = new File(FLAG_PATH);
                    if (f.exists()) {
                        logger.warn("[RESTART] flag detected: {}", FLAG_PATH);
                        // 여러 번 눌러도 1번만 타게 먼저 지움
                        boolean deleted = f.delete();
                        logger.warn("[RESTART] flag deleted={}", deleted);

                        // 비정상 종료로 처리되면 NSSM/복구정책이 재기동
                        System.exit(2);
                    }
                    Thread.sleep(2000); // 2초마다 체크
                } catch (Exception e) {
                    logger.error("[RESTART] watcher error", e);
                }
            }
        }, "restart-flag-watcher");
        t.setDaemon(true);
        t.start();
    }
}
