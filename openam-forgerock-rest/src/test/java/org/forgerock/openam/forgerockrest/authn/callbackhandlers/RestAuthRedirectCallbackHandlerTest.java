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

package org.forgerock.openam.forgerockrest.authn.callbackhandlers;

import com.sun.identity.authentication.share.RedirectCallbackHandler;
import com.sun.identity.authentication.spi.RedirectCallback;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

import static junit.framework.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

public class RestAuthRedirectCallbackHandlerTest {

    private RestAuthCallbackHandler<RedirectCallback> restAuthRedirectCallbackHandler;

    private RedirectCallbackHandler redirectCallbackHandler;

    @BeforeClass
    public void setUp() {

        redirectCallbackHandler = mock(RedirectCallbackHandler.class);

        restAuthRedirectCallbackHandler = new RestAuthRedirectCallbackHandler(redirectCallbackHandler);
    }

    @Test
    public void shouldGetCallbackClassName() {

        //Given

        //When
        String callbackClassName = restAuthRedirectCallbackHandler.getCallbackClassName();

        //Then
        assertEquals(RedirectCallback.class.getSimpleName(), callbackClassName);
    }

    @Test
    public void shouldHandleCallback() throws JSONException {

        //Given
        HttpHeaders headers = mock(HttpHeaders.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JSONObject jsonPostBody = mock(JSONObject.class);
        RedirectCallback originalRedirectCallback = mock(RedirectCallback.class);

        //When
        RedirectCallback redirectCallback = restAuthRedirectCallbackHandler.handle(headers, request, response,
                jsonPostBody, originalRedirectCallback);

        //Then
        assertEquals(originalRedirectCallback, redirectCallback);
    }

    @Test (expectedExceptions = RestAuthException.class)
    public void shouldFailConvertToJson() throws JSONException {

        //Given

        //When
        restAuthRedirectCallbackHandler.convertToJson(null, 1);

        //Then
        fail();
    }

    @Test (expectedExceptions = RestAuthException.class)
    public void shouldFailToConvertFromJson() throws JSONException {

        //Given

        //When
        restAuthRedirectCallbackHandler.convertFromJson(null, null);

        //Then
        fail();
    }
}
