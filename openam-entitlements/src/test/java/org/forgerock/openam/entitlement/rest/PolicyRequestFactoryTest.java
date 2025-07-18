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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.openam.entitlement.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import javax.security.auth.Subject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.entitlement.EntitlementException;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.ClientContext;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.openam.entitlement.rest.model.json.BatchPolicyRequest;
import org.forgerock.openam.entitlement.rest.model.json.PolicyRequest;
import org.forgerock.openam.entitlement.rest.model.json.TreePolicyRequest;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.SubjectContext;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link PolicyRequestFactory}.
 *
 * @since 12.0.0
 */
public class PolicyRequestFactoryTest {

    @Mock
    private SubjectContext subjectContext;
    @Mock
    private ActionRequest actionRequest;

    private Subject restSubject;

    private PolicyRequestFactory factory;
    private RealmTestHelper realmTestHelper;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        restSubject = new Subject();
        realmTestHelper = new RealmTestHelper();
        realmTestHelper.setupRealmClass();
        factory = new PolicyRequestFactory(mock(SSOTokenManager.class));
    }

    @AfterMethod
    public void testDown() {
        realmTestHelper.tearDownRealmClass();
    }

    @Test
    public void shouldRetrieveBatchRequest() throws EntitlementException {
        // When...
        given(subjectContext.getCallerSubject()).willReturn(restSubject);
        given(subjectContext.getCallerSSOToken()).willReturn(mock(SSOToken.class));

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("resources", Arrays.asList("/resource/a", "/resource/b"));
        given(actionRequest.getContent()).willReturn(JsonValue.json(properties));
        Realm realm = realmTestHelper.mockRealm("abc");

        // Given...
        Context context = buildContextStructure(realm);
        PolicyRequest request = factory.buildRequest(PolicyAction.EVALUATE, context, actionRequest);

        // Then...
        assertThat(request).isNotNull();
        assertThat(request.getRealm()).isEqualTo("/abc");
        assertThat(request).isInstanceOfAny(BatchPolicyRequest.class);
        BatchPolicyRequest batchRequest = (BatchPolicyRequest)request;
        assertThat(batchRequest.getResources()).containsOnly("/resource/a", "/resource/b");

        verify(subjectContext).getCallerSubject();
        verify(subjectContext).getCallerSSOToken();
        verify(actionRequest, times(2)).getContent();
        verifyNoMoreInteractions(subjectContext, actionRequest);
    }

    @Test
    public void shouldRetrieveTreeRequest() throws EntitlementException {
        // When...
        given(subjectContext.getCallerSubject()).willReturn(restSubject);
        given(subjectContext.getCallerSSOToken()).willReturn(mock(SSOToken.class));

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("resource", "/resource/a");
        given(actionRequest.getContent()).willReturn(JsonValue.json(properties));
        Realm realm = realmTestHelper.mockRealm("abc");

        // Given...
        Context context = buildContextStructure(realm);
        PolicyRequest request = factory.buildRequest(PolicyAction.TREE_EVALUATE, context, actionRequest);

        // Then...
        assertThat(request).isNotNull();
        assertThat(request.getRealm()).isEqualTo("/abc");
        assertThat(request).isInstanceOfAny(TreePolicyRequest.class);
        TreePolicyRequest treeRequest = (TreePolicyRequest)request;
        assertThat(treeRequest.getResource()).isEqualTo("/resource/a");

        verify(subjectContext).getCallerSubject();
        verify(subjectContext).getCallerSSOToken();
        verify(actionRequest, times(2)).getContent();
        verifyNoMoreInteractions(subjectContext, actionRequest);
    }

    @Test(expectedExceptions = EntitlementException.class)
    public void shouldRejectUnsupportedAction() throws EntitlementException {
        // Given...
        Realm realm = realmTestHelper.mockRealm("abc");
        Context context = buildContextStructure(realm);
        factory.buildRequest(PolicyAction.UNKNOWN, context, actionRequest);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullAction() throws EntitlementException {
        // Given...
        Realm realm = realmTestHelper.mockRealm("abc");
        Context context = buildContextStructure(realm);
        factory.buildRequest(null, context, actionRequest);
    }

    private Context buildContextStructure(final Realm realm) {
        RealmContext realmContext = new RealmContext(subjectContext, realm);
        return ClientContext.newInternalClientContext(realmContext);
    }
}
