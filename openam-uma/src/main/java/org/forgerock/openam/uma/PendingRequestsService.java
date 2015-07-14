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

import static org.forgerock.openam.sm.datalayer.impl.uma.UmaPendingRequest.*;

import javax.inject.Inject;
import java.util.Set;

import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.sm.datalayer.impl.uma.UmaPendingRequest;
import org.forgerock.openam.sm.datalayer.store.NotFoundException;
import org.forgerock.openam.sm.datalayer.store.ServerException;
import org.forgerock.openam.sm.datalayer.store.TokenDataStore;
import org.forgerock.openam.uma.audit.UmaAuditLogger;
import org.forgerock.openam.uma.audit.UmaAuditType;
import org.forgerock.util.query.QueryFilter;

/**
 * Service class responsible for creating, reading, querying, approving and
 * denying UMA Pending Requests.
 *
 * @since 13.0.0
 */
public class PendingRequestsService {

    private final TokenDataStore<UmaPendingRequest> store;
    private final UmaAuditLogger auditLogger;
    private final CoreWrapper coreWrapper;
    private final UmaProviderSettingsFactory settingsFactory;

    @Inject
    public PendingRequestsService(@DataLayer(ConnectionType.UMA_PENDING_REQUESTS) TokenDataStore store,
            UmaAuditLogger auditLogger, CoreWrapper coreWrapper, UmaProviderSettingsFactory settingsFactory) {
        this.store = store;
        this.auditLogger = auditLogger;
        this.coreWrapper = coreWrapper;
        this.settingsFactory = settingsFactory;
    }

    public void createPendingRequest(String resourceSetId, String resourceSetName, String resourceOwnerId,
            String requestingUserId, String realm, Set<String> scopes) throws ServerException {

        if (isEmailResourceOwnerOnPendingRequestCreationEnabled(realm)) {
            //TODO email RO
        }

        UmaPendingRequest pendingRequest = new UmaPendingRequest(resourceSetId, resourceSetName,
                coreWrapper.getIdentity(resourceOwnerId, realm).getUniversalId(), realm,
                coreWrapper.getIdentity(requestingUserId, realm).getUniversalId(), scopes);
        store.create(pendingRequest);
    }

    public UmaPendingRequest readPendingRequest(String id) throws ResourceException {
        try {
            return store.read(id);
        } catch (NotFoundException e) {
            throw new org.forgerock.json.resource.NotFoundException("Pending request, " + id + ", not found", e);
        } catch (ServerException e) {
            throw new InternalServerErrorException("Failed to read pending request, " + id, e);
        }
    }

    public Set<UmaPendingRequest> queryPendingRequests(String resourceOwnerId, String realm) throws ResourceException {
        try {
            return store.query(QueryFilter.and(
                    QueryFilter.equalTo(RESOURCE_OWNER_ID_FIELD, resourceOwnerId),
                    QueryFilter.equalTo(REALM_FIELD, realm)));
        } catch (ServerException e) {
            throw new InternalServerErrorException("Failed to query pending request for resource owner, "
                    + resourceOwnerId, e); //TODO be consistent with exception types
        }
    }

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

    public void approvePendingRequest(String id, String realm) throws ResourceException {
        try {
            if (isEmailRequestingPartyOnPendingRequestApprovalEnabled(realm)) {
                //TODO email RqP
            }
            UmaPendingRequest request = store.read(id);
            store.delete(id);
            auditLogger.log(request.getResourceSetId(), request.getResourceSetName(), request.getResourceOwnerId(),
                    UmaAuditType.REQUEST_APPROVED, request.getRequestingPartyId());
        } catch (NotFoundException e) {
            throw new org.forgerock.json.resource.NotFoundException("Pending request, " + id + ", not found", e);
        } catch (ServerException e) {
            throw new InternalServerErrorException("Failed to mark pending request, " + id + ", as approved", e);
        }
    }

    public void denyPendingRequest(String id, String realm) throws ResourceException {
        try {
            UmaPendingRequest request = store.read(id);
            request.setState(STATE_DENIED);
            store.update(request);
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
