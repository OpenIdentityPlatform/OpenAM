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
 * Portions Copyrighted 2026 3A Systems, LLC.
 */

package org.forgerock.openam.sts.token;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.name.Names;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.HttpURLConnectionWrapper;
import org.forgerock.openam.sts.TokenValidationException;
import org.slf4j.Logger;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

public class AMTokenParserTest {
    private static final String BODY_WITH_TOKEN = "{\"tokenId\":\"da_token_id\",\"successUrl\":\"/openam/console\"}";
    private static final String HTTP_ONLY_BODY = "{\"successUrl\":\"/openam/console\",\"realm\":\"/\"}";

    AMTokenParser tokenParser;

    static class MyModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(AMTokenParser.class).to(AMTokenParserImpl.class);
            bind(Logger.class).toInstance(mock(Logger.class));
            bindConstant().annotatedWith(Names.named(AMSTSConstants.AM_SESSION_COOKIE_NAME)).to("iPlanetDirectoryPro");
        }
    }

    @BeforeTest
    public void initialize() {
        tokenParser = Guice.createInjector(new MyModule()).getInstance(AMTokenParser.class);
    }

    private static HttpURLConnectionWrapper.ConnectionResult response(String body, String... setCookies) {
        return new HttpURLConnectionWrapper.ConnectionResult(200, body, Arrays.asList(setCookies));
    }

    @Test
    public void testParse() throws TokenValidationException {
        assertEquals(tokenParser.getSessionFromAuthNResponse(
                new HttpURLConnectionWrapper.ConnectionResult(200, BODY_WITH_TOKEN, Collections.<String>emptyList())),
                "da_token_id");
    }

    @Test
    public void takesSessionFromCookieWhenBodyHasNoTokenId() throws TokenValidationException {
        assertEquals(tokenParser.getSessionFromAuthNResponse(response(HTTP_ONLY_BODY,
                "iPlanetDirectoryPro=AQIC5wM2LY4Sfczn-cookie-token; Path=/; HttpOnly")),
                "AQIC5wM2LY4Sfczn-cookie-token");
    }

    @Test
    public void prefersTokenIdInBody() throws TokenValidationException {
        assertEquals(tokenParser.getSessionFromAuthNResponse(response(BODY_WITH_TOKEN,
                "iPlanetDirectoryPro=AQIC5wM2LY4Sfczn-cookie-token; Path=/")),
                "da_token_id");
    }

    @Test
    public void lastNonEmptySessionCookieWins() throws TokenValidationException {
        assertEquals(tokenParser.getSessionFromAuthNResponse(response(HTTP_ONLY_BODY,
                "iPlanetDirectoryPro=AQIC5wM2LY4Sfczn-old-token; Path=/",
                "iPlanetDirectoryProExtra=other; Path=/",
                "iPlanetDirectoryPro=AQIC5wM2LY4Sfczn-new-token; Path=/; HttpOnly",
                "iPlanetDirectoryPro=; Expires=Thu, 01-Jan-1970 00:00:10 GMT; Path=/")),
                "AQIC5wM2LY4Sfczn-new-token");
    }

    @Test
    public void decodesUrlEncodedSessionCookie() throws TokenValidationException {
        assertEquals(tokenParser.getSessionFromAuthNResponse(response(HTTP_ONLY_BODY,
                "iPlanetDirectoryPro=AQIC5wM2LY4Sfczn%3D%40AAJTSQACMDE%23; Path=/; HttpOnly")),
                "AQIC5wM2LY4Sfczn=@AAJTSQACMDE#");
    }

    @Test(expectedExceptions = TokenValidationException.class)
    public void failsWhenNeitherBodyNorCookieCarriesTheSession() throws TokenValidationException {
        tokenParser.getSessionFromAuthNResponse(response(HTTP_ONLY_BODY,
                "iPlanetDirectoryPro=; Path=/", "amlbcookie=01; Path=/"));
    }
}
