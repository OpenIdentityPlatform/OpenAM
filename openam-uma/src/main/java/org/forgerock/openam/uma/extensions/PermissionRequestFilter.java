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

package org.forgerock.openam.uma.extensions;

import java.util.Set;

import org.forgerock.oauth2.resources.ResourceSetDescription;

/**
 * Extension filter that will be called before permission request creation.
 *
 * <p>Implementations of this interface can use the Guice setter based injection.</p>
 *
 * @since 13.0.0
 */
public interface PermissionRequestFilter extends Comparable<PermissionRequestFilter> {

    /**
     * Invoked before a permission request is created.
     *
     * @param resourceSetDescription The resource set.
     * @param requestedScopes The requested scopes.
     * @param requestingClientId The requesting client id.
     */
    void onPermissionRequest(ResourceSetDescription resourceSetDescription, Set<String> requestedScopes,
            String requestingClientId);
}
