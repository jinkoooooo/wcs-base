package xyz.elidom.sys.system.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class CustomRoutingDataSource extends AbstractRoutingDataSource {

    private static final ThreadLocal<String> contextHolder = new ThreadLocal<>();

    @Override
    protected Object determineCurrentLookupKey() {
        return contextHolder.get();  // "READ" 또는 "WRITE"
    }

    public static void setDataSourceKey(String key) {
        contextHolder.set(key);
    }

    public static void clearDataSourceKey() {
        contextHolder.remove();
    }
}