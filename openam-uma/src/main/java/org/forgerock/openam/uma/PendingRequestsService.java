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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.uma;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.sm.datalayer.impl.uma.UmaPendingRequest.*;
import static org.forgerock.openam.uma.UmaConstants.UmaPolicy.*;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.sm.datalayer.impl.uma.UmaPendingRequest;
import org.forgerock.openam.sm.datalayer.store.NotFoundException;
import org.forgerock.openam.sm.datalayer.store.ServerException;
import org.forgerock.openam.sm.datalayer.store.TokenDataStore;
import org.forgerock.openam.uma.audit.UmaAuditLogger;
import org.forgerock.openam.uma.audit.UmaAuditType;
import org.forgerock.services.context.Context;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Pair;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;

/**
 * Service class responsible for creating, reading, querying, approving and
 * denying UMA Pending Requests.
 *
 * @since 13.0.0
 */
public class PendingRequestsService {

    private final Debug debug = Debug.getInstance("UmaProvider");
    private final TokenDataStore<UmaPendingRequest> store;
    private final UmaAuditLogger auditLogger;
    private final UmaProviderSettingsFactory settingsFactory;
    private final UmaEmailService emailService;
    private final PendingRequestEmailTemplate pendingRequestEmailTemplate;
    private final UmaPolicyService policyService;
    private final BaseURLProviderFactory baseURLProviderFactory;
    private final CoreWrapper coreWrapper;

    /**
     * Constructs a new {@code PendingRequestsService} instance.
     *
     * @param store An instance of the UMA Pending Requests {@code TokenDataStore}.
     * @param auditLogger An instance of the {@code UmaAuditLogger}.
     * @param settingsFactory An instance of the {@code UmaProviderSettingsFactory}.
     * @param emailService An instance of the {@code UmaEmailService}.
     * @param pendingRequestEmailTemplate An instance of the {@code PendingRequestEmailTemplate}.
     * @param policyService An instance of the {@code UmaPolicyService}.
     * @param baseURLProviderFactory An instance of the {@code BaseUrlProviderFactory}.
     * @param coreWrapper An instance of the {@code CoreWrapper}.
     */
    @Inject
    public PendingRequestsService(@DataLayer(ConnectionType.UMA_PENDING_REQUESTS) TokenDataStore store,
            UmaAuditLogger auditLogger, UmaProviderSettingsFactory settingsFactory,
            UmaEmailService emailService,
            PendingRequestEmailTemplate pendingRequestEmailTemplate, UmaPolicyService policyService,
            BaseURLProviderFactory baseURLProviderFactory, CoreWrapper coreWrapper) {
        this.store = store;
        this.auditLogger = auditLogger;
        this.settingsFactory = settingsFactory;
        this.emailService = emailService;
        this.pendingRequestEmailTemplate = pendingRequestEmailTemplate;
        this.policyService = policyService;
        this.baseURLProviderFactory = baseURLProviderFactory;
        this.coreWrapper = coreWrapper;
    }

    /**
     * Creates a pending request.
     *
     * @param httpRequest The {@code HttpServletRequest}.
     * @param resourceSetId The resource set id.
     * @param resourceSetName The resource set name.
     * @param resourceOwnerId The resource owner id.
     * @param requestingPartyId The requesting party id.
     * @param realm The realm.
     * @param scopes The requested scopes.
     * @throws ServerException If the pending request
     * could not be created.
     */
    public void createPendingRequest(HttpServletRequest httpRequest, String resourceSetId, String resourceSetName,
            String resourceOwnerId,
            String requestingPartyId, String realm, Set<String> scopes) throws ServerException {

        UmaPendingRequest pendingRequest = new UmaPendingRequest(resourceSetId, resourceSetName, resourceOwnerId, realm,
                requestingPartyId, scopes);
        store.create(pendingRequest);

        if (isEmailResourceOwnerOnPendingRequestCreationEnabled(realm)) {
            Pair<String, String> template = pendingRequestEmailTemplate.getCreationTemplate(resourceOwnerId, realm);
            try {
                String scopesString = pendingRequestEmailTemplate.buildScopeString(scopes, resourceOwnerId, realm);
                String baseUrl = baseURLProviderFactory.get(realm).getRootURL(httpRequest);
                emailService.email(realm, resourceOwnerId, template.getFirst(),
                        MessageFormat.format(template.getSecond(), requestingPartyId, resourceSetName,
                                scopesString, baseUrl, pendingRequest.getId()));
            } catch (MessagingException e) {
                debug.warning("Pending Request Creation email could not be sent", e);
            }
        }
    }

