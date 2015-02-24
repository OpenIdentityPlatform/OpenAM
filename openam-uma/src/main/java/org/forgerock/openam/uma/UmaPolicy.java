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

import static org.forgerock.json.fluent.JsonValue.*;
import static org.forgerock.openam.uma.UmaConstants.BackendPolicy.*;
import static org.forgerock.openam.uma.UmaConstants.UMA_POLICY_SCHEME;
import static org.forgerock.openam.uma.UmaConstants.UmaPolicy.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Resource;
import org.forgerock.oauth2.resources.ResourceSetDescription;

/**
 * Represents an UMA policy with operations to convert to and from underlying backend policies and JSON format.
 *
 * @since 13.0.0
 */
public class UmaPolicy {

    /**
     * Parses the unique resource set id, that the UMA policy relates to, from the UMA policy JSON.
     *
     * @param policy The UMA policy in JSON format.
     * @return The UMA policy ID.
     */
    public static String idOf(JsonValue policy) {
        return policy.get(POLICY_ID_KEY).asString();
    }

    /**
     * Parses the UMA policy JSON into a {@code UmaPolicy} instance.
     *
     * @param resourceSet The resource set the policy relates to.
     * @param policy The UMA policy in JSON format.
     * @return A {@code UmaPolicy} instance.
     * @throws BadRequestException If the UMA policy JSON is not valid.
     */
    public static UmaPolicy valueOf(ResourceSetDescription resourceSet, JsonValue policy) throws BadRequestException {
        validateUmaPolicy(policy);
        return new UmaPolicy(resourceSet, policy, null);
    }

    private static void validateUmaPolicy(JsonValue policy) throws BadRequestException {
        try {
            policy.get(POLICY_ID_KEY).required();
        } catch (JsonValueException e) {
            throw new BadRequestException("Invalid UMA policy. Missing required attribute, 'policyId'.");
        }
        try {
            policy.get(POLICY_ID_KEY).asString();
        } catch (JsonValueException e) {
            throw new BadRequestException("Invalid UMA policy. Required attribute, 'policyId', must be a "
                    + "String.");
        }

        try {
            policy.get(PERMISSIONS_KEY).required();
        } catch (JsonValueException e) {
            throw new BadRequestException("Invalid UMA policy. Missing required attribute, 'permissions'.");
        }
        try {
            policy.get(PERMISSIONS_KEY).asList();
        } catch (JsonValueException e) {
            throw new BadRequestException("Invalid UMA policy. Required attribute, 'permissions', must be an "
                    + "array.");
        }

        for (JsonValue permission : policy.get(PERMISSIONS_KEY)) {
            try {
                permission.get(SUBJECT_KEY).required();
            } catch (JsonValueException e) {
                throw new BadRequestException("Invalid UMA policy permission. Missing required attribute, 'subject'.");
            }
            try {
                permission.get(SUBJECT_KEY).asString();
            } catch (JsonValueException e) {
                throw new BadRequestException("Invalid UMA policy permission. Required attribute, 'subject', "
                        + "must be a String.");
            }

            try {
                permission.get(SCOPES_KEY).required();
            } catch (JsonValueException e) {
                throw new BadRequestException("Invalid UMA policy permission. Missing required attribute, 'scopes'.");
            }
            try {
                permission.get(SCOPES_KEY).asList(String.class);
            } catch (JsonValueException e) {
                throw new BadRequestException("Invalid UMA policy permission. Required attribute, 'scopes', "
                        + "must be an array of Strings.");
            }
        }
    }

    private static Set<String> getPolicySubjects(JsonValue subjectsContent) {
        Set<String> subjects = new HashSet<String>();
        for (JsonValue subject : subjectsContent.get(BACKEND_POLICY_SUBJECTS_KEY)) {
            subjects.add(subject.get(BACKEND_POLICY_SUBJECT_CLAIM_VALUE_KEY).asString());
        }
        return subjects;
    }

    /**
     * Converts underlying backend policies into an {@code UmaPolicy}.
     *
     * @param resourceSet The resource set the policy relates to.
     * @param policies The collection of underlying backend policies.
     * @return A {@code UmaPolicy} instance.
     * @throws BadRequestException If the underlying policies do not underpin a valid UMA policy.
     */
    public static UmaPolicy fromUnderlyingPolicies(ResourceSetDescription resourceSet, Collection<Resource> policies)
            throws BadRequestException {

        Set<String> underlyingPolicyIds = new HashSet<String>();
        Map<String, Set<String>> subjectPermissions = new HashMap<String, Set<String>>();
        for (Resource policy : policies) {
            underlyingPolicyIds.add(policy.getId());
            String scope = policy.getContent().get(BACKEND_POLICY_ACTION_VALUES_KEY).asMap()
                    .keySet().iterator().next();
            for (String subject : getPolicySubjects(policy.getContent().get(BACKEND_POLICY_SUBJECT_KEY))) {
                Set<String> scopes = subjectPermissions.get(subject);
                if (scopes == null) {
                    scopes = new HashSet<String>();
                    subjectPermissions.put(subject, scopes);
                }
                scopes.add(scope);
            }
        }
        List<Object> permissions = array();
        JsonValue umaPolicy = json(object(
                field(POLICY_ID_KEY, resourceSet.getId()),
                field(POLICY_NAME, resourceSet.getName()),
                field(PERMISSIONS_KEY, permissions)));
        for (Map.Entry<String, Set<String>> permission : subjectPermissions.entrySet()) {
            permissions.add(object(
                    field(SUBJECT_KEY, permission.getKey()),
                    field(SCOPES_KEY, permission.getValue())));
        }
        return new UmaPolicy(resourceSet, umaPolicy, underlyingPolicyIds);
    }

