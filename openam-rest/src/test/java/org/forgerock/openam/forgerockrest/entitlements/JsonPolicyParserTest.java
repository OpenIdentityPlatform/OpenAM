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
 * Copyright 2014 ForgeRock, AS.
 */

package org.forgerock.openam.forgerockrest.entitlements;

import com.sun.identity.entitlement.AndCondition;
import com.sun.identity.entitlement.AndSubject;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.NotCondition;
import com.sun.identity.entitlement.OrCondition;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.ResourceAttribute;
import com.sun.identity.entitlement.StaticAttributes;
import com.sun.identity.entitlement.UserAttributes;
import com.sun.identity.entitlement.opensso.OpenSSOPrivilege;
import com.sun.identity.entitlement.opensso.PolicyCondition;
import com.sun.identity.policy.plugins.AuthenticateToRealmCondition;
import com.sun.identity.shared.DateUtils;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.entitlement.conditions.environment.OAuth2ScopeCondition;
import org.forgerock.openam.entitlement.conditions.subject.AuthenticatedUsers;
import org.forgerock.openam.utils.CollectionUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;
import static org.forgerock.json.fluent.JsonValue.*;

public class JsonPolicyParserTest {
    private static final String POLICY_NAME = "aPolicy";

    private JsonPolicyParser parser;

    @BeforeMethod
    public void setupTestObjects() {
        parser = new JsonPolicyParser();
    }

    @BeforeClass
    public void mockPrivilegeClass() {
        System.setProperty(Privilege.PRIVILEGE_CLASS_PROPERTY, StubPrivilege.class.getName());
    }

    @AfterClass
    public void unmockPrivilegeClass() {
        System.setProperty(Privilege.PRIVILEGE_CLASS_PROPERTY, OpenSSOPrivilege.class.getName());
    }

    @Test(expectedExceptions = EntitlementException.class)
    public void shouldRejectNullPolicyNames() throws Exception {
        // Given
        JsonValue content = json(object());

        // When
        parser.parsePolicy(null, content);

        // Then - exception thrown
    }

    @Test(expectedExceptions = EntitlementException.class)
    public void shouldRejectEmptyPolicyNames() throws Exception {
        // Given
        String name = " ";
        JsonValue content = buildJson(null);
        content.put("applicationName", name);

        // When
        parser.parsePolicy(name, content);

        // Then - exception thrown
    }

    @Test
    public void shouldUseJsonNameFirst() throws Exception {
        // Given
        String name = "realName";
        JsonValue content = buildJson(field("name", name));

        // When
        Privilege result = parser.parsePolicy("resourceName", content);

        // Then
        assertThat(result.getName()).isEqualTo(name);
    }

    @Test
    public void shouldUsePassedNameIfJsonNameIsMissing() throws Exception {
        // Given
        String name = "resourceName";
        JsonValue content = buildJson(null);

        // When
        Privilege result = parser.parsePolicy(name, content);

        // Then
        assertThat(result.getName()).isEqualTo(name);
    }

    @Test
    public void shouldParsePolicyActiveFlag() throws Exception {
        // Given
        JsonValue content = buildJson(field("active", true));

        // When
        Privilege result = parser.parsePolicy(POLICY_NAME, content);

        // Then
        assertThat(result.isActive()).isTrue();
    }

    @Test
    public void shouldNotAllowSettingCreationDate() throws Exception {
        // Given
        JsonValue content = buildJson(field("creationDate", "2014-01-01T00:00:00.000Z"));

        // When
        Privilege result = parser.parsePolicy(POLICY_NAME, content);

        // Then
        assertThat(result.getCreationDate()).isEqualTo(0); // 0 = not set
    }

    @Test
    public void shouldNotAllowSettingCreatedBy() throws Exception {
        // Given
        JsonValue content = buildJson(field("createdBy", "Bobby Tables"));

        // When
        Privilege result = parser.parsePolicy(POLICY_NAME, content);

        // Then
        assertThat(result.getCreatedBy()).isNull();
    }

