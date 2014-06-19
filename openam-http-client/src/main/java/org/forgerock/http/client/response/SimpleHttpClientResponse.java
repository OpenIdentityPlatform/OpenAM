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
package org.forgerock.http.client.response;

import java.util.Map;

/**
 * A basic implementation of {@link HttpClientResponse} that a script can receive from sending a
 * {@link org.forgerock.http.client.request.HttpClientRequest} over a {@link org.forgerock.http.client.HttpClient}.
 *
 * @since 12.0.0
 */
public class SimpleHttpClientResponse implements HttpClientResponse {

    private Integer statusCode;
    private String reasonPhrase;
    private Map<String, String> headers;
    private String messageBody;
    private Map<String, String> cookies;

    /**
     * Creates a representation of an HTTP/1.1 response. Any or all of the fields can be set as null if desired.
     *
     * @param statusCode The three digit integer that corresponds to the HTTP status code.
     * @param reasonPhrase The HTTP reason phrase.
     * @param headers Any headers sent with the accessed resource.
     * @param messageBody The entity sent with the accessed resource.
     * @param cookies Any cookies sent with the accessed resource.
     */
    public SimpleHttpClientResponse(Integer statusCode, String reasonPhrase, Map<String, String> headers,
                                    String messageBody, Map<String, String> cookies) {
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
        this.headers = headers;
        this.messageBody = messageBody;
        this.cookies = cookies;
    }

    /**
     * {@inheritDoc}
     */
    public Integer getStatusCode() {
        return statusCode;
    }

    /**
     * {@inheritDoc}
     */
    public String getReasonPhrase() {
        return reasonPhrase;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasHeaders() {
        return (!(headers == null) || !(headers.isEmpty()));
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * {@inheritDoc}
     */
    public String getEntity() {
        return messageBody;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasCookies() {
        return (!(cookies == null) || !(cookies.isEmpty()));
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, String> getCookies() {
        return cookies;
    }
}
