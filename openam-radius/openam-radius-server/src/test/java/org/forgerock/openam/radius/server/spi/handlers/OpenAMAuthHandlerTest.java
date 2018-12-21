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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Properties;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import com.google.common.eventbus.EventBus;
import org.forgerock.openam.radius.common.AccessRequest;
import org.forgerock.openam.radius.common.AttributeSet;
import org.forgerock.openam.radius.common.Authenticator;
import org.forgerock.openam.radius.common.Packet;
import org.forgerock.openam.radius.common.UserNameAttribute;
import org.forgerock.openam.radius.common.UserPasswordAttribute;
import org.forgerock.openam.radius.server.RadiusProcessingException;
import org.forgerock.openam.radius.server.RadiusRequest;
import org.forgerock.openam.radius.server.RadiusRequestContext;
import org.forgerock.openam.radius.server.RadiusResponse;
import org.forgerock.openam.radius.server.spi.handlers.amhandler.ContextHolder;
import org.forgerock.openam.radius.server.spi.handlers.amhandler.ContextHolder.AuthPhase;
import org.forgerock.openam.radius.server.spi.handlers.amhandler.ContextHolderCache;
import org.forgerock.openam.radius.server.spi.handlers.amhandler.OpenAMAuthFactory;
import org.testng.annotations.Test;

import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.AuthContext.Status;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.PagePropertiesCallback;

/**
 * Test methods for <code>OpenAMAuthHandler</code>.
 *
 * @see org.forgerock.openam.radius.server.spi.handlers.OpenAMAuthHandler
 */
public class OpenAMAuthHandlerTest {

    /**
     * Test the following method;.
     *
     * @see org.forgerock.openam.radius.server.spi.handlers.OpenAMAuthHandler#handle
     * @throws RadiusProcessingException - should not happen.
     * @throws AuthLoginException - should not happen.
     * @throws IOException - should not happen.
     */
    @Test(enabled = true)
    public void handle() throws RadiusProcessingException, AuthLoginException, IOException {
        // given
        final Callback pagePropCallback = new PagePropertiesCallback("test_module", null, null, 0, null, false, null);
        final Callback nameCallback = new NameCallback("Username:");
        final Callback pwCallback = new PasswordCallback("pw_prompt", false);
        final Callback[] callbacks = new Callback[] { pagePropCallback, nameCallback, pwCallback };

        final String testRealm = "test_realm";
        final String testChain = "test_chain";
        final String cacheKey = "cache_key";
        final Properties props = new Properties();
        props.setProperty("realm", testRealm);
        props.setProperty("chain", testChain);

        final Status status = mock(Status.class);
        final AuthContext authContext = mock(AuthContext.class);
        when(authContext.getStatus()).thenReturn(AuthContext.Status.SUCCESS);
        when(status.toString()).thenReturn("success");
        when(authContext.hasMoreRequirements()).thenReturn(true, false);
        when(authContext.getRequirements(true)).thenReturn(callbacks);

        // Context and context holder
        final ContextHolder holder = mock(ContextHolder.class);
        final OpenAMAuthFactory ctxHolderFactory = mock(OpenAMAuthFactory.class);
        when(holder.getCacheKey()).thenReturn(cacheKey);
        when(holder.getAuthContext()).thenReturn(authContext);
        when(holder.getAuthPhase()).thenReturn(AuthPhase.STARTING, AuthPhase.GATHERING_INPUT, AuthPhase.FINALIZING);
        when(holder.getCallbacks()).thenReturn(callbacks, callbacks, (Callback[]) null);
        when(holder.getIdxOfCurrentCallback()).thenReturn(1, 2);

        final ContextHolderCache ctxHolderCache = mock(ContextHolderCache.class);
        when(ctxHolderCache.createCachedContextHolder()).thenReturn(holder);
        when(ctxHolderCache.get(org.mockito.Matchers.isA(String.class))).thenReturn(holder);

        EventBus eventBus = new EventBus();

        final OpenAMAuthHandler handler = new OpenAMAuthHandler(ctxHolderFactory, ctxHolderCache, eventBus);

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

        final AccessRequest mockRequestPacket = mock(AccessRequest.class);
        when(mockRequestPacket.getAttributeSet()).thenReturn(mockAttrSet);

        RadiusRequestContext reqCtx = mock(RadiusRequestContext.class);
        when(reqCtx.getRequestAuthenticator()).thenReturn((mock(Authenticator.class)));
        when(reqCtx.getClientSecret()).thenReturn("victoria");

        RadiusResponse response = new RadiusResponse();
        Packet mockPacket = mock(Packet.class);
        when(mockPacket.getIdentifier()).thenReturn((short) 1);
        RadiusRequest request = mock(RadiusRequest.class);
        when(request.getRequestPacket()).thenReturn(mockPacket);
        UserNameAttribute userName = mock(UserNameAttribute.class);
        when(userName.getName()).thenReturn("Fred");
        UserPasswordAttribute userPassword = mock(UserPasswordAttribute.class);
        when(userPassword.extractPassword(org.mockito.Matchers.isA(Authenticator.class), org.mockito.Matchers.isA(String.class))).thenReturn("password");
        when(request.getAttribute(UserPasswordAttribute.class)).thenReturn(userPassword);
        when(request.getAttribute(UserNameAttribute.class)).thenReturn(userName);

        String password = userPassword.extractPassword(reqCtx.getRequestAuthenticator(), reqCtx.getClientSecret());
        assertThat(password).isNotNull();
        // when
        handler.handle(request, response, reqCtx);

        // then
        verify(authContext, times(1)).login(AuthContext.IndexType.SERVICE, testChain);
        verify(ctxHolderFactory, times(1)).getAuthContext(testRealm);
        verify(holder, times(3)).getCallbacks();
        verify(holder, times(1)).setAuthPhase(ContextHolder.AuthPhase.TERMINATED);
        verify(authContext, times(1)).logout();
    }
}
