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

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContext;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.metrics.MetricCharSequenceReplacer;
import com.appdynamics.extensions.postgres.connection.ConnectionUtils;
import com.appdynamics.extensions.postgres.connection.PostgresConnectionConfig;
import com.appdynamics.extensions.util.MetricPathUtils;
import com.appdynamics.extensions.yml.YmlReader;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.appdynamics.extensions.postgres.util.Constants.DATABASES;
import static com.appdynamics.extensions.postgres.util.Constants.SERVERS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * @author pradeep.nair
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest(ConnectionUtils.class)
public class DatabaseTaskTest {
    private PostgresConnectionConfig connectionConfig;
    private MetricWriteHelper metricWriteHelper;
    private String metricPrefix;
    private ArgumentCaptor<List> pathCaptor;
    private Map<String, ?> conf;
    private final Phaser phaser = new Phaser();

    @Before()
    public void setup() throws SQLException, ClassNotFoundException {
        conf = YmlReader.readFromFileAsMap(new File("src/test/resources/conf/config.yml"));
        ABaseMonitor baseMonitor = mock(ABaseMonitor.class);
        MonitorContextConfiguration monitorConfiguration = mock(MonitorContextConfiguration.class);
        MonitorContext context = mock(MonitorContext.class);
        when(baseMonitor.getContextConfiguration()).thenReturn(monitorConfiguration);
        when(monitorConfiguration.getContext()).thenReturn(context);
        MetricPathUtils.registerMetricCharSequenceReplacer(baseMonitor);
        MetricCharSequenceReplacer replacer = MetricCharSequenceReplacer.createInstance(conf);
        when(context.getMetricCharSequenceReplacer()).thenReturn(replacer);
        MetricWriter metricWriter = mock(MetricWriter.class);
        when(baseMonitor.getMetricWriter(anyString(), anyString(), anyString(), anyString())).thenReturn(metricWriter);
        pathCaptor = ArgumentCaptor.forClass(List.class);
        metricPrefix = "Custom Metrics|Postgres|";
        metricWriteHelper = mock(MetricWriteHelper.class);
        connectionConfig = mock(PostgresConnectionConfig.class);
        when(connectionConfig.getUrl()).thenReturn("");
        when(connectionConfig.getProps()).thenReturn(null);
        mockStatic(ConnectionUtils.class);
        Connection conn = mock(Connection.class);
        when(ConnectionUtils.getConnection(anyString(), anyString(), any())).thenReturn(conn);
        when(conn.isValid(anyInt())).thenReturn(true);
        Statement stmt = mock(Statement.class);
        when(conn.createStatement()).thenReturn(stmt);
        ResultSet rs = mock(ResultSet.class);
        when(stmt.executeQuery(anyString())).thenReturn(rs);
        when(rs.next()).thenReturn(Boolean.TRUE, Boolean.FALSE);
        when(rs.getString("datname")).thenReturn("Test DB");
        when(rs.getString("numbackends")).thenReturn("20");
        when(rs.getString("dbSize")).thenReturn("2048");
        phaser.register();
    }

    @Test
    public void databaseTaskTest() throws SQLException, ClassNotFoundException {
        Map<String, ?> dbtask =
                ((List<Map<String, ?>>) ((List<Map<String, ?>>) conf.get(SERVERS)).get(0).get(DATABASES)).get(0);
        DatabaseTask task = new DatabaseTask("Local", "Test DB", dbtask, phaser, connectionConfig, metricPrefix,
                metricWriteHelper, new AtomicBoolean());
        task.run();
        verify(metricWriteHelper).transformAndPrintMetrics(pathCaptor.capture());
        List<Metric> metrics = (List<Metric>) pathCaptor.getValue();
        metrics.sort(Comparator.comparing(Metric::getMetricName));
        assertThat(metrics.size(), is(3));
        assertThat(metrics.get(0).getMetricPath(), equalTo("Custom Metrics|Postgres|Local|HEART_BEAT"));
        assertThat(metrics.get(1).getMetricPath(), equalTo("Custom Metrics|Postgres|Local|Test DB|dbSize"));
        assertThat(metrics.get(1).getMetricValue(), is("2048"));
        assertThat(metrics.get(1).getMetricProperties().getAlias(), equalTo("Database Size (KB)"));
        assertThat(metrics.get(1).getMetricProperties().getMultiplier(), is(new BigDecimal(0.0009765625f)));
        assertThat(metrics.get(2).getMetricPath(), equalTo("Custom Metrics|Postgres|Local|Test DB|numbackends"));
        assertThat(metrics.get(2).getMetricValue(), is("20"));
        assertThat(metrics.get(2).getMetricProperties().getAlias(), equalTo("Number of connections"));
    }

    @After
    public void tearDown() {
        phaser.arriveAndAwaitAdvance();
    }
}
