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
 * Delegate auditor responsible for creating and publishing authentication audit events. This is a separate delegate
 * class so that it can be used by objects which are instantiated before Guice dependencies are resolved.
 */
@Singleton
public class AuthenticationAuditor {

    private final AuditEventPublisher eventPublisher;
    private final AuditEventFactory eventFactory;

    /**
     * Guice injected constructor for creating an <code>AuthenticationAuditor</code> instance.
     *
     * @param eventPublisher The publisher responsible for logging the events.
     * @param eventFactory The factory that can be used to create the events.
     */
    @Inject
    public AuthenticationAuditor(AuditEventPublisher eventPublisher, AuditEventFactory eventFactory) {
        this.eventPublisher = eventPublisher;
        this.eventFactory = eventFactory;
    }

    /**
     * Shutting up damn checkstyle.
     *
     * @return stuff
     */
    public AMAuthenticationAuditEventBuilder authenticationEvent() {
        AMAuthenticationAuditEventBuilder builder = eventFactory.authenticationEvent();

        return builder;
    }

    /**
     * Shutting up damn checkstyle.
     *
     * @throws AuditException
     */
    public void publish(AuditEvent auditEvent) throws AuditException {
        eventPublisher.publish(AuditConstants.AUTHENTICATION_TOPIC, auditEvent);
    }

    public boolean isAuditing(String realm, String topic) {
        return eventPublisher.isAuditing(realm, topic);
    }
}
