/**
 * Copyright 2013 AppDynamics
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.appdynamics.monitors.postgres;

import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.util.MetricWriteHelper;
import com.appdynamics.extensions.util.MetricWriteHelperFactory;
import com.google.common.base.Strings;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.List;
import java.util.Map;

public class PostgreSQLMonitor extends AManagedMonitor {

    private static final Logger logger = Logger.getLogger(PostgreSQLMonitor.class);


    /**
     * The metric can be found in Application Infrastructure Performance|{@literal <}Node{@literal >}|Custom Metrics|Postgres Server|
     */
    private static final String METRIC_PREFIX = "Custom Metrics|Postgres Server|";
    private static final String CONFIG_ARG = "config-file";
    private static final String METRIC_ARG = "metric-file";
    private static final String FILE_NAME = "monitors/PostgreSQLMonitor/config.yml";

    private boolean initialized = false;
    private MonitorConfiguration configuration;

    public PostgreSQLMonitor() {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        System.out.println(msg);
    }

    private static String getImplementationVersion() {
        return PostgreSQLMonitor.class.getPackage().getImplementationTitle();
    }

    public TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        logger.info("Starting the Postgres Monitoring task.");

        Thread thread = Thread.currentThread();
        ClassLoader originalCl = thread.getContextClassLoader();
        thread.setContextClassLoader(AManagedMonitor.class.getClassLoader());

        try {
            String configFilename = getConfigFilename(taskArguments.get(CONFIG_ARG));
            String metricFile = getConfigFilename(taskArguments.get(METRIC_ARG));

            if (!initialized) {
                initialize(configFilename, metricFile);
            }
            configuration.executeTask();

            logger.info("Finished Postgres monitor execution");
            return new TaskOutput("Finished Postgres monitor execution");
        } catch (Exception e) {
            logger.error("Failed to execute the Postgres monitoring task", e);
            throw new TaskExecutionException("Failed to execute the Postgres monitoring task" + e);
        } finally {
            thread.setContextClassLoader(originalCl);
        }
    }

    private void initialize(String configFile, String metricFile) {
        if (!initialized) {

            MetricWriteHelper metricWriteHelper = MetricWriteHelperFactory.create(this);
            MonitorConfiguration conf = new MonitorConfiguration(METRIC_PREFIX, new TaskRunnable(), metricWriteHelper);

            conf.setConfigYml(configFile);

            conf.setMetricsXml(metricFile, Stat.Stats.class);

            conf.checkIfInitialized(MonitorConfiguration.ConfItem.CONFIG_YML, MonitorConfiguration.ConfItem.METRICS_XML, MonitorConfiguration.ConfItem.EXECUTOR_SERVICE,
                    MonitorConfiguration.ConfItem.METRIC_PREFIX, MonitorConfiguration.ConfItem.METRIC_WRITE_HELPER);
            this.configuration = conf;
            initialized = true;
        }
    }

    private class TaskRunnable implements Runnable {

        public void run() {

            if (!initialized) {
                logger.info("Postgres Monitor is still initializing");
                return;
            }

            Map<String, ?> config = configuration.getConfigYml();

            List<Map<String, Object>> postgresServers = (List<Map<String, Object>>) config.get("pgServers");

            if (postgresServers != null && !postgresServers.isEmpty()) {
                Stat[] stats = ((Stat.Stats) configuration.getMetricsXmlConfiguration()).getStats();

                if (stats != null && stats.length > 0) {

                    for (Map<String, Object> postgresServer : postgresServers) {

                        String name = (String) postgresServer.get("name");

                        Stat stat = getStat(stats, name);

                        if (stat != null) {
                            PostgreTask postgreTask = new PostgreTask(configuration, postgresServer, stat);
                            configuration.getExecutorService().execute(postgreTask);
                        } else {
                            logger.info("No metrics configuration found for [ " + name + " ] in metrics.xml");
                        }
                    }

                } else {
                    logger.info("No metrics configuration found in metrics.xml");
                }

            } else {
                logger.info("No servers configuration found in config.yml");
            }
        }
    }

    private Stat getStat(Stat[] stats, String name) {

        for (Stat stat : stats) {
            if (stat.getName().equals(name)) {
                return stat;
            }
        }
        return null;
    }

    private String getConfigFilename(String filename) {
        if (filename == null) {
            return "";
        }

        if ("".equals(filename)) {
            filename = FILE_NAME;
        }
        // for absolute paths
        if (new File(filename).exists()) {
            return filename;
        }
        // for relative paths
        File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
        String configFileName = "";
        if (!Strings.isNullOrEmpty(filename)) {
            configFileName = jarPath + File.separator + filename;
        }
        return configFileName;
    }
}