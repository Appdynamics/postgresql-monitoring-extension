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

import com.google.common.base.Strings;

import java.util.Map;

import static com.appdynamics.extensions.postgres.util.Constants.*;

/**
 * @author pradeep.nair
 */
public class PostgresConnectionConfigHelper {

    public static PostgresConnectionConfig getConnectionConfig(String dbName, String serverName, final String password,
                                                               Map<String, ?> server) throws ConnectionConfigException {
        final String host = (String) server.get(HOST);
        final String port = (String) server.get(PORT);
        final String user = (String) server.get(USER);
        boolean err = false;
        String msg = null;
        if (Strings.isNullOrEmpty(host)) {
            err = true;
            msg = "Please provide a host to connect in config.yml for server " + serverName;
        } else if (Strings.isNullOrEmpty(port)) {
            err = true;
            msg = "Please provide a port to connect in config.yml for server " + serverName;
        } else if (Strings.isNullOrEmpty(user)) {
            err = true;
            msg = "Please provide a user to connect in config.yml for server " + serverName;
        }
        if (err) {
            throw new ConnectionConfigException(msg);
        }
        final Boolean useIpv6 = Boolean.valueOf((String) server.get(USE_IPV6));
        final String applicationName = Strings.isNullOrEmpty((String) server.get(APPLICATION_NAME)) ?
                DEFAULT_APPLICATION_NAME : (String) server.get(APPLICATION_NAME);
        Map<String, String> optionalConnProps = (Map<String, String>) server.get(OPTIONAL_CONNECTION_PROPERTIES);
        return new PostgresConnectionConfig.Builder().host(host).useIPv6(useIpv6).port(port).database(dbName).user
                (user).password(password).applicationName(applicationName).readOnly(Boolean.TRUE).properties
                (optionalConnProps).build();
    }
}
