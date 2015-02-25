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
 * Copyright 2014 ForgeRock AS.
 */
package com.sun.identity.shared.debug;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.timeservice.AccelerateTimeService;
import org.forgerock.util.time.TimeService;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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

    @Test
    public void rotation() throws Exception {
        String DEBUG_CONFIG_FOR_TEST = "/debug_config_test/debugconfigRotation.properties";
        SystemPropertiesManager.initializeProperties(DebugConstants.CONFIG_DEBUG_PROPERTIES_VARIABLE,
                DEBUG_CONFIG_FOR_TEST);
        initializeProperties();

        //initialize a scenario
        SimpleDateFormat dateFormat = new SimpleDateFormat("-MM.dd.yyyy-HH.mm");

        String debugNameFile = "debugMerge";


        long initTime = System.currentTimeMillis();

        //Accelerate the test timeservice
        //1s <=> 12 min = 720sec = 720x
        int testDurationMs = 1000;
        int factor = 720;
        TimeService accelerateClock = new AccelerateTimeService(initTime, factor);
        debugFileProvider.setClock(accelerateClock);

        // check debugFiles.properties to see the mapping
        IDebug debugTest1MergeToDebugMerge = provider.getInstance("debugTest1MergeToDebugMerge");
        IDebug debugTest2MergeToDebugMerge = provider.getInstance("debugTest2MergeToDebugMerge");
        IDebug debugTest3MergeToDebugMerge = provider.getInstance("debugTest3MergeToDebugMerge");

        // We will print on logs from threads, for testing the synchronized
        List<Thread> threads = new ArrayList<Thread>();
        threads.add(new Thread(new PrintLogRunnable(debugTest1MergeToDebugMerge, initTime, testDurationMs)));
        threads.add(new Thread(new PrintLogRunnable(debugTest2MergeToDebugMerge, initTime, testDurationMs)));
        threads.add(new Thread(new PrintLogRunnable(debugTest3MergeToDebugMerge, initTime, testDurationMs)));


        //The first writing initialize the log file. So we test that a swift of 1 minute doesn't
        //create a new file
        debugTest1MergeToDebugMerge.message("Should not appear in log", null);
        Thread.sleep(1000 * 60 / factor);
        debugTest2MergeToDebugMerge.message("Should not appear in log", null);
        Thread.sleep(1000 * 60 / factor);
        debugTest3MergeToDebugMerge.message("Should not appear in log", null);


        //Start threads
        for (Thread thread : threads) {
            thread.start();
        }

        //Wait threads
        for (Thread thread : threads) {
            thread.join();
        }

        //Check files creation
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(initTime);

        //It's possible that we stored the init time just before the next minute
        if (!isFileExist(debugNameFile + dateFormat.format(cal.getTime()))) {
            cal.add(Calendar.MINUTE, 1);
        }

        checkLogFileStatus(true, debugNameFile + dateFormat.format(cal.getTime()));

        cal.add(Calendar.MINUTE, 1);
        checkLogFileStatus(false, debugNameFile + dateFormat.format(cal.getTime()));

        cal.add(Calendar.MINUTE, 1);
        checkLogFileStatus(false, debugNameFile + dateFormat.format(cal.getTime()));

        cal.add(Calendar.MINUTE, 1);
        checkLogFileStatus(true, debugNameFile + dateFormat.format(cal.getTime()));

        cal.add(Calendar.MINUTE, 1);
        checkLogFileStatus(false, debugNameFile + dateFormat.format(cal.getTime()));

        cal.add(Calendar.MINUTE, 1);
        checkLogFileStatus(false, debugNameFile + dateFormat.format(cal.getTime()));

        cal.add(Calendar.MINUTE, 1);
        checkLogFileStatus(true, debugNameFile + dateFormat.format(cal.getTime()));

        cal.add(Calendar.MINUTE, 1);
        checkLogFileStatus(false, debugNameFile + dateFormat.format(cal.getTime()));

        cal.add(Calendar.MINUTE, 1);
        checkLogFileStatus(false, debugNameFile + dateFormat.format(cal.getTime()));

    }

    /**
     * Runnable for printing in a log file
     */
    public static class PrintLogRunnable implements Runnable {

        private IDebug debug;
        private long initTime;
        private int testDuration;


        public PrintLogRunnable(IDebug debug, long initTime, int testDurationMS) {
            this.debug = debug;
            this.initTime = initTime;
            this.testDuration = testDurationMS;
        }

        public void run() {
            while (System.currentTimeMillis() - initTime < testDuration) {
                debug.message("Should not appear in log", null);
                debug.message("Should not appear in log", null);
                debug.message("Should not appear in log", null);
            }
        }
    }

}
