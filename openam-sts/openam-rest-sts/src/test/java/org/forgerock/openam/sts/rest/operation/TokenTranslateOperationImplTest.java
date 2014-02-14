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
import com.google.inject.name.Names;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.cache.DefaultInMemoryTokenStore;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.forgerock.openam.sts.*;
import org.forgerock.openam.sts.rest.config.user.TokenTransformConfig;
import org.forgerock.openam.sts.rest.marshal.*;
import org.forgerock.openam.sts.token.provider.OpenAMSessionIdElementBuilder;
import org.forgerock.openam.sts.token.provider.OpenAMSessionIdElementBuilderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.inject.Named;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertTrue;



/*
Note that an injector is created in each test as the behavior of mocks need to be changed in each test.
 */
public class TokenTranslateOperationImplTest {
    static final boolean INVALIDATE_INTERIM_AM_SESSIONS = true;
    static final String fauxJsonUnt = "{\"token_type\": \"USERNAME\", \"username\" : \"username\", \"password\" : \"password\"}";
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
            bind(WebServiceContextFactory.class).to(WebServiceContextFactoryImpl.class);
            bind(TokenStore.class).toInstance(mock(TokenStore.class));
            bind(OpenAMSessionIdElementBuilder.class).to(OpenAMSessionIdElementBuilderImpl.class);
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

    }

    @Test(expectedExceptions = TokenValidationException.class)
    public void testUnsupportedTransform() throws TokenValidationException, TokenCreationException {
        Injector injector = Guice.createInjector(new MyModule());
        TokenTranslateOperation tokenTranslateOperation = injector.getInstance(TokenTranslateOperation.class);
        TokenTransform mockTokenTransform = injector.getInstance(TokenTransform.class);
        when(mockTokenTransform.isTransformSupported(any(TokenType.class), any(TokenType.class))).thenReturn(Boolean.FALSE);
        tokenTranslateOperation.translateToken(fauxJsonUnt, "SAML2", null);
    }

    @Test(expectedExceptions = TokenValidationException.class)
    public void testUnknownTransform() throws TokenValidationException, TokenCreationException {
        TokenTranslateOperation tokenTranslateOperation = Guice.createInjector(new MyModule()).getInstance(TokenTranslateOperation.class);
        tokenTranslateOperation.translateToken(fauxJsonUnt, "unknown_token_type", null);
    }

    @Test(expectedExceptions = TokenValidationException.class)
    public void testIncorrectJson() throws TokenValidationException, TokenCreationException {
        TokenTranslateOperation tokenTranslateOperation = Guice.createInjector(new MyModule()).getInstance(TokenTranslateOperation.class);
        tokenTranslateOperation.translateToken("not_json", "SAML2", null);
    }

    @Test
    public void testHardcodedTransform() throws TokenValidationException, TokenCreationException, TokenMarshalException {
        Injector injector = Guice.createInjector(new MyModule());
        TokenTranslateOperation tokenTranslateOperation = injector.getInstance(TokenTranslateOperation.class);
        TokenTransform mockTokenTransform = injector.getInstance(TokenTransform.class);
        when(mockTokenTransform.isTransformSupported(any(TokenType.class), any(TokenType.class))).thenReturn(Boolean.TRUE);
        TokenProviderResponse mockTokenProviderResponse = mock(TokenProviderResponse.class);
        when(mockTokenTransform.transformToken(any(TokenValidatorParameters.class), any(TokenProviderParameters.class))).thenReturn(mockTokenProviderResponse);
        String fauxSessionId = "faux_session_id";
        when(mockTokenProviderResponse.getToken()).thenReturn(new OpenAMSessionIdElementBuilderImpl().buildOpenAMSessionIdElement(fauxSessionId));
        String result = tokenTranslateOperation.translateToken(fauxJsonUnt, "OPENAM", null);
        assertTrue(result.contains(fauxSessionId));
    }
}