    /**
     * Gets a pending request.
     *
     * @param id The id of the request.
     * @return The pending request.
     * @throws ResourceException If the pending request could not be read or does not exist.
     */
    public UmaPendingRequest readPendingRequest(String id) throws ResourceException {
        try {
            return store.read(id);
        } catch (NotFoundException e) {
            throw new org.forgerock.json.resource.NotFoundException("Pending request, " + id + ", not found", e);
        } catch (ServerException e) {
            throw new InternalServerErrorException("Failed to read pending request, " + id, e);
        }
    }

    /**
     * Queries pending requests for the specified {@literal resourceOwnerId} and {@literal realm}.
     *
     * @param resourceOwnerId The resource owner id.
     * @param realm The realm.
     * @return A {@code Set} of pending requests.
     * @throws ResourceException If the pending requests query failed.
     */
    public Set<UmaPendingRequest> queryPendingRequests(String resourceOwnerId, String realm) throws ResourceException {
        try {
            return store.query(QueryFilter.and(
                    QueryFilter.equalTo(RESOURCE_OWNER_ID_FIELD, resourceOwnerId),
                    QueryFilter.equalTo(REALM_FIELD, realm)));
        } catch (ServerException e) {
            throw new InternalServerErrorException("Failed to query pending request for resource owner, "
                    + resourceOwnerId, e);
        }
    }

    /**
     * Queries pending requests for the specified {@literal resourceSetId},
     * {@literal resourceOwnerId}, {@literal realm} and {@literal requestingPartyId}.
     *
     * @param resourceSetId The resource set id.
     * @param resourceOwnerId The resource owner id.
     * @param realm The realm.
     * @param requestingPartyId The requesting party id.
     * @return A {@code Set} of pending requests.
     * @throws ServerException If the pending requests query failed.
     */
    public Set<UmaPendingRequest> queryPendingRequests(String resourceSetId, String resourceOwnerId, String realm,
            String requestingPartyId) throws ServerException {
        return store.query(
                QueryFilter.and(
                        QueryFilter.equalTo(RESOURCE_SET_ID_FIELD, resourceSetId),
                        QueryFilter.equalTo(RESOURCE_OWNER_ID_FIELD, resourceOwnerId),
                        QueryFilter.equalTo(REALM_FIELD, realm),
                        QueryFilter.equalTo(REQUESTING_PARTY_ID_FIELD, requestingPartyId)
                ));
    }

    /**
     * Approves the pending request with the specified {@literal id}.
     *
     * @param context The request context.
     * @param id The pending request id.
     * @param content The content of the approval request.
     * @param realm The current realm.  @return {@code Promise} which is completed successfully or
     *              failed with a {@code ResourceException}.
     */
    public Promise<Void, ResourceException> approvePendingRequest(Context context, String id,
            JsonValue content, String realm) {
        try {
            final UmaPendingRequest request = store.read(id);
            Collection<String> scopes = getScopes(request, content);
            return createUmaPolicy(context, request, scopes)
                    .thenAsync(approvePendingRequest(request, scopes, id, realm));
        } catch (NotFoundException e) {
            return new org.forgerock.json.resource.NotFoundException("Pending request, " + id + ", not found", e)
                    .asPromise();
        } catch (ServerException e) {
            return new InternalServerErrorException("Failed to mark pending request, " + id + ", as approved", e)
                    .asPromise();
        }
    }

    private AsyncFunction<UmaPolicy, Void, ResourceException> approvePendingRequest(final UmaPendingRequest request,
            final Collection<String> scopes, final String id, final String realm) {
        return new AsyncFunction<UmaPolicy, Void, ResourceException>() {
            @Override
            public Promise<Void, ResourceException> apply(UmaPolicy value) {
                try {
                    if (isEmailRequestingPartyOnPendingRequestApprovalEnabled(realm)) {
                        Pair<String, String> template = pendingRequestEmailTemplate.getApprovalTemplate(
                                request.getRequestingPartyId(), realm);
                        try {
                            emailService.email(realm, request.getRequestingPartyId(), template.getFirst(),
                                    MessageFormat.format(template.getSecond(),
                                            request.getResourceOwnerId(), request.getResourceSetName(),
                                            pendingRequestEmailTemplate.buildScopeString(
                                                    scopes, request.getRequestingPartyId(),
                                                    realm)));
                        } catch (MessagingException e) {
                            debug.warning("Pending Request Approval email could not be sent", e);
                        }
                    }
                    store.delete(id);

                    AMIdentity resourceOwner = coreWrapper.getIdentity(request.getResourceOwnerId(), realm);
                    auditLogger.log(request.getResourceSetId(), request.getResourceSetName(),
                            resourceOwner, UmaAuditType.REQUEST_APPROVED, request.getRequestingPartyId());

                    return newResultPromise(null);
                } catch (NotFoundException e) {
                    return new org.forgerock.json.resource.NotFoundException(
                                    "Pending request, " + id + ", not found", e).asPromise();
                } catch (ServerException e) {
                    return new InternalServerErrorException(
                            "Failed to mark pending request, " + id + ", as approved", e).asPromise();
                }
            }
        };
    }

