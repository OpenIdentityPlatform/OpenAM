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
 * Copyright 2015 ForgeRock AS.
 */
package com.sun.identity.shared.debug;


import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.file.impl.InvalidDebugConfigurationException;
import org.testng.annotations.Test;

public class DebugInitializationTest extends DebugTestTemplate {

    @Test
    public void initializationWithDebugDirectoryNullTest() throws InvalidDebugConfigurationException {

        String debugDirectory = SystemPropertiesManager.get(DebugConstants.CONFIG_DEBUG_DIRECTORY);

        SystemPropertiesManager.initializeProperties(DebugConstants.CONFIG_DEBUG_DIRECTORY, null);

        String DEBUG_CONFIG_FOR_TEST = "/debug_config_test/debugconfig.properties";

        initializeProperties();
        initializeProvider(DEBUG_CONFIG_FOR_TEST);
        IDebug debug = provider.getInstance(logName);

        debug.error("in STD out", null);

        checkLogFileStatus(false, logName);

        SystemPropertiesManager.initializeProperties(DebugConstants.CONFIG_DEBUG_DIRECTORY, debugDirectory);

        debug.error("in log file", null);
        checkLogFileStatus(true, logName);

    }

}
