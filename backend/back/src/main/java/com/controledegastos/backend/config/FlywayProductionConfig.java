package com.controledegastos.backend.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Forces Flyway migrations to run in hosted production environments before JPA validates the schema.
 */
@Configuration(proxyBeanMethods = false)
@Profile({"prod", "docker"})
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FlywayProductionConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayProductionConfig.class);

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        log.info("[FLYWAY] Preparing explicit production migration runner");

        FluentConfiguration configuration = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .validateOnMigrate(true);

        return configuration.load();
    }

    @Bean
    public static FlywayEntityManagerFactoryDependsOnPostProcessor entityManagerFactoryDependsOnFlyway() {
        return new FlywayEntityManagerFactoryDependsOnPostProcessor();
    }

    static class FlywayEntityManagerFactoryDependsOnPostProcessor extends EntityManagerFactoryDependsOnPostProcessor {

        FlywayEntityManagerFactoryDependsOnPostProcessor() {
            super("flyway");
        }
    }
}
