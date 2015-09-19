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
package org.forgerock.openam.scripting.rest;

import static org.forgerock.json.JsonValue.*;
import static org.testng.AssertJUnit.*;

import org.forgerock.services.context.ClientContext;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.services.context.Context;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openam.forgerockrest.utils.ServerContextUtils;
import org.forgerock.openam.scripting.ScriptConstants.ScriptErrorCode;
import org.forgerock.openam.scripting.ScriptException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;

public class ScriptExceptionMappingHandlerTest {

    private ScriptExceptionMappingHandler mappingHandler;

    @BeforeMethod
    public void setUp() throws ResourceException {
        mappingHandler = new ScriptExceptionMappingHandler();
    }

    @Test
    public void shouldHandleAllScriptExceptionCodes() throws Exception {
        // given

        for (ScriptErrorCode errorCode : ScriptErrorCode.values()) {
            // when
            ResourceException re = mappingHandler.handleError(new ScriptException(errorCode));

            // then
            assertNotNull(re);
            assertNotNull(re.getMessage());
            assertTrue(isClientError(re.getCode()) || re.isServerError());
        }
    }

    @Test
    public void shouldTranslateMessageToAcceptLanguage() throws Exception {
        // given
        final Context serverContext = getHttpServerContext("te");

        for (ScriptErrorCode errorCode : ScriptErrorCode.values()) {

            // when
            ResourceException re = mappingHandler.handleError(serverContext, null, new ScriptException(errorCode));

            // then
            assertNotNull(re);
//            assertEquals("Test message", re.getMessage()); //TODO dont get this...
        }
    }

    private boolean isClientError(int code) {
        return code >= 400 && code < 500;
    }

    private Context getHttpServerContext(String ...language) throws Exception {
        final HttpContext httpContext = new HttpContext(json(object(
                field("headers",
                        Collections.singletonMap(ServerContextUtils.ACCEPT_LANGUAGE, Arrays.asList(language))),
                field("parameters", Collections.emptyMap()))), null);
        return ClientContext.newInternalClientContext(httpContext);
    }

}
