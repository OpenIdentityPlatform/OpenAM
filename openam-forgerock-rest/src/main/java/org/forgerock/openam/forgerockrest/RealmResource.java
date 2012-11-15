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
 * Copyright 2012 ForgeRock Inc.
 */
package org.forgerock.openam.forgerockrest;

import java.lang.Exception;
import java.lang.String;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.sun.org.apache.xml.internal.security.utils.resolver.ResourceResolverException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.CollectionResourceProvider;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOTokenManager;

import java.security.AccessController;

import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idsvcs.opensso.IdentityServicesImpl;
import com.sun.identity.idsvcs.Token;

import com.sun.identity.idsvcs.IdentityDetails;
import com.sun.identity.idsvcs.Attribute;
import com.sun.identity.sm.OrganizationConfigManager;

/**
 * A simple {@code Map} based collection resource provider.
 */
public final class RealmResource implements CollectionResourceProvider {
    // TODO: filters, sorting, paged results.

    /*
     * Throughout this example backend we take care not to invoke result
     * handlers while holding locks since result handlers may perform blocking
     * IO operations.
     */

    private Set subRealms = null;

    /**
     * Creates a new empty backend.
     */
    public RealmResource() {
        // No implementation required.
        this.subRealms = null;
    }

    public RealmResource(Set subRealms) {
        this.subRealms = subRealms;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionCollection(final ServerContext context, final ActionRequest request,
                                 final ResultHandler<JsonValue> handler) {
        final ResourceException e =
                new NotSupportedException("Actions are not supported for resource instances");
        handler.handleError(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionInstance(final ServerContext context, final String resourceId, final ActionRequest request,
                               final ResultHandler<JsonValue> handler) {
        final ResourceException e =
                new NotSupportedException("Actions are not supported for resource Realms");
        handler.handleError(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createInstance(final ServerContext context, final CreateRequest request,
                               final ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Create is not supported for resource Realms");
        handler.handleError(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteInstance(final ServerContext context, final String resourceId, final DeleteRequest request,
                               final ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Delete is not supported for resource Realms");
        handler.handleError(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void patchInstance(final ServerContext context, final String resourceId, final PatchRequest request,
                              final ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Patch operations are not supported for resource Realms");
        handler.handleError(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void queryCollection(final ServerContext context, final QueryRequest request,
                                final QueryResultHandler handler) {

        for (Object theRealm : subRealms) {
            String realm = (String) theRealm;
            JsonValue val = new JsonValue(realm);
            Resource resource = new Resource("0", "0", val);
            handler.handleResource(resource);
        }
        handler.handleResult(new QueryResult());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readInstance(final ServerContext context, final String resourceId,
                             final ReadRequest request, final ResultHandler<Resource> handler) {
        JsonValue val = null;
        for (Object theRealm : subRealms) {
            String realm = (String) theRealm;
            if (realm.equalsIgnoreCase(resourceId)) {
                val = new JsonValue(realm);
            }
        }
        if (val != null) {
            Resource resource = new Resource("0", "0", val);
            handler.handleResult(resource);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateInstance(final ServerContext context, final String resourceId,
                               final UpdateRequest request, final ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Update operations are not supported for resource Realms");
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