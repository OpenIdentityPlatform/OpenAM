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
import com.sun.identity.entitlement.SubjectAttributesManager;
import com.sun.identity.shared.debug.Debug;
import java.util.HashSet;
import java.util.Set;
import javax.security.auth.Subject;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.forgerockrest.entitlements.query.QueryResultHandlerBuilder;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import static org.junit.Assert.assertTrue;
import org.mockito.ArgumentCaptor;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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
    public void shouldPerformQueryForSubjectAttributes() throws EntitlementException {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        ServerContext mockServerContext = new ServerContext(realmContext);
        Subject mockSubject = new Subject();
        given(mockSSOTokenContext.getCallerSubject()).willReturn(mockSubject);
        QueryRequest mockRequest = mock(QueryRequest.class);
        QueryResultHandler mockHandler = mock(QueryResultHandler.class);
        QueryResultHandler resultHandler = QueryResultHandlerBuilder.withPagingAndSorting(mockHandler, mockRequest);
        Set<String> attributes = new HashSet<String>();
        attributes.add("attr");
        attributes.add("attr2");
        given(mockSAM.getAvailableSubjectAttributeNames()).willReturn(attributes);

        //when
        subjectAttributesResource.queryCollection(mockServerContext, mockRequest, resultHandler);

        //then
        ArgumentCaptor<Resource> captor = ArgumentCaptor.forClass(Resource.class);
        verify(mockHandler, times(2)).handleResource(captor.capture());

        ArgumentCaptor<QueryResult> captor2 = ArgumentCaptor.forClass(QueryResult.class);
        verify(mockHandler, times(1)).handleResult(captor2.capture());
    }

    @Test
    public void shouldReturnNoResultWhenNoAttributes() throws EntitlementException {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        ServerContext mockServerContext = new ServerContext(realmContext);
        Subject mockSubject = new Subject();
        given(mockSSOTokenContext.getCallerSubject()).willReturn(mockSubject);
        QueryRequest mockRequest = mock(QueryRequest.class);
        QueryResultHandler mockHandler = mock(QueryResultHandler.class);
        QueryResultHandler resultHandler = QueryResultHandlerBuilder.withPagingAndSorting(mockHandler, mockRequest);
        Set<String> attributes = new HashSet<String>();
        given(mockSAM.getAvailableSubjectAttributeNames()).willReturn(attributes);

        //when
        subjectAttributesResource.queryCollection(mockServerContext, mockRequest, resultHandler);

        //then
        ArgumentCaptor<QueryResult> captor2 = ArgumentCaptor.forClass(QueryResult.class);
        verify(mockHandler, times(1)).handleResult(captor2.capture());
    }

    @Test
    public void shouldErrorWhenAttributeRetrievalFails() throws EntitlementException {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        ServerContext mockServerContext = new ServerContext(realmContext);
        Subject mockSubject = new Subject();
        given(mockSSOTokenContext.getCallerSubject()).willReturn(mockSubject);
        QueryRequest mockRequest = mock(QueryRequest.class);
        QueryResultHandler mockHandler = mock(QueryResultHandler.class);
        QueryResultHandler resultHandler = QueryResultHandlerBuilder.withPagingAndSorting(mockHandler, mockRequest);
        given(mockSAM.getAvailableSubjectAttributeNames()).willThrow(new EntitlementException(401));

        //when
        subjectAttributesResource.queryCollection(mockServerContext, mockRequest, resultHandler);

        //then
        verify(mockDebug, times(1)).error(anyString());

        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockHandler, times(1)).handleError(captor.capture());
        assertTrue(captor.getValue().getCode() == 500); //500 is translated from 401 internally in EntitlementException
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
