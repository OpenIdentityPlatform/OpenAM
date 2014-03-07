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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.oauth2.openid;

import org.forgerock.openam.oauth2.provider.OAuth2ProviderSettings;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OpenIDConnectConfiguration extends ServerResource {

    public OpenIDConnectConfiguration(){

    }

    protected Set<String> getResponseTypes(Set<String> responseTypeSet){
        Set<String> responseTypes = new HashSet<String>();
        for (String responseType : responseTypeSet){
            String[] parts = responseType.split("\\|");
            if (parts.length != 2){
                continue;
            }
            responseTypes.add(parts[0]);
        }

        if (responseTypes.contains("code") && responseTypes.contains("token") &&
                responseTypes.contains("id_token")) {
            responseTypes.add("code token id_token");
        }

        if (responseTypes.contains("code") && responseTypes.contains("token")) {
            responseTypes.add("code token");
        }

        if (responseTypes.contains("code") && responseTypes.contains("id_token")) {
            responseTypes.add("code id_token");
        }

        if (responseTypes.contains("token") && responseTypes.contains("id_token")) {
            responseTypes.add("token id_token");
        }
        return responseTypes;
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
        response.put("response_types_supported", getResponseTypes(settings.getResponseTypes()));
        response.put("subject_types_supported", settings.getSubjectTypesSupported());
        response.put("id_token_signing_alg_values_supported", settings.getTheIDTokenSigningAlgorithmsSupported());
        return new JsonRepresentation(response);
    }
}
