/*
 * Copyright 2014 ForgeRock, AS.
 *
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
 */

package org.forgerock.openam.forgerockrest.entitlements.model.json;

import com.sun.identity.entitlement.Entitlement;
import java.util.Map;
import java.util.Set;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.forgerock.util.Reject;

/**
 * Wrapper for an {@link com.sun.identity.entitlement.Entitlement} object to provide a nicer JSON format.
 *
 * @since 12.0.0
 */
public class JsonEntitlement {
    private final Entitlement entitlement;

    /**
     * Constructs a JSON wrapper around the given entitlement implementation.
     *
     * @param entitlement the entitlement to wrap for JSON.
     */
    public JsonEntitlement(Entitlement entitlement) {
        Reject.ifNull(entitlement);
        this.entitlement = entitlement;
    }

    /**
     * Constructs a JSON wrapper using a new blank entitlement. Used by Jackson when deserialising entitlements and
     * policies.
     */
    public JsonEntitlement() {
        this(new Entitlement());
    }

    /**
     * Returns the underlying entitlement instance.
     *
     * @return the entitlement instance.
     */
    @JsonIgnore
    public Entitlement asEntitlement() {
        return entitlement;
    }

    /**
     * Returns the name of this entitlement (if set), or null otherwise.
     * @return the name of this entitlement, or null if not set.
     */
    public String getName() {
        return entitlement.getName();
    }

    /**
     * Sets the name of this entitlement. This is optional.
     *
     * @param name the name to use for this entitlement.
     */
    public void setName(String name) {
        entitlement.setName(name);
    }

    /**
     * Since excluded resource names were removed, there are only included resource names.  Since those are just
     * a set of strings, that is what this function now returns
     *
     * @return the set of resource names.
     */
    public Set<String> getResources() {
        return entitlement.getResourceNames();
    }

    /**
     * Sets the included and excluded resource names on the entitlement.
     *
     * @param resources the resources to set.
     */
    public void setResources(Set<String> resources) {
        entitlement.setResourceNames(resources);
    }

    /**
     * Gets the name of the application that this entitlement refers to.
     *
     * @return the entitlement application name.
     */
    public String getApplicationName() {
        return entitlement.getApplicationName();
    }

    /**
     * Sets the name of the application that this entitlement refers to. Must be a valid application name within this
     * OpenAM instance.
     *
     * @param applicationName the application name.
     */
    public void setApplicationName(String applicationName) {
        entitlement.setApplicationName(applicationName);
    }

    /**
     * Gets the set of action values defined for this entitlement. The result is a map from action names to a boolean
     * allow/deny decision ({@code true} means allow).
     *
     * @return the action values for this entitlement.
     */
    public Map<String, Boolean> getActionValues() {
        return entitlement.getActionValues();
    }

    /**
     * Sets the action values for this entitlement. This should only be used when defining a policy. The action names
     * should match those available for this application.
     *
     * @param actionValues the action values for the entitlement.
     */
    public void setActionValues(Map<String, Boolean> actionValues) {
        entitlement.setActionValues(actionValues);
    }

    /**
     * Returns any advice associated with this entitlement decision. Read-only.
     *
     * @return the advice associated with the entitlement.
     */
    public Map<String, Set<String>> getAdvice() {
        return entitlement.getAdvices();
    }

    /**
     * Returns any attributes associated with this entitlement decision. Read-only.
     *
     * @return the attributes associated with this entitlement.
     */
    public Map<String, Set<String>> getAttributes() {
        return entitlement.getAttributes();
    }

    /**
     * Gets the time-to-live value of this entitlement decision in milliseconds.
     *
     * @return the time-to-live value of the decision in milliseconds.
     */
    public long getTTL() {
        return entitlement.getTTL();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JsonEntitlement)) {
            return false;
        }

        JsonEntitlement that = (JsonEntitlement) o;

        return entitlement.equals(that.entitlement);
    }

    @Override
    public int hashCode() {
        return entitlement.hashCode();
    }

    @Override
    public String toString() {
        return "JsonEntitlement{" +
                "entitlement=" + entitlement +
                '}';
    }
}
