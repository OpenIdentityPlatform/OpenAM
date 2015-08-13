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
import static org.assertj.core.api.Assertions.entry;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.newResourceResponse;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UmaPolicyTest {

    private ResourceSetDescription resourceSet;

    @BeforeMethod
    public void setup() {
        resourceSet = new ResourceSetDescription("RESOURCE_SET_ID", "CLIENT_ID",
                "RESOURCE_OWNER_ID", Collections.<String, Object>emptyMap());
        resourceSet.setDescription(json(object(field("name", "NAME"))));
    }

    private JsonValue createUmaPolicyJson() {
        return json(object(
                field("policyId", "POLICY_ID"),
                field("permissions",  array(
                        object(
                                field("subject", "SUBJECT_ONE"),
                                field("scopes", array("SCOPE_A", "SCOPE_B"))),
                        object(
                                field("subject", "SUBJECT_TWO"),
                                field("scopes", array("SCOPE_A")))
                ))
        ));
    }

    private Set<ResourceResponse> createUnderlyingPolicies(String resourceOwnerId) {
        Set<ResourceResponse> policies = new HashSet<ResourceResponse>();
        policies.add(newResourceResponse("ID_1", "REVISION_1", createUnderlyingSubjectOnePolicyJson(resourceOwnerId)));
        policies.add(newResourceResponse("ID_2", "REVISION_2", createUnderlyingSubjectTwoPolicyJson(resourceOwnerId)));
        return policies;
    }

    private JsonValue createUnderlyingSubjectOnePolicyJson(String resourceOwnerId) {
        return createUnderlyingSubjectOnePolicyJson(resourceOwnerId, "POLICY_ID");
    }

    private JsonValue createUnderlyingSubjectOnePolicyJson(String resourceOwnerId, String id) {
        return json(object(
                field("name", "NAME - " + resourceOwnerId + " - " + id + "-" + "SUBJECT_ONE".hashCode()),
                field("applicationName", "client_id"),
                field("resourceTypeUuid", "RESOURCE_SET_ID"),
                field("resources", array("uma://POLICY_ID")),
                field("actionValues", object(
                        field("SCOPE_A", true),
                        field("SCOPE_B", true))),
                field("subject", object(
                        field("type", "JwtClaim"),
                        field("claimName", "sub"),
                        field("claimValue", "SUBJECT_ONE")
                ))
        ));
    }

    private JsonValue createUnderlyingSubjectTwoPolicyJson(String resourceOwnerId) {
        return createUnderlyingSubjectTwoPolicyJson(resourceOwnerId, "POLICY_ID");
    }

    private JsonValue createUnderlyingSubjectTwoPolicyJson(String resourceOwnerId, String id) {
        return json(object(
                field("name", "NAME - " + resourceOwnerId + " - " + id + "-" + "SUBJECT_TWO".hashCode()),
                field("applicationName", "client_id"),
                field("resourceTypeUuid", "RESOURCE_SET_ID"),
                field("resources", array("uma://POLICY_ID")),
                field("actionValues", object(
                        field("SCOPE_A", true))),
                field("subject", object(
                        field("type", "JwtClaim"),
                        field("claimName", "sub"),
                        field("claimValue", "SUBJECT_TWO")
                ))
        ));
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldFailForMissingIdOfPolicy() throws Exception {

        //Given
        JsonValue policy = json(object());

        //When
        String policyId = UmaPolicy.idOf(policy);

        //Then - exception
    }

    @Test
    public void shouldGetIdOfPolicy() throws Exception {

        //Given
        JsonValue policy = createUmaPolicyJson();

        //When
        String policyId = UmaPolicy.idOf(policy);

        //Then
        assertThat(policyId).isEqualTo("POLICY_ID");
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldValidateUmaPolicyHasPolicyId() throws BadRequestException {

        //Given
        JsonValue policy = json(object(
                field("permissions", array(
                        object(
                                field("subject", "SUBJECT_ONE"),
                                field("scopes", array("SCOPE_A", "SCOPE_B"))),
                        object(
                                field("subject", "SUBJECT_TWO"),
                                field("scopes", array("SCOPE_A")))
                ))
        ));

        //When
        try {
            UmaPolicy.valueOf(resourceSet, policy);
        } catch (BadRequestException e) {
            //Then
            assertThat(e.getMessage()).contains("Missing required attribute, 'policyId'");
            throw e;
        }
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldValidateUmaPolicyHasPolicyIdAsString() throws BadRequestException {

        //Given
        JsonValue policy = json(object(
                field("policyId", 1),
                field("permissions", array(
                        object(
                                field("subject", "SUBJECT_ONE"),
                                field("scopes", array("SCOPE_A", "SCOPE_B"))),
                        object(
                                field("subject", "SUBJECT_TWO"),
                                field("scopes", array("SCOPE_A")))
                ))
        ));

        //When
        try {
            UmaPolicy.valueOf(resourceSet, policy);
        } catch (BadRequestException e) {
            //Then
            assertThat(e.getMessage()).contains("Required attribute, 'policyId', must be a String");
            throw e;
        }
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldValidateUmaPolicyHasPermissions() throws BadRequestException {

        //Given
        JsonValue policy = json(object(
                field("policyId", "POLICY_ID")
        ));

        //When
        try {
            UmaPolicy.valueOf(resourceSet, policy);
        } catch (BadRequestException e) {
            //Then
            assertThat(e.getMessage()).contains("Missing required attribute, 'permissions'");
            throw e;
        }
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldValidateUmaPolicyHasPermissionsAsList() throws BadRequestException {

        //Given
        JsonValue policy = json(object(
                field("policyId", "POLICY_ID"),
                field("permissions", "PERMISSIONS")
        ));

        //When
        try {
            UmaPolicy.valueOf(resourceSet, policy);
        } catch (BadRequestException e) {
            //Then
            assertThat(e.getMessage()).contains("Required attribute, 'permissions', must be an array");
            throw e;
        }
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldValidateUmaPolicyPermissionHasSubject() throws BadRequestException {

        //Given
        JsonValue policy = json(object(
                field("policyId", "POLICY_ID"),
                field("permissions", array(
                        object(
                                field("subject", "SUBJECT_ONE"),
                                field("scopes", array("SCOPE_A", "SCOPE_B"))),
                        object(
                                field("scopes", array("SCOPE_A")))
                ))
        ));

        //When
        try {
            UmaPolicy.valueOf(resourceSet, policy);
        } catch (BadRequestException e) {
            //Then
            assertThat(e.getMessage()).contains("Missing required attribute, 'subject'");
            throw e;
        }
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldValidateUmaPolicyPermissionHasSubjectAsString() throws BadRequestException {

        //Given
        JsonValue policy = json(object(
                field("policyId", "POLICY_ID"),
                field("permissions", array(
                        object(
                                field("subject", "SUBJECT_ONE"),
                                field("scopes", array("SCOPE_A", "SCOPE_B"))),
                        object(
                                field("subject", 1),
                                field("scopes", array("SCOPE_A")))
                ))
        ));

        //When
        try {
            UmaPolicy.valueOf(resourceSet, policy);
        } catch (BadRequestException e) {
            //Then
            assertThat(e.getMessage()).contains("Required attribute, 'subject', must be a String");
            throw e;
        }
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldValidateUmaPolicyPermissionHasScopes() throws BadRequestException {

        //Given
        JsonValue policy = json(object(
                field("policyId", "POLICY_ID"),
                field("permissions", array(
                        object(
                                field("subject", "SUBJECT_ONE"),
                                field("scopes", array("SCOPE_A", "SCOPE_B"))),
                        object(
                                field("subject", "SUBJECT_TWO"))
                ))
        ));

        //When
        try {
            UmaPolicy.valueOf(resourceSet, policy);
        } catch (BadRequestException e) {
            //Then
            assertThat(e.getMessage()).contains("Missing required attribute, 'scopes'");
            throw e;
        }
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldValidateUmaPolicyPermissionHasScopesAsListOfStrings() throws BadRequestException {

        //Given
        JsonValue policy = json(object(
                field("policyId", "POLICY_ID"),
                field("permissions", array(
                        object(
                                field("subject", "SUBJECT_ONE"),
                                field("scopes", array("SCOPE_A", "SCOPE_B"))),
                        object(
                                field("subject", "SUBJECT_TWO"),
                                field("scopes", array(1)))
                ))
        ));

        //When
        try {
            UmaPolicy.valueOf(resourceSet, policy);
        } catch (BadRequestException e) {
            //Then
            assertThat(e.getMessage()).contains("Required attribute, 'scopes', must be an array of Strings");
            throw e;
        }
    }

    @Test
    public void shouldCreateUmaPolicy() throws BadRequestException {

        //Given
        JsonValue policy = createUmaPolicyJson();

        //When
        UmaPolicy umaPolicy = UmaPolicy.valueOf(resourceSet, policy);

        //Then
        assertThat(umaPolicy.getId()).isEqualTo("POLICY_ID");
        assertThat(umaPolicy.getRevision()).isNotNull();
        assertThat(umaPolicy.asJson().asMap()).isEqualTo(policy.asMap());
    }

    @Test
    public void shouldCreateUmaPolicyFromUnderlyingPolicies() throws BadRequestException {

        //Given
        Set<ResourceResponse> policies = createUnderlyingPolicies("RESOURCE_OWNER_ID");

        //When
        UmaPolicy umaPolicy = UmaPolicy.fromUnderlyingPolicies(resourceSet, policies);

        //Then
        assertThat(umaPolicy.getId()).isEqualTo("RESOURCE_SET_ID");
        assertThat(umaPolicy.getRevision()).isNotNull();
        assertThat(umaPolicy.asJson().asMap()).hasSize(3)
                .contains(entry("policyId", "RESOURCE_SET_ID"), entry("name", "NAME"));
        JsonValue permissions = umaPolicy.asJson().get("permissions");
        assertThat(permissions.asList()).hasSize(2);
        assertThat(permissions.get(0).asMap()).contains(entry("subject", "SUBJECT_ONE"));
        assertThat(permissions.get(0).get("scopes").asList()).containsOnly("SCOPE_A", "SCOPE_B");
        assertThat(permissions.get(1).asMap()).contains(entry("subject", "SUBJECT_TWO"));
        assertThat(permissions.get(1).get("scopes").asList()).containsOnly("SCOPE_A");
    }

    @Test
    public void shouldGetUmaPolicyScopes() throws BadRequestException {

        //Given
        UmaPolicy umaPolicy = UmaPolicy.valueOf(resourceSet, createUmaPolicyJson());

        //When
        Set<String> scopes = umaPolicy.getScopes();

        //Then
        assertThat(scopes).containsOnly("SCOPE_A", "SCOPE_B");
    }

    @Test
    public void shouldConvertUmaPolicyToUnderlyingPolicies() throws BadRequestException {

        //Given
        UmaPolicy umaPolicy = UmaPolicy.valueOf(resourceSet, createUmaPolicyJson());

        //When
        Set<JsonValue> underlyingPolicies = umaPolicy.asUnderlyingPolicies("RESOURCE_OWNER_ID");

        //Then
        boolean foundScopeAPolicy = false;
        boolean foundScopeBPolicy = false;
        for (JsonValue policy : underlyingPolicies) {
            if (policy.contains("NAME - RESOURCE_OWNER_ID - RESOURCE_SET_ID-" + "SUBJECT_ONE".hashCode())) {
                assertThat(policy.asMap()).isEqualTo(createUnderlyingSubjectOnePolicyJson("RESOURCE_OWNER_ID", "RESOURCE_SET_ID").asMap());
                foundScopeAPolicy = true;
            } else if (policy.contains("NAME - RESOURCE_OWNER_ID - RESOURCE_SET_ID-"+ "SUBJECT_TWO".hashCode())) {
                assertThat(policy.asMap()).isEqualTo(createUnderlyingSubjectTwoPolicyJson("RESOURCE_OWNER_ID", "RESOURCE_SET_ID").asMap());
                foundScopeBPolicy = true;
            }
        }
        assertThat(foundScopeAPolicy).isTrue();
        assertThat(foundScopeBPolicy).isTrue();
    }
}
