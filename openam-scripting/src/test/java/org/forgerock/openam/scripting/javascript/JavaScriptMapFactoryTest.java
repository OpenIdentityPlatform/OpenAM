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
* Copyright 2014 ForgeRock AS.
*/
package org.forgerock.openam.scripting.javascript;

import java.util.Map;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;

public class JavaScriptMapFactoryTest {

    NativeObject myNativeObject = null;

    private void loadNativeObjectFromJavaScript(final String myJavaScriptObject) {
        new ContextFactory().call(new ContextAction(){

            @Override
            public Object run(Context ctx) {
                Scriptable scope = ctx.initStandardObjects();
                try {

                    myNativeObject = (NativeObject) ctx.evaluateString(
                            scope, myJavaScriptObject,
                            "<inline>", 1, null);

                } catch (Exception e) {
                    return null;
                }

                return null;
            }
        });
    }

    @Test
    public void testValidObjectConvertsSuccessfully() {
        //given
        final String myJavaScriptObject = "({number:1, word:'up'})";
        loadNativeObjectFromJavaScript(myJavaScriptObject);

        myNativeObject.put("rock", myNativeObject, "on");
        myNativeObject.put(0, myNativeObject, "first");
        myNativeObject.put(0, myNativeObject, "second"); //should overwrite "first"

        //when
        Map<String, Object> mapRepresentation = JavaScriptMapFactory.javaScriptObjectToMap(myNativeObject);

        //then
        assertTrue(mapRepresentation.get("word").equals("up"));
        assertTrue(mapRepresentation.get("rock").equals("on"));
        assertTrue(mapRepresentation.get("0").equals("second"));
        assertTrue((Double) mapRepresentation.get("number") == 1);
    }

    @Test
    public void testNullNativeObjectIsNull() {
        //given

        //when
        Map<String, Object> mapRepresentation = JavaScriptMapFactory.javaScriptObjectToMap(null);

        //then
        assertNull(mapRepresentation);
    }

    @Test
    public void testEmptyObjectIsEmptyMap() {
        //given
        final String myJavaScriptObject = "({})";
        loadNativeObjectFromJavaScript(myJavaScriptObject);

        //when
        Map<String, Object> mapRepresentation = JavaScriptMapFactory.javaScriptObjectToMap(myNativeObject);

        //then
        assertTrue(mapRepresentation.isEmpty());
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void testInvalidObjectKeyThrowsIllegalArgument() {
        //given
        NativeObject mockNativeObject = mock(NativeObject.class);
        Object[] listOfItems = new Object[1];
        listOfItems[0] = new Object();
        when(mockNativeObject.getIds()).thenReturn(listOfItems);

        //when
        Map<String, Object> mapRepresentation = JavaScriptMapFactory.javaScriptObjectToMap(mockNativeObject);


        //then - caught by exception
    }

}
