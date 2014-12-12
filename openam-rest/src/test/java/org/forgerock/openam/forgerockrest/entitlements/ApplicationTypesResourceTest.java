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

import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.shared.debug.Debug;
import java.util.List;
import javax.security.auth.Subject;
import static org.fest.assertions.Assertions.assertThat;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.forgerockrest.entitlements.wrappers.ApplicationTypeManagerWrapper;
import org.forgerock.openam.forgerockrest.entitlements.wrappers.ApplicationTypeWrapper;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.mockito.ArgumentCaptor;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ApplicationTypesResourceTest {

    private ApplicationTypesResource testResource;

    private ApplicationTypeManagerWrapper typeManager;
    private ApplicationTypeManagerWrapper mockApplicationTypeManager;
    private Debug mockDebug;

    @BeforeMethod
    public void setUp() {
        typeManager = mock(ApplicationTypeManagerWrapper.class);
        testResource = new ApplicationTypesResource(typeManager, mock(Debug.class));
        mockApplicationTypeManager = mock(ApplicationTypeManagerWrapper.class);
        mockDebug = mock(Debug.class);

        testResource = new ApplicationTypesResource(mockApplicationTypeManager, mockDebug) {
            @Override
            protected List<JsonValue> jsonify(List<ApplicationTypeWrapper> types) {
                return null;
            }
        };
    }

    @Test
    public void undefinedSubjectShouldFail() {
        //given
        SSOTokenContext mockSubjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext);
        ServerContext mockServerContext = new ServerContext(realmContext);

        Subject subject = null;
        given(mockSubjectContext.getCallerSubject()).willReturn(subject);

        ReadRequest request = mock(ReadRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);

        //when
        testResource.readInstance(mockServerContext, "test", request, handler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler, times(1)).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.INTERNAL_ERROR);

    }

    @Test
    public void readShouldFailOnInvalidApplicationType() {
        //given
        SSOTokenContext mockSubjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext);
        ServerContext mockServerContext = new ServerContext(realmContext);

        Subject subject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(subject);

        ReadRequest request = mock(ReadRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);

        //when
        testResource.readInstance(mockServerContext, "test", request, handler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler, times(1)).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.NOT_FOUND);
    }

    @Test
    public void shouldReadInstanceCorrectly() throws IllegalAccessException, InstantiationException {
        //given
        SSOTokenContext mockSubjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext);
        ServerContext mockServerContext = new ServerContext(realmContext);

        Subject subject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(subject);

        ReadRequest request = mock(ReadRequest.class);
        ResultHandler<Resource> handler = mock(ResultHandler.class);

        ApplicationType mockApplicationType = new ApplicationType("test", null, null, null, null);

        given(mockApplicationTypeManager.getApplicationType(subject, "test")).willReturn(mockApplicationType);

        //when
        testResource.readInstance(mockServerContext, "test", request, handler);

        //then
        ArgumentCaptor<Resource> captor = ArgumentCaptor.forClass(Resource.class);
        verify(handler, times(1)).handleResult(captor.capture());
    }



}
