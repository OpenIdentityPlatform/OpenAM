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

import static org.forgerock.openam.audit.AuditConstants.Component.SESSION;
import static org.forgerock.openam.audit.AuditConstants.EventName.AM_SESSION_CREATED;
import static org.forgerock.openam.audit.JsonUtils.assertJsonValue;

import org.forgerock.audit.events.AuditEvent;
import org.testng.annotations.Test;

/**
 * @since 13.0.0
 */
public class AMActivityAuditEventBuilderTest {

    @Test
    public void canBuildAccessAuditEventWithContexts() throws Exception {
        AuditEvent activityEvent = new AMActivityAuditEventBuilder()
                .timestamp(1436389263629L)
                .eventName(AM_SESSION_CREATED)
                .component(SESSION)
                .transactionId("ad1f26e3-1ced-418d-b6ec-c8488411a625")
                .userId("id=demo,ou=user,dc=openam,dc=openidentityplatform,dc=org")
                .trackingId("12345")
                .runAs("cn=dsameuser,ou=DSAME Users,dc=openam,dc=openidentityplatform,dc=org")
                .objectId("/sessions/uniqueSessionAlias")
                .operation("CREATE")
                .toEvent();

        assertJsonValue(activityEvent.getValue(), "/activity-event.json");
    }

    @Test
    public void canBuildAccessAuditEventWithContext() throws Exception {
        AuditEvent activityEvent = new AMActivityAuditEventBuilder()
                .timestamp(1436389263629L)
                .eventName(AM_SESSION_CREATED)
                .component(SESSION)
                .transactionId("ad1f26e3-1ced-418d-b6ec-c8488411a625")
                .userId("id=demo,ou=user,dc=openam,dc=openidentityplatform,dc=org")
                .trackingId("12345")
                .runAs("cn=dsameuser,ou=DSAME Users,dc=openam,dc=openidentityplatform,dc=org")
                .objectId("/sessions/uniqueSessionAlias")
                .operation("CREATE")
                .toEvent();

        assertJsonValue(activityEvent.getValue(), "/activity-event.json");
    }

}
