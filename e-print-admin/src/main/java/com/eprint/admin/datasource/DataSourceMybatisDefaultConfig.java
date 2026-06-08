package com.eprint.admin.datasource;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.sql.SQLException;

@Configuration
@EnableTransactionManagement
public class DataSourceMybatisDefaultConfig {

    @Value("${datasource.mybatis.default.driver-class-name}")
    private String driverClass;

    @Value("${datasource.mybatis.default.url}")
    private String url;

    @Value("${datasource.mybatis.default.username}")
    private String user;

    @Value("${datasource.mybatis.default.password}")
    private String password;

    @Value("${datasource.mybatis.default.connectionTimeout}")
    private int connectionTimeout;

    @Value("${datasource.mybatis.default.idleTimeout}")
    private int idleTimeout;

    @Value("${datasource.mybatis.default.minimumIdle}")
    private int minimumIdle;

    @Value("${datasource.mybatis.default.maximumPoolSize}")
    private int maximumPoolSize;

    @Value("${datasource.mybatis.default.maxLifetime}")
    private int maxLifetime;

    @Value("${datasource.mybatis.default.mapper-location}")
    private String mapperLocation;

    @Value("${mybatis.config-location}")
    private String mybatisConfigLocation;

    @Bean(name = "dataSourceMybatis")
    public DataSource dataSource() throws SQLException {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClass);
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(user);
        dataSource.setPassword(password);
        dataSource.setConnectionTimeout(connectionTimeout);
        dataSource.setIdleTimeout(idleTimeout);
        dataSource.setMinimumIdle(minimumIdle);
        dataSource.setMaximumPoolSize(maximumPoolSize);
        dataSource.setMaxLifetime(maxLifetime);
        return dataSource;
    }

    @Bean(name = "sqlSessionFactoryMybatis")
    @DependsOn({"dataSourceMybatis"})
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource());
        sessionFactory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(mapperLocation));
        sessionFactory.setConfigLocation(new DefaultResourceLoader().getResource(mybatisConfigLocation));
        return sessionFactory.getObject();
    }

    @Bean(name = "sqlSessionTemplateMybatis")
    @DependsOn({"sqlSessionFactoryMybatis"})
    public SqlSessionTemplate sqlSessionTemplate() throws Exception {
        return new SqlSessionTemplate(sqlSessionFactory());
    }

    @Bean(name = "transactionManagerMybatis")
    @DependsOn({"dataSourceMybatis"})
    public DataSourceTransactionManager transactionManager() throws SQLException {
        return new DataSourceTransactionManager(dataSource());
    }
}
