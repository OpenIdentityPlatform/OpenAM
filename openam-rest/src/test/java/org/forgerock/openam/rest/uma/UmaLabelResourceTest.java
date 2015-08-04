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

package org.forgerock.openam.rest.uma;

import java.util.Collections;

import com.google.inject.Provider;
import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.common.LocaleContext;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.openam.forgerockrest.UmaLabelResource;
import org.forgerock.openam.rest.resource.ContextHelper;
import org.forgerock.openam.oauth2.resources.labels.LabelType;
import org.forgerock.openam.oauth2.resources.labels.ResourceSetLabel;
import org.forgerock.openam.oauth2.resources.labels.UmaLabelsStore;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.forgerock.json.fluent.JsonValue.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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
    private ServerContext serverContext;
    private CreateRequest createRequest;
    private DeleteRequest deleteRequest;
    private ResultHandler<Resource> resultHandler;
    private QueryRequest queryRequest;
    private QueryResultHandler queryResultHandler;
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
        serverContext = mock(ServerContext.class);
        createRequest = mock(CreateRequest.class);
        queryRequest = mock(QueryRequest.class);
        resultHandler = mock(ResultHandler.class);
        queryResultHandler = mock(QueryResultHandler.class);
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
        umaLabelResource.createInstance(serverContext, createRequest, resultHandler);

        //Then
        verify(umaLabelsStore, Mockito.times(1)).create(REALM_NAME, RESOURCE_OWNER_ID, resourceSetLabel);
        verify(resultHandler).handleResult(Matchers.<Resource>anyObject());
    }

    /**
     * Should throw an error when "name" attribute is missing.
     */
    @Test
    public void createInstanceFails() {
        //Given
        JsonValue umaLabel = json(object(
                field(NAME_ATTRIBUTE_NAME, LABEL_NAME)
        ));

        given(createRequest.getContent()).willReturn(umaLabel);
        given(contextHelper.getRealm(serverContext)).willReturn(REALM_NAME);
        given(contextHelper.getUserId(serverContext)).willReturn(RESOURCE_OWNER_ID);

        //When
        umaLabelResource.createInstance(serverContext, createRequest, resultHandler);

        //Then
        verifyZeroInteractions(umaLabelsStore);
        verify(resultHandler).handleError(Matchers.<NotSupportedException>anyObject());
    }

    /**
     * Should throw an error when "type" attribute is missing.
     */
    @Test
    public void createInstanceFails2() {
        //Given
        JsonValue umaLabel = json(object(
                field(TYPE_ATTRIBUTE_NAME, LABEL_TYPE)
        ));

        given(createRequest.getContent()).willReturn(umaLabel);
        given(contextHelper.getRealm(serverContext)).willReturn(REALM_NAME);
        given(contextHelper.getUserId(serverContext)).willReturn(RESOURCE_OWNER_ID);

        //When
        umaLabelResource.createInstance(serverContext, createRequest, resultHandler);

        //Then
        verifyZeroInteractions(umaLabelsStore);
        verify(resultHandler).handleError(Matchers.<NotSupportedException>anyObject());
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
        umaLabelResource.deleteInstance(serverContext, LABEL_ID, deleteRequest, resultHandler);

        //Then
        verify(umaLabelsStore, Mockito.times(1)).delete(REALM_NAME, RESOURCE_OWNER_ID, LABEL_ID);
        verify(resultHandler).handleResult(Matchers.<Resource>anyObject());
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
        given(queryRequest.getQueryFilter()).willReturn(QueryFilter.alwaysTrue());

        //When
        umaLabelResource.queryCollection(serverContext, queryRequest, queryResultHandler);

        //Then
        verify(umaLabelsStore, Mockito.times(1)).list(REALM_NAME, RESOURCE_OWNER_ID);
        verify(queryResultHandler).handleResult(Matchers.<QueryResult>anyObject());
    }

    /**
     * Querying labels should fail with anything other than 'true'
     */
    @Test
    public void queryLabelsFails() throws ResourceException {
        //Given
        given(umaLabelsStore.list(REALM_NAME, RESOURCE_OWNER_ID)).willReturn(Collections.<ResourceSetLabel>emptySet());
        given(queryRequest.getQueryFilter()).willReturn(QueryFilter.alwaysFalse());

        //When
        umaLabelResource.queryCollection(serverContext, queryRequest, queryResultHandler);

        //Then
        verifyZeroInteractions(umaLabelsStore);
        verify(queryResultHandler).handleError(Matchers.<NotSupportedException>anyObject());
    }

}
