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

package com.appdynamics.extensions.postgres.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @author pradeep.nair
 */
public class ConnectionUtils {

    public static String buildURL(String protocol, String host, String port, String database, boolean useIpv6) {
        if (useIpv6 && !host.matches("\\[.*]")) {
            host = "[" + host + "]";
        }
        return protocol + "//" + host + ":" + port + "/" + database;
    }

    public static Connection getConnection(String driver, String url, Properties props) throws SQLException,
            ClassNotFoundException {
        Class.forName(driver);
        return DriverManager.getConnection(url, props);
    }
}