    private Collection<String> getScopes(UmaPendingRequest request, JsonValue content) {
        if (content != null && !content.isNull() && content.isDefined("scopes")) {
            return new HashSet<>(content.get("scopes").asList(String.class));
        } else {
            return new HashSet<>(request.getScopes());
        }
    }

    private Promise<UmaPolicy, ResourceException> createUmaPolicy(final Context context,
            final UmaPendingRequest request, final Collection<String> scopes) {
        return policyService.readPolicy(context, request.getResourceSetId())
                .thenAsync(new AsyncFunction<UmaPolicy, UmaPolicy, ResourceException>() {
                    @Override
                    public Promise<UmaPolicy, ResourceException> apply(UmaPolicy umaPolicy) {
                        JsonValue policyJson = umaPolicy.asJson();
                        JsonValue subjectPermission = null;
                        for (JsonValue permission : policyJson.get(PERMISSIONS_KEY)) {
                            if (permission.get(SUBJECT_KEY).asString().equals(request.getRequestingPartyId())) {
                                subjectPermission = permission;
                                break;
                            }
                        }
                        if (subjectPermission == null) {
                            subjectPermission = json(object(
                                    field(SUBJECT_KEY, request.getRequestingPartyId()),
                                    field(SCOPES_KEY, array())));
                            policyJson.get(PERMISSIONS_KEY).add(subjectPermission.getObject());
                        }

                        Set<String> subjectScopes =
                                new HashSet<>(subjectPermission.get(SCOPES_KEY).asCollection(String.class));
                        subjectScopes.addAll(scopes);
                        subjectPermission.put(SCOPES_KEY, subjectScopes);

                        return policyService.updatePolicy(context, request.getResourceSetId(), policyJson);
                    }
                }, new AsyncFunction<ResourceException, UmaPolicy, ResourceException>() {
                    @Override
                    public Promise<UmaPolicy, ResourceException> apply(ResourceException e) {
                        if (e instanceof org.forgerock.json.resource.NotFoundException) {
                            return policyService.createPolicy(context, createPolicyJson(request.getResourceSetId(),
                                    request.getRequestingPartyId(), scopes));
                        }
                        return e.asPromise();
                    }
                });
    }

    private JsonValue createPolicyJson(String resourceSetId, String requestingPartyId, Collection<String> scopes) {
        return json(object(
                field(POLICY_ID_KEY, resourceSetId),
                field(PERMISSIONS_KEY, array(
                        object(
                                field(SUBJECT_KEY, requestingPartyId),
                                field(SCOPES_KEY, scopes)
                        )
                ))));
    }

    /**
     * Denies the pending request with the specified {@literal id}.
     *
     * @param id The pending request id.
     * @param realm The current realm.
     * @throws ResourceException If the pending request is not found or could not be marked as denied.
     */
    public void denyPendingRequest(String id, String realm) throws ResourceException {
        try {
            UmaPendingRequest request = store.read(id);
            store.delete(id);
            AMIdentity resourceOwner = coreWrapper.getIdentity(request.getResourceOwnerId(), realm);
            auditLogger.log(request.getResourceSetId(), request.getResourceSetName(), resourceOwner,
                    UmaAuditType.REQUEST_DENIED, request.getRequestingPartyId());
        } catch (NotFoundException e) {
            throw new org.forgerock.json.resource.NotFoundException("Pending request, " + id + ", not found", e);
        } catch (ServerException e) {
            throw new InternalServerErrorException("Failed to mark pending request, " + id + ", as denied", e);
        }
    }

    private boolean isEmailResourceOwnerOnPendingRequestCreationEnabled(String realm) throws ServerException {
        try {
            return settingsFactory.get(realm).isEmailResourceOwnerOnPendingRequestCreationEnabled();
        } catch (org.forgerock.oauth2.core.exceptions.ServerException |
                org.forgerock.oauth2.core.exceptions.NotFoundException e) {
            throw new ServerException("Failed to read UMA Provider settings", e);
        }
    }

    private boolean isEmailRequestingPartyOnPendingRequestApprovalEnabled(String realm) throws ServerException {
        try {
            return settingsFactory.get(realm).isEmailRequestingPartyOnPendingRequestApprovalEnabled();
        } catch (org.forgerock.oauth2.core.exceptions.ServerException |
                org.forgerock.oauth2.core.exceptions.NotFoundException e) {
            throw new ServerException("Failed to read UMA Provider settings", e);
        }
    }
}
