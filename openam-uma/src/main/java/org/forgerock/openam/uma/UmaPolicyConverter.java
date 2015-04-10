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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.oauth2.resources.ResourceSetDescription;

import com.sun.identity.entitlement.EntitlementException;

/**
 * Converts UmaPolicy instances to backend policy JSON format.
 */
public class UmaPolicyConverter {

    private final UmaResourceTypeFactory resourceTypeFactory;

    @Inject
    public UmaPolicyConverter(UmaResourceTypeFactory resourceTypeFactory) {
        this.resourceTypeFactory = resourceTypeFactory;
    }

    /**
     * Converts the {@code UmaPolicy} into its underlying backend policies in JSON format.
     *
     * @return The set of underlying backend policies that represent this UMA policy.
     */
    public Set<JsonValue> asUnderlyingPolicies(UmaPolicy policy) throws EntitlementException {
        Set<JsonValue> underlyingPolicies = new HashSet<JsonValue>();
        for (JsonValue p : policy.convertFromUmaPolicy()) {
            underlyingPolicies.add(createPolicyJson(policy, p));
        }
        return underlyingPolicies;
    }

    private JsonValue createPolicyJson(UmaPolicy policy, JsonValue aggregatePolicy) throws EntitlementException {
        ResourceSetDescription resourceSet = policy.getResourceSet();
        String policyName = resourceSet.getName() + " - " + resourceSet.getId() + "-"
                + aggregatePolicy.getPointer().get(0).hashCode();
        List<Object> subjects = new ArrayList<Object>();
        for (String subject : aggregatePolicy.asList(String.class)) {
            subjects.add(object(
                    field(BACKEND_POLICY_SUBJECT_TYPE_KEY, BACKEND_POLICY_SUBJECT_TYPE_JWT_CLAIM),
                    field(BACKEND_POLICY_SUBJECT_CLAIM_NAME_KEY, BACKEND_POLICY_SUBJECT_CLAIM_NAME),
                    field(BACKEND_POLICY_SUBJECT_CLAIM_VALUE_KEY, subject)));
        }
        return json(object(
                field(BACKEND_POLICY_NAME_KEY, policyName),
                field("applicationName", policy.getResourceServerId().toLowerCase()), //Lowercase as ldap is case insensitive
                field(BACKEND_POLICY_RESOURCE_TYPE_KEY, resourceTypeFactory.getResourceTypeId(resourceSet.getRealm())),
                field(BACKEND_POLICY_RESOURCES_KEY, array(UMA_POLICY_SCHEME + policy.getId())),
                field(BACKEND_POLICY_ACTION_VALUES_KEY, object(
                                field(aggregatePolicy.getPointer().get(0), true))
                ),
                field(BACKEND_POLICY_SUBJECT_KEY, object(
                                field(BACKEND_POLICY_SUBJECT_TYPE_KEY, BACKEND_POLICY_SUBJECT_TYPE_OR),
                                field(BACKEND_POLICY_SUBJECTS_KEY, subjects))
                )
        ));
    }


}
