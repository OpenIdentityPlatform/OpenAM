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
 * Copyright 2013-2014 ForgeRock AS.
 */
package org.forgerock.openam.cts.impl.query;

import org.forgerock.openam.cts.exceptions.QueryFailedException;
import org.forgerock.openam.cts.impl.LDAPConfig;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.mockito.BDDMockito.*;

public class LDAPSearchHandlerTest {

    protected LDAPConfig mockConfig;
    protected Connection mockConnection;
    protected SearchRequest mockRequest;
    protected LDAPSearchHandler handler;

    @BeforeMethod
    public void setUp() throws Exception {
        mockConfig = mock(LDAPConfig.class);
        mockConnection = mock(Connection.class);
        mockRequest = mock(SearchRequest.class);

        handler = new LDAPSearchHandler(mockConfig);
    }

    @Test
    public void shouldUseConnectionForSearch() throws QueryFailedException, ErrorResultException {
        handler.performSearch(mockConnection, mockRequest, Collections.<Entry>emptyList());
        verify(mockConnection).search(eq(mockRequest), anyCollection());
    }

    @Test (expectedExceptions = QueryFailedException.class)
    public void shouldThrowExceptionOnFailure() throws QueryFailedException, ErrorResultException {
        ErrorResultException error = ErrorResultException.newErrorResult(ResultCode.NO_SUCH_OBJECT);
        given(mockConnection.search(any(SearchRequest.class), anyCollection())).willThrow(error);
        handler.performSearch(mockConnection, mockRequest, Collections.<Entry>emptyList());
    }
}
