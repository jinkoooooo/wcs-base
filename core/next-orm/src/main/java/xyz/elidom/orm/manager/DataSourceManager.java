package xyz.elidom.orm.manager;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import xyz.elidom.dbist.ddl.impl.DdlJdbc;
import xyz.elidom.dbist.dml.impl.DmlJdbc2;
import xyz.elidom.dbist.processor.Preprocessor;
import xyz.elidom.exception.server.ElidomServiceException;
import xyz.elidom.exception.server.ElidomValidationException;
import xyz.elidom.orm.IDataSourceManager;
import xyz.elidom.orm.IQueryManager;
import xyz.elidom.orm.OrmConstants;

/**
 * 시스템 기본 데이터소스외에 다른 데이터소스를 관리하기 위한 매니저
 * 
 */
@Component
public class DataSourceManager implements IDataSourceManager {

    /**
     * IQueryManager Pool - key : datasource name, value : IQueryManager
     */
    private Map<String, IQueryManager> QUERY_MANAGER_POOL = new ConcurrentHashMap<String, IQueryManager>(3);

    @Override
    public boolean isExistDataSource(String dataSourceName) {
        return QUERY_MANAGER_POOL.containsKey(dataSourceName);
    }
    
    @Override
    public DataSource getDataSource(String dataSourceName) {
        IQueryManager queryManager = QUERY_MANAGER_POOL.get(dataSourceName);
        if (queryManager == null) {
            throw new ElidomValidationException("DataSource [" + dataSourceName + "] Not Found!");
        }

        return queryManager.getDml().getDataSource();
    }
    
    @Override
    public IQueryManager getQueryManager(String dataSourceName) {
        IQueryManager queryManager = QUERY_MANAGER_POOL.get(dataSourceName);
        if (queryManager == null) {
            throw new ElidomValidationException("DataSource [" + dataSourceName + "] Not Found!");
        }

        return queryManager;
    }

    @Override
    public void initializeDataSource(String dataSourceName, String driverClassName, String url, String user, String passwd, int minIdle, int maxIdle, int maxActive, long maxWait,
            long evictTime) {
        this.initializeDataSource(dataSourceName, driverClassName, url, OrmConstants.DEFAULT_DOMAIN, user, passwd, minIdle, maxIdle, maxActive, maxWait, evictTime);
    }

    @Override
    public void initializeDataSource(String dataSourceName, String driverClassName, String url, String domain, String user, String passwd, int minIdle, int maxIdle, int maxActive,
            long maxWait, long evictTime) {
        if (QUERY_MANAGER_POOL.containsKey(dataSourceName)) {
            this.destroyDataSource(dataSourceName);
        }

        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName(driverClassName);
        ds.setUrl(url);
        ds.setUsername(user);
        ds.setPassword(passwd);
        ds.setMaxTotal(maxActive);
        ds.setMinIdle(minIdle);
        ds.setMaxIdle(maxIdle);
        ds.setMaxWait(Duration.ofMillis(maxWait));
        ds.setDurationBetweenEvictionRuns(Duration.ofMillis(maxWait));

        try {
            Connection conn = ds.getConnection();
            conn.close();
            
            DataSourceQueryManager queryManager = new DataSourceQueryManager();
            DmlJdbc2 dml = new DmlJdbc2();
            dml.setDomain(domain);
            dml.setDataSource(ds);
            
            JdbcOperations jdbcOperations = new JdbcTemplate(ds);
            dml.setJdbcOperations(jdbcOperations);
            
            NamedParameterJdbcOperations namedParameterJdbcOperations = new NamedParameterJdbcTemplate(ds);
            dml.setNamedParameterJdbcOperations(namedParameterJdbcOperations);
            
            Preprocessor preprocessor = new xyz.elidom.dbist.processor.impl.VelocityPreprocessor();
            dml.setPreprocessor(preprocessor);
            
            DdlJdbc ddl = new DdlJdbc();
            ddl.setDml(dml);
            
            queryManager.setDml(dml);
            queryManager.setDdl(ddl);
            dml.afterPropertiesSet();

            QUERY_MANAGER_POOL.put(dataSourceName, queryManager);

        } catch (Exception e) {
            throw new ElidomServiceException("Failed to initialize datasource!", e.getCause());
        }

    }

    @Override
    public void destroyDataSource(String dataSourceName) {
        DataSource ds = this.getDataSource(dataSourceName);

        if (ds instanceof BasicDataSource) {
            BasicDataSource dataSource = (BasicDataSource) ds;
            if (!dataSource.isClosed()) {
                try {
                    dataSource.close();
                } catch (SQLException e) {
                    // handle exception
                }
            }
        }

        QUERY_MANAGER_POOL.remove(dataSourceName);
    }
    
    @Override
    public Set<String> getDataSourceNames() {
        return QUERY_MANAGER_POOL.keySet();
    }
}