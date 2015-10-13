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
* Copyright 2015 ForgeRock AS.
*/
package org.forgerock.openam.audit;

import org.forgerock.audit.AuditException;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

/**
 * Delegate auditor responsible for creating and publishing authentication and activity audit events specifically for
 * the legacy authentication logging points. This is a separate delegate class so that it can be used by objects which
 * are instantiated before Guice dependencies are resolved.
 *
 * This class abstracts away some of the logic that is needed to publish authentication and activity audit events
 * within the locations where legacy authentication events are logged.
 */
@Singleton
public class LegacyAuthenticationEventAuditor {

    private AuthenticationAuditor authenticationAuditor;
    private ActivityAuditor activityAuditor;

    /**
     * Guice injected constructor for creating an <code>LegacyAuthenticationEventAuditor</code> instance.
     *
     * @param authenticationAuditor The auditor responsible for logging authentication audit events.
     * @param activityAuditor The auditor responsible for logging activity audit events.
     */
    @Inject
    public LegacyAuthenticationEventAuditor(AuthenticationAuditor authenticationAuditor,
                                            ActivityAuditor activityAuditor) {
        this.authenticationAuditor = authenticationAuditor;
        this.activityAuditor = activityAuditor;
    }

    /**
     * AuthenticationLogMessageIDs.xml
     *
     *
     * @param eventDescription
     * @param eventName
     * @param transactionId
     * @param authentication
     * @param time
     * @param contexts
     * @param entries
     * @return
     */
    public boolean handleEvent(String eventName, String eventDescription, String transactionId, String authentication,
                               String realmName, long time, Map<String, String> contexts, List<?> entries) {
        Reject.ifNull(transactionId, "The transactionId field cannot be null");
        Reject.ifNull(authentication, "The authentication field cannot be null");

        boolean isActivityEvent = false;
        boolean isAuthenticationEvent = true;

        //Determine if event is an activity event ONLY.
        if (StringUtils.isNotEmpty(eventName)) {
            if ("CHANGE_USER_PASSWORD_SUCCEEDED".equals(eventName)) {
                isActivityEvent = true;
                isAuthenticationEvent = false;
            }
        }
        //(any remaining events are purely authentication events)

        if (isAuthenticationEvent) {
            return handleAuthenticationEvent(eventDescription, transactionId, authentication, realmName, time, contexts,
                    entries);
        }

        if (isActivityEvent) {
            return handleActivityEvent(eventDescription, transactionId, authentication, realmName, time, contexts);
        }

        return false;
    }

    private boolean handleAuthenticationEvent(String description, String transactionId, String authentication,
                                              String realmName, long time, Map<String, String> contexts,
                                              List<?> entries) {
        boolean couldHandleEvent = true;

        AMAuthenticationAuditEventBuilder builder = authenticationAuditor.authenticationEvent();

        builder.transactionId(transactionId)
                .authentication(authentication)
                .timestamp(time)
                .component(AuditConstants.Component.AUTHENTICATION);

        if (StringUtils.isNotEmpty(description)) {
            builder.eventName(description);
        }
        if (StringUtils.isNotEmpty(realmName)) {
            builder.realm(realmName);
        }
        if (contexts != null && !contexts.isEmpty()) {
            builder.contexts(contexts);
        }
        if (entries != null && !entries.isEmpty()) {
            builder.entries(entries);
        }

        try {
            authenticationAuditor.publish(builder.toEvent());
        } catch (AuditException e) {
            couldHandleEvent = false;
        }

        return couldHandleEvent;
    }

    private boolean handleActivityEvent(String description, String transactionId, String authentication,
                                        String realmName, long time, Map<String, String> contexts) {
        boolean couldHandleEvent = true;

        AMActivityAuditEventBuilder builder = activityAuditor.activityEvent();

        builder.transactionId(transactionId)
                .authentication(authentication)
                .timestamp(time)
                .component(AuditConstants.Component.AUTHENTICATION);

        if (StringUtils.isNotEmpty(description)) {
            builder.eventName(description);
        }
        if (StringUtils.isNotEmpty(realmName)) {
            builder.realm(realmName);
        }
        if (contexts != null && !contexts.isEmpty()) {
            builder.contexts(contexts);
        }

        try {
            activityAuditor.publish(builder.toEvent());
        } catch (AuditException e) {
            couldHandleEvent = false;
        }

        return couldHandleEvent;
    }

    /**
     * AuthenticationLogMessageIDs.xml
     *
     * @param messageName
     * @return
     */
    public boolean isLogoutEvent(String messageName) {
        boolean isLogoutEvent = false;

        if (StringUtils.isNotEmpty(messageName)) {
            if ("LOGOUT".equals(messageName) ||
                    "LOGOUT_USER".equals(messageName) ||
                    "LOGOUT_ROLE".equals(messageName) ||
                    "LOGOUT_SERVICE".equals(messageName) ||
                    "LOGOUT_LEVEL".equals(messageName) ||
                    "LOGOUT_MODULE_INSTANCE".equals(messageName)) {
                isLogoutEvent = true;
            }
        }

        return isLogoutEvent;
    }

    public boolean isAuditing(String realm, String topic) {
        if (AuditConstants.AUTHENTICATION_TOPIC.equals(topic)) {
            return (authenticationAuditor.isAuditing(realm, topic));
        } else if (AuditConstants.ACTIVITY_TOPIC.equals(topic)) {
            return (activityAuditor.isAuditing(realm, topic));
        }

        return false;
    }

    public boolean isAuditing(String realm) {
        return (authenticationAuditor.isAuditing(realm, AuditConstants.AUTHENTICATION_TOPIC) ||
                activityAuditor.isAuditing(realm, AuditConstants.ACTIVITY_TOPIC));
    }
}
