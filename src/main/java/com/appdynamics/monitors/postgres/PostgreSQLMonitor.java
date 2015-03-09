/**
 * Copyright 2013 AppDynamics
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


package com.appdynamics.monitors.postgres;

import com.appdynamics.monitors.common.JavaServersMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

public class PostgreSQLMonitor extends JavaServersMonitor {
    private static final String[] dbStatisticsColumns = new String[]{"numbackends", "xact_commit", "xact_rollback", "blks_read", "blks_hit", "tup_returned", "tup_fetched", "tup_inserted", "tup_updated", "tup_deleted"};
    private static final Map<String, String> columnDescriptions = new ConcurrentSkipListMap<String, String>(String.CASE_INSENSITIVE_ORDER) {{
        put("numbackends", "Num Backends");
        put("xact_commit", "Txn Commits");
        put("xact_rollback", "Txn Rollbacks");
        put("blks_read", "Blocks Read");
        put("blks_hit", "Blocks Hit");
        put("tup_returned", "Tuples Returned");
        put("tup_fetched", "Tuples Fetched");
        put("tup_inserted", "Tuples Inserted");
        put("tup_updated", "Tuples Updated");
        put("tup_deleted", "Tuples Deleted");
    }};
    private volatile String tierName;
    private volatile String database; // the database we are interested in collecting metrics on
    private final Collection<String> cumulativeColumnNames = Arrays.asList("xact_commit", "xact_rollback", "blks_read", "blks_hit", "tup_returned", "tup_fetched", "tup_inserted", "tup_updated", "tup_deleted");

    protected void parseArgs(Map<String, String> args) {
        super.parseArgs(args);
        tierName = getArg(args, "tier", null); // if the tier is not specified then create the metrics for all tiers
        database = getArg(args, "target-database", "postgres");

    }

    private Connection connect() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        String connStr = "jdbc:postgresql://" + host + ":";
        if ((port == null) || (port.equals("")))
            connStr += "5432";
        else
            connStr += port;

        connStr += "/postgres?";
        if ((userName != null) && (!userName.equals("")))
            connStr += "user=" + userName;
        else
            connStr += "user=root";

        if ((passwd != null) && (!passwd.equals("")))
            connStr += "&password=" + passwd;

        logger.debug("Connecting to: " + connStr);
        Class.forName("org.postgresql.Driver").newInstance();
        Connection conn = getConnection(connStr);
        logger.debug("Successfully connected to Postgres DB");
        return conn;
    }

    // collects all monitoring data for this time period from database
    protected Map<String, String> getValuesForColumns(String[] columnNames, String query) throws Exception {
        Map<String, String> columnName2Value = new HashMap<String, String>();

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        boolean debug = logger.isDebugEnabled();

        try {
            conn = connect();
            stmt = conn.createStatement();

            if (debug) {
                logger.debug("Executing query [" + query + "]");
            }

            rs = stmt.executeQuery(query);

            // get most accurate time
            currentTime = System.currentTimeMillis();

            while (rs.next()) {
                for (String columnName : columnNames) {
                    if (columnName != null) {
                        String value = rs.getString(columnName);

                        if (debug) {
                            logger.debug("[column,value] = [" + columnName + "," + value + "]");
                        }

                        columnName2Value.put(columnName, value);
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Error while executing query [" + query + "]", ex);
            throw ex;
        } finally {
            close(rs, stmt, conn);
        }

        return Collections.synchronizedMap(columnName2Value);
    }

    public TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext taskContext)
            throws TaskExecutionException {
        startExecute(taskArguments);

        try {
        logger.debug("Querying fresh values for PostgreSQLMonitor ...");

        // Store the current values for the columns specified in the list
        valueMap.putAll(getValuesForColumns(dbStatisticsColumns, "select * from pg_stat_database where datname='" + database + "'"));

        } catch (Exception ex) {
            throw new TaskExecutionException(ex);
        }

        // just for debug output
        logger.debug("Starting METRIC COLLECTION for PostgreSQL Monitor.......");

        for (String dbColumnName : dbStatisticsColumns) {
            printMetric(dbColumnName, getColumnDescription(dbColumnName));
        }
        return this.finishExecute();
    }

    private String getColumnDescription(String dbColumnName) {
        if (columnDescriptions.containsKey(dbColumnName)) {
            return columnDescriptions.get(dbColumnName);
        } else {
            return dbColumnName;
        }
    }

    private void printMetric(String columnName, String metricLabel) {
        final String currentValue = getString(columnName);

        final String value;
        final boolean print;
        final boolean metricIsCumulative = cumulativeColumnNames.contains(columnName);
        if (metricIsCumulative) {
            if (oldValueMap.containsKey(columnName)) {
                logger.debug(String.format("%s: Current value: %s, previous value: %s", columnName, currentValue, oldValueMap.get(columnName)));
                value = getString(Long.parseLong(currentValue) - Long.parseLong(oldValueMap.get(columnName)));
                print = true;
            } else {
                print = false;
                value = "-1";
            }
        } else {
            value = currentValue;
            print = true;
        }

        if (print) {
            printMetric(metricLabel, value,
                    metricIsCumulative ? MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE : MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                    metricIsCumulative? MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE : MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                    metricIsCumulative ? MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL : MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
        }
    }

    protected String getMetricPrefix() {
        if (tierName != null) {
            return "Server|Component:" + tierName + "|Postgres Server|" + database + "|";
        } else {
            return "Custom Metrics|Postgres Server|" + database + "|";
        }
    }

    protected Connection getConnection(String connString) throws SQLException {
        return DriverManager.getConnection(connString);
    }
}
