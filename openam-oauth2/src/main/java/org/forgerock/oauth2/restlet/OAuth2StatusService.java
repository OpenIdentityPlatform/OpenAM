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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.oauth2.restlet;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.service.StatusService;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles generic Restlet exceptions.
 *
 * @since 11.0.0
 */
public class OAuth2StatusService extends StatusService {

    /**
     * Takes the status reason and description and creates a Json representation of them.
     *
     * @param status {@inheritDoc}
     * @param request {@inheritDoc}
     * @param response {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Representation getRepresentation(Status status, Request request, Response response) {
        Map<String, Object> rep = new HashMap<String, Object>();

        rep.put("error", status.getReasonPhrase());
        rep.put("error_description", status.getDescription());
        return new JsonRepresentation(rep);
    }
}

