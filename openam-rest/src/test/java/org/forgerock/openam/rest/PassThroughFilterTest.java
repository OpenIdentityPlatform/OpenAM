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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import org.forgerock.json.resource.*;
import org.forgerock.services.context.Context;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link PassThroughFilter}.
 *
 * @since 13.5.0
 */
public class PassThroughFilterTest {

    @Mock
    private Context context;

    @Mock
    private RequestHandler nextRequestHandler;

    @BeforeMethod
    public void setup() throws Exception {
        initMocks(this);
    }

    @Test
    public void filterActionShouldPassThroughToNextHandler() {

        ActionRequest request = mock(ActionRequest.class);
        Filter filter = new PassThroughFilter();

        filter.filterAction(context, request, nextRequestHandler);

        verify(nextRequestHandler, times(1)).handleAction(context, request);
    }

    @Test
    public void filterCreateShouldPassThroughToNextHandler() {

        CreateRequest request = mock(CreateRequest.class);
        Filter filter = new PassThroughFilter();

        filter.filterCreate(context, request, nextRequestHandler);

        verify(nextRequestHandler, times(1)).handleCreate(context, request);
    }

    @Test
    public void filterDeleteShouldPassThroughToNextHandler() {

        DeleteRequest request = mock(DeleteRequest.class);
        Filter filter = new PassThroughFilter();

        filter.filterDelete(context, request, nextRequestHandler);

        verify(nextRequestHandler, times(1)).handleDelete(context, request);
    }

    @Test
    public void filterPatchShouldPassThroughToNextHandler() {

        PatchRequest request = mock(PatchRequest.class);
        Filter filter = new PassThroughFilter();

        filter.filterPatch(context, request, nextRequestHandler);

        verify(nextRequestHandler, times(1)).handlePatch(context, request);
    }

    @Test
    public void filterQueryShouldPassThroughToNextHandler() {

        QueryRequest request = mock(QueryRequest.class);
        QueryResourceHandler resourceHandler = mock(QueryResourceHandler.class);
        Filter filter = new PassThroughFilter();

        filter.filterQuery(context, request, resourceHandler, nextRequestHandler);

        verify(nextRequestHandler, times(1)).handleQuery(context, request, resourceHandler);
    }

    @Test
    public void filterReadShouldPassThroughToNextHandler() {

        ReadRequest request = mock(ReadRequest.class);
        Filter filter = new PassThroughFilter();

        filter.filterRead(context, request, nextRequestHandler);

        verify(nextRequestHandler, times(1)).handleRead(context, request);
    }

    @Test
    public void filterUpdateShouldPassThroughToNextHandler() {

        UpdateRequest request = mock(UpdateRequest.class);
        Filter filter = new PassThroughFilter();

        filter.filterUpdate(context, request, nextRequestHandler);

        verify(nextRequestHandler, times(1)).handleUpdate(context, request);
    }
}