    @Test
    public void shouldNotAllowSettingLastModifiedDate() throws Exception {
        // Given
        JsonValue content = buildJson(field("lastModifiedDate", "2014-01-01T00:00:00.000Z"));

        // When
        Privilege result = parser.parsePolicy(POLICY_NAME, content);

        // Then
        assertThat(result.getLastModifiedDate()).isEqualTo(0);
    }

    @Test
    public void shouldNotAllowSettingLastModifiedBy() throws Exception {
        // Given
        JsonValue content = buildJson(field("lastModifiedBy", "Little Bobby"));

        // When
        Privilege result = parser.parsePolicy(POLICY_NAME, content);

        // Then
        assertThat(result.getLastModifiedBy()).isNull();
    }

    @Test
    public void shouldParseDescription() throws Exception {
        // Given
        String description = "A test description";
        JsonValue content = buildJson(field("description", description));

        // When
        Privilege result = parser.parsePolicy(POLICY_NAME, content);

        // Then
        assertThat(result.getDescription()).isEqualTo(description);
    }

    @Test
    public void shouldParseResources() throws Exception {
        // Given
        List<String> included = Arrays.asList("one", "two", "three");
        JsonValue content = json(object(field("resources", included)));

        // When
        Privilege result = parser.parsePolicy(POLICY_NAME, content);

        // Then
        assertThat(result.getEntitlement().getResourceNames()).containsOnly(included.toArray());
    }

    @Test
    public void shouldParseApplicationNameAlongsideResource() throws Exception {
        // Given
        String applicationName = "a test application";
        JsonValue content = buildJson(null);
        content.put("applicationName", applicationName);

        // When
        Privilege result = parser.parsePolicy(POLICY_NAME, content);

        // Then
        assertThat(result.getEntitlement().getApplicationName()).isEqualTo(applicationName);
    }

    @Test
    public void shouldParseActionValues() throws Exception {
        // Given
        Map<String, Boolean> actionValues = new HashMap<String, Boolean>();
        actionValues.put("one", true);
        actionValues.put("two", false);
        JsonValue content = buildJson(null);

        content.put("actionValues", actionValues);

        // When
        Privilege result = parser.parsePolicy(POLICY_NAME, content);

        // Then
        assertThat(result.getEntitlement().getActionValues()).isEqualTo(actionValues);
    }

    @Test
    public void shouldIgnoreAdvice() throws Exception {
        // Given
        Map<String, List<String>> advice = Collections.singletonMap("test", Arrays.asList("one", "two"));
        JsonValue content = buildJson(field("advice", advice));

        // When
        Privilege result = parser.parsePolicy(POLICY_NAME, content);

        // Then
        assertThat(result.getEntitlement().getAdvices()).isNullOrEmpty();
    }

    @Test
    public void shouldIgnoreEntitlementAttributes() throws Exception {
        // Given
        Map<String, List<String>> attributes = Collections.singletonMap("test", Arrays.asList("one", "two"));
        JsonValue content = buildJson(field("attributes", attributes));

        // When
        Privilege result = parser.parsePolicy(POLICY_NAME, content);

        // Then
        assertThat(result.getEntitlement().getAttributes()).isNullOrEmpty();
    }

    @Test
    public void shouldIgnoreTTLForPolicies() throws Exception {
        // Given
        long ttl = 1234l;
        JsonValue content = buildJson(field("ttl", ttl));

        // When
        Privilege result = parser.parsePolicy(POLICY_NAME, content);

        // Then
        assertThat(result.getEntitlement().getTTL()).isNotSameAs(ttl);
    }

    @Test
    public void shouldCorrectlyParseConditionTypes() throws Exception {
        // Given
        String scope = "givenName";
        JsonValue content = buildJson(field("condition",
                object(field("type", "OAuth2Scope"),
                       field("requiredScopes", array(scope)))));

        // When
        Privilege result = parser.parsePolicy(POLICY_NAME, content);

        // Then
        assertThat(result.getCondition()).isInstanceOf(OAuth2ScopeCondition.class);
        assertThat(((OAuth2ScopeCondition) result.getCondition()).getRequiredScopes())
                .isEqualTo(Collections.singleton(scope));
    }

