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

package org.forgerock.openam.sts.rest.operation;

import static org.forgerock.json.JsonValue.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Provides;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import javax.inject.Named;

import org.forgerock.guava.common.collect.Sets;
import org.forgerock.services.context.ClientContext;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenTypeId;
import org.forgerock.openam.sts.XMLUtilities;
import org.forgerock.openam.sts.XMLUtilitiesImpl;
import org.forgerock.openam.sts.config.user.CustomTokenOperation;
import org.forgerock.openam.sts.rest.token.provider.RestTokenProviderParameters;
import org.forgerock.openam.sts.rest.token.provider.saml.Saml2TokenCreationState;
import org.forgerock.openam.sts.rest.token.validator.RestTokenTransformValidatorParameters;
import org.forgerock.openam.sts.token.CTSTokenIdGenerator;
import org.forgerock.openam.sts.token.CTSTokenIdGeneratorImpl;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.forgerock.openam.sts.token.model.OpenAMSessionToken;
import org.forgerock.openam.sts.token.model.RestUsernameToken;
import org.forgerock.openam.sts.user.invocation.ProofTokenState;
import org.forgerock.openam.sts.user.invocation.SAML2TokenCreationState;
import org.forgerock.openam.sts.user.invocation.X509TokenState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class TokenRequestMarshallerImplTest {
    private static final String CUSTOM_TOKEN_NAME = "BOBO";
    private TokenRequestMarshaller tokenMarshaller;
    static class MyModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(TokenRequestMarshaller.class).to(TokenRequestMarshallerImpl.class);
            bind(CTSTokenIdGenerator.class).to(CTSTokenIdGeneratorImpl.class);
            bind(XMLUtilities.class).to(XMLUtilitiesImpl.class);
        }

        @Provides
        Logger getSlf4jLogger() {
            return LoggerFactory.getLogger(AMSTSConstants.REST_STS_DEBUG_ID);
        }

        //Must be empty for the testX509CertificateTokenMarshalling() to reference cert from ClientContext
        @Provides
        @Named(AMSTSConstants.OFFLOADED_TWO_WAY_TLS_HEADER_KEY)
        String getOffloadedTwoWayTLSHeaderKey() {
            return "";
        }

        @Provides
        @Named(AMSTSConstants.TLS_OFFLOAD_ENGINE_HOSTS)
        Set<String> getTlsOffloadEngineHosts() {
            return Collections.emptySet();
        }

        @Provides
        @Named(AMSTSConstants.REST_CUSTOM_TOKEN_VALIDATORS)
        Set<CustomTokenOperation> getCustomTokenValidators() {
            return Sets.newHashSet(new CustomTokenOperation(CUSTOM_TOKEN_NAME, "org.forgerock.bobo.BoboTokenValidator"));
        }

        @Provides
        @Named(AMSTSConstants.REST_CUSTOM_TOKEN_PROVIDERS)
        Set<CustomTokenOperation> getCustomTokenProviders() {
            return Sets.newHashSet(new CustomTokenOperation(CUSTOM_TOKEN_NAME, "org.forgerock.bobo.BoboTokenProvider"));
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
        RestTokenTransformValidatorParameters<?> params = tokenMarshaller.buildTokenTransformValidatorParameters(jsonUnt, null);
        assertEquals("bobo".getBytes(AMSTSConstants.UTF_8_CHARSET_ID), ((RestUsernameToken)params.getInputToken()).getUsername());
    }

    @Test
    public void marshallOpenAMToken() throws TokenMarshalException {
        JsonValue jsonOpenAM = json(object(field("token_type", "OPENAM"),
                field("session_id", "super_random")));
        RestTokenTransformValidatorParameters<?> params = tokenMarshaller.buildTokenTransformValidatorParameters(jsonOpenAM, null);
        assertTrue("super_random".equals(((OpenAMSessionToken) params.getInputToken()).getSessionId()));
    }


    @Test
    public void testX509CertificateTokenMarshalling() throws Exception {
        X509Certificate certificate = getCertificate();
        ClientContext clientInfoContext = ClientContext.buildExternalClientContext(null).certificates(certificate).build();

        @SuppressWarnings("unchecked")
        RestTokenTransformValidatorParameters<X509Certificate[]> params =
                (RestTokenTransformValidatorParameters<X509Certificate[]>)
                tokenMarshaller.buildTokenTransformValidatorParameters(new X509TokenState().toJson(), clientInfoContext);
        assertEquals(certificate.getEncoded(), (params.getInputToken()[0].getEncoded()));
    }

    @Test (expectedExceptions = TokenMarshalException.class)
    public void testMissingX509CertificateTokenMarshalling() throws Exception {
        ClientContext clientInfoContext = ClientContext.buildExternalClientContext(null).build();
        //no certificate present in the ClientContext, and the offload header set to "" by the module above, so
        //exception should be thrown
        tokenMarshaller.buildTokenTransformValidatorParameters(new X509TokenState().toJson(), clientInfoContext);
    }

    @Test
    public void testBuildProviderParametersUsernameToken() throws TokenMarshalException, UnsupportedEncodingException {
        JsonValue jsonUnt = json(object(field("token_type", "USERNAME"),
                field("username", "bobo"), field("password", "cornholio")));
        JsonValue saml2Output =
                SAML2TokenCreationState.builder().saml2SubjectConfirmation(SAML2SubjectConfirmation.BEARER).build().toJson();
        RestTokenProviderParameters<?> params =
                tokenMarshaller.buildTokenProviderParameters(TokenType.USERNAME, jsonUnt, TokenType.SAML2, saml2Output);
        assertEquals(TokenType.USERNAME.getId(), params.getInputTokenType().getId());
    }

    @Test
    public void testBuildProviderParametersOpenAMSaml2HoK() throws IOException, CertificateException {
        JsonValue jsonOpenAM = json(object(field("token_type", "OPENAM"),
                field("session_id", "super_random")));
        X509Certificate certificate = getCertificate();
        JsonValue saml2Output =
                SAML2TokenCreationState.builder()
                        .saml2SubjectConfirmation(SAML2SubjectConfirmation.HOLDER_OF_KEY)
                        .proofTokenState(ProofTokenState.builder().x509Certificate(certificate).build())
                        .build()
                        .toJson();
        RestTokenProviderParameters<?> params =
                tokenMarshaller.buildTokenProviderParameters(TokenType.OPENAM, jsonOpenAM, TokenType.SAML2, saml2Output);
        assertEquals(TokenType.OPENAM.getId(), params.getInputTokenType().getId());
        assertEquals(certificate.getEncoded(),
                ((Saml2TokenCreationState)params.getTokenCreationState()).getProofTokenState().getX509Certificate().getEncoded());
    }

    @Test
    public void testBuildCustomProviderParameters() throws IOException, CertificateException {
        JsonValue jsonUnt = json(object(field("token_type", "USERNAME"),
                field("username", "bobo"), field("password", "cornholio")));
        JsonValue jsonCustomOutput = json(object(field("token_type", CUSTOM_TOKEN_NAME),
                field("whatever", "whatever")));
        TokenTypeId customTokenType = new TokenTypeId() {
            @Override
            public String getId() {
                return CUSTOM_TOKEN_NAME;
            }
        };
        RestTokenProviderParameters<?> params =
                tokenMarshaller.buildTokenProviderParameters(TokenType.USERNAME, jsonUnt, customTokenType, jsonCustomOutput);
        assertEquals(TokenType.USERNAME.getId(), params.getInputTokenType().getId());
        assertEquals(((JsonValue)params.getTokenCreationState()).get("token_type").asString(), CUSTOM_TOKEN_NAME);
    }

    @Test(expectedExceptions = TokenMarshalException.class)
    public void testBuildCustomProviderParametersWithUnregisteredCustomToken() throws IOException, CertificateException {
        JsonValue jsonUnt = json(object(field("token_type", "USERNAME"),
                field("username", "bobo"), field("password", "cornholio")));
        JsonValue jsonCustomOutput = json(object(field("token_type", "NOT_REGISTERED_AS_CUSTOM_TYPE"),
                field("whatever", "whatever")));
        TokenTypeId customTokenType = new TokenTypeId() {
            @Override
            public String getId() {
                return "NOT_REGISTERED_AS_CUSTOM_TYPE";
            }
        };
        RestTokenProviderParameters<?> params =
                tokenMarshaller.buildTokenProviderParameters(TokenType.USERNAME, jsonUnt, customTokenType, new JsonValue(new HashMap<String, Object>()));
    }

    private X509Certificate getCertificate() throws IOException, CertificateException {
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(getClass().getResourceAsStream("/cert.jks"));
    }
}
