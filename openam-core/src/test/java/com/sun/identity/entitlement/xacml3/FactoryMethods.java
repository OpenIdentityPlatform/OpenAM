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
 * Copyright 2014 ForgeRock AS.
 */

package com.sun.identity.entitlement.xacml3;

import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.ReferralPrivilege;
import com.sun.identity.entitlement.ResourceAttribute;
import com.sun.identity.entitlement.xacml3.core.Policy;
import com.sun.identity.entitlement.xacml3.core.PolicySet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import javax.xml.bind.JAXBElement;
import org.forgerock.openam.entitlement.conditions.environment.SessionCondition;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import org.json.JSONException;

/**
 * Collection of factory methods for unit tests.
 */
public final class FactoryMethods {

    public static final String VERSION_TIMESTAMP_FORMAT = "yyyy.MM.dd.HH.mm.ss.SSS";
    public static final int NUMBER_OF_PRIVILEGES_IN_ARBITRARY_PRIVILEGE_SET = 2;

    public static Set<Privilege> createArbitraryPrivilegeSet(long now) throws EntitlementException {
        Set<Privilege> privilegeSet = new HashSet<Privilege>();

        for (int privilegeNumber = 1; privilegeNumber <= NUMBER_OF_PRIVILEGES_IN_ARBITRARY_PRIVILEGE_SET;
             privilegeNumber++) {
            Privilege privilege = createArbitraryPrivilege("Privilege" + privilegeNumber, now);
            privilegeSet.add(privilege);
        }

        return privilegeSet;
    }

    public static Privilege createArbitraryPrivilege(String name, long now) throws EntitlementException {

        Privilege privilege = Privilege.getNewInstance();
        privilege.setName(name);
        privilege.setDescription("Privilege " + name);
        privilege.setCreatedBy("creatingAuthor");
        privilege.setLastModifiedBy("modifyingAuthor");
        privilege.setCreationDate(now);
        privilege.setLastModifiedDate(now);
        privilege.setActive(true);

        privilege.setApplicationIndexes(asSet("arbitraryApplicationIndex"));

        EntitlementCondition entitlementCondition = new SessionCondition();
        entitlementCondition.setState("{ 'maxSessionTime': 10, 'terminateSession': true }");
        privilege.setCondition(entitlementCondition);

        // TODO: Define entitlement Subject

        Set<ResourceAttribute> resourceAttributes = new HashSet<ResourceAttribute>();
        // TODO: Define some ResourceAttributes
        privilege.setResourceAttributes(resourceAttributes);

        Entitlement entitlement = new Entitlement();
        entitlement.setName("arbitraryEntitlementName");
        entitlement.setResourceName("http://www.artibrary.com/resource");
        Map<String, Boolean> actionValues = new HashMap<String, Boolean>();
        actionValues.put("arbitraryAction", true);
        entitlement.setActionValues(actionValues);
        privilege.setEntitlement(entitlement);

        return privilege;
    }

    public static ReferralPrivilege createArbitraryReferralPrivilege(String name, long now) throws EntitlementException {

        Map<String, Set<String>> appNameToResources = new HashMap<String, Set<String>>();
        appNameToResources.put("arbitraryApplicationName", asSet("arbitraryResource1", "arbitraryResource2"));

        ReferralPrivilege referralPrivilege = new ReferralPrivilege(name, appNameToResources, asSet("arbitraryRealm"));
        referralPrivilege.setDescription("ReferralPrivilege " + name);
        referralPrivilege.setCreatedBy("creatingAuthor");
        referralPrivilege.setLastModifiedBy("modifyingAuthor");
        referralPrivilege.setCreationDate(now);
        referralPrivilege.setLastModifiedDate(now);
        referralPrivilege.setActive(true);
        return referralPrivilege;
    }

    public static Policy getArbitraryReferralPrivilegeAsPolicy(long now) throws JSONException, EntitlementException {
        ReferralPrivilege referralPrivilege = createArbitraryReferralPrivilege("ReferralPrivilege1", now);
        return XACMLPrivilegeUtils.referralToPolicy(referralPrivilege);
    }

    public static Policy getArbitraryPrivilegeAsPolicy(long now) throws EntitlementException {
        Set<Privilege> privileges = createArbitraryPrivilegeSet(now);
        PolicySet policySet = XACMLPrivilegeUtils.privilegesToPolicySet("/", privileges);
        return (Policy) policySet.getPolicySetOrPolicyOrPolicySetIdReference().get(0).getValue();
    }

    public static List<Policy> getPoliciesFromPolicySet(PolicySet policySet) {
        List<JAXBElement<?>> policySetOrPolicyOrPolicySetIdReference =
                policySet.getPolicySetOrPolicyOrPolicySetIdReference();

        List<Policy> policies = new ArrayList<Policy>();
        for (JAXBElement element : policySetOrPolicyOrPolicySetIdReference) {
            policies.add((Policy) element.getValue());
        }
        return policies;
    }

    public static String formatMillisecondsAsTimestamp(long creationDate) {
        SimpleDateFormat sdf = new SimpleDateFormat(VERSION_TIMESTAMP_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(creationDate);
    }
}
