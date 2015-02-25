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
package org.forgerock.http.client.request;

import java.util.Map;
import java.util.Set;

/**
 * Models the request that a script can send over a {@link org.forgerock.http.client.HttpClient}. Is designed to be a basic HTTP/1.1
 * request. See http://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html#sec5
 *
 * NB: 'HttpClientRequest' used rather than 'Request' to avoid clashes with {@link org.restlet.Request}.
 *
 * @since 12.0.0
 */
public interface HttpClientRequest {

    /**
     * Add a header field to the request.
     *
     * @param field The name of the header field to add.
     * @param value The value of the header field.
     */
    public void addHeader(String field, String value);

    /**
     * Add a query parameter to the request.
     *
     * @param field The name of the query parameter field to add.
     * @param value The value of the query parameter field.
     */
    public void addQueryParameter(String field, String value);

    /**
     * Add a cookie to the request.
     *
     * @param field The name of the cookie field to add.
     * @param value The value of the cookie field.
     */
    public void addCookie(String domain, String field, String value);

    /**
     * Set the method the request will use.
     *
     * @param method The method to use. See http://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html#sec5.1.1
     */
    public void setMethod(String method);

    /**
     * Set the URI that the request targets.
     *
     * @param uri The URI of the resource.
     */
    public void setUri(String uri);

    /**
     * Set the message entity.
     *
     * @param entity
     */
    public void setEntity(String entity);

    /**
     * Retrieve the headers set on the request.
     *
     * @return The headers set on the request.
     */
    public Map<String, String> getHeaders();

    /**
     * Retrieve the query parameters set on the request.
     *
     * @return The query parameters set on the request.
     */
    public Map<String, String> getQueryParameters();

    /**
     * Retrieve the cookies set on the request.
     *
     * @return The cookies set on the request.
     */
    public Set<HttpClientRequestCookie> getCookies();

    /**
     * Retrieve the method set on the request.
     *
     * @return The method set on the request.
     */
    public String getMethod();

    /**
     * Retrieve the URI that the request targets.
     *
     * @return The URI that the request targets.
     */
    public String getUri();

    /**
     * Retrieve the entity set on the request.
     *
     * @return The entity set on the request.
     */
    public String getEntity();

}
