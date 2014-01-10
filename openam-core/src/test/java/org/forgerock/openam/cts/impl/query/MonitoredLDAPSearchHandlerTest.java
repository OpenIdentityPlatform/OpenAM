/*
 * Copyright 2014 ForgeRock, AS.
 *
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
 */

package org.forgerock.openam.cts.impl.query;

import org.forgerock.openam.cts.CTSOperation;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.monitoring.CTSOperationsMonitoringStore;
import org.forgerock.opendj.ldap.Entry;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Mockito.verify;


/**
 * Unit tests for the {@link MonitoredLDAPSearchHandler}.
 */
public class MonitoredLDAPSearchHandlerTest extends LDAPSearchHandlerTest {
    private CTSOperationsMonitoringStore monitoringStore;

    @Override
    protected LDAPSearchHandler getTestObject() {
        monitoringStore = mock(CTSOperationsMonitoringStore.class);
        return new MonitoredLDAPSearchHandler(mockFactory, mockConstants, monitoringStore);
    }

    @Test
    public void shouldRecordOperationForMonitoringOnSuccess() throws CoreTokenException {
        // Given

        // When
        handler.performSearch(mockRequest, Collections.<Entry>emptyList());

        // Then
        verify(monitoringStore).addTokenOperation(null, CTSOperation.LIST, true);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void shouldRecordOperationForMonitoringOnFailure() throws Exception {
        // Given
        given(mockFactory.getConnection()).willThrow(new RuntimeException());

        // When
        try {
            handler.performSearch(mockRequest, Collections.<Entry>emptyList());
        } finally {
            // Then
            verify(monitoringStore).addTokenOperation(null, CTSOperation.LIST, false);
        }
    }
}
