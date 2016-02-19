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
 * $Id: LogRecord.java,v 1.7 2009/03/05 22:55:37 veiming Exp $
 *
 */

package com.sun.identity.log;

import com.iplanet.sso.SSOException;
import com.sun.identity.log.spi.Debug;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;

/**
 * Extension to the JDK1.4 <code>LogRecord</code> to include the
 * <code>logInfo</code> <code>HashMap</code> and methods to store and retrieve
 * data from this <code>logInfo</code> Map. The <code>logInfo</code> Map is
 * supposed to be used by the client to fill in log-details which
 * will be used by the Formatter to construct the actual log string.
 *
 * For <code>JDK1.4</code> <code>LogRecord</code> please refer to 
 * <pre>
 * http://java.sun.com/j2se/1.4.1/docs/api/java/util/logging/LogRecord.html
 * </pre>
 * @supported.api
 */
public class LogRecord extends java.util.logging.LogRecord
    implements ILogRecord
{
    private Map logInfoMap = new HashMap();
    private Object token;

    /**
     * Construct the <code>LogRecord</code> with the given Level and message
     * values.
     *
     * @param level The log Level
     * @param msg The message string
     *
     * @supported.api
     */
    public LogRecord(Level level, String msg) {
        super(level,msg);
    }

    /**
     * Construct the <code>LogRecord</code> with the given Level and message
     * values.
     *
     * @param level The log Level.
     * @param msg The message string.
     * @param token The single sign-on token which will be used to fill in
     *        details like client IP address into the <code>LogRecord</code>.
     * @supported.api
     */
    public LogRecord(Level level, String msg, Object token) {
        this(level,msg);
        this.token = token;

        try {
            Logger.extractInfoFromLogFor(this);
        } catch (SSOException se) {
            /*
             *  internal auth session doesn't have IPaddr, so stacktrace
             *  was filling up amLog debug file.
             */
            Debug.error("LogRecord:LogRecord:SSOException: " + se.getMessage());
        }
    }

    /**
     * Constructor for auth logging
     * @param level The log Level.
     * @param msg The message string.
     * @param properties The Hashtable containing the properties
     *        for the LogRecord.
     */

    public LogRecord(Level level, String msg, Hashtable properties) {
        this(level,msg);
        String clientDomain = (String)properties.get(LogConstants.DOMAIN);
        String clientID     = (String)properties.get(LogConstants.LOGIN_ID);
        String ipAddress    = (String)properties.get(LogConstants.IP_ADDR); 
        String loginIDSid   = (String)properties.get(LogConstants.LOGIN_ID_SID);
        String moduleName   = (String)properties.get(LogConstants.MODULE_NAME);
        String contextID    = (String)properties.get(LogConstants.CONTEXT_ID);
        String messageID    = (String)properties.get(LogConstants.MESSAGE_ID);
        String nameID       = (String)properties.get(LogConstants.NAME_ID);
        String hostName = ipAddress;
        if (ipAddress != null) {
            try {
                if (Logger.resolveHostName) {
                    hostName =
                        java.net.InetAddress.getByName(ipAddress).getHostName();
                } else {
                    hostName = ipAddress;
                }
            } catch (Exception e) {
               Debug.error("LogRecord:LogRecord:Unable to get Host for:" +
                   ipAddress);
            }
        }
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        /*
         * these are the compulsory fields ... to be logged even if there are
         * exceptions while getting domain, loginid, ipaddr, hostname
         */
        addLogInfo(LogConstants.TIME, sdf.format(date));
        addLogInfo(LogConstants.DATA, getMessage());
        addLogInfo(LogConstants.LOG_LEVEL, getLevel().toString());
        addLogInfo(LogConstants.DOMAIN, clientDomain);
        addLogInfo(LogConstants.LOGIN_ID, clientID);
        addLogInfo(LogConstants.IP_ADDR, ipAddress);
        addLogInfo(LogConstants.HOST_NAME, hostName);
        addLogInfo(LogConstants.LOGIN_ID_SID, loginIDSid);
        addLogInfo(LogConstants.MODULE_NAME, moduleName);
        /* if they're implemented... */
        if ((messageID != null) && (messageID.length() > 0)) {
            addLogInfo(LogConstants.MESSAGE_ID, messageID);
        }
        if ((contextID != null) && (contextID.length() > 0)) {
            addLogInfo(LogConstants.CONTEXT_ID, contextID);
        }
        if ((nameID != null) && (nameID.length() > 0)) {
            addLogInfo(LogConstants.NAME_ID, nameID);
        }
    }

    /**
     * Adds to the log information map, the field key and its corresponding
     * value.
     *
     * @param key The key which will be used by the formatter to determine if
     *        this piece of info is supposed to be added to the log string
     *        according to the selected log fields.
     * @param value The value which may form a part of the actual log-string.
     * @supported.api
     */
    public void addLogInfo(String key,Object value) {
        logInfoMap.put(key,value);
    }
    
    /**
     * Convenience method to set the log information map.
     *
     * @param logInfoMap Handler to the map which contains the log info
     * @supported.api
     */
    public void setLogInfoMap(Map logInfoMap) {
        this.logInfoMap = logInfoMap;
    }
    /**
     * Returns the log information map which contains the set of fields and
     * their corresponding values.
     *
     * @return The log information map.
     * @supported.api
     */
    public Map getLogInfoMap() {
        return logInfoMap;
    }

    /**
     * Returns log by subject.
     *
     * @return log by subject.
     */
    public Object getLogBy() {
        return null;
    }

    /**
     * Returns log for subject.
     *
     * @return log for subject.
     */
    public Object getLogFor() {
        return token;
    }

}
