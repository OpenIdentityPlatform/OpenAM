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
 * $Id: LogManager.java,v 1.14 2009/12/09 00:34:22 bigfatrat Exp $
 *
 */
/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.log;

import java.io.IOException;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Hashtable;
import java.util.Set;
import java.util.logging.Level;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.logging.Handler;
import java.util.HashSet;
import java.util.logging.Formatter;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.sun.identity.log.messageid.LogMessageProviderBase;
import com.sun.identity.log.messageid.MessageProviderFactory;
import com.sun.identity.log.spi.Debug;
import com.sun.identity.log.s1is.LogConfigReader;
import com.sun.identity.monitoring.Agent;
import com.sun.identity.monitoring.MonitoringUtil;
import com.sun.identity.monitoring.SsoServerLoggingSvcImpl;

/**
 * This class keeps track of all the logger objects and does all the
 * bookkeeping work. It is extended from JDK's <code>LogManager</code> to add
 * functionalities, such as adding our logger listening to DS changes, etc.
 * @supported.all.api
 */
public class LogManager extends java.util.logging.LogManager {

    /**
     * Is the Log Service running locally or remotely
     */
    public static boolean isLocal = false;
    /**
     * The handler which will be added to each logger object
     */
    public static String HANDLER = "Handler";
    /**
     * The formatter which depends on the log settings
     */
    public static String FORMATTER = "Formatter";
    
    public static boolean isMonitoringInit = false;

    /*
     * A list to maintain strong references to all loggers that are added,
     * workaround for the file handle issue in OPENAM-184.
     */
    private static Hashtable loggersTable = new Hashtable();


    /**
     * Indicator for whether the first readConfiguration has happened
     */
    private static boolean didFirstReadConfig;
    private static final String strDEFAULT = "DEFAULT";
    private static String oldLocation = strDEFAULT;
    private static String oldLevel = strDEFAULT;
    private static String oldSecurityStatus = strDEFAULT;
    private static String oldBackend = strDEFAULT;
    private static String oldStatus = strDEFAULT;
    private static String newLocation;
    private static String newLevel;
    private static String newSecurityStatus;
    private static String newBackend;
    private static String newStatus;
    private final int OLDLOCATION = 0;
    private final int NEWLOCATION = 1;
    private final int OLDBACKEND = 2;
    private final int NEWBACKEND = 3;
    private final int OLDSECURITYSTATUS = 4;
    private final int NEWSECURITYSTATUS = 5;
    private final int OLDSTATUS = 6;
    private final int NEWSTATUS = 7;
    private final int OLDLEVEL = 8;
    private final int NEWLEVEL = 9;
    private static SsoServerLoggingSvcImpl logServiceImplForMonitoring = null;
    private static int loggerCount = 0;
    private String inactive = "INACTIVE";

    /**
     * Adds a logger to the Log Manager.
     *
     * @param logger Logger object to be added to the Log Manager.
     * @return true if the logger is added.
     */
    public boolean addLogger(java.util.logging.Logger logger) {
        String name = logger.getName();
        /* we have to pass root logger and global logger */
        if (name != null && name.length() != 0 && !name.equals("global")) {
            /* we have to take care of the resourcebundle logger may have */
            String rbName = logger.getResourceBundleName();
            logger = new Logger(name, rbName);
        }
        boolean addSuccess = super.addLogger(logger);

        // Workaround for OPENAM-184, maintains strong reference to the loggers until this class is collected
        // The Hashtable will never be used to retrieve loggers, only to keep then  strongly referenced
        loggersTable.put(name, logger);

        if(addSuccess){
            Enumeration loggerNames = getLoggerNames();
            int lcnt = 0;
            while (loggerNames.hasMoreElements()) {
                String curEl = (String) loggerNames.nextElement();
                /* avoid root logger */
                if (curEl.length() != 0 && curEl.length() != 0 &&
                    !curEl.equals("global"))
                {
                    lcnt++;
                }
            }
            loggerCount = lcnt;
            if (SystemProperties.isServerMode() && MonitoringUtil.isRunning()) {
                if (logServiceImplForMonitoring == null) {
                    logServiceImplForMonitoring =
                        Agent.getLoggingSvcMBean();
                }
                if (logServiceImplForMonitoring != null) {
                    logServiceImplForMonitoring.setSsoServerLoggingLoggers(
                        new Integer(loggerCount));
                }
            }
        }
        return addSuccess;
    }

