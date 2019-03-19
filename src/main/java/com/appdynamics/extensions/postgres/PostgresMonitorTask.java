/*
 * Copyright (c) 2018 AppDynamics,Inc.
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

package com.appdynamics.extensions.postgres;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.postgres.connection.ConnectionConfigException;
import com.appdynamics.extensions.postgres.connection.PostgresConnectionConfig;
import com.appdynamics.extensions.postgres.connection.PostgresConnectionConfigHelper;
import com.appdynamics.extensions.postgres.metrics.DatabaseTask;
import com.appdynamics.extensions.util.CryptoUtils;
import com.google.common.base.Strings;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.appdynamics.extensions.postgres.util.Constants.DATABASES;
import static com.appdynamics.extensions.postgres.util.Constants.DB_NAME;

/**
 * @author pradeep.nair
 */
public class PostgresMonitorTask implements AMonitorTaskRunnable {
    private static final Logger LOGGER = ExtensionsLoggerFactory.getLogger(PostgresMonitorTask.class);
    private final MonitorContextConfiguration contextConfiguration;
    private final MetricWriteHelper metricWriteHelper;
    private final Map<String, ?> server;
    private final String serverName;
    private final AtomicBoolean heart_beat;

    PostgresMonitorTask(MonitorContextConfiguration contextConfiguration, MetricWriteHelper metricWriteHelper,
                        Map<String, ?> server, String serverName) {
        this.contextConfiguration = contextConfiguration;
        this.metricWriteHelper = metricWriteHelper;
        this.server = server;
        this.serverName = serverName;
        this.heart_beat = new AtomicBoolean();
    }

    @Override
    public void onTaskComplete() {
        LOGGER.debug("End metric collection task for database server {}", serverName);
    }

    @Override
    public void run() {
        LOGGER.info("Start metric collection task for server {}", serverName);
        try {
            List<Map<String, ?>> databaseTasks = (List<Map<String, ?>>) server.get(DATABASES);
            if (databaseTasks == null || databaseTasks.isEmpty()) {
                throw new ConnectionConfigException("Atleast one database is required for server " + serverName);
            }
            collectAndPublishMetric(databaseTasks);
        } catch (ConnectionConfigException cce) {
            LOGGER.error("Exception while reading config", cce);
        }
    }

    private void collectAndPublishMetric(List<Map<String, ?>> databaseTasks) throws ConnectionConfigException {
        final Phaser phaser = new Phaser();
        phaser.register();
        LOGGER.info("Found {} databases under server {}", databaseTasks.size(), serverName);
        for (Map<String, ?> databaseTask : databaseTasks) {
            final String dbName = (String) databaseTask.get(DB_NAME);
            if (Strings.isNullOrEmpty(dbName)) {
                LOGGER.debug("Please provide database name for server {}. Skipping entry...", serverName);
            } else {
//                PostgresConnectionConfig connectionConfig = getConnectionConfig(dbName);
                PostgresConnectionConfig connectionConfig = PostgresConnectionConfigHelper.getConnectionConfig(dbName
                        , serverName, CryptoUtils.getPassword(contextConfiguration.getConfigYml()), server);
                DatabaseTask task = new DatabaseTask(serverName, dbName, databaseTask, phaser, connectionConfig,
                        contextConfiguration.getMetricPrefix(), metricWriteHelper, heart_beat);
                contextConfiguration.getContext().getExecutorService().execute("Postgres db task - " + dbName, task);
            }
        }
        phaser.arriveAndAwaitAdvance();
    }
}
