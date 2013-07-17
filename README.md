# AppDynamics PostgreSQL - Monitoring Extension

* [Use Case](postgre.md#use-case)
* [Installation](postgre.md#installation)
* [Metrics](postgre.md#metrics)
* [Contributing](nagios-readme.md#contributing)

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

1. In the \<machine agent home>/monitors directory create a directory for the PostgreSQL monitoring extension.  
2. Extract the postgresql.tar.gz to the new directory. 
3. Edit the monitor.xml and update:

	a. Change \<execution-frequency-in-seconds\> if the default value of 60 seconds is not required. This defines how often the monitor should
execute and collect metrics.  
	b.  Change \<execution-timeout-in-secs\> if the default value of 60 seconds is not required. This defines how long the application should
wait before timing out.  
	c. Change the default-value of "host" under \<monitor-run-task\>\<task-arguments\> if PostgreSQL is not at
"localhost".  
	d. Change the default-value of "port" under \<monitor-run-task\>\<task-arguments\> if PostgreSQL is not at port 5432.  
	e. Change the default-value of the username under \<monitor-run-task\>\<task-arguments\> if the default user is not "postgres".  
	f. Change the default-value of the password under <monitor-run-task\>\<task-arguments\> if the default password is not "welcome".  
	g. (OPTIONAL) Change the default-value of the tier under \<monitor-run-task\>\<task-arguments\> if you want this metric to
appear under a specific tier. Otherwise the metrics will be registered in every tier.  
	h. Change the default-value of "columns" under \<monitor-run-task\>\<task-arguments\> if all the metrics specified above are not required.  
	i. Change the refresh-intervale under \<monitor-run-task\>\<task-arguments\> if the default value of 300 seconds is not required. 
	This determines the duration of time before querying for new data from the database.

4. Restart the Machine Agent
5. Look for the metrics in the AppDynamics Metric Browser.  



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

Always feel free to fork and contribute any changes directly via GitHub.


##Support

For any support questions, please contact ace@appdynamics.com.
