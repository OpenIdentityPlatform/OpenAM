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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.audit;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import org.forgerock.openam.shared.audit.context.AuditRequestContext;
import org.forgerock.openam.shared.audit.context.TransactionId;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @since 13.0.0
 */
public class AuditContextFilterTest {

    @Mock private HttpServletRequest httpServletRequest;
    @Mock private HttpServletResponse httpServletResponse;
    @Mock private FilterChain filterChain;

    @BeforeMethod
    public void setupTests() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldSetTransactionIdFromHttpHeaderAndClearRequestContextWhenFinished() throws Exception {
        // Given
        final String txIdHttpHeader = "txId-http-header";
        final AuditContextFilter auditContextFilter = new AuditContextFilter();
        final TransactionIdCollector transactionIdCollector = new TransactionIdCollector();

        when(httpServletRequest.getHeader(TransactionId.HTTP_HEADER)).thenReturn(txIdHttpHeader);
        doAnswer(transactionIdCollector).when(filterChain).doFilter(httpServletRequest, httpServletResponse);

        // When
        auditContextFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        // Then
        // RequestContext TransactionId value is set to HTTP header while handling all subsequent filters and servlets
        assertThat(transactionIdCollector.transactionIdValue).isEqualTo(txIdHttpHeader);
        // But, RequestContext is cleared before exiting this filter
        assertThat(AuditRequestContext.getTransactionIdValue()).isNotEqualTo(txIdHttpHeader);
    }

    private static final class TransactionIdCollector implements Answer<Object> {

        String transactionIdValue = null;

        public Object answer(InvocationOnMock invocation) {
            transactionIdValue = AuditRequestContext.getTransactionIdValue();
            return null;
        }
    }
}