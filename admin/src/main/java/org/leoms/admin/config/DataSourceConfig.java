package org.leoms.admin.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {
    @Bean
    DataSource dataSource(@Value("${leoms.db.url}") String url,
                          @Value("${leoms.db.user}") String user,
                          @Value("${leoms.db.password-file}") String passwordFile) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(SecretFiles.readRequired("leoms.db.password-file", passwordFile));
        config.setMaximumPoolSize(4);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(5_000);
        config.setPoolName("leoms-admin");
        return new HikariDataSource(config);
    }
}
