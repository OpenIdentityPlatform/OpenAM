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

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.IPrivilege;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.ReferralPrivilege;
import com.sun.identity.entitlement.xacml3.core.Policy;
import com.sun.identity.entitlement.xacml3.core.PolicySet;
import org.json.JSONException;

import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
     * Translate provided XACML PolicySet into OpenAM Privilege and ReferralPrivilege objects.
     *
     * @param policySet The set of policies to translate
     * @return OpenAM Privileges and ReferralPrivileges
     * @throws EntitlementException If there was any unexpected error.
     */
    public PrivilegeSet fromXACML(PolicySet policySet) throws EntitlementException {
        PrivilegeSet privilegeSet = new PrivilegeSet();

        try {

            if (policySet == null) {
                return privilegeSet;
            }

            for (Policy policy : XACMLPrivilegeUtils.getPoliciesFromPolicySet(policySet)) {
                if (XACMLPrivilegeUtils.isReferralPolicy(policy)) {
                    privilegeSet.addReferralPrivilege(XACMLPrivilegeUtils.policyToReferral(policy));
                } else {
                    privilegeSet.addPrivilege(XACMLPrivilegeUtils.policyToPrivilege(policy));
                }
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
