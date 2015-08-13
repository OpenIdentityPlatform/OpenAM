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

import static org.forgerock.json.JsonValue.*;

import javax.security.auth.Subject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.debug.Debug;
import org.apache.commons.lang.StringUtils;
import org.forgerock.json.JsonValue;
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
import org.forgerock.openam.sm.datalayer.impl.uma.UmaPendingRequest;
import org.forgerock.openam.uma.audit.UmaAuditLogger;
import org.forgerock.openam.uma.audit.UmaAuditType;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.json.JSONException;
import org.restlet.Request;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorizationRequestEndpoint extends ServerResource {

    private final Logger logger = LoggerFactory.getLogger("UmaProvider");
    private final UmaProviderSettingsFactory umaProviderSettingsFactory;
    private final Debug debug = Debug.getInstance("UmaProvider");
    private final OAuth2RequestFactory<Request> requestFactory;
    private final TokenStore oauth2TokenStore;

    private static final String UNABLE_TO_RETRIEVE_TICKET_MESSAGE = "Unable to retrieve Permission Ticket";
    private final OAuth2ProviderSettingsFactory oauth2ProviderSettingsFactory;

    private final UmaAuditLogger auditLogger;
    private final PendingRequestsService pendingRequestsService;
    private final Map<String, ClaimGatherer> claimGatherers;

    /**
     * Constructs a new AuthorizationRequestEndpoint
     */
    @Inject
    public AuthorizationRequestEndpoint(UmaProviderSettingsFactory umaProviderSettingsFactory,
            TokenStore oauth2TokenStore, OAuth2RequestFactory<Request> requestFactory,
            OAuth2ProviderSettingsFactory oauth2ProviderSettingsFactory, UmaAuditLogger auditLogger,
            PendingRequestsService pendingRequestsService, Map<String, ClaimGatherer> claimGatherers) {
        this.umaProviderSettingsFactory = umaProviderSettingsFactory;
        this.requestFactory = requestFactory;
        this.oauth2TokenStore = oauth2TokenStore;
        this.oauth2ProviderSettingsFactory = oauth2ProviderSettingsFactory;
        this.auditLogger = auditLogger;
        this.pendingRequestsService = pendingRequestsService;
        this.claimGatherers = claimGatherers;
    }

    @Post
    public Representation requestAuthorization(JsonRepresentation entity) throws BadRequestException, UmaException,
            EntitlementException, ServerException, NotFoundException {
        UmaProviderSettings umaProviderSettings = umaProviderSettingsFactory.get(this.getRequest());
        JsonValue requestBody = json(toMap(entity));
        PermissionTicket permissionTicket = getPermissionTicket(umaProviderSettings.getUmaTokenStore(), requestBody);

        if (hasExpired(permissionTicket)) {
            throw new UmaException(400, UmaConstants.EXPIRED_TICKET_ERROR_CODE, "The permission ticket has expired");
        }

        //Remove permission ticket so it cannot be re-used
        umaProviderSettings.getUmaTokenStore().deletePermissionTicket(permissionTicket.getId());

        final String resourceSetId = permissionTicket.getResourceSetId();
        final Request request = getRequest();
        final String resourceOwnerId = getResourceOwnerId(resourceSetId);

        String requestingPartyId = null;
        try {
            requestingPartyId = getRequestingPartyId(umaProviderSettings, requestBody);
        } finally {
            auditLogger.log(resourceSetId, resourceOwnerId, UmaAuditType.REQUEST, request,
                    requestingPartyId == null ? getAuthorisationApiToken().getResourceOwnerId() : requestingPartyId);
        }

        if (isEntitled(umaProviderSettings, permissionTicket, requestingPartyId)) {
            getResponse().setStatus(new Status(200));
            auditLogger.log(resourceSetId, resourceOwnerId, UmaAuditType.GRANTED, request, requestingPartyId);
            return createJsonRpt(umaProviderSettings.getUmaTokenStore(), permissionTicket);
        } else {
            try {
                if (verifyPendingRequestDoesNotAlreadyExist(resourceSetId, resourceOwnerId, permissionTicket.getRealm(),
                        requestingPartyId, permissionTicket.getScopes())) {
                    auditLogger.log(resourceSetId, resourceOwnerId, UmaAuditType.DENIED, request, requestingPartyId);
                    throw new UmaException(403, UmaConstants.NOT_AUTHORISED_ERROR_CODE,
                            "The client is not authorised to access the requested resource set");
                } else {
                    pendingRequestsService.createPendingRequest(ServletUtils.getRequest(getRequest()), resourceSetId,
                            auditLogger.getResourceName(resourceSetId, request), resourceOwnerId, requestingPartyId,
                            permissionTicket.getRealm(), permissionTicket.getScopes());
                    auditLogger.log(resourceSetId, resourceOwnerId, UmaAuditType.REQUEST_SUBMITTED, request, requestingPartyId);
                }
            } catch (org.forgerock.openam.sm.datalayer.store.ServerException e) {
                logger.error("Failed to create pending request", e);
                throw new UmaException(403, UmaConstants.NOT_AUTHORISED_ERROR_CODE, "Failed to create pending request");
            }
            throw newRequestSubmittedException();
        }
    }

    private String getRequestingPartyId(UmaProviderSettings umaProviderSettings, JsonValue requestBody)
            throws ServerException, NotFoundException, UmaException {
        if (requestBody.isDefined("claim_tokens")) {
            for (JsonValue claimToken : requestBody.get("claim_tokens")) {
                String format = claimToken.get("format").asString();
                ClaimGatherer claimGatherer = claimGatherers.get(format);
                if (claimGatherer == null) {
                    continue;
                }
                String requestingPartyId = claimGatherer.getRequestingPartyId(requestFactory.create(getRequest()),
                        getAuthorisationApiToken(), claimToken.get("token"));
                if (requestingPartyId != null) {
                    return requestingPartyId;
                }
            }
        }
        // Cannot rely on AAT for requesting party if trust elevation is required
        if (umaProviderSettings.isTrustElevationRequired()) {
            throw newNeedInfoException();
        }
        // Default to using AAT
        return getAuthorisationApiToken().getResourceOwnerId();
    }

    private UmaException newNeedInfoException() throws NotFoundException, ServerException {
        List<Object> requiredClaims = new ArrayList<>();
        for (ClaimGatherer claimGatherer : claimGatherers.values()) {
            requiredClaims.add(claimGatherer.getRequiredClaimsDetails(getIssuer()).getObject());
        }
        JsonValue requestingPartyClaims = json(object(
                field("requesting_party_claims", requiredClaims)));
        return new UmaException(403, UmaConstants.NEED_INFO_ERROR_CODE,
                "Additional information is required to determine whether the client is authorized to access "
                        + "the requested resource.").setDetail(requestingPartyClaims);
    }

    private boolean verifyPendingRequestDoesNotAlreadyExist(String resourceSetId, String resourceOwnerId,
            String realm, String requestingUserId, Set<String> scopes)
            throws org.forgerock.openam.sm.datalayer.store.ServerException, UmaException {
        Set<UmaPendingRequest> pendingRequests = pendingRequestsService.queryPendingRequests(resourceSetId,
                resourceOwnerId, realm, requestingUserId);
        if (!pendingRequests.isEmpty()) {
            for (UmaPendingRequest pendingRequest : pendingRequests) {
                if (pendingRequest.getScopes().containsAll(scopes)) {
                    throw newRequestSubmittedException();
                }
            }
        }

        return false;
    }

    private UmaException newRequestSubmittedException() {
        return new UmaException(403, UmaConstants.REQUEST_SUBMITTED_ERROR_CODE,
                "The client is not authorised to access the requested resource set. A request has "
                        + "been submitted to the resource owner requesting access to the resource");
    }

    private String getResourceOwnerId(String resourceSetId) throws NotFoundException, UmaException {
        OAuth2ProviderSettings providerSettings = oauth2ProviderSettingsFactory.get(requestFactory.create(getRequest()));
        ResourceSetDescription resourceSetDescription = getResourceSet(resourceSetId, providerSettings);
        return resourceSetDescription.getResourceOwnerId();
    }

    private String getIssuer() throws NotFoundException, ServerException {
        OAuth2ProviderSettings providerSettings = oauth2ProviderSettingsFactory.get(requestFactory.create(getRequest()));
        return providerSettings.getIssuer();
    }

    private boolean hasExpired(PermissionTicket permissionTicket) {
        return permissionTicket.getExpiryTime() < System.currentTimeMillis();
    }

    private boolean isEntitled(UmaProviderSettings umaProviderSettings, PermissionTicket permissionTicket,
            String requestingPartyId) throws EntitlementException, ServerException {
        String realm = permissionTicket.getRealm();
        String resourceSetId = permissionTicket.getResourceSetId();
        String resourceName = UmaConstants.UMA_POLICY_SCHEME;
        Subject resourceOwnerSubject;
        try {
            ResourceSetStore store = oauth2ProviderSettingsFactory.get(requestFactory.create(getRequest()))
                    .getResourceSetStore();
            Set<ResourceSetDescription> results = store.query(
                    org.forgerock.util.query.QueryFilter.equalTo(ResourceSetTokenField.RESOURCE_SET_ID, resourceSetId));
            if (results.size() != 1) {
                throw new NotFoundException("Could not find Resource Set, " + resourceSetId);
            }
            resourceName += results.iterator().next().getId();
            resourceOwnerSubject = UmaUtils.createSubject(createIdentity(results.iterator().next().getResourceOwnerId(), realm));
        } catch (NotFoundException e) {
            debug.message("Couldn't find resource that permission ticket is registered for", e);
            throw new ServerException("Couldn't find resource that permission ticket is registered for");
        }
        Subject requestingPartySubject = UmaUtils.createSubject(createIdentity(requestingPartyId, realm));

        // Implicitly grant access to the resource owner
        if (isRequestingPartyResourceOwner(requestingPartySubject, resourceOwnerSubject)) {
            return true;
        }

        List<Entitlement> entitlements = umaProviderSettings.getPolicyEvaluator(requestingPartySubject,
                permissionTicket.getClientId().toLowerCase())
                .evaluate(realm, requestingPartySubject, resourceName, null, false);

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

    private boolean isRequestingPartyResourceOwner(Subject requestingParty, Subject resourceOwner) {
        return resourceOwner.equals(requestingParty);
    }

    protected AMIdentity createIdentity(String username, String realm) {
        return IdUtils.getIdentity(username, realm);
    }

    private Representation createJsonRpt(UmaTokenStore umaTokenStore, PermissionTicket permissionTicket) throws ServerException {
        RequestingPartyToken rpt = umaTokenStore.createRPT(permissionTicket);
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
        } catch (NotFoundException e) {
            throw new ServerException(e.getMessage());
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
