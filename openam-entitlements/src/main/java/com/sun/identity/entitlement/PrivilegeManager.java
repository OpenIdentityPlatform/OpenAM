/*
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
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */
package com.sun.identity.entitlement;

import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.entitlement.util.SearchFilter;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.entitlement.PolicyConstants;
import org.forgerock.openam.entitlement.ResourceType;
import org.forgerock.openam.entitlement.constraints.ConstraintValidator;
import org.forgerock.openam.entitlement.service.ApplicationService;
import org.forgerock.openam.entitlement.service.ApplicationServiceFactory;
import org.forgerock.openam.entitlement.service.ResourceTypeService;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Class to manage entitlement privileges: to add, remove, modify privilege
 */
public abstract class PrivilegeManager implements IPrivilegeManager<Privilege> {
    /**
     * Debug for Policy Administration Point classes
     */
    public static final Debug debug = PolicyConstants.DEBUG;

    //REF: make configurable
    private static final Pattern PRIVILEGE_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9\\- _]*");
    public static final Subject superAdminSubject = PolicyConstants.SUPER_ADMIN_SUBJECT;

    private String realm;
    private Subject adminSubject;

    private final ResourceTypeService resourceTypeService;
    private final ApplicationServiceFactory applicationServiceFactory;
    private final ConstraintValidator validator;

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

        try {
            final Class<? extends PrivilegeManager> clazz = Class
                    .forName("com.sun.identity.entitlement.opensso.PolicyPrivilegeManager")
                    .asSubclass(PrivilegeManager.class);
            final PrivilegeManager privilegeManager = InjectorHolder.getInstance(clazz);
            privilegeManager.initialize(realm, subject);
            return privilegeManager;

        } catch (ClassNotFoundException e) {
            debug.error("PrivilegeManager.getInstance", e);
        }

        return null;
    }

    /**
     * Constructor.
     */
    public PrivilegeManager(final ApplicationServiceFactory applicationServiceFactory,
                            final ResourceTypeService resourceTypeService, final ConstraintValidator validator) {
        this.applicationServiceFactory = applicationServiceFactory;
        this.resourceTypeService = resourceTypeService;
        this.validator = validator;
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
        SearchFilter filter = new SearchFilter(Privilege.NAME_SEARCH_ATTRIBUTE, name);
        return !searchNames(asSet(filter)).isEmpty();
    }

    /**
     * Validates the passed policy.
     *
     * @param privilege
     *         the policy instance
     *
     * @throws EntitlementException
     *         should validator fail
     */
    protected void validate(Privilege privilege) throws EntitlementException {
        final String pName = privilege.getName();
        if (pName == null || pName.trim().isEmpty()) {
            throw new EntitlementException(EntitlementException.EMPTY_PRIVILEGE_NAME);
        }

        final Entitlement entitlement = privilege.getEntitlement();
        if (entitlement == null) {
            throw new EntitlementException(EntitlementException.NULL_ENTITLEMENT);
        }

        privilege.validateSubject(privilege.getSubject());

        ApplicationService applicationService = applicationServiceFactory.create(adminSubject, realm);
        Application application = applicationService.getApplication(entitlement.getApplicationName());

        if (application == null) {
            throw new EntitlementException(EntitlementException.APP_RETRIEVAL_ERROR, realm);
        }

        if (CollectionUtils.isEmpty(application.getResourceTypeUuids())) {

            if (StringUtils.isNotEmpty(privilege.getResourceTypeUuid())) {
                throw new EntitlementException(EntitlementException.NO_RESOURCE_TYPE_EXPECTED);
            }

            // If no resource types have been defined then the following resource type validation is irrelevant.
            return;
        }

        if (!application.getResourceTypeUuids().contains(privilege.getResourceTypeUuid())) {
            throw new EntitlementException(
                    EntitlementException.POLICY_DEFINES_INVALID_RESOURCE_TYPE, privilege.getResourceTypeUuid());
        }

        final ResourceType resourceType = resourceTypeService
                .getResourceType(superAdminSubject, realm, privilege.getResourceTypeUuid());

        if (resourceType == null) {
            throw new EntitlementException(
                    EntitlementException.NO_SUCH_RESOURCE_TYPE, privilege.getResourceTypeUuid(), realm);
        }

        validator
                .verifyActions(entitlement.getActionValues().keySet())
                .against(resourceType)
                .throwExceptionIfFailure();

        validator
                .verifyResources(entitlement.getResourceNames())
                .using(entitlement.getResourceComparator(superAdminSubject, realm))
                .against(resourceType)
                .throwExceptionIfFailure();
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
        Date date = newDate();
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
     * Finds all policies within the realm.
     *
     * @return list of matching policies
     *
     * @throws EntitlementException
     *         should some error occur
     */
    public abstract List<Privilege> findAllPolicies() throws EntitlementException;

    /**
     * Finds all policies within the realm and passed application.
     *
     * @param application
     *         the application
     *
     * @return list of matching policies
     *
     * @throws EntitlementException
     *         should some error occur
     */
    public abstract List<Privilege> findAllPoliciesByApplication(String application) throws EntitlementException;

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
