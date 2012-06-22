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
 * $Id: LogConfigReader.java,v 1.19 2009/11/04 22:33:10 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2011 ForgeRock AS
 */

package com.sun.identity.log.s1is;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.log.LogConstants;
import com.sun.identity.log.LogManager;
import com.sun.identity.log.LogManagerUtil;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.AccessController;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * <tt>LogConfigReader</TT> is used to load the configuration from the
 * Directory Server and store the configuration as Properties of
 * <tt>LogManager</tt>. <p>
 * <tt>LogConfigReader</TT> is very Speicific to DSAME. <p>
 * java.util.logging.config.class System property should point to this class,
 * since LogManager uses this property instantiates this class to load
 * the configuration.
 */
public class LogConfigReader implements ServiceListener{
    
    private static Debug debug;
    private static ServiceSchema smsLogSchema           = null;
    private static ServiceSchema smsPlatformSchema      = null;
    private static ServiceSchema smsNamingSchema        = null;
    private static Map logAttributes                    = null;
    private static Map platformAttributes               = null;
    private static Map namingAttributes                 = null;
    
    private String localProtocol = null;
    private String localHost = null;
    private String localPort = null;
    private boolean useOldLogFormat = false;

    /**
     * Local Log service identifier
     */
    public static String localLogServiceID = null;

    private static boolean isRegisteredForDSEvents = false;
    private LogManager manager;
    
    /**
     * The constructor loads the configuration from the DS using
     * DSAME SDK. Constructs a String as "key=value CRLF" for each
     * AttributeSchema in the amLogging.xml. In case the AttributeSchema
     * has multiple values or a List, it gets converted to a "," seperated
     * String.
     * <p> Example1: iplanet-am-logging-backend=FILE \r\n
     * <p> Example2: iplanet-am-logging-logfields=TIME, DOMAIN, IPADDR, 
     * HOSTNAME, DATA, LEVEL, LOGINID \r\n
     * <p> The input stream hence constructed is converted into a
     * ByteArrayInputStream and is loaded into LogManager.
     * @throws IOException
     */
    public LogConfigReader() throws IOException {
        debug = Debug.getInstance("amLog");

        localProtocol = SystemProperties.get("com.iplanet.am.server.protocol");
        localHost = SystemProperties.get("com.iplanet.am.server.host");
        localPort = SystemProperties.get(Constants.AM_SERVER_PORT);
        localLogServiceID = localProtocol + "://" + localHost + ":" + localPort;
        useOldLogFormat =
                SystemProperties.getAsBoolean(Constants.USE_OLD_LOG_FORMAT);

        SSOToken ssoToken;
        try {
            ssoToken = getSSOToken();
        } catch (SSOException ssoe) {
            debug.error("LogConfigReader: Could not get proper SSOToken", ssoe);
            return;
        }
        if (debug.messageEnabled()) {
            debug.message("LogConfigReader: ssoToken obtained" + ssoToken);
        }
        try {
            getDefaultAttributes(ssoToken);
        } catch (SMSException smse) {
            debug.warning("LogConfigReader: Could not " +
                "get defaultAttributes", smse);
            return;
        } catch (SSOException ssoe) {
            debug.error("LogConfigReader: Could not " +
                "get defaultAttributes", ssoe);
            return;
        }
        String configString = constructInputStream();
        ByteArrayInputStream inputStream = null;
        try { inputStream = 
                new ByteArrayInputStream(configString.getBytes("ISO8859-1"));
        } catch (UnsupportedEncodingException unse) {
            debug.error("LogConfigReader: unsupported Encoding" + unse);
        }
        manager = 
            (com.sun.identity.log.LogManager) LogManagerUtil.getLogManager();

        try {
            manager.readConfiguration(inputStream);
        } catch (IOException ioe) {
            debug.error("LogConfigReader: Can not load configuration" + ioe);
            throw new IOException(ioe.toString());
        }
        setLocalFlag();
    }
    