    @Test(expectedExceptions = EntitlementException.class)
    public void shouldErrorForUnknownConditionProperties() throws Exception {
        // Given
        String startIp = "127.0.0.1";
        String endIp = "127.0.0.255";
        JsonValue content = json(object(field("condition",
                object(field("type", "IP"),
                        field("startIp", startIp),
                        field("endIp", endIp),
                        field("notARealField", "no")))));

        // When
        parser.parsePolicy(POLICY_NAME, content);

        // Then - exception
    }

    @Test
    public void shouldParseNestedAndConditions() throws Exception {
        // Given
        // An AND condition containing a single OAuth2Scope condition
        String scope = "givenName";
        JsonValue content = buildJson(field("condition",
                object(field("type", "AND"),
                       field("conditions",
                               Collections.singletonList(object(field("type", "OAuth2Scope"),
                                       field("requiredScopes", array(scope))))))));

        // When
        Privilege result = parser.parsePolicy(POLICY_NAME, content);

        // Then
        assertThat(result.getCondition()).isInstanceOf(AndCondition.class);
        AndCondition and = (AndCondition) result.getCondition();
        assertThat(and.getEConditions()).hasSize(1);
        assertThat(and.getEConditions().iterator().next()).isInstanceOf(OAuth2ScopeCondition.class);
        OAuth2ScopeCondition oauth2Scope = (OAuth2ScopeCondition) and.getEConditions().iterator().next();
        assertThat(oauth2Scope.getRequiredScopes()).isEqualTo(Collections.singleton(scope));
    }

    @Test
    public void shouldParseNestedOrConditions() throws Exception {
        // Given
        // An OR condition containing a single OAuth2Scope condition
        String scope = "givenName";
        JsonValue content = buildJson(field("condition",
                object(field("type", "OR"),
                        field("conditions",
                                Collections.singletonList(object(field("type", "OAuth2Scope"),
                                        field("requiredScopes", array(scope))))))));

        // When
        Privilege result = parser.parsePolicy(POLICY_NAME, content);

        // Then
        assertThat(result.getCondition()).isInstanceOf(OrCondition.class);
        OrCondition or = (OrCondition) result.getCondition();
        assertThat(or.getEConditions()).hasSize(1);
        assertThat(or.getEConditions().iterator().next()).isInstanceOf(OAuth2ScopeCondition.class);
        OAuth2ScopeCondition oauth2Scope = (OAuth2ScopeCondition) or.getEConditions().iterator().next();
        assertThat(oauth2Scope.getRequiredScopes()).isEqualTo(Collections.singleton(scope));
    }

    @Test
    public void shouldParseNotConditions() throws Exception {
        // Given
        // A NOT condition containing an OAuth2Scope condition
        String scope = "givenName";
        JsonValue content = buildJson(field("condition",
                object(field("type", "NOT"),
                        field("condition", object(field("type", "OAuth2Scope"),
                                field("requiredScopes", array(scope)))))));

        // When
        Privilege result = parser.parsePolicy(POLICY_NAME, content);

        // Then
        assertThat(result.getCondition()).isInstanceOf(NotCondition.class);
        NotCondition not = (NotCondition) result.getCondition();
        assertThat(not.getECondition()).isInstanceOf(OAuth2ScopeCondition.class);
        OAuth2ScopeCondition ip = (OAuth2ScopeCondition) not.getECondition();
        assertThat(ip.getRequiredScopes()).isEqualTo(Collections.singleton(scope));
    }

