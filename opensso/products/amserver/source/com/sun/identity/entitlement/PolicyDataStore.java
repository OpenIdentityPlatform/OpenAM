/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * If applicable, addReferral the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: PolicyDataStore.java,v 1.3 2010/01/07 00:19:11 veiming Exp $
 */

package com.sun.identity.entitlement;

import javax.security.auth.Subject;

/**
 * This class implements method to persist policy in datastore.
 */
public abstract class PolicyDataStore {
    private static PolicyDataStore instance;
    protected static final String POLICIES = "Policies";

    static {
        try {
            //RFE: configurable data store
            Class clazz = Class.forName(
                "com.sun.identity.entitlement.opensso.OpenSSOPolicyDataStore");
            instance = (PolicyDataStore)clazz.newInstance();
        } catch (InstantiationException e) {
            PrivilegeManager.debug.error("PolicyDataStore.<init>", e);
        } catch (IllegalAccessException e) {
            PrivilegeManager.debug.error("PolicyDataStore.<init>", e);
        } catch (ClassNotFoundException e) {
            PrivilegeManager.debug.error("PolicyDataStore.<init>", e);
        }
    }

    public static PolicyDataStore getInstance() {
        return instance;
    }

    /**
     * Adds policy.
     *
     * @param subject who is performing the operation.
     * @param realm Realm name.
     * @param privilege privilege object.
     */
    public abstract void addPolicy(
        Subject subject,
        String realm,
        Privilege privilege
    ) throws EntitlementException;

    /**
     * Returns policy object.
     *
     * @param adminSubject Administrator subject that has the credential to
     *        addReferral the policy.
     * @param realm Realm name.
     * @param name Policy name.
     */
    public abstract Object getPolicy(
        Subject adminSubject,
        String realm,
        String name
    ) throws EntitlementException;

    /**
     * Returns referral privilege object.
     *
     * @param adminSubject Administrator subject that has the credential to
     *        addReferral the policy.
     * @param realm Realm name.
     * @param name Policy name.
     */
    public abstract ReferralPrivilege getReferral(
        Subject adminSubject,
        String realm,
        String name
    ) throws EntitlementException;

    /**
     * Removes policy.
     *
     * @param subject Administrator subject that has the credential to
     *        remove privilege.
     * @param realm Realm name.
     * @param privilege Privilege to be deleted
     */
    public abstract void removePrivilege(
        Subject subject,
        String realm,
        Privilege privilege
    ) throws EntitlementException;

    /**
     * Adds a referral privilege.
     *
     * @param subject who is performing the operation.
     * @param realm Realm name.
     * @param referral Referral Privilege
     * @throws EntitlementException if referral privilege cannot be added
     */
    public abstract void addReferral(
        Subject subject,
        String realm,
        ReferralPrivilege referral
    ) throws EntitlementException;

    public abstract void removeReferral(
        Subject subject,
        String realm,
        ReferralPrivilege referral
    ) throws EntitlementException;
}
