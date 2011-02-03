/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: SubOrgReferral.java,v 1.3 2008/06/25 05:43:52 qcheng Exp $
 *
 */




package com.sun.identity.policy.plugins;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.ResBundleUtils;
import com.sun.identity.policy.ValidValues;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.shared.debug.Debug;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/** Class to facilitate policy referrals to sub
 *  organizations sub realms
 */
public class SubOrgReferral extends OrgReferral {

    private static Debug debug 
            = Debug.getInstance(PolicyManager.POLICY_DEBUG_NAME);

    private static final String SUB_ORG_REFERRAL = "SubOrgReferral";
    /** No argument constructor */
    public SubOrgReferral() {
    }

    /**Gets the name of the ReferralType 
     * @return name of the ReferralType representing this referral
     */
    public String getReferralTypeName() {
        return SUB_ORG_REFERRAL;
    }

    /**
     * Gets the valid values for this referral 
     * @param token SSOToken
     * @return <code>ValidValues</code> object
     * @exception SSOException if <code>SSOToken></code> is not valid
     * @exception PolicyException if unable to get the list of valid
     * names.
     */
    public ValidValues getValidValues(SSOToken token) 
            throws SSOException, PolicyException {
        return getValidValues(token, "*");
    }

    /**Gets the valid values for this referral 
     * matching a pattern
     * @param token SSOToken
     * @param pattern a pattern to match against the value
     * @return <code>ValidValues</code> object
     * @exception SSOException if <code>SSOToken></code> is not valid
     * @exception PolicyException if unable to get the list of valid
     * names.
     */
    public ValidValues getValidValues(SSOToken token, String pattern)
            throws SSOException, PolicyException {
        Set values = new HashSet();
        int status = ValidValues.SUCCESS;
        try {
            Set orgSet = (Set) _configurationMap.get(
                                        PolicyManager.ORGANIZATION_NAME);
            if ( (orgSet == null) || (orgSet.isEmpty()) ) {
                OrgReferral.DEBUG.error("SubOrgReferral.getValidValues(): "
                        + " Organization name not set");
                throw new PolicyException(ResBundleUtils.rbName,
                    "org_name_not_set", null, null);
            }
            Iterator iter = orgSet.iterator();
            String orgName = (String) iter.next();
            OrganizationConfigManager orgConfigManager 
                    = new OrganizationConfigManager(token,
                    orgName);
            Set subOrgNames = orgConfigManager.getSubOrganizationNames(pattern,
                    false); //get only first level children
            if ((subOrgNames != null) && !subOrgNames.isEmpty()) {
                Iterator subOrgsIter = subOrgNames.iterator();
                while (subOrgsIter.hasNext()) {
                    String subOrgName = (String)subOrgsIter.next();
                    OrganizationConfigManager subOrgManager =
                            orgConfigManager.getSubOrgConfigManager(subOrgName);
                    if (subOrgManager != null) {
                        values.add(subOrgManager.getOrganizationName());
                    }
                }
            }
            if (debug.messageEnabled()) {
                debug.message("SubOrgReferral.getValidValues():subOrgNames="
                        + subOrgNames);
            }
        } catch (SMSException smse) {
            OrgReferral.DEBUG.error("Can not get valid values for referral " 
                    + getReferralTypeName() 
                    + smse );
            String[] objs = {getReferralTypeName()};
            throw new PolicyException(ResBundleUtils.rbName,
                "can_not_get_values_for_referral", objs, smse);
        }
        return (new ValidValues(status, values));
    }

}
