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

/**
 * Factory provided to hide implementation details from the scripting module. Module should just be able to
 * ask for an HttpClient without concern for the type.
 *
 * @since 12.0.0
 */
public class HttpClientFactory {

    /**
     * Create a new {@link HttpClient} that can be used to send {@link org.forgerock.http.client.request.HttpClientRequest}
     * objects and receive {@link org.forgerock.http.client.response.HttpClientResponse} objects.
     *
     * @return An {@link HttpClient}.
     */
    public HttpClient createHttpClient() {
        return new RestletHttpClient();
    }

}
