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
package com.iplanet.dpro.session.service;

import static org.forgerock.openam.audit.AMAuditEventBuilderUtils.getUserId;
import static org.forgerock.openam.audit.AuditConstants.ACTIVITY_TOPIC;
import static org.forgerock.openam.audit.AuditConstants.*;
import static org.forgerock.openam.utils.StringUtils.isEmpty;

import com.ctc.wstx.util.StringUtil;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.DNMapper;
import org.apache.commons.lang.StringUtils;
import org.forgerock.audit.events.AuditEvent;
import org.forgerock.openam.audit.AMActivityAuditEventBuilder;
import org.forgerock.openam.audit.AuditConstants.EventName;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.context.AuditRequestContext;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Responsible for publishing audit activity for changes to {@link InternalSession} objects.
 *
 * @since 13.0.0
 */
@Singleton
public final class SessionAuditor {

    private final AuditEventPublisher auditEventPublisher;
    private final AuditEventFactory auditEventFactory;
    private final PrivilegedAction<SSOToken> adminTokenAction;

    /**
     * Create a new Auditor.
     *
     * @param auditEventPublisher AuditEventPublisher to which publishing of events can be delegated.
     * @param auditEventFactory   AuditEventFactory for audit event builders.
     */
    @Inject
    public SessionAuditor(
            AuditEventPublisher auditEventPublisher,
            AuditEventFactory auditEventFactory,
            PrivilegedAction<SSOToken> adminTokenAction) {
        this.auditEventPublisher = auditEventPublisher;
        this.auditEventFactory = auditEventFactory;
        this.adminTokenAction = adminTokenAction;
    }

    public void auditActivity(InternalSession session, EventName eventName) {
        String realm = session.getClientDomain();
        realm = isEmpty(realm) ? NO_REALM : DNMapper.orgNameToRealmName(realm);

        if (auditEventPublisher.isAuditing(realm, ACTIVITY_TOPIC)) {

            String contextId = session.getProperty(Constants.AM_CTX_ID);

            AMActivityAuditEventBuilder builder = auditEventFactory.activityEvent()
                    .transactionId(AuditRequestContext.getTransactionIdValue())
                    .eventName(eventName)
                    .component(Component.SESSION)
                    .trackingId(contextId)
                    .runAs(getUserId(getAdminToken()))
                    .objectId(contextId)
                    .operation(getCrudType(eventName))
                    .realm(realm);

            String uid = session.getProperty(Constants.UNIVERSAL_IDENTIFIER);
            if (StringUtils.isNotEmpty(uid)) {
                builder.userId(uid);
            }

            auditEventPublisher.tryPublish(ACTIVITY_TOPIC, builder.toEvent());

        }
    }

    private String getCrudType(EventName eventName) {
        switch (eventName) {
            case AM_SESSION_CREATED:
                return "CREATE";
            case AM_SESSION_IDLE_TIMED_OUT:
            case AM_SESSION_MAX_TIMED_OUT:
            case AM_SESSION_LOGGED_OUT:
            case AM_SESSION_DESTROYED:
                return "DELETE";
            case AM_SESSION_REACTIVATED:
            case AM_SESSION_PROPERTY_CHANGED:
                return "UPDATE";
            default:
                return "";
        }
    }

    private SSOToken getAdminToken() {
        return AccessController.doPrivileged(adminTokenAction);
    }

}
