/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SubRealmObserver.java,v 1.3 2010/01/20 17:01:36 veiming Exp $
 */

package com.sun.identity.entitlement.opensso;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.ApplicationManager;
import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.ReferralPrivilege;
import com.sun.identity.entitlement.ReferralPrivilegeManager;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.setup.SetupListener;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import java.security.AccessController;
import java.util.Iterator;
import java.util.Set;
import javax.security.auth.Subject;

/**
 * This observer will remove all referral and application privileges
 * that have reference to a delete sub realm.
 * 
 */
public class SubRealmObserver implements ServiceListener, SetupListener {
    private static Subject adminSubject = SubjectUtils.createSuperAdminSubject();

    public void addListener() {
        registerListener();
    }

    private static void registerListener() {
        SSOToken adminToken =
            (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());

        EntitlementConfiguration ec = EntitlementConfiguration.getInstance(
            SubjectUtils.createSubject(adminToken), "/");
        if (ec.migratedToEntitlementService()) {
            try {
                ServiceConfigManager scm = new ServiceConfigManager(
                    IdConstants.REPO_SERVICE, adminToken);
                scm.addListener(new SubRealmObserver());
            } catch (SMSException e) {
                PrivilegeManager.debug.error(
                    "SubRealmObserver.registerListener", e);
            } catch (SSOException e) {
                PrivilegeManager.debug.error(
                    "SubRealmObserver.registerListener", e);
            }
        }
    }
    
    public void schemaChanged(String serviceName, String version) {
        // do nothing
    }

    public void globalConfigChanged(String serviceName, String version,
        String groupName, String serviceComponent, int type) {
        // do nothing
    }

    public void organizationConfigChanged(
        String serviceName,
        String version,
        String orgName,
        String groupName,
        String serviceComponent,
        int type
    ) {
        if (type == ServiceListener.REMOVED) {
            ApplicationManager.clearCache(DNMapper.orgNameToRealmName(orgName));
            try {
                OpenSSOApplicationPrivilegeManager.removeAllPrivileges(orgName);
            } catch (EntitlementException ex) {
                PrivilegeManager.debug.error(
                    "SubRealmObserver.organizationConfigChanged: " +
                    "Unable to remove application  privileges", ex);
            }

            String deletedRealm = DNMapper.orgNameToRealmName(orgName);
            try {
                EntitlementService es = new EntitlementService(deletedRealm);
                Set<String> parentAndPeerRealms =
                    es.getParentAndPeerRealmNames();

                if ((parentAndPeerRealms != null) &&
                    !parentAndPeerRealms.isEmpty()) {
                    for (String r : parentAndPeerRealms) {
                        removeReferrals(r, deletedRealm);
                    }
                }

            } catch (EntitlementException ex) {
                PrivilegeManager.debug.error(
                    "SubRealmObserver.organizationConfigChanged: " +
                    "Unable to remove referral privileges", ex);
            }
        } else if (type == ServiceListener.MODIFIED) {
            ApplicationManager.clearCache(DNMapper.orgNameToRealmName(orgName));
        }
    }

    private void removeReferrals(String realm, String deletedRealm) 
        throws EntitlementException {
        Set<String> referralNames = DataStore.getReferralNames(realm,
            deletedRealm);
        if ((referralNames != null) && !referralNames.isEmpty()) {
            ReferralPrivilegeManager rfm = new ReferralPrivilegeManager(
                realm, adminSubject);

            for (String name : referralNames) {
                ReferralPrivilege referral = rfm.getReferral(name);
                Set<String> realms = referral.getRealms();

                for (Iterator<String> i = realms.iterator(); i.hasNext(); ) {
                    String r = i.next();
                    if (r.equalsIgnoreCase(deletedRealm)) {
                        i.remove();
                    }
                }

                if (realms.isEmpty()) {
                    rfm.delete(name);
                } else {
                    referral.setRealms(realms);
                    rfm.modify(referral);
                }
            }
        }
    }

}
