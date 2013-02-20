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

package org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.marchingrunner.riskbased;

import java.io.IOException;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.RiskBasedRunnerConfig;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.attributematcher.ComparisonResult;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.attributematcher.RiskBasedDevicePrintComparator;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.DevicePrint;
import org.forgerock.identity.authentication.modules.common.config.JsonHelper;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class DevicePrintRiskBasedComparatorTest {
	
	private static final long NOTHING_MISSING_PENALTY_POINTS = 0L;
	private static final long MISSING_SCREEN_RESOLUTION_PENALTY_POINTS = 20L;
	private static final long MISSING_COOKIE_PENALTY_POINTS = 100L;
	
	private static final String FULL_DEVICE_PRINT_PATH = "/org/forgerock/identity/authentication/modules/adaptivedeviceprint/logic/marchingrunner/riskbased/FullDevicePrint1.json";
	private static final String FULL_DEVICE_PRINT_MODIFIED_COOKIE_PATH = "/org/forgerock/identity/authentication/modules/adaptivedeviceprint/logic/marchingrunner/riskbased/FullDevicePrintModifiedCookie.json";
	private static final String FULL_DEVICE_PRINT_WITHOUT_SCREEN_DIMENSION_PATH = "/org/forgerock/identity/authentication/modules/adaptivedeviceprint/logic/marchingrunner/riskbased/FullDevicePrintWOScreenDimension1.json";
	private static final String FULL_DEVICE_PRINT_WITHOUT_COOKIE_PATH = "/org/forgerock/identity/authentication/modules/adaptivedeviceprint/logic/marchingrunner/riskbased/FullDevicePrintWOCookie1.json";
	
	private static final String RISK_CONFIG_PATH = "/org/forgerock/identity/authentication/modules/adaptivedeviceprint/logic/marchingrunner/riskbased/RiskBasedConfiguration1.json";
	
	private RiskBasedDevicePrintComparator cut = new RiskBasedDevicePrintComparator();

	@Test
	public void testExactMatch() throws JsonParseException, JsonMappingException, IOException {
		DevicePrint devicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_PATH, DevicePrint.class);
		RiskBasedRunnerConfig config = JsonHelper.readJSONFromFile(RISK_CONFIG_PATH, RiskBasedRunnerConfig.class);
		
		ComparisonResult compareDevicePrintsPart = cut.compareDevicePrintsPart(devicePrint, devicePrint, config);
		assertEquals((long)compareDevicePrintsPart.getPenaltyPoints(), NOTHING_MISSING_PENALTY_POINTS);
		assertTrue(compareDevicePrintsPart.isSuccessful());
		assertFalse(compareDevicePrintsPart.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testNonExactMatch() throws JsonParseException, JsonMappingException, IOException {
		DevicePrint storedDevicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_PATH, DevicePrint.class);
		DevicePrint currentDevicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_WITHOUT_SCREEN_DIMENSION_PATH, DevicePrint.class);
		
		RiskBasedRunnerConfig config = JsonHelper.readJSONFromFile(RISK_CONFIG_PATH, RiskBasedRunnerConfig.class);
		
		ComparisonResult compareDevicePrintsPart = cut.compareDevicePrintsPart(currentDevicePrint, storedDevicePrint, config);
		assertEquals((long)compareDevicePrintsPart.getPenaltyPoints(), MISSING_SCREEN_RESOLUTION_PENALTY_POINTS);
		assertFalse(compareDevicePrintsPart.isSuccessful());
		assertFalse(compareDevicePrintsPart.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testMissingCookie() throws JsonParseException, JsonMappingException, IOException {
		DevicePrint storedDevicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_PATH, DevicePrint.class);
		DevicePrint currentDevicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_WITHOUT_COOKIE_PATH, DevicePrint.class);
		
		RiskBasedRunnerConfig config = JsonHelper.readJSONFromFile(RISK_CONFIG_PATH, RiskBasedRunnerConfig.class);
		
		ComparisonResult compareDevicePrintsPart = cut.compareDevicePrintsPart(currentDevicePrint, storedDevicePrint, config);
		assertEquals((long)compareDevicePrintsPart.getPenaltyPoints(), MISSING_COOKIE_PENALTY_POINTS);
		assertFalse(compareDevicePrintsPart.isSuccessful());
		assertFalse(compareDevicePrintsPart.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testModifiedCookie() throws JsonParseException, JsonMappingException, IOException {
		DevicePrint storedDevicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_PATH, DevicePrint.class);
		DevicePrint currentDevicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_MODIFIED_COOKIE_PATH, DevicePrint.class);
		
		RiskBasedRunnerConfig config = JsonHelper.readJSONFromFile(RISK_CONFIG_PATH, RiskBasedRunnerConfig.class);
		
		ComparisonResult compareDevicePrintsPart = cut.compareDevicePrintsPart(currentDevicePrint, storedDevicePrint, config);
		assertEquals((long)compareDevicePrintsPart.getPenaltyPoints(), MISSING_COOKIE_PENALTY_POINTS);
		assertFalse(compareDevicePrintsPart.isSuccessful());
		assertFalse(compareDevicePrintsPart.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testSuccessfulWithAdditionalInfo() throws JsonParseException, JsonMappingException, IOException {
		DevicePrint currentDevicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_PATH, DevicePrint.class);
		DevicePrint storedDevicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_WITHOUT_COOKIE_PATH, DevicePrint.class);
		
		RiskBasedRunnerConfig config = JsonHelper.readJSONFromFile(RISK_CONFIG_PATH, RiskBasedRunnerConfig.class);
		
		ComparisonResult compareDevicePrintsPart = cut.compareDevicePrintsPart(currentDevicePrint, storedDevicePrint, config);
		assertEquals((long)compareDevicePrintsPart.getPenaltyPoints(), NOTHING_MISSING_PENALTY_POINTS);
		assertTrue(compareDevicePrintsPart.isSuccessful());
		assertTrue(compareDevicePrintsPart.isAdditionalInfoInCurrentValue());
	}
	
	@Test
	public void testNonSuccessfulWithAdditionalInfo() throws JsonParseException, JsonMappingException, IOException {
		DevicePrint currentDevicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_WITHOUT_SCREEN_DIMENSION_PATH, DevicePrint.class);
		DevicePrint storedDevicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_WITHOUT_COOKIE_PATH, DevicePrint.class);
		
		RiskBasedRunnerConfig config = JsonHelper.readJSONFromFile(RISK_CONFIG_PATH, RiskBasedRunnerConfig.class);
		
		ComparisonResult compareDevicePrintsPart = cut.compareDevicePrintsPart(currentDevicePrint, storedDevicePrint, config);
		assertEquals((long)compareDevicePrintsPart.getPenaltyPoints(), MISSING_SCREEN_RESOLUTION_PENALTY_POINTS);
		assertFalse(compareDevicePrintsPart.isSuccessful());
		assertTrue(compareDevicePrintsPart.isAdditionalInfoInCurrentValue());
	}
	
	public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
		DevicePrintRiskBasedComparatorTest test = new DevicePrintRiskBasedComparatorTest();
		test.testSuccessfulWithAdditionalInfo();
		test.testNonSuccessfulWithAdditionalInfo();
		test.testNonExactMatch();
		test.testModifiedCookie();
		test.testMissingCookie();
		test.testExactMatch();
	}
	
}
