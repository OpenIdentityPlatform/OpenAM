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
 * Copyright 2014 Nomura Research Institute, Ltd.
 */

/*
 * Portions Copyrighted 2014 ForgeRock AS.
 */

package com.sun.identity.entitlement.xacml3;

import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.IPCondition;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.ReferralPrivilege;
import com.sun.identity.entitlement.ResourceAttribute;
import com.sun.identity.entitlement.xacml3.core.Match;
import com.sun.identity.entitlement.xacml3.core.Policy;
import com.sun.identity.entitlement.xacml3.core.PolicySet;
import org.json.JSONException;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.*;

/**
 * Unit tests for {@link XACMLPrivilegeUtils}. These are limited scope to only cover the functionality used
 * within com.sun.identity.cli.entitlement.ListXACML#handleRequest(com.sun.identity.cli.RequestContext) and
 * com.sun.identity.cli.entitlement.CreateXACML#handleRequest(com.sun.identity.cli.RequestContext). They will
 * indicate when changes to the code directly used by these methods has broken the existing functionality.
 *
 * Note that the custom JSON-in-XACML is not catered for here.
 *
 * @since 12.0.0
 */
public class XACMLPrivilegeUtilsTest {

    public static final String VERSION_TIMESTAMP_FORMAT = "yyyy.MM.dd.HH.mm.ss.SSS";
    long now = Calendar.getInstance().getTimeInMillis();
    public static final int NUMBER_OF_PRIVILEGES_IN_ARBITRARY_PRIVILEGE_SET = 2;

    @Test
    public void shouldReturnEmptyListWhenNull() {
        // When...
        List<Match> allMatchesFromTarget = XACMLPrivilegeUtils.getAllMatchesFromTarget(null);
        // Then...
        assertThat(allMatchesFromTarget).isNotNull().isEmpty();
    }

    @Test
    public void shouldResultInEquivalentPrivilegesWhenValidPrivilegeSetSerializedToXACMLThenDeserialized()
            throws EntitlementException {
        //Given
        Set<Privilege> privileges = createArbitraryPrivilegeSet();
        PolicySet policySet = XACMLPrivilegeUtils.privilegesToPolicySet("/", privileges);
        List<Policy> policies = getPoliciesFromPolicySet(policySet);
        List<Privilege> deserializedPrivileges = new ArrayList<Privilege>();
        for (Policy policy : policies) {
            //When
            deserializedPrivileges.add(XACMLPrivilegeUtils.policyToPrivilege(policy));
        }
        //Then
        assertDeserializedPrivilegesMatchOriginalPrivileges(deserializedPrivileges, privileges);
    }

    @Test
    public void shouldResultInEquivalentReferralPrivilegeWhenValidReferralPrivilegeSerializedToXACMLThenDeserialized()
            throws JSONException, EntitlementException {
        //Given
        ReferralPrivilege referralPrivilege = createArbitraryReferralPrivilege();
        Policy policy = null;
        policy = XACMLPrivilegeUtils.referralToPolicy(referralPrivilege);
        ReferralPrivilege deserializedReferralPrivilege = null;
        //When
        deserializedReferralPrivilege = XACMLPrivilegeUtils.policyToReferral(policy);
        //Then
        assertDeserializedReferralPrivilegeMatchesOriginalReferralPrivilege(deserializedReferralPrivilege,
                referralPrivilege);
    }

    @Test
    public void shouldReturnXACMLPolicySetWhenGivenValidPrivilegeSet() {
        //Given
        Set<Privilege> privileges = createArbitraryPrivilegeSet();
        //When
        PolicySet policySet = XACMLPrivilegeUtils.privilegesToPolicySet("/", privileges);
        //Then
        assertPolicySetContentsMatchPrivilegesContent(policySet, privileges);
    }

    @Test
    public void shouldReturnNullWhenGivenNullAsPrivilegeSet() {
        //Given
        Set<Privilege> privileges = null;
        //When
        PolicySet policySet = XACMLPrivilegeUtils.privilegesToPolicySet("/", privileges);
        //Then
        assertNull(policySet, "Expected PolicySet to be null.");
    }

    @Test
    public void shouldReturnXACMLPolicyWhenGivenValidReferralPrivilegeSet() throws JSONException, EntitlementException {
        //Given
        ReferralPrivilege referralPrivilege = createArbitraryReferralPrivilege();
        Policy policy = null;
        //When
        policy = XACMLPrivilegeUtils.referralToPolicy(referralPrivilege);
        //Then
        assertPolicyContentMatchesReferralPrivilegeContent(policy, referralPrivilege);
    }

