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

package com.appdynamics.extensions.postgres;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.util.AssertUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.util.List;
import java.util.Map;

import static com.appdynamics.extensions.postgres.util.Constants.*;

/**
 * @author pradeep.nair
 */
public class PostgresMonitor extends ABaseMonitor {

    private final static ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    @Override
    protected String getDefaultMetricPrefix() {
        return DEFAULT_METRIC_PREFIX;
    }

    @Override
    public String getMonitorName() {
        return MONITOR_NAME;
    }

    @Override
    protected void doRun(TasksExecutionServiceProvider tasksExecutionServiceProvider) {
        for (Map<String, ?> server : getServers()) {
            AssertUtils.assertNotNull(server.get(DISPLAY_NAME), "The displayName section for the database " +
                    "server cannot be null");
            PostgresMonitorTask postgresMonitorTask = new PostgresMonitorTask(getContextConfiguration(),
                    tasksExecutionServiceProvider.getMetricWriteHelper(), server, server.get(DISPLAY_NAME).toString());
            tasksExecutionServiceProvider.submit(server.get(DISPLAY_NAME).toString(), postgresMonitorTask);
        }
    }

    @Override
    protected List<Map<String, ?>> getServers() {
        return (List<Map<String, ?>>) getContextConfiguration().getConfigYml().get(SERVERS);
    }

    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