    private final ResourceSetDescription resourceSet;
    private final JsonValue policy;
    private final Collection<String> underlyingPolicyIds;
    private Set<String> scopes;
    private Set<String> subjects;

    private UmaPolicy(ResourceSetDescription resourceSet, JsonValue policy, Collection<String> underlyingPolicyIds) {
        this.resourceSet = resourceSet;
        this.policy = policy;
        this.underlyingPolicyIds = underlyingPolicyIds;
    }

    /**
     * Gets the ID of this UMA policy which is the unique resource set ID that this policy relates to.
     *
     * @return The ID.
     */
    public String getId() {
        return policy.get(POLICY_ID_KEY).asString();
    }

    /**
     * Gets the revision of this UMA policy.
     *
     * @return The revision.
     */
    public String getRevision() {
        return Long.toString(hashCode());
    }

    /**
     * Converts the {@code UmaPolicy} to JSON.
     *
     * @return The JSON representation of the UMA policy.
     */
    public JsonValue asJson() {
        return policy;
    }

    public Collection<String> getUnderlyingPolicyIds() {
        return underlyingPolicyIds;
    }

    /**
     * Parses the unique set of scopes that are defined for all subject in this UMA policy.
     *
     * @return The set of defined scopes on the UMA policy.
     */
    public synchronized Set<String> getScopes() {
        if (scopes == null) {
            scopes = convertFromUmaPolicy().asMap().keySet();
        }
        return scopes;
    }

    /**
     * Converts the {@code UmaPolicy} into its underlying backend policies in JSON format.
     *
     * @return The set of underlying backend policies that represent this UMA policy.
     */
    public Set<JsonValue> asUnderlyingPolicies() {
        Set<JsonValue> underlyingPolicies = new HashSet<JsonValue>();
        for (JsonValue p : convertFromUmaPolicy()) {
            underlyingPolicies.add(createPolicyJson(p));
        }
        return underlyingPolicies;
    }

    private JsonValue convertFromUmaPolicy() {
        JsonValue policies = json(object());
        for (JsonValue permission : policy.get(PERMISSIONS_KEY)) {
            for (JsonValue scope : permission.get(SCOPES_KEY)) {
                if (!policies.isDefined(scope.asString())) {
                    policies.add(scope.asString(), array());
                }
                policies.get(scope.asString()).add(permission.get(SUBJECT_KEY).asString());
            }
        }
        return policies;
    }

    private JsonValue createPolicyJson(JsonValue aggregatePolicy) {
        String policyName = resourceSet.getId() + " - " + aggregatePolicy.getPointer().get(0);
        List<Object> subjects = new ArrayList<Object>();
        for (String subject : aggregatePolicy.asList(String.class)) {
            subjects.add(object(
                    field(BACKEND_POLICY_SUBJECT_TYPE_KEY, BACKEND_POLICY_SUBJECT_TYPE_JWT_CLAIM),
                    field(BACKEND_POLICY_SUBJECT_CLAIM_NAME_KEY, BACKEND_POLICY_SUBJECT_CLAIM_NAME),
                    field(BACKEND_POLICY_SUBJECT_CLAIM_VALUE_KEY, subject)));
        }
        return json(object(
                field(BACKEND_POLICY_NAME_KEY, policyName),
                field(BACKEND_POLICY_RESOURCE_TYPE_KEY, "76656a38-5f8e-401b-83aa-4ccb74ce88d2"), //TODO this value will change once we have an application per resource server
                field(BACKEND_POLICY_RESOURCES_KEY, array(UMA_POLICY_SCHEME + getId())),
                field(BACKEND_POLICY_ACTION_VALUES_KEY, object(
                                field(aggregatePolicy.getPointer().get(0), true))
                ),
                field(BACKEND_POLICY_SUBJECT_KEY, object(
                                field(BACKEND_POLICY_SUBJECT_TYPE_KEY, BACKEND_POLICY_SUBJECT_TYPE_OR),
                                field(BACKEND_POLICY_SUBJECTS_KEY, subjects))
                )
        ));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UmaPolicy)) {
            return false;
        }
        UmaPolicy policy1 = (UmaPolicy) o;
        return policy.asMap().equals(policy1.policy.asMap())
                && resourceSet.equals(policy1.resourceSet);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = resourceSet.hashCode();
        result = 31 * result + policy.hashCode();
        return result;
    }

    /**
     * Parses the unique set of subjects that have permissions defined in this UMA policy.
     *
     * @return The set of defined subjects on the UMA policy.
     */
    public synchronized Set<String> getSubjects() {
        if (subjects == null) {
            subjects = convertSubjectsFromUmaPolicy();
        }
        return subjects;
    }

    private Set<String> convertSubjectsFromUmaPolicy() {
        Set<String> subjects = new HashSet<String>();
        for (JsonValue permission : policy.get(PERMISSIONS_KEY)) {
            subjects.add(permission.get(SUBJECT_KEY).asString());
        }
        return subjects;
    }

    /**
     * Gets the Resource Server Id that the resource set was registered by.
     *
     * @return The Resource Server Id.
     */
    public String getResourceServerId() {
        return resourceSet.getClientId();
    }

    public ResourceSetDescription getResourceSet() {
        return resourceSet;
    }
}
