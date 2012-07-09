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
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: PrivilegeIndexStore.java,v 1.4 2010/01/08 22:20:47 veiming Exp $
 */

package com.sun.identity.entitlement;

import com.sun.identity.entitlement.util.SearchFilter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Set;
import javax.security.auth.Subject;

/**
 * Privilege Index Store is responsible to storing privilege in
 * a persistent data store.
 */
public abstract class PrivilegeIndexStore {
    private static Class clazz;
    private Subject adminSubject;
    private String realm;
    

    static {
        try {
            //RFE: configurable
            clazz = Class.forName(
                "com.sun.identity.entitlement.opensso.OpenSSOIndexStore");
        } catch (ClassNotFoundException e) {
            PrivilegeManager.debug.error("PrivilegeIndexStore.static<init>", e);
        }
    }

    protected PrivilegeIndexStore(
        Subject adminSubject,
        String realm) {
        this.adminSubject = adminSubject;
        this.realm = realm;
    }

    protected Subject getAdminSubject() {
        return adminSubject;
    }

    protected String getRealm() {
        return realm;
    }

    /**
     * Returns an instance of the privilege index store.
     *
     * @param adminSubject Admin Subject who has the privilege to write to
     *        index datastore.
     * @param realm Realm Name.
     * @return an instance of the privilege index store.
     */
    public static PrivilegeIndexStore getInstance(
        Subject adminSubject,
        String realm) {
        if (clazz == null) {
            return null;
        }
        Class[] parameterTypes = {Subject.class, String.class};
            try {
                Constructor constructor = clazz.getConstructor(parameterTypes);
                Object[] args = {adminSubject, realm};
                return ((PrivilegeIndexStore) constructor.newInstance(args));
            } catch (InstantiationException ex) {
                PrivilegeManager.debug.error("PrivilegeIndexStore.getInstance",
                    ex);
            } catch (IllegalAccessException ex) {
                PrivilegeManager.debug.error("PrivilegeIndexStore.getInstance",
                    ex);
            } catch (IllegalArgumentException ex) {
                PrivilegeManager.debug.error("PrivilegeIndexStore.getInstance",
                    ex);
            } catch (InvocationTargetException ex) {
                PrivilegeManager.debug.error("PrivilegeIndexStore.getInstance",
                    ex);
            } catch (NoSuchMethodException ex) {
                PrivilegeManager.debug.error("PrivilegeIndexStore.getInstance",
                    ex);
            } catch (SecurityException ex) {
                PrivilegeManager.debug.error("PrivilegeIndexStore.getInstance",
                    ex);
            }
        return null;
    }

    /**
     * Adds a set of privileges to the data store. Proper indexes will be
     * created to speed up policy evaluation.
     *
     * @param privileges Privileges to be added.
     * @throws EntitlementException if addition failed.
     */
    public abstract void add(Set<IPrivilege> privileges)
        throws EntitlementException;

    /**
     * Deletes a set of privileges from data store.
     *
     * @param privilege Privileges to be deleted.
     * @throws com.sun.identity.entitlement.EntitlementException if deletion
     * failed.
     */
    public abstract void delete(Set<IPrivilege> privilege)
        throws EntitlementException;

    /**
     * Deletes a privilege from data store.
     *
     * @param privilegeName name of privilege to be deleted.
     * @throws com.sun.identity.entitlement.EntitlementException if deletion
     * failed.
     */
    public abstract void delete(String privilegeName)
        throws EntitlementException;

    /**
     * Deletes a referralprivilege from data store.
     *
     * @param privilegeName name of privilege to be deleted.
     * @throws com.sun.identity.entitlement.EntitlementException if deletion
     * failed.
     */
    public abstract void deleteReferral(String privilegeName)
        throws EntitlementException;

    /**
     * Deletes a referralprivilege from data store.
     *
     * @param privilegeName name of privilege to be deleted.
     * @param notify <code>true</code> to notify changes.
     * @throws com.sun.identity.entitlement.EntitlementException if deletion
     * failed.
     */
    public abstract String deleteReferral(String privilegeName, boolean notify)
        throws EntitlementException;

    /**
     * Deletes a privilege from data store.
     *
     * @param privilegeName name of privilege to be deleted.
     * @param notify <code>true</code> to notify changes.
     * @throws com.sun.identity.entitlement.EntitlementException if deletion
     * failed.
     */
    public abstract String delete(String privilegeName, boolean notify)
        throws EntitlementException;

    /**
     * Returns an iterator of matching privilege objects.
     *
     * @param realm Realm name.
     * @param indexes Resource search indexes.
     * @param subjectIndexes Subject search indexes.
     * @param bSubTree <code>true</code> for sub tree evaluation.
     * @return an iterator of matching privilege objects.
     * @throws com.sun.identity.entitlement.EntitlementException if results
     * cannot be obtained.
     */
    public abstract Iterator<IPrivilege> search(
        String realm,
        ResourceSearchIndexes indexes,
        Set<String> subjectIndexes,
        boolean bSubTree
    ) throws EntitlementException;

    /**
     * Returns a set of privilege names that matched a set of search criteria.
     *
     * @param filters Set of search filter (criteria).
     * @param boolAnd <code>true</code> to be inclusive.
     * @param numOfEntries Number of maximum search entries.
     * @param sortResults <code>true</code> to have the result sorted.
     * @param ascendingOrder  <code>true</code> to have the result sorted in
     *        ascending order.
     * @return a set of privilege names that matched a set of search criteria.
     * @throws EntitlementException if search failed.
     */
    public abstract Set<String> searchPrivilegeNames(
        Set<SearchFilter> filters,
        boolean boolAnd,
        int numOfEntries,
        boolean sortResults,
        boolean ascendingOrder
    ) throws EntitlementException;

    /**
     * Returns a set of referral privilege names that matched a set of search
     * criteria.
     *
     * @param filters Set of search filter (criteria).
     * @param boolAnd <code>true</code> to be inclusive.
     * @param numOfEntries Number of maximum search entries.
     * @param sortResults <code>true</code> to have the result sorted.
     * @param ascendingOrder  <code>true</code> to have the result sorted in
     *        ascending order.
     * @return a set of referral privilege names that matched a set of search
     *         criteria.
     * @throws EntitlementException if search failed.
     */
    public abstract Set<String> searchReferralPrivilegeNames(
        Set<SearchFilter> filters,
        boolean boolAnd,
        int numOfEntries,
        boolean sortResults,
        boolean ascendingOrder
    ) throws EntitlementException;

    /**
     * Returns a set of resources that are referred to this realm.
     *
     * @param applicationTypeName Application type name,
     * @return a set of resources that are referred to this realm.
     * @throws EntitlementException if resources cannot be returned.
     */
    public abstract Set<String> getReferredResources(String applicationTypeName)
        throws EntitlementException;

    public abstract boolean hasPrivilgesWithApplication(
        String realm,
        String applName) throws EntitlementException;
}
