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

import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;

import java.util.SortedSet;

/**
 * Model class for common tasks involved in configuring social authentication providers (Google, Facebook etc).
 *
 * @since 12.0.0
 */
public interface SocialAuthNModel extends AMModel {

    /**
     * Get a set of all configured realms, sorted lexicographically.
     *
     * @return the set of all realms.
     */
    SortedSet<String> getRealms() throws AMConsoleException;

    /**
     * The default URL to use for the OAuth2 redirect proxy. This makes a best-effort guess based on the server
     * configuration, but the user may have to override it to account for load balancers etc.
     * @return The default redirect URL.
     */
    String getDefaultRedirectUrl();

    /**
     * Indicates whether the provider has known settings or not. This controls whether we display additional fields.
     *
     * @return true if this is a known social authn provider or false if manual config is required.
     */
    boolean isKnownProvider();

    /**
     * Get the name of this social authn provider suitable for display to a user.
     *
     * @return the (possibly localized) name of the authn provider.
     */
    String getProviderDisplayName();

    /**
     * A localized message describing how to register with this particular social authentication provider. This is
     * localized per provider to allow more detailed help messages to be given. If the provider is not known then this
     * method returns null and some default message should be used.
     *
     * @return a localized help message for the provider, or {@code null} if provider unknown.
     */
    String getLocalizedProviderHelpMessage();
}
