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
 * Copyright 2013 Cybernetica AS
 * Portions copyright 2014-2016 ForgeRock AS.
 */
package org.forgerock.openam.log.handlers.syslog;

import static org.forgerock.openam.utils.Time.*;

import com.iplanet.am.util.ThreadPoolException;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.SystemTimer;
import com.sun.identity.log.LogConstants;
import com.sun.identity.log.LogManagerUtil;
import com.sun.identity.log.handlers.LoggingThread;
import com.sun.identity.log.spi.Debug;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

/**
 * The SyslogHandler publishes log records using default formatter (SyslogFormatter) and sends these to a syslog daemon
 * using SyslogPublisher. The publisher is initialized once, during initialization of SyslogHandler object and flushed
 * after each write.
 */
public class SyslogHandler extends Handler {

    private final LogManager logManager;
    private final SyslogPublisher publisher;
    private final int bufferSize;
    private final Object LOG_FLUSH_LOCK = new Object();
    private final List<String> logRecords = new ArrayList<String>();
    private TimeBufferingTask bufferingTask;

    /**
     * Initialize SyslogHandler for logger "name".
     *
     * @param name is ignored.
     */
    public SyslogHandler(final String name) {
        logManager = LogManagerUtil.getLogManager();

        String protocol = logManager.getProperty(LogConstants.SYSLOG_PROTOCOL);
        boolean useUDP;
        if ("UDP".equals(protocol)) {
            useUDP = true;
        } else if ("TCP".equals(protocol)) {
            useUDP = false;
        } else {
            Debug.error("Invalid syslog protocol " + protocol + ", defaulting to UDP");
            useUDP = true;
        }

        String host = logManager.getProperty(LogConstants.SYSLOG_HOST);
        int port = Integer.valueOf(logManager.getProperty(LogConstants.SYSLOG_PORT));
        Debug.message("Starting syslogging to " + host + ":" + port + " over " + protocol);

        InetSocketAddress socketAddress = new InetSocketAddress(host, port);
        if (useUDP) {
            publisher = new SyslogUdpPublisher(socketAddress);
        } else {
            publisher = new SyslogTcpPublisher(socketAddress);
        }

        String bufferSizeStr = logManager.getProperty(LogConstants.BUFFER_SIZE);
        int size;
        if (bufferSizeStr != null && !bufferSizeStr.isEmpty()) {
            try {
                size = Integer.parseInt(bufferSizeStr);
            } catch (NumberFormatException e) {
                size = 1;
            }
        } else {
            size = 1;
        }
        this.bufferSize = size;
        if ("ON".equalsIgnoreCase(logManager.getProperty(LogConstants.TIME_BUFFERING_STATUS))) {
            startTimeBufferingThread();
        }
        Debug.message("Initialized syslog handler for " + name);
    }

    /**
     * Flushes the buffered logrecords.
     */
    @Override
    public void flush() {
        FlushTask task;
        synchronized (logRecords) {
            task = new FlushTask(new ArrayList<String>(logRecords));
            logRecords.clear();
        }
        task.run();
    }

    /**
     * Flushes the buffered logrecords in a separate thread to prevent blocking.
     * Access to logRecords is guarded by the publish method's synchronized block.
     */
    private void nonBlockingFlush() {
        FlushTask task = new FlushTask(new ArrayList<String>(logRecords));
        logRecords.clear();
        try {
            LoggingThread.getInstance().run(task);
        } catch (ThreadPoolException ex) {
            //Use current thread to complete the task if ThreadPool can not execute it.
            Debug.warning("SyslogHandler.nonBlockingFlush(): ThreadPoolException. Performing blocking flush.");
            task.run();
        }
    }

    /**
     * Flushes the buffered logrecords and closes the connections established by {@link SyslogPublisher}.
     */
    @Override
    public void close() {
        flush();
        try {
            publisher.closeConnection();
        } catch (IOException ex) {
            Debug.error("IOException during syslog socket close", ex);
        }
        stopBufferTimer();
    }

    /**
     * Publishes the log record if it is loggable according to current configuration.
     *
     * @param logRecord A LogRecord which is formatted using the configured formatter, converted to byte array and sent
     * to an outputstream managed by this handler.
     */
    @Override
    public void publish(final LogRecord logRecord) {
        if (!isLoggable(logRecord)) {
            return;
        }

        String formatted = getFormatter().format(logRecord);
        synchronized (logRecords) {
            logRecords.add(formatted);
            if (logRecords.size() >= bufferSize) {
                nonBlockingFlush();
            }
        }
    }

    private void startTimeBufferingThread() {
        String period = logManager.getProperty(LogConstants.BUFFER_TIME);
        long interval;
        if (period != null && !period.isEmpty()) {
            interval = Long.parseLong(period);
        } else {
            interval = LogConstants.BUFFER_TIME_DEFAULT;
        }
        interval *= 1000;
        bufferingTask = new TimeBufferingTask(interval);
        try {
            SystemTimer.getTimer().schedule(bufferingTask,
                    new Date(((currentTimeMillis() + interval) / 1000) * 1000));
        } catch (IllegalArgumentException e) {
            Debug.error("SyslogHandler:startTimeBufferingThread: Unable to schedule buffering task"
                    + e.getMessage());
        } catch (IllegalStateException e) {
            if (Debug.messageEnabled()) {
                Debug.message("SyslogHandler:startTimeBufferingThread: Unable to submit buffering task"
                        + e.getMessage());
            }
        }
        if (Debug.messageEnabled()) {
            Debug.message("SyslogHandler: Time Buffering Thread Started");
        }
    }

    private void stopBufferTimer() {
        if (bufferingTask != null) {
            bufferingTask.cancel();
            bufferingTask = null;
            if (Debug.messageEnabled()) {
                Debug.message("SyslogHandler: Time Buffering Thread Stopped");
            }
        }
    }

    private class FlushTask implements Runnable {

        private final List<String> logRecords;

        FlushTask(final List<String> logRecords) {
            this.logRecords = logRecords;
        }

        @Override
        public void run() {
            synchronized (LOG_FLUSH_LOCK) {
                publisher.publishLogRecords(logRecords);
            }
        }
    }

    private class TimeBufferingTask extends GeneralTaskRunnable {

        private final long runPeriod;

        public TimeBufferingTask(final long runPeriod) {
            this.runPeriod = runPeriod;
        }

        @Override
        public void run() {
            if (Debug.messageEnabled()) {
                Debug.message("SyslogHandler:TimeBufferingTask.run() called");
            }
            flush();
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean addElement(final Object obj) {
            return false;
        }

        @Override
        public boolean removeElement(final Object obj) {
            return false;
        }

        @Override
        public long getRunPeriod() {
            return runPeriod;
        }
    }
}