    @Test
    public void shouldReturnNullWhenGivenNullAsReferralPrivilege() throws JSONException {
        //Given
        ReferralPrivilege referralPrivilege = null;
        Policy policy = null;
        //When
        policy = XACMLPrivilegeUtils.referralToPolicy(referralPrivilege);
        //Then
        assertNull(policy, "Expected Policy to be null.");
    }

    @Test
    public void shouldAddPolicyToPolicySet() throws JAXBException {
        //Given
        Policy policy = new Policy();
        PolicySet policySet = new PolicySet();
        //When
        XACMLPrivilegeUtils.addPolicyToPolicySet(policy, policySet);
        //Then
        assertPolicySetContainsSameSinglePolicy(policySet, policy);
    }

    @Test
    public void shouldNotAddNullToPolicySetAndSoShouldLeavePolicySetUnchanged() throws JAXBException {
        //Given
        Policy policy = null;
        PolicySet policySet = new PolicySet();
        //When
        XACMLPrivilegeUtils.addPolicyToPolicySet(policy, policySet);
        //Then
        assertEquals(policySet.getPolicySetOrPolicyOrPolicySetIdReference().size(), 0, "Expected PolicySet to remain " +
                "empty as a result of not adding null to the empty PolicySet.");
    }

    @Test
    public void shouldIndicateReferralPolicyWhenGivenReferralPrivilege() throws EntitlementException, JSONException {
        //Given
        Policy policy = getArbitraryReferralPrivilegeAsPolicy();
        //When
        boolean answer = XACMLPrivilegeUtils.isReferralPolicy(policy);
        //Then
        assertTrue(answer, "Expected Policy to be reported as a ReferralPolicy");
    }

    @Test
    public void shouldIndicateNotAReferralPolicyWhenGivenPolicy() {
        //Given
        Policy policy = getArbitraryPrivilegeAsPolicy();
        //When
        boolean answer = XACMLPrivilegeUtils.isReferralPolicy(policy);
        //Then
        assertFalse(answer, "Expected Policy to be reported as not a ReferralPolicy");
    }

    private void assertPolicySetContentsMatchPrivilegesContent(PolicySet policySet, Set<Privilege> privileges) {
        if (privileges != null && !privileges.isEmpty()) {
            assertTrue(policySet != null, "Expected PolicySet to not be null.");
        }
        List<Policy> policies = getPoliciesFromPolicySet(policySet);

        assertEquals(policies.size(), privileges.size(), "Mismatch between number of Policy elements in PolicySet, " +
                "and number of original Privileges.");

        List<String> policyIdList = new ArrayList<String>();
        for (Policy policy : policies) {
            policyIdList.add(policy.getPolicyId());
        }
        List<String> privilegeIdList = new ArrayList<String>();
        for (Privilege privilege : privileges) {
            privilegeIdList.add(privilege.getName());
        }
        assertTrue(policyIdList.containsAll(privilegeIdList), "Not all Privilege names were included in the " +
                "PolicySet.");
        assertTrue(privilegeIdList.containsAll(policyIdList), "Extra names were added to the PolicySet which were " +
                "not in the list of Privilege names.");

        List<String> descriptionList = new ArrayList<String>();
        for (Policy policy : policies) {
            descriptionList.add(policy.getDescription());
        }
        for (Privilege privilege : privileges) {
            String description = privilege.getDescription();
            assertTrue(descriptionList.contains(description), "Privilege with description '" + description + "' not " +
                    "found in PolicySet.");
        }

        String privilegesVersion = formatMillisecondsAsTimestamp(now);
        for (Policy policy : policies) {
            assertEquals(policy.getVersion().getValue(), privilegesVersion, "Policy found with version not matching " +
                    "Privilege creation date.");
        }
    }

    private void assertPolicyContentMatchesReferralPrivilegeContent(Policy policy,
                                                                    ReferralPrivilege referralPrivilege) {
        if (referralPrivilege != null) {
            assertTrue(policy != null, "Expected Policy to not be null.");
        }
        assertEquals(policy.getPolicyId(), referralPrivilege.getName(), "Expected Policy ID to match " +
                "ReferralPrivilege name.");
        assertEquals(policy.getDescription(), referralPrivilege.getDescription(), "Expected Policy description to " +
                "match ReferralPrivilege description.");
        String referralPrivilegeCreationDate = formatMillisecondsAsTimestamp(referralPrivilege.getCreationDate());
        assertEquals(policy.getVersion().getValue(), referralPrivilegeCreationDate, "Expected Policy creation date " +
                "to match ReferralPrivilege creation date.");
    }

