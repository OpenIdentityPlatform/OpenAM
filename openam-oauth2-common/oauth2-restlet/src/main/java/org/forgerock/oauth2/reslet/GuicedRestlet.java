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

package org.forgerock.oauth2.reslet;

import org.forgerock.guice.core.InjectorHolder;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.resource.Finder;
import org.restlet.resource.ServerResource;

/**
 * @since 12.0.0
 */
public class GuicedRestlet extends Finder {

    private final Class<? extends ServerResource> serverResource;

    public GuicedRestlet(final Class<? extends ServerResource> serverResource) {
        this.serverResource = serverResource;
    }

    @Override
    public ServerResource create(final Request request, final Response response) {
        return InjectorHolder.getInstance(serverResource);
    }
}
