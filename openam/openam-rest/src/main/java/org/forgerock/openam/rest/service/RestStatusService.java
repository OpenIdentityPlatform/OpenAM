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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.service.StatusService;

import java.util.Map;

/**
 * Service to handle error statuses. If an exception is thrown then the status is pulled from the response and the
 * matching Json Resource exception is created and sent back to the client. This keeps the authenticate REST endpoint
 * in line with the CREST resource exceptions.
 *
 * @since 12.0.0
 */
public abstract class RestStatusService extends StatusService {

    /**
     * {@inheritDoc}
     */
    @Override
    public Representation getRepresentation(Status status, Request request, Response response) {
        final JsonValue jsonResponse;
        Throwable throwable = status.getThrowable();

        if (throwable != null && throwable.getMessage() != null) {

            //by checking resourceException explicitly, we can include the Details segment in the response
            if (throwable instanceof ResourceException) {
                jsonResponse = ((ResourceException) throwable).toJsonValue();
            } else {
                jsonResponse = ResourceException.getException(status.getCode(), throwable.getMessage()).toJsonValue();
            }

        } else if (status.getDescription() != null) {
            jsonResponse = ResourceException.getException(status.getCode(), status.getDescription()).toJsonValue();
        } else {
            jsonResponse = ResourceException.getException(status.getCode()).toJsonValue();
        }
        Map<String, Object> mapRepresentation = jsonResponse.asMap();
        return representMap(mapRepresentation);
    }

    protected abstract Representation representMap(Map<String, Object> map);
}
