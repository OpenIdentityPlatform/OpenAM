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
package org.forgerock.openam.oauth2.openid;

import org.forgerock.openam.oauth2.provider.OAuth2ProviderSettings;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import java.util.HashMap;
import java.util.Map;

public class OpenIDConnectConfiguration extends ServerResource {

    public OpenIDConnectConfiguration(){

    }

    @Get
    public Representation getConfiguration(){
        Map<String, Object> response = new HashMap<String, Object>();
        OAuth2ProviderSettings settings = OAuth2Utils.getSettingsProvider(getRequest());
        response.put("version", settings.getOpenIDConnectVersion());
        response.put("issuer", settings.getOpenIDConnectIssuer());
        response.put("authorization_endpoint", settings.getAuthorizationEndpoint());
        response.put("token_endpoint", settings.getTokenEndpoint());
        response.put("userinfo_endpoint", settings.getUserInfoEndpoint());
        response.put("check_session_iframe", settings.getCheckSessionEndpoint());
        response.put("end_session_endpoint", settings.getEndSessionEndPoint());
        response.put("jwks_uri", settings.getJWKSUri());
        response.put("registration_endpoint", settings.getClientRegistrationEndpoint());
        response.put("claims_supported", settings.getSupportedClaims());
        response.put("response_types_supported", settings.getResponseTypes());
        response.put("subject_types_supported", settings.getSubjectTypesSupported());
        response.put("id_token_siging_alg_values_supported", settings.getTheIDTokenSigningAlgorithmsSupported());
        return new JsonRepresentation(response);
    }
}
