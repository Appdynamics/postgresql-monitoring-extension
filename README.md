# AppDynamics PostgreSQL Database - Monitoring Extension
## Use Case
PostgreSQL is an open source object-relational database system.

The PostgreSQL monitoring extension captures metrics from a PostgreSQL database and displays them in the AppDynamics Metric Browser.
## Prerequisites
Before the extension is installed, the prerequisites mentioned [here](https://community.appdynamics.com/t5/Knowledge-Base/Extensions-Prerequisites-Guide/ta-p/35213) need to be met. Please do not proceed with the extension installation if the specified prerequisites are not met.
## Installation
1.  Unzip the contents of "PostgreSQLMonitor.zip" as "PostgreSQLMonitor" and copy the "PostgreSQLMonitor" directory to `<MachineAgentHome>/monitors/`
2. Configure the extension by referring to the below section.
3. Configure the path to the config.yaml file by editing the task-argments in the monitor.xml file.
    ```
        <task-arguments>
            <argument name="config-file" is-required="true" default-value="monitors/PostgreSQLMonitor/config.yml" />
        </task-arguments>
    ```
4. Restart the machine agent.
## Configuration
Note : Please make sure not to use tab (\t) while editing yaml files. You can validate the yaml file using a [yaml validator](http://yamllint.com)

Configure the extension by editing the config.yml file in `<MachineAgentHome>/monitors/PostgreSQLMonitor/`. The metricPrefix of the extension has to be configured as specified [here](https://community.appdynamics.com/t5/Knowledge-Base/How-do-I-troubleshoot-missing-custom-metrics-or-extensions/ta-p/28695#Configuring%20an%20Extension). Please make sure that the right metricPrefix is chosen based on your machine agent deployment, otherwise this could lead to metrics not being visible in the controller.
### Configuring the servers and database
1. Configure the PostgreSQL clusters/servers properties by specifying the displayName(required), host(required), port(required), user(required), password (only if authentication enabled), encryptedPassword(only if password encryption required) under `servers`. Also specify the databases that have to be monitored. You can specify multiple servers in the same config.yml file.
    ```
    servers:
      - displayName: "Local cluster"
        host: "127.0.0.1"
        useIpv6: "false"
        port: "5432"
        user: ""
        password: ""
        encryptedPassword: ""
    #    optionalConnectionProperties:
    #      connectTimeout: 100
    #      tcpKeepAlive: true
        databases:
          ...
      - displayName: "Local cluster"
        host: "::1"
        useIpv6: "true"
        port: "5432"
        user: ""
        password: ""
        encryptedPassword: ""
        databases:
          ...
    ``` 
    When using ipv6 address set `useIpv6: "false"`. Additional connection properties can be set using `optionalConnectionProperties`, you can refer [here](https://jdbc.postgresql.org/documentation/head/connect.html) for all connection parameters.
2. Configure the databases under each server, atleast on database is required under one server to configure some queries and fetch metrics. Configure the database by providing dbName and configuring queries as explained in the next section. You can configure multiple databases.
    ```
    databases:
      - dbName: "test"
        queries:
          ...
    ```
### Configuring queries
Only queries that start with SELECT are allowed.
The extension supports getting values from multiple columns at once but it can only pull the metrics from the latest value from the row returned.

The name of the metric displayed on the Metric Browser will be the "name" value that is specified in columns.

__queries__ : You can add multiple queries under this field, each query configured will consist of the following 
1. __name__ : The name you would like to give to the metrics produced by this query.
2. __serverLvlQuery__ : Set this to true only if the query returns stats for the databases under the current server
3. __queryStmt__ : This will be your SQL Query that will be used to query the database.
4. __columns__ : Under this field you will have to list all the columns that you are trying to get values from.
    * __name__ : The name of the column you would like to see on the metric browser.
    * __type__ : This value will define if the value returned from the column will be used for the metric path or if it is going to be the value of the metric.
       * __metricPath__ : If you select this, this value will be added to the metric path for the metric.
       * __metricValue__ : If you select this, then the value returned will become your metric value that will correspond to the name you specified above.
       
Example, Consider the below query for server `Local Cluster`
```
databases:
  - dbName: "test"
    queries: 
    # Add where clauses to query to filter databases
      - name: "Database Stats"
        serverLvlQuery: "true"
        queryStmt: "SELECT datname, numbackends
                    FROM pg_stat_database"
        # the columns are the metrics to be extracted
        columns:
          - name: "datname"
            type: "metricPath"
          - name: "numbackends"
            type: "metricValue"
            properties:
              alias: "Number of connections"
              aggregationType: "OBSERVATION"
              timeRollUpType: "AVERAGE"
              clusterRollUpType: "INDIVIDUAL"
```
The above query will return 1 metric, with metric path -
`Custom Metrics|Local Cluster|Database Stats|<datname>|Number of connections`. Since the above query has `serverLvlQuery: true` __dbName__ won't be a part of the metric path.

Consider the below query for server `Local Cluster`
```
databases:
  - dbName: "test"
    queries:
      - name: "Table Stats"
        serverLvlQuery: "false"
        # add where clause to the query to filter tables
        queryStmt: "SELECT relname, seq_scan, seq_tup_read
                    FROM pg_stat_user_tables where relname = 'myTable'"
        columns:
          - name: "relname"
            type: "metricPath"
          - name: "seq_scan"
            type: "metricValue"
            properties:
              alias: "Sequential Scans"
              delta: "true"
              aggregationType: "OBSERVATION"
              timeRollUpType: "AVERAGE"
              clusterRollUpType: "INDIVIDUAL"
          - name: "seq_tup_read"
            type: "metricValue"
            properties:
              alias: "Tuples fetched by Sequential Scans"
              delta: "true"
              aggregationType: "OBSERVATION"
              timeRollUpType: "AVERAGE"
              clusterRollUpType: "INDIVIDUAL"
```
Assume that this query returns -

|relname|seq_scan|seq_tup_read|
|---|---|---|
|myTable|10|200|

The above query will return 2 metrics-
```
Custom Metrics|Local Cluster|test|Table Stats|relname|Sequential Scans = 10
Custom Metrics|Local Cluster|test|Table Stats|relname|Tuples fetched by Sequential Scans = 200
```
### numberOfThreads
Use the following formula for calculating `numberOfThreads`
```
numberOfThreads = for each server (1 + number_of(databases)). For example if you have 1 server and 2 databases then numberOfThreads = 1 + 2 = 3
```
### metricPathReplacements
Please visit [this](https://community.appdynamics.com/t5/Knowledge-Base/Metric-Path-CharSequence-Replacements-in-Extensions/ta-p/35412) page to get detailed instructions on configuring Metric Path Character sequence replacements in Extensions.
### customDashboard
Please visit [this](https://community.appdynamics.com/t5/Knowledge-Base/Uploading-Dashboards-Automatically-with-AppDynamics-Extensions/ta-p/35408) page to get detailed instructions on automatic dashboard upload with extension.
### enableHealthChecks
Please visit [here](https://community.appdynamics.com/t5/Knowledge-Base/Extension-HealthChecks/ta-p/35409) page to get detailed instructions on 
## Credentials Encryption
Please visit [this](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-Password-Encryption-with-Extensions/ta-p/29397) page to get detailed instructions on password encryption. The steps in this document will guide you through the whole process.
## Extensions Workbench
Workbench is an inbuilt feature provided with each extension in order to assist you to fine tune the extension setup before you actually deploy it on the controller. Please review the following [document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-the-Extensions-WorkBench/ta-p/30130) for how to use the Extensions WorkBench
## Troubleshooting
Please follow the steps listed in the [troubleshooting document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) in order to troubleshoot your issue. These are a set of common issues that customers might have faced during the installation of the extension.

## Contributing
Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/Appdynamics/postgresql-monitoring-extension).
## Version
|Name|Version|
|---|---|
|Extension Version|3.0.2|
|Controller Compatibility|4.5 or Later|
|Agent Compatibility|4.5.13 or Later|
|Postgres Version Support|9.4 or later|
|Last Update|10/08/2021|
|Changes list|[ChangeLog](https://github.com/Appdynamics/postgresql-monitoring-extension/blob/master/CHANGES.md)|
