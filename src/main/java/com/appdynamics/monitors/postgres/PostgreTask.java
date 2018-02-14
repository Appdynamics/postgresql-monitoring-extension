/*
 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.monitors.postgres;

import com.appdynamics.TaskInputArgs;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.crypto.CryptoUtil;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Satish Muddam
 */
public class PostgreTask implements Runnable {

    private static final Logger logger = Logger.getLogger(PostgreTask.class);

    private MonitorConfiguration configuration;
    private Map<String, Object> postgresServer;
    private Stat stat;

    public PostgreTask(MonitorConfiguration configuration, Map<String, Object> postgresServer, Stat stat) {

        this.configuration = configuration;
        this.postgresServer = postgresServer;
        this.stat = stat;
    }

    public void run() {

        String name = (String) postgresServer.get("displayName");
        String host = (String) postgresServer.get("host");
        String port = String.valueOf(postgresServer.get("port"));
        String user = (String) postgresServer.get("user");
        String password = (String) postgresServer.get("password");
        String encryptedPassword = (String) postgresServer.get("encryptedPassword");
        String encryptionKey = (String) postgresServer.get("encryptionKey");

        String decryptedPassword = getPassword(password, encryptedPassword, encryptionKey);

        String database = (String) postgresServer.get("targetDatabase");

        List<String> columnNames = getColumnNames(stat);

        Map<String, String> valuesForColumns = getValuesForColumns(host, port, user, decryptedPassword, columnNames, database);
        printMetrics(name, valuesForColumns, stat);

    }

    private List<String> getColumnNames(Stat stat) {

        List<String> columnNames = new ArrayList<String>();
        Metric[] metrics = stat.getMetrics();

        for (Metric metric : metrics) {
            columnNames.add(metric.getColumnName());
        }

        return columnNames;
    }

    private void printMetrics(String name, Map<String, String> valuesForColumns, Stat stat) {

        Metric[] metrics = stat.getMetrics();

        for (Metric metric : metrics) {
            String metricType = getMetricType(stat, metric);
            String value = valuesForColumns.get(metric.getColumnName());
            configuration.getMetricWriter().printMetric(configuration.getMetricPrefix() + "|" + name + "|" + metric.getLabel(), new BigDecimal(value), metricType);
        }
    }

    private String getMetricType(Stat stat, Metric metric) {
        if (!Strings.isNullOrEmpty(metric.getMetricType())) {
            return metric.getMetricType();
        } else if (!Strings.isNullOrEmpty(stat.getMetricType())) {
            return stat.getMetricType();
        } else {
            return null;
        }
    }

    private String getPassword(String password, String encryptedPassword, String encryptionKey) {

        if (!Strings.isNullOrEmpty(password)) {
            return password;
        }

        try {
            Map<String, String> args = Maps.newHashMap();
            args.put(TaskInputArgs.PASSWORD_ENCRYPTED, encryptedPassword);
            args.put(TaskInputArgs.ENCRYPTION_KEY, encryptionKey);
            return CryptoUtil.getPassword(args);
        } catch (IllegalArgumentException e) {
            String msg = "Encryption Key not specified. Please set the value in config.yaml.";
            logger.error(msg);
            throw new IllegalArgumentException(msg);
        }
    }

    private Map<String, String> getValuesForColumns(String host, String port, String user, String password, List<String> columnNames, String database) {
        Map<String, String> columnName2Value = new HashMap<String, String>();

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        boolean debug = logger.isDebugEnabled();

        String query = "select * from pg_stat_database where datname='" + database + "'";

        try {
            conn = connect(host, port, database, user, password);
            stmt = conn.createStatement();

            if (debug) {
                logger.debug("Executing query [" + query + "]");
            }

            rs = stmt.executeQuery(query);

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
            logger.error("Error while executing query [" + query + "] to capture values for columns: " + columnNames, ex);
        } finally {
            close(rs, stmt, conn);
        }

        return columnName2Value;
    }

    private Connection connect(String host, String port, String database, String userName, String passwd) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        String connStr = "jdbc:postgresql://" + host + ":";

        if ((port == null) || (port.equals(""))) {
            connStr += "5432";
        } else {
            connStr += port;
        }

        connStr += "/"+database+"?";

        if ((userName != null) && (!userName.equals(""))) {
            connStr += "user=" + userName;
        } else {
            connStr += "user=root";
        }

        String connStrLog = connStr;
        if ((passwd != null) && (!passwd.equals(""))) {
            connStr += "&password=" + passwd;
            connStrLog += "&password=YES";
        }

        logger.debug("Connecting to: " + connStrLog);
        Class.forName("org.postgresql.Driver").newInstance();
        Connection conn = DriverManager.getConnection(connStr);
        logger.debug("Successfully connected to Postgres DB");
        return conn;
    }

    protected void close(ResultSet rs, Statement stmt, Connection conn) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Exception e) {
                // ignore
            }
        }

        if (stmt != null) {
            try {
                stmt.close();
            } catch (Exception e) {
                // ignore
            }
        }

        if (conn != null) {
            try {
                conn.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
