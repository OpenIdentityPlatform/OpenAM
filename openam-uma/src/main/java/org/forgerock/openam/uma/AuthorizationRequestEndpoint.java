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

import static org.forgerock.json.fluent.JsonValue.*;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.JwtPrincipal;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.debug.Debug;
import org.apache.commons.lang.StringUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.oauth2.resources.ResourceSetStore;
import org.forgerock.openam.cts.api.fields.ResourceSetTokenField;
import org.forgerock.openam.uma.audit.UmaAuditLogger;
import org.forgerock.openam.uma.audit.UmaAuditType;
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

public class AuthorizationRequestEndpoint extends ServerResource {

    private final UmaProviderSettingsFactory umaProviderSettingsFactory;
    private final Debug debug = Debug.getInstance("UmaProvider");
    private final OAuth2RequestFactory<Request> requestFactory;
    private final TokenStore oauth2TokenStore;

    private static final String UNABLE_TO_RETRIEVE_TICKET_MESSAGE = "Unable to retrieve Permission Ticket";
    private final OAuth2ProviderSettingsFactory oauth2ProviderSettingsFactory;

    private final UmaAuditLogger auditLogger;

    /**
     * Constructs a new AuthorizationRequestEndpoint
     */
    @Inject
    public AuthorizationRequestEndpoint(UmaProviderSettingsFactory umaProviderSettingsFactory,
            TokenStore oauth2TokenStore, OAuth2RequestFactory<Request> requestFactory,
            OAuth2ProviderSettingsFactory oauth2ProviderSettingsFactory, UmaAuditLogger auditLogger) {
        this.umaProviderSettingsFactory = umaProviderSettingsFactory;
        this.requestFactory = requestFactory;
        this.oauth2TokenStore = oauth2TokenStore;
        this.oauth2ProviderSettingsFactory = oauth2ProviderSettingsFactory;
        this.auditLogger = auditLogger;
    }

    @Post
    public Representation requestAuthorization(JsonRepresentation entity) throws BadRequestException, UmaException, EntitlementException, ServerException, NotFoundException {
        UmaProviderSettings umaProviderSettings = umaProviderSettingsFactory.get(this.getRequest());
        JsonValue requestBody = json(toMap(entity));
        PermissionTicket permissionTicket = getPermissionTicket(umaProviderSettings.getUmaTokenStore(), requestBody);

        final AccessToken authorisationApiToken = getAuthorisationApiToken();

        if (hasExpired(permissionTicket)) {
            throw new UmaException(400, UmaConstants.EXPIRED_TICKET_ERROR_CODE, "The permission ticket has expired");
        }

        //Remove permission ticket so it cannot be re-used
        umaProviderSettings.getUmaTokenStore().deletePermissionTicket(permissionTicket.getId());

        final String requestingUserId = authorisationApiToken.getResourceOwnerId();
        final String resourceSetId = permissionTicket.getResourceSetId();
        final Request request = getRequest();
        final String resourceOwnerId = getResourceOwnerId(resourceSetId);

        auditLogger.log(resourceSetId, resourceOwnerId, UmaAuditType.REQUEST, request, requestingUserId);

        if (isEntitled(umaProviderSettings, permissionTicket, authorisationApiToken)) {
            getResponse().setStatus(new Status(200));
            auditLogger.log(resourceSetId, resourceOwnerId, UmaAuditType.GRANTED, request, requestingUserId);
            return createJsonRpt(umaProviderSettings.getUmaTokenStore(), permissionTicket, authorisationApiToken);
        } else {
            auditLogger.log(resourceSetId, resourceOwnerId, UmaAuditType.DENIED, request, requestingUserId);
            throw new UmaException(400, UmaConstants.NOT_AUTHORISED_ERROR_CODE, "The client is not authorised to " +
                    "access the requested resource set");
        }

        //TODO not sure where "need_info" error fits in....
    }

    private String getResourceOwnerId(String resourceSetId) throws NotFoundException, UmaException {
        OAuth2ProviderSettings providerSettings = oauth2ProviderSettingsFactory.get(requestFactory.create(getRequest()));
        ResourceSetDescription resourceSetDescription = getResourceSet(resourceSetId, providerSettings);
        return resourceSetDescription.getResourceOwnerId();
    }

    private boolean hasExpired(PermissionTicket permissionTicket) {
        return permissionTicket.getExpiryTime() < System.currentTimeMillis();
    }

