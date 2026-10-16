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
package org.openidentityplatform.openam.federation.plugins;

import org.forgerock.openam.shared.security.whitelist.RedirectUrlValidator;

/**
 * Whether a URL supplied with a request may be redirected to once single sign-on has completed,
 * for protocols whose own configuration carries no allow-list (SAML 1.x {@code TARGET}, WS-Federation
 * {@code wreply}). The realm's <em>Valid goto URL</em> list decides, exactly as it does for the
 * login {@code goto} parameter: a relative URL always passes, a scheme other than http(s) never
 * does, and a realm that configures no list restricts nothing.
 */
public final class RealmGotoUrlValidator {

    private static final String ROOT_REALM = "/";
    private static final RedirectUrlValidator<String> DEFAULT_VALIDATOR =
            new RedirectUrlValidator<String>(new RealmGotoUrlExtractor());
    private static volatile RedirectUrlValidator<String> validator = DEFAULT_VALIDATOR;

    private RealmGotoUrlValidator() {
    }

    /**
     * @param url the URL taken from the request; {@code null} or empty is refused
     * @param realm the realm the session was created in; {@code null} or empty reads as the root realm
     * @return {@code true} if the realm's list admits {@code url}
     */
    public static boolean isValid(String url, String realm) {
        return validator.isRedirectUrlValid(url, realm == null || realm.isEmpty() ? ROOT_REALM : realm);
    }

    /** Test seam: {@code null} restores the validator that reads the realm configuration. */
    static void setValidator(RedirectUrlValidator<String> replacement) {
        validator = replacement == null ? DEFAULT_VALIDATOR : replacement;
    }
}
