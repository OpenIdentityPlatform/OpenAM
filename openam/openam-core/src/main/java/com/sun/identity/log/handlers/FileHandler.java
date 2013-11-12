/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: FileHandler.java,v 1.14 2009/07/27 19:50:55 bigfatrat Exp $
 *
 */
/*
 * Portions Copyrighted 2011-2013 ForgeRock Inc
 */
package com.sun.identity.log.handlers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import com.iplanet.am.util.ThreadPoolException;
import com.iplanet.log.NullLocationException;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.SystemTimer;
import com.sun.identity.log.LogConstants;
import com.sun.identity.log.LogManager;
import com.sun.identity.log.LogManagerUtil;
import com.sun.identity.log.Logger;
import com.sun.identity.log.spi.Debug;
import com.sun.identity.monitoring.Agent;
import com.sun.identity.monitoring.MonitoringUtil;
import com.sun.identity.monitoring.SsoServerLoggingHdlrEntryImpl;
import com.sun.identity.monitoring.SsoServerLoggingSvcImpl;
import java.io.FileNotFoundException;
import java.util.Calendar;

/**
 * This <tt> FileHandler </tt> is very much similar to the
 * <t> java.util.logging.FileHandler </tt>. <p> The <TT> FileHandler </TT>
 * can either write to a specified file, or it can write to a rotating set
 * of files. <P>
 * For a rotating set of files, as each file reaches the limit
 * (<i> LogConstants.MAX_FILE_SIZE</I>), it is closed, rotated out, and a
 * new file opened. Successively older files and named by adding "-1", "-2",
 * etc., * to the base filename. The Locking mechanism is much more relaxed 
 * (in JDK's  FileHandler an exclusive lock is created on the file till the
 * handler is closed which makes reading impossible)
 */
public class FileHandler extends java.util.logging.Handler {

    private LogManager lmanager = LogManagerUtil.getLogManager();
    private OutputStream output;
    private Writer writer;
    private MeteredStream meteredStream;
    private File files[];
    private boolean headerWritten;
    private int count; // count represent number of history files
    private int maxFileSize;
    private String location;
    private Formatter formatter;
    private static SimpleDateFormat simpleDateFormat =
            new SimpleDateFormat("HHmmss.ddMMyyyy");
    private Date date;
    private String currentFileName;
    private String fileName;
    private int recCountLimit;
    private LinkedList recordBuffer;
    private TimeBufferingTask bufferTask;
    private boolean timeBufferingEnabled = false;
    private static String headerString = null;
    private SsoServerLoggingSvcImpl logServiceImplForMonitoring = null;
    private SsoServerLoggingHdlrEntryImpl fileLogHandlerForMonitoring = null;

    private int rotationInterval = -1;
    private long lastRotation;
    /**
     * By default the size based rotation is enabled
     */
    private boolean rotatingBySize = true;

    private static final String DEFAULT_LOG_SUFFIX_FORMAT = "-MM.dd.yy-kk.mm";

    private class MeteredStream extends OutputStream {

        OutputStream out;
        int written;

        MeteredStream(OutputStream out, int written) {
            this.out = out;
            this.written = written;
        }

        /**
         * writes a single integer to the outputstream and increments 
         * the number of bytes written by one.
         * @param b integer value to be written.
         * @throws IOException if it fails to write out.
         */
        public void write(int b) throws IOException {
            out.write(b);
            written++;
        }

        /**
         * Writes the array of bytes to the output stream and increments 
         * the number of bytes written by the size of the array.
         * @param b the byte array to be written. 
         * @throws IOException if it fails to write out.
         */
        public void write(byte[] b) throws IOException {
            out.write(b);
            written += b.length;
        }

        /**
         * Writes the array of bytes to the output stream and increments 
         * the number of bytes written by the size of the array.
         * @param b the byte array to be written. 
         * @param offset the offset of array to be written. 
         * @param length the length of bytes to be written. 
         * @throws IOException if it fails to write out.
         */
        public void write(byte[] b, int offset, int length)
                throws IOException {
            out.write(b, offset, length);
            written += length;
        }

