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
import org.forgerock.openam.utils.StringUtils;

/**
 * Provides the base URL for the OpenAM instance. Subclasses may use the HttpServletRequest
 * to deduce the URL, or work it out by some other means.
 */
public abstract class BaseURLProvider {

    private String contextPath;

    void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    /**
     * The implementation of getting the base URL without the context path, which will be added if configured.
     * @param request The servlet request.
     * @return The base URL.
     */
    protected abstract String getBaseURL(HttpServletRequest request);

    /**
     * Initialise the provider from the settings.
     * @param settings The settings access object.
     * @param realm The realm that is being configured.
     */
    abstract void init(OpenAMSettings settings, String realm);

    /**
     * Gets the base URL to use in the response to the request.
     * @param request The current request.
     * @return The base URL. This will never end in a / character.
     */
    public String getURL(HttpServletRequest request) {
        String baseUrl = getBaseURL(request);
        if (StringUtils.isNotBlank(contextPath)) {
            baseUrl += contextPath;
        }
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

}
