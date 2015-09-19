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

package org.forgerock.openam.uma.rest;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.test.assertj.AssertJResourceResponseAssert.assertThat;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.util.Collections;

import com.google.inject.Provider;
import com.sun.identity.common.LocaleContext;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.openam.oauth2.resources.labels.LabelType;
import org.forgerock.openam.oauth2.resources.labels.ResourceSetLabel;
import org.forgerock.openam.oauth2.resources.labels.UmaLabelsStore;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UmaLabelResourceTest {
    public static final String NAME_ATTRIBUTE_NAME = "name";
    public static final String TYPE_ATTRIBUTE_NAME = "type";
    private static final String ID_ATTRIBUTE_NAME = "_id";
    public static final String REALM_NAME = "REALM";
    public static final String RESOURCE_OWNER_ID = "RESOURCE_OWNER_ID";
    public static final String LABEL_NAME = "LABEL_NAME";
    public static final String LABEL_TYPE = "SYSTEM";
    public static final String LABEL_ID = "LABEL_ID";
    private UmaLabelResource umaLabelResource;
    private UmaLabelsStore umaLabelsStore;
    private ContextHelper contextHelper;
    private Context serverContext;
    private CreateRequest createRequest;
    private DeleteRequest deleteRequest;
    private QueryRequest queryRequest;
    private QueryResourceHandler queryResourceHandler;
    private ClientRegistrationStore clientRegistrationStore;
    private LocaleContext localContext;

    @BeforeMethod
    public void setup() {
        contextHelper = mock(ContextHelper.class);
        umaLabelsStore = mock(UmaLabelsStore.class);
        clientRegistrationStore = mock(ClientRegistrationStore.class);
        localContext = mock(LocaleContext.class);
        umaLabelResource = new UmaLabelResource(umaLabelsStore, contextHelper, clientRegistrationStore, new Provider<LocaleContext>() {
            @Override
            public LocaleContext get() {
                return localContext;
            }
        });
        serverContext = mock(Context.class);
        createRequest = mock(CreateRequest.class);
        queryRequest = mock(QueryRequest.class);
        queryResourceHandler = mock(QueryResourceHandler.class);
        deleteRequest = mock(DeleteRequest.class);
    }

    /**
     * Should successfully create an UMA Label.
     */
    @Test
    public void createInstance() throws ResourceException {
        //Given
        JsonValue umaLabel = json(object(
                field(NAME_ATTRIBUTE_NAME, LABEL_NAME),
                field(TYPE_ATTRIBUTE_NAME, LABEL_TYPE)
        ));

        given(createRequest.getContent()).willReturn(umaLabel);
        given(contextHelper.getRealm(serverContext)).willReturn(REALM_NAME);
        given(contextHelper.getUserId(serverContext)).willReturn(RESOURCE_OWNER_ID);
        final ResourceSetLabel resourceSetLabel = new ResourceSetLabel(null, LABEL_NAME, LabelType.valueOf(LABEL_TYPE), Collections.EMPTY_SET);
        given(umaLabelsStore.create(REALM_NAME, RESOURCE_OWNER_ID, resourceSetLabel)).willReturn(resourceSetLabel);

        //When
        Promise<ResourceResponse, ResourceException> promise = umaLabelResource.createInstance(serverContext, createRequest);

        //Then
        verify(umaLabelsStore, Mockito.times(1)).create(REALM_NAME, RESOURCE_OWNER_ID, resourceSetLabel);
        assertThat(promise).succeeded();
    }

    @Test
    public void createInstanceFailsWhenTypePropertyIsMissing() {
        //Given
        JsonValue umaLabel = json(object(
                field(NAME_ATTRIBUTE_NAME, LABEL_NAME)
        ));

        given(createRequest.getContent()).willReturn(umaLabel);
        given(contextHelper.getRealm(serverContext)).willReturn(REALM_NAME);
        given(contextHelper.getUserId(serverContext)).willReturn(RESOURCE_OWNER_ID);

        //When
        Promise<ResourceResponse, ResourceException> promise = umaLabelResource.createInstance(serverContext, createRequest);

        //Then
        verifyZeroInteractions(umaLabelsStore);
        assertThat(promise).failedWithResourceException().isInstanceOf(BadRequestException.class);
    }

    @Test
    public void createInstanceFailsWhenNamePropertyIsMissing() {
        //Given
        JsonValue umaLabel = json(object(
                field(TYPE_ATTRIBUTE_NAME, LABEL_TYPE)
        ));

        given(createRequest.getContent()).willReturn(umaLabel);
        given(contextHelper.getRealm(serverContext)).willReturn(REALM_NAME);
        given(contextHelper.getUserId(serverContext)).willReturn(RESOURCE_OWNER_ID);

        //When
        Promise<ResourceResponse, ResourceException> promise = umaLabelResource.createInstance(serverContext, createRequest);

        //Then
        verifyZeroInteractions(umaLabelsStore);
        assertThat(promise).failedWithResourceException().isInstanceOf(BadRequestException.class);
    }

    /**
     * Should successfully delete a label.
     */
    @Test
    public void deleteLabel() throws ResourceException {
        //Given
        final ResourceSetLabel resourceSetLabel = new ResourceSetLabel(LABEL_ID, LABEL_NAME, LabelType.valueOf(LABEL_TYPE), Collections.EMPTY_SET);
        given(contextHelper.getRealm(serverContext)).willReturn(REALM_NAME);
        given(contextHelper.getUserId(serverContext)).willReturn(RESOURCE_OWNER_ID);
        given(umaLabelsStore.read(REALM_NAME, RESOURCE_OWNER_ID, LABEL_ID)).willReturn(resourceSetLabel);
        given(deleteRequest.getRevision()).willReturn(String.valueOf(resourceSetLabel.hashCode()));

        //When
        Promise<ResourceResponse, ResourceException> promise = umaLabelResource.deleteInstance(serverContext, LABEL_ID, deleteRequest);

        //Then
        verify(umaLabelsStore, Mockito.times(1)).delete(REALM_NAME, RESOURCE_OWNER_ID, LABEL_ID);
        assertThat(promise).succeeded();
    }

    /**
     * Query labels should succeed with query 'true'
     */
    @Test
    public void queryLabels() throws ResourceException {
        //Given
        given(umaLabelsStore.list(REALM_NAME, RESOURCE_OWNER_ID)).willReturn(Collections.<ResourceSetLabel>emptySet());
        given(contextHelper.getRealm(serverContext)).willReturn(REALM_NAME);
        given(contextHelper.getUserId(serverContext)).willReturn(RESOURCE_OWNER_ID);
        given(queryRequest.getQueryFilter()).willReturn(QueryFilter.<JsonPointer>alwaysTrue());

        //When
        Promise<QueryResponse, ResourceException> promise = umaLabelResource.queryCollection(serverContext,
                queryRequest, queryResourceHandler);

        //Then
        verify(umaLabelsStore, Mockito.times(1)).list(REALM_NAME, RESOURCE_OWNER_ID);
        assertThat(promise).succeeded();
    }

    /**
     * Querying labels should fail with anything other than 'true'
     */
    @Test
    public void queryLabelsFails() throws ResourceException {
        //Given
        given(umaLabelsStore.list(REALM_NAME, RESOURCE_OWNER_ID)).willReturn(Collections.<ResourceSetLabel>emptySet());
        given(queryRequest.getQueryFilter()).willReturn(QueryFilter.<JsonPointer>alwaysFalse());

        //When
        Promise<QueryResponse, ResourceException> promise = umaLabelResource.queryCollection(serverContext,
                queryRequest, queryResourceHandler);

        //Then
        verifyZeroInteractions(umaLabelsStore);
        assertThat(promise).failedWithException().isInstanceOf(BadRequestException.class);
    }

}
