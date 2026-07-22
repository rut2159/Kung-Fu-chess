package com.chessgame.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;

/**
 * Defines the SQLite DataSource directly in code instead of relying on
 * spring.datasource.* properties from application.properties.
 *
 * This sidesteps an entire class of failure modes (typos, file encoding,
 * the file not being copied to target/classes, a stray application.yml
 * overriding it, etc.) - the JDBC URL below is the one and only source of
 * truth for where the database file lives.
 */
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:kungfuchess.db");
        return dataSource;
    }
}
