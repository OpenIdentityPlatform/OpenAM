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

import java.util.Set;

import org.forgerock.audit.events.AuditEvent;
import org.forgerock.services.context.Context;

import com.iplanet.sso.SSOToken;

/**
 * A common interface for all OpenAM audit event builders.
 * @param <T> The self-referencing type, for builder return values.
 */
public interface AMAuditEventBuilder<T extends AMAuditEventBuilder<T>> {
    /**
     * Provide value for "realm" audit log field.
     *
     * @param value Value that should be stored in the 'realm' audit log field.
     * @return this builder for method chaining.
     */
    T realm(String value);

    /**
     * Provide value for "component" audit log field.
     *
     * @param value one of the predefined names from {@link AuditConstants.Component}
     * @return this builder for method chaining.
     */
    T component(AuditConstants.Component value);

    /**
     * Adds trackingId from property of {@link SSOToken}, if the provided
     * <code>SSOToken</code> is not <code>null</code>.
     *
     * @param ssoToken The SSOToken from which the trackingId value will be retrieved.
     * @return this builder
     */
    T trackingIdFromSSOToken(SSOToken ssoToken);

    /**
     * Sets the provided name for the event. This method is preferred over
     * {@link org.forgerock.audit.events.AuditEventBuilder#eventName(String)} as it allows OpenAM to manage event
     * names better and documentation to be automatically generated for new events.
     *
     * @param name one of the predefined names from {@link AuditConstants.EventName}
     * @return this builder
     */
    T eventName(AuditConstants.EventName name);

    /**
     * Expose method from {@link org.forgerock.audit.events.AuditEventBuilder}.
     * @return The event.
     */
    AuditEvent toEvent();

    /**
     * Expose method from {@link org.forgerock.audit.events.AuditEventBuilder}.
     * @param timestamp The timestamp.
     * @return This builder.
     */
    T timestamp(long timestamp);

    /**
     * Expose method from {@link org.forgerock.audit.events.AuditEventBuilder}.
     * @param id The transaction ID.
     * @return This builder.
     */
    T transactionId(String id);

    /**
     * Expose method from {@link org.forgerock.audit.events.AuditEventBuilder}.
     * @param id The user ID.
     * @return This builder.
     */
    T userId(String id);

    /**
     * Expose method from {@link org.forgerock.audit.events.AuditEventBuilder}.
     * @param trackingIdValue A tracking ID.
     * @return This builder.
     */
    T trackingId(String trackingIdValue);

    /**
     * Expose method from {@link org.forgerock.audit.events.AuditEventBuilder}.
     * @param trackingIdValues A set of tracking IDs.
     * @return This builder.
     */
    T trackingIds(Set<String> trackingIdValues);

    /**
     * Expose method from {@link org.forgerock.audit.events.AuditEventBuilder}.
     * @param context The request context.
     * @return This builder.
     */
    T transactionIdFromContext(Context context);
}
