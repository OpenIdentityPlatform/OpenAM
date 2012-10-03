/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PrivilegeManager.java,v 1.8 2010/01/26 20:10:15 dillidorai Exp $
 */
/**
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.entitlement;

import com.sun.identity.entitlement.util.SearchFilter;
import com.sun.identity.shared.debug.Debug;
import java.security.Principal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import javax.security.auth.Subject;

/**
 * Class to manage entitlement privileges: to add, remove, modify privilege
 */
public abstract class PrivilegeManager {
    /**
     * Debug for Policy Administration Point classes
     */
    public static final Debug debug = Debug.getInstance("Entitlement");

    //REF: make configurable
    private static final Pattern PRIVILEGE_NAME_PATTERN = Pattern.compile(
        "[a-zA-Z0-9\\- _]*");
    public static final Subject superAdminSubject = new Subject();

    private String realm;
    private Subject adminSubject;

    /**
     * Returns instance of configured <code>PrivilegeManager</code>
     * @param subject subject that would be used for the privilege management 
     * operations
     * @return instance of configured <code>PrivilegeManager</code>
     */
    static public PrivilegeManager getInstance(
        String realm,
        Subject subject) {
        EntitlementConfiguration ec = EntitlementConfiguration.getInstance(
            subject, realm);
        if (!ec.migratedToEntitlementService()) {
            throw new UnsupportedOperationException(
                "Updating of DITs is required before using the entitlement service");
        }
        PrivilegeManager pm = null;
        try {
            //RFE: read the class name from configuration
            Class clazz = Class.forName(
                "com.sun.identity.entitlement.opensso.PolicyPrivilegeManager");
            pm = (PrivilegeManager)clazz.newInstance();
            pm.initialize(realm, subject);
        } catch (ClassNotFoundException e) {
            debug.error("PrivilegeManager.getInstance", e);
        } catch (InstantiationException e) {
            debug.error("PrivilegeManager.getInstance", e);
        } catch (IllegalAccessException e) {
            debug.error("PrivilegeManager.getInstance", e);
        }
        return pm;
    }

    /**
     * Constructor.
     */
    protected PrivilegeManager() {
    }

    /**
     * Initializes the object.
     *
     * @param realm Realm name
     * @param subject subject to initilialize the privilege manager with
     */
    public void initialize(String realm, Subject subject) {
        this.realm = realm;
        this.adminSubject = subject;
    }

    /**
     * Returns a privilege.
     *
     * @param privilegeName name for the privilege to be returned
     * @param subject Subject to be used to obtain the privilege.
     * @throws EntitlementException if privilege is not found.
     */
    public abstract Privilege getPrivilege(
        String privilegeName,
        Subject subject)
            throws EntitlementException;

    /**
     * Returns a privilege.
     *
     * @param privilegeName name for the privilege to be returned
     * @throws EntitlementException if privilege is not found.
     */
    public abstract Privilege getPrivilege(String privilegeName)
            throws EntitlementException;

    protected void validatePrivilege(Privilege privilege)
        throws EntitlementException {
        String pName = privilege.getName();
        if ((pName == null) || (pName.trim().length() == 0)) {
            throw new EntitlementException(3);
        }

        if (privilege.getEntitlement() == null) {
            throw new EntitlementException(4);
        }

        privilege.validateSubject(privilege.getSubject());
    }

    /**
     * Adds a privilege.
     *
     * @param privilege privilege to be added
     * @throws EntitlementException if the privilege could not be added
     */
    public void addPrivilege(Privilege privilege)
        throws EntitlementException {
        validatePrivilege(privilege);
        Date date = new Date();
        privilege.validateResourceNames(adminSubject, realm);
        privilege.setCreationDate(date.getTime());
        privilege.setLastModifiedDate(date.getTime());

        Set<Principal> principals = adminSubject.getPrincipals();
        String principalName = ((principals != null) && !principals.isEmpty()) ?
            principals.iterator().next().getName() : null;

        if (principalName != null) {
            privilege.setCreatedBy(principalName);
            privilege.setLastModifiedBy(principalName);
        }
    }

