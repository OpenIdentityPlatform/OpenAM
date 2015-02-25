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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openam.forgerockrest.authn.callbackhandlers;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthResponseException;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import static org.forgerock.json.fluent.JsonValue.array;
import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.security.auth.callback.ChoiceCallback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class RestAuthChoiceCallbackHandlerTest {

    private RestAuthCallbackHandler<ChoiceCallback> restAuthChoiceCallbackHandler;

    @BeforeClass
    public void setUp() {
        restAuthChoiceCallbackHandler = new RestAuthChoiceCallbackHandler();
    }

    @Test
    public void shouldGetCallbackClassName() {

        //Given

        //When
        String callbackClassName = restAuthChoiceCallbackHandler.getCallbackClassName();

        //Then
        assertEquals(ChoiceCallback.class.getSimpleName(), callbackClassName);
    }

    @Test
    public void shouldNotUpdateCallbackFromRequest() throws RestAuthResponseException, RestAuthException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        ChoiceCallback choiceCallback = mock(ChoiceCallback.class);

        //When
        boolean updated = restAuthChoiceCallbackHandler.updateCallbackFromRequest(request, response, choiceCallback);

        //Then
        assertFalse(updated);
    }

    @Test
    public void shouldHandleCallback() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        JsonValue jsonPostBody = mock(JsonValue.class);
        ChoiceCallback originalChoiceCallback = mock(ChoiceCallback.class);

        //When
        ChoiceCallback choiceCallback = restAuthChoiceCallbackHandler.handle(request, response, jsonPostBody,
                originalChoiceCallback);

        //Then
        assertEquals(originalChoiceCallback, choiceCallback);
    }

    @Test
    public void shouldConvertToJson() throws RestAuthException {

        //Given
        ChoiceCallback choiceCallback = new ChoiceCallback("Select choice:", new String[]{"1", "34", "66", "93"}, 0,
                false);

        //When
        JsonValue jsonObject = restAuthChoiceCallbackHandler.convertToJson(choiceCallback, 1);

        //Then
        assertEquals("ChoiceCallback", jsonObject.get("type").asString());

        assertNotNull(jsonObject.get("output"));
        assertEquals(3, jsonObject.get("output").size());
        assertEquals("Select choice:", jsonObject.get("output").get(0).get("value").asString());
        assertEquals("1", jsonObject.get("output").get(1).get("value").get(0).asString());
        assertEquals("34", jsonObject.get("output").get(1).get("value").get(1).asString());
        assertEquals("66", jsonObject.get("output").get(1).get("value").get(2).asString());
        assertEquals("93", jsonObject.get("output").get(1).get("value").get(3).asString());
        assertEquals(0, (int) jsonObject.get("output").get(2).get("value").asInteger());

        assertNotNull(jsonObject.get("input"));
        assertEquals(1, jsonObject.get("input").size());
        assertEquals(0, (int) jsonObject.get("input").get(0).get("value").asInteger());
    }

    @Test
    public void shouldConvertToJsonWithPreSelectedIndexes() throws RestAuthException {

        //Given
        ChoiceCallback choiceCallback = new ChoiceCallback("Select choice:", new String[]{"1", "34", "66", "93"}, 0,
                false);
        choiceCallback.setSelectedIndex(1);

        //When
        JsonValue jsonObject = restAuthChoiceCallbackHandler.convertToJson(choiceCallback, 1);

        //Then
        assertEquals("ChoiceCallback", jsonObject.get("type").asString());

        assertNotNull(jsonObject.get("output"));
        assertEquals(3, jsonObject.get("output").size());
        assertEquals("Select choice:", jsonObject.get("output").get(0).get("value").asString());
        assertEquals("1", jsonObject.get("output").get(1).get("value").get(0).asString());
        assertEquals("34", jsonObject.get("output").get(1).get("value").get(1).asString());
        assertEquals("66", jsonObject.get("output").get(1).get("value").get(2).asString());
        assertEquals("93", jsonObject.get("output").get(1).get("value").get(3).asString());
        assertEquals(0, (int) jsonObject.get("output").get(2).get("value").asInteger());

        assertNotNull(jsonObject.get("input"));
        assertEquals(1, jsonObject.get("input").size());
        assertEquals(1, (int) jsonObject.get("input").get(0).get("value").asInteger());
    }

    @Test
    public void shouldConvertFromJson() throws RestAuthException {

        //Given
        ChoiceCallback choiceCallback = new ChoiceCallback("Select choice:", new String[]{"1", "34", "66", "93"}, 0,
                true);
        JsonValue jsonNameCallback = json(object(
                field("input",
                        array(
                                object(field("value", 1))
                        )
                ),
                field("output",
                        array(
                                object(field("value", "Select choice:")),
                                object(field("value", array("1", "34", "66", "93"))),
                                object(field("value", "0"))
                        )
                ),
                field("type", "ChoiceCallback")
        ));

        //When
        ChoiceCallback convertedChoiceCallback = restAuthChoiceCallbackHandler.convertFromJson(choiceCallback,
                jsonNameCallback);

        //Then
        assertEquals(choiceCallback, convertedChoiceCallback);
        assertEquals("Select choice:", convertedChoiceCallback.getPrompt());
        assertEquals(new String[]{"1", "34", "66", "93"}, convertedChoiceCallback.getChoices());
        assertEquals(0, convertedChoiceCallback.getDefaultChoice());
        assertEquals(new int[]{1}, convertedChoiceCallback.getSelectedIndexes());
    }

    @Test (expectedExceptions = RestAuthException.class)
    public void shouldFailToConvertFromJsonWithInvalidType() throws RestAuthException {

        //Given
        ChoiceCallback choiceCallback = new ChoiceCallback("Select choice:", new String[]{"1", "34", "66", "93"}, 0,
                true);
        JsonValue jsonNameCallback = json(object(
                field("input",
                        array(
                                object(field("value", 1))
                        )
                ),
                field("output",
                        array(
                                object(field("value", "Select choice:")),
                                object(field("value", array("1", "34", "66", "93"))),
                                object(field("value", "0"))
                        )
                ),
                field("type", "PasswordCallback")
        ));

        System.out.println("shouldFailToConvertFromJsonWithInvalidType");
        System.out.println(jsonNameCallback.toString());

        //When
        restAuthChoiceCallbackHandler.convertFromJson(choiceCallback, jsonNameCallback);

        //Then
        fail();
    }

    @Test
    public void shouldNotFailToConvertFromJsonWithTypeLowerCase() throws RestAuthException {

        //Given
        ChoiceCallback choiceCallback = new ChoiceCallback("Select choice:", new String[]{"1", "34", "66", "93"}, 0,
                true);
        JsonValue jsonNameCallback = json(object(
                field("input",
                        array(
                                object(field("value", 1))
                        )
                ),
                field("output",
                        array(
                                object(field("value", "Select choice:")),
                                object(field("value", array("1", "34", "66", "93"))),
                                object(field("value", "0"))
                        )
                ),
                field("type", "choicecallback")
        ));

        System.out.println("shouldNotFailToConvertFromJsonWithTypeLowerCase");
        System.out.println(jsonNameCallback.toString());

        //When
        ChoiceCallback convertedChoiceCallback = restAuthChoiceCallbackHandler.convertFromJson(choiceCallback,
                jsonNameCallback);

        //Then
        assertEquals(choiceCallback, convertedChoiceCallback);
        assertEquals("Select choice:", convertedChoiceCallback.getPrompt());
        assertEquals(new String[]{"1", "34", "66", "93"}, convertedChoiceCallback.getChoices());
        assertEquals(0, convertedChoiceCallback.getDefaultChoice());
        assertEquals(new int[]{1}, convertedChoiceCallback.getSelectedIndexes());
    }

    @Test (expectedExceptions = JsonValueException.class)
    public void shouldFailToConvertFromJsonWithInvalidValue() throws RestAuthException {

        //Given
        ChoiceCallback choiceCallback = new ChoiceCallback("Select choice:", new String[]{"A", "B", "C", "D"}, 0,
                true);
        JsonValue jsonNameCallback = json(object(
                field("input",
                        array(
                                object(field("value", "A"))
                        )
                ),
                field("output",
                        array(
                                object(field("value", "Select choice:")),
                                object(field("value", array("A", "B", "C", "D"))),
                                object(field("value", "0"))
                        )
                ),
                field("type", "ChoiceCallback")
        ));

        System.out.println("shouldFailToConvertFromJsonWithInvalidValue");
        System.out.println(jsonNameCallback.toString());

        //When
        restAuthChoiceCallbackHandler.convertFromJson(choiceCallback, jsonNameCallback);

        //Then
        fail();
    }

    @Test
    public void shouldNotFailToConvertFromJsonWithIntValueAsString() throws RestAuthException {

        //Given
        ChoiceCallback choiceCallback = new ChoiceCallback("Select choice:", new String[]{"1", "34", "66", "93"}, 0,
                true);
        JsonValue jsonNameCallback = json(object(
                field("input",
                        array(
                                object(field("value", "1"))
                        )
                ),
                field("output",
                        array(
                                object(field("value", "Select choice:")),
                                object(field("value", array("1", "34", "66", "93"))),
                                object(field("value", "0"))
                        )
                ),
                field("type", "choicecallback")
        ));

        //When
        ChoiceCallback convertedChoiceCallback = restAuthChoiceCallbackHandler.convertFromJson(choiceCallback,
                jsonNameCallback);

        //Then
        assertEquals(choiceCallback, convertedChoiceCallback);
        assertEquals("Select choice:", convertedChoiceCallback.getPrompt());
        assertEquals(new String[]{"1", "34", "66", "93"}, convertedChoiceCallback.getChoices());
        assertEquals(0, convertedChoiceCallback.getDefaultChoice());
        assertEquals(new int[]{1}, convertedChoiceCallback.getSelectedIndexes());
    }

}
