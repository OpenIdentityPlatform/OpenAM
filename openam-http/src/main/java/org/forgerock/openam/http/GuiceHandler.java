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

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.ApiProducer;
import org.forgerock.http.Handler;
import org.forgerock.http.handler.DescribableHandler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.services.context.Context;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

import com.google.inject.Key;

import io.swagger.models.Swagger;

/**
 * A {@link Handler} implementation which delegates to Guice to get the actual
 * {@code Handler} instance that will handle the request.
 *
 * @since 13.0.0
 */
final class GuiceHandler implements DescribableHandler {

    private final Key<? extends Handler> key;
    private volatile Handler handler;
    private volatile Describable<Swagger, Request> describable;
    private volatile boolean isDescribable;

    GuiceHandler(Key<? extends Handler> key) {
        Reject.ifNull(key);
        this.key = key;
        this.isDescribable = Describable.class.isAssignableFrom(key.getTypeLiteral().getRawType());
    }

    @Override
    public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
        if (handler == null) {
            handler = InjectorHolder.getInstance(key);
        }
        return handler.handle(context, request);
    }

    @Override
    public Swagger api(ApiProducer<Swagger> producer) {
        getDescribable();
        return describable != null ? describable.api(producer) : null;
    }

    @Override
    public Swagger handleApiRequest(Context context, Request request) {
        getDescribable();
        return describable != null ? describable.handleApiRequest(context, request) : null;
    }

    @Override
    public void addDescriptorListener(Listener listener) {
        getDescribable();
        if (describable != null) {
            describable.addDescriptorListener(listener);
        }
    }

    @Override
    public void removeDescriptorListener(Listener listener) {
        getDescribable();
        if (describable != null) {
            describable.removeDescriptorListener(listener);
        }
    }

    private void getDescribable() {
        if (describable == null && isDescribable) {
            describable = (Describable<Swagger, Request>) InjectorHolder.getInstance(key);
        }
    }
}
