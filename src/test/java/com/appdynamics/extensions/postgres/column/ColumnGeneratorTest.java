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

package com.appdynamics.extensions.postgres.column;

import com.appdynamics.extensions.yml.YmlReader;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

import static com.appdynamics.extensions.postgres.util.Constants.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * @author pradeep.nair
 */
public class ColumnGeneratorTest {

    @Test
    public void getColumnsPOJOTest() {
        Map<String, ?> conf = YmlReader.readFromFileAsMap(new File("src/test/resources/conf/columns.yml"));
        List<Map<String, ?>> columns = (List<Map<String, ?>>) conf.get(COLUMNS);
        List<Column> cols = ColumnGenerator.getColumnsPOJO(columns);
        assertThat(cols.size(), is(5));
        Column col1 = cols.get(0);
        assertThat(col1.getName(), equalTo("datname"));
        assertThat(col1.getType().toLowerCase(), equalTo("metricpath"));
        assertThat(col1.getProperties(), is(nullValue()));
        Column col2 = cols.get(1);
        assertThat(col2.getName(), equalTo("numbackends"));
        assertThat(col2.getType().toLowerCase(), equalTo("metricvalue"));
        assertThat(col2.getProperties(), is(notNullValue()));
        Map<String, ?> properties = col2.getProperties();
        assertThat(properties.size(), is(4));
        assertThat(properties.get(ALIAS), equalTo("Number of connections"));
        assertThat(properties.get(AGGREGATION_TYPE), equalTo("OBSERVATION"));
        assertThat(properties.get(TIME_ROLLUP_TYPE), equalTo("AVERAGE"));
        assertThat(properties.get(CLUSTER_ROLLUP_TYPE), equalTo("INDIVIDUAL"));
    }
}
