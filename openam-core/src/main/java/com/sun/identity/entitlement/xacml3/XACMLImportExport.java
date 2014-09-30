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

import com.google.inject.Inject;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.ReferralPrivilege;
import com.sun.identity.entitlement.ReferralPrivilegeManager;
import com.sun.identity.entitlement.util.SearchFilter;
import com.sun.identity.entitlement.xacml3.core.Policy;
import com.sun.identity.entitlement.xacml3.core.PolicySet;
import com.sun.identity.shared.debug.Debug;
import org.json.JSONException;

import javax.inject.Named;
import javax.security.auth.Subject;
import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility for handling the Import and Export of Policies into the OpenAM Entitlements
 * framework.
 */
public class XACMLImportExport {

    public static final int JSON_PARSE_ERROR = EntitlementException.JSON_PARSE_ERROR;
    public static final int INVALID_XML = EntitlementException.INVALID_XML;
    public static final String PREFIX = XACMLImportExport.class.getSimpleName();

    // Injected
    private final SearchFilterFactory searchFilterFactory;
    private final Debug debug;

    /**
     * Creates an instance of the XACMLImportExport with dependencies provided.
     *
     * @param searchFilterFactory Non null, required for SearchFilter operations.
     * @param debug Non null.
     */
    @Inject
    public XACMLImportExport(SearchFilterFactory searchFilterFactory, @Named(XACMLConstants.DEBUG) Debug debug) {
        this.searchFilterFactory = searchFilterFactory;
        this.debug = debug;
    }

    /**
     * Default constructor.
     *
     * Note: Typically the Guice constructor is preferred but as this code is
     * also called from a non-Guice context, both must be provided.
     */
    public XACMLImportExport() {
        this(new SearchFilterFactory(), PrivilegeManager.debug);
    }

    /**
     * Performs the Import based on the given Stream. The stream must contain XML in XACML.
     *
     * @param realm Non null Realm to populate with the Policies.
     * @param xacmlStream Non null stream to read.
     * @param admin Non null admin Subject.
     * @return True if import completed successfully, otherwise false.
     * @throws EntitlementException If there was any unexpected error.
     */
    public boolean importXacml(String realm, InputStream xacmlStream, Subject admin) throws EntitlementException {
        try {
            PolicySet ps = XACMLPrivilegeUtils.streamToPolicySet(xacmlStream);

            if (ps == null) {
                return false;
            }

            PrivilegeManager pm = PrivilegeManager.getInstance(realm, admin);
            ReferralPrivilegeManager rpm = new ReferralPrivilegeManager(realm, admin);

            Set<Policy> policySet = XACMLPrivilegeUtils.getPoliciesFromPolicySet(ps);
            message("Import: Policies to Import {0}", policySet.size());

            for (Policy policy : policySet) {
                if (XACMLPrivilegeUtils.isReferralPolicy(policy)) {

                    ReferralPrivilege referralPrivilege = XACMLPrivilegeUtils.policyToReferral(policy);
                    String name = referralPrivilege.getName();

                    if (rpm.canFindByName(name)) {
                        message("Import: Modify Referral {0}", name);
                        rpm.modify(referralPrivilege);
                    } else {
                        message("Import: Add Referral {0}", name);
                        rpm.add(referralPrivilege);
                    }

                } else {

                    Privilege privilege = XACMLPrivilegeUtils.policyToPrivilege(policy);
                    String name = privilege.getName();
                    if (pm.canFindByName(name)) {
                        message("Import: Modify Privilege {0}", name);
                        pm.modify(privilege);
                    } else {
                        message("Import: Add Privilege {0}", name);
                        pm.add(privilege);
                    }

                }
            }
            message("Import: Complete");
            return true;
        } catch (JSONException e) {
            throw new EntitlementException(JSON_PARSE_ERROR, e);
        } catch (JAXBException e) {
            throw new EntitlementException(INVALID_XML, e);
        }
    }

    /**
     * Performs an export of all Policies found in the Privilege Manager that match the
     * provided filters.
     *
     * @param realm Non null realm.
     * @param admin Non null admin subject to authenticate as.
     * @param filters Non null, but maybe empty filters to select Privileges against.
     * @return A non null but possibly empty collection of Policies.
     * @throws EntitlementException If there was any problem with the generation of Policies.
     */
    public PolicySet exportXACML(String realm, Subject admin, List<String> filters)
            throws EntitlementException {

        PrivilegeManager pm = PrivilegeManager.getInstance(realm, admin);
        ReferralPrivilegeManager rpm = new ReferralPrivilegeManager(realm, admin);

        Set<SearchFilter> filterSet = new HashSet<SearchFilter>();
        if (filters != null) {
            for (String filter : filters) {
                SearchFilter searchFilter = searchFilterFactory.getFilter(filter);
                message("Export: Search Filter: {0}", searchFilter);
                filterSet.add(searchFilter);
            }
        }

        Set<String> privilegeNames = pm.searchNames(filterSet);
        Set<String> referralNames = rpm.searchNames(filterSet);
        message("Export: Privilege Matches {0}", privilegeNames.size());
        message("Export: Referral Matches {0}", referralNames.size());

        Set<Privilege> privileges = new HashSet<Privilege>();
        for (String name : privilegeNames) {
            Privilege privilege = pm.findByName(name, admin);
            message("Export: Privilege {0}", privilege.getName());
            privileges.add(privilege);
        }

        PolicySet policySet = XACMLPrivilegeUtils.privilegesToPolicySet(realm, privileges);
        for (String name : referralNames) {
            ReferralPrivilege referralPrivilege = rpm.findByName(name);
            message("Export: Referral {0}", referralPrivilege.getName());
            try {
                Policy policy = XACMLPrivilegeUtils.referralToPolicy(referralPrivilege);
                XACMLPrivilegeUtils.addPolicyToPolicySet(policy, policySet);
            } catch (JSONException e) {
                throw new EntitlementException(JSON_PARSE_ERROR, e);
            } catch (JAXBException e) {
                throw new EntitlementException(JSON_PARSE_ERROR, e);
            }
        }

        message("Export: Complete");
        return policySet;
    }

    private void message(String format, Object... args) {
        if (debug.messageEnabled()) {
            debug.message(MessageFormat.format(PREFIX + format, args));
        }
    }
}
