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

import org.forgerock.audit.events.AuthenticationAuditEventBuilder;
import org.forgerock.openam.audit.model.AuthenticationAuditEntry;
import org.forgerock.openam.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.forgerock.openam.audit.AMAuditEventBuilderUtils.putComponent;

import static org.forgerock.openam.audit.AMAuditEventBuilderUtils.putRealm;

/**
 * Builder for OpenAM audit authentication events.
 *
 * @since 13.0.0
 */
public class AMAuthenticationAuditEventBuilder extends
        AuthenticationAuditEventBuilder<AMAuthenticationAuditEventBuilder> {

    /**
     * Provide value for "realm" audit log field.
     *
     * @param value Value that should be stored in the 'realm' audit log field.
     * @return this builder for method chaining.
     */
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
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isNotEmpty(authenticationAuditEntry.getModuleId())) {
            map.put("moduleId", authenticationAuditEntry.getModuleId());
        }
        if (StringUtils.isNotEmpty(authenticationAuditEntry.getResult().toString())) {
            map.put("result", authenticationAuditEntry.getResult());
        }
        Map<String, String> info = authenticationAuditEntry.getInfo();
        if (info != null && !info.isEmpty()) {
            map.put("info", info);
        }
        entries(Collections.singletonList(map));
        return this;
    }

    /**
     * Provide value for "component" audit log field.
     *
     * @param value one of the predefined names from {@link AuditConstants.Component}
     * @return this builder for method chaining.
     */
    public AMAuthenticationAuditEventBuilder component(AuditConstants.Component value) {
        putComponent(jsonValue, value.toString());
        return this;
    }
}
