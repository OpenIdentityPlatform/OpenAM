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
 *
 * Portions Copyrighted 2011-2014 ForgeRock AS
 */
package com.sun.identity.entitlement;

import com.sun.identity.entitlement.util.SearchFilter;
import com.sun.identity.shared.debug.Debug;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.security.auth.Subject;

import static org.forgerock.openam.utils.CollectionUtils.asSet;

/**
 * Class to manage entitlement privileges: to add, remove, modify privilege
 */
public abstract class PrivilegeManager implements IPrivilegeManager<Privilege> {
    /**
     * Debug for Policy Administration Point classes
     */
    public static final Debug debug = Debug.getInstance("Entitlement");

    //REF: make configurable
    private static final Pattern PRIVILEGE_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9\\- _]*");
    public static final Subject superAdminSubject = new Subject();

    private String realm;
    private Subject adminSubject;

    /**
     * Returns instance of configured <code>PrivilegeManager</code>
     * @param subject subject that would be used for the privilege management operations
     * @return instance of configured <code>PrivilegeManager</code>
     */
    static public PrivilegeManager getInstance(String realm, Subject subject) {
        EntitlementConfiguration ec = EntitlementConfiguration.getInstance(subject, realm);
        if (!ec.migratedToEntitlementService()) {
            throw new UnsupportedOperationException(
                "Updating of DITs is required before using the entitlement service");
        }
        PrivilegeManager pm = null;
        try {
            //RFE: read the class name from configuration
            Class clazz = Class.forName("com.sun.identity.entitlement.opensso.PolicyPrivilegeManager");
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
     * @param name name for the privilege to be returned
     * @param subject Subject to be used to obtain the privilege.
     * @throws EntitlementException if privilege is not found or if the provided subject is not permitted to access it.
     */
    public abstract Privilege findByName(String name, Subject subject) throws EntitlementException;

    /**
     * Checks if a privilege with the specified name can be found.
     *
     * @param name name of the privilege.
     * @throws com.sun.identity.entitlement.EntitlementException if search failed.
     * @return true if a privilege with the specified name exists, false otherwise.
     */
    @Override
    public boolean canFindByName(String name) throws EntitlementException {
        SearchFilter filter = new SearchFilter("name", name);
        return !searchNames(asSet(filter)).isEmpty();
    }

    protected void validate(Privilege privilege) throws EntitlementException {
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
     * Add a privilege.
     *
     * @param privilege privilege to add.
     * @throws EntitlementException if privilege cannot be added.
     */
    @Override
    public void add(Privilege privilege) throws EntitlementException {
        validate(privilege);
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
     * Modifies the specified policy.
     *
     * @param existingName
     *         The existing policy name
     * @param privilege
     *         The new policy content
     *
     * @throws EntitlementException
     *         When an error occurs during modification
     */
    public abstract void modify(String existingName, Privilege privilege) throws EntitlementException;

    /**
     * Returns a set of privilege names for a given search criteria.
     *
     * @param filter Set of search filter.
     * @param searchSizeLimit Search size limit.
     * @param searchTimeLimit Search time limit in seconds.
     * @return a set of privilege names for a given search criteria.
     * @throws EntitlementException if search failed.
     */
    @Override
    public Set<String> searchNames(Set<SearchFilter> filter, int searchSizeLimit, int searchTimeLimit)
            throws EntitlementException {

        List<Privilege> privileges = search(filter, searchSizeLimit, searchTimeLimit);
        Set<String> result = new HashSet<String>(privileges.size());

        for (Privilege privilege : privileges) {
            result.add(privilege.getName());
        }

        return result;
    }

    /**
     * Returns a set of privileges that match the given search criteria.
     *
     * @param filter the search filters to apply. An empty set means no filtering (returns all privileges).
     * @param searchSizeLimit the maximum number of privileges to return.
     * @param searchTimeLimit the maximum time limit in seconds. NOT IMPLEMENTED.
     * @return the matching privileges.
     * @throws EntitlementException if the search fails for any reason.
     */
    public List<Privilege> search(Set<SearchFilter> filter, int searchSizeLimit, int searchTimeLimit)
            throws EntitlementException {
        boolean hasSizeLimit = (searchSizeLimit > 0);

        PrivilegeIndexStore pis = PrivilegeIndexStore.getInstance(adminSubject, realm);
        Set<String> privilegeNames = pis.searchPrivilegeNames(filter, true, searchSizeLimit, false, false);
        // TODO Search time limit

        List<Privilege> results = new ArrayList<Privilege>(privilegeNames.size());

        // Delegation to applications is currently not configurable, passing super admin (see AME-4959)
        ApplicationPrivilegeManager applPrivilegeMgr = ApplicationPrivilegeManager
                .getInstance(realm, PrivilegeManager.superAdminSubject);

        for (String name : privilegeNames) {
            Privilege privilege = findByName(name, PrivilegeManager.superAdminSubject);

            if (applPrivilegeMgr.hasPrivilege(privilege, ApplicationPrivilege.Action.READ)) {
                results.add(privilege);

                if (hasSizeLimit && (results.size() >= searchSizeLimit)) {
                    break;
                }
            }
        }

        return results;
    }

    /**
     * Returns a set of privileges that match the given search criteria with no size or time limits.
     *
     * @param filter the search filters to apply. An empty set means no filtering (returns all privileges).
     * @return the matching privileges.
     * @throws EntitlementException if the search fails for any reason.
     */
    public List<Privilege> search(Set<SearchFilter> filter) throws EntitlementException {
        return search(filter, 0, 0);
    }

    /**
     * Returns a set of privilege names for a given search criteria.
     *
     * @param filter Set of search filter.
     * @return a set of privilege names for a given search criteria.
     * @throws EntitlementException if search failed.
     */
    @Override
    public Set<String> searchNames(Set<SearchFilter> filter) throws EntitlementException {
        return searchNames(filter, 0, 0);
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
    public abstract String getPrivilegeXML(String name) throws EntitlementException;

    /**
     * Returns the XML representation of this privilege.
     *
     * @param names Name of Privileges to export as XML.
     * @return XML representation of the specified privileges
     * @throws EntitlementException if a specified privilege is not found, or cannot
     * be obtained.
     */
    public abstract String getPrivilegesXML(Set<String> names) throws EntitlementException;

    protected Subject getAdminSubject() {
        return adminSubject;
    }

    protected void notifyPrivilegeChanged(String realm, Privilege previous, Privilege current)
            throws EntitlementException {

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
