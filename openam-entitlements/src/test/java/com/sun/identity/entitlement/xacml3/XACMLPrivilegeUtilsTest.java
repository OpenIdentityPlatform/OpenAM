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
 *
 * Portions Copyrighted 2014 ForgeRock AS.
 */

package com.sun.identity.entitlement.xacml3;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.ReferralPrivilege;
import com.sun.identity.entitlement.xacml3.core.Match;
import com.sun.identity.entitlement.xacml3.core.Policy;
import com.sun.identity.entitlement.xacml3.core.PolicySet;
import org.json.JSONException;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import static com.sun.identity.entitlement.xacml3.Assertions.*;
import static com.sun.identity.entitlement.xacml3.FactoryMethods.*;
import static org.fest.assertions.Assertions.assertThat;
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

    private long now = Calendar.getInstance().getTimeInMillis();

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
        Set<Privilege> privileges = createArbitraryPrivilegeSet(now);
        PolicySet policySet = XACMLPrivilegeUtils.privilegesToPolicySet("/", privileges);
        List<Policy> policies = getPoliciesFromPolicySet(policySet);
        List<Privilege> deserializedPrivileges = new ArrayList<Privilege>();
        for (Policy policy : policies) {
            //When
            deserializedPrivileges.add(XACMLPrivilegeUtils.policyToPrivilege(policy));
        }
        //Then
        assertAllPrivilegesEquivalent(deserializedPrivileges, privileges);
    }

    @Test
    public void shouldResultInEquivalentReferralPrivilegeWhenValidReferralPrivilegeSerializedToXACMLThenDeserialized()
            throws JSONException, EntitlementException {
        //Given
        ReferralPrivilege referralPrivilege = createArbitraryReferralPrivilege("ReferralPrivilege1", now);
        Policy policy = XACMLPrivilegeUtils.referralToPolicy(referralPrivilege);
        ReferralPrivilege deserializedReferralPrivilege = null;
        //When
        deserializedReferralPrivilege = XACMLPrivilegeUtils.policyToReferral(policy);
        //Then
        assertReferralPrivilegesEquivalent(deserializedReferralPrivilege, referralPrivilege);
    }

    @Test
    public void shouldReturnXACMLPolicySetWhenGivenValidPrivilegeSet() throws EntitlementException {
        //Given
        Set<Privilege> privileges = createArbitraryPrivilegeSet(now);
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
        ReferralPrivilege referralPrivilege = createArbitraryReferralPrivilege("ReferralPrivilege1", now);
        //When
        Policy policy = XACMLPrivilegeUtils.referralToPolicy(referralPrivilege);
        //Then
        assertPolicyContentMatchesReferralPrivilegeContent(policy, referralPrivilege);
    }

    @Test
    public void shouldReturnNullWhenGivenNullAsReferralPrivilege() throws JSONException {
        //Given
        ReferralPrivilege referralPrivilege = null;
        //When
        Policy policy = XACMLPrivilegeUtils.referralToPolicy(referralPrivilege);
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
        Policy policy = getArbitraryReferralPrivilegeAsPolicy(now);
        //When
        boolean answer = XACMLPrivilegeUtils.isReferralPolicy(policy);
        //Then
        assertTrue(answer, "Expected Policy to be reported as a ReferralPolicy");
    }

    @Test
    public void shouldIndicateNotAReferralPolicyWhenGivenPolicy() throws EntitlementException {
        //Given
        Policy policy = getArbitraryPrivilegeAsPolicy(now);
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

}