        /**
         * Flush any buffered messages.
         * @throws IOException if it fails to write out.
         */
        public void flush() throws IOException {
            out.flush();
        }

        /**
         * close the current output stream.
         * @throws IOException if it fails to close output stream.
         */
        public void close() throws IOException {
            out.close();
        }
    }

    /**
     * sets the output stream to the specified output stream ..picked up from
     * StreamHandler.
     */
    private void setOutputStream(OutputStream out) throws SecurityException,
            UnsupportedEncodingException {
        if (out == null) {
            if (Debug.warningEnabled()) {
                Debug.warning(fileName + ":FileHandler: OutputStream is null");
            }
        }
        output = out;
        headerWritten = false;
        String encoding = getEncoding();
        if (encoding == null) {
            writer = new OutputStreamWriter(output);
        } else {
            try {
                writer = new OutputStreamWriter(output, encoding);
            } catch (UnsupportedEncodingException e) {
                Debug.error(fileName + ":FileHandler: Unsupported Encoding", e);
                throw new UnsupportedEncodingException(e.getMessage());
            }
        }
    }

    /**
     * Set (or change) the character encoding used by this <tt>Handler</tt>.
     * The encoding should be set before any <tt>LogRecords</tt> are written
     * to the <tt>Handler</tt>.
     *
     * @param encoding  The name of a supported character encoding.
     *        May be null, to indicate the default platform encoding.
     * @exception  SecurityException  if a security manager exists and if
     *             the caller does not have
     *             <tt>LoggingPermission("control")</tt>.
     * @exception UnsupportedEncodingException if the named encoding is
     *           not supported.
     */
    public void setEncoding(String encoding) throws SecurityException,
            UnsupportedEncodingException {
        super.setEncoding(encoding);
        if (output == null) {
            return;
        }
        // Replace the current writer with a writer for the new encoding.
        cleanup();
        if (encoding == null) {
            writer = new OutputStreamWriter(output);
        } else {
            writer = new OutputStreamWriter(output, encoding);
        }
    }

    /**
     * This method is used for getting the properties from LogManager
     * and setting the private variables count, maxFileSize etc.
     */
    private void configure()
            throws NullLocationException, FormatterInitException {
        String bufferSize = lmanager.getProperty(LogConstants.BUFFER_SIZE);
        if (bufferSize != null && bufferSize.length() > 0) {
            try {
                recCountLimit = Integer.parseInt(bufferSize);
            } catch (NumberFormatException nfe) {
                Debug.warning(fileName +
                        ":FileHandler: NumberFormatException ", nfe);
                if (Debug.messageEnabled()) {
                    Debug.message(fileName +
                            ":FileHandler: Setting buffer size to 1");
                }
                recCountLimit = 1;
            }
        } else {
            Debug.warning(fileName +
                    ":FileHandler: Invalid buffer size: " + bufferSize);
            if (Debug.messageEnabled()) {
                Debug.message(fileName +
                        ":FileHandler: Setting buffer size to 1");
            }
            recCountLimit = 1;
        }

        String status = lmanager.getProperty(
                LogConstants.TIME_BUFFERING_STATUS);

        if (status != null && status.equalsIgnoreCase("ON")) {
            timeBufferingEnabled = true;
        }

        String strCount = lmanager.getProperty(LogConstants.NUM_HISTORY_FILES);
        if ((strCount == null) || (strCount.length() == 0)) {
            count = 0;
        } else {
            count = Integer.parseInt(strCount);
        }

        String strMaxFileSize = lmanager.getProperty(
                LogConstants.MAX_FILE_SIZE);
        if ((strMaxFileSize == null) || (strMaxFileSize.length() == 0)) {
            maxFileSize = 0;
        } else {
            maxFileSize = Integer.parseInt(strMaxFileSize);
        }
        location = lmanager.getProperty(LogConstants.LOG_LOCATION);
        if ((location == null) || (location.length() == 0)) {
            throw new NullLocationException(
                    "Location Not Specified"); //localize
        }
        if (!location.endsWith(File.separator)) {
            location += File.separator;
        }
        String strFormatter = lmanager.getProperty(LogConstants.ELF_FORMATTER);
        try {
            Class clz = Class.forName(strFormatter);
            formatter = (Formatter) clz.newInstance();
        } catch (Exception e) {
            throw new FormatterInitException(
                    "Unable to initialize Formatter Class" + e);
        }

        String rotation = lmanager.getProperty(LogConstants.LOGFILE_ROTATION);
        try {
            if (rotation != null) {
                rotationInterval = Integer.parseInt(rotation);
            }
        } catch (NumberFormatException nfe) {
            //if we cannot parse it, then we use the size based rotation
            rotationInterval = -1;
        }
        if (rotationInterval > 0) {
            lastRotation = System.currentTimeMillis();
            rotatingBySize = false;
        }
    }

