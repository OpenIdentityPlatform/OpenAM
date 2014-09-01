/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions copyright [year] [name of copyright owner]"
 */
package org.forgerock.openam;

import org.forgerock.openam.oauth2.OAuth2StatusService;
import org.forgerock.openam.oauth2.openid.OpenIDConnectConfiguration;
import org.forgerock.openam.oauth2.openid.OpenIDConnectDiscovery;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.routing.Router;

public class WebFinger extends Application {

    public WebFinger(){
        getMetadataService().setEnabled(true);
        getMetadataService().setDefaultMediaType(MediaType.APPLICATION_JSON);
        setStatusService(new OAuth2StatusService());
    }

    @Override
    public Restlet createInboundRoot() {
        Router root = new Router(getContext());

        /** TODO implement a generic webfinger handler
         * For now we only use webfinger for OpenID Connect. Once the standard is finalized
         * or we decide to use it for other tasks we dont need a full blown handler
         */
        root.attach("/webfinger", OpenIDConnectDiscovery.class);
        root.attach("/openid-configuration", OpenIDConnectConfiguration.class);

        return root;
    }
}
