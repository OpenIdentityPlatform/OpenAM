/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SsoServerLoggingSvcImpl.java,v 1.3 2009/12/09 00:33:03 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.monitoring;

import com.sun.identity.shared.debug.Debug;
import com.sun.management.snmp.agent.SnmpMib;
import com.sun.management.snmp.SnmpStatusException;
import java.util.HashMap;
import java.util.Map;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * This class extends the "SsoServerLoggingSvc" class.
 */
public class SsoServerLoggingSvcImpl extends SsoServerLoggingSvc {
    private static Debug debug = null;
    private boolean isBogus = true;
    private static SsoServerLoggingHdlrEntryImpl lg_dbh = null;
    private static SsoServerLoggingHdlrEntryImpl lg_fh = null;
    private static SsoServerLoggingHdlrEntryImpl lg_sfh = null;
    private static SsoServerLoggingHdlrEntryImpl lg_rh = null;
    public static final String DB_HANDLER_NAME = "DB Handler";
    public static final String FILE_HANDLER_NAME = "File Handler";
    public static final String SECURE_FILE_HANDLER_NAME = "Secure File Handler";
    public static final String REMOTE_HANDLER_NAME = "Remote Handler";

    private Map<String, SsoServerLoggingHdlrEntryImpl> handlerMap =
            new HashMap<String, SsoServerLoggingHdlrEntryImpl>();

    /**
     * Constructor
     */
    public SsoServerLoggingSvcImpl (SnmpMib myMib) {
        super(myMib);
        init(myMib, null);
    }

    public SsoServerLoggingSvcImpl (SnmpMib myMib, MBeanServer server) {
        super(myMib, server);
        init(myMib, server);
    }

    private void init(SnmpMib myMib, MBeanServer server) {
        if (debug == null) {
            debug = Debug.getInstance("amMonitoring");
        }
        String classModule = "SsoServerLoggingServiceImpl.init:";
        if (isBogus) {
            int ind = 1;
            // DB Handler
            lg_dbh = new SsoServerLoggingHdlrEntryImpl(myMib);
            lg_dbh.LoggingHdlrName = DB_HANDLER_NAME;
            lg_dbh.LoggingHdlrIndex = new Integer(ind++);

            final ObjectName dbhName =
                    lg_dbh.createSsoServerLoggingHdlrEntryObjectName(server);
            try {
                SsoServerLoggingHdlrTable.addEntry(lg_dbh, dbhName);
                if ((server != null) && (dbhName != null)) {
                    server.registerMBean(lg_dbh, dbhName);
                }
                handlerMap.put(DB_HANDLER_NAME, lg_dbh);
            } catch (JMException ex) {
                debug.error(classModule + DB_HANDLER_NAME, ex);
            } catch (SnmpStatusException ex) {
                debug.error(classModule + DB_HANDLER_NAME, ex);
            }

            // File Handler
            lg_fh = new SsoServerLoggingHdlrEntryImpl(myMib);
            lg_fh.LoggingHdlrName = FILE_HANDLER_NAME;
            lg_fh.LoggingHdlrIndex = new Integer(ind++);

            final ObjectName fhName =
                    lg_fh.createSsoServerLoggingHdlrEntryObjectName(server);
            try {
                SsoServerLoggingHdlrTable.addEntry(lg_fh, fhName);
                if ((server != null) && (fhName != null)) {
                    server.registerMBean(lg_fh, fhName);
                }
                handlerMap.put(FILE_HANDLER_NAME, lg_fh);
            } catch (JMException ex) {
                debug.error(classModule + FILE_HANDLER_NAME, ex);
            } catch (SnmpStatusException ex) {
                debug.error(classModule + FILE_HANDLER_NAME, ex);
            }

            // Secure File Handler
            lg_sfh = new SsoServerLoggingHdlrEntryImpl(myMib);
            lg_sfh.LoggingHdlrName = SECURE_FILE_HANDLER_NAME;
            lg_sfh.LoggingHdlrIndex = new Integer(ind++);

            final ObjectName sfhName =
                    lg_sfh.createSsoServerLoggingHdlrEntryObjectName(server);
            try {
                SsoServerLoggingHdlrTable.addEntry(lg_sfh, sfhName);
                if ((server != null) && (sfhName != null)) {
                    server.registerMBean(lg_sfh, sfhName);
                }
                handlerMap.put(SECURE_FILE_HANDLER_NAME, lg_sfh);
            } catch (JMException ex) {
                debug.error(classModule + SECURE_FILE_HANDLER_NAME, ex);
            } catch (SnmpStatusException ex) {
                debug.error(classModule + SECURE_FILE_HANDLER_NAME, ex);
            }

            // Remote Handler
            lg_rh = new SsoServerLoggingHdlrEntryImpl(myMib);
            lg_rh.LoggingHdlrName = REMOTE_HANDLER_NAME;
            lg_rh.LoggingHdlrIndex = new Integer(ind++);

            final ObjectName rhName =
                    lg_rh.createSsoServerLoggingHdlrEntryObjectName(server);
            try {
                SsoServerLoggingHdlrTable.addEntry(lg_rh, rhName);
                if ((server != null) && (rhName != null)) {
                    server.registerMBean(lg_rh, rhName);
                }
                handlerMap.put(REMOTE_HANDLER_NAME, lg_rh);
            } catch (JMException ex) {
                debug.error(classModule + REMOTE_HANDLER_NAME, ex);
            } catch (SnmpStatusException ex) {
                debug.error(classModule + REMOTE_HANDLER_NAME, ex);
            }
        }
    }

