# AppDynamics PostgreSQL Database - Monitoring Extension

This extension works only with the standalone machine agent.

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

0. Run mvn clean install. Extract the PostgreSQLMonitor.zip file found in 'target' into the \<machine agent home\>/monitors directory.

```
$ cd <machine agent home>/monitors/

$ unzip PostgreSQLMonitor.zip
```
2. Edit the config.yml and provide details of postgres database to connect to and fetch metrics
3. Edit metrics.xml and update the details
4. Restart the Machine Agent
5. Look for the metrics in the AppDynamics Metric Browser.  


## Sample config.yml

```
#Define postgress server cofiguration
pgServers:
   - displayName: "Local Postgres"
     host: "localhost"
     port: 5432
     user: "postgres"

     #Provide password or encryptedPassword and encryptionKey. See the documentation to find about password encryption.
     password:
     encryptedPassword: "/UNgClxtOb55gAln9NAzrA=="
     encryptionKey: "welcome"
     #The database that the PgStat Activity metrics should be collected from
     targetDatabase: "test"

# number of concurrent tasks
numberOfThreads: 1

taskSchedule:
    numberOfThreads: 1
    taskDelaySeconds: 300

metricPrefix: "Custom Metrics|Postgres Server|"

```

##Sample metrics.xml

```
<stats>
    <!-- name should match the displayName of pgServers in config.yml -->
    <stat name="Local Postgres" metric-type="OBS.CUR.COL">
        <metric columnName="numbackends" label="Number of backends"/>
        <metric columnName="xact_commit" label="Committed Transactions"/>
        <metric columnName="xact_rollback" label="Rolled Back Transactions"/>
        <metric columnName="blks_read" label="Disk Blocks Read"/>
        <metric columnName="blks_hit" label="Disk Blocks Hit"/>
        <metric columnName="tup_returned" label="Rows Returned"/>
        <metric columnName="tup_fetched" label="Rows Fetched"/>
        <metric columnName="tup_inserted" label="Rows Inserted"/>
        <metric columnName="tup_updated" label="Rows Updated"/>
        <metric columnName="tup_deleted" label="Rows Deleted"/>
    </stat>
</stats>
```

Note: For each database defined in pgServers of config.yml, you have to define one \<stat\> element in metrics.xml


##Credentials Encryption

To supply encrypted password in config.yml, follow the steps below:

1. Download the util jar to encrypt the password from [here](https://github.com/Appdynamics/maven-repo/blob/master/releases/com/appdynamics/appd-exts-commons/1.1.2/appd-exts-commons-1.1.2.jar).
2. Run command:

	~~~   
	java -cp appd-exts-commons-1.1.2.jar com.appdynamics.extensions.crypto.Encryptor EncryptionKey CredentialToEncrypt
	
	For example: 
	java -cp "appd-exts-commons-1.1.2.jar" com.appdynamics.extensions.crypto.Encryptor test myPassword

	~~~
3. In the config.yaml, provide the EncryptionKey used in encryptionKey, as well as the resulting encrypted password in encryptedPassword.


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

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:help@appdynamics.com).
