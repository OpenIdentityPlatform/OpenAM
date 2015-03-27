/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * <p/>
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 * <p/>
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 * <p/>
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 * <p/>
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * <p/>
 * $Id: ApplicationManager.java,v 1.11 2010/01/13 23:41:57 veiming Exp $
 *
 * Portions Copyrighted 2013-2015 ForgeRock AS
 */
package com.sun.identity.entitlement;

import static org.forgerock.openam.utils.StringUtils.isBlank;
import static org.forgerock.openam.utils.StringUtils.isNotEmpty;

import com.sun.identity.entitlement.util.SearchFilter;
import com.sun.identity.entitlement.util.SearchFilter.Operator;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.entitlement.service.ApplicationService;
import org.forgerock.util.annotations.VisibleForTesting;

import javax.inject.Singleton;
import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

/**
 * Application Manager handles addition, deletion and listing of applications for each realm.
 * <p />
 * This class should be treated as a singleton for now as it caches applications local to itself.
 */
@Singleton
public final class ApplicationServiceImpl implements ApplicationService {

    private static final Debug DEBUG = Debug.getInstance("Entitlement");

    private final Map<String, Set<Application>> applications;
    private final ReentrantReadWriteLock readWriteLock;

    public ApplicationServiceImpl() {
        applications = new ConcurrentHashMap<String, Set<Application>>();
        readWriteLock = new ReentrantReadWriteLock();
    }

    @Override
    public Set<String> search(Subject adminSubject, String realm, Set<SearchFilter> filters)
            throws EntitlementException {

        if (adminSubject == PrivilegeManager.superAdminSubject) {
            EntitlementConfiguration ec = EntitlementConfiguration.getInstance(adminSubject, realm);
            return ec.searchApplicationNames(adminSubject, filters);
        }

        // Delegation to applications is currently not configurable, passing super admin (see AME-4959)
        ApplicationPrivilegeManager apm =
                ApplicationPrivilegeManager.getInstance(realm, PrivilegeManager.superAdminSubject);
        Set<String> applNames = apm.getApplications(ApplicationPrivilege.Action.READ);
        return filterApplicationNames(realm, applNames, filters);
    }

    private Set<String> filterApplicationNames(String realm, Set<String> applNames, Set<SearchFilter> filters) {
        Set<String> results = new HashSet<String>();

        if ((filters != null) && !filters.isEmpty()) {
            for (String name : applNames) {
                try {
                    Application app = getApplication(PrivilegeManager.superAdminSubject, realm, name);
                    if (app != null) {
                        if (match(filters, app)) {
                            results.add(name);
                        }
                    }
                } catch (EntitlementException ex) {
                    PrivilegeManager.debug.error("ApplicationManager.fitlerApplicationNames", ex);
                }
            }
        } else {
            results.addAll(applNames);
        }

        return results;
    }

