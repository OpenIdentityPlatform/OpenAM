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

/**
 * Factory provided to hide implementation details from the scripting module. Module should just be able to
 * ask for a Request without concern for the type.
 *
 * @since 12.0.0
 */
public class HttpClientRequestFactory {

    /**
     * Create a new empty {@link HttpClientRequest} that can be sent over a {@link org.forgerock.http.client.HttpClient}.
     *
     * @return An empty {@link HttpClientRequest}.
     */
    public HttpClientRequest createRequest() {
        return new SimpleHttpClientRequest();
    }

}
