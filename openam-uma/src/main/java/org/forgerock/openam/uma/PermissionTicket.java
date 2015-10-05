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

package org.forgerock.openam.uma;

import static org.forgerock.json.JsonValue.*;

import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.Field;
import org.forgerock.openam.tokens.JsonValueToJsonBytesConverter;
import org.forgerock.openam.tokens.LongToCalendarConverter;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.openam.tokens.Type;

/**
 * A POJO to represent the UMA Permission Ticket. See
 * <a href="https://docs.kantarainitiative.org/uma/draft-uma-core.html#h-am-register-permission">the UMA Spec</a> for
 * details.
 */
@Type(TokenType.PERMISSION_TICKET)
public class PermissionTicket implements UmaToken {
    private static final String SCOPES = "scopes";
    private static final String CLIENT_CLIENT_ID = "clientClientId";

    @Field(field = CoreTokenField.BLOB, converter = JsonValueToJsonBytesConverter.class)
    private JsonValue contents = json(object());
    @Field(field = CoreTokenField.STRING_ONE)
    private String realm;
    @Field(field = CoreTokenField.STRING_TWO)
    private String resourceSetId;
    @Field(field = CoreTokenField.STRING_THREE)
    private String resourceServerClientId;
    @Field(field = CoreTokenField.TOKEN_ID, generated = true)
    private String id;
    @Field(field = CoreTokenField.EXPIRY_DATE, converter = LongToCalendarConverter.class)
    private Long expiryTime;

    public PermissionTicket() {
    }

    public PermissionTicket(String id, String resourceSetId, Set<String> scopes, String resourceServerClientId) {
        this.id = id;
        this.resourceSetId = resourceSetId;
        this.resourceServerClientId = resourceServerClientId;
        contents.put(SCOPES, scopes);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public Long getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(Long expiryTime) {
        this.expiryTime = expiryTime;
    }

    public String getResourceSetId() {
        return resourceSetId;
    }

    public void setResourceSetId(String resourceSetId) {
        this.resourceSetId = resourceSetId;
    }

    public Set<String> getScopes() {
        JsonValue scopes = contents.get(SCOPES);
        return scopes == null ? null : scopes.asSet(String.class);
    }

    public void setScopes(Set<String> scopes) {
        contents.put(SCOPES, scopes);
    }

    public String getResourceServerClientId() {
        return resourceServerClientId;
    }

    public void setResourceServerClientId(String resourceServerClientId) {
        this.resourceServerClientId = resourceServerClientId;
    }

    public String getClientClientId() {
        JsonValue value = contents.get(CLIENT_CLIENT_ID);
        return value == null ? null : value.asString();
    }

    public void setClientClientId(String clientClientId) {
        contents.put(CLIENT_CLIENT_ID, clientClientId);
    }

    public JsonValue getContents() {
        return contents;
    }

    public void setContents(JsonValue contents) {
        this.contents = contents;
    }
}
