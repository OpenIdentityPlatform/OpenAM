/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SessionService.java,v 1.37 2010/02/03 03:52:54 bina Exp $
 *
 * Portions Copyrighted 2010-2016 ForgeRock AS.
 * Portions Copyrighted 2022 Open Identity Platform Community
 * Portions copyright 2025 3A Systems LLC.
 */

package com.iplanet.dpro.session.service;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Date;
import java.util.StringTokenizer;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.forgerock.openam.session.SessionEventType;
import org.forgerock.openam.utils.StringUtils;

import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.sso.SSOToken;
import com.sun.identity.log.LogConstants;
import com.sun.identity.log.LogRecord;
import com.sun.identity.log.Logger;
import com.sun.identity.log.messageid.LogMessageProvider;
import com.sun.identity.log.messageid.MessageProviderFactory;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

/**
 * Responsible for logging Session events to the audit logs amSSO.access and amSSO.error.
 *
 * @since 13.0.0
 */
@Singleton
public class SessionLogging implements InternalSessionListener {

    private static final String LOG_PROVIDER = "Session";
    private static final String AM_SSO_ACCESS_LOG_FILE = "amSSO.access";
    private static final String AM_SSO_ERROR_LOG_FILE = "amSSO.error";
    private static final String HOST_PROP = "Host";
    private static final String HOSTNAME_PROP = "HostName";

    private final Debug sessionDebug;
    private final SessionServiceConfig serviceConfig;
    private final PrivilegedAction<SSOToken> adminTokenAction;
    private volatile Logger logger = null; // lazily initialized via double-checked locking
    private volatile Logger errorLogger = null; // lazily initialized via double-checked locking
    private volatile LogMessageProvider logProvider = null; // lazily initialized via double-checked locking

    @Inject
    public SessionLogging(
            @Named(SessionConstants.SESSION_DEBUG) Debug sessionDebug,
            SessionServiceConfig serviceConfig,
            PrivilegedAction<SSOToken> adminTokenAction) {

        this.sessionDebug = sessionDebug;
        this.serviceConfig = serviceConfig;
        this.adminTokenAction = adminTokenAction;
    }

    @Override
    public void onEvent(final InternalSessionEvent event) {
        logEvent(event.getInternalSession().toSessionInfo(), event.getType(), event.getTime());
    }

    /**
     * Log the event based on the values contained in the SessionInfo
     *
     * @param sessionInfo SessionInfo
     * @param eventType event type.
     */
    public void logEvent(SessionInfo sessionInfo, SessionEventType eventType, long timestamp) {
        switch (eventType) {
            case SESSION_CREATION:
                logEvent(sessionInfo, "SESSION_CREATED", timestamp);
                break;
            case IDLE_TIMEOUT:
                logEvent(sessionInfo, "SESSION_IDLE_TIMED_OUT", timestamp);
                break;
            case MAX_TIMEOUT:
                logEvent(sessionInfo, "SESSION_MAX_TIMEOUT", timestamp);
                break;
            case LOGOUT:
                logEvent(sessionInfo, "SESSION_LOGOUT", timestamp);
                break;
            case DESTROY:
                logEvent(sessionInfo, "SESSION_DESTROYED", timestamp);
                break;
            case PROPERTY_CHANGED:
                logEvent(sessionInfo, "SESSION_PROPERTY_CHANGED", timestamp);
                break;
            case EVENT_URL_ADDED:
                logEvent(sessionInfo, "SESSION_EVENT_URL_ADDED", timestamp);
                break;
            case QUOTA_EXHAUSTED:
                logEvent(sessionInfo, "SESSION_QUOTA_EXHAUSTED", timestamp);
                break;
            case PROTECTED_PROPERTY:
                logEvent(sessionInfo, "SESSION_PROTECTED_PROPERTY_ERROR", timestamp);
                break;
            default:
                logEvent(sessionInfo, "SESSION_UNKNOWN_EVENT", timestamp);
        }

    }

    private void logEvent(SessionInfo sessionInfo, String logMessageId, long timestamp) {
        try {
            String clientID = sessionInfo.getClientID();
            String uidData;
            if (StringUtils.isEmpty(clientID)) {
                uidData = "N/A";
            } else {
                StringTokenizer st = new StringTokenizer(clientID, ",");
                uidData = (st.hasMoreTokens()) ? st.nextToken() : clientID;
            }
            String[] data = {uidData};
            LogRecord lr = getLogMessageProvider().createLogRecord(logMessageId, data, null);
            lr.addTimeLogInfo(new Date(timestamp));
            lr.addLogInfo(LogConstants.LOGIN_ID_SID, sessionInfo.getSessionID());
            lr.addLogInfo(LogConstants.CONTEXT_ID, sessionInfo.getProperties().get(Constants.AM_CTX_ID));
            lr.addLogInfo(LogConstants.LOGIN_ID, clientID);
            lr.addLogInfo(LogConstants.LOG_LEVEL, lr.getLevel().toString());
            lr.addLogInfo(LogConstants.DOMAIN, sessionInfo.getClientDomain());
            lr.addLogInfo(LogConstants.IP_ADDR, sessionInfo.getProperties().get(HOST_PROP));
            lr.addLogInfo(LogConstants.HOST_NAME, sessionInfo.getProperties().get(HOSTNAME_PROP));
            getLogger().log(lr, AccessController.doPrivileged(adminTokenAction));
        } catch (Exception ex) {
            sessionDebug.error("SessionService.logIt(): Cannot write to the session log file: ", ex);
        }
    }

    private Logger getLogger() {
        // TODO: Establish if this really needs to be lazily initialized
        //       a final field that can be dependency injected would be preferable
        if (logger == null) {
            synchronized (this) {
                if (logger == null) {
                    logger = (Logger) Logger.getLogger(AM_SSO_ACCESS_LOG_FILE);
                }
            }
        }
        return logger;
    }

    private Logger getErrorLogger() {
        // TODO: Establish if this really needs to be lazily initialized
        //       a final field that can be dependency injected would be preferable
        if (errorLogger == null) {
            synchronized (this) {
                if (errorLogger == null) {
                    errorLogger = (Logger) Logger.getLogger(AM_SSO_ERROR_LOG_FILE);
                }
            }
        }
        return errorLogger;
    }

    private LogMessageProvider getLogMessageProvider() throws Exception {
        // TODO: Establish if this really needs to be lazily initialized
        //       a final field that can be dependency injected would be preferable
        if (logProvider == null) {
            synchronized (this) {
                if (logProvider == null) {
                    logProvider = MessageProviderFactory.getProvider(LOG_PROVIDER);
                }
            }
        }
        return logProvider;
    }
}
