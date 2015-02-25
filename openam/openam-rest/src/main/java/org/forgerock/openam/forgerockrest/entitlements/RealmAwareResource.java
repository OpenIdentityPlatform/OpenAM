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
package org.forgerock.openam.forgerockrest.entitlements;

import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.rest.resource.RealmContext;

/**
 * Resource capable of determining a realm from a ServerContext.
 *
 * @since 12.0.0
 */
public abstract class RealmAwareResource extends SubjectAwareResource {

    protected final String EMPTY = "";
    protected final String ROOT = "/";

    /**
     * Retrieves the Realm from a provided {@link org.forgerock.json.resource.ServerContext}.
     *
     * Returns null if there's no RealmContext in the provided ServerContext.
     *
     * @param context The request context.
     * @return a String containing the name of the realm associated with this request,
     *  or null if there is no RealmContext in the ServerContext.
     */
    protected String getRealm(ServerContext context) {

        if (!context.containsContext(RealmContext.class)) {
            return null;
        }

        final RealmContext rc = context.asContext(RealmContext.class);

        if (rc.getResolvedRealm().equals(EMPTY)) {
            return ROOT;
        }

        return rc.getResolvedRealm();
    }

}
