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

package org.forgerock.oauth2.restlet;

import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.json.fluent.JsonValue;
import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.ext.servlet.ServletUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * An implementation of a OAuth2Request for Restlet.
 *
 * @since 12.0.0
 */
public class RestletOAuth2Request extends OAuth2Request {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final Request request;
    private JsonValue body;

    /**
     * Constructs a new RestletOAuth2Request.
     *
     * @param request The Restlet request.
     */
    public RestletOAuth2Request(Request request) {
        this.request = request;
    }

    /**
     * {@inheritDoc}
     */
    public Request getRequest() {
        return request;
    }

    /**
     * Gets the specified parameter from the request. By attempting in the follow order:
     * <ul>
     * <li>as an attribute</li>
     * <li>as a query parameter</li>
     * <li>as the body</li>
     * </ul>
     *
     * @param name {@inheritDoc}
     * @param <T> {@inheritDoc}
     * @return {@inheritDoc}
     */
    public <T> T getParameter(String name) {
        Object value = getAttribute(request, name);
        if (value != null) {
            return (T) value;
        }

        //query param priority over body
        if (getQueryParameter(request, name) != null) {
            return (T) getQueryParameter(request, name);
        }

        if (request.getMethod().equals(Method.POST)) {
            if (request.getEntity() != null) {
                if (MediaType.APPLICATION_WWW_FORM.equals(request.getEntity().getMediaType())) {
                    Form form = new Form(request.getEntity());
                    // restore the entity body
                    request.setEntity(form.getWebRepresentation());
                    return (T) form.getValuesMap().get(name);
                } else if (MediaType.APPLICATION_JSON.equals(request.getEntity().getMediaType())) {
                    return (T) getBody().get(name).getObject();
                }
            }
        }
        return null;
    }

    /**
     * Gets the value for an attribute from the request with the specified name.
     *
     * @param request The request.
     * @param name The name.
     * @return The attribute value, may be {@code null}
     */
    private Object getAttribute(Request request, String name) {
        final Object value = request.getAttributes().get(name);
        return value;
    }

    /**
     * Gets the value for a query parameter from the request with the specified name.
     *
     * @param request The request.
     * @param name The name.
     * @return The query parameter value, may be {@code null}.
     */
    private String getQueryParameter(Request request, String name) {
        return request.getResourceRef().getQueryAsForm().getValuesMap().get(name);
    }

    /**
     *
     *
     * @return {@inheritDoc}
     */
    public JsonValue getBody() {
        if (body == null) {
            final JacksonRepresentation<Map> representation =
                    new JacksonRepresentation<Map>(request.getEntity(), Map.class);
            try {
                body = new JsonValue(representation.getObject());
            } catch (IOException e) {
                logger.error(e.getMessage());
                return JsonValue.json(JsonValue.object());
            }
        }
        return body;
    }

    @Override
    public Locale getLocale() {
        return ServletUtils.getRequest(request).getLocale();
    }
}
