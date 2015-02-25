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
 * Copyright © 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.rest.operation;

import com.google.inject.*;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.*;
import org.forgerock.openam.sts.rest.config.user.TokenTransformConfig;
import org.forgerock.openam.sts.rest.marshal.*;
import org.forgerock.openam.sts.service.invocation.OpenAMTokenState;
import org.forgerock.openam.sts.service.invocation.RestSTSServiceInvocationState;
import org.forgerock.openam.sts.service.invocation.SAML2TokenState;
import org.forgerock.openam.sts.service.invocation.UsernameTokenState;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.forgerock.openam.sts.token.model.OpenAMSessionToken;
import org.forgerock.openam.sts.token.model.OpenAMSessionTokenMarshaller;
import org.forgerock.openam.sts.token.model.OpenIdConnectIdToken;
import org.forgerock.openam.sts.token.model.OpenIdConnectIdTokenMarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import javax.inject.Named;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertTrue;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/*
Note that an injector is created in each test as the behavior of mocks need to be changed in each test.
 */
public class TokenTranslateOperationImplTest {
    static final boolean INVALIDATE_INTERIM_AM_SESSIONS = true;
    class MyModule extends AbstractModule {
        @Override
        protected void configure() {
            TokenTransform mockTokenTransform = mock(TokenTransform.class);
            /*
            Have to mock the TokenTransformFactory in the module because it will be invoked in the TokenTranslateOperationImpl
            ctor to build the set of TokenTransform instances.
             */
            TokenTransformFactory mockTransformFactory = mock(TokenTransformFactory.class);
            try {
                when(mockTransformFactory.buildTokenTransform(any(TokenTransformConfig.class))).thenReturn(mockTokenTransform);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            bind(TokenTransformFactory.class).toInstance(mockTransformFactory);
            bind(TokenTransform.class).toInstance(mockTokenTransform);
            bind(TokenRequestMarshaller.class).to(TokenRequestMarshallerImpl.class);
            bind(TokenResponseMarshaller.class).to(TokenResponseMarshallerImpl.class);
            bind(WebServiceContextFactory.class).to(CrestWebServiceContextFactoryImpl.class);
            bind(TokenStore.class).toInstance(mock(TokenStore.class));
            bind(new TypeLiteral<XmlMarshaller<OpenAMSessionToken>>(){}).to(OpenAMSessionTokenMarshaller.class);
            bind(new TypeLiteral<XmlMarshaller<OpenIdConnectIdToken>>(){}).to(OpenIdConnectIdTokenMarshaller.class);
            bind(new TypeLiteral<JsonMarshaller<OpenIdConnectIdToken>>(){}).to(OpenIdConnectIdTokenMarshaller.class);
            bind(XMLUtilities.class).to(XMLUtilitiesImpl.class);

            bind(TokenTranslateOperation.class).to(TokenTranslateOperationImpl.class);
        }

        @Provides
        @Named(AMSTSConstants.REST_SUPPORTED_TOKEN_TRANSLATIONS)
        Set<TokenTransformConfig> getSupportedTokenTransforms() {
            HashSet<TokenTransformConfig> supportedTransforms = new HashSet<TokenTransformConfig>();
            supportedTransforms.add(new TokenTransformConfig(TokenType.USERNAME, TokenType.SAML2, INVALIDATE_INTERIM_AM_SESSIONS));
            return supportedTransforms;
        }

        @Provides
        @Named (AMSTSConstants.REALM)
        String realm() {
            return "bobo";
        }

        @Provides
        @Named(AMSTSConstants.AM_REST_AUTHN_JSON_ROOT)
        String getJsonRoot() {
            return "json";
        }

        @Provides
        STSPropertiesMBean getSTSPropertiesMBean() {
            return new StaticSTSProperties();
        }

        @Provides
        Logger getSlf4jLogger() {
            return LoggerFactory.getLogger(AMSTSConstants.REST_STS_DEBUG_ID);
        }
        @Provides
        @Named(AMSTSConstants.OFFLOADED_TWO_WAY_TLS_HEADER_KEY)
        String getOffloadedTwoWayTLSHeaderKey() {
            return "client_cert";
        }

        @Provides
        @Named(AMSTSConstants.TLS_OFFLOAD_ENGINE_HOSTS)
        Set<String> getTlsOffloadEngineHosts() {
            return Collections.EMPTY_SET;
        }
    }

    @Test(expectedExceptions = TokenValidationException.class)
    public void testUnsupportedTransform() throws Exception {
        Injector injector = Guice.createInjector(new MyModule());
        TokenTranslateOperation tokenTranslateOperation = injector.getInstance(TokenTranslateOperation.class);
        TokenTransform mockTokenTransform = injector.getInstance(TokenTransform.class);
        when(mockTokenTransform.isTransformSupported(any(TokenType.class), any(TokenType.class))).thenReturn(Boolean.FALSE);
        tokenTranslateOperation.translateToken(buildInvocationState(TokenType.SAML2), null, null);
    }

    @Test(expectedExceptions = TokenMarshalException.class)
    public void testUnknownTransform() throws Exception {
        TokenTranslateOperation tokenTranslateOperation = Guice.createInjector(new MyModule()).getInstance(TokenTranslateOperation.class);
        JsonValue bunkTokenState = json(object(field("token_type", "nonsense")));
        RestSTSServiceInvocationState invocationState =
                RestSTSServiceInvocationState.builder().inputTokenState(bunkTokenState).outputTokenState(bunkTokenState).build();
        tokenTranslateOperation.translateToken(invocationState, null, null);
    }
    @Test
    public void testHardcodedTransform() throws Exception {
        Injector injector = Guice.createInjector(new MyModule());
        TokenTranslateOperation tokenTranslateOperation = injector.getInstance(TokenTranslateOperation.class);
        TokenTransform mockTokenTransform = injector.getInstance(TokenTransform.class);
        when(mockTokenTransform.isTransformSupported(any(TokenType.class), any(TokenType.class))).thenReturn(Boolean.TRUE);
        TokenProviderResponse mockTokenProviderResponse = mock(TokenProviderResponse.class);
        when(mockTokenTransform.transformToken(any(TokenValidatorParameters.class), any(TokenProviderParameters.class))).thenReturn(mockTokenProviderResponse);
        String fauxSessionId = "faux_session_id";
        when(mockTokenProviderResponse.getToken()).thenReturn(new OpenAMSessionTokenMarshaller().toXml(new OpenAMSessionToken(fauxSessionId)));
        JsonValue result = tokenTranslateOperation.translateToken(buildInvocationState(TokenType.OPENAM), null, null);
        assertTrue(result.toString().contains(fauxSessionId));
    }

    private RestSTSServiceInvocationState buildInvocationState(TokenType desiredTokenType) throws Exception {
        UsernameTokenState untState = UsernameTokenState.builder().password("bobo".getBytes()).username("dodo".getBytes()).build();
        JsonValue outputTokenState;
        if (TokenType.SAML2.equals(desiredTokenType)) {
            outputTokenState = SAML2TokenState.builder()
                    .saml2SubjectConfirmation(SAML2SubjectConfirmation.BEARER)
                    .build()
                    .toJson();
        } else if (TokenType.OPENAM.equals(desiredTokenType)) {
            outputTokenState = OpenAMTokenState.builder().sessionId("faux_session_id").build().toJson();
        } else {
            throw new Exception("Unexpected desiredTokenType: " + desiredTokenType);
        }
        return RestSTSServiceInvocationState.builder().inputTokenState(untState.toJson()).outputTokenState(outputTokenState).build();
    }
}
