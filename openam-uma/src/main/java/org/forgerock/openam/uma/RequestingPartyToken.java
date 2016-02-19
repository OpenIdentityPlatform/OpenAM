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

package org.forgerock.openam.uma;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.utils.Time.*;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.tokens.Converter;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.Field;
import org.forgerock.openam.tokens.JsonValueToJsonBytesConverter;
import org.forgerock.openam.tokens.LongToCalendarConverter;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.openam.tokens.Type;

import com.fasterxml.jackson.databind.ObjectMapper;

@Type(TokenType.REQUESTING_PARTY)
public class RequestingPartyToken implements UmaToken {
    @Field(field = CoreTokenField.TOKEN_ID, generated = true)
    private String id;
    @Field(field = CoreTokenField.STRING_ONE)
    private String realm;
    @Field(field = CoreTokenField.STRING_TWO)
    private String resourceServerClientId;
    @Field(field = CoreTokenField.STRING_THREE)
    private String permissionTicketId;
    @Field(field = CoreTokenField.STRING_FOUR)
    private String clientClientId;
    @Field(field = CoreTokenField.BLOB, converter = PermissionsSetConverter.class)
    private Set<Permission> permissions;
    @Field(field = CoreTokenField.EXPIRY_DATE, converter = LongToCalendarConverter.class)
    private Long expiryTime;

    public RequestingPartyToken() {}

    public RequestingPartyToken(String id, String resourceServerClientId, Set<Permission> permissions,
            long expiryTime, String permissionTicketId, String clientClientId) {
        this.id = id;
        this.resourceServerClientId = resourceServerClientId;
        this.permissions = permissions;
        this.expiryTime = expiryTime;
        this.permissionTicketId = permissionTicketId;
        this.clientClientId = clientClientId;
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

    public boolean isExpired() {
        return currentTimeMillis() > expiryTime;
    }

    public void setExpiryTime(Long expiryTime) {
        this.expiryTime = expiryTime;
    }

    public String getResourceServerClientId() {
        return resourceServerClientId;
    }

    public void setResourceServerClientId(String resourceServerClientId) {
        this.resourceServerClientId = resourceServerClientId;
    }

    public String getPermissionTicketId() {
        return permissionTicketId;
    }

    public void setPermissionTicketId(String permissionTicketId) {
        this.permissionTicketId = permissionTicketId;
    }

    public String getClientClientId() {
        return clientClientId;
    }

    public void setClientClientId(String clientClientId) {
        this.clientClientId = clientClientId;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public static final class PermissionsSetConverter implements Converter<Set<Permission>, byte[]> {

        private static final String RESOURCE_SET_ID = "rsID";
        private static final String SCOPES = "scopes";
        private static final String EXPIRES = "exp";

        private final JsonValueToJsonBytesConverter jsonValueConverter;

        @Inject
        public PermissionsSetConverter(@Named("cts-json-object-mapper") ObjectMapper mapper) {
            this.jsonValueConverter = new JsonValueToJsonBytesConverter(mapper);
        }

        @Override
        public byte[] convertFrom(Set<Permission> permissions) {
            JsonValue value = new JsonValue(json(array()));
            for (Permission p : permissions) {
                JsonValue permission = json(object(
                        field(RESOURCE_SET_ID, p.getResourceSetId()),
                        field(SCOPES, p.getScopes())));
                if (p.getExpiryTime() != null) {
                    permission.add(EXPIRES, p.getExpiryTime());
                }
                value.add(permission.getObject());
            }
            return jsonValueConverter.convertFrom(value);
        }

        @Override
        public Set<Permission> convertBack(byte[] bytes) {
            JsonValue value = jsonValueConverter.convertBack(bytes);
            Set<Permission> permissions = new LinkedHashSet<Permission>();
            for (JsonValue permission : value) {
                Permission p = new Permission();
                p.setResourceSetId(permission.get(RESOURCE_SET_ID).asString());
                p.setScopes(permission.get(SCOPES).asSet(String.class));
                if (permission.isDefined(EXPIRES)) {
                    p.setExpiryTime(permission.get(EXPIRES).asLong());
                }
                permissions.add(p);
            }
            return permissions;
        }
    }
}
