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
import com.sun.identity.shared.timeservice.AccelerateTimeService;
import org.forgerock.util.time.TimeService;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DebugRotationTest extends DebugTestTemplate {

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z");

    @Test
    public void rotationInNormalDate() throws Exception {
        Calendar calRandomDate = Calendar.getInstance();
        calRandomDate.set(Calendar.YEAR, 1989);
        calRandomDate.set(Calendar.MONTH, Calendar.MAY);
        calRandomDate.set(Calendar.DAY_OF_MONTH, 16);

        calRandomDate.set(Calendar.HOUR_OF_DAY, 0);
        calRandomDate.set(Calendar.MINUTE, 0);
        calRandomDate.set(Calendar.SECOND, 0);
        calRandomDate.set(Calendar.MILLISECOND, 0);

        long fakeInitTime = calRandomDate.getTimeInMillis();

        System.out.println("Test rotation for date : '" + dateFormat.format(calRandomDate.getTime()) + "'");
        rotation(fakeInitTime);
    }

    @Test
    public void rotationInDSTDateMarch() throws Exception {
        Calendar calDSTMarch = Calendar.getInstance();
        calDSTMarch.set(Calendar.YEAR, 2015);
        calDSTMarch.set(Calendar.MONTH, Calendar.MARCH);
        calDSTMarch.set(Calendar.DAY_OF_MONTH, 29);

        calDSTMarch.set(Calendar.HOUR_OF_DAY, 0);
        calDSTMarch.set(Calendar.MINUTE, 58);
        calDSTMarch.set(Calendar.SECOND, 0);
        calDSTMarch.set(Calendar.MILLISECOND, 0);

        long fakeInitTime = calDSTMarch.getTimeInMillis();

        System.out.println("Test rotation for date : '" + dateFormat.format(calDSTMarch.getTime()) + "'");
        rotation(fakeInitTime);
    }

    @Test
    public void rotationInDSTDateOctober() throws Exception {
        Calendar calDSTOctober = Calendar.getInstance();
        calDSTOctober.set(Calendar.YEAR, 2015);
        calDSTOctober.set(Calendar.MONTH, Calendar.OCTOBER);
        calDSTOctober.set(Calendar.DAY_OF_MONTH, 26);

        calDSTOctober.set(Calendar.HOUR_OF_DAY, 1);
        calDSTOctober.set(Calendar.MINUTE, 58);
        calDSTOctober.set(Calendar.SECOND, 0);
        calDSTOctober.set(Calendar.MILLISECOND, 0);

        long fakeInitTime = calDSTOctober.getTimeInMillis();

        System.out.println("Test rotation for date : '" + dateFormat.format(calDSTOctober.getTime()) + "'");

        rotation(fakeInitTime);
    }

    private void rotation(long fakeInitTime) throws Exception {
        String DEBUG_CONFIG_FOR_TEST = "/debug_config_test/debugconfigRotation.properties";

        DebugConfigurationFromProperties debugConfigurationFromProperties =
                new DebugConfigurationFromProperties(DEBUG_CONFIG_FOR_TEST);
        initializeProperties();
        initializeProvider(DEBUG_CONFIG_FOR_TEST);

        //initialize a scenario
        SimpleDateFormat dateFormat = new SimpleDateFormat("-MM.dd.yyyy-HH.mm");

        String debugNameFile = "debugMerge";

        int rotationPeriod = debugConfigurationFromProperties.getRotationInterval();

        //Accelerate the test timeservice
        // Simulate 1 hours of logs
        int fakeDurationMs = 60 * 60 * 1000;


        //In order to have an effective and short in time test, we accelerate the time
        AccelerateTimeService accelerateClock = new AccelerateTimeService(fakeInitTime);
        debugFileProvider.setClock(accelerateClock);


        // check debugFiles.properties to see the mapping
        IDebug debugTest1MergeToDebugMerge = provider.getInstance("debugTest1MergeToDebugMerge");
        IDebug debugTest2MergeToDebugMerge = provider.getInstance("debugTest2MergeToDebugMerge");
        IDebug debugTest3MergeToDebugMerge = provider.getInstance("debugTest3MergeToDebugMerge");

        // We will print on logs from threads, for testing the synchronized
        List<PrintLogRunnable> printLogRunnableTests = new ArrayList<PrintLogRunnable>();

        PrintLogRunnable printLogRunnableTest1 = new PrintLogRunnable(debugTest1MergeToDebugMerge, fakeInitTime,
                fakeDurationMs, accelerateClock);
        printLogRunnableTests.add(printLogRunnableTest1);

        PrintLogRunnable printLogRunnableTest2 = new PrintLogRunnable(debugTest2MergeToDebugMerge, fakeInitTime,
                fakeDurationMs, accelerateClock);
        printLogRunnableTests.add(printLogRunnableTest2);

        PrintLogRunnable printLogRunnableTest3 = new PrintLogRunnable(debugTest3MergeToDebugMerge, fakeInitTime,
                fakeDurationMs, accelerateClock);
        printLogRunnableTests.add(printLogRunnableTest3);

        List<Thread> threads = new ArrayList<Thread>();
        for (PrintLogRunnable printLogRunnableTest : printLogRunnableTests) {
            threads.add(new Thread(printLogRunnableTest));
        }

        //The first writing initialize the log file. So we test that a swift of 1 minute doesn't
        //create a new file at the end
        debugTest1MergeToDebugMerge.message("Should appear in log", null);

        accelerateClock.incrementTime(1000 * 60 + 10);
        debugTest2MergeToDebugMerge.message("Should appear in log", null);

        accelerateClock.incrementTime(1000 * 60 + 10);
        debugTest3MergeToDebugMerge.message("Should appear in log", null);

        //Start threads
        for (Thread thread : threads) {
            thread.start();
        }

        //Wait threads
        for (Thread thread : threads) {
            thread.join();
        }

        //Check if any thread had a exception
        for (PrintLogRunnable printLogRunnableTest : printLogRunnableTests) {
            if (printLogRunnableTest.ex != null) throw printLogRunnableTest.ex;
        }


        //Check files creation
        Calendar fakeDate = Calendar.getInstance();
        fakeDate.setTimeInMillis(fakeInitTime);

        int currentPeriod = -1;
        while (fakeDate.getTimeInMillis() - fakeInitTime < fakeDurationMs) {

            if (isFileExist(debugNameFile + dateFormat.format(fakeDate.getTime()))) {
                if(currentPeriod != -1 && currentPeriod < rotationPeriod) {
                    failAndPrintFolderStatusReport("A log rotation file is created before the log rotation ended. " +
                            "currentPeriod= '" + currentPeriod + "'");
                }
                currentPeriod = 0;
            }
            currentPeriod++;
            fakeDate.add(Calendar.MINUTE, 1);
        }

    }

    /**
     * Runnable for printing in a log file
     */
    public static class PrintLogRunnable implements Runnable {

        private IDebug debug;
        private long initTime;
        private int testDuration;
        private TimeService accelerateClock;

        //Date formats for helping the debugging
        private SimpleDateFormat dateFormatWithMs;

        public Exception ex = null;

        public PrintLogRunnable(IDebug debug, long initTime, int testDurationMS, TimeService accelerateClock) {
            this.debug = debug;
            this.initTime = initTime;
            this.testDuration = testDurationMS;
            this.accelerateClock = accelerateClock;
            this.dateFormatWithMs = new SimpleDateFormat("-MM.dd.yyyy-HH.mm-ss-SSS");
        }

        public void run() {
            try {
                while (accelerateClock.now() - initTime < testDuration) {

                    String dateInStringWithMs = dateFormatWithMs.format(new Date(accelerateClock.now()));
                    debug.message("Fake date = " + dateInStringWithMs, null);
                }
            } catch (Exception e) {
                this.ex = e;
            }
        }
    }

}
