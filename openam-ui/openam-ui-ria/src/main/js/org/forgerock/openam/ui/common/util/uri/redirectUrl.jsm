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
 * Copyright 2026 3A Systems, LLC.
 */

/**
 * Judges a URL the way the browser will read it when it is assigned to <code>window.location</code>. The URL is
 * resolved by the browser's own parser rather than matched as text, so that the forms a browser reads differently
 * from how they look - leading whitespace, a backslash for a slash, a scheme with no slashes - are judged by what
 * they resolve to.
 * @module org/forgerock/openam/ui/common/util/uri/redirectUrl
 */

function resolve (url, base) {
    if (typeof url !== "string") {
        return null;
    }
    try {
        return new URL(url, base);
    } catch (error) {
        return null;
    }
}

function isHttp (resolved) {
    return resolved !== null && (resolved.protocol === "http:" || resolved.protocol === "https:");
}

/**
 * @param {String} url The URL to judge, absolute or relative.
 * @param {String} [base=window.location.href] The URL a relative one is resolved against.
 * @returns {Boolean} Whether following the URL loads an http(s) page, which rules out script and data URLs.
 */
export function isHttpUrl (url, base = window.location.href) {
    return isHttp(resolve(url, base));
}

/**
 * @param {String} url The URL to judge, absolute or relative.
 * @param {String} [base=window.location.href] The URL a relative one is resolved against, whose origin it has to
 * keep.
 * @returns {Boolean} Whether following the URL loads an http(s) page of the same origin as the base.
 */
export function isSameOriginUrl (url, base = window.location.href) {
    const resolved = resolve(url, base);
    return isHttp(resolved) && resolved.origin === new URL(base).origin;
}
