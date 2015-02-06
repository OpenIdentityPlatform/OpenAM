/**
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
 * Copyright 2014-2015 ForgeRock AS.
 */
package com.sun.identity.shared.debug;

import com.sun.identity.shared.debug.file.impl.DebugConfigurationFromProperties;
import com.sun.identity.shared.debug.file.impl.InvalidDebugConfigurationException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test the loading of debug configuration from properties
 */
public class DebugConfigurationFromPropertiesTest {

    private static final String DEBUG_CONFIG_DIRECTORY = "/debug_config_test/config_validation/";

    /*
     * Valid configuration
     */

    @Test
    public void basicConfig() throws Exception {
        DebugConfigurationFromProperties debugConfigurationFromProperties = new DebugConfigurationFromProperties
                (DEBUG_CONFIG_DIRECTORY + "valid/basicconfig.properties");
        Assert.assertTrue(debugConfigurationFromProperties.getDebugPrefix().isEmpty(), "Debug prefix should be empty");
        Assert.assertEquals(debugConfigurationFromProperties.getDebugSuffix(), "-MM.dd.yyyy-HH.mm");
        Assert.assertEquals(debugConfigurationFromProperties.getRotationInterval(), -1, "Debug rotation should be " +
                "empty");

    }

    @Test
    public void prefixConfig() throws Exception {
        DebugConfigurationFromProperties debugConfigurationFromProperties = new DebugConfigurationFromProperties
                (DEBUG_CONFIG_DIRECTORY + "valid/prefixconfig.properties");
        Assert.assertEquals(debugConfigurationFromProperties.getDebugPrefix(), "openam_");
        Assert.assertTrue(debugConfigurationFromProperties.getDebugSuffix().isEmpty(), "Debug suffix should be empty");
        Assert.assertEquals(debugConfigurationFromProperties.getRotationInterval(), -1, "Debug rotation should be " +
                "empty");

    }

    @Test
    public void rotationConfig() throws Exception {
        DebugConfigurationFromProperties debugConfigurationFromProperties = new DebugConfigurationFromProperties
                (DEBUG_CONFIG_DIRECTORY + "valid/rotationconfig.properties");
        Assert.assertTrue(debugConfigurationFromProperties.getDebugPrefix().isEmpty(), "Debug prefix should be empty");
        Assert.assertEquals(debugConfigurationFromProperties.getDebugSuffix(), "-MM.dd.yyyy-HH.mm");
        Assert.assertEquals(debugConfigurationFromProperties.getRotationInterval(), 3);
    }

     /*
     * Invalid configuration
     */


    @Test(expectedExceptions = InvalidDebugConfigurationException.class)
    public void cantParseSuffix() throws Exception {
        new DebugConfigurationFromProperties(DEBUG_CONFIG_DIRECTORY + "invalid/cantParseSuffix.properties");
    }

    @Test(expectedExceptions = InvalidDebugConfigurationException.class)
    public void negativeRotation() throws Exception {
        new DebugConfigurationFromProperties(DEBUG_CONFIG_DIRECTORY + "invalid/negativeRotation.properties");
    }

    @Test(expectedExceptions = InvalidDebugConfigurationException.class)
    public void rotationNotCompatibleWithSuffix() throws Exception {
        new DebugConfigurationFromProperties(DEBUG_CONFIG_DIRECTORY + "invalid/rotationNotCompatibleWithSuffix" +
                ".properties");
    }

    @Test(expectedExceptions = InvalidDebugConfigurationException.class)
    public void suffixEmptyAndRotationEnabled() throws Exception {
        new DebugConfigurationFromProperties(DEBUG_CONFIG_DIRECTORY + "invalid/suffixEmptyAndRotationEnabled" +
                ".properties");
    }

}
