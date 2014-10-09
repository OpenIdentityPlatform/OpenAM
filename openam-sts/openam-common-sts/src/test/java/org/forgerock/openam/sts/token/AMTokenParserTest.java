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
import org.forgerock.openam.sts.TokenValidationException;
import org.slf4j.Logger;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockito.Mockito.mock;

public class AMTokenParserTest {
    AMTokenParser tokenParser;
    String authNResponse = "{\"tokenId\":\"da_token_id\",\"successUrl\":\"/openam/console\"}";
    static class MyModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(AMTokenParser.class).to(AMTokenParserImpl.class);
            bind(Logger.class).toInstance(mock(Logger.class));
        }
    }

    @BeforeTest
    public void initialize() {
        tokenParser = Guice.createInjector(new MyModule()).getInstance(AMTokenParser.class);
    }

    @Test
    void testParse() throws TokenValidationException, IOException {
        tokenParser.getSessionFromAuthNResponse(authNResponse).equals("da_token_id");
    }
}
