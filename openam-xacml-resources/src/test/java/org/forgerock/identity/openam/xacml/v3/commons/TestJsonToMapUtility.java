/**
 *
 ~ DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 ~
 ~ Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
 ~
 ~ The contents of this file are subject to the terms
 ~ of the Common Development and Distribution License
 ~ (the License). You may not use this file except in
 ~ compliance with the License.
 ~
 ~ You can obtain a copy of the License at
 ~ http://forgerock.org/license/CDDLv1.0.html
 ~ See the License for the specific language governing
 ~ permission and limitations under the License.
 ~
 ~ When distributing Covered Code, include this CDDL
 ~ Header Notice in each file and include the License file
 ~ at http://forgerock.org/license/CDDLv1.0.html
 ~ If applicable, add the following below the CDDL Header,
 ~ with the fields enclosed by brackets [] replaced by
 ~ your own identifying information:
 ~ "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.identity.openam.xacml.v3.commons;


import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Map;


/**
 * XACML Content Handler Test Suite
 *
 * @author Jeff.Schenk@ForgeRock.com
 */
public class TestJsonToMapUtility {

    private final static String testJSONContent_ResourceName = "test_data/request-curtiss.json";

    private static String testData;

    @BeforeClass
    public void before() throws Exception {
        testData = XACML3Utils.getResourceContents(testJSONContent_ResourceName);
        assertNotNull(testData);
    }

    @AfterClass
    public void after() throws Exception {
    }

    @Test
    public void testFromFile() throws IOException {

        Map<String,Object> aMap = JsonToMapUtility.fromString(testData);
        assertNotNull(aMap);
        assertTrue(aMap.containsKey("Request"));

        assertFalse(aMap.containsKey("Action"));
        assertFalse(aMap.containsKey("Environment"));
        assertFalse(aMap.containsKey("Resource"));
        assertFalse(aMap.containsKey("Subject"));

        Object object = aMap.get("Request");
        Map innerMap = (Map) object; // Cast our Object.
        assertTrue( (object instanceof java.util.Map));
        assertTrue(innerMap.containsKey("Environment"));
        assertTrue(innerMap.containsKey("Resource"));
        assertTrue(innerMap.containsKey("Subject"));

    }

}
