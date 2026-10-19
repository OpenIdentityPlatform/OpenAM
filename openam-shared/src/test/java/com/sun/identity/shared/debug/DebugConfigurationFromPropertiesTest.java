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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions Copyright 2026 3A Systems, LLC.
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
        Assert.assertEquals(debugConfigurationFromProperties.getRotationFileSizeInByte(), -1, "Debug file size " +
                "rotation should be empty");
    }

    @Test
    public void prefixConfig() throws Exception {
        DebugConfigurationFromProperties debugConfigurationFromProperties = new DebugConfigurationFromProperties
                (DEBUG_CONFIG_DIRECTORY + "valid/prefixconfig.properties");
        Assert.assertEquals(debugConfigurationFromProperties.getDebugPrefix(), "openam_");
        Assert.assertTrue(debugConfigurationFromProperties.getDebugSuffix().isEmpty(), "Debug suffix should be empty");
        Assert.assertEquals(debugConfigurationFromProperties.getRotationInterval(), -1, "Debug rotation should be " +
                "empty");
        Assert.assertEquals(debugConfigurationFromProperties.getRotationFileSizeInByte(), -1, "Debug file size " +
                "rotation should be empty");
    }

    @Test
    public void timeRotationConfig() throws Exception {
        DebugConfigurationFromProperties debugConfigurationFromProperties = new DebugConfigurationFromProperties
                (DEBUG_CONFIG_DIRECTORY + "valid/timeRotationConfig.properties");
        Assert.assertTrue(debugConfigurationFromProperties.getDebugPrefix().isEmpty(), "Debug prefix should be empty");
        Assert.assertEquals(debugConfigurationFromProperties.getDebugSuffix(), "-MM.dd.yyyy-HH.mm");
        Assert.assertEquals(debugConfigurationFromProperties.getRotationInterval(), 3);
        Assert.assertEquals(debugConfigurationFromProperties.getRotationFileSizeInByte(), -1, "Debug file size " +
                "rotation should be empty");
    }

    @Test
    public void sizeRotationConfig() throws Exception {
        DebugConfigurationFromProperties debugConfigurationFromProperties = new DebugConfigurationFromProperties
                (DEBUG_CONFIG_DIRECTORY + "valid/sizeRotationConfig.properties");
        Assert.assertTrue(debugConfigurationFromProperties.getDebugPrefix().isEmpty(), "Debug prefix should be empty");
        Assert.assertEquals(debugConfigurationFromProperties.getDebugSuffix(), "-MM.dd.yyyy-HH.mm.ss.SSS");
        Assert.assertEquals(debugConfigurationFromProperties.getRotationInterval(), -1, "Debug rotation should be " +
                "empty");
        Assert.assertEquals(debugConfigurationFromProperties.getRotationFileSizeInByte(), 2 << 20);
    }

    @Test
    public void sizeAndTimeRotationConfig() throws Exception {
        DebugConfigurationFromProperties debugConfigurationFromProperties = new DebugConfigurationFromProperties
                (DEBUG_CONFIG_DIRECTORY + "valid/sizeAndTimeRotationConfig.properties");
        Assert.assertTrue(debugConfigurationFromProperties.getDebugPrefix().isEmpty(), "Debug prefix should be empty");
        Assert.assertEquals(debugConfigurationFromProperties.getDebugSuffix(), "-MM.dd.yyyy-HH.mm.ss.SSS");
        Assert.assertEquals(debugConfigurationFromProperties.getRotationInterval(), 3);
        Assert.assertEquals(debugConfigurationFromProperties.getRotationFileSizeInByte(), 2 << 20);
    }

    @Test
    public void largeSizeRotation() throws Exception {
        DebugConfigurationFromProperties debugConfigurationFromProperties = new DebugConfigurationFromProperties
                (DEBUG_CONFIG_DIRECTORY + "valid/largeSizeRotation.properties");
        Assert.assertEquals(debugConfigurationFromProperties.getRotationFileSizeInByte(), 2048L << 20);
    }

    @Test
    public void noPrefixAndSuffixLines() throws Exception {
        DebugConfigurationFromProperties debugConfigurationFromProperties = new DebugConfigurationFromProperties
                (DEBUG_CONFIG_DIRECTORY + "valid/noPrefixAndSuffixLines.properties");
        Assert.assertEquals(debugConfigurationFromProperties.getDebugPrefix(), "");
        Assert.assertEquals(debugConfigurationFromProperties.getDebugSuffix(), "");
        Assert.assertEquals(debugConfigurationFromProperties.getRotationInterval(), -1, "Debug rotation should be " +
                "empty");
        Assert.assertEquals(debugConfigurationFromProperties.getRotationFileSizeInByte(), -1, "Debug file size " +
                "rotation should be empty");
    }

    @Test
    public void trailingWhitespace() throws Exception {
        DebugConfigurationFromProperties debugConfigurationFromProperties = new DebugConfigurationFromProperties
                (DEBUG_CONFIG_DIRECTORY + "valid/trailingWhitespace.properties");
        Assert.assertEquals(debugConfigurationFromProperties.getDebugPrefix(), "openam_");
        Assert.assertEquals(debugConfigurationFromProperties.getDebugSuffix(), "-MM.dd.yyyy-HH.mm.ss.SSS");
        Assert.assertEquals(debugConfigurationFromProperties.getRotationInterval(), 3);
        Assert.assertEquals(debugConfigurationFromProperties.getRotationFileSizeInByte(), 2 << 20);
    }
     /*
     * Invalid configuration
     */


    @Test(expectedExceptions = InvalidDebugConfigurationException.class,
            expectedExceptionsMessageRegExp = "Can't load debug file properties from '.*': "
                    + "java\\.lang\\.IllegalArgumentException: Malformed .*")
    public void malformedUnicodeEscape() throws Exception {
        new DebugConfigurationFromProperties(DEBUG_CONFIG_DIRECTORY + "invalid/malformedUnicodeEscape.properties");
    }

    @Test(expectedExceptions = InvalidDebugConfigurationException.class,
            expectedExceptionsMessageRegExp = "Suffix '.*' can't be parsed\\.")
    public void cantParseSuffix() throws Exception {
        new DebugConfigurationFromProperties(DEBUG_CONFIG_DIRECTORY + "invalid/cantParseSuffix.properties");
    }

    @Test(expectedExceptions = InvalidDebugConfigurationException.class,
            expectedExceptionsMessageRegExp = "Rotation interval needs to be greater than zero\\. .*\\. "
                    + "Leave the value empty to disable time rotation\\.")
    public void negativeRotation() throws Exception {
        new DebugConfigurationFromProperties(DEBUG_CONFIG_DIRECTORY + "invalid/negativeTimeRotation.properties");
    }

    @Test(expectedExceptions = InvalidDebugConfigurationException.class,
            expectedExceptionsMessageRegExp = "Rotation interval needs to be greater than zero\\. .*\\. "
                    + "Leave the value empty to disable time rotation\\.")
    public void minusOneRotation() throws Exception {
        new DebugConfigurationFromProperties(DEBUG_CONFIG_DIRECTORY + "invalid/minusOneTimeRotation.properties");
    }

    @Test(expectedExceptions = InvalidDebugConfigurationException.class,
            expectedExceptionsMessageRegExp = "Rotation interval needs to be greater than zero\\. .*\\. "
                    + "Leave the value empty to disable time rotation\\.")
    public void zeroRotation() throws Exception {
        new DebugConfigurationFromProperties(DEBUG_CONFIG_DIRECTORY + "invalid/zeroTimeRotation.properties");
    }

    @Test(expectedExceptions = InvalidDebugConfigurationException.class,
            expectedExceptionsMessageRegExp = "Suffix '.*' isn't compatible with the rotation interval requested "
                    + "'3'\\.")
    public void rotationNotCompatibleWithSuffix() throws Exception {
        new DebugConfigurationFromProperties(DEBUG_CONFIG_DIRECTORY + "invalid/timeRotationNotCompatibleWithSuffix" +
                ".properties");
    }

    @Test(expectedExceptions = InvalidDebugConfigurationException.class,
            expectedExceptionsMessageRegExp = "Log time rotation is enabled .* but the debug suffix is empty")
    public void suffixEmptyAndRotationEnabled() throws Exception {
        new DebugConfigurationFromProperties(DEBUG_CONFIG_DIRECTORY + "invalid/suffixEmptyAndRotationTimeEnable" +
                ".properties");
    }

    @Test(expectedExceptions = InvalidDebugConfigurationException.class,
            expectedExceptionsMessageRegExp = "File size rotation needs to be greater than zero\\. "
                    + "File size rotation = '-2' MB\\. Leave the value empty to disable file size rotation\\.")
    public void negativeSizeRotation() throws Exception {
        new DebugConfigurationFromProperties(DEBUG_CONFIG_DIRECTORY + "invalid/negativeSizeRotation.properties");
    }

    @Test(expectedExceptions = InvalidDebugConfigurationException.class,
            expectedExceptionsMessageRegExp = "File size rotation needs to be greater than zero\\. .*\\. "
                    + "Leave the value empty to disable file size rotation\\.")
    public void zeroSizeRotation() throws Exception {
        new DebugConfigurationFromProperties(DEBUG_CONFIG_DIRECTORY + "invalid/zeroSizeRotation.properties");
    }

    @Test(expectedExceptions = InvalidDebugConfigurationException.class,
            expectedExceptionsMessageRegExp = "Log size rotation is enabled .* but the debug suffix is empty")
    public void suffixEmptyAndSizeRotationEnable() throws Exception {
        new DebugConfigurationFromProperties(DEBUG_CONFIG_DIRECTORY + "invalid/suffixEmptyAndSizeRotationTimeEnable" +
                ".properties");
    }

    @Test(expectedExceptions = InvalidDebugConfigurationException.class,
            expectedExceptionsMessageRegExp = "This suffix '.*' isn't compatible with the file size rotation "
                    + "enable\\.")
    public void suffixNotCompatibleWithFileSizeRotationEnable() throws Exception {
        new DebugConfigurationFromProperties(DEBUG_CONFIG_DIRECTORY + "invalid/suffixNotCompatibleWithFileSize" +
                "RotationEnable.properties");
    }

    @Test(expectedExceptions = InvalidDebugConfigurationException.class,
            expectedExceptionsMessageRegExp = "Log time rotation is enabled .* but the debug suffix is empty")
    public void noSuffixLineAndTimeRotationEnabled() throws Exception {
        new DebugConfigurationFromProperties(DEBUG_CONFIG_DIRECTORY + "invalid/noSuffixLineAndTimeRotationEnabled" +
                ".properties");
    }

    @Test(expectedExceptions = InvalidDebugConfigurationException.class,
            expectedExceptionsMessageRegExp = "Log size rotation is enabled .* but the debug suffix is empty")
    public void noSuffixLineAndSizeRotationEnabled() throws Exception {
        new DebugConfigurationFromProperties(DEBUG_CONFIG_DIRECTORY + "invalid/noSuffixLineAndSizeRotationEnabled" +
                ".properties");
    }

}
