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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openam.audit;

import org.forgerock.audit.events.AuditEvent;

/**
 * Responsible for publishing locally created audit events to the AuditService.
 *
 * @since 13.0.0
 */
public interface AuditEventPublisher {

    /**
     * Tries to publish the provided AuditEvent to the specified topic of the AuditService.
     * <p/>
     * If an error occurs that prevents the AuditEvent from being published, then details regarding the error
     * are recorded in the debug logs. However, only details relating to the error are logged; the debug logs
     * are not treated as the fallback destination for audit information.
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
