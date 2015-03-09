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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import com.appdynamics.monitors.common.JavaServersMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import com.singularity.ee.util.clock.ClockUtils;

public class PostgreSQLMonitor extends JavaServersMonitor
{
	private volatile String tierName;
	private volatile String database; // the database we are interested in collecting metrics on
	private volatile int refreshIntervalInExecutionTicks;
	private final Map<String, String> cachedValueMap = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
	private volatile int currentNumExecutionTicks = -1;
	private volatile List<String> columnNames;
	private volatile List<String> cumulativeColumnNames;
    private static final String[] dbStatisticsColumns = new String[]{"numbackends", "xact_commit", "xact_rollback", "blks_read", "blks_hit", "tup_returned", "tup_fetched", "tup_inserted", "tup_updated", "tup_deleted"};
    private static final Map<String, String> columnDescriptions = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER){{
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

    private List<String> parseList(String valueString){
        final List<String> result;
        if (valueString == null || valueString.length() == 0)
        {
            result = Collections.emptyList();
        }
        else
        {
            result = Arrays.asList(valueString.split(","));
        }
        return result;
    }
	protected void parseArgs(Map<String, String> args)
	{
		super.parseArgs(args);
		tierName = getArg(args, "tier", null); // if the tier is not specified then create the metrics for all tiers
		database = getArg(args, "target-database", "postgres");
		
		// Assume all the columns we want values for are in a comma separated list
		columnNames = parseList(getArg(args, "columns", null));
		cumulativeColumnNames = parseList(getArg(args, "cumulative_columns", null));

        int refreshIntervalSecs = Integer.parseInt(getArg(args, "refresh-interval", "60"));
		
		if (refreshIntervalSecs <= 60)
		{
			refreshIntervalInExecutionTicks = 1;
		}
		else
		{
			// Convert refresh interval to milliseconds and round up to the nearest minute timeslice.
			// From that we can get the number of 60 second ticks before the next refresh.
			// We do this to prevent time drift issues from preventing this task from running.
			refreshIntervalInExecutionTicks = (int)(ClockUtils.roundUpTimestampToNextMinute(refreshIntervalSecs*1000)/60000);			
		}
		
		if (currentNumExecutionTicks == -1)
		{
			// This is the first time we've parsed the args. Assume we refresh the data 
			// the next time we execute the monitor.
			currentNumExecutionTicks = refreshIntervalInExecutionTicks;
		}
	}
	
	private Connection connect() throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException
	{
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
	protected Map<String, String> getValuesForColumns(List<String> columnNames, String query) throws Exception
	{
		Map<String, String> columnName2Value = new HashMap<String, String>();
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		boolean debug = logger.isDebugEnabled();
		
		try
		{
			conn = connect();
			stmt = conn.createStatement();
			
			if (debug)
			{
				logger.debug("Executing query ["+query+"]");
			}
			
			rs = stmt.executeQuery(query);
			
			// get most accurate time
			currentTime = System.currentTimeMillis();

			while (rs.next())
			{
				for (String columnName : columnNames) 
				{
					if (columnName != null)
					{
						String value = rs.getString(columnName);
						
						if (debug)
						{
							logger.debug("[column,value] = ["+columnName+","+value+"]");
						}
						
						columnName2Value.put(columnName, value);
					}
				}
			}
		}
		catch (Exception ex)
		{
			logger.error("Error while executing query ["+query+"] to capture values for columns: "+columnNames, ex);
			throw ex;
		}
		finally
		{
			close(rs, stmt, conn);
		}
		
		return Collections.synchronizedMap(columnName2Value);
	}

	public TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext taskContext)
			throws TaskExecutionException
	{
		startExecute(taskArguments, taskContext);

		try
		{
			if (++currentNumExecutionTicks >= refreshIntervalInExecutionTicks)
			{
				logger.debug("Querying fresh values for PostgreSQLMonitor ...");
				
				// Store the current values for the columns specified in the list
				valueMap.putAll(getValuesForColumns(columnNames, "select * from pg_stat_database where datname='" + database + "'"));
				
				// Update the cached values
				cachedValueMap.clear();
				cachedValueMap.putAll(valueMap);
								
				currentNumExecutionTicks = 0;
			}
			else
			{
				logger.debug("Using cached values for PostgreSQLMonitor ...");
				
				// Use the cached values
				valueMap.putAll(cachedValueMap);
			}	
		}
		catch (Exception ex)
		{
			throw new TaskExecutionException(ex);
		} 

		// just for debug output
		logger.debug("Starting METRIC COLLECTION for PostgreSQL Monitor.......");
	
		Set<String> uniqueColumnNames = new HashSet<String>(columnNames);

        final String dbMetricPrefix = getMetricPrefix();
        for(String dbColumnName: dbStatisticsColumns){
            printMetric(dbColumnName, dbMetricPrefix + getColumnDescription(dbColumnName), uniqueColumnNames);
        }
		return this.finishExecute();
	}

    private String getColumnDescription(String dbColumnName) {
        if(columnDescriptions.containsKey(dbColumnName)){
            return columnDescriptions.get(dbColumnName);
        }else{
            return dbColumnName;
        }
    }

    private void printMetric(String columnName, String metricLabel, Set<String> uniqueColumnNames)
	{
        final String currentValue = getString(columnName);

        if (uniqueColumnNames.contains(columnName))
		{
            String value;
            final boolean print;
            if(cumulativeColumnNames.contains(columnName)){
                if(oldValueMap.containsKey(columnName)) {
                    value = Long.toString(Long.parseLong(currentValue) - Long.parseLong(oldValueMap.get(columnName)));
                    print = true;
                }else{
                    print = false;
                    value = "-1";
                }
            }else{
                value = currentValue;
                print = true;
            }

            if(print){
                printMetric(metricLabel, value,
                        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                        MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
            }
		}
	}

	protected String getMetricPrefix()
	{
		if (tierName != null)
		{
			return "Server|Component:"+tierName+"|Postgres Server|"+database+"|";
		}
		else
		{	
			return "Custom Metrics|Postgres Server|"+database+"|";
		}
	}

    protected Connection getConnection(String connString) throws SQLException {
        return DriverManager.getConnection(connString);
    }
}
