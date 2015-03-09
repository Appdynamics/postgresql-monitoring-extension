package com.appdynamics.monitors.postgres;

import com.appdynamics.monitors.common.JavaServersMonitor;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        final Map<String, String> rows = new HashMap<String, String>();
        final Map<String, String> printedValues = new HashMap<String, String>();
        monitor = new PostgreSQLMonitor() {
            @Override
            protected Map<String, String> getValuesForColumns(List<String> columnNames, String query) throws Exception {
                return rows;
            }

            @Override
            protected void printMetric(String name, String value, String aggType, String timeRollup, String clusterRollup) {
                printedValues.put(name, value);
            }
        };
        HashMap<String, String> args = new HashMap<String, String>();
        args.put("columns","numbackends,xact_commit");
        args.put("cumulative_columns","xact_commit");

        rows.put("NUMBACKENDS", "1");
        rows.put("xact_commit", "3");
        monitor.execute(args, null);
        Assert.assertEquals(1, printedValues.size());
        String key = printedValues.keySet().toArray()[0].toString();
        Assert.assertTrue(key.contains("Backends"));
        Assert.assertEquals("1", printedValues.get(key));
        rows.put("xact_commit", "5");
        monitor.execute(args, null);
        Assert.assertEquals(2, printedValues.size());
        Assert.assertEquals("2", printedValues.get("Custom Metrics|Postgres Server|postgres|Txn Commits"));
    }
}
