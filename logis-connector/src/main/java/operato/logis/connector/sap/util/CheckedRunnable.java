package operato.logis.connector.sap.util;

@FunctionalInterface
public interface CheckedRunnable {
    void run() throws Exception;
}

