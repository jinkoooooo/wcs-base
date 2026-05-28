package xyz.elidom.sys.system.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import xyz.elidom.dbist.dml.Dml;
import xyz.elidom.dbist.dml.impl.DmlJdbc2;

@Configuration
public class DataSourceConfig {

	@Value("${transation.readonly-pattern:}")
    private String readPattern;
    
    @Bean(name = "writeDataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    DataSource writeDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "readDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.read")
    DataSource readDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "dataSource")
    @Primary
    DataSource routingDataSource() {
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("WRITE", writeDataSource());
        if(readPattern.length()>0) {
            targetDataSources.put("READ", readDataSource());	
        }

        AbstractRoutingDataSource routingDataSource = new CustomRoutingDataSource();
        routingDataSource.setDefaultTargetDataSource(writeDataSource());
        routingDataSource.setTargetDataSources(targetDataSources);

        return routingDataSource;
    }

    @Bean
    Dml dml() {
        DmlJdbc2 dmlJdbc2 = new DmlJdbc2();
        dmlJdbc2.setDataSource(routingDataSource());  // 다중 DataSource 설정
        return dmlJdbc2;
    }
}