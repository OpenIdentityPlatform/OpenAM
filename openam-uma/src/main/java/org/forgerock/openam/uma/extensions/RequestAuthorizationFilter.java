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

import javax.security.auth.Subject;

import org.forgerock.openam.uma.PermissionTicket;
import org.forgerock.openam.uma.UmaException;

/**
 * Extension filter that will be called before request authorization and after
 * request authorization.
 *
 * <p>Implementations of this interface can use the Guice setter based injection.</p>
 *
 * @since 13.0.0
 */
public interface RequestAuthorizationFilter extends Comparable<RequestAuthorizationFilter> {

    /**
     * Invoked before authorization of the request is attempted.
     *
     * @param permissionTicket The permission ticket associated with the authorization request.
     * @param requestingParty The requesting party.
     * @param resourceOwner The resource owner.
     * @throws UmaException If authorization of the request should not be attempted.
     */
    void beforeAuthorization(PermissionTicket permissionTicket, Subject requestingParty, Subject resourceOwner)
            throws UmaException;

    /**
     * Invoked after a successful request authorization attempt.
     *
     * @param permissionTicket The permission ticket associated with the authorization request.
     * @param requestingParty The requesting party.
     * @param resourceOwner The resource owner.
     */
    void afterSuccessfulAuthorization(PermissionTicket permissionTicket, Subject requestingParty,
            Subject resourceOwner);

    /**
     * Invoked after a failed request authorization attempt.
     *
     * @param permissionTicket The permission ticket associated with the authorization request.
     * @param requestingParty The requesting party.
     * @param resourceOwner The resource owner.
     */
    void afterFailedAuthorization(PermissionTicket permissionTicket, Subject requestingParty,
            Subject resourceOwner);
}
