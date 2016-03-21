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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.http;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplication;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.handler.Handlers;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.services.context.Context;
import org.forgerock.util.Factory;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.RuntimeExceptionHandler;

import com.sun.identity.shared.debug.Debug;

/**
 * OpenAM HTTP application.
 *
 * @since 13.0.0
 */
@Singleton
final class OpenAMHttpApplication implements HttpApplication {

    private static final Debug DEBUG = Debug.getInstance("frRest");
    private final Handler handler;

    @Inject
    OpenAMHttpApplication(@Named("HttpHandler") Handler handler) {
        this.handler = handler;
    }

    @Override
    public Handler start() throws HttpApplicationException {
        return Handlers.chainOf(handler, new Filter() {
            @Override
            public Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler next) {
                return next.handle(context, request)
                        .thenOnRuntimeException(new RuntimeExceptionHandler() {
                            @Override
                            public void handleRuntimeException(RuntimeException exception) {
                                DEBUG.error("A runtime exception occurred during the CREST request handling",
                                        exception);
                            }
                        });
            }
        });
    }

    @Override
    public Factory<Buffer> getBufferFactory() {
        return null;
    }

    @Override
    public void stop() {
    }
}
