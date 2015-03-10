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
package org.forgerock.openam.entitlement;

import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The ResourceType is primarily a mapping between resources and actions and is used as
 * a template for resources associated with policies.
 *
 * Examples of resource types are:
 * - URL {patterns: [http://*, https://*] , actions: [Get, Put, Post, Delete, Head, Options, Patch]}
 * - Button {patterns: [button:screen1/*, button:screen2/*] , actions: [visible, push]}
 * - Account {patterns:	[acct:1234567890], actions: [deposit, withdraw]}
 */
public final class ResourceType {

    private final String uuid;
    private final String name;
    private final String realm;
    private final String description;
    private final Set<String> patterns;
    private final Map<String, Boolean> actions;
    private final String createdBy;
    private final long creationDate;
    private final String lastModifiedBy;
    private final long lastModifiedDate;

    private volatile int hashCode = 0;

    public static class Builder {

        private String uuid;
        private String name;
        private String realm;
        private String description;
        private Set<String> patterns = new HashSet<String>();
        private Map<String, Boolean> actions = new HashMap<String, Boolean>();
        private String createdBy;
        private long creationDate;
        private String lastModifiedBy;
        private long lastModifiedDate;

        /**
         * Create a builder for ResourceType with the required parameters, name and realm.
         * @param name A unique name (in the realm) for this resource type.
         * @param realm The realm in which this resource type is visible.
         * @throws NullPointerException if either name or realm is null.
         */
        public Builder(String name, String realm) {
            Reject.ifNull(name, "ResourceType name may not be null.");
            Reject.ifNull(realm, "ResourceType realm may not be null.");
            this.name = name;
            this.realm = realm;
        }

        /**
         * Change the name for the resource type.
         * @param name A unique name (in the realm) for this resource type.
         * @return The ResourceType builder.
         */
        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        /**
         * Set the universally unique identifier for this resource type.
         * @param uuid The UUID.
         * @return The ResourceType builder.
         */
        public Builder setUUID(String uuid) {
            this.uuid = uuid;
            return this;
        }

        /**
         * Generate a universally unique identifier for the resource type.
         * @return The ResourceType builder.
         */
        public Builder generateUUID() {
            this.uuid = UUID.randomUUID().toString();
            return this;
        }

        /**
         * Add the description for the resource type.
         * @param description The description.
         * @return The ResourceType builder.
         */
        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        /**
         * Overwrites the patterns with the passed resource set.
         *
         * @param patterns
         *         the set of patterns
         *
         * @return this builder
         */
        public Builder setPatterns(Set<String> patterns) {
            this.patterns = new HashSet<String>(patterns);
            return this;
        }

        /**
         * Add a resource pattern that guides the resource for a policy.
         * @param pattern The resource pattern.
         * @return The ResourceType builder.
         */
        public Builder addPattern(String pattern) {
            patterns.add(pattern);
            return this;
        }

        /**
         * Add resource patterns that guides the resource for a policy.
         * @param patterns The resource patterns.
         * @return The ResourceType builder.
         */
        public Builder addPatterns(Set<String> patterns) {
            this.patterns.addAll(patterns);
            return this;
        }

        /**
         * Overwrites the actions with the passed action map.
         *
         * @param actions
         *         the map of actions
         *
         * @return this builder
         */
        public Builder setActions(Map<String, Boolean> actions) {
            this.actions = new HashMap<String, Boolean>(actions);
            return this;
        }

        /**
         * Add an action to the resource type and it's default value.
         * @param actionName The action name.
         * @param defaultValue The default value.
         * @return The ResourceType builder.
         */
        public Builder addAction(String actionName, Boolean defaultValue) {
            actions.put(actionName, defaultValue);
            return this;
        }

        /**
         * Add actions to the resource type with their default values.
         * @param actions The action names and their default values.
         * @return The ResourceType builder.
         */
        public Builder addActions(Map<String, Boolean> actions) {
            this.actions.putAll(actions);
            return this;
        }

        /**
         * Set the ID of the user that created the resource type.
         * @param createdBy The user ID.
         * @return The ResourceType builder.
         */
        public Builder setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        /**
         * Set the creation date of the resource type.
         * @param creationDate The creation date in milliseconds.
         * @return The ResourceType builder.
         */
        public Builder setCreationDate(long creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        /**
         * Set the ID of the user that last modified the resource type.
         * @param lastModifiedBy The user ID.
         * @return The ResourceType builder.
         */
        public Builder setLastModifiedBy(String lastModifiedBy) {
            this.lastModifiedBy = lastModifiedBy;
            return this;
        }

        /**
         * Set the last modified date of the resource type.
         * @param lastModifiedDate The last modified date.
         * @return The ResourceType builder.
         */
        public Builder setLastModifiedDate(long lastModifiedDate) {
            this.lastModifiedDate = lastModifiedDate;
            return this;
        }

        /**
         * Construct the ResourceType with the parameters set on this builder.
         * @return An instance of ResourceType.
         * @throws NullPointerException if the name or the UUID is null.
         */
        public ResourceType build() {
            Reject.ifNull(name, "ResourceType name may not be null.");
            Reject.ifNull(uuid, "ResourceType UUID may not be null.");
            return new ResourceType(this);
        }
    }

    /**
     * Create a builder for ResourceType with the required parameters, name and realm.
     * @param name A unique name (in the realm) for this resource type.
     * @param realm The realm in which this resource type is visible.
     * @throws NullPointerException if either name or realm is null.
     * @return A ResourceType builder
     */
    public static final Builder builder(String name, String realm) {
        return new Builder(name, realm);
    }

    /**
     * Construct a ResourceType with the given builder.
     * @param builder The builder that contains the parameters for the ResourceType.
     */
    private ResourceType(Builder builder) {
        this.uuid = builder.uuid;
        this.name = builder.name;
        this.realm = builder.realm;
        this.description = builder.description;
        this.patterns = Collections.unmodifiableSet(builder.patterns);
        this.actions = Collections.unmodifiableMap(builder.actions);
        this.createdBy = builder.createdBy;
        this.creationDate = builder.creationDate;
        this.lastModifiedBy = builder.lastModifiedBy;
        this.lastModifiedDate = builder.lastModifiedDate;
    }

    /**
     * Get the universally unique identifier for the resource type.
     * @return The UUID.
     */
    public String getUUID() {
        return uuid;
    }

    /**
     * Get the unique name (in the realm) for this resource type.
     * @return The resource type name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the realm in which this resource type is visible.
     * @return The realm name.
     */
    public String getRealm() {
        return realm;
    }

    /**
     * Get the description for the resource type.
     * @return The resource type description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the resource patterns that guides the resource for a policy.
     * @return An immutable set of resource patterns.
     */
    public Set<String> getPatterns() {
        return patterns;
    }

    /**
     * Get the actions available for the resource type and their default values.
     * @return An immutable map of actions with their default values.
     */
    public Map<String, Boolean> getActions() {
        return actions;
    }

    /**
     * Get the ID of the user that created the resource type.
     *
     * @return The user ID.
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Get the date this resource type was created.
     * @return The creation date.
     */
    public long getCreationDate() {
        return creationDate;
    }

    /**
     * Get the ID of the user that last modified the resource type.
     *
     * @return The user ID.
     */
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * Get the date this resource type was last modified.
     * @return The last modified date.
     */
    public long getLastModifiedDate() {
        return lastModifiedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ResourceType)) {
            return false;
        }
        ResourceType rt = (ResourceType)o;
        return rt.name.equals(name)
                && rt.realm.equals(realm)
                && StringUtils.isEqualTo(rt.description, description)
                && rt.patterns.equals(patterns)
                && rt.actions.equals(actions)
                && StringUtils.isEqualTo(rt.createdBy, createdBy)
                && rt.creationDate == creationDate
                && StringUtils.isEqualTo(rt.lastModifiedBy, lastModifiedBy)
                && rt.lastModifiedDate == lastModifiedDate
                && StringUtils.isEqualTo(rt.uuid, uuid);
    }

    @Override
    public int hashCode() {
        int result = hashCode;
        if (result == 0) {
            int prime = 31;
            result = 17;
            result = prime * result + name.hashCode();
            result = prime * result + realm.hashCode();
            result = prime * result + (description == null ? 0 : description.hashCode());
            result = prime * result + patterns.hashCode();
            result = prime * result + actions.hashCode();
            result = prime * result + (createdBy == null ? 0 : createdBy.hashCode());
            result = prime * result + (int) (creationDate ^ (creationDate >>> 32));
            result = prime * result + (lastModifiedBy == null ? 0 : lastModifiedBy.hashCode());
            result = prime * result + (int) (lastModifiedDate ^ (lastModifiedDate >>> 32));
            result = prime * result + (uuid == null ? 0 : uuid.hashCode());
            hashCode = result;
        }
        return result;
    }

    /**
     * Create a builder for this ResourceType with all fields populated and ready for modification.
     * @return A populated ResourceType builder.
     */
    public Builder builder() {
        return new Builder(name, realm).setUUID(uuid).setDescription(description)
                .addPatterns(new HashSet<String>(patterns)).addActions(new HashMap<String, Boolean>(actions))
                .setCreatedBy(createdBy).setCreationDate(creationDate)
                .setLastModifiedBy(lastModifiedBy).setLastModifiedDate(lastModifiedDate);
    }

}
