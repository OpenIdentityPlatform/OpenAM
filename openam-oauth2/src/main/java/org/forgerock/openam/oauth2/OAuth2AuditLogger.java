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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.oauth2;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.sun.identity.log.LogRecord;
import com.sun.identity.log.Logger;
import com.sun.identity.log.messageid.LogMessageProvider;
import com.sun.identity.log.messageid.MessageProviderFactory;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.oauth2.core.OAuth2Constants;

import javax.inject.Inject;
import java.io.IOException;
import java.security.AccessController;

/**
 * Audit logger for OAuth2 operations.
 *
 * @since 12.0.0
 */
public class OAuth2AuditLogger {

    private final Debug logger = Debug.getInstance("OAuth2Provider");
    private LogMessageProvider msgProvider;
    private Logger accessLogger;
    private Logger errorLogger;
    private boolean logStatus;

    /**
     * Constructs a new OAuth2AuditLogger.
     */
    @Inject
    public OAuth2AuditLogger() {
        final String status = SystemProperties.get(Constants.AM_LOGSTATUS);
        logStatus = ((status != null) && status.equalsIgnoreCase("ACTIVE"));
        if (logStatus) {
            accessLogger = (Logger)Logger.getLogger(OAuth2Constants.ACCESS_LOG_NAME);
            errorLogger = (Logger)Logger.getLogger(OAuth2Constants.ERROR_LOG_NAME);
        }
    }

    /**
     * Gets the LogMessageProvider instance.
     * <br/>
     * Will create the instance on first call.
     *
     * @return The LogMessageProvider instance.
     * @throws IOException If an error occurs whilst creating the LogMessageProvider instance.
     */
    private synchronized LogMessageProvider getLogMessageProvider() throws IOException {
        if (msgProvider == null) {
            msgProvider = MessageProviderFactory.getProvider("OAuth2Provider");
        }
        return msgProvider;
    }

    /**
     * Determines whether audit logging is enabled.
     *
     * @return {@code true} if audit logging is enabled.
     */
    public boolean isAuditLogEnabled() {
        return logStatus;
    }

    /**
     * Logs an error message.
     *
     * @param msgIdName The message id name.
     * @param data The data to log.
     * @param token The SSO Token to authenticate the log operation.
     */
    public void logErrorMessage(String msgIdName, String[] data, SSOToken token) {
        try {
            getLogMessageProvider();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            logger.error("disabling logging");
            logStatus = false;
        }
        if (errorLogger != null && msgProvider != null) {
            final LogRecord lr = msgProvider.createLogRecord(msgIdName, data, token);
            if (lr != null) {
                final SSOToken ssoToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
                errorLogger.log(lr, ssoToken);
            }
        }
    }

    /**
     * Logs an access message.
     *
     * @param msgIdName The message id name.
     * @param data The data to log.
     * @param token The SSO Token to authenticate the log operation.
     */
    public void logAccessMessage(String msgIdName, String[] data, SSOToken token) {
        try {
            getLogMessageProvider();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            logger.error("disabling logging");
            logStatus = false;
        }
        if ((accessLogger != null) && (msgProvider != null)) {
            final LogRecord lr = msgProvider.createLogRecord(msgIdName, data, token);
            if (lr != null) {
                final SSOToken ssoToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
                accessLogger.log(lr, ssoToken);
            }
        }
    }
}
