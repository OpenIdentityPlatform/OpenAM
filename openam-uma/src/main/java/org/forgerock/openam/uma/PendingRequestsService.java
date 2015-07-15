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
import static org.forgerock.openam.sm.datalayer.impl.uma.UmaPendingRequest.*;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.inject.Inject;
import javax.mail.MessagingException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Set;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.sm.datalayer.impl.uma.UmaPendingRequest;
import org.forgerock.openam.sm.datalayer.store.NotFoundException;
import org.forgerock.openam.sm.datalayer.store.ServerException;
import org.forgerock.openam.sm.datalayer.store.TokenDataStore;
import org.forgerock.openam.uma.audit.UmaAuditLogger;
import org.forgerock.openam.uma.audit.UmaAuditType;
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

    /**
     * Constructs a new {@code PendingRequestsService} instance.
     *
     * @param store An instance of the UMA Pending Requests {@code TokenDataStore}.
     * @param auditLogger An instance of the {@code UmaAuditLogger}.
     * @param settingsFactory An instance of the {@code UmaProviderSettingsFactory}.
     * @param emailService An instance of the {@code UmaEmailService}.
     * @param pendingRequestEmailTemplate An instance of the {@code PendingRequestEmailTemplate}.
     * @param policyService An instance of the {@code UmaPolicyService}.
     */
    @Inject
    public PendingRequestsService(@DataLayer(ConnectionType.UMA_PENDING_REQUESTS) TokenDataStore store,
            UmaAuditLogger auditLogger, UmaProviderSettingsFactory settingsFactory,
            UmaEmailService emailService,
            PendingRequestEmailTemplate pendingRequestEmailTemplate, UmaPolicyService policyService) {
        this.store = store;
        this.auditLogger = auditLogger;
        this.settingsFactory = settingsFactory;
        this.emailService = emailService;
        this.pendingRequestEmailTemplate = pendingRequestEmailTemplate;
        this.policyService = policyService;
    }

    /**
     * Creates a pending request.
     *
     * @param resourceSetId The resource set id.
     * @param resourceSetName The resource set name.
     * @param resourceOwnerId The resource owner id.
     * @param requestingPartyId The requesting party id.
     * @param realm The realm.
     * @param scopes The requested scopes.
     * @throws ServerException If the pending request could not be created.
     */
    public void createPendingRequest(String resourceSetId, String resourceSetName, String resourceOwnerId,
            String requestingPartyId, String realm, Set<String> scopes) throws ServerException {

        if (isEmailResourceOwnerOnPendingRequestCreationEnabled(realm)) {
            Pair<String, String> template = pendingRequestEmailTemplate.getCreationTemplate(resourceOwnerId, realm);
            try {
                emailService.email(realm, resourceOwnerId, template.getFirst(),
                        MessageFormat.format(template.getSecond(), requestingPartyId, resourceSetName,
                                pendingRequestEmailTemplate.buildScopeString(scopes, resourceOwnerId, realm)));
            } catch (MessagingException e) {
                debug.warning("Pending Request Creation email could not be sent", e);
            }
        }

        UmaPendingRequest pendingRequest = new UmaPendingRequest(resourceSetId, resourceSetName, resourceOwnerId, realm,
                requestingPartyId, scopes);
        store.create(pendingRequest);
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
     * @param realm The current realm.  @return {@code Promise} which is completed successfully or failed with a {@code ResourceException}.
     * @throws ResourceException If the pending request is not found or could not be marked as approved.
     */
    public Promise<Void, ResourceException> approvePendingRequest(ServerContext context, final String id,
            JsonValue content, final String realm) throws ResourceException {
        try {
            final UmaPendingRequest request = store.read(id);
            return createUmaPolicy(context, request, content)
                    .thenAsync(new AsyncFunction<UmaPolicy, Void, ResourceException>() {
                        @Override
                        public Promise<Void, ResourceException> apply(UmaPolicy value) throws ResourceException {
                            try {
                                if (isEmailRequestingPartyOnPendingRequestApprovalEnabled(realm)) {
                                    Pair<String, String> template = pendingRequestEmailTemplate.getApprovalTemplate(
                                            request.getRequestingPartyId(), realm);
                                    try {
                                        emailService.email(realm, request.getRequestingPartyId(), template.getFirst(),
                                                MessageFormat.format(template.getSecond(),
                                                        request.getResourceOwnerId(), request.getResourceSetName(),
                                                        pendingRequestEmailTemplate.buildScopeString(
                                                                request.getScopes(), request.getRequestingPartyId(),
                                                                realm)));
                                    } catch (MessagingException e) {
                                        debug.warning("Pending Request Approval email could not be sent", e);
                                    }
                                }
                                store.delete(id);
                                auditLogger.log(request.getResourceSetId(), request.getResourceSetName(),
                                        request.getResourceOwnerId(), UmaAuditType.REQUEST_APPROVED,
                                        request.getRequestingPartyId());

                                return newResultPromise(null);
                            } catch (NotFoundException e) {
                                throw new org.forgerock.json.resource.NotFoundException("Pending request, " + id
                                        + ", not found", e);
                            } catch (ServerException e) {
                                throw new InternalServerErrorException("Failed to mark pending request, " + id
                                        + ", as approved", e);
                            }
                        }
                    });
        } catch (NotFoundException e) {
            throw new org.forgerock.json.resource.NotFoundException("Pending request, " + id + ", not found", e);
        } catch (ServerException e) {
            throw new InternalServerErrorException("Failed to mark pending request, " + id + ", as approved", e);
        }
    }

    private Promise<UmaPolicy, ResourceException> createUmaPolicy(ServerContext context, UmaPendingRequest request,
            JsonValue content) {
        Collection<String> scopes;
        if (content != null && !content.isNull() && content.isDefined("scopes")) {
            scopes = content.get("scopes").asList(String.class);
        } else {
            scopes = request.getScopes();
        }
        JsonValue policy = json(object(
                field("policyId", request.getResourceSetId()),
                field("permissions", array(
                        object(
                                field("subject", request.getRequestingPartyId()),
                                field("scopes", scopes)
                        )
                ))));
        return policyService.createPolicy(context, policy);
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
            auditLogger.log(request.getResourceSetId(), request.getResourceSetName(), request.getResourceOwnerId(),
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
        } catch (org.forgerock.oauth2.core.exceptions.ServerException e) {
            throw new ServerException("Failed to read UMA Provider settings", e);
        }
    }

    private boolean isEmailRequestingPartyOnPendingRequestApprovalEnabled(String realm) throws ServerException {
        try {
            return settingsFactory.get(realm).isEmailRequestingPartyOnPendingRequestApprovalEnabled();
        } catch (org.forgerock.oauth2.core.exceptions.ServerException e) {
            throw new ServerException("Failed to read UMA Provider settings", e);
        }
    }
}
