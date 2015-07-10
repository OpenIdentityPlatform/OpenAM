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

import static org.forgerock.json.resource.Requests.newCreateRequest;

import com.google.inject.Inject;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.audit.AuditException;
import org.forgerock.audit.AuditService;
import org.forgerock.audit.events.AuditEvent;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.RootContext;

/**
 * Responsible for publishing locally created audit events to the AuditService.
 *
 * @since 13.0.0
 */
public class AuditEventPublisher {

    private static Debug debug = Debug.getInstance("amAudit");

    private final ConnectionFactory auditServiceConnectionFactory;

    /**
     * @param auditService AuditService to which events should be published.
     */
    @Inject
    public AuditEventPublisher(AuditService auditService) {
        this.auditServiceConnectionFactory = Resources.newInternalConnectionFactory(auditService);
    }

    /**
     * Publishes the provided AuditEvent to the specified topic of the AuditService.
     *
     * @param topic Coarse-grained categorization of the AuditEvent's type.
     * @param auditEvent The AuditEvent to publish.
     */
    public void publish(String topic, AuditEvent auditEvent) throws AuditException {
        try {

            Connection connection = auditServiceConnectionFactory.getConnection();
            connection.create(new RootContext(), newCreateRequest(topic, auditEvent.getValue()));

        } catch (ResourceException e) {
            debug.error("Unable to publish audit event", e);
            throw new AuditException("Unable to publish audit event", e);
        }
    }

}