    private void assertPolicySetContainsSameSinglePolicy(PolicySet policySet, Policy policy) {
        List<Policy> policies = getPoliciesFromPolicySet(policySet);
        assertEquals(policies.size(), 1, "Expected one Policy to be added to the PolicySet.");

        assertSame(policies.get(0), policy, "Policy that was added to PolicySet does not match the original Policy " +
                "to be added.");
    }

    private void assertDeserializedPrivilegesMatchOriginalPrivileges(List<Privilege> deserializedPrivileges,
            Set<Privilege> originalPrivileges) {
        assertEquals(deserializedPrivileges.size(), originalPrivileges.size(), "Expected deserialized Privileges " +
                "List to contain the same number of Privileges as the original list.");

        List<Privilege> originalPrivilegesMatched = new ArrayList<Privilege>();
        Set<Privilege> deserializedPrivilegesMatched = new HashSet<Privilege>();
        for (Privilege deserializedPrivilege : deserializedPrivileges) {
            for (Privilege originalPrivilege : originalPrivileges) {
                if (originalPrivilege.getName().equals(deserializedPrivilege.getName())) {
                    originalPrivilegesMatched.add(originalPrivilege);
                    deserializedPrivilegesMatched.add(deserializedPrivilege);
                }
            }
        }
        originalPrivileges.removeAll(originalPrivilegesMatched);
        deserializedPrivileges.removeAll(deserializedPrivilegesMatched);
        assertEquals(originalPrivileges.size(), 0, "Original Privileges found which were not represented in the " +
                "list of deserialized Privileges.");
        assertEquals(deserializedPrivileges.size(), 0, "Deserialized Privileges contained Privileges which were not " +
                "present in the original set.");
    }

    private void assertDeserializedReferralPrivilegeMatchesOriginalReferralPrivilege(
            ReferralPrivilege deserializedReferralPrivilege, ReferralPrivilege originalReferralPrivilege) {
        assertEquals(deserializedReferralPrivilege.getName(), originalReferralPrivilege.getName(),
                "Deserialized ReferralPrivilege name does not match name of original ReferralPrivilege.");

        Map<String, Set<String>> deserializedMapApplNameToResources =
                deserializedReferralPrivilege.getMapApplNameToResources();
        Map<String, Set<String>> originalMapApplNameToResources = originalReferralPrivilege.getMapApplNameToResources();
        assertEquals(deserializedMapApplNameToResources.size(), originalMapApplNameToResources.size(),
                "Deserialized ReferralPrivilege map of application names to resources' size does not match original.");
        Set<String> deserializedMapApplNameToResourcesKeySet = deserializedMapApplNameToResources.keySet();
        Set<String> originalMapApplNameToResourcesKeySet = originalMapApplNameToResources.keySet();
        assertTrue(deserializedMapApplNameToResourcesKeySet.containsAll(originalMapApplNameToResourcesKeySet),
                "The application name entries in deserialized ReferralPrivilege map of application names to " +
                        "resources' do not match originals.");
        for (String key : deserializedMapApplNameToResourcesKeySet) {
            assertTrue(deserializedMapApplNameToResources.get(key).containsAll(
                    originalMapApplNameToResources.get(key)));
        }
        assertEquals(deserializedReferralPrivilege.getRealms().size(), originalReferralPrivilege.getRealms().size(),
                "Deserialized ReferralPrivilege list of realms' size does not match original.");
        assertTrue(originalReferralPrivilege.getRealms().containsAll(deserializedReferralPrivilege.getRealms()),
                "Realms were specified in the deserialized ReferralPrivilege which were not in the original " +
                        "ReferralPrivilege.");
        assertEquals(deserializedReferralPrivilege.getCreationDate(), originalReferralPrivilege.getCreationDate(),
                "Deserialized ReferralPrivilege creation date does not match original ReferralPrivilege creation " +
                        "date.");
        assertEquals(deserializedReferralPrivilege.getLastModifiedDate(),
                originalReferralPrivilege.getLastModifiedDate(), "Deserialized ReferralPrivilege last modified date " +
                        "does not match original ReferralPrivilege last modified date.");
        assertEquals(deserializedReferralPrivilege.getCreatedBy(), originalReferralPrivilege.getCreatedBy(),
                "Deserialized ReferralPrivilege created by field does not match original ReferralPrivilege created " +
                        "by field.");
    }

