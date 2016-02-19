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
 * Copyright 2015-2016 ForgeRock AS.
 */
package com.sun.identity.shared.debug.file.impl;

import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.shared.debug.DebugConstants;
import com.sun.identity.shared.debug.file.DebugConfiguration;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.openam.utils.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

/**
 * Read debug configuration from a properties file.
 */
public class DebugConfigurationFromProperties implements DebugConfiguration {

    private String debugPrefix = "";

    private String debugSuffix = "";

    private int rotationInterval = -1;

    private long maxFileSizeInByte = -1;

    /**
     * initialize the properties
     * It will reset the current properties for every Debug instance
     *
     * @param debugFilePropertiesPath path to the debug configuration file
     */
    public DebugConfigurationFromProperties(String debugFilePropertiesPath) throws InvalidDebugConfigurationException {
        InputStream is = null;
        try {
            is = DebugConfigurationFromProperties.class.getResourceAsStream(debugFilePropertiesPath);
            if(is == null) {
                throw new InvalidDebugConfigurationException("Can't find the configuration file '" +
                        debugFilePropertiesPath + "'.");
            }
            Properties rotationConfig = new Properties();
            rotationConfig.load(is);

            debugPrefix = rotationConfig.getProperty(DebugConstants.CONFIG_DEBUG_LOGFILE_PREFIX);
            debugSuffix = rotationConfig.getProperty(DebugConstants.CONFIG_DEBUG_LOGFILE_SUFFIX);

            String maxFileSizedInMb = rotationConfig.getProperty(DebugConstants.CONFIG_DEBUG_LOGFILE_MAX_SIZE);
            if (!StringUtils.isEmpty(maxFileSizedInMb)) {
                try {
                    maxFileSizeInByte = Integer.parseInt(maxFileSizedInMb);
                    //Convert MaxFileSize to byte
                    maxFileSizeInByte <<= 20;
                } catch (NumberFormatException e) {
                    //Can't parse the number
                    String message = "The '" + DebugConstants.CONFIG_DEBUG_LOGFILE_MAX_SIZE + "' value : "
                            + maxFileSizedInMb + "' cannot be parsed. Please check the configuration file '"
                            + DebugConstants.CONFIG_DEBUG_PROPERTIES + "'.";
                    StdDebugFile.printError(DebugConfigurationFromProperties.class.getSimpleName(), message, e);
                }
            }

            String rotation = rotationConfig.getProperty(DebugConstants.CONFIG_DEBUG_LOGFILE_ROTATION);
            if (!StringUtils.isEmpty(rotation)) {
                try {
                    rotationInterval = Integer.parseInt(rotation);
                } catch (NumberFormatException e) {
                    //Can't parse the number
                    String message = "'" + DebugConstants.CONFIG_DEBUG_LOGFILE_ROTATION + "' value can't be parsed: '" +
                            rotation + "'. Please check the configuration file '" +
                            DebugConstants.CONFIG_DEBUG_PROPERTIES + "'.";
                    StdDebugFile.printError(DebugConfigurationFromProperties.class.getSimpleName(), message, e);
                    rotationInterval = -1;
                }

            }
            validate();
        } catch (IOException ex) {
            //it's possible, that we don't have the config file
            String message = "Can't load debug file properties. Please check the configuration file '" +
                    debugFilePropertiesPath + "'.";
            throw new InvalidDebugConfigurationException(message);
        } finally {
            IOUtils.closeIfNotNull(is);
        }

    }

    @Override
    public String getDebugPrefix() {
        return debugPrefix;
    }

    @Override
    public String getDebugSuffix() {
        return debugSuffix;
    }

    @Override
    public int getRotationInterval() {
        return rotationInterval;
    }

    @Override
    public long getRotationFileSizeInByte() {
        return maxFileSizeInByte;
    }

    /**
     * Check if the configuration properties is valid
     *
     * @throws InvalidDebugConfigurationException
     */
    private void validate() throws InvalidDebugConfigurationException {

        if (getRotationFileSizeInByte() != -1) {
            if (getRotationFileSizeInByte() <= 0) {
                throw new InvalidDebugConfigurationException("File size rotation needs to be greater than " +
                        "zero. File size rotation = '" + getRotationFileSizeInByte() + "'", null);
            }

            if (getDebugSuffix().isEmpty()) {
                throw new InvalidDebugConfigurationException("Log size rotation is enabled (File size rotation = " +
                        getRotationFileSizeInByte() + ") but the debug suffix is empty");
            }

            // Check the rotation and suffix consistency
            try {
                if (validateSuffix(Calendar.MILLISECOND, 1)) {
                    throw new InvalidDebugConfigurationException("This suffix '" + getDebugSuffix() + "' isn't " +
                            "compatible with the file size rotation enable.");
                }
            } catch (IllegalArgumentException e) {
                throw new InvalidDebugConfigurationException("Suffix '" + getDebugSuffix() + "' can't be parsed.");
            }
        }

        if (getRotationInterval() != -1) {
            if (getRotationInterval() <= 0) {
                throw new InvalidDebugConfigurationException("Rotation interval needs to be greater than zero. " +
                        "rotationInterval = '" + rotationInterval + "'", null);
            }

            if (getDebugSuffix().isEmpty()) {
                throw new InvalidDebugConfigurationException("Log time rotation is enabled (rotation interval = " +
                        getRotationInterval() + ") but the debug suffix is empty");
            }

            // Check the rotation and suffix consistency
            try {
                if (validateSuffix(Calendar.MINUTE, getRotationInterval())) {
                    throw new InvalidDebugConfigurationException("Suffix '" + getDebugSuffix() + "' isn't compatible" +
                            " with the rotation interval requested '" + getRotationInterval() + "'.");
                }
            } catch (IllegalArgumentException e) {
                throw new InvalidDebugConfigurationException("Suffix '" + getDebugSuffix() + "' can't be parsed.");
            }
        }
    }

    /**
     * Validate Suffix
     * Suffix need to be parse. This function check that the suffix is compatible with the rotation period.
     * @param field Calendar field
     * @param amount number of unit
     * @return true if the suffix generated are different
     * @throws IllegalArgumentException
     */
    private boolean validateSuffix(int field, int amount) throws IllegalArgumentException {
        // Check the rotation and suffix consistency
        SimpleDateFormat dateFormat = new SimpleDateFormat(getDebugSuffix());
        Calendar cal = getCalendarInstance();
        cal.setTimeInMillis(0);
        String initialSuffix = dateFormat.format(cal.getTime());
        cal.add(field, amount);
        String suffixAfterOneRotation = dateFormat.format(cal.getTime());
        return suffixAfterOneRotation.equals(initialSuffix);
    }

    @Override
    public String toString() {
        return "DebugConfigurationFromProperties{" +
                "debugPrefix='" + debugPrefix + '\'' +
                ", debugSuffix='" + debugSuffix + '\'' +
                ", rotationInterval=" + rotationInterval +
                '}';
    }
}
