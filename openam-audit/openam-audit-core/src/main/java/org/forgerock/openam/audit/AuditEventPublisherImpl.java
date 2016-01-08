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

import static org.forgerock.audit.events.AuditEventBuilder.EVENT_NAME;
import static org.forgerock.json.resource.Requests.newCreateRequest;
import static org.forgerock.json.resource.Resources.newInternalConnection;
import static org.forgerock.openam.audit.AuditConstants.EVENT_REALM;
import static org.forgerock.openam.utils.StringUtils.isBlank;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.audit.events.AuditEvent;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.openam.audit.AuditConstants.EventName;
import org.forgerock.services.context.RootContext;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Responsible for publishing locally created audit events to the AuditService.
 *
 * @since 13.0.0
 */
@Singleton
public class AuditEventPublisherImpl implements AuditEventPublisher {

    private static Debug debug = Debug.getInstance("amAudit");

    private final AuditServiceProvider auditServiceProvider;

    /**
     * Constructs a new {@code AuditEventPublisher}.
     *
     * @param auditServiceProvider A {@code AuditServiceProvider} instance.
     */
    @Inject
    public AuditEventPublisherImpl(AuditServiceProvider auditServiceProvider) {
        this.auditServiceProvider = auditServiceProvider;
    }

    @Override
    public void tryPublish(String topic, AuditEvent auditEvent) {
        try {
            String realm = getValue(auditEvent.getValue(), EVENT_REALM, null);
            if (isBlank(realm)) {
                publishToDefault(topic, auditEvent);
            } else {
                publishForRealm(realm, topic, auditEvent);
            }
        } catch (Exception e) {
            logException(e, topic, auditEvent);
        }
    }

    @Override
    public boolean isAuditing(String realm, String topic, EventName eventName) {
        if (isBlank(realm)) {
            return auditServiceProvider.getDefaultAuditService().isAuditEnabled(topic, eventName);
        } else {
            return auditServiceProvider.getAuditService(realm).isAuditEnabled(topic, eventName);
        }
    }

    private void publishToDefault(String topic, AuditEvent auditEvent) throws ResourceException {

        AMAuditService auditService = auditServiceProvider.getDefaultAuditService();
        Connection connection = newInternalConnection(auditService);
        CreateRequest request = newCreateRequest(topic, auditEvent.getValue());

        connection.create(new RootContext(), request);
    }

    private void publishForRealm(String realm, String topic, AuditEvent auditEvent) throws ResourceException {
        AMAuditService auditService = auditServiceProvider.getAuditService(realm);
        Connection connection = newInternalConnection(auditService);
        CreateRequest request = newCreateRequest(topic, auditEvent.getValue());

        try {
            connection.create(new RootContext(), request);
        } catch (ServiceUnavailableException e) {
            debug.message("Audit Service for realm {} is unavailable. Trying the default Audit Service.", realm, e);
            publishToDefault(topic, auditEvent);
        }
    }

    private void logException(Exception exception, String topic, AuditEvent auditEvent) {
        final String eventName = getValue(auditEvent.getValue(), EVENT_NAME, "-unknown-");
        if (exception instanceof ResourceException) {
            debug.error("Unable to publish {} audit event '{}' due to error: {} [{}]",
                    topic, eventName, exception.getMessage(), ((ResourceException) exception).getReason(), exception);
        } else {
            debug.error("Unable to publish {} audit event '{}' due to error: [{}]",
                    topic, eventName, exception.getMessage(), exception);
        }
    }

    private String getValue(JsonValue jsonValue, String key, String defaultValue) {
        return jsonValue.isDefined(key) ? jsonValue.get(key).asString() : defaultValue;
    }
}
