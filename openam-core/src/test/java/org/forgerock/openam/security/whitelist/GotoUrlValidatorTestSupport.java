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
package org.forgerock.openam.security.whitelist;

import jakarta.servlet.http.HttpServletRequest;
import org.forgerock.openam.shared.security.whitelist.ValidDomainExtractor;

/**
 * Lets the tests of the REST endpoints put a goto URL validator of their own behind
 * {@link GotoUrlValidator#getRedirectUrl}, so they can give a realm a Valid goto URL Resources list without a
 * configuration store. It sits in the validator's package because the seam it reaches is package private.
 */
public final class GotoUrlValidatorTestSupport {

    private GotoUrlValidatorTestSupport() {
    }

    /**
     * Puts a validator reading its lists through the given extractor, with no Base URL Source, behind the static
     * entry points.
     *
     * @param extractor The source of each realm's goto URL list.
     */
    public static void install(ValidDomainExtractor<String> extractor) {
        GotoUrlValidator.setInstance(new GotoUrlValidator(extractor, new GotoUrlValidator.BaseUrlSupplier() {
            @Override
            public String getBaseUrl(HttpServletRequest request, String realm) {
                return null;
            }
        }));
    }

    /**
     * Restores the validator that reads the configuration.
     */
    public static void restore() {
        GotoUrlValidator.setInstance(null);
    }
}
