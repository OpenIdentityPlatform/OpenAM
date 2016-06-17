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
 */

package org.forgerock.openam.shared.guice;

import java.io.IOException;

import javax.inject.Inject;

import org.forgerock.http.Client;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.util.thread.listener.ShutdownListener;
import org.forgerock.util.thread.listener.ShutdownManager;

import com.google.inject.Provider;
import com.sun.identity.shared.debug.Debug;

/**
 * This class provides Guice with instances of Client that contain a HttpClientHandler. The underlying HttpClientHandler
 * is registered with the ShutdownManager to tidy up threads when the server is stopped.
 * @since 13.5.0
 */
public class CloseableHttpClientProvider implements Provider<Client> {

    /** Debug instance for general utility classes. */
    public static final Debug DEBUG = Debug.getInstance("amUtil");

    /** The commons ShutdownManager. */
    private ShutdownManager shutdownManager;

    /**
     * Uses the shutdown manager supplied to register all created Client objects for shutdown.
     * @param shutdownManager The commons shutdown manager implementation.
     */
    @Inject
    public CloseableHttpClientProvider(ShutdownManager shutdownManager) {
        this.shutdownManager = shutdownManager;
    }

    /**
     * Creates an instance of Client, assuring that underlying commitments are observer, i.e. listening for shutdown.
     * @return An instance of Client, or null if an exception occurred during creation.
     */
    @Override
    public Client get() {
        try {
            final HttpClientHandler httpClientHandler = new HttpClientHandler();

            // Let the underlying HttpClientHandler clean up it's threads upon shutdown
            shutdownManager.addShutdownListener(new ShutdownListener() {
                @Override
                public void shutdown() {
                    try {
                        httpClientHandler.close();
                    } catch (IOException e) {
                        // Abandon attempt to close the connection
                        DEBUG.message("Unable to close the HttpClientHandler", e);
                    }
                }
            });

            return new Client(httpClientHandler);
        } catch (HttpApplicationException e) {
            // Whether this ultimately results in an error is in the hands of the caller
            DEBUG.message("Unable to create HttpClientHandler", e);
            return null;
        }
    }
}
