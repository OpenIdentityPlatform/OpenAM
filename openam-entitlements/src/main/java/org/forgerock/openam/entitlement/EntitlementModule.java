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
 * Copyright 2014 ForgeRock, AS.
 */

package org.forgerock.openam.entitlement;

/**
 * Service provider interface for registering custom entitlement conditions and subjects. Uses the Java standard
 * {@link java.util.ServiceLoader} mechanism to allow extensions to be loaded.
 *
 * @since 12.0.0
 * @supported.all.api
 */
public interface EntitlementModule {

    /**
     * Register any custom entitlement conditions, subjects etc so that they are available for use with the RESTful
     * web services and UI.
     *
     * @param registry the entitlement component registry to register components with.
     */
    void registerCustomTypes(EntitlementRegistry registry);

}
