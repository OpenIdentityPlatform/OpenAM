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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.entitlement.rest;

import static org.forgerock.json.resource.test.assertj.AssertJQueryResponseAssert.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.SubjectAttributesManager;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.ClientContext;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.entitlement.rest.SubjectAttributesResourceV1;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.util.promise.Promise;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.util.HashSet;
import java.util.Set;

public class SubjectAttributesResourceV1Test {

    private SubjectAttributesResourceV1 subjectAttributesResource;
    private Debug mockDebug = mock(Debug.class);
    private SubjectAttributesManager mockSAM;

    @BeforeMethod
    private void setUp() {
        subjectAttributesResource = new SubjectAttributesResource(mockDebug);
        mockSAM = mock(SubjectAttributesManager.class);
    }

    @Test
    public void shouldPerformQueryForSubjectAttributes() throws Exception {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);
        Subject mockSubject = new Subject();
        given(mockSSOTokenContext.getCallerSubject()).willReturn(mockSubject);
        QueryRequest mockRequest = mock(QueryRequest.class);
        QueryResourceHandler mockHandler = mock(QueryResourceHandler.class);
        Set<String> attributes = new HashSet<>();
        attributes.add("attr");
        attributes.add("attr2");
        given(mockSAM.getAvailableSubjectAttributeNames()).willReturn(attributes);

        //when
        Promise<QueryResponse, ResourceException> promise =
                subjectAttributesResource.queryCollection(mockServerContext, mockRequest, mockHandler);

        //then
        promise.getOrThrowUninterruptibly();
        ArgumentCaptor<ResourceResponse> captor = ArgumentCaptor.forClass(ResourceResponse.class);
        verify(mockHandler, times(2)).handleResource(captor.capture());
    }

    @Test
    public void shouldReturnNoResultWhenNoAttributes() throws Exception {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);
        Subject mockSubject = new Subject();
        given(mockSSOTokenContext.getCallerSubject()).willReturn(mockSubject);
        QueryRequest mockRequest = mock(QueryRequest.class);
        QueryResourceHandler mockHandler = mock(QueryResourceHandler.class);
        Set<String> attributes = new HashSet<>();
        given(mockSAM.getAvailableSubjectAttributeNames()).willReturn(attributes);

        //when
        Promise<QueryResponse, ResourceException> promise =
                subjectAttributesResource.queryCollection(mockServerContext, mockRequest, mockHandler);

        //then
        promise.getOrThrowUninterruptibly();
        ArgumentCaptor<ResourceResponse> captor = ArgumentCaptor.forClass(ResourceResponse.class);
        verify(mockHandler, never()).handleResource(captor.capture());
    }

    @Test
    public void shouldErrorWhenAttributeRetrievalFails() throws EntitlementException {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);
        Subject mockSubject = new Subject();
        given(mockSSOTokenContext.getCallerSubject()).willReturn(mockSubject);
        QueryRequest mockRequest = mock(QueryRequest.class);
        QueryResourceHandler mockHandler = mock(QueryResourceHandler.class);
        given(mockSAM.getAvailableSubjectAttributeNames()).willThrow(new EntitlementException(401));

        //when
        Promise<QueryResponse, ResourceException> promise =
                subjectAttributesResource.queryCollection(mockServerContext, mockRequest, mockHandler);

        //then
        verify(mockDebug, times(1)).error(anyString());
        assertThat(promise).failedWithResourceException().withCode(ResourceException.INTERNAL_ERROR);
    }

    private class SubjectAttributesResource extends SubjectAttributesResourceV1 {

        public SubjectAttributesResource(Debug debug) {
            super(debug);
        }

        @Override
        SubjectAttributesManager getSubjectAttributesManager(Subject mySubject, String realm) {
           return mockSAM;
        }
    }

}
