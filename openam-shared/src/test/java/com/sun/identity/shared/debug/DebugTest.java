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

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for DebugImpl.
 */

public class DebugTest extends DebugTestTemplate {

    protected static final String DEBUG_CONFIG_FOR_TEST = "/debug_config_test/debugconfig.properties";

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        SystemPropertiesManager.initializeProperties(DebugConstants.CONFIG_DEBUG_PROPERTIES_VARIABLE,
                DEBUG_CONFIG_FOR_TEST);
        initializeProperties();

    }

    @Test
    public void changeDebugLevel() throws Exception {
        initializeProvider(DEBUG_CONFIG_FOR_TEST);

        //initialize a scenario
        SystemPropertiesManager.initializeProperties(DebugConstants.CONFIG_DEBUG_LEVEL, DebugLevel.ERROR.getName());
        IDebug debug = provider.getInstance(logName);
        debug.message("Should not appear in log", null);

        //check status before the test
        checkLogFileStatus(false, logName);
        debug.error("Should appear in log", null);
        checkLogFileStatus(true, logName);

        Assert.assertEquals(debug.getState(), DebugLevel.ERROR.getLevel(), "Debug level state");

        //Change debug level
        debug.setDebug(DebugLevel.MESSAGE.getLevel());

        //Check result
        debug.message("Should appear in log", null);
        Assert.assertEquals(debug.getState(), DebugLevel.MESSAGE.getLevel(), "Debug level state");

    }

    @Test
    public void mergeAll() throws Exception {
        initializeProvider(DEBUG_CONFIG_FOR_TEST);

        //initialize a scenario
        SystemPropertiesManager.initializeProperties(DebugConstants.CONFIG_DEBUG_MERGEALL, MERGE_ALL_ON);
        IDebug debug = provider.getInstance(logName);
        debug.message("Should not appear in log", null);
        debug.error("Should appear in log", null);

        //Check that we write in the merge file
        checkLogFileStatus(true, DebugConstants.CONFIG_DEBUG_MERGEALL_FILE);

    }

    @Test
    public void resetDebug() throws Exception {
        initializeProvider(DEBUG_CONFIG_FOR_TEST);

        //initialize a scenario
        IDebug debug = provider.getInstance(logName);
        debug.message("Should appear in log", null);

        //Check status before test
        checkLogFileStatus(false, DebugConstants.CONFIG_DEBUG_MERGEALL_FILE);
        checkLogFileStatus(true, logName);

        //Do the reset
        SystemPropertiesManager.initializeProperties(DebugConstants.CONFIG_DEBUG_LEVEL, DebugLevel.ERROR.getName());
        debug.resetDebug(MERGE_ALL_ON);
        debug.error("Should appear in log", null);

        //Test that every has been reset with the new properties
        Assert.assertEquals(debug.getState(), DebugLevel.ERROR.getLevel(), "Debug level state");
        checkLogFileStatus(true, DebugConstants.CONFIG_DEBUG_MERGEALL_FILE);

    }

}
