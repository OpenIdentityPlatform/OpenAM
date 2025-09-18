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
 * Copyright 2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.shared.guice;

import jakarta.inject.Inject;
import org.forgerock.http.Client;
import org.forgerock.http.handler.HttpClientHandler;

import com.google.inject.Provider;

/**
 * This class provides Guice with instances of Client that contain a HttpClientHandler. The underlying HttpClientHandler
 * is registered with the ShutdownManager to tidy up threads when the server is stopped.
 * @since 13.5.0
 */
public class CloseableHttpClientProvider implements Provider<Client> {

    /** The commons HttpClientHandler. */
    private final HttpClientHandler httpClientHandler;

    @Inject
    public CloseableHttpClientProvider(HttpClientHandler httpClientHandler) {
        this.httpClientHandler = httpClientHandler;
    }

    /**
     * Creates an instance of Client, assuring that underlying commitments are observer, i.e. listening for shutdown.
     * @return An instance of Client, or null if an exception occurred during creation.
     */
    @Override
    public Client get() {
        return new Client(httpClientHandler);
    }
}