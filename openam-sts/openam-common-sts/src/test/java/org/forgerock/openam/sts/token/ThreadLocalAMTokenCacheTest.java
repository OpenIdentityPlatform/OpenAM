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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.token;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCacheImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.concurrent.Callable;

import static org.testng.Assert.assertTrue;


public class ThreadLocalAMTokenCacheTest {
    private ThreadLocalAMTokenCache tokenCache;
    static class MyModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(ThreadLocalAMTokenCache.class).to(ThreadLocalAMTokenCacheImpl.class).in(Scopes.SINGLETON);
        }

        @Provides
        Logger getSlf4jLogger() {
            return LoggerFactory.getLogger(AMSTSConstants.REST_STS_DEBUG_ID);
        }

    }

    class MyRunnable implements Runnable {
        final String value;

        MyRunnable(String value) {
            this.value = value;
        }

        public void run() {
            tokenCache.cacheAMToken(value);
            try {
                assertTrue(tokenCache.getAMToken().equals(value));
            } catch (TokenCreationException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }


    @BeforeTest
    public void initialize() {
        tokenCache = Guice.createInjector(new MyModule()).getInstance(ThreadLocalAMTokenCache.class);
    }

    @Test
    public void testLookup() {
        Thread t1 = new Thread(new MyRunnable("value1"));
        Thread t2 = new Thread(new MyRunnable("value2"));
        Thread t3 = new Thread(new MyRunnable("value3"));

        try {
            t1.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            t3.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
