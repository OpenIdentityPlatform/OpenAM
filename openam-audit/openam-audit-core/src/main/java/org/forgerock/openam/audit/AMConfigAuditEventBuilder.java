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

import static org.forgerock.openam.audit.AMAuditEventBuilderUtils.*;

import org.forgerock.audit.events.ConfigAuditEventBuilder;

import com.iplanet.sso.SSOToken;

/**
 * Builder for OpenAM audit config events.
 *
 * @since 13.0.0
 */
public final class AMConfigAuditEventBuilder extends ConfigAuditEventBuilder<AMConfigAuditEventBuilder>
        implements AMAuditEventBuilder<AMConfigAuditEventBuilder> {
    /**
     * Sets the provided name for the event. This method is preferred over
     * {@link org.forgerock.audit.events.AuditEventBuilder#eventName(String)} as it allows OpenAM to manage event
     * names better and documentation to be automatically generated for new events.
     *
     * @param name one of the predefined names from {@link AuditConstants.EventName}
     * @return this builder
     */
    public AMConfigAuditEventBuilder eventName(AuditConstants.EventName name) {
        return eventName(name.toString());
    }

    /**
     * Sets the operation for the event. This method is preferred over
     * {@link org.forgerock.audit.events.AuditEventBuilder#eventName(String)} as it allows OpenAM to manage event
     * names better and documentation to be automatically generated for new events.
     *
     * @param operation one of the predefined operations from {@link AuditConstants.ConfigOperation}
     * @return this builder
     */
    public AMConfigAuditEventBuilder operation(AuditConstants.ConfigOperation operation) {
        return operation(operation.toString());
    }

    /**
     * Sets the realm for the event.
     *
     * @param realm The name of the realm
     * @return this builder
     */
    public final AMConfigAuditEventBuilder realm(String realm) {
        putRealm(jsonValue, realm);
        return self();
    }

    @Override
    public AMConfigAuditEventBuilder component(AuditConstants.Component value) {
        putComponent(jsonValue, value.toString());
        return this;
    }

    @Override
    public AMConfigAuditEventBuilder trackingIdFromSSOToken(SSOToken ssoToken) {
        trackingId(getTrackingIdFromSSOToken(ssoToken));
        return this;
    }
}
