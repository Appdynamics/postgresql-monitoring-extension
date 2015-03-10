package com.appdynamics.monitors.postgres;

import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class PostgreSQLMonitorTests {
    PostgreSQLMonitor monitor;
    Statement mockStatement;
    Connection mockConnection;

    @Before
    public void before() throws SQLException {
        mockConnection = mock(Connection.class);
        mockStatement = mock(Statement.class);
        when(mockStatement.executeQuery(anyString())).thenReturn(mock(ResultSet.class));
        when(mockConnection.createStatement()).thenReturn(mockStatement);
    }

    @Test
    public void testExecutesQueryAgainstProvidedConnection() throws TaskExecutionException, SQLException {
        monitor = new PostgreSQLMonitor() {
            @Override
            public Connection getConnection(String connString) throws SQLException {
                return mockConnection;
            }
        };
        monitor.execute(new HashMap<String, String>(), null);
        verify(mockStatement).executeQuery(anyString());
    }

    @Test
    public void testPostsRecordsFound() throws TaskExecutionException, SQLException {
        final Map<String, Map<String, String>> rows = new TreeMap<String, Map<String, String>>(String.CASE_INSENSITIVE_ORDER);
        final Map<String, Map<String, String>> printedValues = new TreeMap<String, Map<String, String>>(String.CASE_INSENSITIVE_ORDER);
        monitor = new PostgreSQLMonitor() {
            @Override
            protected Map<String, Map<String, String>> getValuesForColumns(String[] columns, String query) throws Exception {
                return rows;
            }

            @Override
            protected void printMetric(String databaseName, String name, String value, String aggType, String timeRollup, String clusterRollup) {
                if(!printedValues.containsKey(databaseName)){
                    printedValues.put(databaseName, new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
                }
                printedValues.get(databaseName).put(name, value);
            }
        };
        HashMap<String, String> args = new HashMap<String, String>();
        args.put("cumulative_columns","xact_commit");

        rows.put("dev1", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
        rows.put("dev2", new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER));
        rows.get("dev1").put("NUMBACKENDS", "1");
        rows.get("dev2").put("NUMBACKENDS", "5");
        rows.get("dev1").put("xact_commit", "3");
        monitor.execute(args, null);
        Assert.assertTrue(printedValues.containsKey("dev1"));
        Assert.assertEquals("1", printedValues.get("dev1").get("Num Backends"));
        Assert.assertEquals("5", printedValues.get("dev2").get("Num Backends"));
        rows.get("dev1").put("xact_commit", "6");
        rows.get("dev1").put("numbackends", "2");
        monitor.execute(args, null);
        Assert.assertEquals("3", printedValues.get("dev1").get("Txn Commits"));
        Assert.assertEquals("2", printedValues.get("dev1").get("Num Backends"));
    }
}
