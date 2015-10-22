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
package com.sun.identity.authentication;

import org.forgerock.audit.AuditException;
import org.forgerock.openam.audit.AMActivityAuditEventBuilder;
import org.forgerock.openam.audit.AMAuthenticationAuditEventBuilder;
import org.forgerock.openam.audit.ActivityAuditor;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.AuthenticationAuditor;
import org.forgerock.openam.audit.model.AuthenticationAuditEntry;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Delegate auditor responsible for creating and publishing authentication and activity audit events specifically for
 * the legacy authentication logging points. This is a separate delegate class so that it can be used by objects which
 * are instantiated before Guice dependencies are resolved.
 *
 * This class abstracts away some of the logic that is needed to publish authentication and activity audit events
 * within the locations where legacy authentication events were historically logged. It is an intermediary between the
 * legacy publishing locations and the auditor classes ({@link org.forgerock.openam.audit.AuthenticationAuditor} and {@link org.forgerock.openam.audit.ActivityAuditor}).
 *
 * @since 13.0.0
 */
@Singleton
public class LegacyAuthenticationEventAuditor {

    private AuthenticationAuditor authenticationAuditor;
    private ActivityAuditor activityAuditor;

    private static final Set<String> LOGOUT_EVENTS = new HashSet<>(Arrays.asList("LOGOUT", "LOGOUT_USER" +
            "LOGOUT_ROLE", "LOGOUT_SERVICE", "LOGOUT_LEVEL", "LOGOUT_MODULE_INSTANCE"));

    private static final Set<String> ACTIVITY_ONLY_EVENTS =
            new HashSet<>(Arrays.asList("CHANGE_USER_PASSWORD_SUCCEEDED"));

    /**
     * Guice injected constructor for creating a {@link LegacyAuthenticationEventAuditor} instance.
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
     * Audit an event generated from a legacy context. Depending upon the configuration the user has chosen, the
     * event may be audited, or silently ignored.
     *
     * Note that if an event is for a topic which is not being audited, true may still be returned, which would
     * indicate that the event was handled successfully (not sent anywhere, respecting the configuration) and
     * there were no errors. A return value of true does not mean that the event was actually logged, only that
     * no error occurred in the attempt to log it.
     *
     * To find out if a specific topic is being audited, use
     * {@link LegacyAuthenticationEventAuditor#isAuditing(java.lang.String, java.lang.String)}.
     *
     * @param eventName The description of the event which occurred (see {@code AuthenticationLogMessageIDs.xml}
     *                         'name' attribute of each logmessage element.
     * @param eventDescription The description of the event which occurred (see {@code AuthenticationLogMessageIDs.xml}
     *                         'description' attribute of each logmessage element. Cannot be null.
     * @param transactionId The transaction id for the audit event. Cannot be null.
     * @param authentication The authentication details for the audit event. Cannot be null.
     * @param realmName The realm name for the audit event. May be null.
     * @param time The time the audit event occurred. May be null.
     * @param trackingIds Any tracking ids for the audit event. May be null.
     * @param entries Any extra information for the audit event. May be null.
     * @return true if the event was handled, false if there was some sort of problem.
     */
    public boolean audit(String eventName, String eventDescription, String transactionId, String authentication,
                         String realmName, long time, Set<String> trackingIds, List<AuthenticationAuditEntry> entries) {
        Reject.ifNull(transactionId, "The transactionId field cannot be null");
        Reject.ifNull(authentication, "The authentication field cannot be null");
        Reject.ifNull(eventDescription, "The eventDescription field cannot be null");

        boolean isActivityEvent = false;
        boolean isAuthenticationEvent = true;

        //Determine if event is an activity event ONLY. Event names drawn from AuthenticationLogMessageIDs.xml.
        if (StringUtils.isNotEmpty(eventName)) {
            if (isActivityOnlyEvent(eventName)) {
                isActivityEvent = true;
                isAuthenticationEvent = false;
            }
        }
        //(any remaining events are purely authentication events)

        if (isAuthenticationEvent) {
            return auditAuthenticationEvent(eventDescription, transactionId, authentication, realmName, time,
                    trackingIds, entries);
        }

        if (isActivityEvent) {
            return auditActivityEvent(eventDescription, transactionId, authentication, realmName, time, trackingIds);
        }

        return false;
    }

