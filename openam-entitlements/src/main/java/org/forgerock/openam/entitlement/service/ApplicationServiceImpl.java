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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */
package org.forgerock.openam.entitlement.service;

import static com.sun.identity.entitlement.ApplicationPrivilege.Action;
import static com.sun.identity.entitlement.EntitlementException.*;
import static java.util.Collections.emptySet;
import static org.forgerock.openam.entitlement.PolicyConstants.SUPER_ADMIN_SUBJECT;
import static org.forgerock.openam.utils.CollectionUtils.isNotEmpty;
import static org.forgerock.openam.utils.StringUtils.isBlank;
import static org.forgerock.openam.utils.Time.newDate;

import java.security.Principal;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import jakarta.inject.Inject;
import javax.security.auth.Subject;

import com.sun.identity.entitlement.DenyOverride;
import org.forgerock.util.Reject;
import org.forgerock.util.query.QueryFilter;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationPrivilegeManager;
import com.sun.identity.entitlement.ApplicationTypeManager;
import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeIndexStore;
import com.sun.identity.sm.DNMapper;

/**
 * An application service implementation.
 *
 * @since 13.0.0
 */
public class ApplicationServiceImpl implements ApplicationService {

    private static final Map<String, Set<Application>> applications = new ConcurrentHashMap<>();
    private static final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private final Subject subject;
    private final String realm;
    private final EntitlementConfiguration configuration;
    private final EntitlementConfiguration adminConfiguration;
    private final ResourceTypeService resourceTypeService;
    private final boolean superAdminSubject;

    @Inject
    public ApplicationServiceImpl(@Assisted final Subject subject, @Assisted final String realm,
            EntitlementConfigurationFactory configurationFactory, ResourceTypeService resourceTypeService) {
        Reject.ifNull(subject, realm);
        this.subject = subject;
        this.realm = realm;
        this.configuration = configurationFactory.create(subject, realm);
        this.adminConfiguration = configurationFactory.create(SUPER_ADMIN_SUBJECT, realm);
        this.resourceTypeService = resourceTypeService;
        this.superAdminSubject = SUPER_ADMIN_SUBJECT.equals(subject);
    }

    @Override
    public Set<Application> search(QueryFilter<String> queryFilter) throws EntitlementException {
        Set<Application> applications = adminConfiguration.searchApplications(subject, queryFilter);

        if (superAdminSubject) {
            return applications;
        }

        return getAccessibleApplications(realm, applications);
    }

    @Override
    public Set<String> getApplicationNames() throws EntitlementException {
        Set<Application> appls = getApplications(subject, realm);
        Set<String> results = new HashSet<>();
        for (Application appl : appls) {
            results.add(appl.getName());
        }
        return results;
    }

    @Override
    public Set<Application> getApplications() throws EntitlementException {
        return getApplications(subject, realm);
    }

    @Override
    public Application getApplication(String name) throws EntitlementException {
        return getApplication(subject, name);
    }

    @Override
    public void deleteApplication(String name) throws EntitlementException {
        boolean allowed = superAdminSubject;
        if (!allowed) {
            allowed = hasAccessToApplication(subject, name, Action.MODIFY);
        }

        if (!allowed) {
            throw new EntitlementException(PERMISSION_DENIED);
        }

        Application app = getApplication(subject, name);

        if (app != null) {
            if (!app.canBeDeleted(realm)) {
                throw new EntitlementException(APP_NOT_CREATED_POLICIES_EXIST);
            }
            configuration.removeApplication(name);
            clearCache();
        }
    }

    @Override
    public Application saveApplication(Application application) throws EntitlementException {
        checkUserPrivileges(application);
        checkIfResourceTypeExists(application);
        setApplicationMetaData(application);
        setApplicationDefaultValues(application);
        configuration.storeApplication(application);
        clearCache();
        return application;
    }

    @Override
    public void clearCache() {
        for (String name : applications.keySet()) {
            if (name.equalsIgnoreCase(realm)) {
                applications.remove(name);
                break;
            }
        }
    }

    @Override
    public Set<String> getReferredResources(String applicationTypeName) throws EntitlementException {
        boolean allowed = superAdminSubject;
        if (!allowed) {
            // Delegation to applications is currently not configurable, passing super admin (see AME-4959)
            allowed = hasAccessToApplication(SUPER_ADMIN_SUBJECT, applicationTypeName, Action.READ);
        }

        if (!allowed) {
            return emptySet();
        }

        return PrivilegeIndexStore.getInstance(subject, realm).getReferredResources(applicationTypeName);
    }

    private Set<Application> getAccessibleApplications(String realm, Set<Application> applications) {
        // Delegation to applications is currently not configurable, passing super admin (see AME-4959)
        ApplicationPrivilegeManager apm = ApplicationPrivilegeManager.getInstance(realm, SUPER_ADMIN_SUBJECT);
        Set<String> accessibleApplicationNames = apm.getApplications(Action.READ);
        Set<Application> accessible = new HashSet<>();
        for (Application app : applications) {
            String applicationName = app.getName();
            Application cloned = app.clone();
            if (accessibleApplicationNames.contains(applicationName)) {
                accessible.add(cloned);
            }
        }
        return accessible;
    }

