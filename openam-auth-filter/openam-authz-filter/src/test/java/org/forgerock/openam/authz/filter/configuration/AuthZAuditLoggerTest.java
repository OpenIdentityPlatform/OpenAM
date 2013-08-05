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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.authz.filter.configuration;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenID;
import com.sun.identity.log.Level;
import com.sun.identity.log.LogRecord;
import com.sun.identity.log.Logger;
import org.forgerock.auth.common.AuditRecord;
import org.forgerock.auth.common.AuthResult;
import org.forgerock.openam.auth.shared.AuthnRequestUtils;
import org.forgerock.openam.auth.shared.LoggerFactory;
import org.forgerock.openam.auth.shared.SSOTokenFactory;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;

import java.security.Principal;

import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class AuthZAuditLoggerTest {

    private AuthZAuditLogger authZAuditLogger;

    private SSOTokenFactory ssoTokenFactory;
    private AuthnRequestUtils requestUtils;
    private LoggerFactory loggerFactory;

    private SSOToken adminToken;
    private Logger logger;

    @BeforeMethod
    public void setUp() {
        ssoTokenFactory = mock(SSOTokenFactory.class);
        requestUtils = mock(AuthnRequestUtils.class);
        loggerFactory = mock(LoggerFactory.class);

        authZAuditLogger = new AuthZAuditLogger(ssoTokenFactory, requestUtils, loggerFactory);

        adminToken = mock(SSOToken.class);
        given(ssoTokenFactory.getAdminToken()).willReturn(adminToken);
        logger = mock(Logger.class);
        given(loggerFactory.getLogger(anyString())).willReturn(logger);
    }

    @Test
    public void shouldAuditLogSuccessfulAuthorization() throws SSOException {

        //Given
        AuditRecord auditRecord = mock(AuditRecord.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        SSOToken subjectToken = mock(SSOToken.class);
        SSOTokenID subjectTokenId = mock(SSOTokenID.class);
        Principal principal = mock(Principal.class);

        given(auditRecord.getHttpServletRequest()).willReturn(request);
        given(auditRecord.getAuthResult()).willReturn(AuthResult.SUCCESS);
        given(request.getRequestURI()).willReturn("REQUEST_URI");
        given(requestUtils.getTokenId(request)).willReturn("TOKEN_ID");
        given(ssoTokenFactory.getTokenFromId("TOKEN_ID")).willReturn(subjectToken);
        given(subjectToken.getTokenID()).willReturn(subjectTokenId);
        given(subjectTokenId.toString()).willReturn("TOKEN_ID");
        given(subjectToken.getPrincipal()).willReturn(principal);
        given(principal.getName()).willReturn("PRINCIPAL_NAME");

        //When
        authZAuditLogger.audit(auditRecord);

        //Then
        verify(loggerFactory).getLogger("amAuthorization.access");
        ArgumentCaptor<LogRecord> logRecordCaptor = ArgumentCaptor.forClass(LogRecord.class);
        verify(logger).log(logRecordCaptor.capture(), eq(adminToken));
        LogRecord logRecord = logRecordCaptor.getValue();
        assertEquals(logRecord.getLevel(), Level.INFO);
        assertTrue(logRecord.getMessage().contains("Succeeded"));
        assertEquals(logRecord.getLogFor(), subjectToken);
        verify(logger).flush();
    }

    @Test
    public void shouldAuditLogUnsuccessfulAuthorization() throws SSOException {

        //Given
        AuditRecord auditRecord = mock(AuditRecord.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        SSOToken subjectToken = mock(SSOToken.class);
        SSOTokenID subjectTokenId = mock(SSOTokenID.class);
        Principal principal = mock(Principal.class);

        given(auditRecord.getHttpServletRequest()).willReturn(request);
        given(auditRecord.getAuthResult()).willReturn(AuthResult.FAILURE);
        given(request.getRequestURI()).willReturn("REQUEST_URI");
        given(requestUtils.getTokenId(request)).willReturn("TOKEN_ID");
        given(ssoTokenFactory.getTokenFromId("TOKEN_ID")).willReturn(subjectToken);
        given(subjectToken.getTokenID()).willReturn(subjectTokenId);
        given(subjectTokenId.toString()).willReturn("TOKEN_ID");
        given(subjectToken.getPrincipal()).willReturn(principal);
        given(principal.getName()).willReturn("PRINCIPAL_NAME");

        //When
        authZAuditLogger.audit(auditRecord);

        //Then
        verify(loggerFactory).getLogger("amAuthorization.error");
        ArgumentCaptor<LogRecord> logRecordCaptor = ArgumentCaptor.forClass(LogRecord.class);
        verify(logger).log(logRecordCaptor.capture(), eq(adminToken));
        LogRecord logRecord = logRecordCaptor.getValue();
        assertEquals(logRecord.getLevel(), Level.INFO);
        assertTrue(logRecord.getMessage().contains("Failed"));
        assertEquals(logRecord.getLogFor(), subjectToken);
        verify(logger).flush();
    }
}
