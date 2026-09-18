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

package org.forgerock.oauth2.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenID;

import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CsrfProtectionTest {

    private ResourceOwnerSessionValidator sessionValidator;
    private CsrfProtection csrfProtection;
    private OAuth2Request request;
    private SSOToken ssoToken;

    @BeforeMethod
    public void setUp() throws Exception {
        sessionValidator = mock(ResourceOwnerSessionValidator.class);
        csrfProtection = new CsrfProtection(sessionValidator);
        request = mock(OAuth2Request.class);
        ssoToken = mock(SSOToken.class);
        when(sessionValidator.getResourceOwnerSession(request)).thenReturn(ssoToken);
    }

    /**
     * Test double that stubs out the static {@code CookieUtils} access so the double-submit cookie logic can be
     * exercised without a servlet container. It models the cookies actually present on the request and whether the
     * deployment is secure (HTTPS).
     */
    private static final class TestableCsrfProtection extends CsrfProtection {

        private final boolean secure;
        private final Map<String, String> cookies = new HashMap<>();

        TestableCsrfProtection(ResourceOwnerSessionValidator validator, boolean secure) {
            super(validator);
            this.secure = secure;
        }

        TestableCsrfProtection withCookie(String name, String value) {
            cookies.put(name, value);
            return this;
        }

        @Override
        protected boolean isCookieSecure() {
            return secure;
        }

        @Override
        protected String readCookie(OAuth2Request request, String name) {
            return cookies.get(name);
        }
    }

    @Test
    public void shouldStoreDedicatedTokenAndAcceptMatchingValue() throws Exception {
        // given the consent page mints a CSRF token bound to the session
        String token = csrfProtection.createCsrfToken(request);
        ArgumentCaptor<String> stored = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(ssoToken)
                .setProperty(eq(CsrfProtection.CSRF_SESSION_PROPERTY), stored.capture());
        assertThat(token).isNotEmpty().isEqualTo(stored.getValue());

        // and the token is not the SSO token id (it is a dedicated random value)
        when(ssoToken.getProperty(CsrfProtection.CSRF_SESSION_PROPERTY)).thenReturn(token);
        when(request.getParameter("csrf")).thenReturn(token);

        // then a request carrying the matching token is not treated as an attack
        assertThat(csrfProtection.isCsrfAttack(request)).isFalse();
    }

    @Test
    public void shouldRejectMissingToken() {
        when(request.getParameter("csrf")).thenReturn(null);
        assertThat(csrfProtection.isCsrfAttack(request)).isTrue();
    }

    @Test
    public void shouldRejectForgedToken() throws Exception {
        when(ssoToken.getProperty(CsrfProtection.CSRF_SESSION_PROPERTY)).thenReturn("the-real-token");
        when(request.getParameter("csrf")).thenReturn("a-forged-token");
        assertThat(csrfProtection.isCsrfAttack(request)).isTrue();
    }

    @Test
    public void shouldAcceptHostPrefixedCookieOnSecureDeployment() throws Exception {
        // given a stateless session (no CSRF session property) on a secure (HTTPS) deployment
        when(ssoToken.getProperty(CsrfProtection.CSRF_SESSION_PROPERTY)).thenReturn(null);
        TestableCsrfProtection csrf = new TestableCsrfProtection(sessionValidator, true)
                .withCookie(CsrfProtection.CSRF_COOKIE_SECURE, "token-value");
        when(request.getParameter("csrf")).thenReturn("token-value");

        // then the request carrying the matching __Host- cookie is not treated as an attack
        assertThat(csrf.isCsrfAttack(request)).isFalse();
    }

    /**
     * Regression test for the double-submit bypass: on a secure deployment the validation must accept the
     * {@code __Host-oauth2_csrf} cookie exclusively. Accepting the unprefixed {@code oauth2_csrf} would let an
     * attacker who can write a non-Secure cookie for the parent domain defeat the protection.
     */
    @Test
    public void shouldRejectUnprefixedCookieOnSecureDeployment() throws Exception {
        // given a stateless session on a secure (HTTPS) deployment where only the unprefixed cookie is present
        when(ssoToken.getProperty(CsrfProtection.CSRF_SESSION_PROPERTY)).thenReturn(null);
        TestableCsrfProtection csrf = new TestableCsrfProtection(sessionValidator, true)
                .withCookie(CsrfProtection.CSRF_COOKIE_PLAIN, "attacker-fixed-value");
        when(request.getParameter("csrf")).thenReturn("attacker-fixed-value");

        // then the request is rejected: the unprefixed cookie must not be honoured on secure deployments
        assertThat(csrf.isCsrfAttack(request)).isTrue();
    }

    @Test
    public void shouldAcceptUnprefixedCookieOnPlainHttpDeployment() throws Exception {
        // given a stateless session on a plain-HTTP deployment (degraded fallback path)
        when(ssoToken.getProperty(CsrfProtection.CSRF_SESSION_PROPERTY)).thenReturn(null);
        TestableCsrfProtection csrf = new TestableCsrfProtection(sessionValidator, false)
                .withCookie(CsrfProtection.CSRF_COOKIE_PLAIN, "token-value");
        when(request.getParameter("csrf")).thenReturn("token-value");

        // then the unprefixed cookie is honoured so the degraded path keeps working
        assertThat(csrf.isCsrfAttack(request)).isFalse();
    }

    /** {@code toString()} cannot be stubbed on a mock, so the session id is supplied by a real implementation. */
    private void givenSessionId(final String tokenId) {
        when(ssoToken.getTokenID()).thenReturn(new SSOTokenID() {
            @Override
            public String toString() {
                return tokenId;
            }
        });
    }

    /**
     * The documented flow for non-browser clients: authenticate at {@literal /json/authenticate}, then submit the
     * returned {@code tokenId} as {@code csrf}. No consent page fetch, no cookie jar.
     */
    @Test
    public void shouldAcceptSessionIdAsCsrfValue() throws Exception {
        // given a client that never rendered the consent page, so no token was ever minted
        when(ssoToken.getProperty(CsrfProtection.CSRF_SESSION_PROPERTY)).thenReturn(null);
        givenSessionId("AQIC5wM2LY4Sfcy-the-session-id");
        when(request.getParameter("csrf")).thenReturn("AQIC5wM2LY4Sfcy-the-session-id");

        // then submitting its own session id is accepted
        assertThat(csrfProtection.isCsrfAttack(request)).isFalse();
    }

    @Test
    public void shouldRejectSomeoneElsesSessionId() throws Exception {
        when(ssoToken.getProperty(CsrfProtection.CSRF_SESSION_PROPERTY)).thenReturn(null);
        givenSessionId("the-callers-session-id");
        when(request.getParameter("csrf")).thenReturn("a-different-session-id");

        assertThat(csrfProtection.isCsrfAttack(request)).isTrue();
    }

    /**
     * The pre-16.1.1 implementation dereferenced the session unconditionally. An unauthenticated POST must be
     * rejected, not answered with a NullPointerException.
     */
    @Test
    public void shouldRejectWhenThereIsNoSession() {
        when(sessionValidator.getResourceOwnerSession(request)).thenReturn(null);
        when(request.getParameter("csrf")).thenReturn("anything");

        assertThat(csrfProtection.isCsrfAttack(request)).isTrue();
    }

    @Test
    public void shouldRejectWhenSessionHasNoTokenId() throws Exception {
        when(ssoToken.getProperty(CsrfProtection.CSRF_SESSION_PROPERTY)).thenReturn(null);
        when(ssoToken.getTokenID()).thenReturn(null);
        when(request.getParameter("csrf")).thenReturn("anything");

        assertThat(csrfProtection.isCsrfAttack(request)).isTrue();
    }

    /**
     * Minting a token for the consent page does not make it the only accepted value. Both the token and the
     * session id are accepted, and nothing else is. The session-id branch is deliberately not disabled once a
     * token exists, because the server cannot tell which of the two a legitimate caller is holding.
     */
    @Test
    public void shouldAcceptEitherTokenOrSessionIdAndNothingElse() throws Exception {
        givenSessionId("the-session-id");
        when(ssoToken.getProperty(CsrfProtection.CSRF_SESSION_PROPERTY)).thenReturn("the-minted-token");

        when(request.getParameter("csrf")).thenReturn("the-minted-token");
        assertThat(csrfProtection.isCsrfAttack(request)).isFalse();

        when(request.getParameter("csrf")).thenReturn("the-session-id");
        assertThat(csrfProtection.isCsrfAttack(request)).isFalse();

        when(request.getParameter("csrf")).thenReturn("neither-of-them");
        assertThat(csrfProtection.isCsrfAttack(request)).isTrue();
    }
}

