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
package org.forgerock.openam.rest.fluent;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.rest.resource.CrestRouter;
import org.forgerock.openam.rest.DefaultVersionBehaviour;
import org.forgerock.openam.rest.resource.CrestRealmRouter;
import org.forgerock.util.Reject;

/**
 * A Fluent version of the CrestRouter, which can be configured to run in dynamic mode.
 *
 * Dynamic mode is able to route requests to the appropriate endpoint for the requested realm.
 *
 * @see FluentRealmRouter
 * @since 12.0.0
 */
public class FluentRouter<T extends CrestRouter> extends CrestRouter<T> {

    /**
     * Fluent method which returns an instance of a FluentRealmRouter configured to be able to
     * route requests which are realm-specific to the appropriate endpoint.
     *
     * @return A Fluent, version-aware Router which delegates to a Realm-aware Router.
     */
    public FluentRealmRouter dynamically() {
        Reject.ifTrue(getDefaultRoute() != null, "Can only call dynamically once.");

        final CrestRealmRouter realmRouter = InjectorHolder.getInstance(CrestRealmRouter.class);
        setDefaultRoute(realmRouter);

        return new FluentRealmRouter() {

            @Override
            public FluentRoute route(String uriTemplate) {
                return new FluentRoute(realmRouter, uriTemplate);
            }

            @Override
            public FluentRealmRouter setVersioning(DefaultVersionBehaviour behaviour) {
                realmRouter.setVersioning(behaviour);
                return this;
            }

            @Override
            public FluentRealmRouter setHeaderWarningEnabled(boolean warningEnabled) {
                realmRouter.setHeaderWarningEnabled(warningEnabled);
                return this;
            }
        };

    }

    /**
     * {@inheritDoc}
     */
    public FluentRoute route(String uriTemplate) {
        return new FluentRoute(this, uriTemplate);
    }

}
