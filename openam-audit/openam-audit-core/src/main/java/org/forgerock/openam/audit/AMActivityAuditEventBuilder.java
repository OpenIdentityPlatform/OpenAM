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

import com.iplanet.sso.SSOToken;
import org.forgerock.audit.events.ActivityAuditEventBuilder;

/**
 * Builder for OpenAM audit access events.
 *
 * @since 13.0.0
 */
public class AMActivityAuditEventBuilder extends ActivityAuditEventBuilder<AMActivityAuditEventBuilder>
        implements AMAuditEventBuilder<AMActivityAuditEventBuilder> {

    /**
     * Provide value for "component" audit log field.
     *
     * @param value one of the predefined names from {@link AuditConstants.Component}
     * @return this builder for method chaining.
     */
    public AMActivityAuditEventBuilder component(AuditConstants.Component value) {
        putComponent(jsonValue, value.toString());
        return this;
    }

    /**
     * Sets trackingId from property of {@link SSOToken}, if the provided
     * <code>SSOToken</code> is not <code>null</code>.
     *
     * @param ssoToken The SSOToken from which the trackingId value will be retrieved.
     * @return this builder
     */
    public AMActivityAuditEventBuilder trackingIdFromSSOToken(SSOToken ssoToken) {
        trackingId(getTrackingIdFromSSOToken(ssoToken));
        return this;
    }

    /**
     * Sets the provided name for the event. This method is preferred over
     * {@link org.forgerock.audit.events.AuditEventBuilder#eventName(String)} as it allows OpenAM to manage event
     * names better and documentation to be automatically generated for new events.
     *
     * @param name one of the predefined names from {@link AuditConstants.EventName}
     * @return this builder
     */
    public AMActivityAuditEventBuilder eventName(AuditConstants.EventName name) {
        return eventName(name.toString());
    }

    /**
     * Provide value for "realm" audit log field.
     *
     * @param value Value that should be stored in the 'realm' audit log field.
     * @return this builder for method chaining.
     */
    public AMActivityAuditEventBuilder realm(String value) {
        putRealm(jsonValue, value);
        return this;
    }
}
