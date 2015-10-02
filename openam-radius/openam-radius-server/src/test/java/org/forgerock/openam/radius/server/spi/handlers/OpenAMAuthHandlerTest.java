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
 * Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */
package org.forgerock.openam.radius.server.spi.handlers;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Properties;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import org.forgerock.openam.radius.common.AccessAccept;
import org.forgerock.openam.radius.common.AccessRequest;
import org.forgerock.openam.radius.common.AttributeSet;
import org.forgerock.openam.radius.common.Authenticator;
import org.forgerock.openam.radius.common.UserNameAttribute;
import org.forgerock.openam.radius.common.UserPasswordAttribute;
import org.forgerock.openam.radius.server.RadiusProcessingException;
import org.forgerock.openam.radius.server.RadiusResponseHandler;
import org.forgerock.openam.radius.server.spi.handlers.OpenAMAuthHandler;
import org.forgerock.openam.radius.server.spi.handlers.amhandler.ContextHolder;
import org.forgerock.openam.radius.server.spi.handlers.amhandler.ContextHolderCache;
import org.forgerock.openam.radius.server.spi.handlers.amhandler.OpenAMAuthFactory;
import org.forgerock.openam.radius.server.spi.handlers.amhandler.ContextHolder.AuthPhase;
import org.testng.annotations.Test;

import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.AuthContext.Status;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.PagePropertiesCallback;

public class OpenAMAuthHandlerTest {

    @Test(enabled = true)
    public void handle() throws RadiusProcessingException, AuthLoginException, IOException {
        // given
        final Callback pagePropCallback = new PagePropertiesCallback("test_module", null, null, 0, null, false, null);
        final Callback nameCallback = new NameCallback("Username:");
        final Callback pwCallback = new PasswordCallback("pw_prompt", false);
        final Callback callbacks[] = new Callback[] { pagePropCallback, nameCallback, pwCallback };

        final String TEST_REALM = "test_realm";
        final String TEST_CHAIN = "test_chain";
        final String CACHE_KEY = "cache_key";
        final String PASSWORD = "password";
        final Properties props = new Properties();
        props.setProperty("realm", TEST_REALM);
        props.setProperty("chain", TEST_CHAIN);

        final Status status = mock(Status.class);
        final AuthContext authContext = mock(AuthContext.class);
        when(authContext.getStatus()).thenReturn(AuthContext.Status.SUCCESS);
        when(status.toString()).thenReturn("success");
        when(authContext.hasMoreRequirements()).thenReturn(true, false);
        when(authContext.getRequirements(true)).thenReturn(callbacks);

        // Context and context holder
        final ContextHolder holder = mock(ContextHolder.class);
        final OpenAMAuthFactory ctxHolderFactory = mock(OpenAMAuthFactory.class);
        when(holder.getCacheKey()).thenReturn(CACHE_KEY);
        when(holder.getAuthContext()).thenReturn(authContext);
        when(holder.getAuthPhase()).thenReturn(AuthPhase.STARTING, AuthPhase.GATHERING_INPUT, AuthPhase.FINALIZING);
        when(holder.getCallbacks()).thenReturn(callbacks, callbacks, (Callback[]) null);
        when(holder.getIdxOfCurrentCallback()).thenReturn(1, 2);

        final ContextHolderCache ctxHolderCache = mock(ContextHolderCache.class);
        when(ctxHolderCache.createCachedContextHolder()).thenReturn(holder);
        when(ctxHolderCache.get(isA(String.class))).thenReturn(holder);

        final OpenAMAuthHandler handler = new OpenAMAuthHandler(ctxHolderFactory, ctxHolderCache);
        handler.init(props);
        final Authenticator authenticator = mock(Authenticator.class);
        when(authenticator.getOctets()).thenReturn("authenticator".getBytes());
        // final StateAttribute mockStateAttribute = new StateAttribute("1");
        final UserPasswordAttribute mockUserPasswordAttribute = new UserPasswordAttribute(authenticator, "secret",
                "testPassword");
        final UserNameAttribute mockUsernameAttribute = new UserNameAttribute("testUser");
        final AttributeSet mockAttrSet = mock(AttributeSet.class);
        when(mockAttrSet.size()).thenReturn(2);
        // when(mockAttrSet.getAttributeAt(0)).thenReturn(mockStateAttribute);
        when(mockAttrSet.getAttributeAt(0)).thenReturn(mockUserPasswordAttribute);
        when(mockAttrSet.getAttributeAt(1)).thenReturn(mockUsernameAttribute);
        final AccessRequest mockRequest = mock(AccessRequest.class);
        when(mockRequest.getAttributeSet()).thenReturn(mockAttrSet);
        final RadiusResponseHandler respHandler = mock(RadiusResponseHandler.class);
        when(respHandler.extractPassword(mockUserPasswordAttribute)).thenReturn(PASSWORD);

        // when
        handler.handle(mockRequest, respHandler);

        // then
        verify(authContext, times(1)).login(AuthContext.IndexType.SERVICE, TEST_CHAIN);
        verify(ctxHolderFactory, times(1)).getAuthContext(TEST_REALM);
        verify(respHandler, times(1)).send(isA(AccessAccept.class));
        verify(holder, times(3)).getCallbacks();
        verify(holder, times(1)).setAuthPhase(ContextHolder.AuthPhase.TERMINATED);
        verify(authContext, times(1)).logout();
    }
}
