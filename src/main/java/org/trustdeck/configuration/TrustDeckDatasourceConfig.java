/*
 * Trust Deck Services
 * Copyright 2022-2024 Armin Müller and Eric Wündisch
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.trustdeck.configuration;

import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * The data source configuration for trust deck.
 * 
 * @author Armin Müller and Eric Wündisch
 */
@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
@Component
public class TrustDeckDatasourceConfig {

    /**
     * Create new data source properties.
     *
     * @return the data source properties
     */
    @Bean(name = "trustdeckDataSourceProperties")
    @ConfigurationProperties(prefix = "app.datasource.trustdeck")
    public DataSourceProperties trustdeckDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * Create a new Hikari data source for the trustdeck services.
     *
     * @param dataSourceProperties the properties for the data source
     * @return the Hikari data source
     */
    @Bean(name = "trustdeckDataSource")
    @ConfigurationProperties(prefix = "app.datasource.trustdeck.configuration")
    public HikariDataSource trustdeckDataSource(@Qualifier("trustdeckDataSourceProperties") DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    /**
     * Create a new data source connection provider.
     *
     * @param dataSource the data source
     * @return the data source connection provider
     */
    @Bean
    public DataSourceConnectionProvider trustdeckConnectionProvider(@Qualifier("trustdeckDataSource") HikariDataSource dataSource) {
        return new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(dataSource));
    }

    /**
     * Create a new domain-specific language (DSL) context.
     *
     * @param dataSource the data source
     * @return the dsl context
     */
    @Bean(name = "trustdeckDsl")
    public DSLContext trustdeckDsl(@Qualifier("trustdeckDataSource") HikariDataSource dataSource) {
        return new DefaultDSLContext(trustdeckConfiguration(dataSource));
    }

    /**
     * Create a new default configuration for the trustdeck services.
     *
     * @param dataSource the data source
     * @return the default configuration
     */
    public DefaultConfiguration trustdeckConfiguration(@Qualifier("trustdeckDataSource") HikariDataSource dataSource) {
        DefaultConfiguration config = new DefaultConfiguration();
        config.set(trustdeckConnectionProvider(dataSource));
        config.set(SQLDialect.POSTGRES);
        return config;
    }
}
