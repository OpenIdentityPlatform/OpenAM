/*
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
 * $Id: LogWriter.java,v 1.3 2008/06/25 05:42:09 qcheng Exp $
 *
 * Portions Copyrighted 2015-2016 ForgeRock AS.
 */

package com.sun.identity.cli;


import static org.forgerock.audit.events.AuthenticationAuditEventBuilder.Status.*;
import static org.forgerock.http.routing.Version.*;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.audit.AuditConstants.*;
import static org.forgerock.openam.utils.Time.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.forgerock.audit.events.AccessAuditEventBuilder;
import org.forgerock.guava.common.collect.ImmutableMap;
import org.forgerock.http.Client;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.http.header.AcceptApiVersionHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.audit.AMAccessAuditEventBuilder;
import org.forgerock.openam.audit.AMAuditEventBuilder;
import org.forgerock.openam.audit.AMAuthenticationAuditEventBuilder;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.slf4j.LoggerFactory;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.sso.SSOToken;
import com.sun.identity.log.LogRecord;
import com.sun.identity.log.Logger;
import com.sun.identity.log.messageid.LogMessageID;
import com.sun.identity.log.messageid.LogMessageProvider;
import com.sun.identity.log.messageid.MessageProviderFactory;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;

/**
 * Writes audit log entries.
 */
public class LogWriter {

    private static final Debug DEBUG = Debug.getInstance("amCLI");

    private static final String LOG_MSG_XML = "CLI";

    /**
     * Access Log Type.
     */
    public final static int LOG_ACCESS = 0;

    /**
     * Error Log Type.
     */
    public final static int LOG_ERROR = 1;
    private static final List<String> IGNORED_LOG_FIELDS = Arrays.asList("error message", "realm", "user id");

    private static final Map<String, String> NORMALIZED_FIELD_NAMES = new ImmutableMap.Builder<String, String>()
            .put("name of realm", "realm")
            .put("realm where entity resides", "realm")
            .put("realm where circle of trust resides", "realm")
            .build();

    private LogWriter() {
    }

    /**
     * Writes to log.
     *
     * @param mgr Command Manager Object.
     * @param type Type of log message.
     * @param level Logging level of the message.
     * @param msgid ID for message.
     * @param msgdata array of log message "data".
     * @param ssoToken Single Sign On Token of the user who committed the
     *        operation.
     * @throws CLIException if log cannot be written.
     */
    public static void log(
        CommandManager mgr,
        int type,
        Level level,
        String msgid,
        String[] msgdata,
        SSOToken ssoToken
    ) throws CLIException {
        if (!mgr.isLogOff()) {
            Logger logger;
            String logName = mgr.getLogName();
            switch (type) {
                case LOG_ERROR:
                    logger = (com.sun.identity.log.Logger) Logger.getLogger(logName + ".error");
                    break;
                default:
                    logger = (com.sun.identity.log.Logger) Logger.getLogger(logName + ".access");
            }
            try {
                LogMessageProvider msgProvider = MessageProviderFactory.getProvider(LOG_MSG_XML);
                SSOToken adminSSOToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
                if (ssoToken == null) {
                    ssoToken = adminSSOToken;
                }

                if (logger.isLoggable(level)) {
                    LogRecord logRec = msgProvider.createLogRecord(msgid, msgdata, ssoToken);
                    if (logRec != null) {
                        logger.log(logRec, adminSSOToken);
                    }
                }

                logToAuditService(type, msgid, msgdata, ssoToken, msgProvider, adminSSOToken);
            } catch (Exception e) {
                throw new CLIException(e, ExitCodes.CANNOT_WRITE_LOG);
            }
        }
    }

    private static void logToAuditService(int type, String msgid, String[] msgdata, SSOToken ssoToken,
            LogMessageProvider msgProvider, SSOToken adminSSOToken) throws Exception {
        String operation = msgid.substring(msgid.indexOf('_') + 1);
        LogMessageID logMessageID = msgProvider.getAllHashMessageIDs().get(msgid);
        if (logMessageID == null) {
            DEBUG.error("Attempted audit logging for unknown message ID {}", msgid);
            return;
        }
        List<String> fields = logMessageID.getDataColumns();

        AMAuditEventBuilder builder;
        String topic;
        if ("LOGIN".equals(operation) && !msgid.startsWith("ATTEMPT")) {
            builder = authenticationEventBuilder(type, msgid, fields, msgdata);
            topic = AuditConstants.AUTHENTICATION_TOPIC;
        } else if (!"LOGIN".equals(operation)) {
            builder = accessEventBuilder(type, msgid, msgdata, operation, fields);
            topic = AuditConstants.ACCESS_TOPIC;
        } else {
            return;
        }

        JsonValue eventJson = builder.transactionId(CommandManager.TRANSACTION_ID.getValue())
                .timestamp(currentTimeMillis())
                .userId(ssoToken.getPrincipal().getName())
                .trackingIdFromSSOToken(ssoToken)
                .component(AuditConstants.Component.SSOADM)
                .toEvent().getValue();

        String sessionId = adminSSOToken.getTokenID().toString();
        sendEvent(topic, eventJson, sessionId, new SessionID(sessionId).getSessionServerURL());
    }