    private void openFiles(String fileName) throws IOException {
        if (rotatingBySize) {
            // make sure that we have a valid maxFileSize
            if (maxFileSize < 0) {
                Debug.error(fileName
                        + ":FileHandler: maxFileSize cannot be negative");
                maxFileSize = 0;
            }
        }
        // make sure that we have a valid history count
        if (count < 0) {
            Debug.error(fileName
                    + ":FileHandler: no. of history files negative " + count);
            count = 0;
        }
        files = new File[count + 1]; // count is the number of history files
        for (int i = 0; i < count + 1; i++) {
                if (i != 0) {
                    files[i] = new File(fileName + "-" + i);
                } else {
                    files[0] = new File(fileName);
                }
            }
            open(files[0], true);
    }

    /** 
     * Algorithm: Check how many bytes have already been written to to that file
     * Get an instance of MeteredStream and assign it as the output stream. 
     * Create a file of the name of FileOutputStream.
     */
    private void open(File fileName, boolean append) throws IOException {
        String filename = fileName.toString();
        int len = 0;
        len = (int) fileName.length();
        FileOutputStream fout = new FileOutputStream(filename, append);

        BufferedOutputStream bout = new BufferedOutputStream(fout);
        meteredStream = new MeteredStream(bout, len);
        setOutputStream(meteredStream);
        checkForHeaderWritten(fileName.toString());
    }

    /**
     * Creates a new FileHandler. It takes a string parameter which represents
     * file name. When this constructor is called a new file to be created.
     * Assuming that the fileName logger provides is the timestamped fileName.
     * @param fileName The filename associate with file handler.
     */
    public FileHandler(String fileName) {
        if ((fileName == null) || (fileName.length() == 0)) {
            return;
        }
        this.fileName = fileName;
        try {
            configure();
        } catch (NullLocationException nle) {
            Debug.error(fileName + ":FileHandler: Location not specified", nle);
        } catch (FormatterInitException fie) {
            Debug.error(fileName +
                    ":FileHandler: could not instantiate Formatter", fie);
        }
        if (!rotatingBySize) {
            fileName = wrapFilename(fileName);
        }
        fileName = location + fileName;
        Logger logger = (Logger) Logger.getLogger(this.fileName);
        if (logger.getLevel() != Level.OFF) {
            try {
                openFiles(fileName);
            } catch (IOException ioe) {
                Debug.error(fileName + ":FileHandler: Unable to open Files",
                        ioe);
            }
        }
        logger.setCurrentFile(this.fileName);

        recordBuffer = new LinkedList();

        if (timeBufferingEnabled) {
            startTimeBufferingThread();
        }

        if (MonitoringUtil.isRunning()) {
            logServiceImplForMonitoring =
                Agent.getLoggingSvcMBean();
            fileLogHandlerForMonitoring =
                logServiceImplForMonitoring.getHandler(
                    SsoServerLoggingSvcImpl.FILE_HANDLER_NAME);
        }
    }

