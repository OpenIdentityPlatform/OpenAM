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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.RiskBasedRunnerConfig;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.attributematcher.ComparisonResult;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.logic.matchingrunner.riskbased.attributematcher.RiskBasedDevicePrintComparator;
import org.forgerock.identity.authentication.modules.adaptivedeviceprint.model.DevicePrint;
import org.forgerock.identity.authentication.modules.common.config.JsonHelper;
import org.testng.annotations.Test;

public class DevicePrintRiskBasedInstalledPluginsComparatorTest {
	
	private static final long EXACT_MATCH = 0L;
	private static final long PLUGINS_NOT_SAME_PENALTY_POINTS = 10L;
	
	private static final String FULL_DEVICE_PRINT_WITH_PLUGINS_PATH = "/org/forgerock/identity/authentication/modules/adaptivedeviceprint/logic/marchingrunner/riskbased/installedplugins/FullDevicePrintWithPlugins.json";
	private static final String FULL_DEVICE_PRINT_WITH_NO_PLUGINS_PATH = "/org/forgerock/identity/authentication/modules/adaptivedeviceprint/logic/marchingrunner/riskbased/installedplugins/FullDevicePrintWithNoPlugins.json";
	private static final String FULL_DEVICE_PRINT_WITH_3_DIFFERENT_PLUGINS_PATH = "/org/forgerock/identity/authentication/modules/adaptivedeviceprint/logic/marchingrunner/riskbased/installedplugins/FullDevicePrintWith3DifferentPlugins.json";
	private static final String FULL_DEVICE_PRINT_WITH_10_DIFFERENT_PLUGINS_PATH = "/org/forgerock/identity/authentication/modules/adaptivedeviceprint/logic/marchingrunner/riskbased/installedplugins/FullDevicePrintWith10DifferentPlugins.json";
	private static final String FULL_DEVICE_PRINT_WITH_ONE_PLUGIN_PATH = "/org/forgerock/identity/authentication/modules/adaptivedeviceprint/logic/marchingrunner/riskbased/installedplugins/FullDevicePrintWith1Plugin.json";

	private static final String RISK_CONFIG_PATH = "/org/forgerock/identity/authentication/modules/adaptivedeviceprint/logic/marchingrunner/riskbased/installedplugins/RiskBasedConfiguration1.json";
	private static final String IGNORE_PLUGINS_DIFFERENCES_MAX_TOLERATE_IS_2_OR_30PERCENT_CONFIG_PATH = "/org/forgerock/identity/authentication/modules/adaptivedeviceprint/logic/marchingrunner/riskbased/installedplugins/RiskBasedConfiguration2.json";
	
	private RiskBasedDevicePrintComparator cut = new RiskBasedDevicePrintComparator();