    /**
     * LogManager needs inputStream in the form of " Key = Value \r\n ". 
     * so to get that we need to get the keys of the default attributs append 
     * a "=", get the value for that key and append a CRLF. This input stream 
     * will then be loaded into the logmanager via properties API.
     */
    private String constructInputStream() {
        StringBuilder sbuffer = new StringBuilder(2000);
        String key = null;
        String value = null;
        Set set;
        Iterator it;
        StringBuilder tempBuffer = new StringBuilder();
        boolean fileBackend = false;
        String basedir = null;
        String famuri = null;
        // processing logging attributes.
        try {
            /*
             * generate %BASE_DIR% and %SERVER_URI% values, in case
             * they're not set up yet (e.g., during configuration
             */
            famuri = SystemProperties.get(
                Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
            famuri = famuri.replace('\\','/');
            basedir = SystemProperties.get(
                SystemProperties.CONFIG_PATH);
            basedir = basedir.replace('\\','/');
            if (famuri.startsWith("/")) {
                byte[] btmp = famuri.getBytes();
                famuri = new String(btmp, 1, (btmp.length - 1));
            }
            if (basedir.endsWith("/")) {
                byte[] btmp = basedir.getBytes();
                basedir = new String(btmp, 0, (btmp.length - 1));
            }

            logAttributes = smsLogSchema.getAttributeDefaults();
            // File/jdbc
            key = LogConstants.BACKEND;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader: Backend string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
                fileBackend = value.equals("File");
            }
        } catch (Exception e) {
            debug.error("LogConfigReader: Could not read Backend ", e);
        }
        // Database Driver
        try {
            key = LogConstants.DB_DRIVER;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader: Driver string is null");
            } else {
                sbuffer.append(key).append("=").append(value)
                       .append(LogConstants.CRLF);
            }
        }  catch (Exception e) {
            debug.error("LogConfigReader: Could not read driver ", e);
        }
        // Database Password
        try {
            key = LogConstants.DB_PASSWORD;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader: Password string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        }   catch (Exception e) {
            debug.error("LogConfigReader: Could not read password ", e);
        }
        // Database USER
        try {
            key = LogConstants.DB_USER;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader: DB_USER string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        }   catch (Exception e) {
            debug.error("LogConfigReader: Could not read db user ", e);
        }
        // all Fields
        try {
            key = LogConstants.ALL_FIELDS;
            tempBuffer.append("time, Data, ");
            set = (Set) logAttributes.get(key);

            if (!useOldLogFormat) {
                it = set.iterator();
            } else {
                TreeSet orderedHeaders = new TreeSet(new LogHeaderComparator());
                orderedHeaders.addAll(set);
                it = orderedHeaders.descendingIterator();
            }

            String headerValue = (String) it.next();

            if (headerValue.contains(Constants.COLON)) {
                headerValue =
                        headerValue.substring(headerValue.indexOf(Constants.COLON) + 1);
            }

            tempBuffer.append(headerValue);

            while(it.hasNext()) {
                headerValue = (String) it.next();

                if (headerValue.contains(Constants.COLON)) {
                    headerValue =
                        headerValue.substring(headerValue.indexOf(Constants.COLON) + 1);
                }

                tempBuffer.append(", ").append(headerValue);
            }

            sbuffer.append(key).append(Constants.EQUALS)
                   .append(tempBuffer).append(LogConstants.CRLF);

        } catch (Exception ex) {
            debug.error("LogConfigReader: Could not read all field  ", ex);
        }
        // Selected Log Fields
        try {
            key = LogConstants.LOG_FIELDS;
            set = (Set) logAttributes.get(key);
            if ((set != null) && (!set.isEmpty())) {
                it = set.iterator();
                tempBuffer = new StringBuilder();
                tempBuffer.append((String) it.next());
                while(it.hasNext()) {
                    tempBuffer.append(", ").append((String) it.next());
                }
                sbuffer.append(key).append("=")
                       .append(tempBuffer).append(LogConstants.CRLF);
            }
        }   catch (Exception e) {
            debug.error("LogConfigReader: Could not read log-field ", e);
        }
        // Max file size
        try {
            key = LogConstants.MAX_FILE_SIZE;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader: Max File Size string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        }   catch (Exception e) {
            debug.error("LogConfigReader: Could not read maxfilesize ", e);
        }
        // log filename prefix
        try {
            key = LogConstants.LOGFILE_PREFIX;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if (value == null || value.length() == 0) {
                debug.warning("LogConfigReader: Logfile prefix string is null");
            } else {
                sbuffer.append(key).append('=')
                        .append(value).append(LogConstants.CRLF);
            }
        } catch (Exception e) {
            debug.error("LogConfigReader: Could not read logfile prefix ", e);
        }
        // log filename suffix
        try {
            key = LogConstants.LOGFILE_SUFFIX;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if (value == null || value.length() == 0) {
                debug.warning("LogConfigReader: Logfile suffix string is null");
            } else {
                sbuffer.append(key).append('=')
                        .append(value).append(LogConstants.CRLF);
            }
        } catch (Exception e) {
            debug.error("LogConfigReader: Could not read logfile suffix ", e);
        }
        // log filename rotation interval
        try {
            key = LogConstants.LOGFILE_ROTATION;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if (value == null || value.length() == 0) {
                debug.warning("LogConfigReader: Logfile rotation interval is null");
            } else {
                sbuffer.append(key).append('=')
                        .append(value).append(LogConstants.CRLF);
            }
        } catch (Exception e) {
            debug.error("LogConfigReader: Could not read logfile rotation interval ", e);
        }
        // number of history files
        try {
            key = LogConstants.NUM_HISTORY_FILES;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader: NUM_HIST_FILES string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        }   catch (Exception e) {
            debug.error("LogConfigReader: Could not read numhistfiles ", e);
        }
        // archiver  class
        try {
            key = LogConstants.ARCHIVER;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader: Archiver string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        }   catch (Exception e) {
            debug.error("LogConfigReader: Could not read filehandler ", e);
        }
        // file handler class
        try {
            key = LogConstants.FILE_HANDLER;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning(
                    "LogConfigReader: FileHandler class string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        }   catch (Exception e) {
            debug.error("LogConfigReader: Could not read filehandler ", e);
        }
        // secure File handler class
        try {
            key = LogConstants.SECURE_FILE_HANDLER;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader: Secure FH string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        }   catch (Exception e) {
            debug.error("LogConfigReader: Could not read " +
                "secure filehandler ", e);
        }
        // db handler class
        try {
            key = LogConstants.DB_HANDLER;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader: DBHandler string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        }   catch (Exception e) {
            debug.error("LogConfigReader: Could not read dbhandler ", e);
        }
        // remote handler class
        try {
            key = LogConstants.REMOTE_HANDLER;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader: Remote Handler string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        }   catch (Exception e) {
            debug.error("LogConfigReader: Could not read remotehandler ", e);
        }
        
        // elf formatter class
        try {
            key = LogConstants.ELF_FORMATTER;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader:ELFFormatter string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        }   catch (Exception e) {
            debug.error("LogConfigReader: Could not read elfformatter ", e);
        }
        // secure elf formatter class
        try {
            key = LogConstants.SECURE_ELF_FORMATTER;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader: Secure " +
                    "ELFFormatter string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        }   catch (Exception e) {
            debug.error("LogConfigReader: Could not read secure formatter ", e);
        }
        
        // db formatter class
        try {
            key = LogConstants.DB_FORMATTER;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            sbuffer.append(key).append("=")
                   .append(value).append(LogConstants.CRLF);
        }   catch (Exception e) {
            debug.error("LogConfigReader: Could not read dbformatter ", e);
        }
        // db formatter class
        try {
            key = LogConstants.REMOTE_FORMATTER;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            sbuffer.append(key).append("=")
                   .append(value).append(LogConstants.CRLF);
        }   catch (Exception e) {
            debug.error("LogConfigReader: Could not read remoteformatter ", e);
        }
        // authz class
        try {
            key = LogConstants.AUTHZ;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader: AUTHZ string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        }   catch (Exception e) {
            debug.error("LogConfigReader: Could not read authz class", e);
        }

        getLoggingDirectory(fileBackend, basedir, famuri, sbuffer);

        // security status (on or off)
        try {
            key = LogConstants.SECURITY_STATUS;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning(
                    "LogConfigReader: Security status string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        } catch (Exception e) {
            debug.error("LogConfigReader: Could not read security status ", e);
        }

        // secure log signing algorithm name 
        // MD2withRSA, MD5withRSA, SHA1withDSA, SHA1withRSA
        try {
            key = LogConstants.SECURITY_SIGNING_ALGORITHM;
            value = CollectionHelper.getMapAttr(logAttributes, key, 
                LogConstants.DEFAULT_SECURITY_SIGNING_ALGORITHM);
            sbuffer.append(key).append("=")
                .append(value).append(LogConstants.CRLF);
        } catch (Exception e) {
            debug.error("LogConfigReader: Could not read secure " +
                "log signing alogorithm ", e);
        }

        // secure log helper class name 
        // com.sun.identity.log.secure.impl.SecureLogHelperJSSImpl or 
        // com.sun.identity.log.secure.impl.SecureLogHelperJCEImpl
        try {
            key = LogConstants.SECURE_LOG_HELPER;
            value = CollectionHelper.getMapAttr(logAttributes, key, 
                LogConstants.SECURE_DEFAULT_LOG_HELPER);
            sbuffer.append(key).append("=")
                   .append(value).append(LogConstants.CRLF);
        } catch (Exception e) {
            debug.error("LogConfigReader: Could not read secure " +
                "log helper class name ", e);
        }
        
        // secure logger certificate store
        try {
            key = LogConstants.LOGGER_CERT_STORE;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader: secure logger " +
                    "certificate store is null");
            } else {
                value = value.replace('\\','/');
                if (value.contains("%BASE_DIR%") ||
                    value.contains("%SERVER_URI%"))
                {
                    value = value.replaceAll("%BASE_DIR%", basedir);
                    value = value.replaceAll("%SERVER_URI%", famuri);
                }
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        } catch (Exception e) {
            debug.error("LogConfigReader: Could not read secure " +
                "logger certificate store ", e);
        }
        // log verification period in seconds
        try {
            key = LogConstants.LOGVERIFY_PERIODINSECONDS;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader: Verify period string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        }   catch (Exception e) {
            debug.error("LogConfigReader: Could not read logverify period", e);
        }
        // log signing period in seconds
        try {
            key = LogConstants.LOGSIGN_PERIODINSECONDS;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader: sign period string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        }   catch (Exception e) {
            debug.error("LogConfigReader: Could not read sign fieldname ", e);
        }
        // file read handler class
        try {
            key = LogConstants.FILE_READ_HANDLER;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader: file readhandler " +
                    "string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        }   catch (Exception e) {
            debug.error("LogConfigReader: Could not read " +
                "filehandler class ", e);
        }
        // DB read handler class
        try {
            key = LogConstants.DB_READ_HANDLER;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader: DB readhandler string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        }   catch (Exception e) {
            debug.error("LogConfigReader: could not read DBreadhandler class ",
                e);
        }
        // MAX_RECORDS
        try {
            key = LogConstants.MAX_RECORDS;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader: Max records string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        }   catch (Exception e) {
            debug.error("LogConfigReader: Could not read max-records ", e);
        }
        // FILES_PER_KEYSTORE
        try {
            key = LogConstants.FILES_PER_KEYSTORE;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader: filesper " +
                    "keystore string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        }   catch (Exception e) {
            debug.error("LogConfigReader:Could not read files per keystore", e);
        }
        // Token Generating Class
        try {
            key = LogConstants.TOKEN_PROVIDER;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader: token provider string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        } catch(Exception e) {
            debug.error("LogConfigReader:Could not read Token " +
                "Generation Class name");
        }
        // Secure Timestamp generator class
        try {
            key = LogConstants.SECURE_TIMESTAMP_GENERATOR;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader: timestamp " +
                    "generator string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        } catch(Exception e) {
            debug.error("LogConfigReader:Could not read Token " +
                "Generation Class name");
        }
        // Verifier Action Output Class
        try {
            key = LogConstants.VERIFIER_ACTION_CLASS;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader: verifier " +
                    "actionclass string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        } catch(Exception e) {
            debug.error("LogConfigReader:Could not read verifier " +
                "output Class name");
        }
        // filter class name
        try {
            key = LogConstants.FILTER_CLASS_NAME;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader: filter class " +
                    "name string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        } catch(Exception e) {
            debug.error("LogConfigReader:Could not read filter class");
        }
        // debug Implementation Class
        try {
            key = LogConstants.DEBUG_IMPL_CLASS;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.error("LogConfigReader: debug implclass string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        } catch(Exception e) {
            debug.error("LogConfigReader:Could not read debug Impl Class name");
        }
        
        // Buffer size
        try {
            key = LogConstants.BUFFER_SIZE;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader: buffer size string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        } catch(Exception e) {
            debug.error("LogConfigReader:Could not read buf size");
        }

        // Max DB Mem Buffer size
        try {
            key = LogConstants.DB_MEM_MAX_RECS;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() == 0)) {
                debug.warning(
                "LogConfigReader: Max DB mem buffer size string is null");
            } else {
                sbuffer.append(key).append("=").append(value).
                    append(LogConstants.CRLF);
            }
        } catch(Exception e) {
            debug.error("LogConfigReader:Could not read max db mem buf size");
        }

