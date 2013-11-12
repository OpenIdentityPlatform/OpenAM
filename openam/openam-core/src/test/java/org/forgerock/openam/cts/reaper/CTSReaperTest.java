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
import org.forgerock.openam.cts.CoreTokenConfig;
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.query.QueryBuilder;
import org.forgerock.openam.cts.impl.query.QueryFactory;
import org.forgerock.openam.cts.impl.query.QueryFilter;
import org.forgerock.openam.cts.utils.LDAPDataConversion;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.ResultHandler;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.BDDMockito.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

/**
 * @author robert.wapshott@forgerock.com
 */
public class CTSReaperTest {

    private QueryFactory mockQueryFactory;
    private CoreTokenConfig mockConfig;
    private CTSReaper reaper;
    private QueryBuilder mockBuilder;
    private TokenDeletion mockTokenDeletion;

    @BeforeMethod
    public void setUp() throws Exception {
        mockQueryFactory = mock(QueryFactory.class);
        mockBuilder = mock(QueryBuilder.class);
        given(mockQueryFactory.createInstance()).willReturn(mockBuilder);

        mockConfig = mock(CoreTokenConfig.class);
        mockTokenDeletion = mock(TokenDeletion.class);

        reaper = new CTSReaper(mockQueryFactory, mockConfig, mockTokenDeletion, mock(Debug.class));
    }

    @Test
    public void shouldStartup() {
        // Given
        ScheduledExecutorService mockService = mock(ScheduledExecutorService.class);

        // When
        reaper.startup(mockService);

        // Then
        verify(mockService).scheduleAtFixedRate(eq(reaper), anyLong(), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void shouldShutdown() {
        ScheduledExecutorService mockService = mock(ScheduledExecutorService.class);
        ScheduledFuture mockFuture = mock(ScheduledFuture.class);
        given(mockService.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class))).willReturn(mockFuture);
        reaper.startup(mockService);

        // When
        reaper.shutdown();

        // Then
        verify(mockFuture).cancel(anyBoolean());
    }

    @Test (timeOut = 5000)
    public void shouldDeleteResultsOfQuery() throws CoreTokenException, ErrorResultException {
        // Given
        String one = "one";
        String two = "two";
        String thr = "three";

        // Create some search results
        Collection<Entry> results = Arrays.asList(
                generateEntry(one),generateEntry(two),generateEntry(thr));

        // Enough mocking to get past QueryPageIterator
        given(mockBuilder.executeRawResults()).willReturn(results);
        given(mockBuilder.getPagingCookie()).willReturn(QueryBuilder.getEmptyPagingCookie());
        given(mockBuilder.withFilter(any(Filter.class))).willReturn(mockBuilder);
        given(mockBuilder.returnTheseAttributes(any(CoreTokenField.class))).willReturn(mockBuilder);

        QueryFilter queryFilter = new QueryFilter(new LDAPDataConversion());
        given(mockQueryFactory.createFilter()).willReturn(queryFilter);

        // Setup the simulated behaviour for the TokenDeletion to trigger the count down latch.
        willAnswer(new Answer() {
            public Object answer(InvocationOnMock invo) throws Throwable {
                Collection entries = (Collection) invo.getArguments()[0];
                ResultHandler handler = (ResultHandler) invo.getArguments()[1];
                for (int ii = 0; ii < entries.size(); ii++) {
                    handler.handleResult(null);
                }
                return null;
            }
        }).given(mockTokenDeletion).deleteBatch(any(Collection.class), any(ResultHandler.class));

        // When
        reaper.run();

        // Then
        verify(mockTokenDeletion).deleteBatch(any(Collection.class), any(ResultHandler.class));
    }

    @Test
    public void shouldCloseTokenDeletionWhenComplete() throws CoreTokenException {
        // Given
        given(mockBuilder.executeRawResults()).willReturn(Collections.EMPTY_LIST);
        given(mockBuilder.getPagingCookie()).willReturn(QueryBuilder.getEmptyPagingCookie());
        given(mockBuilder.withFilter(any(Filter.class))).willReturn(mockBuilder);
        given(mockBuilder.returnTheseAttributes(any(CoreTokenField.class))).willReturn(mockBuilder);

        QueryFilter queryFilter = new QueryFilter(new LDAPDataConversion());
        given(mockQueryFactory.createFilter()).willReturn(queryFilter);

        // When
        reaper.run();

        // Then
        verify(mockTokenDeletion).close();
    }

    private static Entry generateEntry(String id) {
        Attribute attribute = mock(Attribute.class);
        given(attribute.firstValueAsString()).willReturn(id);

        Entry entry = mock(Entry.class);
        given(entry.getAttribute(anyString())).willReturn(attribute);

        return entry;
    }
}