    private static void sendEvent(String topic, JsonValue eventJson, String sessionId, String baseUrl) throws HttpApplicationException, URISyntaxException {
        Client client = new Client(new HttpClientHandler());
        Request request = new Request();
        request.setMethod("POST");
        if (eventJson.isDefined(EVENT_REALM)) {
            String realm = eventJson.get(EVENT_REALM).asString();
            baseUrl = baseUrl + "/json/realm-audit" + (realm.endsWith("/") ? realm : realm + "/");
        } else {
            baseUrl = baseUrl + "/json/global-audit/";
        }
        request.setUri(baseUrl + topic + "?_action=create");
        request.getHeaders().add(SystemProperties.get("com.iplanet.am.cookie.name"), sessionId);
        request.getHeaders().add(new AcceptApiVersionHeader(version(1), version(1)));
        request.getEntity().setJson(eventJson.getObject());
        client.send(request).then(WARN_OF_FAILURES_FUNCTION);
    }

    private static AMAccessAuditEventBuilder accessEventBuilder(int type, String msgid, String[] msgdata,
            String operation, List<String> fields) {
        AMAccessAuditEventBuilder accessEventBuilder = new AMAccessAuditEventBuilder();
        String realm = null;
        JsonValue requestData = json(object());
        if (msgdata != null) {
            for (int i = 0; i < msgdata.length; i++) {
                String fieldName = fields.get(i).toLowerCase();
                if (NORMALIZED_FIELD_NAMES.containsKey(fieldName)) {
                    fieldName = NORMALIZED_FIELD_NAMES.get(fieldName);
                }
                if (!IGNORED_LOG_FIELDS.contains(fieldName)) {
                    requestData.put(fieldName, msgdata[i]);
                } else if (fieldName.equals("realm")) {
                    realm = msgdata[i];
                }
            }
        }
        accessEventBuilder.request("ssoadm", operation, requestData);
        if (type == LOG_ERROR) {
            int errorMessageIndex = fields.indexOf("error message");
            if (errorMessageIndex > -1) {
                accessEventBuilder.responseWithDetail(AccessAuditEventBuilder.ResponseStatus.FAILED,
                        null, json(object(field("message", msgdata[errorMessageIndex]))));
            } else {
                accessEventBuilder.response(AccessAuditEventBuilder.ResponseStatus.FAILED, null);
            }
            accessEventBuilder.eventName(AuditConstants.EventName.AM_ACCESS_OUTCOME);
        } else if (msgid.startsWith("SUCCEED")) {
            accessEventBuilder.response(AccessAuditEventBuilder.ResponseStatus.SUCCESSFUL, null);
            accessEventBuilder.eventName(AuditConstants.EventName.AM_ACCESS_OUTCOME);
        } else {
            accessEventBuilder.eventName(AuditConstants.EventName.AM_ACCESS_ATTEMPT);
        }

        if (realm != null) {
            accessEventBuilder.realm(realm);
        }
        return accessEventBuilder;
    }

    private static AMAuthenticationAuditEventBuilder authenticationEventBuilder(int type, String msgid, List<String> fields,
            String[] principal) {
        AMAuthenticationAuditEventBuilder authEventBuilder = new AMAuthenticationAuditEventBuilder()
                .principal(principal[fields.indexOf("user ID")]);
        if (!msgid.startsWith("ATTEMPT")) {
            authEventBuilder.result(type == LOG_ERROR ? FAILED : SUCCESSFUL);
        }
        return authEventBuilder.eventName(AuditConstants.EventName.AM_LOGIN_COMPLETED);
    }

    private static Function<Response, Void, NeverThrowsException> WARN_OF_FAILURES_FUNCTION =
            new Function<Response, Void, NeverThrowsException>() {
                private final org.slf4j.Logger logger = LoggerFactory.getLogger("amAudit");
                @Override
                public Void apply(Response response) throws NeverThrowsException {
                    if (!response.getStatus().isSuccessful()) {
                        String responseText;
                        try {
                            responseText = response.getEntity().getString();
                        } catch (IOException e) {
                            responseText = "--unknown--";
                        }
                        logger.warn("Could not log audit via REST API: Status: {}, Response: {}",
                                response.getStatus(), responseText);
                    }
                    return null;
                }
            };
}
