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
package org.forgerock.openam.audit.configuration;

import static java.util.Collections.emptySet;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.audit.AuditConstants.*;
import static org.forgerock.openam.utils.CollectionUtils.isNotEmpty;

import com.sun.identity.sm.DefaultValues;
import org.forgerock.json.JsonValue;

import java.util.Map;
import java.util.Set;

/**
 * Contains all the possible values for the JDBC handler event field to column mapping.
 *
 * @since 13.0.0
 */
public final class JdbcFieldToColumnDefaultValues extends DefaultValues {

    private static final JsonValue DEFAULT_VALUES = json(object(
            field(AUTHENTICATION_TOPIC, set(
                    "[_id]=id",
                    "[transactionId]=transactionid",
                    "[timestamp]=timestamp_",
                    "[eventName]=eventname",
                    "[userId]=userid",
                    "[trackingIds]=trackingids",
                    "[result]=result",
                    "[principal]=principals",
                    "[context]=context",
                    "[entries]=entries",
                    "[component]=component",
                    "[realm]=realm")),
            field(ACTIVITY_TOPIC, set(
                    "[_id]=id",
                    "[transactionId]=transactionid",
                    "[timestamp]=timestamp_",
                    "[eventName]=eventname",
                    "[userId]=userid",
                    "[trackingIds]=trackingids",
                    "[runAs]=runas",
                    "[objectId]=objectid",
                    "[operation]=operation",
                    "[before]=beforeObject",
                    "[after]=afterObject",
                    "[changedFields]=changedfields",
                    "[revision]=rev",
                    "[component]=component",
                    "[realm]=realm")),
            field(ACCESS_TOPIC, set(
                    "[_id]=id",
                    "[transactionId]=transactionid",
                    "[timestamp]=timestamp_",
                    "[eventName]=eventname",
                    "[userId]=userid",
                    "[trackingIds]=trackingids",
                    "[server/ip]=server_ip",
                    "[server/port]=server_port",
                    "[client/ip]=client_ip",
                    "[client/port]=client_port",
                    "[request/protocol]=request_protocol",
                    "[request/operation]=request_operation",
                    "[request/detail]=request_detail",
                    "[http/request/secure]=http_request_secure",
                    "[http/request/method]=http_request_method",
                    "[http/request/path]=http_request_path",
                    "[http/request/queryParameters]=http_request_queryparameters",
                    "[http/request/headers]=http_request_headers",
                    "[http/request/cookies]=http_request_cookies",
                    "[http/response/headers]=http_response_headers",
                    "[response/status]=response_status",
                    "[response/statusCode]=response_statuscode",
                    "[response/elapsedTime]=response_elapsedtime",
                    "[response/elapsedTimeUnits]=response_elapsedtimeunits",
                    "[component]=component",
                    "[realm]=realm")),
            field(CONFIG_TOPIC, set(
                    "[_id]=id",
                    "[transactionId]=transactionid",
                    "[timestamp]=timestamp_",
                    "[eventName]=eventname",
                    "[userId]=userid",
                    "[trackingIds]=trackingids",
                    "[runAs]=runas",
                    "[objectId]=objectid",
                    "[operation]=operation",
                    "[before]=beforeObject",
                    "[after]=afterObject",
                    "[changedFields]=changedfields",
                    "[revision]=rev",
                    "[component]=component",
                    "[realm]=realm"))));

    @Override
    public Set getDefaultValues() {
        String topic = null;
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> keyValues = getConfiguredKeyValues();
        if (isNotEmpty(keyValues)) {
            Set<String> values = keyValues.get("topic");
            if (isNotEmpty(values)) {
                topic = values.iterator().next();
            }
        }

        return getDefaultValues(topic);
    }

    /**
     * Get the default values for the given topic.
     *
     * @param topic The topic for which the default values are required.
     * @return The default field to column mappings.
     */
    public static Set<String> getDefaultValues(String topic) {
        if (DEFAULT_VALUES.isDefined(topic)) {
            return DEFAULT_VALUES.get(topic).asSet(String.class);
        }
        return emptySet();
    }
}
