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
 * Copyright 2012 ForgeRock AS.
 */
package org.forgerock.openam.forgerockrest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.exception.BadRequestException;
import org.forgerock.json.resource.exception.ConflictException;
import org.forgerock.json.resource.exception.InternalServerErrorException;
import org.forgerock.json.resource.exception.NotFoundException;
import org.forgerock.json.resource.exception.NotSupportedException;
import org.forgerock.json.resource.exception.ResourceException;
import org.forgerock.json.resource.provider.CollectionResourceProvider;
import org.forgerock.json.resource.provider.SingletonResourceProvider;
import org.forgerock.json.resource.provider.RequestHandler;
import org.forgerock.json.resource.provider.Router;
import org.forgerock.json.resource.provider.UriTemplateRoutingStrategy;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOTokenManager;
import java.security.AccessController;

import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.idm.AMIdentity;

/**
 * A simple {@code Map} based collection resource provider.
 */
public final class TestResource implements CollectionResourceProvider {

    public TestResource() {
        // No implementation required.
    }

    /**
     * {@inheritDoc}
     */

    public void actionCollection(final Context context, final ActionRequest request,
                                 final ResultHandler<JsonValue> handler) {
        final ResourceException e =
                new NotSupportedException("Actions are not supported for resource instances");
        handler.handleError(e);
    }

    /**
     * {@inheritDoc}
     */

    public void actionInstance(final Context context, final ActionRequest request,
                               final ResultHandler<JsonValue> handler) {
        final ResourceException e =
                new NotSupportedException("Actions are not supported for resource instances");
        handler.handleError(e);
    }

    /**
     * {@inheritDoc}
     */

    public void createInstance(final Context context, final CreateRequest request,
                               final ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Actions are not supported for resource instances");
        handler.handleError(e);
    }

    /**
     * {@inheritDoc}
     */

    public void deleteInstance(final Context context, final DeleteRequest request,
                               final ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Actions are not supported for resource instances");
        handler.handleError(e);
    }

    /**
     * {@inheritDoc}
     */

    public void patchInstance(final Context context, final PatchRequest request,
                              final ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Patch operations are not supported");
        handler.handleError(e);
    }

    /**
     * {@inheritDoc}
     */

    public void queryCollection(final Context context, final QueryRequest request,
                                final QueryResultHandler handler) {
        JsonValue val = new JsonValue("Test:queryCollection");
        Resource resource = new Resource("0","0",val)  ;
        handler.handleResource(resource);
        handler.handleResult(new QueryResult());

    }

    /**
     * {@inheritDoc}
     */

    public void readInstance(final Context context, final ReadRequest request,
                             final ResultHandler<Resource> handler) {
        final String id = request.getResourceId();
        JsonValue val = new JsonValue("Test: readInstance");
        Resource resource = new Resource("0","0",val)  ;
        handler.handleResult(resource);
    }

    /**
     * {@inheritDoc}
     */

    public void updateInstance(final Context context, final UpdateRequest request,
                               final ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Update operations are not supported");
        handler.handleError(e);
    }

    /*
     * Add the ID and revision to the JSON content so that they are included
     * with subsequent responses. We shouldn't really update the passed in
     * content in case it is shared by other components, but we'll do it here
     * anyway for simplicity.
     */
    private void addIdAndRevision(final Resource resource) throws ResourceException {
        final JsonValue content = resource.getContent();
        try {
            content.asMap().put("_id", resource.getId());
            content.asMap().put("_rev", resource.getRevision());
        } catch (final JsonValueException e) {
            throw new BadRequestException(
                    "The request could not be processed because the provided "
                            + "content is not a JSON object");
        }
    }

    private String getNextRevision(final String rev) throws ResourceException {
        try {
            return String.valueOf(Integer.parseInt(rev) + 1);
        } catch (final NumberFormatException e) {
            throw new InternalServerErrorException("Malformed revision number '" + rev
                    + "' encountered while updating a resource");
        }
    }



}