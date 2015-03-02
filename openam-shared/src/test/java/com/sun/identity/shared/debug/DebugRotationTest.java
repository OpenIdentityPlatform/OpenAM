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


import com.sun.identity.shared.timeservice.AccelerateTimeService;
import org.forgerock.util.time.TimeService;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

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

        System.out.println(TimeZone.getDefault().getDisplayName());
        System.out.println(TimeZone.getDefault().getID());
        System.out.println("Test rotation for date : '" + dateFormat.format(calDSTOctober.getTime()) + "'");

        rotation(fakeInitTime);
    }

    private void rotation(long fakeInitTime) throws Exception {
        String DEBUG_CONFIG_FOR_TEST = "/debug_config_test/debugconfigRotation.properties";

        initializeProperties();
        initializeProvider(DEBUG_CONFIG_FOR_TEST);

        //initialize a scenario
        SimpleDateFormat dateFormat = new SimpleDateFormat("-MM.dd.yyyy-HH.mm");

        String debugNameFile = "debugMerge";

        long initTime = System.currentTimeMillis();

        //Accelerate the test timeservice
        //2s <=> 12 min = 720sec = 360x
        int testDurationMs = 2000;
        int factor = 360;
        int fakeDurationMs = testDurationMs * factor;


        //In order to have an effective and short in time test, we accelerate the time
        TimeService accelerateClock = new AccelerateTimeService(fakeInitTime, factor);
        debugFileProvider.setClock(accelerateClock);


        // check debugFiles.properties to see the mapping
        IDebug debugTest1MergeToDebugMerge = provider.getInstance("debugTest1MergeToDebugMerge");
        IDebug debugTest2MergeToDebugMerge = provider.getInstance("debugTest2MergeToDebugMerge");
        IDebug debugTest3MergeToDebugMerge = provider.getInstance("debugTest3MergeToDebugMerge");

        // We will print on logs from threads, for testing the synchronized
        List<PrintLogRunnable> printLogRunnableTests = new ArrayList<PrintLogRunnable>();

        PrintLogRunnable printLogRunnableTest1 = new PrintLogRunnable(debugTest1MergeToDebugMerge, initTime,
                testDurationMs, accelerateClock);
        printLogRunnableTests.add(printLogRunnableTest1);

        PrintLogRunnable printLogRunnableTest2 = new PrintLogRunnable(debugTest2MergeToDebugMerge, initTime,
                testDurationMs, accelerateClock);
        printLogRunnableTests.add(printLogRunnableTest2);

        PrintLogRunnable printLogRunnableTest3 = new PrintLogRunnable(debugTest3MergeToDebugMerge, initTime,
                testDurationMs, accelerateClock);
        printLogRunnableTests.add(printLogRunnableTest3);

        List<Thread> threads = new ArrayList<Thread>();
        for (PrintLogRunnable printLogRunnableTest : printLogRunnableTests) {
            threads.add(new Thread(printLogRunnableTest));
        }

        //The first writing initialize the log file. So we test that a swift of 1 minute doesn't
        //create a new file at the end
        debugTest1MergeToDebugMerge.message("Should appear in log", null);
        long currentAccelerateTimeInMin = accelerateClock.now() / (1000 * 60);

        while (accelerateClock.now() / (1000 * 60) < currentAccelerateTimeInMin) { Thread.sleep(100); }
        debugTest2MergeToDebugMerge.message("Should appear in log", null);

        currentAccelerateTimeInMin = accelerateClock.now() / (1000 * 60);
        while (accelerateClock.now() / (1000 * 60) < currentAccelerateTimeInMin) { Thread.sleep(100); }
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
        Calendar calRandomDate = Calendar.getInstance();
        calRandomDate.setTimeInMillis(fakeInitTime);

        //It's possible that we stored the init time just before the next minute
        if (!isFileExist(debugNameFile + dateFormat.format(calRandomDate.getTime()))) {
            calRandomDate.add(Calendar.MINUTE, 1);
        }

        while (calRandomDate.getTimeInMillis() - fakeInitTime < fakeDurationMs) {

            checkLogFileStatus(true, debugNameFile + dateFormat.format(calRandomDate.getTime()));
            calRandomDate.add(Calendar.MINUTE, 1);

            checkLogFileStatus(false, debugNameFile + dateFormat.format(calRandomDate.getTime()));
            calRandomDate.add(Calendar.MINUTE, 1);

            checkLogFileStatus(false, debugNameFile + dateFormat.format(calRandomDate.getTime()));
            calRandomDate.add(Calendar.MINUTE, 1);
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
                while (System.currentTimeMillis() - initTime < testDuration) {

                    String dateInStringWithMs = dateFormatWithMs.format(new Date(accelerateClock.now()));
                    debug.message("Fake date = " + dateInStringWithMs, null);
                }
            } catch (Exception e) {
                this.ex = e;
            }
        }
    }

}
