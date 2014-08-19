/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.openam.sm.datalayer.utils;

import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.StoreMode;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.text.MessageFormat;

import static org.fest.assertions.Assertions.assertThat;

public class ConnectionCountTest {

    private ConnectionCount count;

    @BeforeMethod
    public void setup() {
        count = new ConnectionCount(StoreMode.DEFAULT);
    }

    @Test
    public void shouldReturnASingleConnectionForReaper() {
        assertThat(count.getConnectionCount(10, ConnectionType.CTS_REAPER)).isEqualTo(1);
        assertThat(count.getConnectionCount(100, ConnectionType.CTS_REAPER)).isEqualTo(1);
    }

    @Test
    public void shouldReturnAPowerOfTwoForCTSAsync() {
        int max = 10;
        int result = count.getConnectionCount(max, ConnectionType.CTS_ASYNC);
        assertThat(ConnectionCount.findPowerOfTwo(result)).isLessThan(max);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectAMinimumCount() {
        count.getConnectionCount(3, ConnectionType.CTS_ASYNC);
    }

    @Test
    public void shouldReturnAPositiveValueForDataLayer() {
        for (int ii = ConnectionCount.MINIMUM_CONNECTIONS; ii < 1000; ii++) {
            assertThat(count.getConnectionCount(ii, ConnectionType.DATA_LAYER)).isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    public void shouldAddUpToTheMax() {
        int max = 10;
        int total = count.getConnectionCount(max, ConnectionType.CTS_ASYNC) +
                count.getConnectionCount(max, ConnectionType.CTS_REAPER) +
                count.getConnectionCount(max, ConnectionType.DATA_LAYER);
        assertThat(total).isEqualTo(max);
    }


    @Test
    public void shouldFindPowerOfTwo() {
        int value = 64;
        assertThat(ConnectionCount.findPowerOfTwo(value)).isEqualTo(value);
    }

    @Test
    public void shouldFindPowerOfTwoLessThanStartingValue() {
        int value = 20;
        assertThat(ConnectionCount.findPowerOfTwo(value)).isEqualTo(16);
    }

    public static void main(String... args) {
        ConnectionCount count = new ConnectionCount(StoreMode.DEFAULT);
        System.out.println("Total = Async:Reaper:Data");
        for (int ii = ConnectionCount.MINIMUM_CONNECTIONS; ii < 1000; ii++) {
            int a = count.getConnectionCount(ii, ConnectionType.CTS_ASYNC);
            int r = count.getConnectionCount(ii, ConnectionType.CTS_REAPER);
            int d = count.getConnectionCount(ii, ConnectionType.DATA_LAYER);
            System.out.println(MessageFormat.format("Total: {0} = {1}:{2}:{3}", ii, a, r, d));
        }
    }
}