    private Set<Privilege> createArbitraryPrivilegeSet() {
        Set<Privilege> privilegeSet = new HashSet<Privilege>();

        for (int privilegeNumber = 1; privilegeNumber <= NUMBER_OF_PRIVILEGES_IN_ARBITRARY_PRIVILEGE_SET;
             privilegeNumber++) {
            Privilege privilege = createArbitraryPrivilege("Privilege" + privilegeNumber);
            privilegeSet.add(privilege);
        }

        return privilegeSet;
    }

    private Privilege createArbitraryPrivilege(String name) {
        Privilege privilege = null;
        try {
            privilege = Privilege.getNewInstance();
            privilege.setName(name);
            privilege.setDescription("Privilege " + name);
            privilege.setCreatedBy("creatingAuthor");
            privilege.setLastModifiedBy("modifyingAuthor");
            privilege.setCreationDate(now);
            privilege.setLastModifiedDate(now);
            privilege.setActive(true);

            Set<String> applicationIndexes = new HashSet<String>();
            applicationIndexes.add("arbitraryApplicationIndex");
            privilege.setApplicationIndexes(applicationIndexes);

            EntitlementCondition entitlementCondition = new IPCondition();
            privilege.setCondition(entitlementCondition);

            Set<ResourceAttribute> resourceAttributes = new HashSet<ResourceAttribute>();
            privilege.setResourceAttributes(resourceAttributes);

            Entitlement entitlement = new Entitlement();
            entitlement.setName("arbitraryEntitlementName");
            Map<String,Boolean> actionValues = new HashMap<String, Boolean>();
            actionValues.put("arbitraryAction", true);
            entitlement.setActionValues(actionValues);
            privilege.setEntitlement(entitlement);
        } catch (EntitlementException ee) {
            fail("Unable to create arbitrary Privilege Set.", ee);
        }
        return privilege;
    }

    private ReferralPrivilege createArbitraryReferralPrivilege() throws EntitlementException {
        ReferralPrivilege referralPrivilege = null;
        String name = "ReferralPrivilege1";
        HashSet<String> realms = new HashSet<String>();
        realms.add("arbitraryRealm");
        Map<String, Set<String>> appNameToResources = new HashMap<String, Set<String>>();
        Set<String> resources = new HashSet<String>();
        resources.add("arbitraryResource1");
        resources.add("arbitraryResource2");
        appNameToResources.put("arbitraryApplicationName", resources);
        referralPrivilege = new ReferralPrivilege(name, appNameToResources, realms);
        referralPrivilege.setDescription("ReferralPrivilege " + name);
        referralPrivilege.setCreatedBy("creatingAuthor");
        referralPrivilege.setLastModifiedBy("modifyingAuthor");
        referralPrivilege.setCreationDate(now);
        referralPrivilege.setLastModifiedDate(now);
        referralPrivilege.setActive(true);
        return referralPrivilege;
    }

    private Policy getArbitraryReferralPrivilegeAsPolicy() throws JSONException, EntitlementException {
        ReferralPrivilege referralPrivilege = createArbitraryReferralPrivilege();
        Policy policy = null;
        policy = XACMLPrivilegeUtils.referralToPolicy(referralPrivilege);
        return policy;
    }

    private Policy getArbitraryPrivilegeAsPolicy() {
        Set<Privilege> privileges = createArbitraryPrivilegeSet();
        PolicySet policySet = XACMLPrivilegeUtils.privilegesToPolicySet("/", privileges);
        Policy policy = (Policy) policySet.getPolicySetOrPolicyOrPolicySetIdReference().get(0).getValue();
        return policy;
    }

    private List<Policy> getPoliciesFromPolicySet(PolicySet policySet) {
        List<JAXBElement<?>> policySetOrPolicyOrPolicySetIdReference =
                policySet.getPolicySetOrPolicyOrPolicySetIdReference();

        List<Policy> policies = new ArrayList<Policy>();
        for (JAXBElement element : policySetOrPolicyOrPolicySetIdReference) {
            policies.add((Policy) element.getValue());
        }
        return policies;
    }

    private String formatMillisecondsAsTimestamp(long creationDate) {
        SimpleDateFormat sdf = new SimpleDateFormat(VERSION_TIMESTAMP_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(creationDate);
    }
}
