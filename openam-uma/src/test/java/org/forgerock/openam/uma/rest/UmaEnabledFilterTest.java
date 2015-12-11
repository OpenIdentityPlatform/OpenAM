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

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.when;
import static org.mockito.Mockito.*;

import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.openam.core.RealmInfo;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.uma.UmaProviderSettings;
import org.forgerock.openam.uma.UmaProviderSettingsFactory;
import org.forgerock.services.context.ClientContext;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promise;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class UmaEnabledFilterTest {

    private static UmaProviderSettingsFactory notYetConfiguredFactory;
    private static UmaProviderSettingsFactory notEnabledFactory;
    private static UmaProviderSettingsFactory enabledFactory;
    private Context context;
    private RequestHandler requestHandler;

    @BeforeClass
    public static void setupFactories() throws Exception {
        notYetConfiguredFactory = mock(UmaProviderSettingsFactory.class);
        given(notYetConfiguredFactory.get(anyString()))
                .willThrow(NotFoundException.class);
        UmaProviderSettings notEnabled = mock(UmaProviderSettings.class);
        given(notEnabled.isEnabled()).willReturn(false);
        notEnabledFactory = mock(UmaProviderSettingsFactory.class);
        given(notEnabledFactory.get(anyString())).willReturn(notEnabled);
        UmaProviderSettings enabled = mock(UmaProviderSettings.class);
        given(enabled.isEnabled()).willReturn(true);
        enabledFactory = mock(UmaProviderSettingsFactory.class);
        given(enabledFactory.get(anyString())).willReturn(enabled);
    }

    @BeforeMethod
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = ClientContext.newInternalClientContext(new RealmContext(new RootContext()));
        requestHandler = mock(RequestHandler.class);
        when(requestHandler.handleAction(any(Context.class), any(ActionRequest.class)))
                .thenReturn(promise(newActionResponse(null)));
        when(requestHandler.handleCreate(any(Context.class), any(CreateRequest.class)))
                .thenReturn(promise(newResourceResponse(null, null, null)));
        when(requestHandler.handleDelete(any(Context.class), any(DeleteRequest.class)))
                .thenReturn(promise(newResourceResponse(null, null, null)));
        when(requestHandler.handlePatch(any(Context.class), any(PatchRequest.class)))
                .thenReturn(promise(newResourceResponse(null, null, null)));
        when(requestHandler.handleQuery(any(Context.class), any(QueryRequest.class), any(QueryResourceHandler.class)))
                .thenReturn(promise(newQueryResponse()));
        when(requestHandler.handleRead(any(Context.class), any(ReadRequest.class)))
                .thenReturn(promise(newResourceResponse(null, null, null)));
        when(requestHandler.handleUpdate(any(Context.class), any(UpdateRequest.class)))
                .thenReturn(promise(newResourceResponse(null, null, null)));
    }

    private <V> Promise<V, ResourceException> promise(V response) {
        return newResultPromise(response);
    }

    @DataProvider
    public Object[][] factories() {
        return new Object[][] {
                new Object[] { notYetConfiguredFactory, true },
                new Object[] { notEnabledFactory, true },
                new Object[] { enabledFactory, false }
        };
    }

    @Test(dataProvider = "factories")
    public void testFilterAction(UmaProviderSettingsFactory factory, boolean expectFailure) throws Exception {
        // Given
        UmaEnabledFilter filter = new UmaEnabledFilter(factory);
        ActionRequest request = Requests.newActionRequest("test", "test", "test");

        // When
        Promise<ActionResponse, ResourceException> promise = filter.filterAction(context, request, requestHandler);

        // Then
        checkResult(expectFailure, promise);
        if (expectFailure) {
            verifyNoMoreInteractions(requestHandler);
        } else {
            verify(requestHandler).handleAction(context, request);
        }
    }

    private void checkResult(boolean expectFailure, Promise<?, ResourceException> promise) {
        if (expectFailure) {
            assertThat(promise).failedWithException().isInstanceOf(NotSupportedException.class);
        } else {
            assertThat(promise).succeeded();
        }
    }

    @Test(dataProvider = "factories")
    public void testFilterCreate(UmaProviderSettingsFactory factory, boolean expectFailure) throws Exception {
        // Given
        UmaEnabledFilter filter = new UmaEnabledFilter(factory);
        CreateRequest request = Requests.newCreateRequest("test", json(object()));

        // When
        Promise<ResourceResponse, ResourceException> promise = filter.filterCreate(context, request, requestHandler);

        // Then
        checkResult(expectFailure, promise);
        if (expectFailure) {
            verifyNoMoreInteractions(requestHandler);
        } else {
            verify(requestHandler).handleCreate(context, request);
        }
    }

    @Test(dataProvider = "factories")
    public void testFilterDelete(UmaProviderSettingsFactory factory, boolean expectFailure) throws Exception {
        // Given
        UmaEnabledFilter filter = new UmaEnabledFilter(factory);
        DeleteRequest request = Requests.newDeleteRequest("test");

        // When
        Promise<ResourceResponse, ResourceException> promise = filter.filterDelete(context, request, requestHandler);

        // Then
        checkResult(expectFailure, promise);
        if (expectFailure) {
            verifyNoMoreInteractions(requestHandler);
        } else {
            verify(requestHandler).handleDelete(context, request);
        }
    }

    @Test(dataProvider = "factories")
    public void testFilterPatch(UmaProviderSettingsFactory factory, boolean expectFailure) throws Exception {
        // Given
        UmaEnabledFilter filter = new UmaEnabledFilter(factory);
        PatchRequest request = Requests.newPatchRequest("test", "test");

        // When
        Promise<ResourceResponse, ResourceException> promise = filter.filterPatch(context, request, requestHandler);

        // Then
        checkResult(expectFailure, promise);
        if (expectFailure) {
            verifyNoMoreInteractions(requestHandler);
        } else {
            verify(requestHandler).handlePatch(context, request);
        }
    }

    @Test(dataProvider = "factories")
    public void testFilterQuery(UmaProviderSettingsFactory factory, boolean expectFailure) throws Exception {
        // Given
        UmaEnabledFilter filter = new UmaEnabledFilter(factory);
        QueryResourceHandler resultHandler = mock(QueryResourceHandler.class);
        QueryRequest request = Requests.newQueryRequest("test");

        // When
        Promise<QueryResponse, ResourceException> promise = filter.filterQuery(context, request, resultHandler, requestHandler);

        // Then
        checkResult(expectFailure, promise);
        if (expectFailure) {
            verifyNoMoreInteractions(requestHandler);
        } else {
            verify(requestHandler).handleQuery(context, request, resultHandler);
        }
    }

    @Test(dataProvider = "factories")
    public void testFilterRead(UmaProviderSettingsFactory factory, boolean expectFailure) throws Exception {
        // Given
        UmaEnabledFilter filter = new UmaEnabledFilter(factory);
        ReadRequest request = Requests.newReadRequest("test");

        // When
        Promise<ResourceResponse, ResourceException> promise = filter.filterRead(context, request, requestHandler);

        // Then
        checkResult(expectFailure, promise);
        if (expectFailure) {
            verifyNoMoreInteractions(requestHandler);
        } else {
            verify(requestHandler).handleRead(context, request);
        }
    }

    @Test(dataProvider = "factories")
    public void testFilterUpdate(UmaProviderSettingsFactory factory, boolean expectFailure) throws Exception {
        // Given
        UmaEnabledFilter filter = new UmaEnabledFilter(factory);
        UpdateRequest request = Requests.newUpdateRequest("test", json(object()));

        // When
        Promise<ResourceResponse, ResourceException> promise = filter.filterUpdate(context, request, requestHandler);

        // Then
        checkResult(expectFailure, promise);
        if (expectFailure) {
            verifyNoMoreInteractions(requestHandler);
        } else {
            verify(requestHandler).handleUpdate(context, request);
        }
    }
}