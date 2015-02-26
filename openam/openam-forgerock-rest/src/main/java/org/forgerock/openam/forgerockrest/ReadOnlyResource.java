/*
 * Copyright 2013 ForgeRock, Inc.
 *
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
 */

package org.forgerock.openam.forgerockrest;

import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;

/**
 * Represents a read only view of a resource.
 */
public abstract class ReadOnlyResource implements CollectionResourceProvider  {

    /**
     * Creates a NotSupportedException for a given operation type.
     *
     * @param type The type of operation which is not supported.
     * @return A NotSupportedException.
     */
    private NotSupportedException generateException(String type) {
        return new NotSupportedException(type + " are not supported for this Resource");
    }

    /**
     * Will throw an exception as Creates are not allowed for a Read Only Resource.
     *
     * {@inheritDoc}
     */
    public final void createInstance(ServerContext ctx, CreateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(generateException("Creates"));
    }

    /**
     * Will throw an exception as Deletes are not allowed for a Read Only Resource.
     *
     * {@inheritDoc}
     */
    public final void deleteInstance(ServerContext ctx, String resId, DeleteRequest request,
                               ResultHandler<Resource> handler) {
        handler.handleError(generateException("Deletes"));
    }

    /**
     * Will throw an exception as Patches are not allowed for a Read Only Resource.
     *
     * {@inheritDoc}
     */
    public final void patchInstance(ServerContext ctx, String resId, PatchRequest request,
                               ResultHandler<Resource> handler) {
        handler.handleError(generateException("Patches"));
    }

    /**
     * Will throw an exception as Updates are not allowed for a Read Only Resource.
     *
     * {@inheritDoc}
     */
    public final void updateInstance(ServerContext ctx, String resId, UpdateRequest request,
                               ResultHandler<Resource> handler) {
        handler.handleError(generateException("Updates"));
    }
}
