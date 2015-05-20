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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.services.baseurl;

import javax.servlet.http.HttpServletRequest;

import org.forgerock.openam.utils.OpenAMSettings;

/**
 * A {@link BaseURLProvider} implementation that uses a fixed configured value.
 */
public class FixedBaseURLProvider extends BaseURLProvider {
    private static final String FIXED_VALUE_SETTING = "base-url-fixed-value";
    private String baseUrl;

    @Override
    protected String getBaseURL(HttpServletRequest request) {
        return baseUrl;
    }

    @Override
    void init(OpenAMSettings settings, String realm) {
        try {
            this.baseUrl = settings.getStringSetting(realm, FIXED_VALUE_SETTING);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot access fixed value setting for realm " + realm);
        }
    }
}