    @Test
    public void shouldAllowLegacyPolicyConditions() throws Exception {
        // Given
        List<String> realm = Arrays.asList("REALM");

        JsonValue content = buildJson(field("condition",
                object(field("type", "Policy"),
                        field("className", AuthenticateToRealmCondition.class.getName()),
                        field("properties", object(field("AuthenticateToRealm", realm))))));

        // When
        Privilege result = parser.parsePolicy(POLICY_NAME, content);

        // Then
        assertThat(result.getCondition()).isInstanceOf(PolicyCondition.class);
        PolicyCondition condition = (PolicyCondition) result.getCondition();
        assertThat(condition.getClassName()).isEqualTo(AuthenticateToRealmCondition.class.getName());
        assertThat(condition.getProperties()).isEqualTo(Collections.singletonMap("AuthenticateToRealm",
                                                                                    new HashSet<String>(realm)));
    }

    @Test
    public void shouldParseSimpleSubjects() throws Exception {
        // Given
        JsonValue content = buildJson(field("subject", object(field("type", "AuthenticatedUsers"))));

        // When
        Privilege result = parser.parsePolicy(POLICY_NAME, content);

        // Then
        assertThat(result.getSubject()).isInstanceOf(AuthenticatedUsers.class);
    }

    @Test
    public void shouldParseComplexSubjects() throws Exception {
        // Given
        JsonValue content = buildJson(field("subject",
                object(field("type", "AND"),
                       field("subjects",
                               Arrays.asList(object(field("type", "AuthenticatedUsers")))))));

        // When
        Privilege result = parser.parsePolicy(POLICY_NAME, content);

        // Then
        assertThat(result.getSubject()).isInstanceOf(AndSubject.class);
        AndSubject and = (AndSubject) result.getSubject();
        assertThat(and.getESubjects()).hasSize(1);
        assertThat(and.getESubjects().iterator().next()).isInstanceOf(AuthenticatedUsers.class);
    }

    @Test
    public void shouldParseResourceAttributes() throws Exception {
        // Given
        List<String> values = Arrays.asList("one", "two", "three");
        JsonValue content = buildJson(field("resourceAttributes",
                Arrays.asList(object(field("type", "Static"),
                                     field("propertyName", "test"),
                                     field("propertyValues", values)))));

        // Given
        Privilege result = parser.parsePolicy(POLICY_NAME, content);

        // Then
        assertThat(result.getResourceAttributes()).hasSize(1);
        ResourceAttribute attr = result.getResourceAttributes().iterator().next();
        assertThat(attr).isInstanceOf(StaticAttributes.class);
        assertThat(attr.getPropertyName()).isEqualTo("test");
        assertThat(attr.getPropertyValues()).containsOnly(values.toArray());
    }

    @Test
    public void shouldPrintPolicyName() throws Exception {
        // Given
        Privilege policy = new StubPrivilege();
        String name = "test name";
        policy.setName(name);

        // When
        JsonValue result = parser.printPolicy(policy);

        // Then
        assertThat(result.get("name").asString()).isEqualTo(name);
    }

    @Test
    public void shouldPrintActiveFlag() throws Exception {
        // Given
        Privilege policy = new StubPrivilege();
        boolean active = true;
        policy.setActive(active);

        // When
        JsonValue result = parser.printPolicy(policy);

        // Then
        assertThat(result.get("active").asBoolean()).isEqualTo(active);
    }

    @Test
    public void shouldPrintCreationDateInIsoFormatButWithMilliseconds() throws Exception {
        // Given
        Privilege policy = new StubPrivilege();
        Date createdDate = new Date(123456789l);
        policy.setCreationDate(createdDate.getTime());

        // When
        JsonValue result = parser.printPolicy(policy);

        // Then
        assertThat(result.get("creationDate").asString())
                .isEqualTo(DateUtils.toUTCDateFormatWithMilliseconds(createdDate));
    }

    @Test
    public void shouldPrintCreatedBy() throws Exception {
        // Given
        Privilege policy = new StubPrivilege();
        String createdBy = "test user";
        policy.setCreatedBy(createdBy);

        // When
        JsonValue result = parser.printPolicy(policy);

        // Then
        assertThat(result.get("createdBy").asString()).isEqualTo(createdBy);
    }

