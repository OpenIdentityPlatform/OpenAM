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
 * $Id: Logger.java,v 1.15 2009/12/09 00:34:21 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.log;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.ReaderWriterLock;
import com.sun.identity.log.messageid.LogMessageProviderBase;
import com.sun.identity.log.messageid.MessageProviderFactory;
import com.sun.identity.log.spi.Authorizer;
import com.sun.identity.log.spi.Debug;
import com.sun.identity.monitoring.Agent;
import com.sun.identity.monitoring.MonitoringUtil;
import com.sun.identity.monitoring.SsoServerLoggingSvcImpl;
import com.sun.identity.monitoring.SsoServerLoggingHdlrEntryImpl;
import com.sun.identity.shared.Constants;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import javax.security.auth.Subject;

/**
 * OpenSSO extension to the jdk1.4 Logger
 * This extension gives some functionality required by
 * OpenSSO secure logger.
 * For <code>JDK1.4</code> Logger please refer to
 * <pre>
 * http://java.sun.com/j2se/1.4.1/docs/api/java/util/logging/Logger.html
 * </pre>
 * @supported.all.api
 */
public class Logger extends java.util.logging.Logger {
    
    public static ThreadLocal token = new ThreadLocal();
    private String currentFileName = new String();
    private static LogManager lm;
    private String logName;
    protected static boolean resolveHostName;

    /**
     * Lock to prevent parallel writing and reading at the same time.
     */
    public static ReaderWriterLock rwLock = new ReaderWriterLock();
    
