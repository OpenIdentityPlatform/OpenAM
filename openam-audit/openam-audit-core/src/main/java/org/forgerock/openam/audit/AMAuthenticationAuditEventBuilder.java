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

import static java.util.Collections.singletonList;
import static org.forgerock.openam.audit.AMAuditEventBuilderUtils.*;
import static org.forgerock.openam.utils.StringUtils.isNotEmpty;

import org.forgerock.audit.events.AuthenticationAuditEventBuilder;
import org.forgerock.openam.audit.model.AuthenticationAuditEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.iplanet.sso.SSOToken;

/**
 * Builder for OpenAM audit authentication events.
 *
 * @since 13.0.0
 */
public final class AMAuthenticationAuditEventBuilder
        extends AuthenticationAuditEventBuilder<AMAuthenticationAuditEventBuilder>
        implements AMAuditEventBuilder<AMAuthenticationAuditEventBuilder> {

    @Override
    public AMAuthenticationAuditEventBuilder realm(String value) {
        putRealm(jsonValue, value);
        return this;
    }

    /**
     * Provide value for "entries" audit log field.
     *
     * @param entries Entries that should be stored in the 'entries' audit log field.
     * @return this builder for method chaining.
     */
    public AMAuthenticationAuditEventBuilder entryList(List<AuthenticationAuditEntry> entries) {
        List<Map<String, Object>> convertedEntries = new ArrayList<>();
        for (AuthenticationAuditEntry entry : entries) {
            convertedEntries.add(entry.toMap());
        }
        super.entries(convertedEntries);
        return this;
    }

    /**
     * Provide a single value for the "entries" audit log field.
     *
     * @param authenticationAuditEntry The single entry object representing the fields to be audited in the "entries"
     *              field in the audit logs.
     * @return this builder for method chaining.
     */
    public AMAuthenticationAuditEventBuilder entry(AuthenticationAuditEntry authenticationAuditEntry) {
        if (authenticationAuditEntry != null) {
            super.entries(singletonList(authenticationAuditEntry.toMap()));
        }
        return this;
    }

    @Override
    public AMAuthenticationAuditEventBuilder component(AuditConstants.Component value) {
        putComponent(jsonValue, value.toString());
        return this;
    }

    @Override
    public AMAuthenticationAuditEventBuilder eventName(AuditConstants.EventName name) {
        return eventName(name.toString());
    }

    /**
     * A principal of the authentication event.
     *
     * @param principal the principal
     * @return an audit authentication event builder
     */
    public AMAuthenticationAuditEventBuilder principal(String principal) {
        if (isNotEmpty(principal)) {
            principal(singletonList(principal));
        }
        return this;
    }

    @Override
    public AMAuthenticationAuditEventBuilder trackingIdFromSSOToken(SSOToken ssoToken) {
        trackingId(getTrackingIdFromSSOToken(ssoToken));
        return this;
    }

}
