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

import static org.forgerock.openam.audit.AMAccessAuditEventBuilder.amAccessEvent;
import static org.forgerock.openam.audit.JsonUtils.*;

import org.forgerock.audit.events.AuditEvent;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

/**
 * @since 13.0.0
 */
public class AMAccessAuditEventBuilderTest {

    @Test
    public void canBuildAccessAuditEvent() throws Exception {
        AuditEvent accessEvent = amAccessEvent()
                .timestamp(1436389263629L)
                .eventName("AM-REST-1")
                .component("REST")
                .transactionId("ad1f26e3-1ced-418d-b6ec-c8488411a625")
                .authentication("id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org")
                .contextId("uniqueSessionAlias")
                .domain("dc=openam,dc=forgerock,dc=org")
                .client("172.16.101.7", 62375)
                .server("216.58.208.36", 80)
                .resourceOperation("/some/path", "CREST", "READ")
                .http("GET", "/some/path", "p1=v1&p2=v2", Collections.<String, List<String>>emptyMap())
                .response("200", 42)
                .extraInfo("extra", "info")
                .toEvent();

        assertJsonValue(accessEvent.getValue(), "/access-event.json");
    }

}

