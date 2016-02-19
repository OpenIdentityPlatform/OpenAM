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
 * $Id: SecureFileHandler.java,v 1.12 2009/07/27 22:29:42 hvijay Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
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
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import com.iplanet.log.NullLocationException;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.SystemTimer;
import com.sun.identity.log.LogConstants;
import com.sun.identity.log.LogManagerUtil;
import com.sun.identity.log.LogQuery;
import com.sun.identity.log.LogReader;
import com.sun.identity.log.Logger;
import com.sun.identity.log.secure.LogSign;
import com.sun.identity.log.secure.LogVerifier;
import com.sun.identity.log.secure.SecureLogHelper;
import com.sun.identity.log.secure.VerifierList;
import com.sun.identity.log.spi.Archiver;
import com.sun.identity.log.spi.Authorizer;
import com.sun.identity.log.spi.Debug;
import com.sun.identity.log.spi.Token;
import com.sun.identity.monitoring.Agent;
import com.sun.identity.monitoring.MonitoringUtil;
import com.sun.identity.monitoring.SsoServerLoggingHdlrEntryImpl;
import com.sun.identity.monitoring.SsoServerLoggingSvcImpl;
import com.sun.identity.security.AdminPasswordAction;
import com.sun.identity.security.keystore.AMPassword;
/**
 * This <tt> SecureFileHandler </tt> is very much similar to the
 * <t> java.util.logging.FileHandler </tt>. <p> The <TT> FileHandler </TT>
 * can either write to a specified file, or it can write to a rotating set
 * of files. <P>
 * For a archiving set of files, as each file reaches the limit
 * (<i> LogConstants.MAX_FILE_SIZE</I>), it is closed, archived, and a
 * new file opened. Successively older files and named by adding 
 * a ".ddMMyyyyHHmmss" to the base filename. 
 * The Locking mechanism is much more relaxed(in JDK's
 * FileHandler an exclusive lock is created on the file till the handler is
 * closed which makes reading impossible)
 * @author rk133022
 * @version 6.0
 */
public class SecureFileHandler extends java.util.logging.Handler {
    
    private LogManager lmanager = LogManagerUtil.getLogManager();
    private static String PREFIX = "_secure.";
    private OutputStream output;
    private Writer writer;
    private MeteredStream meteredStream;
    private static Hashtable archiverTable = new Hashtable();
    private static Map currentFileList = new HashMap();
    private static Hashtable helperTable = new Hashtable();
    private static boolean verificationInitialized = false;
    private boolean headerWritten;
    private int maxFileSize;
    private String location;
    private String logName;
    private int filesPerKeyStore;
    private String archiverClass;
    
    private SignTask signTask = null;
    private long signInterval = 0;
    private static AMPassword logPassword = null;
    private static AMPassword verPassword = null;
    private SecureLogHelper helper = null;
    private LogVerifier lv = null;
    private SsoServerLoggingSvcImpl logServiceImplForMonitoring = null;
    private SsoServerLoggingHdlrEntryImpl sfLogHandlerForMonitoring = null;
    private static String token = null;
    
