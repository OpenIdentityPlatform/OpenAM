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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package com.sun.identity.entitlement.xacml3;

import static com.sun.identity.entitlement.xacml3.XACMLPrivilegeUtils.*;
import static org.forgerock.openam.xacml.v3.XACMLApplicationUtils.getApplicationNameFromPolicy;
import static org.forgerock.openam.xacml.v3.XACMLApplicationUtils.policyToApplication;
import static org.forgerock.openam.xacml.v3.XACMLResourceTypeUtils.createResourceType;
import static org.forgerock.openam.xacml.v3.XACMLResourceTypeUtils.generateResourceTypeDummyUuid;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.forgerock.openam.entitlement.ResourceType;
import org.json.JSONException;

import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.ReferralPrivilege;
import com.sun.identity.entitlement.xacml3.core.Policy;
import com.sun.identity.entitlement.xacml3.core.PolicySet;

/**
 * Facade for reading and writing XACML and translating between XACML PolicySets and AM Privilege types.
 */
public class XACMLReaderWriter {

    public static final int JSON_PARSE_ERROR = EntitlementException.JSON_PARSE_ERROR;
    public static final int INVALID_XML = EntitlementException.INVALID_XML;

    /**
     * Reads a sequence of XACML policies as OpenAM Privileges.
     *
     * @param xacml Non null stream to read.
     * @return The XACML policies translated to OpenAM privileges.
     * @throws EntitlementException If there was any unexpected error.
     */
    public PrivilegeSet read(InputStream xacml) throws EntitlementException {

        PolicySet policySet;
        try {
            policySet = XACMLPrivilegeUtils.streamToPolicySet(xacml);

        } catch (JAXBException e) {
            throw new EntitlementException(INVALID_XML, e);
        }
        return fromXACML(policySet);
    }

    /**
     * Translate provided XACML PolicySet into OpenAM Privileges, ReferralPrivileges, Applications and ResourceTypes.
     * XACML export file doesn't map Application and Resource Type completely and hence dummy ResourceType Ids
     * are assigned to ResourceTypes created and same is used for linking Application, Privilege to the ResourceType.
     * <p />
     *
     * From a policySet instance: <br />
     * <li> An application is created for every unique application name found in the Policy instances.</li>
     * <li> One ResourceType instance (with dummy uuid) per Policy is created.</li>
     * <li> One instance of Privilege per Policy instance is created. </li>
     *
     * @param policySet The set of policies to translate
     * @return OpenAM Privileges, ReferralPrivileges, Applications and ResourceTypes.
     * @throws EntitlementException If there was any unexpected error.
     */
    public PrivilegeSet fromXACML(PolicySet policySet) throws EntitlementException {
        PrivilegeSet privilegeSet = new PrivilegeSet();
        try {

            if (policySet == null) {
                return privilegeSet;
            }

            Map<String, Application> applicationMap = new HashMap<>();

            for (Policy policy : getPoliciesFromPolicySet(policySet)) {

                if (isReferralPolicy(policy)) {
                    privilegeSet.addReferralPrivilege(policyToReferral(policy));
                    continue;
                }

                String applicationName = getApplicationNameFromPolicy(policy);

                Application application = applicationMap.get(applicationName);
                if (application == null) {
                    application = policyToApplication(policy);
                    applicationMap.put(applicationName, application);
                    privilegeSet.addApplication(application);
                }

                // Create one resourceType instance (with a dummy uuid) per policy read from the XACML file.
                // Later these instances with dummy ids will be replaced by either an existing instance
                //  in the data store or by a new instance created during Resource Type Import Step generation.
                ResourceType resourceType = createResourceType(applicationName, null,
                        getResourceNamesFromPolicy(policy),
                        getActionValuesFromPolicy(policy),
                        generateResourceTypeDummyUuid());
                privilegeSet.addResourceType(resourceType);

                Privilege privilege = policyToPrivilege(policy);
                privilegeSet.addPrivilege(privilege);

                application.addResourceTypeUuid(resourceType.getUUID());
                privilege.setResourceTypeUuid(resourceType.getUUID());
            }

            return privilegeSet;

        } catch (JSONException e) {
            throw new EntitlementException(JSON_PARSE_ERROR, e);
        }
    }

    /**
     * Translate provided OpenAM Privilege and ReferralPrivilege objects into XACML PolicySet.
     *
     * @param realm The realm to which the provided privileges belong.,
     * @param privilegeSet The Privileges and ReferralPrivileges to translate.
     * @return XACML PolicySet
     * @throws EntitlementException If there was any unexpected error.
     */
    public PolicySet toXACML(String realm, PrivilegeSet privilegeSet) throws EntitlementException {

        PolicySet policySet = XACMLPrivilegeUtils.privilegesToPolicySet(realm, privilegeSet.getPrivileges());
        for (ReferralPrivilege referralPrivilege : privilegeSet.getReferralPrivileges()) {
            try {
                Policy policy = XACMLPrivilegeUtils.referralToPolicy(referralPrivilege);
                XACMLPrivilegeUtils.addPolicyToPolicySet(policy, policySet);
            } catch (JSONException e) {
                throw new EntitlementException(JSON_PARSE_ERROR, e);
            } catch (JAXBException e) {
                throw new EntitlementException(JSON_PARSE_ERROR, e);
            }
        }
        return policySet;
    }

}
