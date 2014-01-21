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
 * Copyright 2012-2014 ForgeRock Inc.
 */

package org.forgerock.openam.rest.dashboard;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.PermanentException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.dashboard.Dashboard;
import org.forgerock.openam.dashboard.ServerContextHelper;

import java.util.HashMap;

/**
 * A simple {@code Map} based collection resource provider.
 */
public final class DashboardResource implements CollectionResourceProvider {

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionCollection(final ServerContext context, final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        handler.handleError(new NotSupportedException("Actions are not supported for resource instances"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionInstance(final ServerContext context, final String resourceId, final ActionRequest request,
            final ResultHandler<JsonValue> handler) {
        handler.handleError(new NotSupportedException("Actions are not supported for resource instances"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createInstance(final ServerContext context, final CreateRequest request,
            final ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException("Actions are not supported for resource instances"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteInstance(final ServerContext context, final String resourceId, final DeleteRequest request,
            final ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException("Actions are not supported for resource instances"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void patchInstance(final ServerContext context, final String resourceId, final PatchRequest request,
            final ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException("Patch operations are not supported"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void queryCollection(final ServerContext context, final QueryRequest request,
            final QueryResultHandler handler) {
        handler.handleError(new NotSupportedException("Query operations are not supported"));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readInstance(final ServerContext context, final String resourceId, final ReadRequest request,
            final ResultHandler<Resource> handler) {
        try {
            SSOTokenManager mgr = SSOTokenManager.getInstance();
            SSOToken token = mgr.createSSOToken(ServerContextHelper.getCookieFromServerContext(context));

            JsonValue val = new JsonValue(new HashMap<String, Object>());
            if (resourceId.equals("defined")) {
                val = Dashboard.getDefinitions(token);
            }
            if (resourceId.equals("available")) {
                val = Dashboard.getAllowedDashboard(token);
            }
            if (resourceId.equals("assigned")) {
                val = Dashboard.getAssignedDashboard(token);
            }
            Resource resource = new Resource("0", String.valueOf(System.currentTimeMillis() ), val);
            handler.handleResult(resource);
        } catch (SSOException ex) {
            handler.handleError(new PermanentException(401, "Unauthorized", null));
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateInstance(final ServerContext context, final String resourceId, final UpdateRequest request,
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