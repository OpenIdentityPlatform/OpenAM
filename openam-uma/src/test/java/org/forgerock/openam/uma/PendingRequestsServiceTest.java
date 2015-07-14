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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.sm.datalayer.impl.uma.UmaPendingRequest.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.Set;

import com.sun.identity.idm.AMIdentity;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.sm.datalayer.impl.uma.UmaPendingRequest;
import org.forgerock.openam.sm.datalayer.store.NotFoundException;
import org.forgerock.openam.sm.datalayer.store.ServerException;
import org.forgerock.openam.sm.datalayer.store.TokenDataStore;
import org.forgerock.openam.uma.audit.UmaAuditLogger;
import org.forgerock.openam.uma.audit.UmaAuditType;
import org.forgerock.util.query.QueryFilter;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PendingRequestsServiceTest {

    private PendingRequestsService service;

    private TokenDataStore<UmaPendingRequest> store;
    private UmaAuditLogger auditLogger;
    private CoreWrapper coreWrapper;
    private UmaProviderSettings settings;

    @SuppressWarnings("unchecked")
    @BeforeMethod
    public void setup() {
        store = mock(TokenDataStore.class);
        auditLogger = mock(UmaAuditLogger.class);
        coreWrapper = mock(CoreWrapper.class);
        settings = mock(UmaProviderSettings.class);
        UmaProviderSettingsFactory settingsFactory = mock(UmaProviderSettingsFactory.class);
        given(settingsFactory.get(anyString())).willReturn(settings);

        service = new PendingRequestsService(store, auditLogger, coreWrapper, settingsFactory);
    }

    @Test
    public void shouldCreatePendingRequest() throws Exception {

        //Given
        mockResourceOwnerIdentity("RESOURCE_OWNER_ID", "REALM");
        mockRequestingPartyIdentity("REQUESTING_PARTY_ID", "REALM");

        //When
        service.createPendingRequest("RESOURCE_SET_ID", "RESOURCE_SET_NAME", "RESOURCE_OWNER_ID", "REQUESTING_PARTY_ID",
                "REALM", Collections.singleton("SCOPE"));

        //Then
        ArgumentCaptor<UmaPendingRequest> pendingRequestCaptor = ArgumentCaptor.forClass(UmaPendingRequest.class);
        verify(store).create(pendingRequestCaptor.capture());
        UmaPendingRequest pendingRequest = pendingRequestCaptor.getValue();
        assertThat(pendingRequest.getResourceSetId()).isEqualTo("RESOURCE_SET_ID");
        assertThat(pendingRequest.getResourceSetName()).isEqualTo("RESOURCE_SET_NAME");
        assertThat(pendingRequest.getResourceOwnerId()).isEqualTo("RESOURCE_OWNER_ID_UNIVERSAL_ID");
        assertThat(pendingRequest.getRequestingPartyId()).isEqualTo("REQUESTING_PARTY_ID_UNIVERSAL_ID");
        assertThat(pendingRequest.getRealm()).isEqualTo("REALM");
        assertThat(pendingRequest.getScopes()).containsExactly("SCOPE");
        assertThat(pendingRequest.getRequestedAt()).isNotNull();
    }

    @Test
    public void shouldReadPendingRequest() throws Exception {

        //When
        service.readPendingRequest("PENDING_REQUEST_ID");

        //Then
        verify(store).read("PENDING_REQUEST_ID");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldQueryPendingRequestByResourceOwner() throws Exception {

        //When
        service.queryPendingRequests("RESOURCE_OWNER_ID", "REALM");

        //Then
        ArgumentCaptor<QueryFilter> queryFilterCaptor = ArgumentCaptor.forClass(QueryFilter.class);
        verify(store).query(queryFilterCaptor.capture());
        QueryFilter<String> queryFilter = queryFilterCaptor.getValue();
        assertThat(queryFilter.toString()).contains(RESOURCE_OWNER_ID_FIELD + " eq \"RESOURCE_OWNER_ID\" and "
                + REALM_FIELD + " eq \"REALM\"");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldQueryPendingRequestByResourceOwnerAndRequestingParty() throws Exception {

        //When
        service.queryPendingRequests("RESOURCE_SET_ID", "RESOURCE_OWNER_ID", "REALM", "REQUESTING_PARTY_ID");

        //Then
        ArgumentCaptor<QueryFilter> queryFilterCaptor = ArgumentCaptor.forClass(QueryFilter.class);
        verify(store).query(queryFilterCaptor.capture());
        QueryFilter<String> queryFilter = queryFilterCaptor.getValue();
        assertThat(queryFilter.toString()).contains(RESOURCE_SET_ID_FIELD + " eq \"RESOURCE_SET_ID\" and "
                + RESOURCE_OWNER_ID_FIELD + " eq \"RESOURCE_OWNER_ID\" and " + REALM_FIELD + " eq \"REALM\" and "
                + REQUESTING_PARTY_ID_FIELD + " eq \"REQUESTING_PARTY_ID\"");
    }

    @Test
    public void shouldApprovePendingRequest() throws Exception {

        //Given
        createPendingRequest("PENDING_REQUEST_ID", "RESOURCE_SET_ID", "RESOURCE_SET_NAME", "RESOURCE_OWNER_ID",
                "REALM", "REQUESTING_PARTY_ID", Collections.singleton("SCOPE"));

        //When
        service.approvePendingRequest("PENDING_REQUEST_ID", "REALM");

        //Then
        verify(store).delete("PENDING_REQUEST_ID");
        verify(auditLogger).log("RESOURCE_SET_ID", "RESOURCE_SET_NAME", "RESOURCE_OWNER_ID",
                UmaAuditType.REQUEST_APPROVED, "REQUESTING_PARTY_ID");
    }

    @Test
    public void shouldDenyPendingRequest() throws Exception {

        //Given
        createPendingRequest("PENDING_REQUEST_ID", "RESOURCE_SET_ID", "RESOURCE_SET_NAME", "RESOURCE_OWNER_ID",
                "REALM", "REQUESTING_PARTY_ID", Collections.singleton("SCOPE"));

        //When
        service.denyPendingRequest("PENDING_REQUEST_ID", "REALM");

        //Then
        ArgumentCaptor<UmaPendingRequest> pendingRequestCaptor = ArgumentCaptor.forClass(UmaPendingRequest.class);
        verify(store).update(pendingRequestCaptor.capture());
        assertThat(pendingRequestCaptor.getValue().getState()).isEqualTo(STATE_DENIED);
        verify(auditLogger).log("RESOURCE_SET_ID", "RESOURCE_SET_NAME", "RESOURCE_OWNER_ID",
                UmaAuditType.REQUEST_DENIED, "REQUESTING_PARTY_ID");
    }

    private void mockResourceOwnerIdentity(String resourceOwnerId, String realm) {
        mockIdentity(resourceOwnerId, resourceOwnerId + "_UNIVERSAL_ID", realm);
    }

    private void mockRequestingPartyIdentity(String requestingPartyId, String realm) {
        mockIdentity(requestingPartyId, requestingPartyId + "_UNIVERSAL_ID", realm);
    }

    private void mockIdentity(String username, String universalId, String realm) {
        AMIdentity identity = mock(AMIdentity.class);
        given(identity.getUniversalId()).willReturn(universalId);
        given(coreWrapper.getIdentity(username, realm)).willReturn(identity);
    }

    private void createPendingRequest(String id, String resourceSetId, String resourceSetName, String resourceOwnerId,
            String realm, String requestingPartyId, Set<String> scopes) throws NotFoundException, ServerException {
        UmaPendingRequest pendingRequest = new UmaPendingRequest(resourceSetId, resourceSetName, resourceOwnerId,
                realm, requestingPartyId, scopes);
        pendingRequest.setId(id);
        given(store.read(id)).willReturn(pendingRequest);
    }
}
