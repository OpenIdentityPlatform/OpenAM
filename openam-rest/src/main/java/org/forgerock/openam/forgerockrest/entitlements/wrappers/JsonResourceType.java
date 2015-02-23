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
package org.forgerock.openam.forgerockrest.entitlements.wrappers;

import com.sun.identity.entitlement.EntitlementException;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.entitlement.ResourceType;
import org.forgerock.openam.utils.JsonValueBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper for the conversion of ResourceType to and from JSON.
 *
 * Taking an instance of an {@link org.forgerock.openam.entitlement.ResourceType} this class exposes the necessary
 * parts of that class to the Jackson {@link org.codehaus.jackson.map.ObjectMapper} via annotations.
 */
public class JsonResourceType {

    // Field used as resource ID
    public static String FIELD_NAME = "name";

    private String uuid;
    private String name;
    private String realm;
    private String description;
    private Set<String> patterns;
    private Map<String, Boolean> actions;
    private String createdBy;
    private long creationDate;
    private String lastModifiedBy;
    private long lastModifiedDate;

    public JsonResourceType() {
        // Default constructor
    }

    public JsonResourceType(final ResourceType resourceType) {
        uuid = resourceType.getUUID();
        name = resourceType.getName();
        realm = resourceType.getRealm();
        description = resourceType.getDescription();
        patterns = resourceType.getPatterns();
        actions = resourceType.getActions();
        createdBy = resourceType.getCreatedBy();
        creationDate = resourceType.getCreationDate();
        lastModifiedBy = resourceType.getLastModifiedBy();
        lastModifiedDate = resourceType.getLastModifiedDate();
    }

    @JsonProperty("uuid")
    public String getUUID() {
        return uuid;
    }

    @JsonProperty("uuid")
    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("realm")
    public String getRealm() {
        return realm;
    }

    @JsonProperty("realm")
    public void setRealm(String realm) {
        this.realm = realm;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("patterns")
    public Set<String> getPatterns() {
        return patterns;
    }

    @JsonProperty("patterns")
    public void setPatterns(Set<String> patterns) {
        this.patterns = patterns;
    }

    @JsonProperty("actions")
    public Map<String, Boolean> getActions() {
        return actions;
    }

    @JsonProperty("actions")
    public void setActions(Map<String, Boolean> actions) {
        this.actions = actions;
    }

    @JsonProperty("createdBy")
    public String getCreatedBy() {
        return createdBy;
    }

    @JsonProperty("createdBy")
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @JsonProperty("creationDate")
    public long getCreationDate() {
        return creationDate;
    }

    @JsonProperty("creationDate")
    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    @JsonProperty("lastModifiedBy")
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    @JsonProperty("lastModifiedBy")
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    @JsonProperty("lastModifiedDate")
    public long getLastModifiedDate() {
        return lastModifiedDate;
    }

    @JsonProperty("lastModifiedDate")
    public void setLastModifiedDate(long lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    @JsonIgnore
    public ResourceType getResourceType(boolean generateID) {
        final ResourceType.Builder builder = ResourceType.builder(name, realm)
                .setUUID(uuid)
                .addPatterns(patterns)
                .addActions(actions)
                .setDescription(description)
                .setCreatedBy(createdBy)
                .setCreationDate(creationDate)
                .setLastModifiedBy(lastModifiedBy)
                .setLastModifiedDate(lastModifiedDate);
        if (generateID) {
            builder.generateUUID();
        }
        return builder.build();
    }

    /**
     * Focus of this class. Calling this function will return a transportable
     * {@link org.forgerock.json.fluent.JsonValue}
     * representing the contained instantiation of {@link org.forgerock.openam.entitlement.ResourceType}.
     *
     * @return JsonValue representing the contained ResourceType
     * @throws EntitlementException if there were issues writing the value
     */
    public JsonValue toJsonValue() throws EntitlementException {
        try {
            final ObjectMapper mapper = JsonValueBuilder.getObjectMapper();
            return JsonValueBuilder.toJsonValue(mapper.writeValueAsString(this));
        } catch (IOException e) {
            throw new EntitlementException(EntitlementException.INVALID_APPLICATION_CLASS);
        }
    }
}