    private boolean auditAuthenticationEvent(String description, String transactionId, String authentication,
                                             String realmName, long time, Set<String> trackingIds,
                                             List<AuthenticationAuditEntry> entries) {
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
        if (trackingIds != null && !trackingIds.isEmpty()) {
            builder.trackingIds(trackingIds);
        }
        if (entries != null && !entries.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (AuthenticationAuditEntry authenticationAuditEntry : entries) {
                Map<String, Object> map = new HashMap<>();
                map.put("moduleId", authenticationAuditEntry.getModuleId());
                map.put("result", authenticationAuditEntry.getResult());
                map.put("info", authenticationAuditEntry.getInfo());
                list.add(map);
            }
            builder.entries(list);
        }

        try {
            authenticationAuditor.publish(builder.toEvent());
        } catch (AuditException e) {
            couldHandleEvent = false;
        }

        return couldHandleEvent;
    }

    private boolean auditActivityEvent(String description, String transactionId, String authentication,
                                       String realmName, long time, Set<String> trackingIds) {
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
        if (trackingIds != null && !trackingIds.isEmpty()) {
            builder.trackingIds(trackingIds);
        }

        try {
            activityAuditor.publish(builder.toEvent());
        } catch (AuditException e) {
            couldHandleEvent = false;
        }

        return couldHandleEvent;
    }

    /**
     * Reports whether or not the legacy authn message indicates a logout event has occurred. The list of messages
     * which indicate a logout event are drawn from {@code AuthenticationLogMessageIDs.xml}.
     *
     * @param message The legacy authn event message.
     * @return true if this is a logout event, false in all other cases.
     */
    public static boolean isLogoutEvent(String message) {
        boolean isLogoutEvent = false;

        if (StringUtils.isNotEmpty(message)) {
            if (LOGOUT_EVENTS.contains(message)) {
                isLogoutEvent = true;
            }
        }

        return isLogoutEvent;
    }

    /**
     * Reports whether or not the legacy authn event is to be logged to the authentication audit log only. The list
     * of messages which indicate an authentication event are drawn from {@code AuthenticationLogMessageIDs.xml}.
     *
     * @param message The legacy authn event message.
     * @return true if this is an authentication event, false in all other cases.
     */
    public static boolean isAuthenticationOnlyEvent(String message) {
        boolean isAuthenticationOnlyEvent = false;

        if (StringUtils.isNotEmpty(message)) {
            if (!ACTIVITY_ONLY_EVENTS.contains(message)) {
                isAuthenticationOnlyEvent = true;
            }
        }

        return isAuthenticationOnlyEvent;
    }

    /**
     * Reports whether or not the legacy activity event is to be logged to the authentication audit log only. The list
     * of messages which indicate an activity event are drawn from {@code AuthenticationLogMessageIDs.xml}.
     *
     * @param message The legacy authn event message.
     * @return true if this is an activity event, false in all other cases.
     */
    public static boolean isActivityOnlyEvent(String message) {
        boolean isActivityOnlyEvent = false;

        if (StringUtils.isNotEmpty(message)) {
            if (ACTIVITY_ONLY_EVENTS.contains(message)) {
                isActivityOnlyEvent = true;
            }
        }

        return isActivityOnlyEvent;
    }

    /**
     * Reports whether or not this auditor is logging audit events for the specified realm and topic.
     *
     * As this is a legacy auditor it is not logging just one specific audit topic, as there is not a direct
     * one to one correlation between the set of all legacy authn events and one of the new audit topics. This
     * function will report more specifically which audit topics this auditor is auditing.
     *
     * @param realm The realm
     * @param topic The topic (see {@link AuditConstants})
     * @return true if this auditor is performing some form of auditing for this realm and topic, false otherwise.
     */
    public boolean isAuditing(String realm, String topic) {
        if (AuditConstants.AUTHENTICATION_TOPIC.equals(topic)) {
            return (authenticationAuditor.isAuditing(realm, topic));
        } else if (AuditConstants.ACTIVITY_TOPIC.equals(topic)) {
            return (activityAuditor.isAuditing(realm, topic));
        }

        return false;
    }
}
