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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.fluent.JsonValue.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PolicySearchTest {

    private ResourceSetDescription resourceSet1;
    private ResourceSetDescription resourceSet2;

    @BeforeMethod
    public void setup() {
        resourceSet1 = new ResourceSetDescription("RESOURCE_SET_ID_1", "CLIENT_ID_1",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        resourceSet1.setDescription(json(object(field("name", "NAME_1"))));

        resourceSet2 = new ResourceSetDescription("RESOURCE_SET_ID_2", "CLIENT_ID_2",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        resourceSet2.setDescription(json(object(field("name", "NAME_2"))));
    }

    private JsonValue createUmaPolicyJson(String subjectOneName) {
        return json(object(
                field("policyId", "POLICY_ID"),
                field("permissions",  array(
                        object(
                                field("subject", subjectOneName),
                                field("scopes", array("SCOPE_A", "SCOPE_B"))),
                        object(
                                field("subject", "SUBJECT_TWO"),
                                field("scopes", array("SCOPE_A")))
                ))
        ));
    }

    @Test
    public void shouldSearchBasedOnSubject() throws Exception {

        //Given
        JsonValue policy1 = createUmaPolicyJson("SUBJECT_ONE");
        JsonValue policy2 = createUmaPolicyJson("SUBJECT_THREE");
        UmaPolicy umaPolicy1 = UmaPolicy.valueOf(resourceSet1, policy1);
        UmaPolicy umaPolicy2 = UmaPolicy.valueOf(resourceSet2, policy2);
        Set<UmaPolicy> policies = new HashSet<UmaPolicy>();
        policies.add(umaPolicy1);
        policies.add(umaPolicy2);

        //When
        PolicySearch search = new PolicySearch(policies).equals(new JsonPointer("/permissions/subject"), "SUBJECT_ONE");

        //Then
        assertThat(search.getPolicies()).hasSize(1).contains(umaPolicy1);
    }

    @Test
    public void shouldSearchBasedOnResourceServer() throws Exception {

        //Given
        JsonValue policy1 = createUmaPolicyJson("SUBJECT_ONE");
        JsonValue policy2 = createUmaPolicyJson("SUBJECT_THREE");
        UmaPolicy umaPolicy1 = UmaPolicy.valueOf(resourceSet1, policy1);
        UmaPolicy umaPolicy2 = UmaPolicy.valueOf(resourceSet2, policy2);
        Set<UmaPolicy> policies = new HashSet<UmaPolicy>();
        policies.add(umaPolicy1);
        policies.add(umaPolicy2);

        //When
        PolicySearch search = new PolicySearch(policies).equals(new JsonPointer("/resourceServer"), "CLIENT_ID_2");

        //Then
        assertThat(search.getPolicies()).hasSize(1).contains(umaPolicy2);
    }

    @Test
    public void shouldCombinePolicySearches() throws Exception {

        //Given
        JsonValue policy1 = createUmaPolicyJson("SUBJECT_ONE");
        JsonValue policy2 = createUmaPolicyJson("SUBJECT_THREE");
        UmaPolicy umaPolicy1 = UmaPolicy.valueOf(resourceSet1, policy1);
        UmaPolicy umaPolicy2 = UmaPolicy.valueOf(resourceSet2, policy2);
        Set<UmaPolicy> policies1 = new HashSet<UmaPolicy>();
        Set<UmaPolicy> policies2 = new HashSet<UmaPolicy>();
        policies1.add(umaPolicy1);
        policies2.add(umaPolicy2);

        PolicySearch policySearch1 = new PolicySearch(policies1);
        PolicySearch policySearch2 = new PolicySearch(policies2);

        //When
        PolicySearch search = policySearch1.combine(policySearch2);

        //Then
        assertThat(search.getPolicies()).hasSize(2).contains(umaPolicy1, umaPolicy2);
    }

    @Test
    public void shouldRemovePolicySearches() throws Exception {

        //Given
        JsonValue policy1 = createUmaPolicyJson("SUBJECT_ONE");
        JsonValue policy2 = createUmaPolicyJson("SUBJECT_THREE");
        UmaPolicy umaPolicy1 = UmaPolicy.valueOf(resourceSet1, policy1);
        UmaPolicy umaPolicy2 = UmaPolicy.valueOf(resourceSet2, policy2);
        Set<UmaPolicy> policies1 = new HashSet<UmaPolicy>();
        Set<UmaPolicy> policies2 = new HashSet<UmaPolicy>();
        policies1.add(umaPolicy1);
        policies1.add(umaPolicy2);
        policies2.add(umaPolicy2);

        PolicySearch policySearch1 = new PolicySearch(policies1);
        PolicySearch policySearch2 = new PolicySearch(policies2);

        //When
        PolicySearch search = policySearch1.remove(policySearch2);

        //Then
        assertThat(search.getPolicies()).hasSize(1).contains(umaPolicy1);
    }
}
