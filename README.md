# AppDynamics PostgreSQL Database - Monitoring Extension

##Use Case
PostgreSQL is an open source object-relational database system.

The PostgreSQL monitoring extension captures metrics from a PostgreSQL database and displays them in the AppDynamics Metric Browser. 

Metrics include:
* Transaction commits and rollbacks
* Blocks hit and read
* Tuples fetched and returned
* Tuples inserted/updated/deleted

##Installation

**Note**: Configure the AppDynamics Machine Agent prior to installing this monitoring extension.

1. Run ant package. Deploy the PostgreSQLMonitor.zip file found in 'dist' into the \<machine agent home\>/monitors directory.

```
$ cd <machine agent home>/monitors/

$ unzip PostgreSQLMonitor.zip

```

2. Edit the monitor.xml and update:

	a. Change \<execution-frequency-in-seconds\> if the default value of 60 seconds is not required. This defines how often the monitor should
execute and collect metrics.  

	b.  Change \<execution-timeout-in-secs\> if the default value of 60 seconds is not required. This defines how long the application should
wait before timing out.  

	c. Change the default-value of "host" under \<monitor-run-task\>\<task-arguments\> if PostgreSQL is not at
"localhost".  

	d. Change the default-value of "port" under \<monitor-run-task\>\<task-arguments\> if PostgreSQL is not at port 5432.  
	
	e. Change the default-value of the username under \<monitor-run-task\>\<task-arguments\> if the default user is not "postgres".  
	
	f. Change the default-value of the password under <monitor-run-task\>\<task-arguments\> if the default password is not "welcome".  
	
	g. (OPTIONAL) Change the default-value of the tier under \<monitor-run-task\>\<task-arguments\> if you want this metric to appear under a specific tier. Otherwise the metrics will be registered in every tier. 
	
	h. Change the default-value of "columns" under \<monitor-run-task\>\<task-arguments\> if all the metrics specified above are not required.  
	
	i. Change the refresh-intervale under \<monitor-run-task\>\<task-arguments\> if the default value of 300 seconds is not required. This determines the duration of time before querying for new data from the database.

3. Restart the Machine Agent
4. Look for the metrics in the AppDynamics Metric Browser.  


##Metrics

| Variable | Description |
| --- | --- |
| numbackends | Number of Backends |
| xact\_commit | Transaction Commits |
| xact\_rollback | Transaction Rollback |
| blks\_read | Blocks Read |
| blks\_hit | Blocks Hit |
| tup\_returned | Tuples Returned |
| tup\_fetched | Tuples Fetched |
| tup\_inserted | Tuples Inserted |
| tup\_updated | Tuples Updated |
| tup\_deleted | Tuples Deleted |


  


##Contributing

Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/Appdynamics/postgresql-monitoring-extension).

##Community

Find out more in the [AppSphere](http://appsphere.appdynamics.com/t5/Extensions/PostgresSQL-Database-Monitoring-Extension/idi-p/837) community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:ace-request@appdynamics.com).
