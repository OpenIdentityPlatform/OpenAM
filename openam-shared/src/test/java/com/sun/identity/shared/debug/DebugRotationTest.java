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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DebugRotationTest extends DebugTestTemplate {

    @Test
    public void rotation() throws Exception {
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
        TimeService accelerateClock = new AccelerateTimeService(initTime, factor);
        debugFileProvider.setClock(accelerateClock);

        //Log history is to avoid the possibility of preemption due to the accelerate time
        Set<String> logDatesHistory = new HashSet<String>();

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
        //create a new file
        logDatesHistory.add(dateFormat.format(new Date(accelerateClock.now())));
        debugTest1MergeToDebugMerge.message("Should appear in log", null);
        Thread.sleep(1000 * 60 / factor);

        logDatesHistory.add(dateFormat.format(new Date(accelerateClock.now())));
        debugTest2MergeToDebugMerge.message("Should appear in log", null);
        Thread.sleep(1000 * 60 / factor);

        logDatesHistory.add(dateFormat.format(new Date(accelerateClock.now())));
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

        //Merge Log dates history
        //NB : we merge the log dates history from each thread instead of directly insert
        //to a global set in order to not reduce the performance of each thread by
        //inserting in a concurrent set.
        logDatesHistory.addAll(printLogRunnableTest1.getLogDatesHistory());
        logDatesHistory.addAll(printLogRunnableTest2.getLogDatesHistory());
        logDatesHistory.addAll(printLogRunnableTest3.getLogDatesHistory());

        //Check files creation
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(initTime);

        //It's possible that we stored the init time just before the next minute
        if (!isFileExist(debugNameFile + dateFormat.format(cal.getTime()))) {
            cal.add(Calendar.MINUTE, 1);
        }

        while (cal.getTimeInMillis() - initTime < fakeDurationMs) {

            /*
                Before each file exist = true, we will check if there is a log in the history
                if not, it means that the unit test has been preempted at the checked time.
            */
            String currentDateInString = dateFormat.format(cal.getTime());

            if (!logDatesHistory.contains(currentDateInString)) {
                cal.add(Calendar.MINUTE, 1);
                continue;
            }
            checkLogFileStatus(true, debugNameFile + dateFormat.format(cal.getTime()));
            cal.add(Calendar.MINUTE, 1);

            checkLogFileStatus(false, debugNameFile + dateFormat.format(cal.getTime()));
            cal.add(Calendar.MINUTE, 1);

            checkLogFileStatus(false, debugNameFile + dateFormat.format(cal.getTime()));
            cal.add(Calendar.MINUTE, 1);
        }

    }

    /**
     * Runnable for printing in a log file
     */
    public static class PrintLogRunnable implements Runnable {

        private IDebug debug;
        private long initTime;
        private int testDuration;
        private Set<String> logDatesHistory = new HashSet<String>();
        private TimeService accelerateClock;

        //Date formats for helping the debugging
        private SimpleDateFormat dateFormat;
        private SimpleDateFormat dateFormatWithMs;

        public Exception ex = null;

        public PrintLogRunnable(IDebug debug, long initTime, int testDurationMS, TimeService accelerateClock) {
            this.debug = debug;
            this.initTime = initTime;
            this.testDuration = testDurationMS;
            this.accelerateClock = accelerateClock;
            this.dateFormat = new SimpleDateFormat("-MM.dd.yyyy-HH.mm");
            this.dateFormatWithMs = new SimpleDateFormat("-MM.dd.yyyy-HH.mm-ss-SSS");
        }

        public void run() {
            try {
                while (System.currentTimeMillis() - initTime < testDuration) {

                    String dateInString = dateFormat.format(new Date(accelerateClock.now()));
                    String dateInStringWithMs = dateFormatWithMs.format(new Date(accelerateClock.now()));
                    logDatesHistory.add(dateInString);

                    debug.message("Fake date = " + dateInStringWithMs, null);
                }
            } catch (Exception e) {
                this.ex = e;
            }
        }

        public Set<String> getLogDatesHistory() {
            return logDatesHistory;
        }
    }

}
