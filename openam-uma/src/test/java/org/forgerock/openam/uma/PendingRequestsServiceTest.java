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
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.openam.sm.datalayer.impl.uma.UmaPendingRequest.*;
import static org.forgerock.openam.uma.UmaConstants.UmaPolicy.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Set;

import com.sun.identity.idm.AMIdentity;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.services.baseurl.BaseURLProvider;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.sm.datalayer.impl.uma.UmaPendingRequest;
import org.forgerock.openam.sm.datalayer.store.NotFoundException;
import org.forgerock.openam.sm.datalayer.store.ServerException;
import org.forgerock.openam.sm.datalayer.store.TokenDataStore;
import org.forgerock.openam.uma.audit.UmaAuditLogger;
import org.forgerock.openam.uma.audit.UmaAuditType;
import org.forgerock.services.context.Context;
import org.forgerock.util.Pair;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.forgerock.util.query.QueryFilter;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PendingRequestsServiceTest {

    private static final String PENDING_REQUEST_ID = "PENDING_REQUEST_ID";
    private static final String RESOURCE_SET_ID = "RESOURCE_SET_ID";
    private static final String RESOURCE_SET_NAME = "RESOURCE_SET_NAME";
    private static final String RESOURCE_OWNER_ID = "RESOURCE_OWNER_ID";
    private static final String REQUESTING_PARTY_ID = "REQUESTING_PARTY_ID";
    private static final String REALM = "REALM";
    private static final String SCOPE = "SCOPE";

    private PendingRequestsService service;

    @Mock
    private TokenDataStore<UmaPendingRequest> store;
    @Mock
    private UmaAuditLogger auditLogger;
    @Mock
    private UmaProviderSettings settings;
    @Mock
    private UmaEmailService emailService;
    @Mock
    private PendingRequestEmailTemplate pendingRequestEmailTemplate;
    @Mock
    private UmaPolicyService policyService;
    @Mock
    private BaseURLProvider baseUrlProvider;
    @Mock
    private AMIdentity resourceOwnerIdentity;

    @SuppressWarnings("unchecked")
    @BeforeMethod
    public void setup() throws Exception {
        initMocks(this);
        UmaProviderSettingsFactory settingsFactory = mock(UmaProviderSettingsFactory.class);
        given(settingsFactory.get(anyString())).willReturn(settings);
        BaseURLProviderFactory baseUrlProviderFactory = mock(BaseURLProviderFactory.class);
        given(baseUrlProviderFactory.get(anyString())).willReturn(baseUrlProvider);
        CoreWrapper coreWrapper = mock(CoreWrapper.class);
        given(coreWrapper.getIdentity(RESOURCE_OWNER_ID, REALM)).willReturn(resourceOwnerIdentity);

        service = new PendingRequestsService(store, auditLogger, settingsFactory, emailService,
                pendingRequestEmailTemplate, policyService, baseUrlProviderFactory, coreWrapper);
    }

    @Test
    public void shouldCreatePendingRequest() throws Exception {

        //Given
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        //When
        service.createPendingRequest(httpRequest, RESOURCE_SET_ID, RESOURCE_SET_NAME, RESOURCE_OWNER_ID,
                REQUESTING_PARTY_ID, REALM, Collections.singleton(SCOPE));

        //Then
        ArgumentCaptor<UmaPendingRequest> pendingRequestCaptor = ArgumentCaptor.forClass(UmaPendingRequest.class);
        verify(store).create(pendingRequestCaptor.capture());
        UmaPendingRequest pendingRequest = pendingRequestCaptor.getValue();
        assertThat(pendingRequest.getResourceSetId()).isEqualTo(RESOURCE_SET_ID);
        assertThat(pendingRequest.getResourceSetName()).isEqualTo(RESOURCE_SET_NAME);
        assertThat(pendingRequest.getResourceOwnerId()).isEqualTo(RESOURCE_OWNER_ID);
        assertThat(pendingRequest.getRequestingPartyId()).isEqualTo(REQUESTING_PARTY_ID);
        assertThat(pendingRequest.getRealm()).isEqualTo(REALM);
        assertThat(pendingRequest.getScopes()).containsExactly(SCOPE);
        assertThat(pendingRequest.getRequestedAt()).isNotNull();
    }

    @Test
    public void shouldSendEmailOnPendingRequestCreation() throws Exception {

        //Given
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        given(settings.isEmailResourceOwnerOnPendingRequestCreationEnabled()).willReturn(true);
        mockPendingRequestCreationEmailTemplate(RESOURCE_OWNER_ID, REALM);

        //When
        service.createPendingRequest(httpRequest, RESOURCE_SET_ID, RESOURCE_SET_NAME, RESOURCE_OWNER_ID,
                REQUESTING_PARTY_ID, REALM, Collections.singleton(SCOPE));

        //Then
        verify(emailService).email(REALM, RESOURCE_OWNER_ID, "CREATION_SUBJECT",
                "CREATION_BODY " + REQUESTING_PARTY_ID + " " + RESOURCE_SET_NAME + " " + SCOPE);
        ArgumentCaptor<UmaPendingRequest> pendingRequestCaptor = ArgumentCaptor.forClass(UmaPendingRequest.class);
        verify(store).create(pendingRequestCaptor.capture());
    }

    @Test
    public void shouldReadPendingRequest() throws Exception {

        //When
        service.readPendingRequest(PENDING_REQUEST_ID);

        //Then
        verify(store).read(PENDING_REQUEST_ID);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldQueryPendingRequestByResourceOwner() throws Exception {

        //When
        service.queryPendingRequests(RESOURCE_OWNER_ID, REALM);

        //Then
        ArgumentCaptor<QueryFilter> queryFilterCaptor = ArgumentCaptor.forClass(QueryFilter.class);
        verify(store).query(queryFilterCaptor.capture());
        QueryFilter<String> queryFilter = queryFilterCaptor.getValue();
        assertThat(queryFilter.toString()).contains(RESOURCE_OWNER_ID_FIELD + " eq \"" + RESOURCE_OWNER_ID + "\" and "
                + REALM_FIELD + " eq \"" + REALM + "\"");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldQueryPendingRequestByResourceOwnerAndRequestingParty() throws Exception {

        //When
        service.queryPendingRequests(RESOURCE_SET_ID, RESOURCE_OWNER_ID, REALM, REQUESTING_PARTY_ID);

        //Then
        ArgumentCaptor<QueryFilter> queryFilterCaptor = ArgumentCaptor.forClass(QueryFilter.class);
        verify(store).query(queryFilterCaptor.capture());
        QueryFilter<String> queryFilter = queryFilterCaptor.getValue();
        assertThat(queryFilter.toString()).contains(RESOURCE_SET_ID_FIELD + " eq \"RESOURCE_SET_ID\" and "
                + RESOURCE_OWNER_ID_FIELD + " eq \"" + RESOURCE_OWNER_ID + "\" and " + REALM_FIELD + " eq \"REALM\" and "
                + REQUESTING_PARTY_ID_FIELD + " eq \"" + REQUESTING_PARTY_ID + "\"");
    }

    @Test
    public void shouldApprovePendingRequest() throws Exception {

        //Given
        Context context = mock(Context.class);
        createPendingRequest(PENDING_REQUEST_ID, RESOURCE_SET_ID, RESOURCE_SET_NAME, RESOURCE_OWNER_ID,
                REALM, REQUESTING_PARTY_ID, Collections.singleton(SCOPE));
        mockSuccessfulPolicyCreationForPendingRequest();
        JsonValue content = json(object());

        //When
        service.approvePendingRequest(context, PENDING_REQUEST_ID, content, REALM);

        //Then
        ArgumentCaptor<JsonValue> policyCaptor = ArgumentCaptor.forClass(JsonValue.class);
        verify(policyService).createPolicy(eq(context), policyCaptor.capture());
        JsonValue policy = policyCaptor.getValue();
        assertThat(policy).stringAt("policyId").isEqualTo(RESOURCE_SET_ID);
        assertThat(policy).hasArray("permissions").hasSize(1);
        assertThat(policy).stringAt("permissions/0/subject").isEqualTo(REQUESTING_PARTY_ID);
        assertThat(policy).hasArray("permissions/0/scopes").containsOnly(SCOPE);
        verify(store).delete(PENDING_REQUEST_ID);
        verify(auditLogger).log(RESOURCE_SET_ID, RESOURCE_SET_NAME, resourceOwnerIdentity,
                UmaAuditType.REQUEST_APPROVED, REQUESTING_PARTY_ID);
    }

    @Test
    public void shouldApprovePendingRequestUsingScopesFromRequestContent() throws Exception {

        //Given
        Context context = mock(Context.class);
        createPendingRequest(PENDING_REQUEST_ID, RESOURCE_SET_ID, RESOURCE_SET_NAME, RESOURCE_OWNER_ID,
                REALM, REQUESTING_PARTY_ID, Collections.singleton(SCOPE));
        mockSuccessfulPolicyCreationForPendingRequest();
        JsonValue content = json(object(field("scopes", array("SCOPE_A", "SCOPE_B"))));

        //When
        service.approvePendingRequest(context, PENDING_REQUEST_ID, content, REALM);

        //Then
        ArgumentCaptor<JsonValue> policyCaptor = ArgumentCaptor.forClass(JsonValue.class);
        verify(policyService).createPolicy(eq(context), policyCaptor.capture());
        JsonValue policy = policyCaptor.getValue();
        assertThat(policy).stringAt("policyId").isEqualTo(RESOURCE_SET_ID);
        assertThat(policy).hasArray("permissions").hasSize(1);
        assertThat(policy).stringAt("permissions/0/subject").isEqualTo(REQUESTING_PARTY_ID);
        assertThat(policy).hasArray("permissions/0/scopes").containsOnly("SCOPE_A", "SCOPE_B");
        verify(store).delete(PENDING_REQUEST_ID);
        verify(auditLogger).log(RESOURCE_SET_ID, RESOURCE_SET_NAME, resourceOwnerIdentity,
                UmaAuditType.REQUEST_APPROVED, REQUESTING_PARTY_ID);
    }

    @Test
    public void shouldApprovePendingRequestUpdatingExistingPolicy() throws Exception {

        //Given
        Context context = mock(Context.class);
        createPendingRequest(PENDING_REQUEST_ID, RESOURCE_SET_ID, RESOURCE_SET_NAME, RESOURCE_OWNER_ID,
                REALM, REQUESTING_PARTY_ID, Collections.singleton(SCOPE));
        UmaPolicy existingPolicy = existingUmaPolicy("charlie", "SCOPE_A");
        mockSuccessfulPolicyUpdateForPendingRequest(existingPolicy);
        JsonValue content = json(object());

        //When
        service.approvePendingRequest(context, PENDING_REQUEST_ID, content, REALM);

        //Then
        ArgumentCaptor<JsonValue> policyCaptor = ArgumentCaptor.forClass(JsonValue.class);
        verify(policyService).updatePolicy(eq(context), eq(RESOURCE_SET_ID), policyCaptor.capture());
        JsonValue policy = policyCaptor.getValue();
        assertThat(policy).stringAt("policyId").isEqualTo(RESOURCE_SET_ID);
        assertThat(policy).hasArray("permissions").hasSize(2);
        assertThat(policy).stringAt("permissions/0/subject").isEqualTo("charlie");
        assertThat(policy).hasArray("permissions/0/scopes").containsOnly("SCOPE_A");
        assertThat(policy).stringAt("permissions/1/subject").isEqualTo(REQUESTING_PARTY_ID);
        assertThat(policy).hasArray("permissions/1/scopes").containsOnly(SCOPE);
        verify(store).delete(PENDING_REQUEST_ID);
        verify(auditLogger).log(RESOURCE_SET_ID, RESOURCE_SET_NAME, resourceOwnerIdentity,
                UmaAuditType.REQUEST_APPROVED, REQUESTING_PARTY_ID);
    }

    @Test
    public void shouldSendEmailOnPendingRequestApproval() throws Exception {

        //Given
        Context context = mock(Context.class);
        createPendingRequest(PENDING_REQUEST_ID, RESOURCE_SET_ID, RESOURCE_SET_NAME, RESOURCE_OWNER_ID,
                REALM, REQUESTING_PARTY_ID, Collections.singleton(SCOPE));
        given(settings.isEmailRequestingPartyOnPendingRequestApprovalEnabled()).willReturn(true);
        mockPendingRequestApprovalEmailTemplate(REQUESTING_PARTY_ID, REALM);
        mockSuccessfulPolicyCreationForPendingRequest();
        JsonValue content = json(object());

        //When
        service.approvePendingRequest(context, PENDING_REQUEST_ID, content, REALM);

        //Then
        verify(policyService).createPolicy(eq(context), any(JsonValue.class));
        verify(emailService).email(REALM, REQUESTING_PARTY_ID, "APPROVAL_SUBJECT",
                "APPROVAL_BODY " + RESOURCE_OWNER_ID + " " + RESOURCE_SET_NAME + " " + SCOPE);
        verify(store).delete(PENDING_REQUEST_ID);
        verify(auditLogger).log(RESOURCE_SET_ID, RESOURCE_SET_NAME, resourceOwnerIdentity,
                UmaAuditType.REQUEST_APPROVED, REQUESTING_PARTY_ID);
    }

    @Test
    public void shouldDenyPendingRequest() throws Exception {

        //Given
        createPendingRequest(PENDING_REQUEST_ID, RESOURCE_SET_ID, RESOURCE_SET_NAME, RESOURCE_OWNER_ID,
                REALM, REQUESTING_PARTY_ID, Collections.singleton(SCOPE));

        //When
        service.denyPendingRequest(PENDING_REQUEST_ID, REALM);

        //Then
        verify(store).delete(PENDING_REQUEST_ID);
        verify(auditLogger).log(RESOURCE_SET_ID, RESOURCE_SET_NAME, resourceOwnerIdentity,
                UmaAuditType.REQUEST_DENIED, REQUESTING_PARTY_ID);
    }

    private void createPendingRequest(String id, String resourceSetId, String resourceSetName, String resourceOwnerId,
            String realm, String requestingPartyId, Set<String> scopes) throws NotFoundException, ServerException {
        UmaPendingRequest pendingRequest = new UmaPendingRequest(resourceSetId, resourceSetName, resourceOwnerId,
                realm, requestingPartyId, scopes);
        pendingRequest.setId(id);
        given(store.read(id)).willReturn(pendingRequest);
    }

    private void mockPendingRequestCreationEmailTemplate(String resourceOwnerId, String realm) {
        given(pendingRequestEmailTemplate.getCreationTemplate(resourceOwnerId, realm))
                .willReturn(Pair.of("CREATION_SUBJECT", "CREATION_BODY {0} {1} {2}"));
        given(pendingRequestEmailTemplate.buildScopeString(anySetOf(String.class), eq(resourceOwnerId), eq(realm)))
                .willReturn(SCOPE);
    }

    private void mockPendingRequestApprovalEmailTemplate(String resourceOwnerId, String realm) {
        given(pendingRequestEmailTemplate.getApprovalTemplate(resourceOwnerId, realm))
                .willReturn(Pair.of("APPROVAL_SUBJECT", "APPROVAL_BODY {0} {1} {2}"));
        given(pendingRequestEmailTemplate.buildScopeString(anySetOf(String.class), eq(resourceOwnerId), eq(realm)))
                .willReturn(SCOPE);
    }

    private void mockSuccessfulPolicyCreationForPendingRequest() {
        Promise<UmaPolicy, ResourceException> readPromise = new org.forgerock.json.resource.NotFoundException().asPromise();
        given(policyService.readPolicy(any(Context.class), anyString())).willReturn(readPromise);
        Promise<UmaPolicy, ResourceException> createPromise = Promises.newResultPromise(null);
        given(policyService.createPolicy(any(Context.class), any(JsonValue.class))).willReturn(createPromise);
    }

    private UmaPolicy existingUmaPolicy(String grantedSubject, String... scopes) throws Exception {
        return UmaPolicy.valueOf(new ResourceSetDescription(), json(object(
                field(POLICY_ID_KEY, RESOURCE_SET_ID),
                field(PERMISSIONS_KEY, array(
                        object(
                                field(SUBJECT_KEY, grantedSubject),
                                field(SCOPES_KEY, array(scopes))
                        )
                ))))
        );
    }

    private void mockSuccessfulPolicyUpdateForPendingRequest(UmaPolicy policy) {
        Promise<UmaPolicy, ResourceException> readPromise = Promises.newResultPromise(policy);
        given(policyService.readPolicy(any(Context.class), anyString())).willReturn(readPromise);
        Promise<UmaPolicy, ResourceException> updatePromise = Promises.newResultPromise(null);
        given(policyService.updatePolicy(any(Context.class), anyString(), any(JsonValue.class)))
                .willReturn(updatePromise);
    }
}
