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

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;
import org.forgerock.services.context.Context;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.openam.rest.RealmContext;

import javax.security.auth.Subject;

/**
 * Helper class to get around the fact that some CREST Contexts are final and have package private
 * constructors, i.e. UriRouterContext.
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
    public String getUserUid(Context context) {
        return getUserUid(context, getUserId(context));
    }

    /**
     * Gets the universal id for the given username.
     *
     * @param context The context.
     * @param username The username.
     * @return The users UID.
     */
    public String getUserUid(Context context, String username) {
        final AMIdentity identity = IdUtils.getIdentity(username, getRealm(context));

        if (identity == null) {
            return null;
        }

        return identity.getUniversalId();
    }

    /**
     * Gets the username for the user of the accessed resource.
     *
     * @param context The context.
     * @return The resource users username.
     */
    public String getUserId(Context context) {
        UriRouterContext routerContext = context.asContext(UriRouterContext.class);
        String userId = routerContext.getUriTemplateVariables().get("user");
        if (userId == null && !routerContext.isRootContext()
                && routerContext.getParent().containsContext(UriRouterContext.class)) {
            return getUserId(routerContext.getParent());
        }
        return userId;
    }

    /**
     * Gets the realm for the accessed resource.
     *
     * @param context The context.
     * @return The resource realm.
     */
    public String getRealm(Context context) {
        return context.asContext(RealmContext.class).getResolvedRealm();
    }

    /**
     * Gets the subject for the user of the accessed resource.
     *
     * @param context
     *         the context
     *
     * @return the subject attempting to access the resource
     */
    public Subject getSubject(Context context) {
        if (!context.containsContext(SubjectContext.class)) {
            return null;
        }

        return context.asContext(SubjectContext.class).getCallerSubject();
    }

}
