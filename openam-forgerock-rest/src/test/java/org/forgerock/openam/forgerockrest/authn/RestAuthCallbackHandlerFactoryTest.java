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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.forgerockrest.authn;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class RestAuthCallbackHandlerFactoryTest {

    private RestAuthCallbackHandlerFactory restAuthCallbackHandlerFactory;

    @BeforeClass
    public void setUp() {
        restAuthCallbackHandlerFactory = RestAuthCallbackHandlerFactory.getInstance();
    }

    @Test
    public void shouldGetNameRestAuthCallbackHandler() {

        //Given

        //When
        RestAuthCallbackHandler restAuthCallbackHandler = restAuthCallbackHandlerFactory.getRestAuthCallbackHandler(
                NameCallback.class);

        //Then
        assertEquals(restAuthCallbackHandler.getClass(), RestAuthNameCallbackHandler.class);
    }

    @Test
    public void shouldGetPasswordRestAuthCallbackHandler() {

        //Given

        //When
        RestAuthCallbackHandler restAuthCallbackHandler = restAuthCallbackHandlerFactory.getRestAuthCallbackHandler(
                PasswordCallback.class);

        //Then
        assertEquals(restAuthCallbackHandler.getClass(), RestAuthPasswordCallbackHandler.class);
    }

    @Test (expectedExceptions = RuntimeException.class)
    public void shouldThrowExceptionWithUnknownCallback() {        //TODO check for specific exception and message??

        //Given

        //When
        restAuthCallbackHandlerFactory.getRestAuthCallbackHandler(UnknownCallback.class);

        //Then
        fail();
    }

    static class UnknownCallback implements Callback {
    }
}
