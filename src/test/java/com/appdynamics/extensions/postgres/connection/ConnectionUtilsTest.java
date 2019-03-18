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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.sql.DriverManager;

import static org.hamcrest.CoreMatchers.equalTo;
/**
 * @author pradeep.nair
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest(DriverManager.class)
public class ConnectionUtilsTest {

    @Test
    public void whenUseIpv6IsTrueHostShouldBeSurroundedByBrackets() {
        String url = ConnectionUtils.buildURL("shield", "marvel", "1234", "avengers", true);
        Assert.assertThat(url, equalTo("shield//[marvel]:1234/avengers"));
    }

    @Test
    public void whenUseIpv6IsFalseHostShouldNotBeSurroundedByBrackets() {
        String url = ConnectionUtils.buildURL("shield", "marvel", "1234", "avengers", false);
        Assert.assertThat(url, equalTo("shield//marvel:1234/avengers"));
    }
}