    static {
        lm = (com.sun.identity.log.LogManager) LogManagerUtil.getLogManager();
        try {
            lm.readConfiguration();
        } catch (Exception ex) {
            ex.printStackTrace();
            /* our Debug system will no be up now, so can't Debug */
        }
        String location = lm.getProperty(LogConstants.LOG_LOCATION);
        String type = lm.getProperty(LogConstants.BACKEND);
        if ((location != null) && type.equals("File")) {
            File dir = new File(location);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Debug.error("Logger:Creation of Log Directory failed: " +
                        location);
                }
            }
        }

        /* Check if hostnames have to be resolved */
        resolveHostName = Boolean.valueOf(
            lm.getProperty(LogConstants.LOG_RESOLVE_HOSTNAME_ATTR)).
                booleanValue();
    }
    
    /**
     * Protected method to construct a logger for a named subsystem.
     * <p>
     * The logger will be initially configured with a null Level
     * and with useParentHandlers true.
     *
     * @param name A name for the logger.  This should be a
     *        dot-separated name and should normally be based on the
     *        package name or class name of the subsystem, such as java.net
     *        or javax.swing.  It may be null for anonymous Loggers.
     * @param resourceBundleName Name of the ResourceBundle to be used for
     *        localizing messages for this logger.  May be null if none
     *        of the messages require localization.
     * @throws MissingResourceException if the ResourceBundleName is
     *         non-null and no corresponding resource can be found.
     */
    protected Logger(String name,String resourceBundleName) {
        super(name,resourceBundleName);
    }

    /**
     * To add handlers and formatters to the new logger object
     */
    private static void processNewLoggerObject (Logger result) {
        Formatter formatter = null;
        String handlerClass = LogManager.HANDLER;
        String formatterClass = LogManager.FORMATTER;
        String levelProp = LogConstants.LOG_PROP_PREFIX + "." +
            result.logName + ".level";
        
        /*
         *  see if logging level for this file already defined.
         *  if not, then check AMConfig.properties.
         *  if not, then use Logging service config value.
         *  if not, then use default ("INFO")
         */
        String levelString = lm.getProperty(levelProp);

        if ((levelString == null) || !(levelString.length() > 0)) {
            levelString = SystemProperties.get (levelProp);
            if ((levelString == null) || !(levelString.length() > 0)) {
                levelString = lm.getProperty(LogConstants.LOGGING_LEVEL);
                if ((levelString == null) || !(levelString.length() > 0)) {
                    levelString = LogConstants.DEFAULT_LOGGING_LEVEL_STR;
                }
            }
        }
        Level logLevel = null;
        try {
            logLevel = Level.parse(levelString);
        } catch (IllegalArgumentException iaex) {
            logLevel = LogConstants.DEFAULT_LOGGING_LEVEL;
        }

        result.setLevel(logLevel);

        //  but disabled logging in AMConfig.properties takes precedence
        String logStatus = lm.getProperty(LogConstants.LOG_STATUS_ATTR);
        if (logStatus != null && logStatus.startsWith("INACTIVE")) {
            logLevel = Level.OFF;
        }
        result.setLevel(logLevel);

        Class clz = null;
        Class [] parameters = {String.class};
        Object [] parameterObjects = {result.logName};
        Constructor cons = null;
        Handler handler = null;
        
        if (handlerClass == null) {
            Debug.error("Logger:processNewLoggerObject:" +
                "HandlerClass not in classpath ");
            return;
        }
        try {
            clz = Class.forName(handlerClass);
        } catch (Exception e) {
            Debug.error("Logger:processNewLoggerObject:" +
                "HandlerClass not in classpath: " + handlerClass, e);
            return;
        }
        try {
            if(clz != null) {
                cons = clz.getDeclaredConstructor(parameters);
            }
        } catch (Exception e) {
            Debug.error("Logger:processNewLoggerObject:" +
                "constructor parameter mismatch ", e);
            return;
        }
        try {
            if(cons != null) {
                handler = (Handler) cons.newInstance(parameterObjects);
            }
        } catch (Exception e) {
            Debug.error("Logger:processNewLoggerObject:" +
                "Could not instantiate handler: " + handlerClass, e);
            return;
        }
        if (formatterClass == null) {
            Debug.error("Logger:processNewLoggerObject:" +
                "formatterClass not in classpath ");
            return;
        }
        try {
            clz = Thread.currentThread().getContextClassLoader().
                loadClass(formatterClass);
        } catch (Exception e) {
            Debug.error("Logger:processNewLoggerObject:" +
                "Could not load Formatter Class: " + formatterClass, e);
            return;
        }
        try {
            if(clz != null) {
                formatter = (Formatter) clz.newInstance();
            }
        } catch (Exception e) {
            Debug.error("Logger:processNewLoggerObject:" +
                "Could not get Formatter instance " + formatterClass, e);
            return;
        }
        try {
            handler.setFormatter(formatter);
            result.addHandler(handler);
        } catch (Exception e) {
            Debug.error("Logger:processNewLoggerObject:" +
                "Unable to add Handler", e);
            return;
        }
        String filterClassName = lm.getProperty(LogConstants.FILTER_CLASS_NAME);
        try {
            if (filterClassName != null) {
                Filter filter =
                    (Filter)Class.forName(filterClassName).newInstance();
                result.setFilter(filter);
            }
        } catch (Exception e) {
            Debug.error("Logger:processNewLoggerObject:" +
                "Could not set Filter: "+ filterClassName, e);
        }
        
        result.setUseParentHandlers(false);

        resolveHostName = Boolean.valueOf(
            lm.getProperty(LogConstants.LOG_RESOLVE_HOSTNAME_ATTR)).
                booleanValue();
    }
    
    /**
     * Directs every log call to <code>log(LogRecord, Object)</code>
     * And thus the default authorization check does not allow logging 
     * when an application uses this interface.
     *
     * @param record The <code>LogRecord</code> to be logged.
     */
    public void log(LogRecord record) {
        if (record instanceof ILogRecord) {
            log((ILogRecord)record);
        } else {
            Object obj = token.get();
            log(record, obj);
        }
    }

    private boolean validateLogBy(Object cred) {
        if (!LogManager.isLocal) {
            if (cred == null) {
                /* In case of remote sso token must be provide. */
                Debug.error("Logger.validateLogBy:" + logName +
                    ": remote logging, ssoToken is null; Will not log");
                return false;
            }
        } else {
            /* Authorizer need not be called in the case of remote. */
            if (!Authorizer.isAuthorized(logName, "MODIFY", cred)) {
                incMonReject(); // increment log svc and handler stats
                Debug.error("Logger.validateLogBy:" + logName +
                    ": authorization failed; Will not log");
                throw new AMLogException(logName + ":" +
                    AMLogException.LOG_WRT_AUTH_FAILED);
            }
        }
        return true;
    }

    private void addLogByInfo(ILogRecord record, Object cred) {
        if (cred instanceof SSOToken) {
            SSOToken ssoToken = (SSOToken) cred;
            String loggedBySID = ssoToken.getTokenID().toString();
            record.addLogInfo(LogConstants.LOGGED_BY_SID, loggedBySID);
            String clientID = null;
            try {
                clientID = ssoToken.getPrincipal().getName();
            } catch (SSOException ssoe) {
                Debug.error("Logger:log:" + logName +
                    ": could not get clientID from ssoToken:", ssoe);
            }
            record.addLogInfo(LogConstants.LOGGED_BY, clientID);
        }
    }

    private void addModuleName(ILogRecord record) {
        String existModuleName =
            (String)record.getLogInfoMap().get(LogConstants.MODULE_NAME);
        if (existModuleName == null || existModuleName.length() <= 0) {
            /* add module name only if it's already not added. */
            record.addLogInfo(LogConstants.MODULE_NAME, this.getName());
        }

    }

    /**
     * Log entitlement log record.
     *
     * @param record Log record.
     */
    public void log(ILogRecord record) {
        try {
            extractInfoFromLogFor(record);
        } catch (SSOException e) {
            Debug.error("Logger.log " + e.getMessage());
        }

        if (record instanceof java.util.logging.LogRecord) {
            Object logBy = record.getLogBy();
            Object cred = (logBy instanceof Subject) ?
                getPrivateCred((Subject)logBy) : logBy;
            log((java.util.logging.LogRecord)record, cred);
        } else {
            Debug.error(
                "Logger.log: cannot log non java.util.logging.LogRecord class");
        }
    }

    private static Object getPrivateCred(Subject sbj) {
        Set privCreds = sbj.getPrivateCredentials();
        return ((privCreds != null) && !privCreds.isEmpty()) ?
            privCreds.iterator().next() : null;
    }

    /**
     * Calls super.log after checking authorization.
     * Data is not logged at all if this check fails.
     *
     * @param record The <code>LogRecord</code> to be logged.
     * @param cred To prove authorization for log WRITE.
     *        The default authorization hook checks validity of the single
     *        sign on token which should be passed as the <code>cred</code>.
     */
    public void log(LogRecord record, Object cred) {
        validateLogBy(cred);
        if (record instanceof com.sun.identity.log.ILogRecord) {
            com.sun.identity.log.ILogRecord openssoLogRecord =
                (com.sun.identity.log.ILogRecord)record;
            addLogByInfo(openssoLogRecord, cred);
            addModuleName(openssoLogRecord);
        }

        /*
         * These are normally done by the LogManager private method
         * doLog(). But since this record is not passing through that
         * method we have to explicitly do this.
         * ResourceBundle logic has been simplified.
         */
        record.setLoggerName(getName());
        String rbName = this.getResourceBundleName();
        if (rbName != null) {
            ResourceBundle bundle = ResourceBundle.getBundle(rbName);
            record.setResourceBundle(bundle);
        }
        writeToLog(record);
    }

    private void writeToLog(LogRecord record) {
        try {
            rwLock.readRequest();
            /*
             * this is to serialize logging,signing and verifying
             * threads so that no signing or verification takes
             * place once a logging thread has gone past this point
             */
            if (lm.isSecure()) {
                synchronized (this) {
                    super.log(record);
                }
            } else {
                super.log(record);
            }
        } catch (Exception ex) {
            Debug.error("Logger.writeToLog:" + logName + ":" + ex.getMessage());
            throw new AMLogException(logName + ":" + ex.getMessage());
        } finally {
            rwLock.readDone();
        }
    }

    /** Writes all the buffered log records.
     */
    public void flush() {
        /*
         * Post the LogRecord to all our Handlers, and then to
         * our parents' handlers, all the way up the tree.
         */
        Logger logger = this;
        Handler targets[] = logger.getHandlers();
        if (targets != null) {
            for (int i = 0; i < targets.length; i++) {
                targets[i].flush();
            }
        }
    }
   
    /**
     * Find or create a logger for a named subsystem.  If a logger has
     * already been created with the given name it is returned.  Otherwise
     * a new logger is created.
     * <p>
     * If a new logger is created its log level will be configured
     * based on the <code>LogManager</code> and it will be configured NOT to
     * send logging output to its parent loggers Handlers.  It will be
     * registered in the <code>LogManager</code> global namespace.
     *
     * @param name A name for the logger.  This should be a dot-separated name
     *        and should be the file name you want to have for your logs,
     *        such as <code>amSSO.access</code>, or audit.
     * @return a suitable <code>Logger</code>.
     */
    public static synchronized java.util.logging.Logger getLogger(String name)
    {
        if ((name == null) || (name.length() == 0) || name.indexOf("..") >= 0)
        {
            /* Do not allow logging if logName has "..". */
            return null;
        }
        Logger result;

        boolean loggerExists = false;
        Enumeration e = lm.getLoggerNames();
        while (e.hasMoreElements()) {
            if (((String) e.nextElement()).equals(name)) {
                loggerExists = true;
            }
        }

        if (loggerExists)  {
            result = (Logger) lm.getLogger(name);
            if (result != null) {
                return result;
            }
        }
        java.util.logging.Logger newLog = (java.util.logging.Logger)
            java.util.logging.Logger.getLogger(name);
        lm.addLogger(newLog);
        result = (Logger) lm.getLogger(name);

        result.logName = name;
        processNewLoggerObject(result);
        if (SystemProperties.isServerMode()) {
            logStartRecord(result);
        }

        /* Logging service starts earlier than Monitoring.
         * Because of this the first call to LogManager's readConfiguration()
         * does not update the monitoring handle with the config information for
         * logging. Hence we need to call updateMonitConfigForLogService() here
         * to make sure the monitoring handle does get updated with the config
         * information eventually.
         */
        if(!lm.isMonitoringInit){
            lm.updateMonitConfigForLogService();
        }
        return result;
    }
    
    /** Find or create a logger for a named subsystem.  If a logger has
     * already been created with the given name it is returned.  Otherwise
     * a new logger is created.
     * <p>
     * If a new logger is created, its log level will be configured
     * based on the <code>LogManager</code> and it will configured to also
     * send logging output to its parent logger's Handlers.  It will be
     * registered in the <code>LogManager</code> global namespace.
     * <p>
     * If the named Logger already exists and does not yet have a
     * localization resource bundle then the given resource bundle
     * name is used.  If the named Logger already exists and has
     * a different resource bundle name then an
     * <code>IllegalArgumentException</code> is thrown.
     *
     * @param name A name for the logger.  This should be a dot-separated name
     *        and should be the file name you want to have for your logs, such
     *        as <code>amSSO.access</code> or audit.
     * @param rbName A resource bundle to be used for localizing the log
     *        messages.
     * @return logger for a named subsystem.
     */
    public static synchronized java.util.logging.Logger getLogger(
        String name, String rbName)
    {
        if ((name == null) || (name.length() == 0) || name.indexOf("..") >= 0) {
            /* Do not allow logging if logName has "..". */
            return null;
        }
        boolean loggerExists = false;
        Enumeration e = lm.getLoggerNames();
        while (e.hasMoreElements()) {
            if (((String) e.nextElement()).equals(name)) {
                // The LoggerName is in the list, but we should check whether the
                // referenced Logger still exists, see OPENAM-14
                if (lm.getLogger(name) != null) {
                    loggerExists = true;
                }
            }
        }
        Logger result = (Logger)
        java.util.logging.Logger.getLogger(name, rbName);
        result.logName = name;
        if (loggerExists)  {
            return result;
        }
        /*
         * if the logger is a new object, we have to set the appropriate
         * handlers and formatters to the logger before returning the result.
         */
        
        processNewLoggerObject(result);
        if (SystemProperties.isServerMode()) {
            logStartRecord(result);
        }
        return result;
    }
    
    /**
     *  Log a LogRecord indicating the start of logging to this file
     */
    private static void logStartRecord (Logger logger) {
        /*
         *  SSOToken not required to instantiate a log file, so
         *  need one to say who's doing the logging of the record,
         *  and whose it "about".
         */
        try {
            LogMessageProviderBase provider =
                (LogMessageProviderBase)MessageProviderFactory.getProvider(
                    "Logging");
            SSOToken ssot = LogManagerUtil.getLoggingSSOToken();
            String location = lm.getProperty(LogConstants.LOG_LOCATION);
            String[] s = {location};
            com.sun.identity.log.LogRecord lr =
            provider.createLogRecord(LogConstants.START_LOG_NEW_LOGGER_NAME,
                s, ssot);
            logger.log(lr, ssot);
        } catch (IOException ioex) {
            Debug.error("Logger.logStartRecord:could not log to " + 
                logger.getName() + ":" + ioex.getMessage());
        }
    }

    /**
     * Returns the current file to which the logger's handler is writing.
     * This is useful only in case of file..
     *
     * @return the current file to which the logger's handler is writing.
     */
    public String getCurrentFile() {
        return currentFileName;
    }
    
    /**
     * Set the current file to which the logger's handler is writing.
     *
     * @param fileName name of file.
     */
    public void setCurrentFile(String fileName) {
        currentFileName = fileName;
    }
    
    /**
     * Return whether resolve host name is enabled
     *
     * @return <code>resolveHostName</code> 
     */
    public static boolean resolveHostNameEnabled() {
        return resolveHostName;
    }

    static void extractInfoFromLogFor(ILogRecord rec)
        throws SSOException {
        Object logFor = rec.getLogFor();
        Object cred = (logFor instanceof Subject) ?
            getPrivateCred((Subject)logFor) : logFor;

        if (!(cred instanceof SSOToken)) {
            return;
        }
        SSOToken ssoToken = (SSOToken)cred;
        rec.addLogInfo(LogConstants.LOGIN_ID_SID,
            ssoToken.getTokenID().toString());

        String ctxID = ssoToken.getProperty(Constants.AM_CTX_ID);
        if ((ctxID != null) && (ctxID.length() > 0)) {
            rec.addLogInfo(LogConstants.CONTEXT_ID, ctxID);
        }

        resolveHostName(rec, ssoToken);
        String clientDomain = ssoToken.getProperty("Organization");
        if (clientDomain == null || clientDomain.length() == 0) {
            clientDomain = ssoToken.getProperty("cdomain");
        }
        rec.addLogInfo(LogConstants.DOMAIN, clientDomain);
        rec.addLogInfo(LogConstants.LOGIN_ID,
            ssoToken.getPrincipal().getName());

        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        /*
         * these are the compulsory fields ... to be logged even if there are
         * exceptions while getting domain, loginid, ipaddr, hostname
         */
        rec.addLogInfo(LogConstants.TIME, sdf.format(date));

        if (rec instanceof java.util.logging.LogRecord) {
            java.util.logging.LogRecord jLogRecord =
                (java.util.logging.LogRecord)rec;
            rec.addLogInfo(LogConstants.DATA, jLogRecord.getMessage());
            rec.addLogInfo(LogConstants.LOG_LEVEL,
                jLogRecord.getLevel().toString());
        }

    }

    static void resolveHostName(ILogRecord rec, SSOToken ssoToken)
        throws SSOException {
        /*
         *  using the SSOToken, get the hostname first, as
         *  getting the IPAddr appears to use an Inet call using
         *  the hostname...
         *
         *  if com.sun.identity.log.resolveHostName=false, then
         *  IPAddr field will end up "Not Available"
         */
        String hostName = ssoToken.getHostName();
        String ipAddress = null;

        if (Logger.resolveHostName) {
            java.net.InetAddress ipAddr = ssoToken.getIPAddress();
            if (ipAddr != null) {
                /*
                 * getting a leading "/" from InetAddress.getByName(host)
                 * in SSOTokenImpl.java when "host" is an IPaddress.
                 */
                ipAddress = ipAddr.getHostAddress();

                /*
                 *  if no hostname returned, or only IP address,
                 *  try getting hostname from InetAddr
                 */
                if ((hostName == null) ||
                    ((ipAddress != null) && (ipAddress.equals(hostName)))) {
                    hostName = ipAddr.getHostName();
                }
            }
        }
        rec.addLogInfo(LogConstants.HOST_NAME, hostName);
        rec.addLogInfo(LogConstants.IP_ADDR, ipAddress);
    }

    /*
     *  increment the logging service LoggingRecsRejected attribute and
     *  the logging handler's (File, DB, and Secure only) LoggingHdlrFailureCt.
     *  this is for the count of rejections due to unauthorized userid trying
     *  to write to the log.
     */
    private void incMonReject() {
        if (LogManager.isLocal && MonitoringUtil.isRunning()) {
            // logging service stat
            SsoServerLoggingSvcImpl logSvcMon =
                Agent.getLoggingSvcMBean();
            if (logSvcMon != null) {
                 logSvcMon.incSsoServerLoggingRecsRejected();
            }

            // handler's stat
            // if DB then database, else if secure then secure file, else file
            SsoServerLoggingHdlrEntryImpl logH = null;
            if (lm.isDBLogging()) {
                logH = logSvcMon.getHandler(
                        SsoServerLoggingSvcImpl.DB_HANDLER_NAME);
            } else if (lm.isSecure()) {
                logH = logSvcMon.getHandler(
                        SsoServerLoggingSvcImpl.SECURE_FILE_HANDLER_NAME);
            } else {
                logH = logSvcMon.getHandler(
                        SsoServerLoggingSvcImpl.FILE_HANDLER_NAME);
            } 
            if (logH != null) {
                logH.incHandlerFailureCount(1);
                /*
                 *  also increment handler's request count.  if it gets
                 *  through the authorization check, it gets incremented
                 *  in the handler itself.
                 */
                logH.incHandlerRequestCount(1);
            }
        }
    }
}