    @VisibleForTesting
    boolean match(Set<SearchFilter> filters, Application app) {
        for (SearchFilter filter : filters) {
            if (Application.NAME_ATTRIBUTE.equals(filter.getName())) {
                if (!match(app.getName(), filter.getValue())) {
                    return false;
                }
            } else if (Application.DESCRIPTION_ATTRIBUTE.equals(filter.getName())) {
                if (!match(app.getDescription(), filter.getValue())) {
                    return false;
                }
            } else if (Application.CREATED_BY_ATTRIBUTE.equals(filter.getName())) {
                if (!match(app.getCreatedBy(), filter.getValue())) {
                    return false;
                }
            } else if (Application.LAST_MODIFIED_BY_ATTRIBUTE.equals(filter.getName())) {
                if (!match(app.getLastModifiedBy(), filter.getValue())) {
                    return false;
                }
            } else if (Application.CREATION_DATE_ATTRIBUTE.equals(filter.getName())) {
                if (!match(app.getCreationDate(), filter.getNumericValue(), filter.getOperator())) {
                    return false;
                }
            } else if (Application.LAST_MODIFIED_DATE_ATTRIBUTE.equals(filter.getName())) {
                if (!match(app.getLastModifiedDate(), filter.getNumericValue(), filter.getOperator())) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean match(long value, long pattern, Operator operator) {
        switch (operator) {
            case EQUALS_OPERATOR:
                return value == pattern;
            case GREATER_THAN_OPERATOR:
                return value > pattern;
            case GREATER_THAN_OR_EQUAL_OPERATOR:
                return value >= pattern;
            case LESS_THAN_OPERATOR:
                return value < pattern;
            case LESS_THAN_OR_EQUAL_OPERATOR:
                return value <= pattern;
            default:
                return false;
        }
    }

    private boolean match(String value, String strPattern) {
        if (isNotEmpty(strPattern)) {
            if (isBlank(value)) {
                return strPattern.equals("*");
            }
            value = value.toLowerCase();
            strPattern = strPattern.toLowerCase();
            StringBuilder buff = new StringBuilder();

            for (int i = 0; i < strPattern.length() - 1; i++) {
                char c = strPattern.charAt(i);
                if (c == '*') {
                    buff.append(".*?");
                } else {
                    buff.append(c);
                }
            }

            char lastChar = strPattern.charAt(strPattern.length() - 1);
            if (lastChar == '*') {
                buff.append(".*");
            } else {
                buff.append(lastChar);
            }
            return Pattern.matches(buff.toString(), value);
        }

        return true;
    }

    @Override
    public Set<String> getApplicationNames(
            Subject adminSubject,
            String realm
    ) throws EntitlementException {
        Set<Application> appls = getApplications(adminSubject, realm);
        Set<String> results = new HashSet<String>();
        for (Application appl : appls) {
            results.add(appl.getName());
        }
        return results;
    }

    private Set<Application> getAllApplication(String realm)
            throws EntitlementException {
        EntitlementConfiguration ec = EntitlementConfiguration.getInstance(
                PrivilegeManager.superAdminSubject, realm);
        realm = ec.getRealmName(realm);

        Set<Application> appls = applications.get(realm);
        if (appls != null) {
            return appls;
        }

        readWriteLock.writeLock().lock();
        try {
            appls = ec.getApplications();
            applications.put(realm, appls);

            ReferredApplicationManager mgr = ReferredApplicationManager.getInstance();
            Set<ReferredApplication> referredApplications = mgr.getReferredApplications(realm);

            if (!"/".equals(realm) && (referredApplications == null || referredApplications.isEmpty())) {
                DEBUG.warning("No referred applications for sub-realm: " + realm);
            }

            appls.addAll(referredApplications);
            return appls;
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    private Set<Application> getApplications(Subject adminSubject,
                                             String realm) throws EntitlementException {
        Set<Application> appls = getAllApplication(realm);

        if (adminSubject == PrivilegeManager.superAdminSubject) {
            return appls;
        }

        Set<Application> accessible = new HashSet<Application>();
        // Delegation to applications is currently not configurable, passing super admin (see AME-4959)
        ApplicationPrivilegeManager apm =
                ApplicationPrivilegeManager.getInstance(realm, PrivilegeManager.superAdminSubject);
        Set<String> accessibleApplicationNames =
                apm.getApplications(ApplicationPrivilege.Action.READ);

        for (Application app : appls) {
            String applicationName = app.getName();
            Application cloned = app.clone();

            if (accessibleApplicationNames.contains(applicationName)) {
                accessible.add(cloned);
            }
        }

        return accessible;
    }

    @Override
    public Application getApplicationForEvaluation(
            String realm,
            String name
    ) throws EntitlementException {
        return getApplication(PrivilegeManager.superAdminSubject, realm,
                name);
    }

    @Override
    public Application getApplication(
            Subject adminSubject,
            String realm,
            String name
    ) throws EntitlementException {
        if ((name == null) || (name.length() == 0)) {
            name = ApplicationTypeManager.URL_APPLICATION_TYPE_NAME;
        }

        Set<Application> appls = getApplications(adminSubject, realm);
        for (Application appl : appls) {
            if (appl.getName().equals(name)) {
                return appl;
            }
        }

        // try again, to get application for sub realm.
        clearCache(realm);

        appls = getApplications(adminSubject, realm);
        for (Application appl : appls) {
            if (appl.getName().equals(name)) {
                return appl;
            }
        }

        return null;
    }

    @Override
    public void deleteApplication(
            Subject adminSubject,
            String realm,
            String name
    ) throws EntitlementException {
        boolean allowed = (adminSubject == PrivilegeManager.superAdminSubject);
        if (!allowed) {
            allowed = hasAccessToApplication(realm, adminSubject, name,
                    ApplicationPrivilege.Action.MODIFY);
        }

        if (!allowed) {
            throw new EntitlementException(EntitlementException.PERMISSION_DENIED);
        }

        Application app = getApplication(adminSubject, realm, name);

        if (app != null) {
            if (!app.canBeDeleted()) {
                throw new EntitlementException(EntitlementException.APP_NOT_CREATED_POLICIES_EXIST);
            }
            EntitlementConfiguration ec = EntitlementConfiguration.getInstance(
                    adminSubject, realm);
            ec.removeApplication(name);
            clearCache(realm);
        }
    }

    @Override
    public void saveApplication(
            Subject adminSubject,
            String realm,
            Application application
    ) throws EntitlementException {
        boolean allow = (adminSubject == PrivilegeManager.superAdminSubject);

        if (!allow) {
            ApplicationPrivilegeManager apm =
                    ApplicationPrivilegeManager.getInstance(realm, adminSubject);
            if (isNewApplication(realm, application)) {
                allow = apm.canCreateApplication(realm);
            } else {
                allow = hasAccessToApplication(apm, application,
                        ApplicationPrivilege.Action.MODIFY);
            }
        }

        if (!allow) {
            throw new EntitlementException(326);
        }

        if (isReferredApplication(realm, application)) {
            throw new EntitlementException(228);
        }

        Date date = new Date();
        Set<Principal> principals = adminSubject.getPrincipals();
        String principalName = ((principals != null) && !principals.isEmpty()) ?
                principals.iterator().next().getName() : null;

        if (application.getCreationDate() == -1) {
            long creationDate = getApplicationCreationDate(realm,
                    application.getName());
            if (creationDate == -1) {
                application.setCreationDate(date.getTime());
                if (principalName != null) {
                    application.setCreatedBy(principalName);
                }
            } else {
                application.setCreationDate(creationDate);
                String createdBy = application.getCreatedBy();
                if ((createdBy == null) || (createdBy.trim().length() == 0)) {
                    createdBy = getApplicationCreatedBy(realm,
                            application.getName());
                    if ((createdBy == null) || (createdBy.trim().length() == 0)) {
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

        EntitlementConfiguration ec = EntitlementConfiguration.getInstance(
                adminSubject, realm);
        ec.storeApplication(application);
        clearCache(realm);
    }

    private String getApplicationCreatedBy(
            String realm,
            String applName
    ) {
        try {
            Application appl = getApplication(PrivilegeManager.superAdminSubject,
                    realm, applName);
            return (appl == null) ? null : appl.getCreatedBy();
        } catch (EntitlementException ex) {
            // new application.
            return null;
        }
    }

    private long getApplicationCreationDate(
            String realm,
            String applName
    ) {
        try {
            Application appl = getApplication(PrivilegeManager.superAdminSubject,
                    realm, applName);
            return (appl == null) ? -1 : appl.getCreationDate();
        } catch (EntitlementException ex) {
            // new application.
            return -1;
        }
    }


    private boolean isReferredApplication(
            String realm,
            Application application) throws EntitlementException {
        Set<ReferredApplication> referredAppls =
                ReferredApplicationManager.getInstance().getReferredApplications(
                        realm);
        for (ReferredApplication ra : referredAppls) {
            if (ra.getName().equals(application.getName())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAccessToApplication(
            String realm,
            Subject adminSubject,
            String applicationName,
            ApplicationPrivilege.Action action) {
        ApplicationPrivilegeManager apm =
                ApplicationPrivilegeManager.getInstance(realm,
                        adminSubject);
        Set<String> applicationNames = apm.getApplications(action);

        // applicationNames may be empty if the sub realm is removed.
        // or the sub realm really do not have referral privilege assigned to
        // it. In the latter case, clearing the cache for referral privilege
        // should be ok.
        return applicationNames.isEmpty() ||
                applicationNames.contains(applicationName);
    }

    private boolean hasAccessToApplication(
            ApplicationPrivilegeManager apm,
            Application application,
            ApplicationPrivilege.Action action) {
        Set<String> applNames = apm.getApplications(action);
        return applNames.contains(application.getName());
    }

    private boolean isNewApplication(
            String realm,
            Application application
    ) throws EntitlementException {
        Set<Application> existingAppls = getAllApplication(realm);
        String applName = application.getName();

        for (Application app : existingAppls) {
            if (app.getName().equals(applName)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void clearCache(String realm) {
        for (String name : applications.keySet()) {
            if (name.equalsIgnoreCase(realm)) {
                applications.remove(name);
                break;
            }
        }
    }

    @Override
    public Set<String> getReferredResources(
            Subject adminSubject,
            String realm,
            String applicationTypeName
    ) throws EntitlementException {
        boolean allowed = (adminSubject == PrivilegeManager.superAdminSubject);
        if (!allowed) {
            // Delegation to applications is currently not configurable, passing super admin (see AME-4959)
            allowed = hasAccessToApplication(realm, PrivilegeManager.superAdminSubject,
                    applicationTypeName, ApplicationPrivilege.Action.READ);
        }

        if (!allowed) {
            return Collections.EMPTY_SET;
        }

        PrivilegeIndexStore pis = PrivilegeIndexStore.getInstance(
                adminSubject, realm);
        return pis.getReferredResources(applicationTypeName);
    }


    @Override
    public void updateApplication(Application oldApplication, Application newApplication, Subject subject,
                                  String realm)
            throws EntitlementException {

        readWriteLock.writeLock().lock();

        try {
            newApplication.setCreationDate(oldApplication.getCreationDate());
            newApplication.setCreatedBy(oldApplication.getCreatedBy());
            deleteApplication(subject, realm, oldApplication.getName());
            saveApplication(subject, realm, newApplication);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }
}
