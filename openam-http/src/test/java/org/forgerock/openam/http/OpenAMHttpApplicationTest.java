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
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplication;
import org.forgerock.http.HttpApplicationException;
import org.mockito.Mock;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class OpenAMHttpApplicationTest extends GuiceTestCase {

    private HttpApplication httpApplication;

    @Mock
    private Handler handler;

    @BeforeClass
    public void setupClass() {
        initMocks(this);
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(Key.get(Handler.class, Names.named("HttpHandler"))).toInstance(handler);
    }

    @BeforeMethod
    public void setup() {
        httpApplication = InjectorHolder.getInstance(OpenAMHttpApplication.class);
    }

    @Test
    public void startShouldReturnHttpHandler() throws HttpApplicationException {
        //When/Then
        assertThat(httpApplication.start()).isEqualTo(handler);
    }

    @Test
    public void bufferFactoryShouldBeNull() {
        //When/Then
        assertThat(httpApplication.getBufferFactory()).isNull();
    }

    @Test
    public void callingStopShouldComplete() {
        //When
        httpApplication.stop();

        //Then
        // Just testing the method completes
    }
}
