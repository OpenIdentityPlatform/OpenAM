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

import javax.security.auth.Subject;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;

/**
 * Resource capable of determining a subject from a ServerContext.
 *
 * @since 12.0.0
 */
public abstract class SubjectAwareResource implements CollectionResourceProvider {

    /**
     * Retrieves the {@link javax.security.auth.Subject} from the {@link org.forgerock.json.resource.ServerContext}.
     * If there's no Subject or no SSOTokenContext in the provided context this method will return null.
     *
     * @param context Context of the request made to this resource.
     * @return an instance of the Subject in the context, or null.
     */
    protected Subject getContextSubject(ServerContext context) {

        if (!context.containsContext(SSOTokenContext.class)) {
            return null;
        }

        final SSOTokenContext sc = context.asContext(SSOTokenContext.class);
        return sc.getCallerSubject();
    }

}
