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
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.soap.token.config;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.name.Named;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.cache.DefaultInMemoryTokenStore;
import org.apache.cxf.sts.operation.TokenIssueOperation;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCacheImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

public class TokenIssueOperationProviderTest {
    static class MyModule extends AbstractModule {
        TokenOperationFactory tokenOperationFactory;
        MyModule(TokenOperationFactory factory) {
            tokenOperationFactory = factory;
        }
        @Override
        protected void configure() {
            bind(TokenStore.class).to(DefaultInMemoryTokenStore.class);
            bind(ThreadLocalAMTokenCache.class).to(ThreadLocalAMTokenCacheImpl.class);
            bind(TokenOperationFactory.class).toInstance(tokenOperationFactory);
            bind(TokenIssueOperationProvider.class);
        }

        @Provides
        STSPropertiesMBean getSTSPropertiesMBean() {
            return new StaticSTSProperties();
        }

        @Provides
        @Named(AMSTSConstants.TOKEN_ISSUE_OPERATION)
        public Set<TokenType> getTokenTypes() {
            HashSet<TokenType> tokenTypes = new HashSet<TokenType>();
            tokenTypes.add(TokenType.SAML2);
            tokenTypes.add(TokenType.OPENAM);
            return tokenTypes;
        }

        @Provides
        Logger getSlf4jLogger() {
            return LoggerFactory.getLogger(AMSTSConstants.REST_STS_DEBUG_ID);
        }

    }

    @Test
    public void testDelegate() throws STSInitializationException {
        TokenOperationFactory mockOperationFactory = mock(TokenOperationFactory.class);
        TokenProvider mockProvider = mock(TokenProvider.class);
        when(mockOperationFactory.getTokenProviderForType(any(TokenType.class))).thenReturn(mockProvider);
        TokenIssueOperationProvider issueOperationProvider =
                Guice.createInjector(new MyModule(mockOperationFactory)).getInstance(TokenIssueOperationProvider.class);
        assertTrue(issueOperationProvider.get() instanceof TokenIssueOperationProvider.TokenIssueOperationWrapper);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testExceptionInitialization() throws STSInitializationException {
        TokenOperationFactory mockOperationFactory = mock(TokenOperationFactory.class);
        TokenProvider mockProvider = mock(TokenProvider.class);
        when(mockOperationFactory.getTokenProviderForType(any(TokenType.class))).thenThrow(STSInitializationException.class);
        TokenIssueOperationProvider issueOperationProvider =
                Guice.createInjector(new MyModule(mockOperationFactory)).getInstance(TokenIssueOperationProvider.class);
        issueOperationProvider.get();
    }
}
