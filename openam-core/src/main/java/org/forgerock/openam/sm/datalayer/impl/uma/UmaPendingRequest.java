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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.sm.datalayer.impl.uma;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.utils.Time.*;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.Field;
import org.forgerock.openam.tokens.JsonValueToJsonBytesConverter;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.openam.tokens.Type;
import org.forgerock.opendj.ldap.GeneralizedTime;

import java.util.Calendar;
import java.util.Set;

/**
 * A data layer persistent object for UMA Pending requests.
 *
 * @since 13.0.0
 */
@Type(TokenType.UMA_PENDING_REQUEST)
public class UmaPendingRequest {

    public static final String RESOURCE_SET_ID_FIELD = "resourceSetId";
    public static final String RESOURCE_OWNER_ID_FIELD = "resourceOwnerId";
    public static final String REALM_FIELD = "realm";
    public static final String REQUESTING_PARTY_ID_FIELD = "requestingPartyId";
    public static final String ID = "_id";

    @Field(field = CoreTokenField.TOKEN_ID, generated = true)
    private String id;
    @Field(field = CoreTokenField.STRING_ONE)
    private String resourceSetId;
    @Field(field = CoreTokenField.STRING_TWO)
    private String resourceSetName;
    @Field(field = CoreTokenField.USER_ID)
    private String resourceOwnerId;
    @Field(field = CoreTokenField.STRING_THREE)
    private String realm;
    @Field(field = CoreTokenField.STRING_FOUR)
    private String requestingPartyId;
    @Field(field = CoreTokenField.BLOB, converter = JsonValueToJsonBytesConverter.class)
    private JsonValue blob;

    public UmaPendingRequest() {
        this.blob = json(object(
                field("requestedAt", GeneralizedTime.valueOf(getCalendarInstance()).toString())));
    }

    public UmaPendingRequest(String resourceSetId, String resourceSetName, String resourceOwnerId, String realm,
            String requestingPartyId, Set<String> scopes) {
        this.resourceSetId = resourceSetId;
        this.resourceSetName = resourceSetName;
        this.resourceOwnerId = resourceOwnerId;
        this.realm = realm;
        this.requestingPartyId = requestingPartyId;
        this.blob = json(object(
                field("scopes", scopes),
                field("requestedAt", GeneralizedTime.valueOf(getCalendarInstance()).toString())));
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

    public String getResourceSetName() {
        return resourceSetName;
    }

    public void setResourceSetName(String resourceSetName) {
        this.resourceSetName = resourceSetName;
    }

    public String getResourceOwnerId() {
        return resourceOwnerId;
    }

    public void setResourceOwnerId(String resourceOwnerId) {
        this.resourceOwnerId = resourceOwnerId;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getRequestingPartyId() {
        return requestingPartyId;
    }

    public void setRequestingPartyId(String requestingPartyId) {
        this.requestingPartyId = requestingPartyId;
    }

    public JsonValue getBlob() {
        return blob;
    }

    public void setBlob(JsonValue blob) {
        this.blob = blob;
    }

    public Set<String> getScopes() {
        return blob.get("scopes").asSet(String.class);
    }

    public Calendar getRequestedAt() {
        return GeneralizedTime.valueOf(blob.get("requestedAt").asString()).toCalendar();
    }

    public JsonValue asJson() {
        return json(object(
                field(ID, id),
                field("user", requestingPartyId),
                field("resource", resourceSetName),
                field("when", getRequestedAt()),
                field("permissions", getScopes())));
    }
}
