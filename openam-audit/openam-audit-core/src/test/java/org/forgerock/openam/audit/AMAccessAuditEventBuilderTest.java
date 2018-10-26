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

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.audit.events.AccessAuditEventBuilder.ResponseStatus.SUCCESSFUL;
import static org.forgerock.openam.audit.AuditConstants.*;
import static org.forgerock.openam.audit.JsonUtils.*;

import org.forgerock.audit.events.AuditEvent;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 13.0.0
 */
public class AMAccessAuditEventBuilderTest {

    @Test
    public void canBuildAccessAuditEventWithContexts() throws Exception {
        AuditEvent accessEvent = new AMAccessAuditEventBuilder()
                .timestamp(1436389263629L)
                .eventName(EventName.AM_ACCESS_ATTEMPT)
                .component(Component.AUDIT)
                .transactionId("ad1f26e3-1ced-418d-b6ec-c8488411a625")
                .userId("id=amadmin,ou=user,dc=openam,dc=openidentityplatform,dc=org")
                .trackingId("12345")
                .client("172.16.101.7", 62375)
                .server("216.58.208.36", 80)
                .request("CREST", "READ")
                .httpRequest(false, "GET", "/some/path", getQueryParameters(), getHeaders())
                .response(SUCCESSFUL, "200", 42, MILLISECONDS)
                .toEvent();

        assertJsonValue(accessEvent.getValue(), "/access-event.json");
    }

    @Test
    public void canBuildAccessAuditEventWithContext() throws Exception {
        AuditEvent accessEvent = new AMAccessAuditEventBuilder()
                .timestamp(1436389263629L)
                .eventName(EventName.AM_ACCESS_ATTEMPT)
                .component(Component.AUDIT)
                .transactionId("ad1f26e3-1ced-418d-b6ec-c8488411a625")
                .userId("id=amadmin,ou=user,dc=openam,dc=openidentityplatform,dc=org")
                .trackingId("12345")
                .client("172.16.101.7", 62375)
                .server("216.58.208.36", 80)
                .request("CREST", "READ")
                .httpRequest(false, "GET", "/some/path", getQueryParameters(), getHeaders())
                .response(SUCCESSFUL, "200", 42, MILLISECONDS)
                .toEvent();

        assertJsonValue(accessEvent.getValue(), "/access-event.json");
    }

    @Test
    public void canHandleNullComponent() {
        AuditEvent accessEvent = new AMAccessAuditEventBuilder()
                .timestamp(1436389263629L)
                .eventName(EventName.AM_ACCESS_ATTEMPT)
                .transactionId("ad1f26e3-1ced-418d-b6ec-c8488411a625")
                .realm(null)
                .component(null)
                .toEvent();

        assertThat(accessEvent).isNotNull();
    }

    private Map<String, List<String>> getQueryParameters() {
        HashMap<String, List<String>> queryParameters = new LinkedHashMap<>();
        queryParameters.put("p1", asList("v1"));
        queryParameters.put("p2", asList("v2"));
        return queryParameters;
    }

    private Map<String, List<String>> getHeaders() {
        HashMap<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("h1", asList("v1"));
        headers.put("h2", asList("v2"));
        headers.put("Cookie", asList("JSESSIONID=92F2583684E45A3612AAC1743FE70362; amlbcookie=01"));
        return headers;
    }

}