    @Test
    public void shouldPrintLastModifiedDateInIsoFormat() throws Exception {
        // Given
        Privilege policy = new StubPrivilege();
        Date lastModified = new Date(123456789l);
        policy.setLastModifiedDate(lastModified.getTime());

        // When
        JsonValue result = parser.printPolicy(policy);

        // Then
        assertThat(result.get("lastModifiedDate").asString())
                .isEqualTo(DateUtils.toUTCDateFormatWithMilliseconds(lastModified));
    }

    @Test
    public void shouldPrintLastModifiedBy() throws Exception {
        // Given
        Privilege policy = new StubPrivilege();
        String lastModifiedBy = "test user";
        policy.setLastModifiedBy(lastModifiedBy);

        // When
        JsonValue result = parser.printPolicy(policy);

        // Then
        assertThat(result.get("lastModifiedBy").asString()).isEqualTo(lastModifiedBy);
    }

    @Test
    public void shouldPrintDescription() throws Exception {
        // Given
        Privilege policy = new StubPrivilege();
        String description = "a test description";
        policy.setDescription(description);

        // When
        JsonValue result = parser.printPolicy(policy);

        // Then
        assertThat(result.get("description").asString()).isEqualTo(description);
    }

    @Test
    public void shouldPrintPolicyResourceSet() throws Exception {
        // Given
        Privilege policy = new StubPrivilege();
        Set<String> included = CollectionUtils.asSet("one", "two", "three");
        Entitlement resources = new Entitlement();
        resources.setResourceNames(included);
        policy.setEntitlement(resources);

        // When
        JsonValue result = parser.printPolicy(policy);

        // Then
        assertThat(result.get("resources").asList()).containsOnly(included.toArray());
    }

    @Test
    public void shouldPrintPolicyApplicationName() throws Exception {
        // Given
        Privilege policy = new StubPrivilege();
        policy.setEntitlement(new Entitlement());
        String applicationName = "testApp";
        policy.getEntitlement().setApplicationName(applicationName);

        // When
        JsonValue result = parser.printPolicy(policy);

        // Then
        assertThat(result.get("applicationName").asString()).isEqualTo(applicationName);
    }

    @Test
    public void shouldPrintPolicyActionValues() throws Exception {
        // Given
        Privilege policy = new StubPrivilege();
        policy.setEntitlement(new Entitlement());
        Map<String, Boolean> actionValues = new HashMap<String, Boolean>();
        actionValues.put("one", true);
        actionValues.put("two", false);
        policy.getEntitlement().setActionValues(actionValues);

        // When
        JsonValue result = parser.printPolicy(policy);

        // Then
        assertThat(result.get("actionValues").asMap(Boolean.class)).isEqualTo(actionValues);
    }

    @Test
    public void shouldNotPrintPolicyAdvice() throws Exception {
        // Given
        Privilege policy = new StubPrivilege();
        policy.setEntitlement(new Entitlement());
        policy.getEntitlement().setAdvices(Collections.singletonMap("one", CollectionUtils.asSet("two")));

        // When
        JsonValue result = parser.printPolicy(policy);

        // Then
        // "Advice" should not appear on the policy entitlement
        assertThat(result.get("advice").asMapOfList(String.class)).isNullOrEmpty();
    }

    @Test
    public void shouldNotPrintPolicyAttributes() throws Exception {
        // Given
        Privilege policy = new StubPrivilege();
        policy.setEntitlement(new Entitlement());
        policy.getEntitlement().setAttributes(Collections.singletonMap("one", CollectionUtils.asSet("two")));

        // When
        JsonValue result = parser.printPolicy(policy);

        // Then
        // Attributes should not appear on the policy entitlement
        assertThat(result.get("attributes").asMapOfList(String.class)).isNullOrEmpty();
    }

