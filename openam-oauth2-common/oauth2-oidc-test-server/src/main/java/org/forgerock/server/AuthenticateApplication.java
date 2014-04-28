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

package org.forgerock.server;

import org.forgerock.common.SessionManager;
import org.forgerock.common.UserStore;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.oauth2.ResourceOwnerImpl;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.data.CookieSetting;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Redirector;
import org.restlet.routing.Router;

import java.io.IOException;
import java.util.UUID;

import static java.net.URLDecoder.decode;

/**
 * @since 12.0.0
 */
public class AuthenticateApplication extends Application {

    @Override
    public Restlet createInboundRoot() {

        final Context context = getContext();

        final Router router = new Router(context);

        router.attachDefault(AuthenticateResource.class);

        return router;
    }

    public static final class AuthenticateResource extends ServerResource {

        private final UserStore userStore;
        private final SessionManager sessionManager;

        public AuthenticateResource() {
            this.userStore = InjectorHolder.getInstance(UserStore.class);
            this.sessionManager = InjectorHolder.getInstance(SessionManager.class);
        }

        @Post
        public Representation authenticate(Representation entity) throws IOException {

            final String[] params = entity.getText().split("&");

            String username = null;
            String password = null;
            String gotoUrl = null;

            for (final String param : params) {
                final String[] split = param.split("=");
                if (split[0].equals("username")) {
                    username = split[1];
                    continue;
                }
                if (split[0].equals("password")) {
                    password = split[1];
                    continue;
                }
                if (split[0].equals("goto")) {
                    gotoUrl = split[1];
                    continue;
                }
            }

            final ResourceOwnerImpl resourceOwner = userStore.get(username);

            if (resourceOwner != null && resourceOwner.getPassword().equals(password)) {

                CookieSetting cS = new CookieSetting(0, "FR_OAUTH2_SESSION_ID",
                        sessionManager.create(resourceOwner.getId()));
                getResponse().getCookieSettings().add(cS);

                Redirector redirector = new Redirector(new Context(), decode(gotoUrl, "UTF-8"),
                        Redirector.MODE_CLIENT_PERMANENT);
                redirector.handle(getRequest(), getResponse());
                return new EmptyRepresentation();
            }

            return new StringRepresentation("Authentication Failed");
        }
    }
}
