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
import java.util.Properties;

import static com.appdynamics.extensions.postgres.util.Constants.*;

/**
 * @author pradeep.nair
 */
public class PostgresConnectionConfig {

    private final String url;
    private final Properties props;

    private PostgresConnectionConfig(String url, Properties props) {
        this.url = url;
        this.props = props;
    }

    public String getUrl() {
        return url;
    }

    public Properties getProps() {
        return props;
    }

    private static PostgresConnectionConfig processBuilder(Builder builder) {
        String url = ConnectionUtils.buildURL(PROTOCOL, builder.host, builder.port, builder.database, builder.useIpv6);
        Properties props = new Properties();
        props.setProperty(USER, builder.user);
        if (!Strings.isNullOrEmpty(builder.password)) {
            props.setProperty(PASSWORD, builder.password);
        }
        props.setProperty(APPLICATION_NAME, builder.applicationName);
        props.setProperty("readOnly", builder.readOnly);
        if (builder.properties != null && builder.properties.size() != 0) {
            // TODO though using putAll is not recommend for Properties this should be fine imo, since the Map is
            //  bounded by String, String. lmk
            // todo this looks fine to me because none of the elements are nullable
            props.putAll(builder.properties);
        }
        return new PostgresConnectionConfig(url, props);
    }

    public static class Builder {
        private String host;
        private Boolean useIpv6;
        private String port;
        private String database;
        private String user;
        private String password;
        private String applicationName;
        private String readOnly;
        private Map<String, String> properties;

        Builder host(String host) {
            this.host = host;
            return this;
        }

        Builder useIPv6(Boolean useIpv6) {
            this.useIpv6 = useIpv6;
            return this;
        }

        Builder port(String port) {
            this.port = port;
            return this;
        }

        Builder database(String database) {
            this.database = database;
            return this;
        }

        Builder user(String user) {
            this.user = user;
            return this;
        }

        Builder password(String password) {
            this.password = password;
            return this;
        }

        Builder applicationName(String applicationName) {
            this.applicationName = applicationName;
            return this;
        }

        Builder readOnly(String readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        Builder properties(Map<String, String> properties) {
            this.properties = properties;
            return this;
        }

        PostgresConnectionConfig build() {
            return processBuilder(this);
        }
    }
}