    static {
        String logPass= (String)
              AccessController.doPrivileged(
                  new AdminPasswordAction());
        AMPassword passwd = new AMPassword(logPass.toCharArray());
        setLogPassword(passwd, new Object());
        initializeVerifier(passwd, new Object());
        setTokenName(null);
    }
    
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
            written ++;
        }
        /**
         * Writes the array of bytes to the output stream and increments 
         * the number of bytes written by the size of the array.
         * @param b the byte array to be written. 
         * @throws IOException if it fails to write out.
         */
        public void write(byte [] b) throws IOException {
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
        public void write(byte [] b, int offset, int length)
        throws java.io.IOException {
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
     *
     */
    private void setOutputStream(OutputStream out) throws SecurityException,
    UnsupportedEncodingException {
        if (out == null) {
            if (Debug.warningEnabled()) {
                Debug.warning(logName+":SecureFileHandler: " +
                    "OutputStream is null");
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
            } catch (UnsupportedEncodingException ex) {
                Debug.error(logName+":SecureFileHandler: " +
                    "Unsupported Encoding", ex);
                throw new UnsupportedEncodingException(ex.getMessage());
            }
        }
    }
    
    /**
     * Set (or change) the character encoding used by this <tt>Handler</tt>.
     * The encoding should be set before any <tt>LogRecords</tt> are written
     * to the <tt>Handler</tt>.
     *
     * @param encoding  The name of a supported character encoding.
     *              May be null, to indicate the default platform encoding.
     * @exception  SecurityException  if a security manager exists and if
     *             the caller does not have <tt>LoggingPermission("control")
     *             </tt>.
     * @exception  UnsupportedEncodingException if the named encoding is
     *             not supported.
     */
    public void setEncoding(String encoding) throws SecurityException,
    UnsupportedEncodingException {
        super.setEncoding(encoding);
        if (output == null) {
            return;
        }
        // Replace the current writer with a writer for the new encoding.
        flush();
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
        String Interval = 
            lmanager.getProperty(LogConstants.LOGSIGN_PERIODINSECONDS);
        if ((Interval == null) || (Interval.length() == 0)) {
            signInterval = LogConstants.LOGSIGN_PERIODINSECONDS_DEFAULT * 1000;
        } else {
            signInterval = Long.parseLong(Interval) * 1000;
        }

        String strMaxFileSize = 
            lmanager.getProperty(LogConstants.MAX_FILE_SIZE);

        if ((strMaxFileSize == null) || (strMaxFileSize.length() == 0)) {
            maxFileSize = 0;
        } else {
            maxFileSize = Integer.parseInt(strMaxFileSize);
        }

        location = lmanager.getProperty(LogConstants.LOG_PROP_PREFIX + "." +
        logName + ".location");
        if (location == null) {
            location = lmanager.getProperty(LogConstants.LOG_LOCATION);
        }
        if ((location == null) || (location.length() == 0)) {
            throw new NullLocationException("Location Not Specified"); 
        }
        if (!location.endsWith(File.separator)) {
            location += File.separator;
        }
        String filesPerKeyStoreString = 
            lmanager.getProperty(LogConstants.FILES_PER_KEYSTORE);
        if ((filesPerKeyStoreString == null) || 
            (filesPerKeyStoreString.length() == 0)) {
            if (Debug.warningEnabled()) {
                Debug.warning(logName+":Archiver: could not get the files " +
                    "per keystore str setting it to 1");
            }
            filesPerKeyStoreString = "5";
        }
        filesPerKeyStore = Integer.parseInt(filesPerKeyStoreString);
        if (Debug.messageEnabled()) {
            Debug.message(logName+":Files per Key Store = " 
                + filesPerKeyStoreString);
        }
        String archiverClassString = 
            lmanager.getProperty(LogConstants.ARCHIVER);
        if((archiverClassString == null) || 
           (archiverClassString.length() == 0)) {
            throw new NullLocationException("Archvier class not specified");
        }
        archiverClass = archiverClassString;
    }
    
    private void openFiles(String fileName) throws IOException {
        open(new File(fileName), true);
    }
    
    /** 
     * Algorithm: Check how many bytes have already been written to to that file
     * Get an instance of MeteredStream and assign it as the output stream. 
     * Create a file of the name of FileOutputStream.
     */
    private void open(File fileName, boolean append) throws IOException {
        int len = 0;
        len = (int)fileName.length();
        FileOutputStream fout = new FileOutputStream(fileName.toString(), true);
        
        BufferedOutputStream bout = new BufferedOutputStream(fout);
        meteredStream = new MeteredStream(bout, len);
        setOutputStream(meteredStream);
        checkForHeaderWritten(fileName.toString());
    }
    
    /**
     * Creates a new SecureFileHandler. It takes a string parameter which
     * represents file name. When this constructor is called a new file to be
     * created. Assuming that the fileName logger provides is the timestamped
     * fileName.
     * @param fileName The filename associate with file handler.
     */
    public SecureFileHandler(String fileName) {
        if ((fileName == null) || (fileName.length() == 0)) {
            return;
        }
        this.logName = fileName;
        try {
            configure();
        } catch (NullLocationException nle) {
            Debug.error(logName +
                ":SecureFileHandler: Location not specified", nle);
        } catch (FormatterInitException fie) {
            Debug.error(logName +
                ":SecureFileHandler: could not instantiate Formatter", fie);
        }
        fileName = location + PREFIX + fileName;
        Logger logger = (Logger)Logger.getLogger(logName);
        if (logger.getLevel() != Level.OFF) {
                try {
                openFiles(fileName);
            } catch (IOException ioe) {
                Debug.error(logName +
                     ":SecureFileHandler: Unable to open Files", ioe);
            }
        
            logger.setCurrentFile(PREFIX + logName);
            initializeSecurity();
        }

        if(MonitoringUtil.isRunning()){
    	    logServiceImplForMonitoring =
                Agent.getLoggingSvcMBean();
            sfLogHandlerForMonitoring = 
		logServiceImplForMonitoring.getHandler(
                    SsoServerLoggingSvcImpl.SECURE_FILE_HANDLER_NAME);
        }
        
    }
    
    /**
     * Flush any buffered messages.
     */
    public void flush() {
        if (writer != null) {
            try {
                writer.flush();
            } catch (Exception ex) {
                Debug.error(logName+":SecureFileHandler: " +
                                "Could not Flush Output", ex);
            }
        }
    }
    
    /**
     * Flush any buffered messages and Close all the files.
     */
    public void close() {
        if (Debug.messageEnabled()) {
            Debug.message(logName+":SecureFileHandler: close() called");
        }
        flush();
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException ioe) {
            Debug.error(logName +
                ":SecureFileHandler: Could not close writer", ioe);
        }
        if(signTask != null) {
            stopPeriodicLogSigner();
            if (Debug.messageEnabled()) {
                Debug.message(logName+":Stopped Log Signer");
            }
        }
        if(lv != null) {
            lv.stopLogVerifier();
            if (Debug.messageEnabled()) {
                Debug.message(logName+":Stopped Log Verifier");
            }
        }
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
    public synchronized void publish(LogRecord lrecord) {
        if (MonitoringUtil.isRunning() && sfLogHandlerForMonitoring != null) {
            sfLogHandlerForMonitoring.incHandlerRequestCount(1);
        }
        if (writer == null) {
            Debug.warning(logName+":SecureFileHandler: Writer is null");
            return;
        }
        if (!isLoggable(lrecord)) {
            return;
        }
        String message = "";
        message = getFormatter().format(lrecord);
        try {
            if (!headerWritten) {
                writer.write(getFormatter().getHead(this));
                headerWritten = true;
            }
            writer.write(message);
            if (MonitoringUtil.isRunning() && sfLogHandlerForMonitoring != null) {
                sfLogHandlerForMonitoring.incHandlerSuccessCount(1);
            }
        } catch (IOException ex) {
            Debug.error(logName +
                ":SecureFileHandler: could not write to file", ex);
            if (MonitoringUtil.isRunning() && sfLogHandlerForMonitoring != null) {
                sfLogHandlerForMonitoring.incHandlerDroppedCount(1);
            }
        }
        flush();
        // This flag is set only when the Verification is on and at that time
        // the last line for the logger is not set for the duration of the 
        // verification.
        // This is because the verifier will read the set of current records and
        // the logger will keep logging. So the last line for the verifier will
        // not be the last line for the logger which will have written more
        // records in the time the verifier finishes verifying.
        if(lv.getVerificationFlag() == false) {
            helper.setLastLineforLogger(true);
        }
        if (Debug.messageEnabled()) {
                Debug.message(logName+":Check for file size = "
                + maxFileSize+" with size written = " 
                + meteredStream.written);
        }
        if ((message.length() > 0 ) && (meteredStream.written >= maxFileSize)) {
            if (Debug.messageEnabled()) {
                Debug.message("SecureFileHandler: FileFull Event reached");
            }
            archive();
        }
    }
    
    /**
     * This method takes in a logger name and returns a archiver object
     * corresponding to that logger name. This can also be implemented by
     * keeping the archiver a private instance variable and a getter and
     * setter methods to get/set that private variable.
     * 
     * @param logName a logger name associate with archiver object
     * @return a archiver object corresponding to that logger name
     */
    public static Archiver getArchiver(String logName) {
        if ((logName == null) || (logName.length() == 0)) {
            Debug.warning(logName +
                ":Logger: could not get archiver, null logName");
            return null;
        }
        return ((Archiver)archiverTable.get(logName));
    }
    
    /**
     * This method is to set the archiver corresponding to a  loggerName
     * @param logName the loggerName corresponding to a archiver
     * @param archiver the archiver corresponding to a loggerName
     */
    public static void setArchiver(String logName, Archiver archiver) {
        if ((logName == null) || (logName.length() == 0)) {
            if (Debug.warningEnabled()) {
                Debug.warning(logName +
                    ":Logger: Could not set archiver, null logName");
            }
            return;
        }
        archiverTable.put(logName, archiver);
    }
    
    /**
     * This method does the following in sequence:
     * 1: get the signature for the fileName
     * 2: create a dummy logrecord with the signature field and the info to lr.
     * 3: get the formatted string from the formatter..
     * 4: write the signature to the file....
     * 5: archive the file(append timestamp and keep it away) n open new file
     * 6: write the headers in the new file created and also the previous
     * signature...
     */
    private void archive() {
        Archiver archiver = getArchiver(logName);
        String message = "";
        String signature = "Signature";
        
        try {
            LogSign ls = new LogSign(logName);
            signature = ls.sign();
        } catch (Exception e) {
            Debug.error(logName +
                ":SecureFileHandler: could not generate signature");
        }
        /*
         * periodic signer is creating log record at Level.SEVERE, so
         * do it here, too.
         */
        com.sun.identity.log.LogRecord lr =
        new com.sun.identity.log.LogRecord(Level.SEVERE, "Signature");

        lr.setLoggerName(logName);
        lr.addLogInfo(LogConstants.SIGNATURE_FIELDNAME, signature);
        message = getFormatter().format(lr);
        try {
            writer.write(message);
        } catch (IOException ioe) {
            Debug.error(logName +
                ":SecureLogHelper: could not write signature to file", ioe);
        }
        flush();
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException ioe) {
            Debug.error(logName +
                ":SecureFileHandler: Couldnot close writer", ioe);
        }
        try {
            archiver.archive(logName, location);
            int fileCount = archiver.checkCount();
            if (Debug.messageEnabled()) {
                Debug.message(logName+":Files per keystore=" 
                + filesPerKeyStore +" and current file count = "
                + fileCount);
            }
            if(fileCount >= filesPerKeyStore) {
                Debug.message(logName+":Keystore limit reached");
                archiver.archiveKeyStore(logName, location);
                Debug.message(logName +
                    ":FilesPerKeystore counter = " + archiver.checkCount());
                initializeKeyStore();
            }
        } catch(Exception ioe) {
            Debug.error(logName +
                ":SecureFileHandler: Could not archive file", ioe);
        }
        try {
            open(new File(location + PREFIX + logName), false);
            writer.write(getFormatter().getHead(this));
            headerWritten = true;
            int fileCount = archiver.checkCount();
            if(fileCount != 0)
                writer.write(message);
        } catch (IOException ex) {
            Debug.error(logName +
                ":SecureFileHandler: could not write to file", ex);
        }
        flush();
    }
    
    private void checkForHeaderWritten(String fileName) {
        byte [] bytes = new byte[1024];
        FileInputStream fins = null;
        try {
            fins = new FileInputStream(new File(fileName));
            fins.read(bytes);
        } catch (IOException ioe) {
            Debug.error(logName +
                ":SecureFileHandler: could not read file content", ioe);
        } finally {
            if(fins != null){
                try{
                    fins.close();
                } catch(IOException ex) {
                Debug.error(logName +
                    ":SecureFileHandler: IOException while closing file", ex);
                }
            }
        }

        String fileContent = new String(bytes);
        fileContent = fileContent.trim();
        if (fileContent.startsWith("#Version")) {
            headerWritten = true;
        }
    }
    
    /**
     * Set logger password
     * @param logPass the logger password
     * @param token AM token
     */
    public static void setLogPassword(AMPassword logPass, Object token) {
        if (Authorizer.isAuthorized(token)) {
            logPassword = logPass;
        }
    }
    
    /**
     * Sets verifier password.
     *
     * @param verPass the verifier password.
     * @param token AM token
     */
    public static void setVerPassword(AMPassword verPass, Object token) {
        if (Authorizer.isAuthorized(token)) {
            verPassword = verPass;
        }
    }
    
    /**
     * Returns logger password
     * @return logger password
     */
    static AMPassword getLogPassword(){
        return logPassword;
    }
    
    /**
     * Returns verifier password
     * @return verifier password
     */
    static AMPassword getVerPassword(){
        return verPassword;
    }

    /**
     * Returns SecureLogHelper instance
     * @return SecureLogHelper instance
     */
    public static SecureLogHelper getSecureLogHelperInst() {
            SecureLogHelper helper = null;
        String helperClass = LogManagerUtil.getLogManager().
                                   getProperty(LogConstants.SECURE_LOG_HELPER);
        if (Debug.messageEnabled()) {
            Debug.message("Configured SecureLogHelper Impl class : " + 
                                  helperClass);
        }
        
        try {
            helper = (SecureLogHelper) Class.forName(helperClass).newInstance();
        } catch (Exception e) {
            Debug.error("Could not instantiate class " + helperClass, e);
        }
        
        return helper;
    }
    
    /**
     * This method takes in a logger name and returns a helper object
     * corresponding to that logger name. This can also be implemented by
     * keeping the helper a private instance variable and a getter and
     * setter methods to get/set that private variable.
     * @param logName the logger name associate with helper object.
     * @return a helper object corresponding to that logger name.
     */
    public static SecureLogHelper getSecureLogHelper(String logName) {
        if ((logName == null) || (logName.length() == 0)) {
            Debug.warning(logName +
                ":Logger: could not get SecureLogHelper , null logName");
            return null;
        }
        
        return (SecureLogHelper)helperTable.get(logName);
    }
    
    /**
     * This method is to set the helper corresponding to a  loggerName
     */
    private static void setSecureLogHelper(String logName, 
                                           SecureLogHelper helper) {
        if ((logName == null) || (logName.length() == 0)) {
            Debug.warning(logName +
                ":Logger: Could not set SecureLogHelper , null logName");
            return;
        }
        helperTable.put(logName, helper);
    }
    
    void initializeSecurity(){
        String currentFileName = logName;
        try {
            String logPath = lmanager.getProperty(LogConstants.LOG_LOCATION);
            if(!logPath.endsWith("/"))
                logPath += "/";
            String FileName = currentFileName;
            String loggerFileName = logPath + PREFIX + "log." + FileName;
            String verifierFileName = logPath + PREFIX + "ver." + FileName;
            helper = (SecureLogHelper)getSecureLogHelper(logName);
            if(helper == null) {
                helper = getSecureLogHelperInst();
                setSecureLogHelper(logName, helper);
            }
            helper.initializeSecureLogHelper(loggerFileName, logPassword, 
                                             verifierFileName, logPassword);
            if(verificationInitialized) {
                helper.initializeVerifier(verifierFileName, 
                                          logPassword, verPassword);
            }
        } catch (Exception e) {
            Debug.error(logName +
                ":Logger: exception thrown while initializing secure logger",
                e);
            //throw custom defined exception
        }
        Archiver archiver = null;
        try {
            if(getArchiver(logName) == null) {
                archiver = 
                    (Archiver) Class.forName(archiverClass).newInstance();
                setArchiver(logName, archiver);
            }
        } catch (Exception e) {
            Debug.error(logName +
                ":SecureFileHandler: Could Not set Archiver", e);
        }
        
        String Interval = 
            lmanager.getProperty(LogConstants.LOGSIGN_PERIODINSECONDS);
        if ((Interval == null) || (Interval.length() == 0)) {
            signInterval = LogConstants.LOGSIGN_PERIODINSECONDS_DEFAULT * 1000;
        } else {
            signInterval = Long.parseLong(Interval) * 1000;
        }
        startPeriodicLogSigner();
        // Uncomment lines before deploying
        if(verificationInitialized) {
            startVerifierThread();
        }
        Debug.message(logName+":Done initializeSecurity in Handler");
        
        // add the non archived files in the current file list.
        VerifierList vl = new VerifierList();
        String path = location;
        if(!path.endsWith("/")) {
            path += "/";
        }
        TreeMap tm = vl.getKeysAndFiles(new File(path), logName);
        Vector logFiles = (Vector)tm.get(PREFIX + "log." + logName);
        for(int j = 1; j < logFiles.size(); j++) {
            // fiels are sorted according to the timestamp so first add all the 
            // files ending with timestamp
            String name = (String)logFiles.elementAt(j);
            name = name.substring(PREFIX.length(), name.length());
            addToCurrentFileList(name, name, logName);
            if (archiver != null) {
                // increment filesPerKeystoreCounter
                archiver.incrementCount();
            }
        }
        // now add the current file (without time stamp)
        String name = (String)logFiles.elementAt(0);
        name = name.substring(PREFIX.length(), name.length());
        addToCurrentFileList(name, name, logName);
    }
    
    /**
     * Initialize SecureLog verifier
     * @param verPass verifier password
     * @param token AM token
     */
    public static void initializeVerifier(AMPassword verPass, Object token) {
        /*  Remove the relevant verifier initialization code when deploying
         *  it finally. For the timebeing it will be done in the old way
         *  of doing it in the constructor thru initializeSecurity();
         */
        try{
            setVerPassword(verPass, token);
            LogManager lmanager = LogManagerUtil.getLogManager();
            String logPath = lmanager.getProperty(LogConstants.LOG_LOCATION);
            if(!logPath.endsWith("/"))
                logPath += "/";
            LogManager manager = LogManagerUtil.getLogManager();
            Enumeration e = manager.getLoggerNames();
            while(e.hasMoreElements()) {
                String FileName = (String)e.nextElement();
                String verifierFileName = logPath + PREFIX + "ver." + FileName;
                SecureLogHelper helper = getSecureLogHelper(FileName);
                AMPassword logPassword = getLogPassword();
                // This check takes care of the the situation where
                // this code is called at a time when no loggers
                // are in place. Necessary because there is no
                // explicit auditor role in DSAME to initialize ver.
                if (helper != null) {
                    helper.initializeVerifier(verifierFileName, 
                            logPassword, verPassword);
                    try {
                        Debug.message(FileName +
                            ":Trying to start the Verifier Thread");
                        Logger logger = (com.sun.identity.log.Logger)Logger.
                                        getLogger(FileName);
                        Handler[] handlers = logger.getHandlers();
                        ((com.sun.identity.log.handlers.SecureFileHandler)
                                handlers[0]).startVerifierThread();
                        Debug.message(FileName+":Started Log Verifier thread");
                    } catch (Exception ex) {
                        Debug.error(FileName +
                            ":Unable to start Verifier Thread", ex);
                        // throw custom exception
                    }
                }
                verificationInitialized = true;
            }
        }catch(Exception e){
            Debug.error("Error initializing Verification",e);
        }
    }
    
    LogVerifier getVerifier() {
        return lv;
    }
    
    void startVerifierThread() {
        try {
            if (Debug.messageEnabled()) {
                Debug.message(logName+":Trying to start the Verifier Thread");
            }
            lv = new LogVerifier(logName, verPassword);
            lv.startLogVerifier();
            if (Debug.messageEnabled()) {
                Debug.message(logName+":Started Log Verifier thread");
            }
        } catch (Exception e) {
            Debug.error(logName+":Unable to start Verifier Thread", e);
        }
    }
    
    /**
     * Add new file name to the file list
     * @param oldFileName old file name already exist.
     * @param newFileName new file name needs to be added to file list.
     * @param logName logger name for this file list.
     */
    public static void addToCurrentFileList(
        String oldFileName, 
        String newFileName, 
        String logName) {
        ArrayList fileList = (ArrayList)currentFileList.get(PREFIX + logName);
        if(fileList == null) {
            fileList = new ArrayList();
        }
        currentFileList.remove(PREFIX + logName);
        fileList.remove(PREFIX + oldFileName);
        fileList.add(PREFIX + newFileName);
        if(!oldFileName.equals(newFileName)) {
            fileList.add(PREFIX + oldFileName);
        }
        currentFileList.put(PREFIX + logName, fileList);
    }

    /**
     * Return the current file list for the logger.
     * @param logName Associated logger name for this file list.
     * @return the current file list for the logger.
     */
    public static ArrayList getCurrentFileList(String logName) {
        return ((ArrayList)currentFileList.get(PREFIX + logName));
    }
    
    /**
     * Reset the current file list for the logger.
     * @param logName logName Associated logger name for this file list.
     */
    public static void resetCurrentFileList(String logName) {
        currentFileList.remove(PREFIX + logName);
    }
    
    /**
     * Initialize logger key store 
     */
    public void initializeKeyStore(){
        try{
            Logger logger = 
                   (com.sun.identity.log.Logger)Logger.getLogger(logName);
            resetCurrentFileList(logName);
            addToCurrentFileList(logName, logName, logName);
            String logPath = lmanager.getProperty(LogConstants.LOG_LOCATION);
            if(!logPath.endsWith("/"))
                logPath += "/";
            String fileName = logName;
            String loggerFileName = logPath + PREFIX + "log." + fileName;
            String verifierFileName = logPath + PREFIX + "ver." +fileName;
            Debug.message(logName+":Logger Keystore name = " + loggerFileName);
            Debug.message(logName +
                ":Verifier Keystore name= " + verifierFileName);
            helper.initializeSecureLogHelper(loggerFileName, logPassword, 
                                             verifierFileName, logPassword);
            Debug.message(logName+":Initialized SecureLogHelper");
            helper.initializeVerifier(verifierFileName, 
                                      logPassword, verPassword);
            Debug.message(logName+":Done init of SecureLogHelper and Verifier");
        } catch (Exception e) {
            Debug.error(logName +
                ":Logger: exception thrown while initializing secure logger",e);
        }
    }
    
    /**
     *  Sets the token name to be used to initialize the SecureLogHelper object.
     *  @param tokenName the token name associate with logger key store
     */
    public static void setTokenName(String tokenName) {
        if(tokenName != null) {
            token = tokenName;
        }
    }

    /**
     *  Returns the token name used in the SecureLogHelper object.
     */
    public static String getTokenName() {
        return token;
    }

    /**
     * Set logger key name in associated keystore
     * @param name the name of key in the key store
     */
    public static void setLoggerKeyName(String name) {
        SecureLogHelper.setLoggerKeyName(name);
    }

    /**
     * Return logger key name in associated key store
     * @return logger key name in associated key store
     */
    public static String getLoggerKeyName() {
        return SecureLogHelper.getLoggerKeyName();
    }
    
    /**
     * Return secure logger helper instance
     * @return secure logger helper instance
     */
    public SecureLogHelper getSecureLogHelper() {
        return helper;
    }
    
    /**
     *  Starts the LogSign as a separate thread and also sets the time interval
     *  with which it will run and also the time interval after which it will
     *  start for the first time. It checks if the last entry is a signature
     *  and applies the signature to the log if the last entry was not another
     *  signature.
     */
    void startPeriodicLogSigner() {
        if (signTask == null){
            signTask = new SignTask(signInterval);
            SystemTimer.getTimer().schedule(signTask, new Date(((
                    System.currentTimeMillis() + signInterval) / 1000) * 1000));
        }
    }
    
    /**
     *  Stops the log signing thread if it is running.
     */
    void stopPeriodicLogSigner(){
        if(signTask != null) {
            Debug.message(logName+"Sign Thread being stopped");
            signTask.cancel();
            signTask = null;
        }
    }
    
    /**
     *  Inner class which extends the abstract TimerTask class and impelements
     *  the run method which is run periodically which does the actual signing.
     */
    class SignTask extends GeneralTaskRunnable {
        
        private long runPeriod;
        
        public SignTask(long runPeriod) {
            this.runPeriod = runPeriod;
        }
        
        /**
         *  Runs the log signing method and generates the sign and writes 
         *  the sign to the log.
         *  If the earlier entry is a sign then it refrains from signing again.
         */
        public void run(){
            Logger logger = 
                (com.sun.identity.log.Logger)Logger.getLogger(logName);
            try {
                Logger.rwLock.readRequest();
                synchronized(logger) {
                    try {
                        String[][] result = LogReader.read(PREFIX + logName, 
                                        new LogQuery(1), 
                                        Token.createToken("Auditor", 
                                        new String(logPassword.getChars())));
                        if (!((result == null) || (result.length == 0))) {
                            LogSign logSign = new LogSign(logName);
                            int signPos=-1;
                            String signFieldName = 
                                        LogConstants.SIGNATURE_FIELDNAME;
                            for(int j = 0; j < result[0].length; j++){
                                if(result[0][j].equalsIgnoreCase(signFieldName)
                                ) {
                                    signPos = j;
                                    break;
                                }
                            }
                            if (signPos == -1) {
                                Debug.error("Could not locate sign header");
                                return;
                            }
                            // If last record was also a signature then don't 
                            // generate a signature.
                            if((result.length > 1) && 
                               (result[1][signPos].trim().equals("-"))) {
                                String signature = logSign.sign();
                                if(!((signature == null) || 
                                      signature.equals(""))){
                                    com.sun.identity.log.LogRecord lr =
                                    new com.sun.identity.log.LogRecord(
                                            Level.SEVERE, "Signature");
                                    ((com.sun.identity.log.LogRecord)lr).
                                    addLogInfo(LogConstants.SIGNATURE_FIELDNAME,
                                        signature);
                                    publish(lr);
                                } else {
                                    Debug.warning(logName+"Signature is Null");
                                }
                            } else {
                                Debug.message(logName +
                                    ": Read returned only header or last " +
                                    "record was a signature ");
                            }
                        } else {
                            Debug.message(
                                logName + ": Read returned null records");
                        }
                    }catch (Exception e) {
                        Debug.error(logName+":Error Writing Signature", e);
                    }
                }// End of synchronized Logger
            } finally {
                Logger.rwLock.readDone();
            }
        } // end of run()
        
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
        
    }// end of class SignTask
}

