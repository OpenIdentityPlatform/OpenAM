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

package org.forgerock.openam.rest;

import org.forgerock.json.resource.Router;

/**
 * A provider interface that allows CREST routes to be registered on the root
 * and realm REST {@link Router}.
 *
 * @since 13.0.0
 */
public interface RestRouteProvider {

    /**
     * Implementations to add route registrations to the provided routers.
     *
     * @param rootResourceRouter The root CREST router.
     * @param realmResourceRouter The realm CREST router.
     * @param rootServiceRouter The root CHF REST router.
     * @param realmServiceRouter The realm CHF REST router.
     */
    void addRoutes(ResourceRouter rootResourceRouter, ResourceRouter realmResourceRouter, ResourceRouter internalRouter,
            ServiceRouter rootServiceRouter, ServiceRouter realmServiceRouter);

}
