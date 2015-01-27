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
package com.sun.identity.entitlement;

import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    private final String name;
    private final String realm;
    private final String description;
    private final Set<String> patterns;
    private final Map<String, Boolean> actions;

    private volatile int hashCode = 0;

    public static class Builder {
        private String name;
        private String realm;
        private String description;
        private Set<String> patterns = new HashSet<String>();
        private Map<String, Boolean> actions = new HashMap<String, Boolean>();

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
         * Add the description for the resource type.
         * @param description
         * @return The ResourceType builder.
         */
        public Builder setDescription(String description) {
            this.description = description;
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
         * Construct the ResourceType with the parameters set on this builder.
         * @return An instance of ResourceType.
         */
        public ResourceType build() {
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
        this.name = builder.name;
        this.realm = builder.realm;
        this.description = builder.description;
        this.patterns = Collections.unmodifiableSet(builder.patterns);
        this.actions = Collections.unmodifiableMap(builder.actions);
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
                && rt.actions.equals(actions);
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
            hashCode = result;
        }
        return result;
    }

}