    /* security status updated by readConfiguration */
    private boolean securityStatus = false;

    /* all fields read during read configuration */
    private String[] allFields;
    private Set selectedFieldSet;
    protected Level loggingLevel = null;

    /**
     * Return whether secure logging is specified.
     *
     * @return <code>securityStatus</code>
     */
    public final boolean isSecure() {
        return securityStatus;
    }

    /**
     * Return the array of all LogRecord fields available for selection.
     *
     * @return <code>allFields</code>
     */
    public synchronized final String[] getAllFields() {
        return allFields;
    }

    /**
     * Return the LogRecord fields selected to be included.
     *
     * @return <code>selectedFieldSet</code>
     */
    public synchronized final Set getSelectedFieldSet() {
        return selectedFieldSet;
    }

    private synchronized final void readAllFields() {
        String strAllFields = getProperty(LogConstants.ALL_FIELDS);
        StringTokenizer strToken = new StringTokenizer(strAllFields, ", ");
        int count = strToken.countTokens();
        String localAllFields[] = new String[count];
        count = 0;
        while (strToken.hasMoreElements()) {
            localAllFields[count++] = strToken.nextToken().trim();
        }
        allFields = localAllFields;
    }

    private synchronized final void readSelectedFieldSet() {
        HashSet fieldSet = new HashSet();

        String strSelectedFields = getProperty(LogConstants.LOG_FIELDS);

        if ((strSelectedFields != null) && (strSelectedFields.length() != 0)) {
            StringTokenizer stoken =
                new StringTokenizer(strSelectedFields, ", ");

            while (stoken.hasMoreElements()) {
                fieldSet.add(stoken.nextToken());
            }
        }
        selectedFieldSet = fieldSet;
        return;
    }

