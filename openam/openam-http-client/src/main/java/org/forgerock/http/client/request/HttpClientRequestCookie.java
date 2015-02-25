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

import org.forgerock.util.Reject;

/**
 * Models a cookie which can be added to a {@link org.forgerock.http.client.request.HttpClientRequest}. Only used
 * internally to the {@link org.forgerock.http.client.request.HttpClientRequest},
 * {@link org.forgerock.http.client.request.SimpleHttpClientRequest} and
 * {@link org.forgerock.http.client.RestletHttpClient} classes. Needs to be public as
 * {@link org.forgerock.http.client.RestletHttpClient} is not in this package.
 *
 * @since 12.0.0
 */
public class HttpClientRequestCookie {

    private final String domain;
    private final String field;
    private final String value;

    protected HttpClientRequestCookie(String domain, String field, String value) {

        Reject.ifNull(domain, "domain cannot be null");
        Reject.ifNull(field, "field cannot be null");
        Reject.ifNull(value, "value cannot be null");

        this.domain = domain;
        this.field = field;
        this.value = value;
    }

    /**
     * Get the domain for which the cookie was set.
     *
     * @return The domain for which the cookie was set.
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Get the field of the cookie.
     *
     * @return The field of the cookie.
     */
    public String getField() {
        return field;
    }

    /**
     * Get the value of the cookie.
     *
     * @return The value of the cookie.
     */
    public String getValue() {
        return value;
    }
}
