package com.qiujie.config;

import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Primary
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.master")
    public DataSource masterDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * 为主数据源显式配置事务管理器，确保 @Transactional 正确提交。
     * 多数据源场景下 Spring Boot 可能不确定用哪个 TransactionManager。
     */
    @Primary
    @Bean
    public PlatformTransactionManager transactionManager(
            @Qualifier("masterDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }


    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.flowable")
    public DataSource flowableDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.kb")
    public DataSource kbDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * 让 Flowable 使用独立的 flowable 数据源，而非主数据源
     */
    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> engineConfigurationConfigurer(
            @Qualifier("flowableDataSource") DataSource dataSource) {
        return config -> config.setDataSource(dataSource);
    }
}
