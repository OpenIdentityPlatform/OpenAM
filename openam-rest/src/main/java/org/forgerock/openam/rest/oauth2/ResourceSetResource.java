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

package org.forgerock.openam.rest.oauth2;

import javax.inject.Inject;
import java.util.HashMap;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.openam.oauth2.resources.ResourceSetStoreFactory;
import org.forgerock.openam.rest.resource.RealmContext;

/**
 * <p>Resource Set resource to expose registered Resource Sets for a given user.</p>
 *
 * <p>Only non-modifiable operations allowed. To alter a Resource Set use the OAuth2 Resource Set Registration
 * endpoint.</p>
 *
 * @since 13.0.0
 */
public class ResourceSetResource implements CollectionResourceProvider {

    private final ResourceSetStoreFactory resourceSetStoreFactory;

    /**
     * Constructs a new ResourceSetResource instance.
     *
     * @param resourceSetStoreFactory An instance of the ResourceSetStoreFactory.
     */
    @Inject
    public ResourceSetResource(ResourceSetStoreFactory resourceSetStoreFactory) {
        this.resourceSetStoreFactory = resourceSetStoreFactory;
    }

    /**
     * Not supported.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request,
            ResultHandler<Resource> handler) {

        try {
            RealmContext realmContext = context.asContext(RealmContext.class);
            ResourceSetDescription resourceSet = resourceSetStoreFactory.create(realmContext.getResolvedRealm())
                    .read(resourceId);

            handler.handleResult(newResource(resourceId, new JsonValue(resourceSet.asMap())));

        } catch (org.forgerock.oauth2.core.exceptions.NotFoundException e) {
            handler.handleError(new NotFoundException("No resource set with uid, " + resourceId + ", found."));
        } catch (org.forgerock.oauth2.core.exceptions.ServerException e) {
            handler.handleError(new InternalServerErrorException(e));
        }
    }

    /**
     * Not supported.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request,
            ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException());
    }

    /**
     * Not supported.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request,
            ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException());
    }

    /**
     * Not supported.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request,
            ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException());
    }

    /**
     * Not supported.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(new NotSupportedException());
    }

    /**
     * Not supported.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
            ResultHandler<JsonValue> handler) {
        handler.handleError(new NotSupportedException());
    }

    /**
     * Not supported.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        handler.handleError(new NotSupportedException());
    }

    private Resource newResource(String id, JsonValue content) {
        return new Resource(id, Long.toString(content.hashCode()), content);
    }
}