    private boolean isEntitled(UmaProviderSettings umaProviderSettings, PermissionTicket permissionTicket,
            AccessToken authorisationApiToken) throws EntitlementException, ServerException {
        String realm = permissionTicket.getRealm();
        String resourceSetId = permissionTicket.getResourceSetId();
        String resourceName = UmaConstants.UMA_POLICY_SCHEME;
        try {
            ResourceSetStore store = oauth2ProviderSettingsFactory.get(requestFactory.create(getRequest()))
                    .getResourceSetStore();
            Set<ResourceSetDescription> results = store.query(
                    org.forgerock.util.query.QueryFilter.equalTo(ResourceSetTokenField.RESOURCE_SET_ID, resourceSetId));
            if (results.size() != 1) {
                throw new NotFoundException("Could not find Resource Set, " + resourceSetId);
            }
            resourceName += results.iterator().next().getId();
        } catch (NotFoundException e) {
            debug.message("Couldn't find resource that permission ticket is registered for", e);
            throw new ServerException("Couldn't find resource that permission ticket is registered for");
        }
        Subject subject = createSubject(authorisationApiToken.getResourceOwnerId(), realm);

        List<Entitlement> entitlements = umaProviderSettings.getPolicyEvaluator(subject).evaluate(realm, subject,
                resourceName, null, false);

        Set<String> requestedScopes = permissionTicket.getScopes();
        Set<String> requiredScopes = new HashSet<String>(requestedScopes);
        for (Entitlement entitlement : entitlements) {
            for (String requestedScope : requestedScopes) {
                final Boolean actionValue = entitlement.getActionValue(requestedScope);
                if (actionValue != null && actionValue) {
                    requiredScopes.remove(requestedScope);
                }
            }
        }

        return requiredScopes.isEmpty();
    }

    protected Subject createSubject(String username, String realm) {
        AMIdentity identity = IdUtils.getIdentity(username, realm);
        JwtPrincipal principal = new JwtPrincipal(json(object(field("sub", identity.getUniversalId()))));
        Set<Principal> principals = new HashSet<Principal>();
        principals.add(principal);
        return new Subject(false, principals, Collections.emptySet(), Collections.emptySet());
    }

    private Representation createJsonRpt(UmaTokenStore umaTokenStore, PermissionTicket permissionTicket, AccessToken authorisationApiToken) throws ServerException {
        RequestingPartyToken rpt = umaTokenStore.createRPT(authorisationApiToken, permissionTicket);
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("rpt", rpt.getId());
        return new JacksonRepresentation<Map<String, Object>>(response);
    }

    private PermissionTicket getPermissionTicket(UmaTokenStore umaTokenStore, JsonValue requestBody) throws BadRequestException, UmaException {
        try {
            return umaTokenStore.readPermissionTicket(getTicketId(requestBody));
        } catch (NotFoundException e) {
            throw new UmaException(400, UmaConstants.INVALID_TICKET_ERROR_CODE, UNABLE_TO_RETRIEVE_TICKET_MESSAGE);
        }
    }

    private String getTicketId(JsonValue requestBody) throws BadRequestException {
        final JsonValue ticket = requestBody.get("ticket");

        String ticketId = null;

        try {
            ticketId = ticket.asString();
        } catch (Exception e) {
            throw new BadRequestException(UNABLE_TO_RETRIEVE_TICKET_MESSAGE);
        }

        if (ticketId == null) {
            throw new BadRequestException(UNABLE_TO_RETRIEVE_TICKET_MESSAGE);
        }

        return ticketId;
    }

    protected AccessToken getAuthorisationApiToken() throws ServerException {
        Request req = getRequest();
        ChallengeResponse challengeResponse = req.getChallengeResponse();
        try {
            return oauth2TokenStore.readAccessToken(requestFactory.create(req),
                    challengeResponse.getRawValue());
        } catch (InvalidGrantException e) {
            throw new ServerException("Unable to verify client identity.");
        }
    }

    private Map<String, Object> toMap(JsonRepresentation entity) throws BadRequestException {
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
            throw new BadRequestException(e.getMessage());
        }
    }

    private ResourceSetDescription getResourceSet(String resourceSetId, OAuth2ProviderSettings providerSettings)
            throws UmaException {
        try {
            ResourceSetStore store = providerSettings.getResourceSetStore();
            Set<ResourceSetDescription> results = store.query(
                    org.forgerock.util.query.QueryFilter.equalTo(ResourceSetTokenField.RESOURCE_SET_ID, resourceSetId));
            if (results.size() != 1) {
                throw new UmaException(400, "invalid_resource_set_id", "Could not fing Resource Set, " + resourceSetId);
            }
            return results.iterator().next();
        } catch (ServerException e) {
            throw new UmaException(400, "invalid_resource_set_id", e.getMessage());
        }
    }

}
