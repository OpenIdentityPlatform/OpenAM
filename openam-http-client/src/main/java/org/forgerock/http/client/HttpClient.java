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
package org.forgerock.http.client;

import org.forgerock.http.client.request.HttpClientRequest;
import org.forgerock.http.client.response.HttpClientResponse;

import java.io.UnsupportedEncodingException;

/**
 * Models an HTTP client that can be used to send {@link org.forgerock.http.client.request.HttpClientRequest} objects and receive
 * {@link org.forgerock.http.client.response.HttpClientResponse} objects.
 *
 * @since 12.0.0
 */
public interface HttpClient {

    /**
     * Perform the HTTP/1.1 request and return an HTTP/1.1 response as the result.
     *
     * @param request The Request to perform.
     * @return The response received as a result of sending the request.
     *
     */
    public HttpClientResponse perform(HttpClientRequest request) throws UnsupportedEncodingException;

}