    private String wrapFilename(String fileName) {
        String prefix = lmanager.getProperty(LogConstants.LOGFILE_PREFIX);
        String suffixFormat = lmanager.getProperty(LogConstants.LOGFILE_SUFFIX);

        StringBuilder newFileName = new StringBuilder();

        if (prefix != null) {
            newFileName.append(prefix);
        }

        newFileName.append(fileName);

        SimpleDateFormat suffixDateFormat = null;
        if (suffixFormat != null && suffixFormat.trim().length() > 0) {
            try {
                suffixDateFormat = new SimpleDateFormat(suffixFormat);
            } catch (IllegalArgumentException iae) {
                Debug.error("Date format invalid; " + suffixFormat, iae);
            }

            if (suffixDateFormat != null) {
                newFileName.append(suffixDateFormat.format(new Date()));
            }
        }
        if (rotationInterval > 0 && suffixDateFormat == null) {
            //fallback to a default dateformat, so the logfilenames will differ
            suffixDateFormat = new SimpleDateFormat(DEFAULT_LOG_SUFFIX_FORMAT);
        }

        return newFileName.toString();

    }

    private void cleanup() {
        if (writer != null) {
            try {
                writer.flush();
            } catch (Exception ex) {
                Debug.error(fileName +
                        ":FileHandler: Could not Flush Output", ex);
            }
        }
    }

