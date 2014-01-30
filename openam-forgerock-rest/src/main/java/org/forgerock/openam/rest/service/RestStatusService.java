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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.rest.service;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.service.StatusService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Service to handle error statuses. If an exception is thrown then the status is pulled from the response and the
 * matching Json Resource exception is created and sent back to the client. This keeps the authenticate REST endpoint
 * in line with the CREST resource exceptions.
 *
 * @since 12.0.0
 */
public class RestStatusService extends StatusService {

    /**
     * {@inheritDoc}
     */
    @Override
    public Representation getRepresentation(Status status, Request request, Response response) {

        final JsonValue jsonResponse;
        if (status.getThrowable() != null && status.getThrowable().getMessage() != null) {
            jsonResponse =
                    ResourceException.getException(status.getCode(), status.getThrowable().getMessage()).toJsonValue();
        } else if (status.getDescription() != null) {
            jsonResponse =
                    ResourceException.getException(status.getCode(), status.getDescription()).toJsonValue();
        } else {
            jsonResponse = ResourceException.getException(status.getCode()).toJsonValue();
        }

        final ObjectMapper mapper = JsonValueBuilder.getObjectMapper();
        try {
            return new JacksonRepresentation<Map>(mapper.readValue(jsonResponse.toString(), Map.class));
        } catch (IOException e) {
            Map<String, Object> rep = new HashMap<String, Object>();
            rep.put("code", 500);
            rep.put("reason", "IOException");
            rep.put("message", e.getMessage());
            return new JsonRepresentation(rep);
        }
    }
}
