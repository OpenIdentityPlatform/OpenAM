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
 * Copyright 2014 ForgeRock Inc.
 */

package org.forgerock.openam.forgerockrest.utils;

import java.security.Principal;
import javax.security.auth.Subject;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.rest.resource.SubjectContext;

/**
 * A collection of ForgeRock-REST based utility functions that do not rely on an application SSO token existing.
 *
 * @since 12.0.0
 */
final public class PrincipalRestUtils {

    /**
     * Returns the authenticated principal associated with this context.
     *
     * @return the authenticated principal associated with this context, or null if not authenticated.
     */
    public static String getPrincipalNameFromServerContext(ServerContext context) {

        if (context == null || !context.containsContext(SubjectContext.class)) {
            return null;
        }

        Subject subject = context.asContext(SubjectContext.class).getCallerSubject();

        return getPrincipalNameFromSubject(subject);
    }

    /**
     * Returns the authenticated principal associated with this subject.
     *
     * @return the authenticated principal associated with this subject, or null if not authenticated.
     */
    public static String getPrincipalNameFromSubject(Subject subject) {
        if (subject == null) {
            return null;
        }

        Principal[] principalArray = (subject.getPrincipals().toArray(new Principal[subject.getPrincipals().size()]));
        if (principalArray.length == 0) {
            return null;
        }

        Principal principal = principalArray[0];

        return principal.getName();
    }

}
