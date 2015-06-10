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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenMarshalException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.XMLUtilities;
import org.forgerock.openam.sts.XMLUtilitiesImpl;
import org.forgerock.openam.sts.config.user.TokenTransformConfig;
import org.forgerock.openam.sts.rest.marshal.TokenRequestMarshaller;
import org.forgerock.openam.sts.rest.marshal.TokenRequestMarshallerImpl;
import org.forgerock.openam.sts.user.invocation.RestSTSServiceInvocationState;
import org.forgerock.openam.sts.user.invocation.SAML2TokenState;
import org.forgerock.openam.sts.user.invocation.UsernameTokenState;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import javax.inject.Named;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;

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
            bind(XMLUtilities.class).to(XMLUtilitiesImpl.class);

            bind(TokenTranslateOperation.class).to(TokenTranslateOperationImpl.class);
        }

        @Provides
        @Named(AMSTSConstants.REST_SUPPORTED_TOKEN_TRANSLATIONS)
        Set<TokenTransformConfig> getSupportedTokenTransforms() {
            HashSet<TokenTransformConfig> supportedTransforms = new HashSet<>();
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

    private RestSTSServiceInvocationState buildInvocationState(TokenType desiredTokenType) throws Exception {
        UsernameTokenState untState = UsernameTokenState.builder().password("bobo".getBytes()).username("dodo".getBytes()).build();
        JsonValue outputTokenState;
        if (TokenType.SAML2.equals(desiredTokenType)) {
            outputTokenState = SAML2TokenState.builder()
                    .saml2SubjectConfirmation(SAML2SubjectConfirmation.BEARER)
                    .build()
                    .toJson();
        } else {
            throw new Exception("Unexpected desiredTokenType: " + desiredTokenType);
        }
        return RestSTSServiceInvocationState.builder().inputTokenState(untState.toJson()).outputTokenState(outputTokenState).build();
    }
}
