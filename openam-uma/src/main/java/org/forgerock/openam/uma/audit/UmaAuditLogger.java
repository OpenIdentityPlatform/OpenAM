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

package org.forgerock.openam.uma.audit;

import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.oauth2.resources.ResourceSetStore;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.sm.datalayer.impl.uma.UmaAuditEntry;
import org.forgerock.openam.sm.datalayer.store.ServerException;
import org.forgerock.openam.sm.datalayer.store.TokenDataStore;
import org.forgerock.openam.uma.UmaException;
import org.restlet.Request;
import org.restlet.data.ChallengeResponse;

@Singleton
public class UmaAuditLogger {
    private final TokenDataStore<UmaAuditEntry> delegate;
    private final Debug logger = Debug.getInstance("UmaAuditLogger");
    private final OAuth2RequestFactory<Request> requestFactory;
    private final TokenStore oauth2TokenStore;
    private final OAuth2ProviderSettingsFactory oauth2ProviderSettingsFactory;

    @Inject
    public UmaAuditLogger(@DataLayer(ConnectionType.UMA_AUDIT_ENTRY) TokenDataStore delegate,
            TokenStore oauth2TokenStore, OAuth2RequestFactory<Request> requestFactory,
            OAuth2ProviderSettingsFactory oauth2ProviderSettingsFactory) {
        this.delegate = delegate;
        this.requestFactory = requestFactory;
        this.oauth2TokenStore = oauth2TokenStore;
        this.oauth2ProviderSettingsFactory = oauth2ProviderSettingsFactory;
    }

    public void log(String resourceSetId, String resourceOwnerId, UmaAuditType message, Request request, String requestingPartyId) {
        try {
            log(getResourceName(resourceSetId, request), resourceOwnerId, message, requestingPartyId);
        } catch (UmaException e) {
            logger.warning("Error writing to UMA audit log", e);
        } catch (NotFoundException e) {
            logger.warning("Error writing to UMA audit log", e);
        } catch (org.forgerock.oauth2.core.exceptions.ServerException e) {
            logger.warning("Error writing to UMA audit log", e);
        }
    }

    public void log(String resourceSetId, String resourceOwnerId, UmaAuditType message, String requestingPartyId) {
        final UmaAuditEntry umaAuditEntry;
        try {
            umaAuditEntry = new UmaAuditEntry(resourceSetId, resourceOwnerId, message.toString(), requestingPartyId);
            delegate.create(umaAuditEntry);
        } catch (ServerException e) {
            logger.warning("Error writing to UMA audit log", e);
        }
    }

    public Set<UmaAuditEntry> getEntireHistory(AMIdentity identity) throws ServerException {
        return delegate.query(QueryFilter.equalTo("resourceOwnerId", identity.getName()).accept(new UmaAuditQueryFilterVisitor(), null));
    }

    public Set<UmaAuditEntry> getHistory(AMIdentity identity, QueryRequest request) throws ServerException {
        return delegate.query(getQueryFilters(identity, request));
    }

    private org.forgerock.util.query.QueryFilter<String> getQueryFilters(AMIdentity identity, QueryRequest request) {
        return QueryFilter.and(request.getQueryFilter(), QueryFilter.equalTo("resourceOwnerId", identity.getName())).accept(new
                UmaAuditQueryFilterVisitor(), null);
    }

    private String getResourceName(String resourceSetId, Request request) throws NotFoundException, UmaException, org.forgerock.oauth2.core.exceptions.ServerException {
        OAuth2ProviderSettings providerSettings = oauth2ProviderSettingsFactory.get(requestFactory.create(request));
        ResourceSetDescription resourceSetDescription = getResourceSet(resourceSetId, providerSettings);
        return resourceSetDescription.getName();
    }

    private ResourceSetDescription getResourceSet(String resourceSetId, OAuth2ProviderSettings providerSettings) throws UmaException {
        try {
            ResourceSetStore store = providerSettings.getResourceSetStore();
            return store.read(resourceSetId);
        } catch (NotFoundException e) {
            throw new UmaException(400, "invalid_resource_set_id", e.getMessage());
        } catch (org.forgerock.oauth2.core.exceptions.ServerException e) {
            throw new UmaException(400, "invalid_resource_set_id", e.getMessage());
        }
    }

    private String getClientId(Request request) throws org.forgerock.oauth2.core.exceptions.ServerException {
        ChallengeResponse challengeResponse = request.getChallengeResponse();
        try {
            AccessToken accessToken = oauth2TokenStore.readAccessToken(requestFactory.create(request),
                    challengeResponse.getRawValue());
            return accessToken.getClientId();
        } catch (InvalidGrantException e) {
            throw new org.forgerock.oauth2.core.exceptions.ServerException("Unable to verify client identity.");
        }
    }

}
