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
package org.forgerock.openam.forgerockrest.utils;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.util.Reject;

/**
 * Utilities to help with logging.
 *
 * @since 12.0.0
 */
public final class LoggingUtils {

    /**
     * Logs an operation attempt on a resource, pulling the principal's name out of the context passed in,
     * and indicating in which realm the operation is requested (if appropriate).
     *
     * @param resource the resource being used. Not nullable.
     * @param operation the operation on the resource being performed. Not nullable.
     * @param context the context of the operation request. Not nullable.
     * @param realm the realm in which we are executing. Nullable.
     * @param debug the debug to write messages out to. Not nullable.
     * @return the name of the principal which requested the operation, or null if not available.
     */
    public static String logOperationAttemptAsPrincipal(String resource, String operation, ServerContext context,
                                                    String realm, Debug debug) {

        Reject.ifNull(resource, operation, context, debug);

        final String principalName = PrincipalRestUtils.getPrincipalNameFromServerContext(context);

        StringBuilder sb = new StringBuilder();
        sb.append(resource).append(" :: ").append(operation.toUpperCase());
        sb.append(" attempted by ");
        sb.append(principalName == null ? "[unknown]" : principalName);

        if (realm != null) {
            sb.append(" in realm ").append(realm);
        }

        debug.message(sb.toString());

        return principalName;
    }
}
