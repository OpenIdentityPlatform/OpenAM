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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.forgerockrest.entitlements;

import com.sun.identity.entitlement.EntitlementException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.forgerockrest.entitlements.model.json.BatchPolicyRequest;
import org.forgerock.openam.forgerockrest.entitlements.model.json.PolicyRequest;
import org.forgerock.openam.forgerockrest.entitlements.model.json.TreePolicyRequest;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.rest.resource.SubjectContext;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        restSubject = new Subject();
        factory = new PolicyRequestFactory();
    }

    @Test
    public void shouldRetrieveBatchRequest() throws EntitlementException {
        // When...
        given(subjectContext.getCallerSubject()).willReturn(restSubject);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("resources", Arrays.asList("/resource/a", "/resource/b"));
        given(actionRequest.getContent()).willReturn(JsonValue.json(properties));

        // Given...
        ServerContext context = buildContextStructure("/abc");
        PolicyRequest request = factory.buildRequest(PolicyAction.EVALUATE, context, actionRequest);

        // Then...
        assertThat(request).isNotNull();
        assertThat(request.getRealm()).isEqualTo("/abc");
        assertThat(request).isInstanceOfAny(BatchPolicyRequest.class);
        BatchPolicyRequest batchRequest = (BatchPolicyRequest)request;
        assertThat(batchRequest.getResources()).containsOnly("/resource/a", "/resource/b");

        verify(subjectContext).getCallerSubject();
        verify(actionRequest, times(2)).getContent();
        verifyNoMoreInteractions(subjectContext, actionRequest);
    }

    @Test
    public void shouldRetrieveTreeRequest() throws EntitlementException {
        // When...
        given(subjectContext.getCallerSubject()).willReturn(restSubject);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("resource", "/resource/a");
        given(actionRequest.getContent()).willReturn(JsonValue.json(properties));

        // Given...
        ServerContext context = buildContextStructure("/abc");
        PolicyRequest request = factory.buildRequest(PolicyAction.TREE_EVALUATE, context, actionRequest);

        // Then...
        assertThat(request).isNotNull();
        assertThat(request.getRealm()).isEqualTo("/abc");
        assertThat(request).isInstanceOfAny(TreePolicyRequest.class);
        TreePolicyRequest treeRequest = (TreePolicyRequest)request;
        assertThat(treeRequest.getResource()).isEqualTo("/resource/a");

        verify(subjectContext).getCallerSubject();
        verify(actionRequest, times(2)).getContent();
        verifyNoMoreInteractions(subjectContext, actionRequest);
    }

    @Test(expectedExceptions = EntitlementException.class)
    public void shouldRejectUnsupportedAction() throws EntitlementException {
        // Given...
        ServerContext context = buildContextStructure("/abc");
        factory.buildRequest(PolicyAction.UNKNOWN, context, actionRequest);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullAction() throws EntitlementException {
        // Given...
        ServerContext context = buildContextStructure("/abc");
        factory.buildRequest(null, context, actionRequest);
    }

    private ServerContext buildContextStructure(final String realm) {
        RealmContext realmContext = new RealmContext(subjectContext);
        realmContext.addSubRealm(realm, realm);
        return new ServerContext(realmContext);
    }


}
