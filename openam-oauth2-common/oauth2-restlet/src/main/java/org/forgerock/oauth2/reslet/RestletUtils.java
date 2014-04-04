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

package org.forgerock.oauth2.reslet;

import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.resource.ResourceException;

import java.io.IOException;
import java.util.Map;

/**
 * @since 12.0.0
 */
public final class RestletUtils {

    public static String getAttribute(final Request request, final String name) {
        final Object value = request.getAttributes().get(name);
        return value == null ? null : value.toString();
    }

    private static String getQueryParameter(final Request request, final String name) {
        return request.getResourceRef().getQueryAsForm().getValuesMap().get(name);
    }

    public static String getParameter(final Request request, final String name) {
        String value = getAttribute(request, name);
        if (value != null) {
            return value;
        }

        if (request.getMethod().equals(Method.GET)) {
            return getQueryParameter(request, name);
        }

        if (request.getMethod().equals(Method.POST)) {
            if (request.getEntity() != null) {
                if (MediaType.APPLICATION_WWW_FORM.equals(request.getEntity().getMediaType())) {
                    Form form = new Form(request.getEntity());
                    // restore the entity body
                    request.setEntity(form.getWebRepresentation());
                    return form.getValuesMap().get(name);
                } else if (MediaType.APPLICATION_JSON
                        .equals(request.getEntity().getMediaType())) {
                    JacksonRepresentation<Map> representation =
                            new JacksonRepresentation<Map>(request.getEntity(), Map.class);
                    try {
                        return (String) representation.getObject().get(name);
                    } catch (IOException e) {
                        throw new ResourceException(e);
                    }
                }
            }
        }
        return null;
    }
}