        // Buffer Time
        try {
            key = LogConstants.BUFFER_TIME;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader: buffer time string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        } catch(Exception e) {
            debug.error("LogConfigReader:Could not read buf time");
        }
        // Time Buffering Status
        try {
            key = LogConstants.TIME_BUFFERING_STATUS;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader: time " +
                    "buffering status string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        } catch(Exception e) {
            debug.error("LogConfigReader:Could not read time " +
                "buffering status ");
        }

        // Oracle DB data type for DATA field
        try {
            key = LogConstants.ORA_DBDATA_FIELDTYPE;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            sbuffer.append(key).append("=")
                .append(value).append(LogConstants.CRLF);
        } catch(Exception e) {
            debug.error("LogConfigReader:Could not read ORA DB data type");
        }

        // MySQL DB data type for DATA field
        try {
            key = LogConstants.MYSQL_DBDATA_FIELDTYPE;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            sbuffer.append(key).append("=")
                .append(value).append(LogConstants.CRLF);
        } catch(Exception e) {
            debug.error("LogConfigReader:Could not read MYSQL DB data type");
        }

        // DB date/time formats (Oracle and MySQL)
        try {
            key = LogConstants.ORA_DBDATETIME_FORMAT;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            sbuffer.append(key).append("=")
                .append(value).append(LogConstants.CRLF);
        } catch(Exception e) {
            debug.error(
                "LogConfigReader:Could not read Oracle DB date/time format");
        }

