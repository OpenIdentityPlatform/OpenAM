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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.services.baseurl;

import jakarta.servlet.http.HttpServletRequest;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openam.utils.OpenAMSettings;

/**
 * Wraps a 3rd party implemented BaseURLProvider.
 */
public class ExtensionBaseURLProvider extends BaseURLProvider {
    private static final String EXTENSION_TYPE = "base-url-extension-class";
    private BaseURLProvider delegate;

    @Override
    protected String getBaseURL(HttpServletRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected String getBaseURL(HttpContext context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRootURL(HttpServletRequest request) {
        return delegate.getRootURL(request);
    }

    @Override
    public String getRootURL(HttpContext context) {
        return delegate.getRootURL(context);
    }

    @Override
    void setContextPath(String contextPath) {
        delegate.setContextPath(contextPath);
    }

    @Override
    void init(OpenAMSettings settings, String realm) {
        try {
            this.delegate = InjectorHolder.getInstance(
                    Class.forName(settings.getStringSetting(realm, EXTENSION_TYPE)).asSubclass(BaseURLProvider.class));
        } catch (Exception e) {
            throw new IllegalStateException("Could not initialise base URL provider for realm " + realm);
        }
    }
}
