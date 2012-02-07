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
 * $Id: SubjectReferentialIntegrityPlugin.java,v 1.5 2009/01/28 05:35:01 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy.plugins;


import com.sun.identity.shared.debug.Debug;
import com.iplanet.am.sdk.AMCallBack;
import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMPostCallBackException;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.PolicyConfig;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.PolicyUtils;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.identity.sm.SMSEntry;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import com.sun.identity.shared.ldap.util.DN;

/**
 * The class <code>SubjectReferentialIntegrityPlugin</code> provides
 * the implementation to preserve referential integrity
 * between the subjects in policies to the corresponding objects in local
 * directory.
 */

public class SubjectReferentialIntegrityPlugin extends AMCallBack {

    // Debug
    static Debug debug = Debug.getInstance(PolicyManager.POLICY_DEBUG_NAME);

    /**
     * This implementation would visit all the subjects in policies
     * across all orgs/sub-orgs and remove the subject values
     * corresponding to the deleted entry DN. After removing an entry from a
     * subject, checks if that entry is the only one in the subject to
     * remove the subject as well.
     */
    public void postProcessDelete(SSOToken token, String entryDN,
        Map attributes, boolean softDeleteEnabled, int objectType)
        throws AMPostCallBackException 
    {
        try {
            if (debug.messageEnabled()) {
                debug.message("ReferentialIntegrityPlugin.postProcessDelete()");
            }

            // check the subject types
            Set objectTypes = new HashSet();
            objectTypes.add(new Integer(AMObject.USER));
            objectTypes.add(new Integer(AMObject.ROLE));
            objectTypes.add(new Integer(AMObject.ORGANIZATION));
            objectTypes.add(new Integer(AMObject.GROUP));
            objectTypes.add(new Integer(AMObject.ASSIGNABLE_DYNAMIC_GROUP));
            objectTypes.add(new Integer(AMObject.DYNAMIC_GROUP));
            objectTypes.add(new Integer(AMObject.FILTERED_ROLE));
            if(objectTypes.contains(new Integer(objectType))) {
                String subOrg,policyName,subjectName;
                Policy policy;
                Subject subject;
                Iterator policyIter,subjectIter;
                // create a DN for the entry to be deleted
                DN entryDName = new DN(entryDN);
                //a connection to the Identity Server data store.
                AMStoreConnection dpStore = new AMStoreConnection(token);
                DN rootDN = new DN(SMSEntry.getRootSuffix());
                if (debug.messageEnabled()) {
                    debug.message("Searching for all policies from root DN: " 
                    + rootDN.toString());
                }
                PolicyManager pm = new PolicyManager(token, rootDN.toString());
                String org = pm.getOrganizationName();
                /**
                 *  find out from org policy config that is the directory
                 *  specified is the local directory
                 */
                Map configParams = PolicyConfig.getPolicyConfig(org);
                String ldapServer = 
                    ((String)configParams.get(PolicyConfig.LDAP_SERVER)).
                    toLowerCase();
                boolean localDS = PolicyUtils.isLocalDS(ldapServer);
                /** 
                 * process IdentityServer Role irrespective of local or 
                 * non-local DS
                 */
                if(objectType == AMObject.ROLE) {
                    localDS = true;
                }
                if(localDS) {
                    AMOrganization rootOrg = 
                        (AMOrganization)dpStore.getOrganization(org);
                    Set subOrgs = null;
                    //all orgs/sub-orgs
                    subOrgs = rootOrg.searchSubOrganizations("*", 
                        AMConstants.SCOPE_SUB);
                    Iterator orgIter = subOrgs.iterator();
                    while (orgIter.hasNext()) {
                        subOrg = (String)orgIter.next();
                        if (debug.messageEnabled()) {
                            debug.message("Visiting suborg: " + subOrg);
                        }
                        PolicyManager pmSubOrg = 
                            new PolicyManager(token,subOrg);
                        // all policies
                        Set policies = pmSubOrg.getPolicyNames();
                        policyIter = policies.iterator();
                        while (policyIter.hasNext()) {
                            policyName = (String)policyIter.next();
                            if (debug.messageEnabled()) {
                                debug.message("policyName: " + policyName);
                            }
                            policy = pmSubOrg.getPolicy(policyName);
                            // referral policies don't have subjects defined
                            if (!policy.isReferralPolicy()) {
                                // all subjects
                                boolean replacePolicy = false;
                                Set subjectsInPolicy = policy.getSubjectNames();
                                Set subjects = new HashSet();
                                subjects.addAll(subjectsInPolicy);
                                subjectIter = subjects.iterator();
                                while (subjectIter.hasNext()) {
                                    subjectName = (String)subjectIter.next();
                                    if (debug.messageEnabled()) {
                                        debug.message("subjectName: " 
                                        + subjectName);
                                    }
                                    subject = policy.getSubject(subjectName);
                                    Set set = subject.getValues();
                                    Iterator ite = set.iterator();
                                    String str = null;
                                    DN strDN = null;
                                    while (ite.hasNext()) {
                                        str = (String)ite.next();
                                        strDN = new DN(str);
                                        if(entryDName.equals(strDN)) {
                                            replacePolicy = true;
                                            if (debug.messageEnabled()) {
                                                debug.message("DNs match, str:" 
                                                    + str + "entryDN:"+entryDN);
                                            }
                                            set.remove(str);
                                            if (set.isEmpty()) {
                                                policy.removeSubject(
                                                    subjectName);
                                                if (debug.messageEnabled()) {
                                                    debug.message(
                                                        "subjectDeleted:"
                                                        +subjectName);
                                                }
                                            } else {
                                                subject.setValues(set);
                                            }
                                            break;
                                        } // match DNs
                                    } // all subject values in the subject
                                } // all subjects in the policy
                                if(replacePolicy) {
                                    pmSubOrg.replacePolicy(policy);
                                }
                            } // for referral policies
                        } // all policies
                    } // all orgs
                } // localDS check
            } // objectType check
        } catch(PolicyException pe) {
            debug.error("ReferentialIntegrityPlugin.postProcessDelete():", pe);
        }
        catch(SSOException sse) {
            debug.error("ReferentialIntegrityPlugin.postProcessDelete():",sse);
        }
        catch(Exception e) {
            debug.error("ReferentialIntegrityPlugin.postProcessDelete():", e);
        }
    } // end of postProcess...
} // end of class
