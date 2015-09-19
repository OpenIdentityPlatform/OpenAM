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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openam.rest;

import org.forgerock.services.context.Context;
import org.forgerock.services.context.SecurityContext;

/**
 * This class contains method that help with getting information out of ServerContexts objects.
 */
public final class ServerContextHelper {

    /**
     * Gets the iPlanetDirectoryPro cookie from the Context.
     *
     * @param context The Context instance.
     * @return The cookie value or null.
     */
    public static String getCookieFromServerContext(Context context) {
        SecurityContext securityContext = context.asContext(SecurityContext.class);
        if (securityContext.getAuthorization() != null) {
            return (String) securityContext.getAuthorization().get("tokenId");
        }
        return null;
    }
}
