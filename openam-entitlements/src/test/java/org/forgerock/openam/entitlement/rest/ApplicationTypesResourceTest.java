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

import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.testng.AssertJUnit.*;

import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.shared.debug.Debug;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.security.auth.Subject;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.services.context.ClientContext;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.entitlement.rest.ApplicationTypesResource;
import org.forgerock.openam.entitlement.rest.wrappers.ApplicationTypeManagerWrapper;
import org.forgerock.openam.entitlement.rest.wrappers.ApplicationTypeWrapper;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.util.promise.Promise;
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
            protected List<ResourceResponse> getResourceResponses(List<ApplicationTypeWrapper> types) {
                return null;
            }
        };
    }

    @Test (expectedExceptions = InternalServerErrorException.class)
    public void undefinedSubjectShouldFail() throws ResourceException {
        //given
        SSOTokenContext mockSubjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);

        Subject subject = null;
        given(mockSubjectContext.getCallerSubject()).willReturn(subject);

        ReadRequest request = mock(ReadRequest.class);

        //when
        Promise<ResourceResponse, ResourceException> result =
                testResource.readInstance(mockServerContext, "test", request);

        result.getOrThrowUninterruptibly();
    }

    @Test (expectedExceptions = NotFoundException.class)
    public void readShouldFailOnInvalidApplicationType() throws ResourceException {
        //given
        SSOTokenContext mockSubjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);

        Subject subject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(subject);

        ReadRequest request = mock(ReadRequest.class);

        //when
        Promise<ResourceResponse, ResourceException> result =
                testResource.readInstance(mockServerContext, "test", request);

        //then
        result.getOrThrowUninterruptibly();
    }

    @Test
    public void shouldReadInstanceCorrectly() throws IllegalAccessException, InstantiationException,
            ExecutionException, InterruptedException {
        //given
        SSOTokenContext mockSubjectContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSubjectContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);

        Subject subject = new Subject();
        given(mockSubjectContext.getCallerSubject()).willReturn(subject);

        ReadRequest request = mock(ReadRequest.class);

        ApplicationType mockApplicationType = new ApplicationType("test", null, null, null, null);

        given(mockApplicationTypeManager.getApplicationType(subject, "test")).willReturn(mockApplicationType);

        //when
        Promise<ResourceResponse, ResourceException> result =
                testResource.readInstance(mockServerContext, "test", request);

        //then
        assertTrue(result.get().getId().equals("test"));
    }



}
