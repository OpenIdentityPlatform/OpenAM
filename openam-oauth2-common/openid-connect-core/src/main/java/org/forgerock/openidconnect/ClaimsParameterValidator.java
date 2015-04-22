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
package org.forgerock.openidconnect;

import javax.inject.Inject;
import org.forgerock.oauth2.core.AuthorizeRequestValidator;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidScopeException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnsupportedResponseTypeException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Implements OIDC Spec, Section 5.5 -- ensures that any values sent in as the
 * claims parameter is legal.
 */
public class ClaimsParameterValidator implements AuthorizeRequestValidator {

    private final OAuth2ProviderSettingsFactory providerSettingsFactory;

    @Inject
    public ClaimsParameterValidator(OAuth2ProviderSettingsFactory providerSettingsFactory) {
        this.providerSettingsFactory = providerSettingsFactory;
    }

    @Override
    public void validateRequest(OAuth2Request request) throws InvalidClientException, InvalidRequestException,
            RedirectUriMismatchException, UnsupportedResponseTypeException, ServerException, BadRequestException,
            InvalidScopeException, NotFoundException {

        final OAuth2ProviderSettings settings = providerSettingsFactory.get(request);

        final String claims = request.getParameter(OAuth2Constants.Custom.CLAIMS);

        //if we aren't supporting this no need to validate
        if (!settings.getClaimsParameterSupported()) {
            return;
        }

        //if we support, but it's not requested, no need to validate
        if (claims == null) {
            return;
        }

        final JSONObject claimsJson;

        //convert claims into JSON object
        try {
            claimsJson = new JSONObject(claims);
        } catch (JSONException e) {
            throw new BadRequestException("Invalid JSON in supplied claims parameter.");
        }

        JSONObject userinfoClaims = null;

        try {
            userinfoClaims = claimsJson.getJSONObject(OAuth2Constants.UserinfoEndpoint.USERINFO);
        } catch (Exception e) {
            //fall through
        }

        //When the userinfo member is used, the request MUST also use a response_type value that
        //results in an Access Token being issued to the Client for use at the UserInfo Endpoint.
        if (userinfoClaims != null) {
            String responseType = request.getParameter(OAuth2Constants.Params.RESPONSE_TYPE);
            if (responseType != null && responseType.trim().equals(OAuth2Constants.JWTTokenParams.ID_TOKEN)) {
                throw new BadRequestException("Must request an access token when providing " +
                        "userinfo in claims parameter.");
            }
        }

    }
}