    /**
     * Flush any buffered messages and Close all the files.
     */
    public void close() {
        flush();
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                Debug.error(fileName + ":FileHandler: Error closing writer", e);
            }
        }
        stopBufferTimer();
    }

    /**
     * Format and publish a LogRecord.
     * <p>
     * This FileHandler is associated with a Formatter, which has to format the
     * LogRecord according to ELF and return back the string formatted as per 
     * ELF. This method first checks if the header is already written to the 
     * file, if not, gets the header from the Formatter and writes it at the 
     * beginning of the file.
     * @param lrecord the log record to be published.
     */
    public void publish(LogRecord lrecord) {
        if (MonitoringUtil.isRunning() && fileLogHandlerForMonitoring != null) {
            fileLogHandlerForMonitoring.incHandlerRequestCount(1);
        }

        if (maxFileSize <= 0) {
            return;
        }
        if (!isLoggable(lrecord)) {
            return;
        }
        Formatter formatter = getFormatter();
        String message = formatter.format(lrecord);
        synchronized (this) {        
            recordBuffer.add(message);
            if (recordBuffer.size() >= recCountLimit) {
                if (Debug.messageEnabled()) {
                    Debug.message(fileName + ":FileHandler.publish(): got " +
                        recordBuffer.size() + " records, writing all");
                }
                nonBlockingFlush();
            }
        }
    }

    private String getHeaderString() {
        if (headerString == null) {
            headerString = getFormatter().getHead(this);
        }
        return headerString;
    }

    /**
     * Flush any buffered messages.
     */
    protected void nonBlockingFlush() {
        LinkedList writeBuffer = null;
        synchronized (this) {
            if (recordBuffer.size() <= 0) {
                if (Debug.messageEnabled()) {
                    Debug.message(fileName +
                        ":FileHandler.flush: no records in buffer to write");
                }
                return;
            }
            writeBuffer = recordBuffer;
            recordBuffer = new LinkedList();
        }
        LogTask task = new LogTask(writeBuffer);
        try {
            // Get an instance as required otherwise it can cause issues on container restart.
            LoggingThread.getInstance().run(task);
        } catch (ThreadPoolException ex) {
            // use current thread to flush the data if ThreadPool is shutdown
            synchronized (this) {
                task.run();
            }
        }
    }

    public void flush() {
        synchronized (this) {
            if (recordBuffer.size() <= 0) {
                return;
            }
            if (writer == null) {
                int recordsToBeDropped = recordBuffer.size();
                if (MonitoringUtil.isRunning() && fileLogHandlerForMonitoring !=
                    null) {
                    fileLogHandlerForMonitoring.incHandlerDroppedCount(
                        recordsToBeDropped);
                }
                recordBuffer.clear();
                return;
            }
            for (Iterator iter = recordBuffer.iterator(); iter.hasNext();) {
                String message = (String) iter.next();
                if (needsRotation(message)) {
                    rotate();
                }
                try {
                    if (!headerWritten) {
                        writer.write(getHeaderString());
                        headerWritten = true;
                    }
                    writer.write(message);
                    if (MonitoringUtil.isRunning() &&
                        fileLogHandlerForMonitoring != null) {
                        fileLogHandlerForMonitoring.incHandlerSuccessCount(1);
                    }
                } catch (IOException ex) {
                }
                cleanup();
            }
            recordBuffer.clear();
        }        
    }

    private boolean needsRotation(String message) {
        if (rotatingBySize) {
            if (message.length() > 0 &&
                    meteredStream.written + message.length() >= maxFileSize) {
                return true;
            }
        } else {
            Calendar now = Calendar.getInstance();
            Calendar then = Calendar.getInstance();
            then.setTimeInMillis(lastRotation);

            then.add(Calendar.MINUTE, rotationInterval);
            if (now.after(then)) {
                return true;
            }
        }
        return false;
    }

    private void rotate() {
       if (writer != null) {
            try {
                writer.flush();
                writer.close();
            } catch (Exception ex) {
                Debug.error(fileName + ":FileHandler: " +
                        "Error closing writer", ex);
            }
        }
        if (rotatingBySize) {
            //
            //  delete file<n>; file<n-1> becomes file<n>; and so on.
            //
            for (int i = count - 1; i >= 0; i--) {
                File f1 = files[i];
                File f2 = files[i + 1];
                if (f1.exists()) {
                    if (f2.exists()) {
                        try {
                            f2.delete();
                        } catch (SecurityException secex) {
                            Debug.error(fileName
                                    + ":FileHandler: could not delete file. msg = "
                                    + secex.getMessage());
                        }
                    }

                    boolean renameSuccess = f1.renameTo(f2);

                    // In case renaming fails, copy the contents of source file
                    // to destination file.
                    if (!renameSuccess) {
                        copyFile(f1.toString(), f2.toString());
                    }
                }
            }
        } else {
            // remember when we last rotated
            lastRotation = System.currentTimeMillis();
            // Delete the oldest file if it exists
            if (files[count].exists()) {
                try {
                    files[count].delete();
                } catch (SecurityException secex) {
                    Debug.error(fileName
                            + ":FileHandler: could not delete file. msg = "
                            + secex.getMessage());
                }
            }
            // Move each file up a slot and then replace 0th one with new file
            for (int i = count - 1; i >= 0; i--) {
                files[i + 1] = files[i];
            }
            // generate a new timestamp based filename
            String wrappedFilename = wrapFilename(this.fileName);
            File newLogFile = new File(location, wrappedFilename);

            if (newLogFile.exists()) {
                Debug.error(newLogFile.getName()
                        + ":FileHandler: could not rotate file. msg = "
                        + "file already exists!");
            } else {
                // swap across to the new file
                files[0] = newLogFile;
            }
        }
        if (Debug.messageEnabled()) {
            Debug.message(fileName
                    + ":FileHandler: rotate to file " + files[0].getName());
        }
        try {
            open(files[0], false);
        } catch (IOException ix) {
            Debug.error(fileName + ":FileHandler: error opening file" + ix);
        }
    }

    private void copyFile(String input, String output) {
        if (Debug.messageEnabled()) {
            Debug.message(fileName + ":FileHandler: CopyFile Method called");
        }
        FileInputStream fis = null;
        FileOutputStream fos = null;

        try {
            //input file
            fis = new FileInputStream(input);
            int s;
            //output file
            fos = new FileOutputStream(output);
            while ((s = fis.read()) > -1) {
                fos.write(s);
            }
        } catch (FileNotFoundException fnfe) {
            Debug.error(fileName + ":FileHandler: copyFile: File not found: ",
                    fnfe);
        } catch (IOException ioex) {
            Debug.error(fileName + ":FileHandler: copyFile: IOException",
                    ioex);
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }

                if (fos != null) {
                    fos.close();
                }
            } catch (IOException ex) {
                Debug.error(fileName + 
                    ":FileHandler: copyFile: IOException while closing file",
                    ex);
            }
        }
    }

    private void checkForHeaderWritten(String fileName) {
        byte[] bytes = new byte[1024];
        FileInputStream fins = null;
        try {
            fins = new FileInputStream(new File(fileName));
            fins.read(bytes);
        } catch (IOException ioe) {
            Debug.error(fileName + ":FileHandler: couldnot read file content",
                    ioe);
        } finally {
            if (fins != null) {
                try {
                    fins.close();
                } catch (IOException ex) {
                    Debug.error(fileName +
                            ":FileHandler: could not close file.", ex);
                }
            }
        }

        String fileContent = new String(bytes);
        fileContent = fileContent.trim();
        if (fileContent.startsWith("#Version")) {
            headerWritten = true;
        } else {
            headerWritten = false;
        }
    }

    private class LogTask implements Runnable {

        private LinkedList buffer;

        public LogTask(LinkedList buffer) {
            this.buffer = buffer;
        }

        public void run() {
            if (writer == null) {
                Debug.error(fileName + ":FileHandler: Writer is null");
                int recordsToBeDropped = buffer.size();
                if (MonitoringUtil.isRunning() && fileLogHandlerForMonitoring !=
                    null) {
                    fileLogHandlerForMonitoring.incHandlerDroppedCount(
                        recordsToBeDropped);
                }
                buffer.clear();
                return;
            }
            if (Debug.messageEnabled()) {
                Debug.message(fileName + ":FileHandler.flush: writing " +
                    "buffered records (" +
                    buffer.size() + " records)");
            }
            for (Iterator iter = buffer.iterator(); iter.hasNext();) {
                String message = (String) iter.next();
                if (needsRotation(message)) {
                    rotate();
                }
                try {
                    if (!headerWritten) {
                        writer.write(getHeaderString());
                        headerWritten = true;
                    }
                    writer.write(message);
                    if (MonitoringUtil.isRunning() &&
                        fileLogHandlerForMonitoring != null) {
                        fileLogHandlerForMonitoring.incHandlerSuccessCount(1);
                    }
                } catch (IOException ex) {
                    Debug.error(fileName +
                        ":FileHandler: could not write to file: ", ex);
                }
                cleanup();
            }
        }

    }

    private class TimeBufferingTask extends GeneralTaskRunnable {

        private long runPeriod;

        public TimeBufferingTask(long runPeriod) {
            this.runPeriod = runPeriod;
        }

        /**
         * The method which implements the GeneralTaskRunnable.
         */
        public void run() {
            if (Debug.messageEnabled()) {
                Debug.message(fileName +
                        ":FileHandler:TimeBufferingTask.run() called");
            }
            nonBlockingFlush();
        }

        /**
         *  Methods that need to be implemented from GeneralTaskRunnable.
         */
        public boolean isEmpty() {
            return true;
        }

        public boolean addElement(Object obj) {
            return false;
        }

        public boolean removeElement(Object obj) {
            return false;
        }

        public long getRunPeriod() {
            return runPeriod;
        }
    }

    private void startTimeBufferingThread() {
        String period = lmanager.getProperty(LogConstants.BUFFER_TIME);
        long interval;
        if ((period != null) || (period.length() != 0)) {
            interval = Long.parseLong(period);
        } else {
            interval = LogConstants.BUFFER_TIME_DEFAULT;
        }
        interval *= 1000;
        if (bufferTask == null) {
            bufferTask = new TimeBufferingTask(interval);
            try {
                SystemTimer.getTimer().schedule(bufferTask, new Date(((System.currentTimeMillis() + interval) / 1000) * 1000));
            } catch (IllegalArgumentException e) {
                Debug.error(fileName + ":FileHandler:BuffTimeArg: " +
                        e.getMessage());
            } catch (IllegalStateException e) {
                if (Debug.messageEnabled()) {
                    Debug.message(fileName + ":FileHandler:BuffTimeState: " +
                            e.getMessage());
                }
            }
            if (Debug.messageEnabled()) {
                Debug.message(fileName +
                        ":FileHandler: Time Buffering Thread Started");
            }
        }
    }

    private void stopBufferTimer() {
        if (bufferTask != null) {
            bufferTask.cancel();
            bufferTask = null;
            if (Debug.messageEnabled()) {
                Debug.message(fileName + ":FileHandler: Buffer Timer Stopped");
            }
        }
    }
}
