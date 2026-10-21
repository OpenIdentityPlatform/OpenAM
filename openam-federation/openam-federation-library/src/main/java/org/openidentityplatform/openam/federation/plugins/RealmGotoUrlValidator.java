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

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import jakarta.servlet.http.HttpServletRequest;
import org.forgerock.openam.shared.security.whitelist.FailSafeRedirectUrlValidator;
import org.forgerock.openam.shared.security.whitelist.ValidDomainExtractor;

/**
 * Whether a URL supplied with a request may be redirected to once single sign-on has completed,
 * for protocols whose own configuration carries no allow-list (SAML 1.x {@code TARGET}, the
 * WS-Federation relying party sign-in {@code wreply}). The realm's <em>Valid goto URL</em> list
 * decides, exactly as it does for the login {@code goto} parameter: a relative URL always passes,
 * a scheme other than http(s) never does, a populated list must match, and a realm that
 * configures no list accepts only the origins this server answers on - the origin of the request
 * and the URL OpenAM was installed under.
 * <p>
 * {@link Constants#GOTO_ALLOW_ANY_URL_WITHOUT_WHITELIST}, the escape hatch of the login
 * {@code goto}, decides what an empty list means here too, since it is the same list. A list that
 * could not be read is never taken for an empty one, whatever the property says.
 * <p>
 * Unlike the login {@code goto}, a base URL pinned through the realm's Base URL Source service is
 * not counted as an origin of this server: that service lives in {@code openam-core}, which the
 * federation library cannot reach.
 */
public final class RealmGotoUrlValidator {

    private static final String ROOT_REALM = "/";
    private static final FailSafeRedirectUrlValidator<String> DEFAULT_VALIDATOR =
            new GotoListValidator(new RealmGotoUrlExtractor());
    private static volatile FailSafeRedirectUrlValidator<String> validator = DEFAULT_VALIDATOR;

    private RealmGotoUrlValidator() {
    }

    /**
     * @param request the request being processed, used to establish the origins of this server
     * @param url the URL taken from the request; {@code null} or empty is refused
     * @param realm the realm the session was created in; {@code null} or empty reads as the root realm
     * @return {@code true} if the realm's list admits {@code url}, or it has none and {@code url}
     *         points back at this server
     */
    public static boolean isValid(HttpServletRequest request, String url, String realm) {
        return validator.isRedirectUrlValid(request, url, realm == null || realm.isEmpty() ? ROOT_REALM : realm);
    }

    /**
     * Test seam: reads the realm's list through {@code extractor} instead of the configuration;
     * {@code null} restores the validator that reads the realm configuration.
     */
    static void setExtractor(ValidDomainExtractor<String> extractor) {
        validator = extractor == null ? DEFAULT_VALIDATOR : new GotoListValidator(extractor);
    }

    /**
     * The fail-safe validator, with the escape hatch of the login {@code goto} for an empty list.
     */
    private static final class GotoListValidator extends FailSafeRedirectUrlValidator<String> {

        GotoListValidator(ValidDomainExtractor<String> extractor) {
            super(extractor);
        }

        @Override
        protected boolean allowAnyUrlWithoutWhitelist() {
            // Read on every call, so it can be changed without a restart. The validator only asks once it knows
            // the realm has no list, so a populated one - and a list that could not be read - still rules.
            return SystemPropertiesManager.getAsBoolean(Constants.GOTO_ALLOW_ANY_URL_WITHOUT_WHITELIST, false);
        }
    }
}
