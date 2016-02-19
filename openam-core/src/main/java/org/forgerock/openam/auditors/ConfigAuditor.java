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
package org.forgerock.openam.auditors;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.audit.AuditConstants.*;
import static org.forgerock.openam.utils.Time.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.shared.debug.Debug;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.audit.AMAuditEventBuilderUtils;
import org.forgerock.openam.audit.AMConfigAuditEventBuilder;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.AuditConstants.*;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.context.AuditRequestContext;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Responsible for publishing audit config events for a configuration operation.
 * Contains generic methods for publishing the different types of event from standard data structures.
 *
 * @since 13.0.0
 */
public abstract class ConfigAuditor {

    private final Debug debug;
    private final AuditEventPublisher auditEventPublisher;
    private final AuditEventFactory auditEventFactory;
    private final Set<SMSAuditFilter> filters;
    private final long startTime;
    private final String runAsName;
    private final JsonValue beforeState;
    private static final ObjectMapper mapper = new ObjectMapper();
    private final String objectId;
    private final String realm;
    private final SSOToken runAs;

    /**
     * Creates the base for a ConfigAuditor
     * @param debug The debugger
     * @param auditEventPublisher The publisher being used
     * @param auditEventFactory The factory used to create the EventBuilder
     * @param runAs The user that the configuration was run as
     * @param realm The realm the configuration takes place in
     * @param objectId The id (e.g. dn) of the object being configured
     * @param initialState The initialState of the object being configured
     * @param filters The filters used to determine if the event should be audited
     */
    protected ConfigAuditor(Debug debug, AuditEventPublisher auditEventPublisher, AuditEventFactory
            auditEventFactory, SSOToken runAs, String realm,
            String objectId, Map<String, Object> initialState, Set<SMSAuditFilter> filters) {
        this.debug = debug;
        this.auditEventPublisher = auditEventPublisher;
        this.auditEventFactory = auditEventFactory;
        this.objectId = objectId;
        this.startTime = currentTimeMillis();
        this.filters = filters;
        this.runAs = runAs;

        if (realm == null) {
            this.realm = AuditConstants.NO_REALM;
        } else {
            this.realm = realm;
        }

        String runAsName = null;

        if (runAs != null) {
            runAsName = AMAuditEventBuilderUtils.getUserId(runAs);
        }

        this.runAsName = runAsName;

        this.beforeState = convertObjectToJsonValue(initialState);
    }

    /**
     * Publishes an event with details of the successfully completed configuration creation operation,
     * if the 'config' topic is audited.
     * <p/>
     * Any exception that occurs while trying to publish the audit event will be
     * captured in the debug logs but otherwise ignored.
     * @param newState The state of the entry which is created
     */
    public void auditCreate(Map<String, Object> newState) {
        if (shouldAudit(ConfigOperation.CREATE)) {
            JsonValue afterState = convertObjectToJsonValue(newState);

            AMConfigAuditEventBuilder builder = getBaseBuilder()
                    .operation(ConfigOperation.CREATE);
            recordBeforeStateIfNotNull(builder, beforeState);
            recordAfterStateIfNotNull(builder, afterState);

            auditEventPublisher.tryPublish(CONFIG_TOPIC, builder.toEvent());
        }
    }

    /**
     * Publishes an event with details of the successfully completed configuration modification operation,
     * if the 'config' topic is audited.
     * <p/>
     * Any exception that occurs while trying to publish the audit event will be
     * captured in the debug logs but otherwise ignored.
     * @param finalState The derived final state of the entry
     * @param modifiedAttributes The attributes modified
     */
    public void auditModify(Map<String, Object> finalState, String[] modifiedAttributes) {
        if (shouldAudit(ConfigOperation.UPDATE)) {
            JsonValue afterState = convertObjectToJsonValue(finalState);
            AMConfigAuditEventBuilder builder = getBaseBuilder()
                    .operation(ConfigOperation.UPDATE)
                    .changedFields(modifiedAttributes);
            recordBeforeStateIfNotNull(builder, beforeState);
            recordAfterStateIfNotNull(builder, afterState);

            auditEventPublisher.tryPublish(CONFIG_TOPIC, builder.toEvent());
        }
    }

    /**
     * Publishes an event with details of the successfully completed configuration deletion operation,
     * if the 'config' topic is audited.
     * <p/>
     * Any exception that occurs while trying to publish the audit event will be
     * captured in the debug logs but otherwise ignored.
     */
    public void auditDelete() {
        if (shouldAudit(ConfigOperation.DELETE)) {
            JsonValue afterState = json(object());
            AMConfigAuditEventBuilder builder = getBaseBuilder()
                    .operation(ConfigOperation.DELETE)
                    .after(afterState);
            recordBeforeStateIfNotNull(builder, beforeState);

            auditEventPublisher.tryPublish(CONFIG_TOPIC, builder.toEvent());
        }
    }

    private JsonValue convertObjectToJsonValue(Object obj) {
        try {
            String json = mapper.writeValueAsString(obj);
            return new JsonValue(mapper.readValue(json, Map.class));
        } catch (IOException e) {
            debug.warning("ConfigAuditor: Failed to populate field");
            return null;
        }
    }

    /**
     * Creates a builder which contains the elements common to all events. Reduces the amount of information
     * required in each event method.
     * @return The builder used as a foundation for all events
     */
    protected AMConfigAuditEventBuilder getBaseBuilder() {
        return auditEventFactory.configEvent(realm)
                .timestamp(startTime)
                .objectId(objectId)
                .runAs(runAsName)
                .transactionId(AuditRequestContext.getTransactionIdValue())
                .eventName(EventName.AM_CONFIG_CHANGE);
    }

    /**
     * Getter for the initial state of the event
     * @return The initial state (key: attribute name)
     */
    protected Map<String, Object> getInitialState() {
        return beforeState.asMap();
    }

    /**
     * Determines if a given event should be audited
     * @param operation The operation that is being applied to the object
     * @return True if auditing is enabled for configuration, and the specific operation is audited for the object.
     */
    protected boolean shouldAudit(ConfigOperation operation) {
        return auditEventPublisher.isAuditing(realm, CONFIG_TOPIC, EventName.AM_CONFIG_CHANGE)
                    && isAudited(operation);
    }

    private boolean isAudited(ConfigOperation operation) {
        for (SMSAuditFilter filter : filters) {
            if (!filter.isAudited(objectId, realm, operation, SubjectUtils.createSubject(runAs))) {
                return false;
            }
        }
        return true;
    }

    private void recordBeforeStateIfNotNull(AMConfigAuditEventBuilder builder, JsonValue beforeState) {
        if (beforeState != null) {
            builder.before(beforeState);
        }
    }

    private void recordAfterStateIfNotNull(AMConfigAuditEventBuilder builder, JsonValue afterState) {
        if (afterState != null) {
            builder.after(afterState);
        }
    }
}