    /**
     * This method overrides the <code>readConfiguration</code> method in
     * JDK <code>LogManager</code> class.
     * The base class method resets the loggers in memory. This method
     * must add handlers to the loggers in memory according to the
     * new configuration.
     *
     * @throws IOException if there are IO problems reading the configuration.
     * @throws SecurityException if a security manager exists and if the caller
     * does not have <code>LoggingPermission("control")</code>.
     */
    public final void readConfiguration()
            throws IOException, SecurityException {
        String[] xlogData = null;
        try {
            /*
             * This writeLock ensures that no logging threads will execute
             * a logger.log call after this point since they request for
             * a readLock.
             */
            Logger.rwLock.writeRequest();

            /*
             * This sync is for avoiding this thread snathing away
             * time slice from a thread executing getLogger() method
             * which is also sync on Logger.class
             * which may lead to two handlers being added to the same logger.
             */
            synchronized (Logger.class) {
                Enumeration loggerNames = getLoggerNames();
                LogManagerUtil.setupEnv();

                if (didFirstReadConfig && SystemProperties.isServerMode()) {
                    oldLocation = getProperty(LogConstants.LOG_LOCATION);
                    oldLevel = getProperty(LogConstants.LOGGING_LEVEL);
                    oldSecurityStatus =
                            getProperty(LogConstants.SECURITY_STATUS);
                    oldBackend = getProperty(LogConstants.BACKEND);
                    oldStatus = getProperty(LogConstants.LOG_STATUS_ATTR);
                }

                try {
                    /*
                     * This change is done for deploying AM as a single
                     * war. In server mode we will always use our
                     * LogConfigReader. On the client side the
                     * the JVM property will define whether to use the
                     * LogConfigReader or the remote handlers. If no
                     * JVM property is set, the remote handlers will
                     * be used.
                     */
                    if (SystemProperties.isServerMode()) {
                        LogConfigReader logConfigReader =
                                new LogConfigReader();
                    } else {
                        super.readConfiguration();
                    }
                    didFirstReadConfig = true;
                } catch (Exception ex) {
                    /* no debug since our debugging system is not up. */
                } finally {
                    LogManagerUtil.resetEnv();
                }

                if (isLocal) {
                    securityStatus = false;
                    readAllFields();
                    readSelectedFieldSet();

                    if (SystemProperties.isServerMode()) {
                        newLocation = getProperty(LogConstants.LOG_LOCATION);
                        newLevel = getProperty(LogConstants.LOGGING_LEVEL);
                        newSecurityStatus =
                                getProperty(LogConstants.SECURITY_STATUS);
                        newBackend = getProperty(LogConstants.BACKEND);
                        newStatus = getProperty(LogConstants.LOG_STATUS_ATTR);
                    }

                    /*
                     * give all the pertinent values to decide why
                     * logging to the file was terminated.  still
                     * have to check that one of the attributes
                     * that changed would cause a "real" termination
                     * of logging to the files/tables in the current
                     * location (as opposed to just a buffer timer change,
                     * for instance).
                     */

                    String[] logData = {oldLocation, newLocation,
                        oldBackend, newBackend,
                        oldSecurityStatus, newSecurityStatus,
                        oldStatus, newStatus,
                        oldLevel, newLevel};

                    if (getProperty(LogConstants.BACKEND).equals("DB")) {
                        HANDLER = getProperty(LogConstants.DB_HANDLER);
                        FORMATTER = getProperty(LogConstants.DB_FORMATTER);
                        String driver = getProperty(LogConstants.DB_DRIVER);
                    } else if (getProperty(
                        LogConstants.SECURITY_STATUS).equalsIgnoreCase("ON"))
                    {
                        securityStatus = true;
                        HANDLER = getProperty(LogConstants.SECURE_FILE_HANDLER);
                        FORMATTER =
                                getProperty(LogConstants.SECURE_ELF_FORMATTER);
                    } else {
                        HANDLER = getProperty(LogConstants.FILE_HANDLER);
                        FORMATTER = getProperty(LogConstants.ELF_FORMATTER);
                    }

                    if (getProperty(LogConstants.BACKEND).equals("File")) {
                        /*
                         * create new log directory if it has changed and
                         * the new directory does not exist.
                         */

                        if (SystemProperties.isServerMode() &&
                                (newLocation != null) &&
                                (oldLocation != null) &&
                                !oldLocation.equals(newLocation)) {
                            File dir = new File(newLocation);
                            if (!dir.exists()) {
                                if (!dir.mkdirs()) {
                                    Debug.error(
                                        "LogManager:readConfiguration:" +
                                        "Unable to create the new log " +
                                        "directory. Verify that the " +
                                        "process has necessary permissions");
                                }
                            }
                        }
                    }

                    boolean loggingInactive =
                            (getProperty(LogConstants.LOG_STATUS_ATTR).
                            equals(inactive));

                    String strLogLevel =
                            getProperty(LogConstants.LOGGING_LEVEL);
                    try {
                        loggingLevel = Level.parse(strLogLevel);
                    } catch (IllegalArgumentException iaex) {
                        loggingLevel = Level.INFO;  // default
                        Debug.error("LogManager:readConfiguration:" +
                                "Log level '" + strLogLevel +
                                "' unknown; setting to Level.INFO.");
                    }

                    /*
                     *  get the global logging level from the logging
                     *  service config.  however, if logging status is
                     *  INACTIVE, the overriding level becomes OFF
                     */

                    if (loggingInactive) {
                        loggingLevel = Level.OFF;
                    }
                    xlogData = logData;
                } else {
                    HANDLER = getProperty(LogConstants.REMOTE_HANDLER);
                    if (HANDLER == null) {
                        HANDLER = LogConstants.DEFAULT_REMOTE_HANDER;
                    }
                    FORMATTER = getProperty(LogConstants.REMOTE_FORMATTER);
                    if (FORMATTER == null) {
                        FORMATTER = LogConstants.DEFAULT_REMOTE_FORMATTER;
                    }
                }

                Logger.resolveHostName = Boolean.valueOf(
                        getProperty(LogConstants.LOG_RESOLVE_HOSTNAME_ATTR)).
                        booleanValue();

                /*
                 * modify existing loggers in memory according
                 * to the new configuration
                 */
                loggerNames = getLoggerNames();

                while (loggerNames.hasMoreElements()) {
                    String curEl = (String) loggerNames.nextElement();
                    /* avoid root logger */
                    if (curEl.length() != 0 && curEl.length() != 0 &&
                            !curEl.equals("global")) {
                        if (Debug.messageEnabled()) {
                            Debug.message(
                                    "LogManager:readConfiguration:" +
                                    "Processing Logger: " + curEl);
                        }

                        /*
                         * remove all handlers and add new handlers for
                         * this logger
                         */
                        Logger l = (Logger) Logger.getLogger(curEl);
                        Handler[] handlers = l.getHandlers();
                        for (int i = 0; i < handlers.length; i++) {
                            handlers[i].close();
                            l.removeHandler(handlers[i]);
                        }

                        String handlerClass = LogManager.HANDLER;
                        Class clz = null;
                        Class[] parameters = {String.class};
                        Object[] parameterObjects = {l.getName()};
                        Constructor cons = null;
                        Handler h = null;
                        try {
                            clz = Class.forName(handlerClass);
                        } catch (Exception e) {
                            Debug.error(
                                "LogManager.readConfiguration:could not load " +
                                handlerClass, e);
                        }
                        try {
                            cons = clz.getDeclaredConstructor(parameters);
                        } catch (Exception e) {
                            Debug.error(
                                    "LogManager.readConfiguration:could not" +
                                    " instantiate" + handlerClass, e);
                        }
                        try {
                            h = (Handler) cons.newInstance(parameterObjects);
                        } catch (Exception e) {
                            Debug.error(
                                    "LogManager.readConfiguration:could not" +
                                    " instantiate" + handlerClass, e);
                        }
                        String formatterClass = LogManager.FORMATTER;
                        Formatter f = null;
                        try {
                            f = (Formatter) Class.forName(formatterClass).
                                    newInstance();
                        } catch (Exception e) {
                            Debug.error(
                                    "LogManager.readConfiguration:could not" +
                                    " instantiate Formatter " +
                                    formatterClass, e);
                        }
                        h.setFormatter(f);
                        l.addHandler(h);

                        /*
                         *  get the "iplanet-am-logging.<logfilename>.level
                         *  value for this file, if it's been added on the
                         *  server's advanced config page.
                         *
                         *  BUT: logging status set to Inactive means
                         *  all logging is turned Level.OFF
                         */
                        Level tlevel = loggingLevel;
                        if (loggingLevel != Level.OFF) {
                            String levelProp =
                                    LogConstants.LOG_PROP_PREFIX + "." +
                                    l.getName() + ".level";
                            String lvlStr = SystemProperties.get(levelProp);

                            if ((lvlStr != null) && (lvlStr.length() > 0)) {
                                try {
                                    tlevel = Level.parse(lvlStr);
                                } catch (IllegalArgumentException iaex) {
                                    // use value for all others
                                }
                            }
                        }
                        if (loggingLevel != null) {  // only if isLocal
                            // update logging level
                            l.setLevel(tlevel);
                        }
                    } /* end of avoid rootlogger */
                } /* end of while(loggerNames.hasMoreElements) */
            } /* end of synchronized(Logger.class) */
        } finally {
            Logger.rwLock.writeDone();
        }

        if (SystemProperties.isServerMode() && isLocal) {
            checkStartLogs(xlogData);
            //Update the new configuration info in Monitoring handle also
            updateMonitConfigForLogService();
        }
    } /* end of readConfiguration() */


