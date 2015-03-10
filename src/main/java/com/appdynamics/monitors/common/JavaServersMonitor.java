package com.appdynamics.monitors.common;

import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public abstract class JavaServersMonitor extends AManagedMonitor {
    protected final Map<String, Map<String, String>> valueMap = new ConcurrentSkipListMap<String, Map<String, String>>(String.CASE_INSENSITIVE_ORDER);
    protected final Map<String, Map<String, String>> oldValueMap = new ConcurrentSkipListMap<String, Map<String, String>>(String.CASE_INSENSITIVE_ORDER);
    protected final Logger logger = Logger.getLogger(this.getClass().getName());

    protected volatile String host;
    protected volatile String port;
    protected volatile String userName;
    protected volatile String passwd;

    protected volatile long oldTime = 0;
    protected volatile long currentTime = 0;

    public abstract TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext taskContext)
            throws TaskExecutionException;

    protected void parseArgs(Map<String, String> args) {
        host = getArg(args, "host", "localhost");
        userName = getArg(args, "user", userName);
        passwd = getArg(args, "password", passwd);
        port = getArg(args, "port", "90");
    }

    // safe way to get parameter from monitor, but if null, use default
    protected String getArg(Map<String, String> args, String arg, String oldVal) {
        String result = args.get(arg);

        if (result == null)
            return oldVal;

        return result;
    }


    protected void startExecute(Map<String, String> taskArguments) {
        valueMap.clear();
        parseArgs(taskArguments);
    }

    protected TaskOutput finishExecute() {
        for (Map.Entry<String, Map<String, String>> entry:valueMap.entrySet()){
            oldValueMap.put(entry.getKey(), new ConcurrentSkipListMap<String, String>(entry.getValue()));
        }
        oldTime = currentTime;

        // just for debug output
        logger.debug("Finished METRIC COLLECTION for Monitor.......");

        return new TaskOutput("Success");
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

    protected String getString(float num) {
        int result = Math.round(num);
        return Integer.toString(result);
    }


}
