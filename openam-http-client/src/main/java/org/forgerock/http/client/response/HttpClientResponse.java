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
 * Models the response that a script can receive from sending a
 * {@link org.forgerock.http.client.request.HttpClientRequest} over a {@link org.forgerock.http.client.HttpClient}.
 * Is designed to be a basic HTTP/1.1 response. See http://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html#sec6
 *
 * NB: 'HttpClientResponse' used rather than 'Response' to avoid clashes with {@link org.restlet.Response}.
 *
 * @since 12.0.0
 */
public interface HttpClientResponse {

    /**
     * Retrieve the status code of the accessed resource.
     *
     * @return A three digit integer that corresponds to an HTTP status code. Can be null if no status code was set.
     */
    public Integer getStatusCode();

    /**
     * Retrieve the reason phrase of the accessed resource.
     *
     * @return The HTTP reason phrase. Can be null if no status code was set.
     */
    public String getReasonPhrase();

    /**
     * Indicates if the accessed resource has headers.
     *
     * @return True if the accessed resource has headers.
     */
    public boolean hasHeaders();

    /**
     * Retrieve the headers sent with the accessed resource.
     *
     * @return The headers sent with the accessed resource. Can be implemented as returning null or an empty map
     * in the absence of any headers.
     */
    public Map<String, String> getHeaders();

    /**
     * Retrieve the entity sent with the accessed resource.
     *
     * @return The entity sent with the accessed resource. Can be null if no entity was set.
     */
    public String getEntity();

    /**
     * Indicates if the accessed resource had cookies.
     *
     * @return True if the accessed resource had cookies.
     */
    public boolean hasCookies();

    /**
     * Retrieve any cookies sent with the accessed resource.
     *
     * @return Any cookies sent with the accessed resource. Can be implemented as returning null or an empty map
     * in the absence of any cookies.
     */
    public Map<String, String> getCookies();

}
