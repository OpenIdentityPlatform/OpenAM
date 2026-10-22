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
 * Portions Copyrighted 2025-2026 3A Systems LLC.
 */

package org.forgerock.openam.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.forgerock.guice.core.InjectorConfiguration;
import org.forgerock.http.header.TransactionIdHeader;
import org.forgerock.openam.audit.context.AuditContextFilter;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.openam.audit.context.TransactionIdConfiguration;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.google.inject.AbstractModule;

/**
 * @since 13.0.0
 */
public class AuditContextFilterTest {

    @Mock private HttpServletRequest httpServletRequest;
    @Mock private HttpServletResponse httpServletResponse;
    @Mock private FilterChain filterChain;

    /**
     * {@link AuditContextFilter} asks {@link org.forgerock.guice.core.InjectorHolder} for a
     * {@link TransactionIdConfiguration}, which no module the default loader finds on this classpath binds. This class
     * is not a {@code GuiceTestCase}, so no per-test injector is registered: the binding from {@link TestGuiceModule}
     * has to be in the holder's own injector, the first injector of the JVM. The holder builds that injector from the
     * module loader in place at that moment and never consults the loader again, so ask for {@link TestGuiceModule}
     * before any test of the suite runs: a {@code @BeforeClass} comes too late whenever surefire happens to run another
     * class that touches the holder ahead of this one.
     *
     * Two limits remain. TestNG creates the instances of all test classes before any {@code @BeforeSuite} method runs,
     * so a constructor or field initializer that reaches the holder still goes first. And the loader is set for the
     * whole JVM: every other class in it that reaches the holder sees this binding, and a second class that sets a
     * different loader from its own {@code @BeforeSuite} would make the winner depend on class order again.
     */
    @BeforeSuite(alwaysRun = true)
    public void setupGuiceModuleLoader() {
        InjectorConfiguration.setGuiceModuleLoader(moduleAnnotation -> Collections.singleton(TestGuiceModule.class));
    }

    @BeforeMethod
    public void setupTests() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * The binding from {@link TestGuiceModule} serves every class in the JVM, so put the flag the tests change back to
     * its default.
     */
    @AfterMethod(alwaysRun = true)
    public void resetTrustHeader() {
        TransactionIdConfigurationImpl.trustHeader = true;
    }

    @Test
    public void shouldSetTransactionIdFromHttpHeaderAndClearRequestContextWhenFinished() throws Exception {
        // Given
        TransactionIdConfigurationImpl.trustHeader = true;
        final String txIdHttpHeader = "txId-http-header";
        final AuditContextFilter auditContextFilter = new AuditContextFilter();
        final TransactionIdCollector transactionIdCollector = new TransactionIdCollector();

        when(httpServletRequest.getHeader(TransactionIdHeader.NAME)).thenReturn(txIdHttpHeader);
        doAnswer(transactionIdCollector).when(filterChain).doFilter(httpServletRequest, httpServletResponse);

        // When
        auditContextFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        // Then
        // RequestContext TransactionId value is set to HTTP header while handling all subsequent filters and servlets
        assertThat(transactionIdCollector.transactionIdValue).isEqualTo(txIdHttpHeader);
        // But, RequestContext is cleared before exiting this filter
        assertThat(AuditRequestContext.getTransactionIdValue()).isNotEqualTo(txIdHttpHeader);
    }

    @Test
    public void shouldIgnoreTransactionIdHttpHeaderIfConfiguredToDoSo() throws Exception {
        // Given
        TransactionIdConfigurationImpl.trustHeader = false;
        final String txIdHttpHeader = "txId-http-header";
        final AuditContextFilter auditContextFilter = new AuditContextFilter();
        final TransactionIdCollector transactionIdCollector = new TransactionIdCollector();

        when(httpServletRequest.getHeader(TransactionIdHeader.NAME)).thenReturn(txIdHttpHeader);
        doAnswer(transactionIdCollector).when(filterChain).doFilter(httpServletRequest, httpServletResponse);

        // When
        auditContextFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        // Then
        // RequestContext TransactionId value is set to HTTP header while handling all subsequent filters and servlets
        assertThat(transactionIdCollector.transactionIdValue).isNotEqualTo(txIdHttpHeader);
        // But, RequestContext is still cleared before exiting this filter
        assertThat(AuditRequestContext.getTransactionIdValue()).isNotEqualTo(transactionIdCollector.transactionIdValue);
    }

    private static final class TransactionIdCollector implements Answer<Object> {

        String transactionIdValue = null;

        public Object answer(InvocationOnMock invocation) {
            transactionIdValue = AuditRequestContext.getTransactionIdValue();
            return null;
        }
    }

    public static final class TransactionIdConfigurationImpl implements TransactionIdConfiguration {

        static boolean trustHeader = true;

        @Override
        public boolean trustHttpTransactionHeader() {
            return trustHeader;
        }
    }

    public static class TestGuiceModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(TransactionIdConfiguration.class).to(TransactionIdConfigurationImpl.class);
        }
    }

}
