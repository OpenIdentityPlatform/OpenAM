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

package org.forgerock.openam.http;

import javax.inject.Provider;
import java.util.Set;

import org.forgerock.http.routing.Router;

/**
 * A provider interface that allows HTTP routes to be registered on the root
 * HTTP {@link Router}.
 *
 * @since 13.0.0
 */
public interface HttpRouteProvider extends Provider<Set<HttpRoute>> {

    /**
     * Gets the HTTP routes that should be registered on the root HTTP
     * {@link Router}.
     *
     * @return The root HTTP routes.
     */
    @Override
    Set<HttpRoute> get();
}
