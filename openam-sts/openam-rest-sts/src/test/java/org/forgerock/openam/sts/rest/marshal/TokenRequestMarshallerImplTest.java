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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.rest.marshal;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Provides;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenTypeId;
import org.forgerock.openam.sts.rest.service.RestSTSServiceHttpServletContext;
import org.forgerock.openam.sts.rest.token.provider.RestTokenProviderParameters;
import org.forgerock.openam.sts.rest.token.provider.saml.Saml2TokenCreationState;
import org.forgerock.openam.sts.rest.token.validator.RestTokenValidatorParameters;
import org.forgerock.openam.sts.user.invocation.ProofTokenState;
import org.forgerock.openam.sts.user.invocation.SAML2TokenState;
import org.forgerock.openam.sts.user.invocation.X509TokenState;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.forgerock.openam.sts.token.model.OpenAMSessionToken;
import org.forgerock.openam.sts.token.model.RestUsernameToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Set;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class TokenRequestMarshallerImplTest {
    private TokenRequestMarshaller tokenMarshaller;
    static class MyModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(TokenRequestMarshaller.class).to(TokenRequestMarshallerImpl.class);
        }

        @Provides
        Logger getSlf4jLogger() {
            return LoggerFactory.getLogger(AMSTSConstants.REST_STS_DEBUG_ID);
        }

        //Must be empty for the testX509CertificateTokenMarshalling() to reference cert from ServletRequest attribute
        @Provides
        @Named(AMSTSConstants.OFFLOADED_TWO_WAY_TLS_HEADER_KEY)
        String getOffloadedTwoWayTLSHeaderKey() {
            return "";
        }

        @Provides
        @Named(AMSTSConstants.TLS_OFFLOAD_ENGINE_HOSTS)
        Set<String> getTlsOffloadEngineHosts() {
            return Collections.EMPTY_SET;
        }

    }

    @BeforeTest
    public void initialize() {
        tokenMarshaller = Guice.createInjector(new MyModule()).getInstance(TokenRequestMarshaller.class);
    }

    @Test
    public void marshallUsernameToken() throws TokenMarshalException, UnsupportedEncodingException {
        JsonValue jsonUnt = json(object(field("token_type", "USERNAME"),
                field("username", "bobo"), field("password", "cornholio")));
        RestTokenValidatorParameters<?> params = tokenMarshaller.buildTokenValidatorParameters(jsonUnt, null, null);
        assertEquals(TokenType.USERNAME.getId(), params.getId());
        assertEquals("bobo".getBytes(AMSTSConstants.UTF_8_CHARSET_ID), ((RestUsernameToken)params.getInputToken()).getUsername());
    }

    @Test
    public void marshallOpenAMToken() throws TokenMarshalException {
        JsonValue jsonOpenAM = json(object(field("token_type", "OPENAM"),
                field("session_id", "super_random")));
        RestTokenValidatorParameters<?> params = tokenMarshaller.buildTokenValidatorParameters(jsonOpenAM, null, null);
        assertTrue(TokenType.OPENAM.getId().equals(params.getId()));
        assertTrue("super_random".equals(((OpenAMSessionToken) params.getInputToken()).getSessionId()));
    }


    @Test
    public void testX509CertificateTokenMarshalling() throws Exception {
        HttpServletRequest mockServletRequest = mock(HttpServletRequest.class);
        RestSTSServiceHttpServletContext mockServletContext = mock(RestSTSServiceHttpServletContext.class);
        when(mockServletContext.getHttpServletRequest()).thenReturn(mockServletRequest);
        X509Certificate certificate = getCertificate();
        X509Certificate[] certificates = new X509Certificate[] {certificate};
        when(mockServletRequest.getAttribute("javax.servlet.request.X509Certificate")).thenReturn(certificates);
        RestTokenValidatorParameters<X509Certificate[]> params = (RestTokenValidatorParameters<X509Certificate[]>)
                tokenMarshaller.buildTokenValidatorParameters(new X509TokenState().toJson(), null, mockServletContext);
        assertEquals(certificate.getEncoded(), (params.getInputToken()[0].getEncoded()));
    }

    @Test
    public void testBuildProviderParametersUsernameToken() throws TokenMarshalException, UnsupportedEncodingException {
        JsonValue jsonUnt = json(object(field("token_type", "USERNAME"),
                field("username", "bobo"), field("password", "cornholio")));
        JsonValue saml2Output =
                SAML2TokenState.builder().saml2SubjectConfirmation(SAML2SubjectConfirmation.BEARER).build().toJson();
        RestTokenProviderParameters<? extends TokenTypeId> params =
                tokenMarshaller.buildTokenProviderParameters(TokenType.USERNAME, jsonUnt, TokenType.SAML2, saml2Output);
        assertEquals(TokenType.SAML2.getId(), params.getTokenCreationState().getId());
    }

    @Test
    public void testBuildProviderParametersOpenAMSaml2HoK() throws TokenMarshalException, IOException, CertificateException {
        JsonValue jsonOpenAM = json(object(field("token_type", "OPENAM"),
                field("session_id", "super_random")));
        X509Certificate certificate = getCertificate();
        JsonValue saml2Output =
                SAML2TokenState.builder()
                        .saml2SubjectConfirmation(SAML2SubjectConfirmation.HOLDER_OF_KEY)
                        .proofTokenState(ProofTokenState.builder().x509Certificate(certificate).build())
                        .build()
                        .toJson();
        RestTokenProviderParameters<? extends TokenTypeId> params =
                tokenMarshaller.buildTokenProviderParameters(TokenType.OPENAM, jsonOpenAM, TokenType.SAML2, saml2Output);
        assertEquals(TokenType.SAML2.getId(), params.getTokenCreationState().getId());
        assertEquals(certificate.getEncoded(),
                ((Saml2TokenCreationState)params.getTokenCreationState()).getProofTokenState().getX509Certificate().getEncoded());
    }

    private X509Certificate getCertificate() throws IOException, CertificateException {
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(getClass().getResourceAsStream("/cert.jks"));
    }
}
