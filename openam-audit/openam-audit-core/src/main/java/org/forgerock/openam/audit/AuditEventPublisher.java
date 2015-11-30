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

/**
 * Responsible for publishing locally created audit events to the AuditService.
 *
 * @since 13.0.0
 */
public interface AuditEventPublisher {

    /**
     * Publishes the provided AuditEvent to the specified topic of the AuditService.
     * <p/>
     * If an error occurs that prevents the AuditEvent from being published, then details regarding the error
     * are recorded in the debug logs. However, the debug logs are not be treated as the fallback destination
     * for audit information. If we need guaranteed capture of audit information then this needs to be a feature
     * of the audit service itself. Also, the audit event may contain sensitive information that shouldn't be
     * stored in debug logs.
     * <p/>
     * After recording details of the error, the exception will only be propagated back to the caller if the
     * 'suppress exceptions' configuration option is set to false.
     *
     * @param topic Coarse-grained categorization of the AuditEvent's type.
     * @param auditEvent The AuditEvent to publish.
     *
     * @throws AuditException if an exception occurs while trying to publish the audit event.
     */
    void publish(String topic, AuditEvent auditEvent) throws AuditException;

    /**
     * Tries to publish the provided AuditEvent to the specified topic of the AuditService.
     * <p/>
     * If an exception occurs, details are logged but the exception is suppressed.
     *
     * @param topic Coarse-grained categorization of the AuditEvent's type.
     * @param auditEvent The AuditEvent to publish.
     */
    void tryPublish(String topic, AuditEvent auditEvent);

    /**
     * Determines if the audit service is auditing the specified {@literal topic} in the specified {@literal realm}. If
     * the {@literal realm} is either {@code null} or empty, the check will be done against the default audit service.
     *
     * Note that We deliberately do not provide a convenience method with no realm to force implementers to consider
     * providing the realm. We must publish per realm wherever applicable.
     *
     * @param realm The realm in which the audit event occurred, or null if realm is not applicable.
     * @param topic The auditing topic.
     * @param eventName The event name, may be {@literal null} if not known.
     * @return {@code true} if the topic should be audited.
     */
    boolean isAuditing(String realm, String topic, AuditConstants.EventName eventName);
}
