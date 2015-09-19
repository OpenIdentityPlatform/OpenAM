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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Provider;
import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.services.context.Context;
import org.forgerock.http.Handler;
import org.forgerock.services.context.RootContext;
import org.forgerock.http.protocol.Request;
import org.mockito.Mock;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class HandlerProviderTest extends GuiceTestCase {

    private HandlerProvider handlerProvider;

    @Mock
    private Handler handler;
    private Key<Handler> key;
    private AtomicInteger handlerReturnCount;

    @BeforeClass
    public void setupClass() {
        initMocks(this);
        key = Key.get(Handler.class);
        handlerReturnCount = new AtomicInteger();
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(key).toProvider(new Provider<Handler>() {
            @Override
            public Handler get() {
                handlerReturnCount.incrementAndGet();
                return handler;
            }
        });
    }

    @BeforeMethod
    public void setup() {
        handlerProvider = new HandlerProvider(key);
        handlerReturnCount.set(0);
    }

    @Test
    public void shouldCallInjectorHolderOnFirstCallToHandler() {

        //Given
        Context context = new RootContext();
        Request request = new Request();

        //When
        handlerProvider.handle(context, request);

        //Then
        assertThat(handlerReturnCount.get()).isEqualTo(1);
        verify(handler).handle(context, request);
    }

    @Test
    public void shouldNotCallInjectorHolderTwice() {

        //Given
        Context context = new RootContext();
        Request request = new Request();

        //When
        handlerProvider.handle(context, request);
        handlerProvider.handle(context, request);

        //Then
        assertThat(handlerReturnCount.get()).isEqualTo(1);
        verify(handler, times(2)).handle(context, request);
    }
}