    /**
     * Removes a privilege.
     *
     * @param privilegeName name of the privilege to be removed
     * @throws EntitlementException if privilege cannot be removed.
     */
    public void removePrivilege(String privilegeName)
        throws EntitlementException {
        
    }

    /**
     * Modifies a privilege.
     *
     * @param privilege the privilege to be modified
     * @throws EntitlementException if privilege cannot be modified.
     */
    public abstract void modifyPrivilege(Privilege privilege)
        throws EntitlementException;

    /**
     * Returns a set of privilege names for a given search criteria.
     *
     * @param filter Set of search filter.
     * @param searchSizeLimit Search size limit.
     * @param searchTimeLimit Search time limit in seconds.
     * @return a set of privilege names for a given search criteria.
     * @throws EntitlementException if search failed.
     */
    public Set<String> searchPrivilegeNames(
        Set<SearchFilter> filter,
        int searchSizeLimit,
        int searchTimeLimit
    ) throws EntitlementException {
        boolean hasSizeLimit = (searchSizeLimit > 0);
        PrivilegeIndexStore pis = PrivilegeIndexStore.getInstance(
            adminSubject, realm);
        Set<String> privilegeNames = pis.searchPrivilegeNames(
            filter, true, searchSizeLimit, false, false); // TODO Search time limit
        Set<String> results = new HashSet<String>();

        ApplicationPrivilegeManager applPrivilegeMgr =
            ApplicationPrivilegeManager.getInstance(realm, adminSubject);

        for (String name : privilegeNames) {
            Privilege privilege = getPrivilege(name,
                PrivilegeManager.superAdminSubject);

            if (applPrivilegeMgr.hasPrivilege(privilege,
                ApplicationPrivilege.Action.READ)) {
                results.add(name);

                if (hasSizeLimit && (results.size() >= searchSizeLimit)) {
                    break;
                }
            }
        }

        return results;
    }

    /**
     * Returns a set of privilege names for a given search criteria.
     *
     * @param filter Set of search filter.
     * @return a set of privilege names for a given search criteria.
     * @throws EntitlementException if search failed.
     */
    public Set<String> searchPrivilegeNames(
        Set<SearchFilter> filter
    ) throws EntitlementException {
        return searchPrivilegeNames(filter, 0, 0);
    }

    /**
     * Returns realm name.
     *
     * @return realm name.
     */
    public String getRealm() {
        return realm;
    }

    /**
     * Returns the XML representation of this privilege.
     *
     * @param name Name of Privilege.
     * @return XML representation of this privilege.
     * @throws EntitlementException if privilege is not found, or cannot
     * be obtained.
     */
    public abstract String getPrivilegeXML(String name)
        throws EntitlementException;

    /**
     * Returns the XML representation of this privilege.
     *
     * @param names Name of Privileges to export as XML.
     * @return XML representation of the specified privileges
     * @throws EntitlementException if a specified privilege is not found, or cannot
     * be obtained.
     */
    public abstract String getPrivilegesXML(Set<String> names)
        throws EntitlementException;

    protected Subject getAdminSubject() {
        return adminSubject;
    }

    protected void notifyPrivilegeChanged(
        String realm,
        Privilege previous,
        Privilege current) {
        Set<String> resourceNames = new HashSet<String>();
        if (previous != null) {
            Set<String> r = previous.getEntitlement().getResourceNames();
            if (r != null) {
                resourceNames.addAll(r);
            }
        }

        Set<String> r = current.getEntitlement().getResourceNames();
        if (r != null) {
            resourceNames.addAll(r);
        }

        String applicationName = current.getEntitlement().getApplicationName();

        PrivilegeChangeNotifier.getInstance().notify(adminSubject, realm,
            applicationName, current.getName(), resourceNames);
    }

    public static boolean isNameValid(String target) {
        return PRIVILEGE_NAME_PATTERN.matcher(target).matches();
    }
}
