/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.identity.authentication.modules.common;

import java.util.HashMap;

import org.forgerock.identity.authentication.modules.common.config.JsonHelper;
import org.forgerock.identity.authentication.modules.common.config.MapObjectTransformer;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ConfigurationReadingTest {

	private static final String MAP_READER_TEST_FILE = "/org/forgerock/identity/authentication/modules/common/object-map-transformer-map.json";
	private MapObjectTransformer cut; 
	
	@BeforeTest
	public void setUp() {
		cut = new MapObjectTransformer();
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testReadConfiguration() {
		HashMap<String, Object> readJSONFromFile = JsonHelper.readJSONFromFile(MAP_READER_TEST_FILE, HashMap.class);
		cut.setInputMap(readJSONFromFile);
		SampleConfigurationClass createObjectUsingAttributes = cut.createObjectUsingAttributes(SampleConfigurationClass.class);
		assertEquals(createObjectUsingAttributes.getSampleLongAttr(), new Long(1L));
		assertEquals(createObjectUsingAttributes.getSampleIntegerAttr(), new Integer(1));
		assertEquals(createObjectUsingAttributes.getSampleBooleanAttr(), new Boolean(true));
		assertEquals(createObjectUsingAttributes.getSampleStringAttr(), "a");
		assertNull(createObjectUsingAttributes.getSampleNullIntegerAttr());
		//System.out.println(readConfigurationToObject(SampleConfigurationClass.class));
	}

}
