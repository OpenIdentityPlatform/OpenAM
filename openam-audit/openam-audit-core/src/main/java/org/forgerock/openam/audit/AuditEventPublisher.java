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

import static org.forgerock.audit.events.AuditEventBuilder.EVENT_NAME;
import static org.forgerock.json.resource.Requests.newCreateRequest;

import com.google.inject.Inject;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.audit.AuditException;
import org.forgerock.audit.AuditService;
import org.forgerock.audit.events.AuditEvent;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.RootContext;
import org.forgerock.openam.audit.configuration.AuditServiceConfigurator;

/**
 * Responsible for publishing locally created audit events to the AuditService.
 *
 * @since 13.0.0
 */
public class AuditEventPublisher {

    private static Debug debug = Debug.getInstance("amAudit");

    private final AuditService auditService;
    private final ConnectionFactory auditServiceConnectionFactory;
    private final AuditServiceConfigurator configurator;

    /**
     * @param auditService AuditService to which events should be published.
     */
    @Inject
    public AuditEventPublisher(AuditService auditService, AuditServiceConfigurator configurator) {
        this.auditService = auditService;
        this.auditServiceConnectionFactory = Resources.newInternalConnectionFactory(auditService);
        this.configurator = configurator;
    }

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
    public void publish(String topic, AuditEvent auditEvent) throws AuditException {

        try {

            Connection connection = auditServiceConnectionFactory.getConnection();
            connection.create(new RootContext(), newCreateRequest(topic, auditEvent.getValue()));

        } catch (ResourceException e) {

            final String eventName = getValue(auditEvent.getValue(), EVENT_NAME, "-unknown-");
            debug.error("Unable to publish {} audit event '{}' due to error: {} [{}]",
                    topic, eventName, e.getMessage(), e.getReason(), e);

            if (!isSuppressExceptions()) {
                throw new AuditException("Unable to publish " + topic + " audit event '" + eventName + "'", e);
            }
        }
    }

    /**
     * Tries to publish the provided AuditEvent to the specified topic of the AuditService.
     * <p/>
     * If an exception occurs, details are logged but the exception is suppressed.
     *
     * @param topic Coarse-grained categorization of the AuditEvent's type.
     * @param auditEvent The AuditEvent to publish.
     */
    public void tryPublish(String topic, AuditEvent auditEvent) {
        try {
            publish(topic, auditEvent);
        } catch (AuditException e) {
            // suppress
        }
    }

    private String getValue(JsonValue jsonValue, String key, String defaultValue) {
        return jsonValue.isDefined(key) ? jsonValue.get(key).asString() : defaultValue;
    }

    public boolean isAuditing(String topic) {
        return configurator.getAuditServiceConfiguration().isAuditEnabled() && auditService.isAuditing(topic);
    }

    /**
     * @return True if the operation being audited can proceed if an exception occurs while publishing an audit event.
     */
    public boolean isSuppressExceptions() {
        return configurator.getAuditServiceConfiguration().isAuditFailureSuppressed();
    }
}
