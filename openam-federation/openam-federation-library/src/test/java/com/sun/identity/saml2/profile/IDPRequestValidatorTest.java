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
package com.sun.identity.saml2.profile;

import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.saml2.IDPRequestValidator;
import org.forgerock.openam.saml2.UtilProxyIDPRequestValidator;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

/**
 * Note: Tests are excluding {@link IDPRequestValidator#getIDPEntity(String, String, SAML2MetaManager)}
 * and {@link IDPRequestValidator#getIDPAdapter(String, String)} because they are currently untestable.
 */
public class IDPRequestValidatorTest {
    private HttpServletRequest mockRequest;
    private IDPRequestValidator validator;

    @BeforeMethod
    public void setUp() throws ServerFaultException, ClientFaultException {
        mockRequest = mock(HttpServletRequest.class);
        validator = new UtilProxyIDPRequestValidator("", false, mock(Debug.class), mock(SAML2MetaManager.class));
    }

    @Test (expectedExceptions = ClientFaultException.class)
    public void shouldNotAllowMissingMetaAlias() throws ServerFaultException, ClientFaultException {
        given(mockRequest.getParameter(SAML2MetaManager.NAME_META_ALIAS_IN_URI)).willReturn("");
        given(mockRequest.getRequestURI()).willReturn("");
        validator.getMetaAlias(mockRequest);
    }

    @Test
    public void shoulAllowMetaAliasInParameter() throws ServerFaultException, ClientFaultException {
        // Given
        String metaBadger = "badger";
        given(mockRequest.getParameter(SAML2MetaManager.NAME_META_ALIAS_IN_URI)).willReturn(metaBadger);
        given(mockRequest.getRequestURI()).willReturn("");
        // When
        String result = validator.getMetaAlias(mockRequest);
        // Then
        assertThat(result).isEqualTo(metaBadger);
    }

    @Test
    public void shoulAllowMetaAliasInURI() throws ServerFaultException, ClientFaultException {
        // Given
        String metaBadger = "badger";
        given(mockRequest.getParameter(SAML2MetaManager.NAME_META_ALIAS_IN_URI)).willReturn("");
        given(mockRequest.getRequestURI()).willReturn("metaAlias" + metaBadger);
        // When
        String result = validator.getMetaAlias(mockRequest);
        // Then
        assertThat(result).isEqualTo(metaBadger);
    }
}