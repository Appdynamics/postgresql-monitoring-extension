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

package com.appdynamics.extensions.postgres.util;

/**
 *
 * @author pradeep.nair
 */
public class Constants {
    public static final String DEFAULT_METRIC_PREFIX = "Custom Metrics|PostgreSQL";
    public static final String DEFAULT_METRIC_SEPARATOR = "|";
    public static final String MONITOR_NAME = "PostgresMonitor";
    public static final String SERVERS = "servers";
    public static final String DISPLAY_NAME = "displayName";
    public static final String HOST = "host";
    public static final String USE_IPV6 = "useIpv6";
    public static final String PORT = "port";
    public static final String USER = "user";
    public static final String PASSWORD = "password";
    public static final String ENCRYPTION_KEY = "encryptionKey";
    public static final String OPTIONAL_CONNECTION_PROPERTIES = "optionalConnectionProperties";
    public static final String DATABASES = "databases";
    public static final String DB_NAME = "dbName";
    public static final String QUERIES = "queries";
    public static final String NAME = "name";
    public static final String SERVER_LVL_QUERY = "serverLvlQuery";
    public static final String QUERY_STATEMENT = "queryStmt";
    public static final String COLUMNS = "columns";
    public static final String ALIAS = "alias";
    public static final String AGGREGATION_TYPE = "aggregationType";
    public static final String TIME_ROLLUP_TYPE = "timeRollUpType";
    public static final String CLUSTER_ROLLUP_TYPE = "clusterRollUpType";
    public static final String APPLICATION_NAME = "ApplicationName";
    public static final String PROTOCOL = "jdbc:postgresql:";
    public static final String DRIVER = "org.postgresql.Driver";
    public static final String DEFAULT_APPLICATION_NAME = "AppDynamicsPSQLExtension";
    public static final String HEART_BEAT = "HEART_BEAT";
}
