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

package org.forgerock.oauth2.resources;

import static org.forgerock.json.fluent.JsonValue.*;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.Field;
import org.forgerock.openam.tokens.JsonValueToJsonBytesConverter;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.openam.tokens.Type;

/**
 * Represents a resource set description created by an OAuth2 client (resource server).
 *
 * @since 13.0.0
 */
@Type(TokenType.RESOURCE_SET)
public class ResourceSetDescription {

    @Field(field = CoreTokenField.TOKEN_ID)
    private String id;
    @Field(field = CoreTokenField.STRING_ONE)
    private String policyUri;
    @Field(field = CoreTokenField.STRING_TWO)
    private String clientId;
    @Field(field = CoreTokenField.STRING_THREE)
    private String realm;
    @Field(field = CoreTokenField.STRING_FOUR)
    private String resourceOwnerId;
    @Field(field = CoreTokenField.BLOB, converter = JsonValueToJsonBytesConverter.class)
    private JsonValue description;

    /**
     * Constructs a new ResourceSetDescription instance.
     *
     * @param id The unique id across all resource sets.
     * @param clientId The id of the client (resource server) which created the resource set.
     * @param resourceOwnerId The id of the user that owns this resource set.
     * @param description The description of the resource set.
     */
    public ResourceSetDescription(String id, String clientId, String resourceOwnerId,
            Map<String, Object> description) {
        this.id = id;
        this.clientId = clientId;
        this.resourceOwnerId = resourceOwnerId;
        this.description = json(description);
    }

    /**
     * Bean-spec compliant constructor
     */
    public ResourceSetDescription() {}

    /**
     * Gets the unique resource set id of the resource set across all clients (resource servers).
     *
     * @return The unique id.
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the realm for the Resource Set Description.
     *
     * @return The realm.
     */
    public String getRealm() {
        return realm;
    }

    /**
     * Sets the realm for the Resource Set Description.
     *
     * @param realm The realm.
     */
    public void setRealm(String realm) {
        this.realm = realm;
    }

    /**
     * Gets the client id that created the resource set.
     *
     * @return The client id.
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Gets the resource owner id of the resource set.
     *
     * @return The resource owner id.
     */
    public String getResourceOwnerId() {
        return resourceOwnerId;
    }

    /**
     * Gets the policy uri for the resource set.
     *
     * @return The policy uri.
     */
    public String getPolicyUri() {
        return policyUri;
    }

    /**
     * Gets the name of the resource set.
     *
     * @return The resource set name.
     */
    @Field(field = CoreTokenField.STRING_SIX)
    public String getName() {
        return description.get("name").asString();
    }

    /**
     * Gets the uri of the resource set.
     *
     * @return The resource set uri.
     */
    public URI getUri() {
        return description.get("uri").asURI();
    }

    /**
     * Gets the type of the resource set.
     *
     * @return The resource set type.
     */
    public String getType() {
        return description.get("type").asString();
    }

    /**
     * Gets the set of available scopes for the resource set.
     *
     * @return The available scopes.
     */
    public Set<String> getScopes() {
        return description.get("scopes").asSet(String.class);
    }

    /**
     * Gets the resource set icon uri.
     *
     * @return The icon uri.
     */
    public URI getIconUri() {
        return description.get("icon_uri").asURI();
    }

    /**
     * Replaces the description of the resource set with the given {@code description}.
     *
     * @param description The description to replace with.
     * @return This resource set description.
     */
    public ResourceSetDescription update(Map<String, Object> description) {
        this.description = json(description);
        return this;
    }

    /**
     * Gets the resource set description as a {@code JsonValue}.
     *
     * @return The description as a {@code JsonValue}.
     */
    public JsonValue getDescription() {
        return description;
    }

    /**
     * Returns the description of the resource set as a {@code Map}.
     *
     * @return The description.
     */
    public Map<String, Object> asMap() {
        return description.asMap();
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setResourceOwnerId(String resourceOwnerId) {
        this.resourceOwnerId = resourceOwnerId;
    }

    public void setDescription(JsonValue description) {
        this.description = description;
    }

    public void setPolicyUri(String policyUri) {
        this.policyUri = policyUri;
    }

    public void setName(String name) {
        description.put("name", name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ResourceSetDescription that = (ResourceSetDescription) o;

        if (!clientId.equals(that.clientId)) return false;
        if (!description.equals(that.description)) return false;
        if (!id.equals(that.id)) return false;
        if (policyUri != null ? !policyUri.equals(that.policyUri) : that.policyUri != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + clientId.hashCode();
        result = 31 * result + (policyUri != null ? policyUri.hashCode() : 0);
        result = 31 * result + description.asMap().hashCode();
        return result;
    }
}
