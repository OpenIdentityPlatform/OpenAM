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
 * Copyright 2026 3A Systems LLC.
 */

package org.openidentityplatform.openam.mcp.server.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openidentityplatform.openam.mcp.server.config.OpenAMConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.client.RestClient;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

    @Mock
    private OpenAMConfig openAMConfig;

    @Mock
    private RestClient restClient;
    private AuthInterceptor interceptor;

    private Cache<String, String> tokenCache;

    @BeforeEach
    void beforeEach() {
        tokenCache = Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES).build();
        interceptor = new AuthInterceptor(restClient, openAMConfig, tokenCache);
    }

    @Test
    void preHandle_routesToOAuth_whenUseOAuthIsTrue() throws Exception {
        when(openAMConfig.useOAuthForAuthentication()).thenReturn(true);

        // Spy so we can verify the OAuth branch was entered without actually
        // calling OpenAM.
        AuthInterceptor spy = spy(interceptor);
        doReturn(true).when(spy).preHandleOAuth(any(), any());

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean result = spy.preHandle(req, res, new Object());

        assertThat(result).isTrue();
        verify(spy).preHandleOAuth(req, res);
        verify(spy, never()).preHandleUsernamePassword(any());
    }

    @Test
    void preHandle_routesToUsernamePassword_whenUseOAuthIsFalse() throws Exception {
        when(openAMConfig.useOAuthForAuthentication()).thenReturn(false);

        AuthInterceptor spy = spy(interceptor);
        doReturn(true).when(spy).preHandleUsernamePassword(any());

        MockHttpServletRequest  req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean result = spy.preHandle(req, res, new Object());

        assertThat(result).isTrue();
        verify(spy).preHandleUsernamePassword(req);
        verify(spy, never()).preHandleOAuth(any(), any());
    }

    @Test
    void preHandleOAuth_returns401_whenAuthorizationHeaderIsMissing() throws Exception {
        MockHttpServletRequest  req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean result = interceptor.preHandleOAuth(req, res);

        assertThat(result).isFalse();
        assertThat(res.getStatus()).isEqualTo(401);
        assertThat(res.getHeader("WWW-Authenticate")).contains("Bearer");
    }

    @Test
    void preHandleOAuth_returns401_whenAuthorizationHeaderHasNoBearer() throws Exception {
        MockHttpServletRequest  req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean result = interceptor.preHandleOAuth(req, res);

        assertThat(result).isFalse();
        assertThat(res.getStatus()).isEqualTo(401);
    }

    @Test
    void preHandleOAuth_returns401_whenTokenIsInvalid() throws Exception {
        String invalidAccessToken = "bad-token";

        MockHttpServletRequest  req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + invalidAccessToken);
        MockHttpServletResponse res = new MockHttpServletResponse();

        AuthInterceptor spy = spy(interceptor);
        doReturn(false).when(spy).accessTokenValid(invalidAccessToken);

        boolean result = spy.preHandleOAuth(req, res);

        assertThat(result).isFalse();
        assertThat(res.getStatus()).isEqualTo(401);
        assertThat(res.getHeader("WWW-Authenticate")).startsWith("Bearer");
    }

    @Test
    void preHandleUsernamePassword_reusesValidCachedToken() {
        String cachedToken = "cached-session-token";
        tokenCache.put("login-password-token", cachedToken);

        MockHttpServletRequest req = new MockHttpServletRequest();

        AuthInterceptor spy = spy(interceptor);
        doReturn(300L).when(spy).tokenValidSeconds(cachedToken);

        boolean result = spy.preHandleUsernamePassword(req);

        assertThat(result).isTrue();
        assertThat(req.getAttribute("tokenId")).isEqualTo(cachedToken);
        verify(spy, never()).getUserNamePasswordToken();
    }

    @Test
    void preHandleUsernamePassword_refreshesToken_whenNearlyExpired() throws Exception {
        String expiredToken = "expiring-token";
        String freshToken   = "fresh-token";
        tokenCache.put("login-password-token", expiredToken);

        MockHttpServletRequest req = new MockHttpServletRequest();

        AuthInterceptor spy = spy(interceptor);
        doReturn(1L).when(spy).tokenValidSeconds(expiredToken);
        doReturn(freshToken).when(spy).getUserNamePasswordToken();
        doReturn(300L).when(spy).tokenValidSeconds(freshToken);

        boolean result = spy.preHandleUsernamePassword(req);

        assertThat(result).isTrue();
        assertThat(req.getAttribute("tokenId")).isEqualTo(freshToken);
        verify(spy, times(1)).getUserNamePasswordToken();
    }

    /**
     * Guards against infinite recursion: if the refreshed token is also
     * short-lived (e.g. session endpoint is down), the method must NOT recurse
     * indefinitely but instead fail fast after a bounded number of attempts.
     *
     * <p>This test will catch a {@link StackOverflowError} and re-fail with a
     * descriptive message so the fix requirement is obvious in CI output.
     */
    @Test
    void preHandleUsernamePassword_failsFast_whenRefreshedTokenIsAlsoExpired() throws Exception {
        String badToken = "always-expiring-token";
        tokenCache.put("login-password-token", badToken);

        MockHttpServletRequest req = new MockHttpServletRequest();

        AuthInterceptor spy = spy(interceptor);
        doReturn(0L).when(spy).tokenValidSeconds(anyString());
        doReturn(badToken).when(spy).getUserNamePasswordToken();

        try {
            boolean result = spy.preHandleUsernamePassword(req);
            // Returning false is acceptable fail-fast behaviour
            assertThat(result).isFalse();
        } catch (IllegalStateException e) {
            // A clear exception is also acceptable — just not a StackOverflowError
        } catch (StackOverflowError e) {
            throw new AssertionError(
                    "preHandleUsernamePassword must not recurse indefinitely. "
                            + "Replace the recursive call with a bounded retry loop "
                            + "and throw IllegalStateException when the limit is exceeded.", e);
        }

        // At most MAX_RETRY attempts should be made (suggested: 3)
        verify(spy, atMost(3)).getUserNamePasswordToken();
    }

    @Test
    public void preHandleUsernamePassword_logsInAndCachesToken_whenCacheIsEmpty() throws Exception {
        String newToken = "brand-new-token";

        MockHttpServletRequest req = new MockHttpServletRequest();

        AuthInterceptor spy = spy(interceptor);
        doReturn(newToken).when(spy).getUserNamePasswordToken();
        doReturn(300L).when(spy).tokenValidSeconds(newToken);

        boolean result = spy.preHandleUsernamePassword(req);

        assertThat(result).isTrue();
        assertThat(req.getAttribute("tokenId")).isEqualTo(newToken);
        assertThat(tokenCache.getIfPresent("login-password-token")).isEqualTo(newToken);
    }
}