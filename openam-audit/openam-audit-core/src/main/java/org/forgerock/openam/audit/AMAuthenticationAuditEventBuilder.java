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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.forgerock.openam.audit.AMAuditEventBuilderUtils.putContexts;
import static org.forgerock.openam.audit.AMAuditEventBuilderUtils.putRealm;

/**
 * Shut up checkstyle.
 */
public class AMAuthenticationAuditEventBuilder extends
        AuthenticationAuditEventBuilder<AMAuthenticationAuditEventBuilder> {

        /**
         * Provide value for "contextId" audit log field.
         *
         * @param contexts Map "contexts" value.
         * @return this builder for method chaining.
         */
        public AMAuthenticationAuditEventBuilder contexts(Map<String, String> contexts) {
                putContexts(jsonValue, contexts);
                return this;
        }

        /**
         * Provide single value which will be used in "contexts" audit log field.
         *
         * @param context Context key which will be used in the "contexts" audit log field.
         * @param contextId Context key which will be used in the "contexts" audit log field.
         * @return this builder for method chaining.
         */
        public AMAuthenticationAuditEventBuilder context(AuditConstants.Context context, String contextId) {
                putContexts(jsonValue, Collections.singletonMap(context.toString(), contextId));
                return this;
        }

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
         * Provide value for "timestamp" audit log field.
         *
         * @param value Value that should be stored in the 'timestamp' audit log field.
         * @return this builder for method chaining.
         */
        public AMAuthenticationAuditEventBuilder time(long value) {
                timestamp(value);
                return this;
        }

        /**
         * Provide value for "entries" audit log field.
         *
         * @param entries Entries that should be stored in the 'entries' audit log field.
         * @return this builder for method chaining.
         */
        public AMAuthenticationAuditEventBuilder entries(List<?> entries) {
                super.entries(entries);
                return this;
        }

        /**
         * Provide a single value for the "entries" audit log field.
         *
         * @param moduleId The "moduleId" field for the single "entries" entry in the audit logs.
         * @param result The "result" field for the single "entries" entry in the audit logs.
         * @param info The "info" field for the single "entries" entry in the audit logs.
         * @return this builder for method chaining.
         */
        public AMAuthenticationAuditEventBuilder entries(String moduleId, String result, Map<String, String> info) {
                Map<String, Object> map = new HashMap<>();
                map.put("moduleId", moduleId);
                map.put("result", result);
                map.put("info", info);
                entries(Collections.singletonList(map));
                return this;
        }
}
