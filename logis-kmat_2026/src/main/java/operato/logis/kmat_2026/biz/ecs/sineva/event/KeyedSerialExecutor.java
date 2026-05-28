package operato.logis.kmat_2026.biz.ecs.sineva.event;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class KeyedSerialExecutor {

    private final ExecutorService workerPool;
    private final Map<String, SerialQueue> queues = new ConcurrentHashMap<>();

    public KeyedSerialExecutor(ExecutorService workerPool) {
        this.workerPool = workerPool;
    }

    public void execute(String key, Runnable task) {
        queues.computeIfAbsent(
                key,
                k -> new SerialQueue(workerPool, () -> queues.remove(k))
        ).enqueue(task);
    }

    private static class SerialQueue {
        private final Executor executor;
        private final Deque<Runnable> queue = new ArrayDeque<>();
        private final AtomicBoolean running = new AtomicBoolean(false);
        private final Runnable cleanup;

        SerialQueue(Executor executor, Runnable cleanup) {
            this.executor = executor;
            this.cleanup = cleanup;
        }

        synchronized void enqueue(Runnable task) {
            queue.addLast(wrap(task));
            if (running.compareAndSet(false, true)) {
                scheduleNext();
            }
        }

        private Runnable wrap(Runnable task) {
            return () -> {
                try {
                    task.run();
                } finally {
                    onDone();
                }
            };
        }

        private void onDone() {
            synchronized (this) {
                if (queue.isEmpty()) {
                    running.set(false);
                    cleanup.run();
                    return;
                }
            }
            scheduleNext();
        }

        private void scheduleNext() {
            Runnable next;
            synchronized (this) {
                next = queue.pollFirst();
            }
            if (next != null) {
                executor.execute(next);
            }
        }
    }
}