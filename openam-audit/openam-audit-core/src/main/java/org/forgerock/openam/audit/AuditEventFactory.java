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

import static org.forgerock.openam.utils.StringUtils.isNotBlank;

import javax.inject.Singleton;

/**
 * Factory for creation of AuditEvent builders.
 *
 * Facilitates mocking of event builders.
 *
 * @since 13.0.0
 */
@Singleton
public class AuditEventFactory {

    /**
     * Creates a new AMAccessAuditEventBuilder for the specified {@literal realm} and adds the realm to the event. If
     * the {@literal realm} is either {@code null} or empty it will not be added to the event.
     *
     * Note that we deliberately do not provide a convenience method with no realm to force implementers to consider
     * providing the realm. We must publish per realm wherever applicable.
     *
     * @param realm The realm in which the audit event occurred, or null if realm is not applicable.
     * @return AMAccessAuditEventBuilder
     */
    public AMAccessAuditEventBuilder accessEvent(String realm) {
        AMAccessAuditEventBuilder auditEventBuilder = new AMAccessAuditEventBuilder();
        if (isNotBlank(realm)) {
            auditEventBuilder.realm(realm);
        }
        return auditEventBuilder;
    }

    /**
     * Creates a new AMActivityAuditEventBuilder for the specified {@literal realm} and adds the realm to the event. If
     * the {@literal realm} is either {@code null} or empty it will not be added to the event.
     *
     * Note that we deliberately do not provide a convenience method with no realm to force implementers to consider
     * providing the realm. We must publish per realm wherever applicable.
     *
     * @param realm The realm in which the audit event occurred, or null if realm is not applicable.
     * @return AMActivityAuditEventBuilder
     */
    public AMActivityAuditEventBuilder activityEvent(String realm) {
        AMActivityAuditEventBuilder auditEventBuilder = new AMActivityAuditEventBuilder();
        if (isNotBlank(realm)) {
            auditEventBuilder.realm(realm);
        }
        return auditEventBuilder;
    }

    /**
     * Creates a new AMConfigAuditEventBuilder for the specified {@literal realm} and adds the realm to the event. If
     * the {@literal realm} is either {@code null} or empty it will not be added to the event.
     *
     * Note that we deliberately do not provide a convenience method with no realm to force implementers to consider
     * providing the realm. We must publish per realm wherever applicable.
     *
     * @param realm The realm in which the audit event occurred, or null if realm is not applicable.
     * @return AMConfigAuditEventBuilder
     */
    public AMConfigAuditEventBuilder configEvent(String realm) {
        AMConfigAuditEventBuilder auditEventBuilder = new AMConfigAuditEventBuilder();
        if (isNotBlank(realm)) {
            auditEventBuilder.realm(realm);
        }
        return auditEventBuilder;
    }

    /**
     * Creates a new AMAuthenticationAuditEventBuilder for the specified {@literal realm} and adds the realm to the
     * event. If the {@literal realm} is either {@code null} or empty it will not be added to the event.
     *
     * Note that we deliberately do not provide a convenience method with no realm to force implementers to consider
     * providing the realm. We must publish per realm wherever applicable.
     *
     * @param realm The realm in which the audit event occurred, or null if realm is not applicable.
     * @return AMAuthenticationAuditEventBuilder
     */
    public AMAuthenticationAuditEventBuilder authenticationEvent(String realm) {
        AMAuthenticationAuditEventBuilder auditEventBuilder = new AMAuthenticationAuditEventBuilder();
        if (isNotBlank(realm)) {
            auditEventBuilder.realm(realm);
        }
        return auditEventBuilder;
    }

}
