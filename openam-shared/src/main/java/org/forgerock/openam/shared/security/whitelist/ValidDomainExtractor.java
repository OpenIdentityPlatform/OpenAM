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
package org.forgerock.openam.shared.security.whitelist;

import java.util.Collection;

/**
 * An interface to extract valid goto URL domains (or relay state URLs) from the OpenAM configuration.
 *
 * @param <T> The type of the object that holds key details about where exactly to look when retrieving the settings.
 */
public interface ValidDomainExtractor<T> {

    /**
     * Extracts the Valid goto URL domains based on the provided configuration details.
     *
     * @param configInfo The necessary information to retrieve the correct configuration.
     * @return The collection of valid goto URL domains based on the provided configuration information. May be null.
     */
    public Collection<String> extractValidDomains(final T configInfo);
}
