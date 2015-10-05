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
import static org.forgerock.json.resource.Resources.newInternalConnection;
import static org.forgerock.openam.audit.AuditConstants.EVENT_REALM;
import static org.forgerock.openam.utils.StringUtils.isBlank;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.audit.AuditException;
import org.forgerock.audit.events.AuditEvent;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.services.context.RootContext;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Responsible for publishing locally created audit events to the AuditService.
 *
 * @since 13.0.0
 */
@Singleton
public class AuditEventPublisher {

    private static Debug debug = Debug.getInstance("amAudit");

    private final AuditServiceProvider auditServiceProvider;

    /**
     * Constructs a new {@code AuditEventPublisher}.
     *
     * @param auditServiceProvider A {@code AuditServiceProvider} instance.
     */
    @Inject
    public AuditEventPublisher(AuditServiceProvider auditServiceProvider) {
        this.auditServiceProvider = auditServiceProvider;
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
        String realm = getValue(auditEvent.getValue(), EVENT_REALM, null);
        if (isBlank(realm)) {
            publishToDefault(topic, auditEvent);
        } else {
            publishForRealm(realm, topic, auditEvent);
        }
    }

    private void publishToDefault(String topic, AuditEvent auditEvent) throws AuditException {
        AMAuditService auditService = auditServiceProvider.getDefaultAuditService();
        Connection connection = newInternalConnection(auditService);
        CreateRequest request = newCreateRequest(topic, auditEvent.getValue());

        try {
            connection.create(new RootContext(), request);
        } catch (ResourceException e) {
            handleResourceException(e, auditService, topic, auditEvent);
        }
    }

    private void publishForRealm(String realm, String topic, AuditEvent auditEvent) throws AuditException {
        AMAuditService auditService = auditServiceProvider.getAuditService(realm);
        Connection connection = newInternalConnection(auditService);
        CreateRequest request = newCreateRequest(topic, auditEvent.getValue());

        try {
            connection.create(new RootContext(), request);
        } catch (ServiceUnavailableException e) {
            debug.message("Audit Service for realm {} is unavailable. Trying the default Audit Service.", realm, e);
            publishToDefault(topic, auditEvent);
        } catch (ResourceException e) {
            handleResourceException(e, auditService, topic, auditEvent);
        }
    }

    private void handleResourceException(ResourceException e, AMAuditService auditService, String topic,
            AuditEvent auditEvent) throws AuditException {

        final String eventName = getValue(auditEvent.getValue(), EVENT_NAME, "-unknown-");
        debug.error("Unable to publish {} audit event '{}' due to error: {} [{}]",
                topic, eventName, e.getMessage(), e.getReason(), e);

        if (!auditService.isAuditFailureSuppressed()) {
            throw new AuditException("Unable to publish " + topic + " audit event '" + eventName + "'", e);
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
            // suppress - error logged in publish method
        }
    }

    private String getValue(JsonValue jsonValue, String key, String defaultValue) {
        return jsonValue.isDefined(key) ? jsonValue.get(key).asString() : defaultValue;
    }

    /**
     * Determines if the audit service is auditing the specified {@literal topic} in the specified {@literal realm}. If
     * the {@literal realm} is either {@code null} or empty, the check will be done against the default audit service.
     *
     * Note that We deliberately do not provide a convenience method with no realm to force implementers to consider
     * providing the realm. We must publish per realm wherever applicable.
     *
     * @param realm The realm in which the audit event occurred, or null if realm is not applicable.
     * @param topic The auditing topic.
     * @return {@code true} if the topic should be audited.
     */
    public boolean isAuditing(String realm, String topic) {
        if (isBlank(realm)) {
            return auditServiceProvider.getDefaultAuditService().isAuditEnabled(topic);
        } else {
            return auditServiceProvider.getAuditService(realm).isAuditEnabled(topic);
        }
    }
}
