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

import com.google.inject.Provider;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import java.io.IOException;
import jakarta.inject.Inject;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.util.Options;
import org.forgerock.util.thread.listener.ShutdownManager;

public class CloseableHttpClientHandlerProvider implements Provider<HttpClientHandler> {

    /** Debug instance for general utility classes. */
    public static final Debug DEBUG = Debug.getInstance("amUtil");

    /** The commons ShutdownManager. */
    private ShutdownManager shutdownManager;

    /**
     * Uses the shutdown manager supplied to register all created HttpClientHandler objects for shutdown.
     * @param shutdownManager The commons shutdown manager implementation.
     */
    @Inject
    public CloseableHttpClientHandlerProvider(ShutdownManager shutdownManager) {
        this.shutdownManager = shutdownManager;
    }

    /**
     * Creates an instance of HttpClientHandler, assuring that underlying commitments are observer, i.e. listening for shutdown.
     * @return An instance of HttpClientHandler, or an exception if an error occurred during creation.
     */
    @Override
    public HttpClientHandler get() {
        try {
            // Enable the system proxy if the corresponding server property is set
            Options options = Options.defaultOptions();
            options.set(HttpClientHandler.OPTION_PROXY_SYSTEM,
                SystemPropertiesManager.getAsBoolean(Constants.SYSTEM_PROXY_ENABLED, false));

            DEBUG.message("CloseableHttpClientHandlerProvider.get: System proxy enabled for HttpClientHandler: {}",
                options.get(HttpClientHandler.OPTION_PROXY_SYSTEM));

            HttpClientHandler httpClientHandler = new HttpClientHandler(options);

            // Let the underlying HttpClientHandler clean up it's threads upon shutdown
            this.shutdownManager.addShutdownListener(() -> {
                try {
                    httpClientHandler.close();
                } catch (IOException e) {
                    // Abandon attempt to close the connection
                    DEBUG.message("Unable to close the HttpClientHandler", e);
                }
            });

            return httpClientHandler;
        } catch (HttpApplicationException e) {
            // Whether this ultimately results in an error is in the hands of the caller
            DEBUG.error("Unable to create a new HttpClientHandler", e);
            return null;
        }
    }
}
