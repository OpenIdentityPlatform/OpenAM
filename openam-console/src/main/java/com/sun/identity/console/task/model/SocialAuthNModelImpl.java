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
 * Copyright 2014 ForgeRock AS.
 */

package com.sun.identity.console.task.model;

import com.sun.identity.console.base.AMConsoleConfig;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModelBase;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Default implementation of the social authentication model.
 */
public class SocialAuthNModelImpl extends AMModelBase implements SocialAuthNModel {
    private static final String PROVIDER_NAME_PREFIX = "social.authentication.provider.";
    private static final String PROVIDER_HELP_PREFIX = "configure.social.authentication.help.";
    private static final String SOCIAL_AUTHN_PROVIDERS_KEY = "socialAuthNProviders";

    private static final Set<String> KNOWN_PROVIDERS = loadKnownProviders();

    private final String provider;

    public SocialAuthNModelImpl(final HttpServletRequest req, final Map map) {
        super(req, map);
        this.provider = req.getParameter("type");
    }

    public SortedSet<String> getRealms() throws AMConsoleException {
        final SortedSet<String> realms = new TreeSet<String>(super.getRealmNames("/", "*"));
        realms.add("/");
        return realms;
    }

    public String getDefaultRedirectUrl() {
        return AMConsoleConfig.SERVER_URL + AMConsoleConfig.SERVER_DEPLOYMENT_URI + "/oauth2c/OAuthProxy.jsp";
    }

    public boolean isKnownProvider() {
        return KNOWN_PROVIDERS.contains(provider);
    }

    public String getProviderDisplayName() {
        return provider == null ? "Unknown" : getLocalizedString(PROVIDER_NAME_PREFIX + provider);
    }

    public String getLocalizedProviderHelpMessage() {
        return provider == null ? null : getLocalizedString(PROVIDER_HELP_PREFIX + provider);
    }

    private static Set<String> loadKnownProviders() {
        return ResourceBundle.getBundle(SOCIAL_AUTHN_PROVIDERS_KEY).keySet();
    }
}
