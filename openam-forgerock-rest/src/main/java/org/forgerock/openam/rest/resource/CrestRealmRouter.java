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

package org.forgerock.openam.rest.resource;

import javax.inject.Inject;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.RouterContext;
import static org.forgerock.json.resource.RoutingMode.STARTS_WITH;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.rest.resource.CrestRouter;
import org.forgerock.openam.rest.router.RestRealmValidator;
import org.forgerock.openam.rest.router.VersionedRouter;

/**
 * A CREST request handler which will route to resource endpoints, dynamically handling realm URI parameters.
 *
 * This implementation ensures that the context passed along with the request to the endpoint contains
 * a {@link org.forgerock.openam.rest.resource.RealmContext}.
 *
 * @since 12.0.0
 */
public class CrestRealmRouter extends CrestRouter<CrestRealmRouter> implements VersionedRouter<CrestRealmRouter> {

    private final RestRealmValidator realmValidator;

    /**
     * Constructs a new RealmRouter instance.
     *
     * @param realmValidator An instance of the RestRealmValidator.
     */
    @Inject
    public CrestRealmRouter(RestRealmValidator realmValidator) {
        this.realmValidator = realmValidator;
        getRouter().addRoute(STARTS_WITH, "/{realm}", this);
    }

    /**
     * <p>Creates a or adds to the {@link org.forgerock.openam.rest.resource.RealmContext}.</p>
     *
     * <p>If multiple realm URI parameters are present then this method will be called repeatedly and for each call
     * the last realm URI parameter will be appended to the realm on the {@code RealmContext}.</p>
     *
     * @param context The context.
     * @return The augmented context.
     * @throws BadRequestException If the current full realm is not a valid realm.
     */
    @Override
    protected ServerContext transformContext(ServerContext context) throws BadRequestException {
        String realm = null;

        if (context.containsContext(RouterContext.class)) {
            realm = context.asContext(RouterContext.class).getUriTemplateVariables().get("realm");
        }

        if (realm == null) {
            realm = "/";
        }

        RealmContext realmContext;
        if (context.containsContext(RealmContext.class)) {
            realmContext = context.asContext(RealmContext.class);
            realmContext.addSubRealm(realm);
        } else {
            realmContext = new RealmContext(context, realm);
        }

        // Check that the path references an existing realm
        if (!realmValidator.isRealm(realmContext.getRealm())) {
            throw new BadRequestException("Invalid realm, " + realmContext.getRealm());
        }

        return realmContext;
    }

}