    private Set<Application> getApplications(Subject adminSubject, String realm) throws EntitlementException {
        Set<Application> applications = getApplicationsFromCache(realm);

        if (SUPER_ADMIN_SUBJECT.equals(adminSubject)) {
            return applications;
        }

        return getAccessibleApplications(realm, applications);
    }

    private Application getApplication(Subject adminSubject, String name) throws EntitlementException {
        name = validateApplicationName(name);

        Set<Application> appls = getApplications(adminSubject, realm);
        for (Application appl : appls) {
            if (appl.getName().equalsIgnoreCase(name)) {
                return appl;
            }
        }

        // try again by refreshing the cache
        clearCache();

        appls = getApplications(adminSubject, realm);
        for (Application appl : appls) {
            if (appl.getName().equalsIgnoreCase(name)) {
                return appl;
            }
        }

        return null;
    }

    private String validateApplicationName(String name) {
        return isBlank(name) ? ApplicationTypeManager.URL_APPLICATION_TYPE_NAME : name;
    }

    private Set<Application> getApplicationsFromCache(String realm) throws EntitlementException {
        realm = DNMapper.orgNameToRealmName(realm);

        Set<Application> appls = applications.get(realm);
        if (appls != null) {
            return appls;
        }

        readWriteLock.writeLock().lock();
        try {
            appls = adminConfiguration.getApplications();
            applications.put(realm, appls);
            return appls;
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    private void checkUserPrivileges(Application application) throws EntitlementException {
        boolean allow = superAdminSubject;

        if (!allow) {
            ApplicationPrivilegeManager apm = ApplicationPrivilegeManager.getInstance(realm, subject);
            if (apm == null) {
                allow = false;
            } else if (isNewApplication(application)) {
                allow = apm.canCreateApplication(realm);
            } else {
                allow = hasAccessToApplication(apm, application, Action.MODIFY);
            }
        }

        if (!allow) {
            throw new EntitlementException(PERMISSION_DENIED);
        }
    }

    private void checkIfResourceTypeExists(Application application) throws EntitlementException {
        if (isNotEmpty(application.getResourceTypeUuids())) {
            Set<String> resourceTypeIds = application.getResourceTypeUuids();

            for (String resourceTypeId : resourceTypeIds) {
                if (!resourceTypeService.contains(subject, realm, resourceTypeId)) {
                    throw new EntitlementException(INVALID_RESOURCE_TYPE, resourceTypeId);
                }
            }
        }
    }

    private void setApplicationMetaData(Application application) {
        Date date = newDate();
        Set<Principal> principals = subject.getPrincipals();
        String principalName = isNotEmpty(principals) ? principals.iterator().next().getName() : null;
        if (application.getCreationDate() == -1) {
            long creationDate = getApplicationCreationDate(application.getName());
            if (creationDate == -1) {
                application.setCreationDate(date.getTime());
                if (principalName != null) {
                    application.setCreatedBy(principalName);
                }
            } else {
                application.setCreationDate(creationDate);
                String createdBy = application.getCreatedBy();
                if (isBlank(createdBy)) {
                    createdBy = getApplicationCreatedBy(application.getName());
                    if (isBlank(createdBy)) {
                        application.setCreatedBy(principalName);
                    } else {
                        application.setCreatedBy(createdBy);
                    }
                }
            }
        }
        application.setLastModifiedDate(date.getTime());
        if (principalName != null) {
            application.setLastModifiedBy(principalName);
        }
    }

    private void setApplicationDefaultValues(Application application) {
        if (application.getEntitlementCombiner() == null) {
            application.setEntitlementCombiner(DenyOverride.class);
        }
    }

    private String getApplicationCreatedBy(String applName) {
        try {
            Application appl = getApplication(SUPER_ADMIN_SUBJECT, applName);
            return (appl == null) ? null : appl.getCreatedBy();
        } catch (EntitlementException ex) {
            // new application.
            return null;
        }
    }

    private long getApplicationCreationDate(String applName) {
        try {
            Application appl = getApplication(SUPER_ADMIN_SUBJECT, applName);
            return (appl == null) ? -1 : appl.getCreationDate();
        } catch (EntitlementException ex) {
            // new application.
            return -1;
        }
    }

    private boolean hasAccessToApplication(Subject adminSubject, String applicationName, Action action) {
        ApplicationPrivilegeManager apm = ApplicationPrivilegeManager.getInstance(realm, adminSubject);
        Set<String> applicationNames = apm.getApplications(action);

        // applicationNames may be empty if the sub realm is removed.
        // or the sub realm really do not have referral privilege assigned to
        // it. In the latter case, clearing the cache for referral privilege
        // should be ok.
        return applicationNames.isEmpty() || applicationNames.contains(applicationName);
    }

    private boolean hasAccessToApplication(ApplicationPrivilegeManager apm, Application application, Action action) {
        Set<String> applNames = apm.getApplications(action);
        return applNames.contains(application.getName());
    }

    private boolean isNewApplication(Application application) throws EntitlementException {
        Set<Application> existingAppls = getApplicationsFromCache(realm);
        String applName = application.getName();

        for (Application app : existingAppls) {
            if (app.getName().equalsIgnoreCase(applName)) {
                return false;
            }
        }
        return true;
    }

}