    /**
     *  check the existing ("old") config and the new config for
     *  specific attribute changes that would mean logging has
     *  changed to a new location or has re-started.  these are:
     *    1. logging location
     *    2. new Status == ACTIVE && old Level == OFF &&
     *       new Level != OFF
     *    3. old Status == INACTIVE && new Status == ACTIVE &&
     *       new Level != OFF
     *    4. old Backend != new Backend (File <-> DB)
     *    5. old Security Status != new Security Status
     *
     *  the String[] passed contains:
     *    [0] = old Location
     *    [1] = new Location
     *    [2] = old Backend
     *    [3] = new Backend
     *    [4] = old Security Status
     *    [5] = new Security Status
     *    [6] = old Status
     *    [7] = new Status
     *    [8] = old Level
     *    [9] = new Level
     */
    private void checkStartLogs(String[] vals) {
        Enumeration loggerNames = getLoggerNames();
        boolean loggingIsActive = false;
        boolean levelIsOff = true;

        // if the values array or any of its elements is null, just return
        if (vals == null) {
            return;
        }
        for (int i = 0; i <= NEWLEVEL; i++) {
            if ((vals[i] == null) || (vals[i].length() == 0)) {
                return;
            }
        }

        if (vals[NEWSTATUS] != null) {
            loggingIsActive = vals[NEWSTATUS].equals("ACTIVE");
        }
        if (vals[NEWLEVEL] != null) {
            levelIsOff = vals[NEWLEVEL].equals("OFF");
        }

        /*
         *  if current status == ACTIVE and Level != OFF,
         *  and individual log's Level != OFF,
         *  then write a start record to the log.
         *
         *  note that status == INACTIVE overrides any Level setting
         *  for the logging service, or an individual log file.
         *  
         */
        if (loggingIsActive) {
            // see if there's a reason to write the log record
            if (!vals[OLDBACKEND].equals(vals[NEWBACKEND]) ||
                    !vals[OLDLOCATION].equals(vals[NEWLOCATION]) ||
                    !vals[OLDSECURITYSTATUS].equals(vals[NEWSECURITYSTATUS]) ||
                    !vals[OLDSTATUS].equals(vals[NEWSTATUS]) ||
                    !vals[OLDLEVEL].equals(vals[NEWLEVEL])) {
                loggerNames = getLoggerNames();
                String saveLevel = vals[NEWLEVEL];
                Level level = Level.INFO;
                try {
                    level = Level.parse(vals[NEWLEVEL]);
                } catch (IllegalArgumentException iaex) {
                    // just leave it at "INFO" as a default
                }

                while (loggerNames.hasMoreElements()) {
                    vals[NEWLEVEL] = saveLevel;
                    String curEl = (String) loggerNames.nextElement();
                    /* avoid root logger */
                    if (curEl.length() != 0 && curEl.length() != 0 &&
                            !curEl.equals("global")) {
                        Logger l = (Logger) Logger.getLogger(curEl);

                        /*
                         *  additional reason to check if start record
                         *  should be written:
                         *    general Level is now "OFF", but this
                         *    individual file's level != OFF
                         *    then log to the individual file
                         *  and
                         *    general Level != OFF, but this
                         *    individual file's level == oFF
                         *    then don't log to the individual file
                         *  the individual file's level is set
                         *  in the readConfiguration stuff, above.
                         */

                        //  get this log's level
                        Level tlevel = l.getLevel();

                        if (levelIsOff) {
                            if (tlevel != Level.OFF) {
                                vals[NEWLEVEL] = tlevel.toString();
                                logIt(l, vals,
                                        LogConstants.START_LOG_CONFIG_NAME);
                            }
                        } else {
                            logIt(l, vals, LogConstants.START_LOG_CONFIG_NAME);
                        }
                    }
                }
            }
        }
    }

