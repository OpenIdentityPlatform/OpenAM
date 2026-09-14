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

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openidentityplatform.openam.mcp.server.config.OpenAMConfig;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
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

    @Test
    void maskToken_keepsOnlyAShortPrefix() {
        assertThat(AuthInterceptor.maskToken("AQIC5wM2LY4SfczntBcXfFoFJwA6zAV2i4fnU8Sd7ao")).isEqualTo("AQIC***");
        assertThat(AuthInterceptor.maskToken("short")).isEqualTo("***");
        assertThat(AuthInterceptor.maskToken("")).isEqualTo("***");
        assertThat(AuthInterceptor.maskToken(null)).isEqualTo("null");
    }

    /**
     * A session id or an access token in the log file lets anyone who can read
     * the logs hijack that session, so the interceptor must never log them raw.
     */
    @Test
    void preHandleUsernamePassword_doesNotLogRawToken_whenRefreshing() throws Exception {
        String expiredToken = "AQIC5wM2LY4Sfczn-expired-session-token";
        String freshToken   = "AQIC5wM2LY4Sfczn-fresh-session-token";
        tokenCache.put("login-password-token", expiredToken);

        AuthInterceptor spy = spy(interceptor);
        doReturn(1L).when(spy).tokenValidSeconds(expiredToken);
        doReturn(freshToken).when(spy).getUserNamePasswordToken();
        doReturn(300L).when(spy).tokenValidSeconds(freshToken);

        List<String> messages = captureLogs(() -> spy.preHandleUsernamePassword(new MockHttpServletRequest()));

        assertThat(messages).anyMatch(m -> m.contains("about to expire"));
        assertThat(messages).noneMatch(m -> m.contains(expiredToken));
        assertThat(messages).noneMatch(m -> m.contains(freshToken));
    }

    @Test
    void preHandleOAuth_doesNotLogRawSessionToken_whenRefreshing() {
        String accessToken  = "f3c1a9e0-access-token-value";
        String expiredToken = "AQIC5wM2LY4Sfczn-expired-session-token";
        tokenCache.put(accessToken, expiredToken);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + accessToken);

        AuthInterceptor spy = spy(interceptor);
        doReturn(true).when(spy).accessTokenValid(accessToken);
        doReturn(1L).when(spy).tokenValidSeconds(expiredToken);

        List<String> messages = captureLogs(() -> {
            try {
                spy.preHandleOAuth(req, new MockHttpServletResponse());
            } catch (Exception ignored) {
                // The mocked RestClient cannot mint a new token; the log line
                // under test is written before that call.
            }
        });

        assertThat(messages).anyMatch(m -> m.contains("about to expire"));
        assertThat(messages).noneMatch(m -> m.contains(expiredToken));
    }

    @Test
    void accessTokenValid_doesNotLogRawAccessToken_onInvalidResponse() {
        String accessToken = "f3c1a9e0-access-token-value";

        // userinfo answers without a "name": the token must be reported as invalid
        // without echoing it into the log.
        @SuppressWarnings("rawtypes")
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        doReturn(uriSpec).when(restClient).get();
        doReturn(uriSpec).when(uriSpec).uri(anyString());
        doReturn(uriSpec).when(uriSpec).header(anyString(), any(String[].class));
        doReturn(responseSpec).when(uriSpec).retrieve();
        doReturn(Map.of("error", "invalid_token")).when(responseSpec).body(any(ParameterizedTypeReference.class));

        List<String> messages = captureLogs(() -> assertThat(interceptor.accessTokenValid(accessToken)).isFalse());

        assertThat(messages).anyMatch(m -> m.contains("got invalid response"));
        assertThat(messages).noneMatch(m -> m.contains(accessToken));
    }

    private static List<String> captureLogs(Runnable action) {
        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(AuthInterceptor.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            action.run();
        } finally {
            logger.detachAppender(appender);
        }
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.toList());
    }
}