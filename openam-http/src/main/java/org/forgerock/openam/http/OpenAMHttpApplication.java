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
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.http;

import static com.sun.identity.shared.Constants.*;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.forgerock.http.ApiProducer;
import org.forgerock.http.DescribedHttpApplication;
import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.handler.Handlers;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.swagger.OpenApiRequestFilter;
import org.forgerock.http.swagger.SwaggerApiProducer;
import org.forgerock.services.context.Context;
import org.forgerock.util.Factory;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.RuntimeExceptionHandler;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.debug.Debug;

import io.swagger.models.Info;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;

/**
 * OpenAM HTTP application.
 *
 * @since 13.0.0
 */
@Singleton
final class OpenAMHttpApplication implements DescribedHttpApplication {

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
        }, new ApiDescriptorFilter(), new OpenApiRequestFilter());
    }

    @Override
    public Factory<Buffer> getBufferFactory() {
        return null;
    }

    @Override
    public void stop() {
    }

    @Override
    public ApiProducer<Swagger> getApiProducer() {
        String basePath = SystemProperties.get(AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
        String host = SystemProperties.get(AM_SERVER_HOST) + ":" + SystemProperties.get(AM_SERVER_PORT);
        Scheme scheme = Scheme.forValue(SystemProperties.get(AM_SERVER_PROTOCOL));
        return new SwaggerApiProducer(new Info().title("OpenAM"), basePath, host, scheme);
    }
}
