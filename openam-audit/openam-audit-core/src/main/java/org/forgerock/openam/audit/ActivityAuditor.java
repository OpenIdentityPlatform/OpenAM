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
import org.forgerock.audit.events.AuditEvent;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Delegate auditor responsible for creating and publishing activity audit events. This is a separate delegate
 * class so that it can be used by objects which are instantiated before Guice dependencies are resolved.
 *
 * @since 13.0.0
 */
@Singleton
public class ActivityAuditor {

    private final AuditEventPublisher eventPublisher;
    private final AuditEventFactory eventFactory;

    /**
     * Guice injected constructor for creating an {@link ActivityAuditor} instance.
     *
     * @param eventPublisher The publisher responsible for logging the events.
     * @param eventFactory   The factory that can be used to create the events.
     */
    @Inject
    public ActivityAuditor(AuditEventPublisher eventPublisher, AuditEventFactory eventFactory) {
        this.eventPublisher = eventPublisher;
        this.eventFactory = eventFactory;
    }

    /**
     * Obtain a builder for an activity event.
     *
     * @return An instance of {@link AMActivityAuditEventBuilder}.
     */
    public AMActivityAuditEventBuilder activityEvent() {
        AMActivityAuditEventBuilder builder = eventFactory.activityEvent();

        return builder;
    }

    /**
     * Publish an audit event for the activity topic.
     *
     * @param auditEvent The event to audit.
     * @throws AuditException if an exception occurs while trying to publish the audit event.
     */
    public void publish(AuditEvent auditEvent) throws AuditException {
        eventPublisher.publish(AuditConstants.ACTIVITY_TOPIC, auditEvent);
    }

    /**
     * Reports whether or not this auditor is logging audit events for the specified realm and topic.
     *
     * @param realm The realm.
     * @param topic The topic.
     * @return true if the topic is being audited for the specified realm, false otherwise.
     */
    public boolean isAuditing(String realm, String topic) {
        return eventPublisher.isAuditing(realm, topic);
    }
}
