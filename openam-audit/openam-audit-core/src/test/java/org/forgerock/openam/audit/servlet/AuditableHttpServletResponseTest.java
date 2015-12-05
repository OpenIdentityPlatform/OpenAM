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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.audit.servlet;

import static javax.servlet.http.HttpServletResponse.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletResponse;

/**
 * @since 13.0.0
 */
public class AuditableHttpServletResponseTest {

    private HttpServletResponse delegate;
    private AuditableHttpServletResponse auditableHttpServletResponse;

    @BeforeMethod
    protected void setUp() {
        delegate = mock(HttpServletResponse.class);
        auditableHttpServletResponse = new AuditableHttpServletResponse(delegate);
    }

    @Test
    public void capturesStatusCodeWhenSendErrorCalled() throws Exception {
        // Given setUp()
        // When
        auditableHttpServletResponse.sendError(SC_BAD_REQUEST);
        // Then
        verify(delegate, times(1)).sendError(SC_BAD_REQUEST);
        assertThat(auditableHttpServletResponse.getStatusCode()).isEqualTo(SC_BAD_REQUEST);
        assertThat(auditableHttpServletResponse.getMessage()).isEqualTo("");
    }

    @Test
    public void capturesStatusCodeAndMessageWhenSendErrorCalled() throws Exception {
        // Given setUp()
        // When
        auditableHttpServletResponse.sendError(SC_BAD_REQUEST, "message");
        // Then
        verify(delegate, times(1)).sendError(SC_BAD_REQUEST, "message");
        assertThat(auditableHttpServletResponse.getStatusCode()).isEqualTo(SC_BAD_REQUEST);
        assertThat(auditableHttpServletResponse.getMessage()).isEqualTo("message");
    }

    @Test
    public void capturesStatusCodeWhenSendRedirectCalled() throws Exception {
        // Given setUp()
        // When
        auditableHttpServletResponse.sendRedirect("http://new.location.com/path");
        // Then
        verify(delegate, times(1)).sendRedirect("http://new.location.com/path");
        assertThat(auditableHttpServletResponse.getStatusCode()).isEqualTo(SC_MOVED_TEMPORARILY);
        assertThat(auditableHttpServletResponse.getMessage()).isEqualTo("");
    }

    @Test
    public void capturesStatusCodeWhenSetStatusCalled() throws Exception {
        // Given setUp()
        // When
        auditableHttpServletResponse.setStatus(SC_BAD_REQUEST);
        // Then
        verify(delegate, times(1)).setStatus(SC_BAD_REQUEST);
        assertThat(auditableHttpServletResponse.getStatusCode()).isEqualTo(SC_BAD_REQUEST);
        assertThat(auditableHttpServletResponse.getMessage()).isEqualTo("");
    }

    @Test
    public void capturesStatusCodeAndMessageWhenSetStatusCalled() throws Exception {
        // Given setUp()
        // When
        auditableHttpServletResponse.setStatus(SC_BAD_REQUEST, "message");
        // Then
        verify(delegate, times(1)).setStatus(SC_BAD_REQUEST, "message");
        assertThat(auditableHttpServletResponse.getStatusCode()).isEqualTo(SC_BAD_REQUEST);
        assertThat(auditableHttpServletResponse.getMessage()).isEqualTo("message");
    }

}
