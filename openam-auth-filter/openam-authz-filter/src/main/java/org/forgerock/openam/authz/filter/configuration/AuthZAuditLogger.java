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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openam.authz.filter.configuration;

import com.google.inject.Singleton;
import com.iplanet.sso.SSOToken;
import com.sun.identity.log.LogRecord;
import com.sun.identity.log.Logger;
import org.forgerock.auth.common.AuditLogger;
import org.forgerock.auth.common.AuditRecord;
import org.forgerock.openam.auth.shared.AuthnRequestUtils;
import org.forgerock.openam.auth.shared.LoggerFactory;
import org.forgerock.openam.auth.shared.SSOTokenFactory;

import javax.inject.Inject;
import javax.security.auth.message.MessageInfo;
import javax.servlet.http.HttpServletRequest;
import java.util.logging.Level;

/**
 * Implementation of the AuditLogger for AM authorization.
 *
 * @since 10.2.0
 */
@Singleton
public class AuthZAuditLogger implements AuditLogger<HttpServletRequest> {

    private static final String AUDIT_LOG_NAME = "amAuthorization";

    private final SSOTokenFactory ssoTokenFactory;
    private final AuthnRequestUtils requestUtils;
    private final LoggerFactory loggerFactory;

    /**
     * Constructs a new instance of the AuthZAuditLogger.
     *
     * @param ssoTokenFactory An instance of the SSOTokenFactory.
     * @param requestUtils An instance of the AuthnRequestUtils.
     * @param loggerFactory An instance of the LoggerFactory.
     */
    @Inject
    public AuthZAuditLogger(SSOTokenFactory ssoTokenFactory, AuthnRequestUtils requestUtils,
            LoggerFactory loggerFactory) {
        this.ssoTokenFactory = ssoTokenFactory;
        this.requestUtils = requestUtils;
        this.loggerFactory = loggerFactory;
    }

    /**
     * Audits the AuthZ request.
     *
     * @param auditRecord {@inheritDoc}
     */
    @Override
    public void audit(AuditRecord<HttpServletRequest> auditRecord) {

        HttpServletRequest request = auditRecord.getAuditObject();

        String tokenId = requestUtils.getTokenId(request);

        String message;
        Logger logger;
        switch (auditRecord.getAuthResult()) {
            case SUCCESS: {
                message = "Authorization Succeeded. " + request.getRequestURI();
                logger = loggerFactory.getLogger(AUDIT_LOG_NAME + ".access");
                break;
            }
            default: {
                message = "Authorization Failed. " + request.getRequestURI();
                logger = loggerFactory.getLogger(AUDIT_LOG_NAME + ".error");
            }
        }

        SSOToken adminToken = ssoTokenFactory.getAdminToken();
        SSOToken subjectToken = ssoTokenFactory.getTokenFromId(tokenId);
        LogRecord logRecord = new LogRecord(Level.INFO, message, subjectToken);

        logger.log(logRecord, adminToken);
        logger.flush();
    }
}
