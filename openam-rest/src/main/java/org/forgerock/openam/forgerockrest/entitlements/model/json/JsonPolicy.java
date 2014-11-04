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
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.ResourceAttribute;
import java.util.Date;
import java.util.Set;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.annotate.JsonUnwrapped;
import org.forgerock.util.Reject;

/**
 * A representation of a policy for the entitlements resource. Provides a thin wrapper over an underlying
 * privilege implementation to allow easy marshalling to/from JSON schema. Note that the structure of this class and
 * its sub-components maps directly to the JSON schema via Jackson data binding conventions so care should be taken
 * when making alterations.
 *
 * @since 12.0.0
 */
@JsonPropertyOrder({
        "name", "active", "description", "entitlement", "subject", "condition",
        "resourceAttributes", "lastModifiedBy", "lastModifiedDate", "createdBy", "creationDate"
})
public final class JsonPolicy {

    /**
     * The underlying privilege object to delegate getters and setters to.
     */
    private final Privilege privilege;

    /**
     * Constructs a JSON wrapper policy using the given privilege object as the underlying policy implementation.
     *
     * @param privilege the policy implementation.
     * @throws EntitlementException if the policy is invalid.
     * @throws NullPointerException if the policy is null.
     */
    public JsonPolicy(Privilege privilege) throws EntitlementException {
        Reject.ifNull(privilege);
        this.privilege = privilege;
        if (privilege.getEntitlement() == null) {
            privilege.setEntitlement(new Entitlement());
        }
    }

    /**
     * Default constructor for Jackson to use. Creates a concrete privilege using
     * {@link com.sun.identity.entitlement.Privilege#getNewInstance()}.
     *
     * @throws EntitlementException if a fresh privilege cannot be created.
     */
    public JsonPolicy() throws EntitlementException {
        this(Privilege.getNewInstance());
    }

    /**
     * Returns the underlying privilege instance that this JsonPolicy is wrapping.
     *
     * @return the privilege instance.
     */
    @JsonIgnore
    public Privilege asPrivilege() {
        return privilege;
    }

    /**
     * Returns the name of this policy.
     *
     * @return the name of the policy.
     */
    public String getName() {
        return privilege.getName();
    }

    /**
     * Set the policy name.
     *
     * @param name
     *         The name of the policy
     *
     * @throws EntitlementException
     *         Should some error occur whilst setting the name
     */
    public void setName(String name) throws EntitlementException {
        privilege.setName(name);
    }

    /**
     * Returns {@code true} if this policy is active (i.e., in use).
     *
     * @return true if the policy is active, otherwise false.
     */
    public boolean isActive() {
        return privilege.isActive();
    }

    /**
     * Activates/deactivates the policy.
     *
     * @param active whether the policy should be active.
     */
    public void setActive(boolean active) {
        privilege.setActive(active);
    }

    /**
     * Gets the date/time at which the policy was first created.
     *
     * @return the creation date/time.
     */
    public Date getCreationDate() {
        return new Date(privilege.getCreationDate());
    }

    /**
     * Gets the DN of the user that created the policy.
     *
     * @return the user who created the policy.
     */
    public String getCreatedBy() {
        return privilege.getCreatedBy();
    }

    /**
     * Gets the last modified timestamp of the policy.
     *
     * @return the last modified timestamp.
     */
    public Date getLastModifiedDate() {
        return new Date(privilege.getLastModifiedDate());
    }

    /**
     * Gets the DN of the user that last modified this policy.
     *
     * @return the user who last modified the policy.
     */
    public String getLastModifiedBy() {
        return privilege.getLastModifiedBy();
    }

    /**
     * Gets the description of this policy. May be null.
     *
     * @return the description of the policy.
     */
    public String getDescription() {
        return privilege.getDescription();
    }

    /**
     * Sets the description of the policy.
     *
     * @param description the description.
     */
    public void setDescription(String description) {
        privilege.setDescription(description);
    }

    /**
     * Gets the entitlement used to identify resources and actions for this policy. Note that this object will not be
     * fully constructed, as e.g., the advice and attributes fields are not used in this case. We unwrap the entitlement
     * into individual fields in the policy definition to avoid reusing the term entitlement for something that isn't
     * actually an entitlement.
     *
     * @return the entitlement object.
     */
    @JsonUnwrapped
    public JsonEntitlementPattern getEntitlement() {
        return new JsonEntitlementPattern(privilege.getEntitlement());
    }

    /**
     * Sets the entitlement pattern used to determine which resources and actions this policy is applicable to.
     *
     * @param jsonEntitlement the JSON wrapped entitlement to set on the policy.
     * @throws EntitlementException if the entitlement is invalid, including if its resource list is empty.
     */
    public void setEntitlement(JsonEntitlementPattern jsonEntitlement) throws EntitlementException {

        if (jsonEntitlement.asEntitlement() != null && (jsonEntitlement.asEntitlement().getResourceNames() == null ||
                jsonEntitlement.asEntitlement().getResourceNames().size() == 0)) {
            throw new EntitlementException(EntitlementException.RESOURCE_LIST_EMPTY);
        }

        privilege.setEntitlement(jsonEntitlement.asEntitlement());
    }

    /**
     * Gets the condition used in this policy. May be null. May be a composite condition with an arbitrarily nested
     * logical structure of sub-conditions.
     *
     * @return the condition associated with this policy, if one is defined.
     */
    public EntitlementCondition getCondition() {
        return privilege.getCondition();
    }

    /**
     * Sets the condition to use for this policy.
     *
     * @param condition the condition to use. May be null to remove any existing condition.
     */
    public void setCondition(EntitlementCondition condition) {
        privilege.setCondition(condition);
    }

    /**
     * Gets the subject to match for this policy.
     *
     * @return the subject defined in the policy, or null if not defined.
     */
    public EntitlementSubject getSubject() {
        return privilege.getSubject();
    }

    /**
     * Sets the subject to use for this policy.
     *
     * @param subject the subject.
     * @throws EntitlementException if the subject is invalid.
     */
    public void setSubject(EntitlementSubject subject) throws EntitlementException {
        privilege.setSubject(subject);
    }

    /**
     * Gets the resource attribute providers for this policy. These are used to add additional static or
     * dynamic attributes onto responses from policy decisions.
     *
     * @return the set of resource attribute providers configured for this policy.
     */
    public Set<ResourceAttribute> getResourceAttributes() {
        return privilege.getResourceAttributes();
    }

    /**
     * Sets the resource attribute providers to use when evaluating this policy.
     *
     * @param attributes the attribute providers to use.
     */
    public void setResourceAttributes(Set<ResourceAttribute> attributes) {
        privilege.setResourceAttributes(attributes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JsonPolicy)) {
            return false;
        }

        JsonPolicy that = (JsonPolicy) o;

        return privilege.equals(that.privilege);

    }

    @Override
    public int hashCode() {
        return privilege.hashCode();
    }

    @Override
    public String toString() {
        return "JsonPolicy{" +
                "privilege=" + privilege +
                '}';
    }
}
