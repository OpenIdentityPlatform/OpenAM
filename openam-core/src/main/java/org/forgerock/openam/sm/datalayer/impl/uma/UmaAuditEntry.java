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

package org.forgerock.openam.sm.datalayer.impl.uma;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.Field;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.openam.tokens.Type;

import java.util.Calendar;

import static org.forgerock.json.fluent.JsonValue.*;

@Type(TokenType.UMA_AUDIT_ENTRY)
public class UmaAuditEntry {
    @Field(field = CoreTokenField.TOKEN_ID, generated = true)
    private String id;
    @Field(field = CoreTokenField.STRING_ONE)
    private String resourceSetId;
    @Field(field = CoreTokenField.STRING_TWO)
    private String requestingPartyId;
    @Field(field = CoreTokenField.STRING_THREE)
    private String type;
    @Field(field = CoreTokenField.STRING_FOUR)
    private String resourceOwnerId;
    @Field(field = CoreTokenField.DATE_ONE)
    private Calendar eventTime;

    public UmaAuditEntry() {
    }

    public UmaAuditEntry(String resourceSetId, String resourceOwnerId, String type, String requestingPartyId) {
        this.resourceSetId = resourceSetId;
        this.resourceOwnerId = resourceOwnerId;
        this.type = type;
        this.requestingPartyId = requestingPartyId;
        this.eventTime = Calendar.getInstance();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getResourceSetId() {
        return resourceSetId;
    }

    public void setResourceSetId(String resourceSetId) {
        this.resourceSetId = resourceSetId;
    }

    public String getRequestingPartyId() {
        return requestingPartyId;
    }

    public void setRequestingPartyId(String requestingPartyId) {
        this.requestingPartyId = requestingPartyId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Calendar getEventTime() {
        return eventTime;
    }

    public void setEventTime(Calendar eventTime) {
        this.eventTime = eventTime;
    }
    public String getResourceOwnerId() {
        return resourceOwnerId;
    }

    public void setResourceOwnerId(String resourceOwnerId) {
        this.resourceOwnerId = resourceOwnerId;
    }

    public JsonValue asJson() {
        JsonValue auditEntry = json(object(
                field("resourceSetId", resourceSetId),
                field("requestingPartyId", requestingPartyId),
                field("type", type),
                field("eventTime", eventTime)));
        return auditEntry;
    }
}