    /**
     * Setter for the "LoggingLoggers" variable.
     */
    public void setSsoServerLoggingLoggers(Integer l) {
        LoggingLoggers = l;
    }

    /**
     * Setter for the "LoggingBufferSize" variable.
     */
    public void setSsoServerLoggingBufferSize(long l) {
        LoggingBufferSize = l;
    }

    /**
     * Setter for the "LoggingBufferTime" variable.
     */
    public void setSsoServerLoggingBufferTime(long l) {
        LoggingBufferTime = l;
    }

    /**
     * Setter for the "LoggingTimeBuffering" variable.
     */
    public void setSsoServerLoggingTimeBuffering(String s) {
        LoggingTimeBuffering = s;
    }

    /**
     * Setter for the "LoggingSecure" variable.
     */
    public void setSsoServerLoggingSecure(String s) {
        LoggingSecure = s;
    }

    /**
     * Setter for the "LoggingNumberHistoryFiles" variable.
     */
    public void setSsoServerLoggingNumberHistoryFiles(long l) {
        LoggingNumHistFiles = l;
    }

    /**
     * Setter for the "LoggingMaxLogSize" variable.
     */
    public void setSsoServerLoggingMaxLogSize(long l) {
        LoggingMaxLogSize = l;
    }

    /**
     * Setter for the "LoggingLocation" variable.
     */
    public void setSsoServerLoggingLocation(String s) {
        LoggingLocation = s;
    }

    /**
     * Setter for the "LoggingType" variable.
     */
    public void setSsoServerLoggingType(String s) {
        LoggingType = s;
    }

    /**
     * Setter for the "LoggingRecsRejected" variable.
     */
    public void setSsoServerLoggingRecsRejected(long l) {
        LoggingRecsRejected = l;
    }

    /**
     * Incrementer for the "LoggingRecsRejected" variable.
     */
    public void incSsoServerLoggingRecsRejected() {
        long li = LoggingRecsRejected.longValue();
        li++;
        LoggingRecsRejected = Long.valueOf(li);
    }

    /**
     *  Provides the handle to the specific handler
     *  eg. DBHandler, FileHandler etc.
     *  @param handlerName String which acts as key to retrieve handler
     *                     from the map. The allowed keys are DB_HANDLER_NAME,
     *                     FILE_HANDLER_NAME etc.
     *  @return A null value may mean following:
     *          1) The Agent is not running.
     *          2) The parameter 'handlerName' is blank or null.
     *          3) There is no entry in the handler map for the key handlerName
     *             i.e. it is an invalid key.
     */
    public SsoServerLoggingHdlrEntryImpl getHandler(String handlerName) {
        String classMethod = "SsoServerLoggingSvcImpl.getHandler:";

        if ((handlerName != null) && (handlerName.length() > 0)) {
            SsoServerLoggingHdlrEntryImpl handler = handlerMap.get(handlerName);
            if (handler != null) {
                return handler;
            } else {
                return null;
            }
        } else {
            if (debug.warningEnabled()) {
                debug.warning(classMethod + "no handler name provided");
            }
            return null;
        }
    }
}
