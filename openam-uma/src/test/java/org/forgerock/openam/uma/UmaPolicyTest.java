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
import static org.forgerock.json.fluent.JsonValue.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.Resource;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UmaPolicyTest {

    private ResourceSetDescription resourceSet;

    @BeforeMethod
    public void setup() {
        resourceSet = new ResourceSetDescription("RESOURCE_SET_UID", "RESOURCE_SET_ID", "CLIENT_ID",
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

    private Set<Resource> createUnderlyingPolicies() {
        Set<Resource> policies = new HashSet<Resource>();
        policies.add(new Resource("ID_1", "REVISION_1", createUnderlyingScopeAPolicyJson()));
        policies.add(new Resource("ID_2", "REVISION_2", createUnderlyingScopeBPolicyJson()));
        return policies;
    }

    private JsonValue createUnderlyingScopeAPolicyJson() {
        return createUnderlyingScopeAPolicyJson("POLICY_ID");
    }

    private JsonValue createUnderlyingScopeAPolicyJson(String id) {
        return json(object(
                field("name", "NAME - " + id + "-" + "SCOPE_A".hashCode()),
                field("applicationName", "client_id"),
                field("resourceTypeUuid", "RESOURCE_SET_UID"),
                field("resources", array("uma://POLICY_ID")),
                field("actionValues", object(field("SCOPE_A", true))),
                field("subject", object(
                        field("type", "OR"),
                        field("subjects", array(
                                object(
                                        field("type", "JwtClaim"),
                                        field("claimName", "sub"),
                                        field("claimValue", "SUBJECT_ONE")
                                ), object(
                                        field("type", "JwtClaim"),
                                        field("claimName", "sub"),
                                        field("claimValue", "SUBJECT_TWO")
                                )))
                ))
        ));
    }

    private JsonValue createUnderlyingScopeBPolicyJson() {
        return createUnderlyingScopeBPolicyJson("POLICY_ID");
    }

    private JsonValue createUnderlyingScopeBPolicyJson(String id) {
        return json(object(
                field("name", "NAME - " + id + "-" + "SCOPE_B".hashCode()),
                field("applicationName", "client_id"),
                field("resourceTypeUuid", "RESOURCE_SET_UID"),
                field("resources", array("uma://POLICY_ID")),
                field("actionValues", object(field("SCOPE_B", true))),
                field("subject", object(
                                field("type", "OR"),
                                field("subjects", array(
                                                object(
                                                        field("type", "JwtClaim"),
                                                        field("claimName", "sub"),
                                                        field("claimValue", "SUBJECT_ONE")
                                                )))
                ))
        ));
    }

    @Test
    public void shouldGetIdOfPolicy() {

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
        Set<Resource> policies = createUnderlyingPolicies();

        //When
        UmaPolicy umaPolicy = UmaPolicy.fromUnderlyingPolicies(resourceSet, policies);

        //Then
        assertThat(umaPolicy.getId()).isEqualTo("RESOURCE_SET_UID");
        assertThat(umaPolicy.getRevision()).isNotNull();
        assertThat(umaPolicy.asJson().asMap()).hasSize(3)
                .contains(entry("policyId", "RESOURCE_SET_UID"), entry("name", "NAME"));
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
        Set<JsonValue> underlyingPolicies = umaPolicy.asUnderlyingPolicies();

        //Then
        boolean foundScopeAPolicy = false;
        boolean foundScopeBPolicy = false;
        for (JsonValue policy : underlyingPolicies) {
            if (policy.contains("NAME - RESOURCE_SET_UID-" + "SCOPE_A".hashCode())) {
                assertThat(policy.asMap()).isEqualTo(createUnderlyingScopeAPolicyJson("RESOURCE_SET_UID").asMap());
                foundScopeAPolicy = true;
            } else if (policy.contains("NAME - RESOURCE_SET_UID-"+ "SCOPE_B".hashCode())) {
                assertThat(policy.asMap()).isEqualTo(createUnderlyingScopeBPolicyJson("RESOURCE_SET_UID").asMap());
                foundScopeBPolicy = true;
            }
        }
        assertThat(foundScopeAPolicy).isTrue();
        assertThat(foundScopeBPolicy).isTrue();
    }
}
