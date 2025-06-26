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

package org.forgerock.openam.entitlement.rest.model.json;

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
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.SubjectContext;
import org.forgerock.services.context.ClientContext;
import org.forgerock.services.context.Context;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link BatchPolicyRequest}.
 *
 * @since 12.0.0
 */
public class BatchPolicyRequestTest {

    @Mock
    private SubjectContext subjectContext;
    @Mock
    private ActionRequest actionRequest;
    @Mock
    private SSOTokenManager tokenManager;
    private RealmTestHelper realmTestHelper;

    private Subject restSubject;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        realmTestHelper = new RealmTestHelper();
        realmTestHelper.setupRealmClass();
        restSubject = new Subject();
    }

    @AfterMethod
    public void tearDown() {
        realmTestHelper.tearDownRealmClass();
    }

    @Test
    public void shouldConstructBatchPolicyRequest() throws EntitlementException {
        given(subjectContext.getCallerSubject()).willReturn(restSubject);
        given(subjectContext.getCallerSSOToken()).willReturn(mock(SSOToken.class));

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("resources", Arrays.asList("/resource/a", "/resource/b"));
        given(actionRequest.getContent()).willReturn(JsonValue.json(properties));
        Realm realm = realmTestHelper.mockRealm("abc");

        Context context = buildContextStructure(realm);
        BatchPolicyRequest request = BatchPolicyRequest.getBatchPolicyRequest(context, actionRequest, tokenManager);

        assertThat(request).isNotNull();
        assertThat(request.getResources()).containsOnly("/resource/a", "/resource/b");

        verify(subjectContext).getCallerSubject();
        verify(subjectContext).getCallerSSOToken();
        verify(actionRequest, times(2)).getContent();
        verifyNoMoreInteractions(subjectContext, actionRequest);
    }

    @Test(expectedExceptions = EntitlementException.class)
    public void shouldRejectNullResources() throws EntitlementException {
        // When...
        Map<String, Object> properties = new HashMap<String, Object>();
        given(actionRequest.getContent()).willReturn(JsonValue.json(properties));
        given(subjectContext.getCallerSubject()).willReturn(restSubject);
        Realm realm = realmTestHelper.mockRealm("abc");

        // Given...
        Context context = buildContextStructure(realm);
        BatchPolicyRequest.getBatchPolicyRequest(context, actionRequest, tokenManager);
    }

    @Test(expectedExceptions = EntitlementException.class)
    public void shouldRejectEmptyResources() throws EntitlementException {
        // When...
        Map<String, Object> properties = new HashMap<String, Object>();
        given(actionRequest.getContent()).willReturn(JsonValue.json(properties));
        given(subjectContext.getCallerSubject()).willReturn(restSubject);
        Realm realm = realmTestHelper.mockRealm("abc");

        // Given...
        Context context = buildContextStructure(realm);
        BatchPolicyRequest.getBatchPolicyRequest(context, actionRequest, tokenManager);
    }

    private Context buildContextStructure(final Realm realm) {
        return ClientContext.newInternalClientContext(new RealmContext(subjectContext, realm));
    }

}
