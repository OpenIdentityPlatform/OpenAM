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

package org.forgerock.openam.forgerockrest.entitlements;

import com.sun.identity.entitlement.EntitlementException;
import javax.security.auth.Subject;
import org.forgerock.json.resource.ServerContext;

/**
 * Abstract factory pattern for looking up {@link PolicyStore} implementations for a given realm and caller subject
 * (determined by server context).
 *
 * @since 12.0.0
 */
public interface PolicyStoreProvider {
    /**
     * Gets a policy store for the given realm.
     *
     * @param context the request context to get a policy store for.
     * @return a policy store for the given realm.
     */
    PolicyStore getPolicyStore(ServerContext context) throws EntitlementException;

    /**
     * Gets a policy store for the given realm.
     *
     * @param adminSubject Responsible for retrieving the store.
     * @param realm In which the store resides.
     * @return a policy store for the given realm.
     */
    PolicyStore getPolicyStore(Subject adminSubject, String realm) throws EntitlementException;
}
