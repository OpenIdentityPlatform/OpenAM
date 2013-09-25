/**
 * Copyright 2013 ForgeRock AS.
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
package org.forgerock.openam.cts.reaper;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.impl.LDAPAdapter;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.ResultHandler;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author robert.wapshott@forgerock.com
 */
public class TokenDeletionTest {

    private ConnectionFactory mockFactory;
    private LDAPAdapter mockAdapter;
    private TokenDeletion deletion;
    private Connection mockConnection;

    @BeforeMethod
    public void setUp() throws Exception {
        mockFactory = mock(ConnectionFactory.class);
        mockConnection = mock(Connection.class);
        given(mockFactory.getConnection()).willReturn(mockConnection);

        mockAdapter = mock(LDAPAdapter.class);

        deletion = new TokenDeletion(mockAdapter, mockFactory, mock(Debug.class));
    }

    @Test
    public void shouldCloseConnectionWhenComplete() throws ErrorResultException {
        // Given
        // When
        deletion.deleteBatch(Collections.EMPTY_LIST, mock(ResultHandler.class));
        // Then
        verify(mockConnection).close();
    }

    @Test
    public void shouldDeleteEntries() {
        // Given
        Collection<Entry> entries = Arrays.asList(
                generateEntry("one"),generateEntry("two"),generateEntry("three"));
        // When
        deletion.deleteBatch(entries, mock(ResultHandler.class));
        // Then
        verify(mockAdapter, times(3)).deleteAsync(
                eq(mockConnection),
                anyString(),
                any(ResultHandler.class));
    }

    private static Entry generateEntry(String id) {
        Attribute attribute = mock(Attribute.class);
        given(attribute.firstValueAsString()).willReturn(id);

        Entry entry = mock(Entry.class);
        given(entry.getAttribute(anyString())).willReturn(attribute);

        return entry;
    }
}
