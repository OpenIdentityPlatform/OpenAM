/*
 * Copyright 2012-2014 ForgeRock AS.
 *
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
 * information: "Portions Copyrighted [2012] [ForgeRock Inc]".
 */

package org.forgerock.openam.oauth2.rest;

import javax.servlet.ServletException;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.VersionRouter;
import org.forgerock.openam.rest.DefaultVersionBehaviour;
import org.forgerock.openam.rest.router.VersionBehaviourConfigListener;
import org.forgerock.openam.rest.router.VersionedRouter;

/**
 * Connection factory provider for OAuth2Rest CREST HttpServlet.
 *
 * @see org.forgerock.json.resource.servlet.HttpServlet
 */
public class RestTokenDispatcher {

    public static ConnectionFactory getConnectionFactory() throws ServletException {
        try {
            final VersionRouter router = new VersionRouter();
            router.addRoute("/token/").addVersion("1.0", InjectorHolder.getInstance(TokenResource.class));
            router.addRoute("/client/").addVersion("1.0", InjectorHolder.getInstance(ClientResource.class));
            VersionBehaviourConfigListener.bindToServiceConfigManager(new VersionedRouterAdapter(router));
            return Resources.newInternalConnectionFactory(router);
        } catch (final Exception e) {
            throw new ServletException(e);
        }
    }

    /**
     * Adapts CREST {@link VersionRouter} to {@link VersionedRouter} for {@link VersionBehaviourConfigListener}.
     */
    private static final class VersionedRouterAdapter implements VersionedRouter<VersionRouter> {

        private final VersionRouter versionRouter;

        private VersionedRouterAdapter(VersionRouter versionRouter) {
            this.versionRouter = versionRouter;
        }

        @Override
        public VersionRouter setVersioning(DefaultVersionBehaviour behaviour) {
            switch(behaviour) {
                case LATEST:
                    versionRouter.setVersioningToDefaultToLatest();
                    break;
                case OLDEST:
                    versionRouter.setVersioningToDefaultToOldest();
                    break;
                case NONE:
                    versionRouter.setVersioningBehaviourToNone();
                    break;
            }
            return versionRouter;
        }

        @Override
        public VersionRouter setHeaderWarningEnabled(boolean warningEnabled) {
            //versionRouter.setWarningEnabled(warningEnabled); //todo: update when crest supports
            return versionRouter;
        }
    }

}
