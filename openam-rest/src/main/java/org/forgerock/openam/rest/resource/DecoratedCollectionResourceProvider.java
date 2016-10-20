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

package org.forgerock.openam.rest.resource;

import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;

/**
 * Allows for an existing {@link CollectionResourceProvider} to be easily wrapped to add additional behaviour.
 *
 * @since 13.0.0
 */
public abstract class DecoratedCollectionResourceProvider implements CollectionResourceProvider {

    private final CollectionResourceProvider wrappedCollectionResourceProvider;

    protected DecoratedCollectionResourceProvider(CollectionResourceProvider wrappedCollectionResourceProvider) {
        Reject.ifNull(wrappedCollectionResourceProvider);
        this.wrappedCollectionResourceProvider = wrappedCollectionResourceProvider;
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(
            Context context, ActionRequest actionRequest) {
        return wrappedCollectionResourceProvider.actionCollection(context, actionRequest);
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(
            Context context, String resourceId, ActionRequest actionRequest) {
        return wrappedCollectionResourceProvider.actionInstance(context, resourceId, actionRequest);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(
            Context context, CreateRequest createRequest) {
        return wrappedCollectionResourceProvider.createInstance(context, createRequest);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(
            Context context, String resourceId, DeleteRequest deleteRequest) {
        return wrappedCollectionResourceProvider.deleteInstance(context, resourceId, deleteRequest);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(
            Context context, String resourceId, PatchRequest patchRequest) {
        return wrappedCollectionResourceProvider.patchInstance(context, resourceId, patchRequest);
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(
            Context context, QueryRequest queryRequest, QueryResourceHandler queryResourceHandler) {
        return wrappedCollectionResourceProvider.queryCollection(context, queryRequest, queryResourceHandler);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(
            Context context, String resourceId, ReadRequest readRequest) {
        return wrappedCollectionResourceProvider.readInstance(context, resourceId, readRequest);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(
            Context context, String resourceId, UpdateRequest updateRequest) {
        return wrappedCollectionResourceProvider.updateInstance(context, resourceId, updateRequest);
    }

}
