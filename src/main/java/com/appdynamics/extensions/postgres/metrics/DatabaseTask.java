/*
 * Copyright (c) 2019 AppDynamics,Inc.
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

package com.appdynamics.extensions.postgres.metrics;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.postgres.column.Column;
import com.appdynamics.extensions.postgres.column.ColumnGenerator;
import com.appdynamics.extensions.postgres.connection.ConnectionUtils;
import com.appdynamics.extensions.postgres.connection.PostgresConnectionConfig;
import com.google.common.base.Strings;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.Phaser;

import static com.appdynamics.extensions.postgres.util.Constants.*;

/**
 * @author pradeep.nair
 */
public class DatabaseTask implements Runnable {
    private static final Logger LOGGER = ExtensionsLoggerFactory.getLogger(DatabaseTask.class);

    private final String serverName;
    private final String dbName;
    private final Map<String, ?> databaseTask;
    private final Phaser phaser;
    private final String metricPrefix;
    private final PostgresConnectionConfig connConfig;
    private final MetricWriteHelper metricWriteHelper;

    public DatabaseTask(String serverName, String dbName, Map<String, ?> databaseTask, Phaser phaser,
                        PostgresConnectionConfig connConfig, String metricPrefix, MetricWriteHelper metricWriteHelper) {
        this.serverName = serverName;
        this.dbName = dbName;
        this.databaseTask = databaseTask;
        this.phaser = phaser;
        this.connConfig = connConfig;
        this.metricPrefix = metricPrefix;
        this.metricWriteHelper = metricWriteHelper;
        phaser.register();
    }

    @Override
    public void run() {
        LOGGER.debug("Collecting metrics for database {}, server {}", dbName, serverName);
        List<Map<String, ?>> queries = (List<Map<String, ?>>) databaseTask.get(QUERIES);
        if (queries == null || queries.size() == 0) {
            LOGGER.debug("No queries under database {} server {}.", dbName, serverName);
        } else {
            metricWriteHelper.transformAndPrintMetrics(getMetricsForQueries(queries));
        }
        phaser.arriveAndDeregister();
    }

    private List<Metric> getMetricsForQueries(List<Map<String, ?>> queries) {
        final List<Metric> metrics = new ArrayList<>();
        for (Map<String, ?> query : queries) {
            String name = (String) query.get(NAME);
            Boolean isServerLvlQuery = Boolean.valueOf((String) query.get(SERVER_LVL_QUERY));
            String queryStmt = (String) query.get(QUERY_STATEMENT);
            List<Map<String, ?>> columns = (List<Map<String, ?>>) query.get(COLUMNS);
            if (!isServerLvlQuery) {
                if (Strings.isNullOrEmpty(name)) {
                    LOGGER.debug("Query name is required for non server level queries. Skipping one query for " +
                            "database {} server {}", dbName, serverName);
                }
            }
            if (columns == null || columns.size() == 0) {
                LOGGER.debug("Columns not configured in config.yml for query {} database {} server {}", name, dbName,
                        serverName);
            }
            List<Column> cols = ColumnGenerator.getColumnsPOJO(columns);
            metrics.addAll(executeQuery(queryStmt, isServerLvlQuery, name, cols));
        }
        return metrics;
    }

    private List<Metric> executeQuery(String queryStmt, Boolean isServerLvlQuery, String name, List<Column> cols) {
        List<Metric> metrics = new ArrayList<>();
        try (Connection conn = ConnectionUtils.getConnection(DRIVER, connConfig.getUrl(),
                connConfig.getProps())) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(queryStmt)) {
                    if (rs != null) {
                        metrics = collectMetricsFromResultSet(isServerLvlQuery, name, rs, cols);
                    }
                    LOGGER.debug("Executed query {} database {} server {}. Size of metrics {}", name, dbName,
                            serverName, metrics.size());
                }
            }
        } catch (ClassNotFoundException cce) {
            LOGGER.error("ClassNotFoundException check drivers", cce);
        } catch (SQLException se) {
            LOGGER.error("Error executing SQL query", se);
        } catch (Exception e) {
            LOGGER.error("Unforeseen exception", e);
        }
        return metrics;
    }

    private List<Metric> collectMetricsFromResultSet(Boolean isServerLvlQuery, String name, ResultSet rs,
                                                     List<Column> cols) throws Exception {
        List<Metric> metrics = new ArrayList<>();
        while (rs.next()) {
            LinkedList<String> metricTokens = new LinkedList<>();
            // {metricPrefix}|{servername}
            metricTokens.add(serverName);
            // if not a server level query then dbname and query name should be included in metric path
            if (!isServerLvlQuery) {
                //{metricPrefix}|{servername}|{dbname}|{queryname}
                metricTokens.add(dbName);
                metricTokens.add(name);
            }
            Map<Column, String> metricValues = new HashMap<>();
            // first check get all tokens and values from ResultSet
            for (Column col : cols) {
                if (col.getType().equalsIgnoreCase("metricPath")) {
                    metricTokens.add(rs.getString(col.getName()));
                } else if (col.getType().equalsIgnoreCase("metricValue")) {
                    metricValues.put(col, rs.getString(col.getName()));
                }
            }
            // once all tokens are obtained from ResultSet, create metrics
            for (Map.Entry<Column, String> metricVal : metricValues.entrySet()) {
                Column col = metricVal.getKey();
                String metricName = col.getName();
                Map<String, ?> metricProps = col.getProperties();
                String metricValue = metricVal.getValue();
                metricTokens.add(metricName);
                String[] tokens = new String[metricTokens.size()];
                tokens = metricTokens.toArray(tokens);
                metricTokens.removeLast();
                Metric metric;
                if (metricProps == null || metricProps.size() == 0) {
                    metric = new Metric(metricName, metricValue, metricPrefix, tokens);
                } else {
                    metric = new Metric(metricName, metricValue, metricProps, metricPrefix, tokens);
                }
                metrics.add(metric);
            }
        }
        return metrics;
    }
}