    private void logIt(Logger logger, String[] msg, String msgName) {
        try {
            LogMessageProviderBase provider =
                    (LogMessageProviderBase) MessageProviderFactory.getProvider(
                    "Logging");
            SSOToken ssot = LogManagerUtil.getLoggingSSOToken();
            com.sun.identity.log.LogRecord lr =
                    provider.createLogRecord(msgName, msg, ssot);
            logger.log(lr, ssot);
            logger.flush();
        } catch (IOException ioex) {
            Debug.error("LogManager.logIt:could not log to " +
                    logger.getName() + ": " + ioex.getMessage());
        }
    }

    /**
     * This method is called from two places, from readConfiguration() and from
     * Logger.getLoggers().
     */
    public void updateMonitConfigForLogService() {
        /*
         * if haven't gotten the logging service monitoring handle
         * yet, see if it's setup now
         */
        if (SystemProperties.isServerMode() && MonitoringUtil.isRunning()) {
            if (logServiceImplForMonitoring == null) {
                logServiceImplForMonitoring =
                    Agent.getLoggingSvcMBean();
            }
            if (logServiceImplForMonitoring == null) {
                return;
            }

            logServiceImplForMonitoring.setSsoServerLoggingLoggers(
                    new Integer(loggerCount));
            logServiceImplForMonitoring.setSsoServerLoggingSecure(
                    newSecurityStatus);
            logServiceImplForMonitoring.setSsoServerLoggingTimeBuffering(
                    getProperty(LogConstants.TIME_BUFFERING_STATUS));
            logServiceImplForMonitoring.setSsoServerLoggingBufferSize(
                    Long.valueOf(getProperty(LogConstants.BUFFER_SIZE)).
                    longValue());
            logServiceImplForMonitoring.setSsoServerLoggingBufferTime(
                    Long.valueOf(getProperty(LogConstants.BUFFER_TIME)).
                    longValue());
            logServiceImplForMonitoring.setSsoServerLoggingMaxLogSize(
                    Long.valueOf(getProperty(
                    LogConstants.MAX_FILE_SIZE)).longValue());
            logServiceImplForMonitoring.
                setSsoServerLoggingNumberHistoryFiles(Long.valueOf(
                    getProperty(LogConstants.NUM_HISTORY_FILES)).
                    longValue());
            logServiceImplForMonitoring.setSsoServerLoggingLocation(
                    getProperty(LogConstants.LOG_LOCATION));
            logServiceImplForMonitoring.setSsoServerLoggingType(
                    getProperty(LogConstants.BACKEND));
            logServiceImplForMonitoring.setSsoServerLoggingRecsRejected(
                    (long)0);
            
            isMonitoringInit = true;
        }
    }