    @Test
    public void shouldNotPrintPolicyTTL() throws Exception {
        // Given
        Privilege policy = new StubPrivilege();
        policy.setEntitlement(new Entitlement());
        policy.getEntitlement().setTTL(1234l);

        // When
        JsonValue result = parser.printPolicy(policy);

        // Then
        // TTL should not appear on the policy entitlement
        assertThat(result.get("ttl").asLong()).isNull();
    }

    @Test
    public void shouldPrintComplexConditions() throws Exception {
        // Given
        Privilege policy = new StubPrivilege();
        AndCondition and = new AndCondition();
        Set<EntitlementCondition> subConditions = new LinkedHashSet<EntitlementCondition>();

        Map<String, Set<String>> props = new HashMap<String, Set<String>>();
        props.put("AuthenticateToRealm", Collections.singleton("REALM"));
        PolicyCondition policyCondition = new PolicyCondition("test", AuthenticateToRealmCondition.class.getName(), props);

        NotCondition not = new NotCondition(policyCondition);
        subConditions.add(not);

        and.setEConditions(subConditions);
        policy.setCondition(and);

        // When
        JsonValue result = parser.printPolicy(policy);

        // Then
        assertThat(result.get(new JsonPointer("condition/type")).asString()).isEqualTo("AND");
        assertThat(result.get(new JsonPointer("condition/conditions/0/type")).asString()).isEqualTo("NOT");
        assertThat(result.get(new JsonPointer("condition/conditions/0/condition/type")).asString()).isEqualTo("Policy");
        assertThat(result.get(new JsonPointer("condition/conditions/0/condition/className")).asString())
                .isEqualTo(AuthenticateToRealmCondition.class.getName());
        assertThat(result.get(new JsonPointer("condition/conditions/0/condition/properties")).asMapOfList(String.class))
                .includes(entry("AuthenticateToRealm", Arrays.asList("REALM")));
    }

    @Test
    public void shouldPrintSimpleSubjects() throws Exception {
        // Given
        Privilege policy = new StubPrivilege();
        policy.setSubject(new AuthenticatedUsers());

        // When
        JsonValue result = parser.printPolicy(policy);

        // Then
        assertThat(result.get(new JsonPointer("subject/type")).asString()).isEqualTo("AuthenticatedUsers");
    }

    @Test
    public void shouldPrintResourceAttributes() throws Exception {
        // Given
        Privilege policy = new StubPrivilege();
        ResourceAttribute userAttrs = new UserAttributes();
        String userAttrName = "testUserAttribute";
        userAttrs.setPropertyName(userAttrName);
        StaticAttributes staticAttributes = new StaticAttributes();
        String staticAttrName = "testStaticAttribute";
        staticAttributes.setPropertyName(staticAttrName);
        Set<String> staticAttrValue = CollectionUtils.asSet("one", "two", "three");
        staticAttributes.setPropertyValues(staticAttrValue);
        policy.setResourceAttributes(new LinkedHashSet<ResourceAttribute>(Arrays.asList(userAttrs, staticAttributes)));

        // When
        JsonValue result = parser.printPolicy(policy);

        // Then
        assertThat(result.get("resourceAttributes").asList()).hasSize(2);
        assertThat(result.get(new JsonPointer("resourceAttributes/0/type")).asString()).isEqualTo("User");
        assertThat(result.get(new JsonPointer("resourceAttributes/0/propertyName")).asString()).isEqualTo(userAttrName);
        assertThat(result.get(new JsonPointer("resourceAttributes/1/type")).asString()).isEqualTo("Static");
        assertThat(result.get(new JsonPointer("resourceAttributes/1/propertyName")).asString())
                .isEqualTo(staticAttrName);
        assertThat(result.get(new JsonPointer("resourceAttributes/1/propertyValues")).asList(String.class))
                .containsOnly(staticAttrValue.toArray());
    }

    private JsonValue buildJson(Map.Entry<String, Object> fieldValue) {
        return json(object(fieldValue,
                field("applicationName", "iPlanetAMWebAgentService"),
                field("resources", array("http://www.arbitrary.com/resource")),
                field("actionValues", object(field("GET", "true"))))
        );
    }
}
