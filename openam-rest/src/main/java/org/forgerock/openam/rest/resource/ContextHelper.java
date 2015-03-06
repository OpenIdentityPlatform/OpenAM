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

package org.forgerock.openam.rest.resource;

import com.sun.identity.idm.IdUtils;
import org.forgerock.json.resource.RouterContext;
import org.forgerock.json.resource.ServerContext;

/**
 * Helper class to get around the fact that some CREST Contexts are final and have package private
 * constructors, i.e. RouterContext.
 *
 * @since 13.0.0
 */
public class ContextHelper {

    /**
     * Gets the universal id for the user of the accessed resource.
     *
     * @param context The context.
     * @return The resource users UID.
     */
    public String getUserUid(ServerContext context) {
        return IdUtils.getIdentity(getUserId(context), getRealm(context)).getUniversalId();
    }

    /**
     * Gets the username for the user of the accessed resource.
     *
     * @param context The context.
     * @return The resource users username.
     */
    public String getUserId(ServerContext context) {
        return context.asContext(RouterContext.class).getUriTemplateVariables().get("user");
    }

    /**
     * Gets the realm for the accessed resource.
     *
     * @param context The context.
     * @return The resource realm.
     */
    public String getRealm(ServerContext context) {
        return context.asContext(RealmContext.class).getResolvedRealm();
    }
}
