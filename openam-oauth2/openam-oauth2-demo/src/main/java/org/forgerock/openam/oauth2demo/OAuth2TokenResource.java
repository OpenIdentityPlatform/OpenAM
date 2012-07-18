/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
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
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.openam.oauth2demo;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.restlet.ext.oauth2.OAuth2;
import org.forgerock.restlet.ext.oauth2.consumer.OAuth2User;
import org.restlet.data.Reference;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.restlet.security.User;

import com.iplanet.am.util.SystemProperties;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public class OAuth2TokenResource extends ServerResource {
    /**
     * TODO Description.
     * 
     * @return TODO Description
     */
    @Get("json")
    public Representation getStatusInfo() {
        Map<String, Object> response = new HashMap<String, Object>();
        User u = getClientInfo().getUser();
        if (u instanceof OAuth2User) {
            OAuth2User user = (OAuth2User) u;
            Object o = getContext().getAttributes().get("org.forgerock.openam.oauth2demo");
            if (o instanceof URI) {
                Reference tokenInfo =
                        new Reference(SystemProperties
                                .get(OAuth2DemoApplication.OAUTH2_ENDPOINT_TOKENINFO));
                tokenInfo.addQueryParameter(OAuth2.Params.ACCESS_TOKEN, user.getAccessToken());
                response.put("uri", tokenInfo.toString());
            }
            response.put("name", user.getIdentifier());
            response.put("access_token", user.getAccessToken());
            response.put("scope", user.getScope());
        } else {
            response.put("user", getClientInfo().getUser() != null ? getClientInfo().getUser()
                    .getClass().getName() : "null");
        }
        return new JacksonRepresentation<Map>(response);
    }
}
