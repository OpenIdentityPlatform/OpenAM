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

package org.forgerock.openam.uma;

import static org.forgerock.json.fluent.JsonValue.json;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.oauth2.resources.ResourceSetStore;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.json.JSONException;
import org.restlet.Request;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

/**
 * Restlet endpoint for UMA resource servers to register client attempts to access a protected resource.
 *
 * @since 13.0.0
 */
public class PermissionRequestEndpoint extends ServerResource {

    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final OAuth2RequestFactory<Request> requestFactory;
    private final TokenStore oauth2TokenStore;
    private final UmaProviderSettingsFactory umaProviderSettingsFactory;

    /**
     * Constructs a new PermissionRequestEndpoint instance
     *
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     * @param oauth2TokenStore An instance of the Oauth 2.0 TokenStore.
     * @param requestFactory An instance of the OAuth2RequestFactory.
     */
    @Inject
    public PermissionRequestEndpoint(OAuth2ProviderSettingsFactory providerSettingsFactory,
            OAuth2RequestFactory<Request> requestFactory, TokenStore oauth2TokenStore,
            UmaProviderSettingsFactory umaProviderSettingsFactory) {
        this.providerSettingsFactory = providerSettingsFactory;
        this.requestFactory = requestFactory;
        this.oauth2TokenStore = oauth2TokenStore;
        this.umaProviderSettingsFactory = umaProviderSettingsFactory;
    }

    /**
     * Registers the permission that the client requires for it to be able to access a protected resource.
     *
     * @param entity The permission request JSON body.
     * @return A JSON object containing the permission ticket.
     * @throws UmaException If the JSON request body is invalid or the requested resource set does not exist.
     */
    @Post
    public Representation registerPermissionRequest(JsonRepresentation entity) throws UmaException, NotFoundException,
            ServerException {
        JsonValue permissionRequest = json(toMap(entity));
        String resourceSetId = getResourceSetId(permissionRequest);
        String clientId = getClientId();
        OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(requestFactory.create(getRequest()));
        ResourceSetDescription resourceSetDescription = getResourceSet(resourceSetId, clientId, providerSettings);
        Set<String> scopes = validateScopes(permissionRequest, resourceSetDescription);
        String ticket = umaProviderSettingsFactory.get(getRequest()).getUmaTokenStore()
                .createPermissionTicket(resourceSetId, scopes, clientId).getId();
        return setResponse(201, Collections.<String, Object>singletonMap("ticket", ticket));
    }

    private String getResourceSetId(JsonValue permissionRequest) throws UmaException {
        JsonValue resource_set_id = permissionRequest.get("resource_set_id");
        try {
            resource_set_id.required();
        } catch (JsonValueException e) {
            throw new UmaException(400, "invalid_resource_set_id",
                    "Invalid Permission Request. Missing required attribute, 'resource_set_id'.");
        }
        if (!resource_set_id.isString()) {
            throw new UmaException(400, "invalid_resource_set_id",
                    "Invalid Permission Request. Required attribute, 'resource_set_id', must be a String.");
        }
        return resource_set_id.asString();
    }

    private Set<String> validateScopes(JsonValue permissionRequest, ResourceSetDescription resourceSetDescription)
            throws UmaException {

        Set<String> permissionScopes = getScopes(permissionRequest);

        if (!resourceSetDescription.getDescription().get("scopes").asSet(String.class).containsAll(permissionScopes)) {
            throw new UmaException(400, "invalid_scope",
                    "Requested scopes are not in allowed scopes for resource set.");
        }

        return permissionScopes;
    }

    private Set<String> getScopes(JsonValue permissionRequest) throws UmaException {
        try {
            permissionRequest.get("scopes").required();
        } catch (JsonValueException e) {
            throw new UmaException(400, "invalid_scope",
                    "Invalid Permission Request. Missing required attribute, 'scopes'.");
        }
        try {
            permissionRequest.get("scopes").asSet(String.class);
        } catch (JsonValueException e) {
            throw new UmaException(400, "invalid_scope",
                    "Invalid Permission Request. Required attribute, 'scopes', must be an array of Strings.");
        }
        return permissionRequest.get("scopes").asSet(String.class);
    }

    private ResourceSetDescription getResourceSet(String resourceSetId, String clientId, OAuth2ProviderSettings providerSettings) throws UmaException {
        try {
            ResourceSetStore store = providerSettings.getResourceSetStore();
            return store.read(resourceSetId);
        } catch (NotFoundException e) {
            throw new UmaException(400, "invalid_resource_set_id", e.getMessage());
        } catch (ServerException e) {
            throw new UmaException(400, "invalid_resource_set_id", e.getMessage());
        }
    }

    private String getClientId() throws ServerException {
        Request req = getRequest();
        ChallengeResponse challengeResponse = req.getChallengeResponse();
        try {
            AccessToken accessToken = oauth2TokenStore.readAccessToken(requestFactory.create(req),
                    challengeResponse.getRawValue());
            return accessToken.getClientId();
        } catch (InvalidGrantException e) {
            throw new ServerException("Unable to verify client identity.");
        }
    }

    private Representation setResponse(int statusCode, Map<String, Object> entity) {
        getResponse().setStatus(new Status(statusCode));
        return new JacksonRepresentation<Map<String, Object>>(entity);
    }

    private Map<String, Object> toMap(JsonRepresentation entity) throws UmaException {
        if (entity == null) {
            return Collections.emptyMap();
        }

        try {
            final String jsonString = entity.getJsonObject().toString();
            if (StringUtils.isNotEmpty(jsonString)) {
                JsonValue jsonContent = JsonValueBuilder.toJsonValue(jsonString);
                return jsonContent.asMap(Object.class);
            }

            return Collections.emptyMap();
        } catch (JSONException e) {
            throw new UmaException(400, "invalid_resource_set_id", e.getMessage());
        }
    }
}
