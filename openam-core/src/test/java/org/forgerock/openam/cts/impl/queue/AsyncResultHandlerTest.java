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
 * Copyright 2014-2015 ForgeRock AS.
 */
package org.forgerock.openam.cts.impl.queue;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.impl.queue.config.CTSQueueConfiguration;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.shared.debug.Debug;

public class AsyncResultHandlerTest {

    private String badger;
    private AsyncResultHandler<String> handler;
    private CTSQueueConfiguration mockConfig;
    private Debug mockDebug;

    @BeforeMethod
    public void setup() {
        badger = "Badger";

        mockConfig = mock(CTSQueueConfiguration.class);
        mockDebug = mock(Debug.class);

        handler = new AsyncResultHandler<String>(mockConfig, mockDebug);
    }

    @Test
    public void shouldReturnResultProvided() {
        handler.processResults(badger);
        assertThat(await(handler)).isEqualTo(badger);
    }

    @Test
    public void shouldThrowExceptionProvided() {
        handler.processError(new CoreTokenException("Test"));
        assertThat(await(handler)).isInstanceOfAny(Throwable.class);
    }

    @Test
    public void shouldWrapNonCoreTokenExceptions() {
        // Given
        Exception ex = new Exception("test");

        // When
        handler.processError(ex);
        Exception result = (Exception) await(handler);

        // Then
        assertThat(result).isInstanceOf(CoreTokenException.class);
        assertThat(result.getCause()).isSameAs(ex);
    }

    @Test
    public void shouldHandleNullObject() {
        handler.processResults(null);
        assertThat(await(handler)).isNull();
    }

    @Test (expectedExceptions = IllegalStateException.class)
    public void shouldNotAllowMultipleAdditionsToResultHandler() {
        handler.processResults("badger");
        handler.processResults("weasel");
    }

    @Test (timeOut = 1000)
    public void shouldWaitOnResultHandler() throws Exception {
        // Given
        given(mockConfig.getQueueTimeout()).willReturn(1);
        final String key = "badgers!";
        final ResultHandler<String, ?>[] ref = new ResultHandler[1];

        final Thread resultThread = new Thread(new Runnable() {
            @Override
            public void run() {
                ref[0].processResults(key);
            }
        });
        ArrayBlockingQueue<Object> mockQueue = new ArrayBlockingQueue<Object>(1) {
            @Override
            public Object poll(long l, TimeUnit timeUnit) throws InterruptedException {
                resultThread.start();
                return super.poll(l, timeUnit);
            }
        };
        ref[0] = new AsyncResultHandler<String>(mockConfig, mockQueue, mockDebug);

        // When
        String results = ref[0].getResults();

        // Then
        assertThat(results).isEqualTo(key);
    }

    @Test (timeOut = 1000)
    public void shouldTimeoutIfNoResultsPublished() {
        // Given
        given(mockConfig.getQueueTimeout()).willReturn(0);

        // When
        CoreTokenException error = null;
        try {
            handler.getResults();
        } catch (CoreTokenException e) {
            error = e;
        }

        // Then
        assertThat(error).isNotNull();
    }

    private Object await(ResultHandler<?, ?> handler) {
        try {
            return handler.getResults();
        } catch (Exception e) {
            return e;
        }
    }
}