    public boolean getLoggingStatusIsActive() {
        String oStatus = getProperty(LogConstants.LOG_STATUS_ATTR);
        return (oStatus.equalsIgnoreCase("ACTIVE"));
    }

    protected String getBackend() {
        return (newBackend);
    }

    protected boolean isDBLogging() {
        return (newBackend.equals("DB"));
    }

    public boolean getDidFirstReadConfig() {
        return didFirstReadConfig;
    }

    /*
     *  only meant to be called from s1is.LogConfigReader when
     *  logging status goes from ACTIVE to INACTIVE
     */
    public synchronized void logStopLogs() {
        String location = getProperty(LogConstants.LOG_LOCATION);
        String level = getProperty(LogConstants.LOGGING_LEVEL);
        String securityStatus = getProperty(LogConstants.SECURITY_STATUS);
        String backend = getProperty(LogConstants.BACKEND);
        String status = getProperty(LogConstants.LOG_STATUS_ATTR);
        //  only care about status going from ACTIVE to INACTIVE
        String[] vals = {location, location,
                        backend, backend,
                        securityStatus, securityStatus,
                        status, inactive,
                        level, level};

        Enumeration loggerNames = getLoggerNames();
        while (loggerNames.hasMoreElements()) {
            String curEl = (String) loggerNames.nextElement();
            /* avoid root logger */
            if (curEl.length() != 0 && curEl.length() != 0 &&
                !curEl.equals("global"))
            {
                Logger l = (Logger) Logger.getLogger(curEl);

                /*
                 *  additional reason to check if end record
                 *  should be written:
                 *    this individual file's level == oFF
                 *    then don't log to the individual file
                 */

                //  get this log's level
                Level tlevel = l.getLevel();

                if (tlevel != Level.OFF) {
                    logIt(l, vals, LogConstants.END_LOG_CONFIG_NAME);
                }
            }
        }
    }
}

