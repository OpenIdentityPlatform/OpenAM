/*
 * Copyright (c) 2012 ForgeRock AS. All rights reserved.
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
 * information: "Portions Copyrighted [2012] [ForgeRock Inc]".
 *
 */

package org.forgerock.openam.ext.cts;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.json.resource.SimpleJsonResource;

/**
 * The CoreTokenService handles the store and retrieval of tokens, whether persisted, distributed, encrypted or not.
 * Backend store is not necessarily in a single store, and flexible processing will be introduced at a later stage.
 * In this version, the requests are forwarded directly to a single repo.
 *
 * Note that this class is a prototype and highly likely to change.
 *
 * @author Jonathan Scudder
 */
public class CoreTokenService extends SimpleJsonResource {

    private JsonResource resource;

    /**
     * Simple constructor that sets up the CoreTokenService with a single repo.
     * @param resource the underlying repo that will store the token
     */
    public CoreTokenService(JsonResource resource) {
        this.resource = resource;
    }
    
    @Override
    protected JsonValue create(JsonValue request) throws JsonResourceException {
        return resource.handle(request);
    }

    @Override
    protected JsonValue read(JsonValue request) throws JsonResourceException {
        return resource.handle(request);
    }

    @Override
    protected JsonValue update(JsonValue request) throws JsonResourceException {
        return resource.handle(request);
    }

    @Override
    protected JsonValue delete(JsonValue request) throws JsonResourceException {
        return resource.handle(request);
    }

    @Override
    protected JsonValue patch(JsonValue request) throws JsonResourceException {
        return resource.handle(request);
    }

    @Override
    protected JsonValue query(JsonValue request) throws JsonResourceException {
        return resource.handle(request);
    }

    @Override
    protected JsonValue action(JsonValue request) throws JsonResourceException {
        return resource.handle(request);
    }

    @Override
    protected void onException(Exception exception) throws JsonResourceException {
        super.onException(exception);
    }
}
