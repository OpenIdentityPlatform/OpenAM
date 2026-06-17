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
 * Portions copyright 2021-2026 3A Systems, LLC.
 */

/**
 * The Session Token (tokenId) used by OpenAM to track an authenticated session.
 * @module org/forgerock/openam/ui/user/login/tokens/SessionToken
 */

import CookieHelper from "org/forgerock/commons/ui/common/util/CookieHelper";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";

/**
 * Sentinel value returned by {@link get} when the OpenAM session cookie is configured as
 * <code>HttpOnly</code> and therefore cannot be read by JavaScript. In that case the real token
 * value is held by the browser in the HttpOnly cookie and is sent automatically with every
 * (same-origin, credentialed) request. The server resolves the token from the cookie/header when
 * no <code>tokenId</code> is supplied, so this sentinel is enough for the XUI to know that a
 * session may exist.
 * @type {String}
 */
export const HTTP_ONLY_TOKEN = "HTTP_ONLY_SESSION_TOKEN";

/**
 * Retains the token value during the lifetime of the page when the session cookie is HttpOnly.
 * This allows JavaScript to use the real token immediately after authentication, even though it
 * cannot be read back from the (HttpOnly) cookie.
 */
let inMemoryToken;

function cookieName () {
    return Configuration.globalData.auth.cookieName;
}

function cookieDomains () {
    return Configuration.globalData.auth.cookieDomains;
}

function secureCookie () {
    return Configuration.globalData.secureCookie;
}

function cookieSameSite () {
    return Configuration.globalData.auth.cookieSameSite;
}

/**
 * Whether the OpenAM session cookie is configured to be HttpOnly. When true the session cookie
 * cannot be read or written from JavaScript and is managed entirely by the server.
 * @returns {Boolean} true if the session cookie is HttpOnly.
 */
export function isHttpOnly () {
    const httpOnly = Configuration.globalData.cookieHttpOnly;
    return httpOnly === true || httpOnly === "true";
}

/**
 * Whether the supplied token is a real (JavaScript readable) token value, as opposed to the
 * {@link HTTP_ONLY_TOKEN} sentinel used when the session cookie is HttpOnly.
 * @param {String} token The token to test.
 * @returns {Boolean} true if the token value is usable as a tokenId on the client side.
 */
export function isResolvable (token) {
    return Boolean(token) && token !== HTTP_ONLY_TOKEN;
}

/**
 * Whether the supplied <code>/json/authenticate</code> response represents a completed (successful)
 * authentication.
 * <p>
 * Normally a completion is detected by the presence of a <code>tokenId</code> in the response body.
 * However, when the session cookie is <code>HttpOnly</code> the server delivers the token solely via
 * the <code>Set-Cookie</code> header and does NOT echo <code>tokenId</code> in the body (so an XSS on
 * the origin cannot read a replayable token). In that case a completion is instead indicated by the
 * presence of a <code>successUrl</code> with no further callbacks (no <code>authId</code>) to satisfy.
 * @param {Object} response The parsed authenticate response.
 * @returns {Boolean} true if the response represents a successful authentication.
 */
export function isAuthenticated (response) {
    if (!response) {
        return false;
    }
    if (Object.prototype.hasOwnProperty.call(response, "tokenId")) {
        return true;
    }
    return isHttpOnly() &&
        Object.prototype.hasOwnProperty.call(response, "successUrl") &&
        !Object.prototype.hasOwnProperty.call(response, "authId");
}

export function set (token) {
    if (isHttpOnly()) {
        // The server is responsible for setting the HttpOnly session cookie. JavaScript cannot
        // write it, so we only retain the value in memory for the lifetime of the page.
        inMemoryToken = token;
        return token;
    }
    return CookieHelper.setCookie(cookieName(), token, "", "/", cookieDomains(), secureCookie(), cookieSameSite());
}

export function get () {
    if (isHttpOnly()) {
        // Return the token retained in memory if available (e.g. straight after authentication),
        // otherwise a sentinel so that callers know a session may exist. The real value is held in
        // the HttpOnly cookie and is resolved server-side from the request.
        return inMemoryToken || HTTP_ONLY_TOKEN;
    }
    return CookieHelper.getCookie(cookieName());
}

export function remove () {
    inMemoryToken = undefined;
    if (isHttpOnly()) {
        // An HttpOnly cookie cannot be removed from JavaScript; the server clears it on logout.
        return undefined;
    }
    return CookieHelper.deleteCookie(cookieName(), "/", cookieDomains());
}