	@Test
	public void testDifferentPluginsWithoutIgnore() throws JsonParseException, JsonMappingException, IOException {
		DevicePrint currentDevicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_WITH_PLUGINS_PATH, DevicePrint.class);
		DevicePrint storedDevicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_WITH_NO_PLUGINS_PATH, DevicePrint.class);
		
		RiskBasedRunnerConfig config = JsonHelper.readJSONFromFile(RISK_CONFIG_PATH, RiskBasedRunnerConfig.class);
		
		ComparisonResult compareDevicePrintsPart = cut.compareDevicePrintsPart(currentDevicePrint, storedDevicePrint, config);
		assertEquals((long)compareDevicePrintsPart.getPenaltyPoints(), PLUGINS_NOT_SAME_PENALTY_POINTS);
		assertFalse(compareDevicePrintsPart.isSuccessful());
	}
	
	@Test
	public void testNoDifferentsInPluginsWithoutIgnore() throws JsonParseException, JsonMappingException, IOException {
		DevicePrint currentDevicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_WITH_PLUGINS_PATH, DevicePrint.class);
		DevicePrint storedDevicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_WITH_PLUGINS_PATH, DevicePrint.class);
		
		RiskBasedRunnerConfig config = JsonHelper.readJSONFromFile(RISK_CONFIG_PATH, RiskBasedRunnerConfig.class);
		
		ComparisonResult compareDevicePrintsPart = cut.compareDevicePrintsPart(currentDevicePrint, storedDevicePrint, config);
		assertEquals((long)compareDevicePrintsPart.getPenaltyPoints(), EXACT_MATCH);
		assertTrue(compareDevicePrintsPart.isSuccessful());
	}
	
	@Test
	public void testBothNoPluginsWithoutIgnore() throws JsonParseException, JsonMappingException, IOException {
		DevicePrint currentDevicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_WITH_NO_PLUGINS_PATH, DevicePrint.class);
		DevicePrint storedDevicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_WITH_NO_PLUGINS_PATH, DevicePrint.class);
		
		RiskBasedRunnerConfig config = JsonHelper.readJSONFromFile(RISK_CONFIG_PATH, RiskBasedRunnerConfig.class);
		
		ComparisonResult compareDevicePrintsPart = cut.compareDevicePrintsPart(currentDevicePrint, storedDevicePrint, config);
		assertEquals((long)compareDevicePrintsPart.getPenaltyPoints(), EXACT_MATCH);
		assertTrue(compareDevicePrintsPart.isSuccessful());
	}
	
	@Test
	public void test3DifferentPluginsWithoutIgnore() throws JsonParseException, JsonMappingException, IOException {
		DevicePrint currentDevicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_WITH_PLUGINS_PATH, DevicePrint.class);
		DevicePrint storedDevicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_WITH_3_DIFFERENT_PLUGINS_PATH, DevicePrint.class);
		
		RiskBasedRunnerConfig config = JsonHelper.readJSONFromFile(RISK_CONFIG_PATH, RiskBasedRunnerConfig.class);
		
		ComparisonResult compareDevicePrintsPart = cut.compareDevicePrintsPart(currentDevicePrint, storedDevicePrint, config);
		assertEquals((long)compareDevicePrintsPart.getPenaltyPoints(), PLUGINS_NOT_SAME_PENALTY_POINTS);
		assertFalse(compareDevicePrintsPart.isSuccessful());
	}
	
	@Test
	public void test3DifferentPluginsWith3ItemsOr30PercentIgnore() throws JsonParseException, JsonMappingException, IOException {
		DevicePrint currentDevicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_WITH_PLUGINS_PATH, DevicePrint.class);
		DevicePrint storedDevicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_WITH_3_DIFFERENT_PLUGINS_PATH, DevicePrint.class);
		
		RiskBasedRunnerConfig config = JsonHelper.readJSONFromFile(IGNORE_PLUGINS_DIFFERENCES_MAX_TOLERATE_IS_2_OR_30PERCENT_CONFIG_PATH, RiskBasedRunnerConfig.class);
		
		ComparisonResult compareDevicePrintsPart = cut.compareDevicePrintsPart(currentDevicePrint, storedDevicePrint, config);
		assertEquals((long)compareDevicePrintsPart.getPenaltyPoints(), EXACT_MATCH);
		assertTrue(compareDevicePrintsPart.isSuccessful());
	}
	
	@Test
	public void testMoreThen30PercentButLessThen3DifferencesWith3ItemsOr30PercentIgnore() throws JsonParseException, JsonMappingException, IOException {
		DevicePrint currentDevicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_WITH_NO_PLUGINS_PATH, DevicePrint.class);
		DevicePrint storedDevicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_WITH_ONE_PLUGIN_PATH, DevicePrint.class);
		
		RiskBasedRunnerConfig config = JsonHelper.readJSONFromFile(IGNORE_PLUGINS_DIFFERENCES_MAX_TOLERATE_IS_2_OR_30PERCENT_CONFIG_PATH, RiskBasedRunnerConfig.class);
		
		ComparisonResult compareDevicePrintsPart = cut.compareDevicePrintsPart(currentDevicePrint, storedDevicePrint, config);
		assertEquals((long)compareDevicePrintsPart.getPenaltyPoints(), EXACT_MATCH);
		assertTrue(compareDevicePrintsPart.isSuccessful());
	}
	
	@Test
	public void test10And40PercentDifferentPluginsWith3ItemsOr30PercentIgnore() throws JsonParseException, JsonMappingException, IOException {
		DevicePrint currentDevicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_WITH_PLUGINS_PATH, DevicePrint.class);
		DevicePrint storedDevicePrint = JsonHelper.readJSONFromFile(FULL_DEVICE_PRINT_WITH_10_DIFFERENT_PLUGINS_PATH, DevicePrint.class);
		
		RiskBasedRunnerConfig config = JsonHelper.readJSONFromFile(IGNORE_PLUGINS_DIFFERENCES_MAX_TOLERATE_IS_2_OR_30PERCENT_CONFIG_PATH, RiskBasedRunnerConfig.class);
		
		ComparisonResult compareDevicePrintsPart = cut.compareDevicePrintsPart(currentDevicePrint, storedDevicePrint, config);
		assertEquals((long)compareDevicePrintsPart.getPenaltyPoints(), PLUGINS_NOT_SAME_PENALTY_POINTS);
		assertFalse(compareDevicePrintsPart.isSuccessful());
	}
	
	public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
		DevicePrintRiskBasedInstalledPluginsComparatorTest test = new DevicePrintRiskBasedInstalledPluginsComparatorTest();
		test.testNoDifferentsInPluginsWithoutIgnore();
		test.testMoreThen30PercentButLessThen3DifferencesWith3ItemsOr30PercentIgnore();
		test.testDifferentPluginsWithoutIgnore();
		test.testBothNoPluginsWithoutIgnore();
		test.test3DifferentPluginsWithoutIgnore();
		test.test3DifferentPluginsWith3ItemsOr30PercentIgnore();
		test.test10And40PercentDifferentPluginsWith3ItemsOr30PercentIgnore();
	}
	
}
