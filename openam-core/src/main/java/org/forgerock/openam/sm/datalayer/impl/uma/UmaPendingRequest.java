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

import static org.forgerock.json.fluent.JsonValue.*;

import java.util.Calendar;
import java.util.Set;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.Field;
import org.forgerock.openam.tokens.SetToJsonBytesConverter;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.openam.tokens.Type;
import org.forgerock.opendj.ldap.GeneralizedTime;

/**
 * A data layer persistent object for UMA Pending requests.
 *
 * @since 13.0.0
 */
@Type(TokenType.UMA_PENDING_REQUEST)
public class UmaPendingRequest {
    @Field(field = CoreTokenField.TOKEN_ID, generated = true)
    private String id;
    @Field(field = CoreTokenField.STRING_ONE)
    private String resourceSetId;
    @Field(field = CoreTokenField.STRING_TWO)
    private String resourceOwnerId;
    @Field(field = CoreTokenField.STRING_THREE)
    private String requestingPartyId;
    @Field(field = CoreTokenField.BLOB, converter = SetToJsonBytesConverter.class)
    private JsonValue blob;

    public UmaPendingRequest() {
    }

    public UmaPendingRequest(String resourceSetId, String resourceOwnerId, String requestingPartyId,
            Set<String> scopes) {
        this.resourceSetId = resourceSetId;
        this.resourceOwnerId = resourceOwnerId;
        this.requestingPartyId = requestingPartyId;
        this.blob = json(object(
                field("scopes", scopes),
                field("requestedAt", GeneralizedTime.valueOf(Calendar.getInstance()).toString())));
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

    public String getResourceOwnerId() {
        return resourceOwnerId;
    }

    public void setResourceOwnerId(String resourceOwnerId) {
        this.resourceOwnerId = resourceOwnerId;
    }

    public String getRequestingPartyId() {
        return requestingPartyId;
    }

    public void setRequestingPartyId(String requestingPartyId) {
        this.requestingPartyId = requestingPartyId;
    }

    public Set<String> getScopes() {
        return blob.get("scopes").asSet(String.class);
    }

    public void setScopes(Set<String> scopes) {
        blob.put("scopes", scopes);
    }

    public Calendar getRequestedAt() {
        return GeneralizedTime.valueOf(blob.get("requestedAt").asString()).toCalendar();
    }

    public void setRequestedAt(Calendar requestedAt) {
        blob.put("requestedAt", GeneralizedTime.valueOf(requestedAt).toString());
    }

    public JsonValue asJson() {
        return json(object(
                field("_id", id),
                field("user", requestingPartyId),
                field("resource", resourceSetName),
                field("when", getRequestedAt()),
                field("permissions", getScopes())));
    }
}
