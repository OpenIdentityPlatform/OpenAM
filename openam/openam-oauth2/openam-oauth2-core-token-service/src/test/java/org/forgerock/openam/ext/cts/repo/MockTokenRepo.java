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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2012 ForgeRock. All rights reserved.
 */

package org.forgerock.openam.ext.cts.repo;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.json.resource.SimpleJsonResource;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jonathan Scudder
 */
public class MockTokenRepo extends SimpleJsonResource {
    final static Logger LOGGER = Logger.getLogger(MockTokenRepo.class.toString());

    private HashMap<String, Object> primaryStore = new HashMap<String, Object>();
    private HashMap<String, Object> secondaryStore = new HashMap<String, Object>();

    @Override
    protected JsonValue create(JsonValue request) throws JsonResourceException {
        LOGGER.info("create called: " + request.toString());

        // TODO: validate request

        String primaryKey = request.get("value").get("id").asString();
        String secondaryKey = request.get("value").get("parent").asString();
        long expirationTime = 0L;

        if (primaryKey != null) {
            primaryStore.put(primaryKey, request.get("value"));
        }

        if (secondaryKey != null) {
            secondaryStore.put(secondaryKey, request.get("value"));
        }
        
        JsonValue retValue = new JsonValue(new HashMap<String, Object>());
        retValue.put("_id", primaryKey);
        retValue.put("_rev", 1); // TODO: relevance of revision
        return retValue;
    }

    @Override
    protected JsonValue read(JsonValue request) throws JsonResourceException {
        LOGGER.log(Level.INFO, "read called", request.toString());

        String primaryKey = request.get("id").asString();
        return (JsonValue) primaryStore.get(primaryKey);
    }

    @Override
    protected JsonValue update(JsonValue request) throws JsonResourceException {
        LOGGER.log(Level.INFO, "update called", request.toString());

        return create(request);
    }

    @Override
    protected JsonValue delete(JsonValue request) throws JsonResourceException {
        LOGGER.log(Level.INFO, "delete called", request.toString());

        String primaryKey = request.get("id").asString();
        
        JsonValue token = (JsonValue)primaryStore.get(primaryKey);
        secondaryStore.values().remove(token);
        primaryStore.remove(primaryKey);

        // TODO: confirm that returning the JsonValue representation of the token is in line with convention
        JsonValue retValue = new JsonValue(new HashMap<String, Object>());
        retValue.put("_id", primaryKey);
        retValue.put("_rev", 1); // TODO: relevance of revision
        return retValue;
    }

    @Override
    protected JsonValue patch(JsonValue request) throws JsonResourceException {
        LOGGER.log(Level.INFO, "patch called", request.toString());

        return create(request);
    }

    @Override
    protected JsonValue action(JsonValue request) throws JsonResourceException {
        LOGGER.log(Level.INFO, "action called", request.toString());

        throw new JsonResourceException(JsonResourceException.FORBIDDEN);
    }

    @Override
    protected JsonValue query(JsonValue request) throws JsonResourceException {
        LOGGER.log(Level.INFO, "query called", request.toString());

        // TODO TODO TODO
        return null;
    }


}
