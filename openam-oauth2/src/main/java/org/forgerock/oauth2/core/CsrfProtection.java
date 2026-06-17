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
 * Copyright 2016 ForgeRock AS.
 * Portions Copyrighted 2025-2026 3A Systems LLC.
 */

package org.forgerock.oauth2.core;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.CookieUtils;

import org.forgerock.openam.rest.jakarta.servlet.ServletUtils;
import org.forgerock.openam.utils.StringUtils;
import org.restlet.Request;
import org.restlet.Response;

/**
 * This class provides methods for checking if a request is a part of a cross-site request forgery attack (CSRF).
 *
 * <p>The CSRF token used by the OAuth2/OIDC consent flow is a dedicated, random value bound to the authorization
 * request. It is no longer derived from the SSO cookie, so the SSO cookie no longer needs to be script-readable
 * and can be shipped as {@code HttpOnly}. For stateful sessions the token is stored as a protected session
 * property; for stateless sessions a double-submit cookie is used as a fallback.</p>
 */
public class CsrfProtection {

    private static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");

    /** Protected session property (see {@link Constants#AM_PROTECTED_PROPERTY_PREFIX}) holding the consent CSRF token. */
    public static final String CSRF_SESSION_PROPERTY = Constants.AM_PROTECTED_PROPERTY_PREFIX + ".oauth2.csrf";

    /** Double-submit cookie name used on secure connections (cannot be set by other origins or read by script). */
    static final String CSRF_COOKIE_SECURE = "__Host-oauth2_csrf";

    /** Double-submit cookie name used as a degraded fallback on plain-HTTP deployments. */
    static final String CSRF_COOKIE_PLAIN = "oauth2_csrf";

    private static final Debug logger = Debug.getInstance("OAuth2Provider");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ResourceOwnerSessionValidator resourceOwnerSessionValidator;

    @Inject
    public CsrfProtection(ResourceOwnerSessionValidator resourceOwnerSessionValidator) {
        this.resourceOwnerSessionValidator = resourceOwnerSessionValidator;
    }

    /**
     * Creates a fresh, random CSRF token bound to the current authorization request. The token is stored as a
     * protected session property (for stateful sessions) and also issued as a double-submit cookie (the fallback
     * used by stateless sessions). The returned value must be rendered into the consent page.
     *
     * @param request The request.
     * @return The freshly minted CSRF token.
     */
    public String createCsrfToken(OAuth2Request request) {
        byte[] random = new byte[32];
        SECURE_RANDOM.nextBytes(random);
        final String token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);

        SSOToken ssoToken = resourceOwnerSessionValidator.getResourceOwnerSession(request);
        if (ssoToken != null) {
            try {
                ssoToken.setProperty(CSRF_SESSION_PROPERTY, token);
            } catch (SSOException e) {
                logger.warning("CsrfProtection: unable to store CSRF token as a session property, "
                        + "falling back to the double-submit cookie", e);
            }
        }

        issueCsrfCookie(request, token);
        return token;
    }

    /**
     * Checks if the request contains the required "csrf" parameter and that it matches the token bound to the
     * resource owner's authorization request (either the protected session property or the double-submit cookie).
     *
     * @param request The request.
     * @return {@code true} if the request is a CSRF attack, {@code false} if not.
     */
    public boolean isCsrfAttack(OAuth2Request request) {
        final String csrfValue = request.getParameter("csrf");
        if (StringUtils.isEmpty(csrfValue)) {
            return true;
        }

        // Stateful sessions: compare against the protected session property.
        SSOToken ssoToken = resourceOwnerSessionValidator.getResourceOwnerSession(request);
        if (ssoToken != null) {
            try {
                String stored = ssoToken.getProperty(CSRF_SESSION_PROPERTY);
                if (StringUtils.isNotEmpty(stored) && constantTimeEquals(stored, csrfValue)) {
                    return false;
                }
            } catch (SSOException e) {
                logger.warning("CsrfProtection: unable to read CSRF token from session", e);
            }
        }

        // Stateless sessions (and fallback): compare against the double-submit cookie.
        String cookieValue = readCsrfCookie(request);
        if (StringUtils.isNotEmpty(cookieValue) && constantTimeEquals(cookieValue, csrfValue)) {
            return false;
        }

        return true;
    }

    private void issueCsrfCookie(OAuth2Request request, String token) {
        HttpServletResponse response = getServletResponse();
        if (response == null) {
            return;
        }

        final boolean secure = isCookieSecure();
        final String cookieName = secure ? CSRF_COOKIE_SECURE : CSRF_COOKIE_PLAIN;
        
        StringBuilder sb = new StringBuilder(96);
        sb.append(cookieName).append('=').append(token);
        sb.append(";Path=/");
        if (secure) {
            sb.append(";Secure");
        }
        sb.append(";HttpOnly");
        String sameSite = CookieUtils.getCookieSameSite();
        sb.append(";SameSite=").append(sameSite == null ? "Lax" : sameSite);
        response.addHeader("Set-Cookie", sb.toString());
    }

    private String readCsrfCookie(OAuth2Request request) {
        String value = readCookie(request, CSRF_COOKIE_SECURE);
        // Only fall back to the unprefixed cookie on plain-HTTP deployments. On secure
        // deployments we must accept the "__Host-" cookie exclusively: that prefix is what
        // guarantees the cookie cannot be set over plain HTTP or from a sibling subdomain.
        // Accepting the unprefixed "oauth2_csrf" here would let an attacker who can write a
        // (non-Secure) cookie for the parent domain defeat the double-submit protection, so the
        // read side is kept consistent with issueCsrfCookie(), which also branches on isCookieSecure().
        if (value == null && !isCookieSecure()) {
            value = readCookie(request, CSRF_COOKIE_PLAIN);
        }
        return value;
    }

    /**
     * Reads the value of the named cookie from the underlying servlet request. Extracted as a seam so that
     * the double-submit logic can be unit-tested without static mocking.
     *
     * @param request The OAuth2 request.
     * @param name The cookie name.
     * @return The cookie value, or {@code null} if absent.
     */
    protected String readCookie(OAuth2Request request, String name) {
        HttpServletRequest servletRequest = getServletRequest(request);
        if (servletRequest == null) {
            return null;
        }
        return CookieUtils.getCookieValueFromReq(servletRequest, name);
    }

    /**
     * Whether cookies are issued with the {@code Secure} attribute (i.e. the deployment is HTTPS). Extracted as
     * a seam so that the double-submit logic can be unit-tested without static mocking.
     *
     * @return {@code true} if cookies are marked secure.
     */
    protected boolean isCookieSecure() {
        return CookieUtils.isCookieSecure();
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(UTF_8_CHARSET), b.getBytes(UTF_8_CHARSET));
    }

    private HttpServletRequest getServletRequest(OAuth2Request request) {
        Request restletRequest = request.getRequest();
        if (restletRequest != null) {
            return ServletUtils.getRequest(restletRequest);
        }
        return null;
    }

    private HttpServletResponse getServletResponse() {
        Response restletResponse = Response.getCurrent();
        if (restletResponse != null) {
            return ServletUtils.getResponse(restletResponse);
        }
        return null;
    }
}
