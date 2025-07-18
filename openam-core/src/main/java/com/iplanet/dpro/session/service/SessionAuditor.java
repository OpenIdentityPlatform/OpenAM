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
 * Portions Copyrighted 2022-2025 3A Systems, LLC.
 */
package com.iplanet.dpro.session.service;

import static org.forgerock.openam.audit.AMAuditEventBuilderUtils.getUserId;
import static org.forgerock.openam.audit.AuditConstants.*;
import static org.forgerock.openam.audit.AuditConstants.ConfigOperation.CREATE;
import static org.forgerock.openam.audit.AuditConstants.ConfigOperation.DELETE;
import static org.forgerock.openam.audit.AuditConstants.ConfigOperation.UPDATE;
import static org.forgerock.openam.audit.AuditConstants.EventName.*;
import static org.forgerock.openam.utils.StringUtils.isEmpty;

import java.security.AccessController;
import java.security.PrivilegedAction;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.openam.audit.AMActivityAuditEventBuilder;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.openam.core.DNWrapper;
import org.forgerock.openam.session.SessionEventType;

import com.iplanet.dpro.session.share.SessionInfo;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.Constants;

/**
 * Responsible for publishing audit activity for changes to {@link SessionInfo} objects.
 *
 * @since 13.0.0
 */
@Singleton
public class SessionAuditor implements InternalSessionListener {

    private final AuditEventPublisher auditEventPublisher;
    private final AuditEventFactory auditEventFactory;
    private final PrivilegedAction<SSOToken> adminTokenAction;
    private final DNWrapper dnWrapper;

    /**
     * Create a new Auditor.
     *
     * @param auditEventPublisher AuditEventPublisher to which publishing of events can be delegated.
     * @param auditEventFactory AuditEventFactory for audit event builders.
     * @param adminTokenAction PrivilegedAction for populating the audit event runas field with the admin user ID.
     */
    @Inject
    public SessionAuditor(
            AuditEventPublisher auditEventPublisher,
            AuditEventFactory auditEventFactory,
            PrivilegedAction<SSOToken> adminTokenAction,
            DNWrapper dnWrapper) {
        this.auditEventPublisher = auditEventPublisher;
        this.auditEventFactory = auditEventFactory;
        this.adminTokenAction = adminTokenAction;
        this.dnWrapper = dnWrapper;
    }

    @Override
    public void onEvent(final InternalSessionEvent event) {
        auditActivity(event.getInternalSession().toSessionInfo(), event.getType(), event.getTime());
    }

    public void auditActivity(SessionInfo sessionInfo, SessionEventType eventType, long timestamp) {
        switch (eventType) {
            case SESSION_CREATION:
                auditActivity(sessionInfo, AM_SESSION_CREATED, CREATE, timestamp);
                break;
            case IDLE_TIMEOUT:
                auditActivity(sessionInfo, AM_SESSION_IDLE_TIMED_OUT, DELETE, timestamp);
                break;
            case MAX_TIMEOUT:
                auditActivity(sessionInfo, AM_SESSION_MAX_TIMED_OUT, DELETE, timestamp);
                break;
            case LOGOUT:
                auditActivity(sessionInfo, AM_SESSION_LOGGED_OUT, DELETE, timestamp);
                break;
            case DESTROY:
                auditActivity(sessionInfo, AM_SESSION_DESTROYED, DELETE, timestamp);
                break;
            case PROPERTY_CHANGED:
                auditActivity(sessionInfo, AM_SESSION_PROPERTY_CHANGED, UPDATE, timestamp);
                break;
            case EVENT_URL_ADDED:
                auditActivity(sessionInfo, AM_SESSION_EVENT_URL_ADDED, UPDATE, timestamp);
                break;
            default:
                // ignore other session events
        }
    }

    private void auditActivity(SessionInfo sessionInfo, EventName eventName, ConfigOperation operation, long timestamp) {
        String realm = sessionInfo.getClientDomain();
        realm = isEmpty(realm) ? NO_REALM : dnWrapper.orgNameToRealmName(realm);

        if (auditEventPublisher.isAuditing(realm, ACTIVITY_TOPIC, eventName)) {

            String contextId = sessionInfo.getProperties().get(Constants.AM_CTX_ID);
            String uid = sessionInfo.getProperties().get(Constants.UNIVERSAL_IDENTIFIER);

            AMActivityAuditEventBuilder builder = auditEventFactory.activityEvent(realm)
                    .transactionId(AuditRequestContext.getTransactionIdValue())
                    .timestamp(timestamp)
                    .eventName(eventName)
                    .component(Component.SESSION)
                    .trackingId(contextId)
                    .runAs(getUserId(getAdminToken()))
                    .objectId(contextId)
                    .operation(String.valueOf(operation));

            if (StringUtils.isNotEmpty(uid)) {
                builder.userId(uid);
            }

            auditEventPublisher.tryPublish(ACTIVITY_TOPIC, builder.toEvent());

        }
    }

    private SSOToken getAdminToken() {
        return AccessController.doPrivileged(adminTokenAction);
    }

}