        try {
            key = LogConstants.MYSQL_DBDATETIME_FORMAT;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            sbuffer.append(key).append("=")
                .append(value).append(LogConstants.CRLF);
        } catch(Exception e) {
            debug.error(
                "LogConfigReader:Could not read MySQl DB date/time format");
        }

        // Log status from the logging config
        try {
            key = LogConstants.LOG_STATUS_ATTR;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() == 0)) {
                debug.warning("LogConfigReader:reading from SystemProperties");
                // try to read from AMConfig.properties
                value = SystemProperties.get(LogConstants.LOG_STATUS);
                if (debug.messageEnabled()) {
                    debug.message("####### SystemProperties logStatus is: " +
                        value);
                }
                if ((value == null) || (value.length() == 0)) {
                    value = "ACTIVE";
                }
            }
            // "value" will have a value
            sbuffer.append(key).append("=").append(value).
                append(LogConstants.CRLF);
        } catch(Exception e) {
            debug.error("LogConfigReader:Could not read Log Status attribute");
        }

        // Logging Level attribute
        try {
            key = LogConstants.LOGGING_LEVEL;
            value = CollectionHelper.getMapAttr(logAttributes, key,
                LogConstants.DEFAULT_LOGGING_LEVEL_STR);
            sbuffer.append(key).append("=")
                .append(value).append(LogConstants.CRLF);
        } catch(Exception e) {
            debug.error("LogConfigReader:Could not read Logging Level");
        }

        // processing platform attributes
        try {
            platformAttributes = smsPlatformSchema.getAttributeDefaults();
            key = LogConstants.LOCALE;
            value = CollectionHelper.getMapAttr(platformAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader: locale string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        }   catch (Exception e) {
            debug.error("LogConfigReader: Could not read platform ", e);
        }
        // processing naming attributes
        try {
            namingAttributes = smsNamingSchema.getAttributeDefaults();
            key = LogConstants.LOGGING_SERVICE_URL;
            value = CollectionHelper.getMapAttr(namingAttributes, key);
            if ((value == null) || (value.length() ==0)) {
                debug.warning("LogConfigReader: loggins " +
                    "service url string is null");
            } else {
                sbuffer.append(key).append("=")
                       .append(value).append(LogConstants.CRLF);
            }
        } catch (Exception e) {
            debug.error("LogConfigReader: could not get from DS", e);
        }
        // hostname resolution
        try {
            key = LogConstants.LOG_RESOLVE_HOSTNAME_ATTR;
            value = CollectionHelper.getMapAttr(logAttributes, key);
            if ((value == null) || (value.length() == 0)) {
                debug.warning("LogConfigReader: " +
                    "Log Resolve Hostname attribute is null");
                // try to read from AMConfig.properties
                value =
                    SystemProperties.get(LogConstants.LOG_RESOLVE_HOSTNAME);
                if (debug.messageEnabled()) {
                    debug.message(
                        "####### SystemProperties resolveHostName is: " +
                        value);
                }
                if ((value == null) || (value.length() == 0)) {
                    value = "false";
                }
            }
            // "value" will have a value
            sbuffer.append(key).append("=")
                .append(value).append(LogConstants.CRLF);
        } catch (Exception e) {
            debug.error("LogConfigReader: could not get from DS", e);
        }
        return sbuffer.toString();
    }

    class LogHeaderComparator implements Comparator {
        /**
         * Compares two strings from the Log headers. Names should either be
         * in the form ##:HeaderName or HeaderName, where ## is a two digit 
         * number. Instances with ##: preceding will go first, in ascending
         * order according to the two digit number. If two of the same number
         * appear they will be ordered according to the order of in which they
         * were compared.
         *
         * @param o1 First object in comparison
         * @param o2 Second object in comparison
         * @return Returns positive if prefix for o2 is gt o1, otherwise negative
         */
        public int compare(Object o1, Object o2) {
            String s1 = (String) o1;
            String s2 = (String) o2;

            if (s1.contains(Constants.COLON) && s2.contains(Constants.COLON)) {
                int s1v = Integer.parseInt(s1.substring(0, s1.indexOf(Constants.COLON)));
                int s2v = Integer.parseInt(s2.substring(0, s2.indexOf(Constants.COLON)));

                if (s1v < s2v) {
                    return 1;
                } else {
                    return -1;
                }
            } else if (s1.contains(Constants.COLON)) {
                return 1;
            } else if (s2.contains(Constants.COLON)) {
                return -1;
            }

            return -1;
        }

        public boolean equals(Object obj) {
            return obj instanceof LogHeaderComparator;
        }
    }

    private void getLoggingDirectory(
        boolean fileBackend,
        String basedir,
        String famuri,
        StringBuilder sbuffer
    ) {
        String key = LogConstants.LOG_LOCATION;

        String logDir = SystemProperties.get(LogConstants.SYS_PROP_LOG_DIR);
        if ((logDir != null) && (logDir.trim().length() > 0)) {
            logDir = logDir.replace('\\','/');
            logDir += "/";
            sbuffer.append(key).append("=")
                .append(logDir).append(LogConstants.CRLF);
            return;
        }

        /*
         *  log location subdirectory
         *  is specified in AMConfig.properties.  read it here and append
         *  to log location, so only have to deal with it here.
         */
        String locSubdir = null;
        if (fileBackend) {
            locSubdir = SystemProperties.get(LogConstants.LOG_LOCATION_SUBDIR);
            if ((locSubdir != null) &&
                (locSubdir.trim().length() > 0) &&
                (!locSubdir.endsWith("/")))
            {
                locSubdir += "/";
            }
        }

        String value = CollectionHelper.getMapAttr(logAttributes, key);
        if ((value == null) || (value.length() ==0)) {
            debug.warning("LogConfigReader: LogLocation string is null");
        } else {
            value = value.replace('\\','/');
            if (value.contains("%BASE_DIR%") ||
                    value.contains("%SERVER_URI%"))
            {
                value = value.replaceAll("%BASE_DIR%", basedir);
                value = value.replaceAll("%SERVER_URI%", famuri);
            }
            if (fileBackend && !value.endsWith("/")) {
                value += "/";
            }
            if ((locSubdir != null) && (locSubdir.trim().length() > 0)) {
                // locSubdir already ensured trailing slash, above
                value += locSubdir;
            }
            sbuffer.append(key).append("=")
                .append(value).append(LogConstants.CRLF);
        }
    }
    
    /**
     * This method is used to get the global schemas of Logging, Platform
     * and Naming Services. Platform service schema is used to determine the
     * platform locale, naming service is used to get the logging service url.
     */
    private void getDefaultAttributes(SSOToken ssoToken)
    throws SMSException, SSOException {
        
        ServiceSchemaManager schemaManager =
        new ServiceSchemaManager("iPlanetAMLoggingService", ssoToken);
        smsLogSchema = schemaManager.getGlobalSchema();
        if (!isRegisteredForDSEvents) {
            schemaManager.addListener(this);
        }
        schemaManager =
        new ServiceSchemaManager("iPlanetAMPlatformService", ssoToken);
        if (!isRegisteredForDSEvents) {
            schemaManager.addListener(this);
        }
        smsPlatformSchema = schemaManager.getGlobalSchema();
        schemaManager =
        new ServiceSchemaManager("iPlanetAMNamingService", ssoToken);
        if (!isRegisteredForDSEvents) {
            schemaManager.addListener(this);
            isRegisteredForDSEvents = true;
        }
        smsNamingSchema = schemaManager.getGlobalSchema();
        
        // get the default attributes of each service(Logging, Platform and
        // Naming).
        logAttributes           = smsLogSchema.getAttributeDefaults();
        platformAttributes      = smsPlatformSchema.getAttributeDefaults();
        namingAttributes        = smsNamingSchema.getAttributeDefaults();
    }
    
    /**
     * This method is used for gettting the SSOToken from the
     * TokenManager using Principal and defaultOrg. Need to
     * figure out a better method and/or to confirm
     * whether the existing method is good enough.
     * This method of obtaining token has problems in DSAME5.2 alpha(hanging)
     * Written on 25/4/2002 for DSAME6.0
     * @throws SMSException
     * @throws SSOException
     */
    private SSOToken getSSOToken() throws SSOException {
        return (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    }
    
    /**
     * This method checks whether the logging service url is explicitly 
     * mentioned in the naming service. If yes then validates the URL against 
     * the platform server list of trusted servers. if the logging service 
     * url is not mentioned explicitly it sets the local flag to true.
     */
    private void setLocalFlag() {
        if (debug.messageEnabled()) {
            debug.message("LogConfigReader: logserviceID is" 
                + localLogServiceID);
        }
        try{
            // can't do this here because NamingService is not
            // dynamically updated.
            // URL url =  WebtopNaming.getServiceURL(LOGGING_SERVICE,
            // protocol, host, port);
            
            String urlString = 
                manager.getProperty(LogConstants.LOGGING_SERVICE_URL);
            String logHost = null;
            if (urlString.indexOf("%") == -1) {
                logHost = urlString;
            } else {
                logHost = localLogServiceID;
            }
            if ((localLogServiceID) != null && (logHost != null)) {
                if (logHost.startsWith(localLogServiceID))
                {
                    LogManager.isLocal = true;
                }
                else {
                    LogManager.isLocal = false;
                }
            }
        } catch (Exception e) {
            debug.error("LogConfigReader: Error setting localFlag: ",e);
        }
    }
    
    // following methods
    // to implement ServiceListener
    
    public void globalConfigChanged(
        String servName,
        String ver,
        String frpName,
        String servComp,
        int type
    ) {
        debug.message("Global config change");
    }
    
    public void organizationConfigChanged(
        String servName,
        String ver,
        String orgName,
        String grpName,
        String servComp,
        int type
    ) {
        debug.message("Org config change");
    }
    
    public void schemaChanged(String servName,String ver) {
        
        if (debug.messageEnabled()) {
            debug.message("LogService schemaChanged(): ver = " + ver);
        }

        /*
         *  if logging config has been read before (i.e., eliminating
         *  the case where logging status is inactive on startup),
         *  then if logging status goes from active to inactive,
         *  force a write of a record (to each active log file)
         *  indicating the change.  can't really do it in LogManager,
         *  as the java.util.LogManager (below) has already set
         *  the logging status to inactive, and no more records will
         *  get written out.
         */
        manager = 
            (com.sun.identity.log.LogManager) LogManagerUtil.getLogManager();
        if (manager.getDidFirstReadConfig() &&
            manager.getLoggingStatusIsActive() &&
            newStatusIsInactive())
        {
            manager.logStopLogs();
        }
        //shifting to LogManager according to review.
        try{
            manager.readConfiguration();
        } catch (Exception e) {
            debug.error("Error in readConfiguration()",e);
        }
    }

    private boolean newStatusIsInactive() {
        SSOToken ssoToken;
        try {
            ssoToken = getSSOToken();
        } catch (SSOException ssoe) {
            debug.error("LogConfigReader:newStatusIsInactive:" +
                "Could not get proper SSOToken", ssoe);
            return false;
        }
        try {
            ServiceSchemaManager schemaManager =
                new ServiceSchemaManager("iPlanetAMLoggingService", ssoToken);
            ServiceSchema smsLogSchema = schemaManager.getGlobalSchema();
            Map sss = smsLogSchema.getAttributeDefaults();

            String key = LogConstants.LOG_STATUS_ATTR;
            String value = CollectionHelper.getMapAttr(sss, key);
            if ((value == null) || (value.length() == 0)) {
                value = "ACTIVE";
            }
            return (value.equalsIgnoreCase("INACTIVE"));
        } catch(Exception e) {
            debug.error("LogConfigReader:newStatusIsInactive:" +
                "error reading Log Status attribute: " + e.getMessage());
        }
        return false;
    }
}
