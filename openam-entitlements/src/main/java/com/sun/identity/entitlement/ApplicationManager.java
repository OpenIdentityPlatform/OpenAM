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
 * $Id: ApplicationManager.java,v 1.11 2010/01/13 23:41:57 veiming Exp $
 */

/*
 * Portions Copyrighted 2013 ForgeRock AS
 */
package com.sun.identity.entitlement;

import com.sun.identity.entitlement.interfaces.ResourceName;
import com.sun.identity.entitlement.util.SearchFilter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.Principal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;
import javax.security.auth.Subject;

/**
 * Application Manager handles addition, deletion and listing of applications
 * for each realm.
 */
public final class ApplicationManager {
    private static Map<String, Set<Application>> applications =
        new ConcurrentHashMap<String, Set<Application>>();
    private static final ReentrantReadWriteLock readWriteLock =
        new ReentrantReadWriteLock();

    private ApplicationManager() {
    }


    /**
     * Returns the application names in a realm.
     *
     * @param adminSubject Admin Subject who has the rights to access
     *        configuration datastore.
     * @param realm Realm name.
     * @param filters Search Filters
     * @return application names in a realm.
     */
    public static Set<String> search(
        Subject adminSubject,
        String realm,
        Set<SearchFilter> filters
    ) throws EntitlementException {
        if (adminSubject == PrivilegeManager.superAdminSubject) {
            EntitlementConfiguration ec = EntitlementConfiguration.getInstance(
                adminSubject, realm);
            return ec.searchApplicationNames(adminSubject, filters);
        }

        ApplicationPrivilegeManager apm =
            ApplicationPrivilegeManager.getInstance(realm,
            adminSubject);
        Set<String> applNames = apm.getApplications(
            ApplicationPrivilege.Action.READ);
        return filterApplicationNames(realm, applNames, filters);
    }

    private static Set<String> filterApplicationNames(
        String realm,
        Set<String> applNames,
        Set<SearchFilter> filters
    ) {
        Set<String> results = new HashSet<String>();

        if ((filters != null) && !filters.isEmpty()) {
            for (String name : applNames) {
                try {
                    Application app = ApplicationManager.getApplication(
                        PrivilegeManager.superAdminSubject, realm, name);
                    if (app != null) {
                        if (match(filters, app)) {
                            results.add(name);
                        }
                    }
                } catch (EntitlementException ex) {
                    PrivilegeManager.debug.error(
                        "ApplicationManager.fitlerApplicationNames", ex);
                }
            }
        } else {
            results.addAll(applNames);
        }

        return results;
    }

    private static boolean match(Set<SearchFilter> filters, Application app) {
        for (SearchFilter f : filters) {
            String filterName = f.getName();
            if (filterName.equals(Application.NAME_ATTRIBUTE))  {
                if (!match(app.getName(), f.getValue())) {
                    return false;
                }
            } else if (filterName.equals(Application.DESCRIPTION_ATTRIBUTE)) {
                if (!match(app.getDescription(), f.getValue())) {
                    return false;
                }
            } else if (filterName.equals(Application.CREATED_BY_ATTRIBUTE)) {
                if (!match(app.getCreatedBy(), f.getValue())) {
                    return false;
                }
            } else if (filterName.equals(
                Application.LAST_MODIFIED_BY_ATTRIBUTE)) {
                if (!match(app.getLastModifiedBy(), f.getValue())) {
                    return false;
                }
            } else if (filterName.equals(Application.CREATION_DATE_ATTRIBUTE)){
                if (!match(app.getCreationDate(), f.getNumericValue(),
                    f.getOperator())
                ) {
                    return false;
                }
            } else if (filterName.equals(
                Application.LAST_MODIFIED_DATE_ATTRIBUTE)
            ){
                if (!match(app.getLastModifiedDate(), f.getNumericValue(),
                    f.getOperator())
                ) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean match(
        long value,
        long pattern,
        SearchFilter.Operator op
    ) {
        if (op.equals(SearchFilter.Operator.EQUAL_OPERATOR)) {
            return (value == pattern);
        }
        if (op.equals(SearchFilter.Operator.GREATER_THAN_OPERATOR)) {
            return (value > pattern);
        }
        if (op.equals(SearchFilter.Operator.LESSER_THAN_OPERATOR)) {
            return (value < pattern);
        }

        return false;
    }


    private static boolean match(String value, String strPattern) {
        if ((strPattern != null) && (strPattern.length() > 0)) {
            if ((value == null) || (value.trim().length() == 0)) {
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

            char lastChar = strPattern.charAt(strPattern.length()-1);
            if (lastChar == '*') {
                buff.append(".*");
            } else {
                buff.append(lastChar);
            }
            return Pattern.matches(buff.toString(), value);
        }

        return true;
    }



    /**
     * Returns the application names in a realm.
     *
     * @param adminSubject Admin Subject who has the rights to access
     *        configuration datastore.
     * @param realm Realm name.
     * @return application names in a realm.
     */
    public static Set<String> getApplicationNames(
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

    private static Set<Application> getAllApplication(String realm) 
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

            ReferredApplicationManager mgr =
                ReferredApplicationManager.getInstance();
            appls.addAll(mgr.getReferredApplications(realm));
            return appls;
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    private static Set<Application> getApplications(Subject adminSubject,
        String realm) throws EntitlementException {
        Set<Application> appls = getAllApplication(realm);

        if (adminSubject == PrivilegeManager.superAdminSubject) {
            return appls;
        }

        Set<Application> accessible = new HashSet<Application>();
        ApplicationPrivilegeManager apm =
            ApplicationPrivilegeManager.getInstance(realm, adminSubject);
        Set<String> accessibleApplicationNames =
            apm.getApplications(ApplicationPrivilege.Action.READ);

        for (Application app : appls) {
            String applicationName = app.getName();
            Application cloned = app.clone();

            if (accessibleApplicationNames.contains(applicationName)) {
                cloned.setResources(apm.getResources(applicationName,
                    ApplicationPrivilege.Action.READ));
                accessible.add(cloned);
            }
        }

        return accessible;
    }

    /**
     * Returns application.
     *
     * @param realm Realm name.
     * @param name Name of Application.
     * @return application.
     */
    public static Application getApplicationForEvaluation(
        String realm,
        String name
    ) throws EntitlementException {
        return getApplication(PrivilegeManager.superAdminSubject, realm,
            name);
    }

    /**
     * Returns application.
     *
     * @param adminSubject Admin Subject who has the rights to access
     *        configuration datastore.
     * @param realm Realm name.
     * @param name Name of Application.
     * @return application.
     */
    public static Application getApplication(
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

    /**
     * Removes application.
     *
     * @param adminSubject Admin Subject who has the rights to access
     *        configuration datastore.
     * @param realm Realm Name.
     * @param name Application Name.
     * @throws EntitlementException
     */
    public static void deleteApplication(
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
            throw new EntitlementException(326);
        }

        Application app = getApplication(adminSubject, realm, name);

        if (app != null) {
            if (!app.canBeDeleted()) {
                throw new EntitlementException(404);
            }
            EntitlementConfiguration ec = EntitlementConfiguration.getInstance(
                adminSubject, realm);
            ec.removeApplication(name);
            clearCache(realm);
        }
    }

    /**
     * Saves application data.
     *
     * @param adminSubject Admin Subject who has the rights to access
     *        configuration datastore.
     * @param realm Realm Name.
     * @param application Application object.
     */
    public static void saveApplication(
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

        validateApplication(adminSubject, realm, application);
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
                    if ((createdBy == null) || (createdBy.trim().length() == 0))
                    {
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

    private static String getApplicationCreatedBy(
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

    private static long getApplicationCreationDate(
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


    private static boolean isReferredApplication(
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

    private static boolean hasAccessToApplication(
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

    private static boolean hasAccessToApplication(
        ApplicationPrivilegeManager apm,
        Application application,
        ApplicationPrivilege.Action action) {
        Set<String> applNames = apm.getApplications(action);
        return applNames.contains(application.getName());
    }

    private static boolean isNewApplication(
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

    private static void validateApplication(
        Subject adminSubject,
        String realm,
        Application application
    ) throws EntitlementException {
        if (!realm.equals("/")) {
            String applTypeName = application.getApplicationType().getName();
            ResourceName comp = application.getResourceComparator();
            Set<String> referredRes = getReferredResources(
                adminSubject, realm, applTypeName);
            for (String r : application.getResources()) {
                validateApplication(application, comp, r, referredRes);
            }
        }
    }

    private static void validateApplication(
        Application application,
        ResourceName comp,
        String res,
        Set<String> referredRes) throws EntitlementException {
        for (String r : referredRes) {
            ResourceMatch match = comp.compare(res, r, true);
            if (match.equals(ResourceMatch.EXACT_MATCH) ||
                match.equals(ResourceMatch.WILDCARD_MATCH) ||
                match.equals(ResourceMatch.SUB_RESOURCE_MATCH)) {
                return;
            }
        }
        Object[] param = {application.getName()};
        throw new EntitlementException(247, param);
    }
    
    /**
     * Clears the cached applications. Must be called when notifications are
     * received for changes to applications.
     */
    public static void clearCache(String realm) {
        for (String name : applications.keySet()) {
            if (name.equalsIgnoreCase(realm)) {
                applications.remove(name);
                break;
            }
        }
    }

    /**
     * Refers resources to another realm.
     *
     * @param adminSubject Admin Subject who has the rights to access
     *        configuration datastore.
     * @param parentRealm Parent realm name.
     * @param referRealm Referred realm name.
     * @param applicationName Application name.
     * @param resources Referred resources.
     * @throws EntitlementException if resources cannot be referred.
     */
    public static void referApplication(
        Subject adminSubject,
        String parentRealm,
        String referRealm,
        String applicationName,
        Set<String> resources
    ) throws EntitlementException {
        boolean allowed = (adminSubject == PrivilegeManager.superAdminSubject);
        if (!allowed) {
            allowed = hasAccessToApplication(parentRealm, adminSubject,
                applicationName, ApplicationPrivilege.Action.MODIFY);
        }

        if (!allowed) {
            throw new EntitlementException(326);
        }

        Application appl = getApplication(PrivilegeManager.superAdminSubject,
            parentRealm, applicationName);
        if (appl == null) {
            Object[] params = {parentRealm, referRealm, applicationName};
            throw new EntitlementException(280, params);
        }

        ReferredApplicationManager.getInstance().clearCache(referRealm);
    }

    /**
     * Derefers resources from a realm.
     *
     * @param adminSubject Admin Subject who has the rights to access
     *        configuration datastore.
     * @param referRealm Referred realm name,
     * @param applicationName Application name.
     * @param resources Resources to be dereferred.
     * @throws EntitlementException if resources cannot be dereferred.
     */
    public static void dereferApplication(
        Subject adminSubject,
        String referRealm,
        String applicationName,
        Set<String> resources
    ) throws EntitlementException {
        boolean allowed = (adminSubject == PrivilegeManager.superAdminSubject);
        if (!allowed) {
            allowed = hasAccessToApplication(referRealm, adminSubject,
                applicationName, ApplicationPrivilege.Action.MODIFY);
        }

        if (!allowed) {
            throw new EntitlementException(326);
        }

        ReferredApplicationManager.getInstance().clearCache(referRealm);
    }

    /**
     * Returns referred resources for a realm.
     *
     * @param adminSubject Admin Subject who has the rights to access
     *        configuration datastore.
     * @param realm Realm name
     * @param applicationTypeName Application Type Name.
     * @return referred resources for a realm.
     * @throws EntitlementException if referred resources cannot be returned.
     */
    public static Set<String> getReferredResources(
        Subject adminSubject,
        String realm,
        String applicationTypeName
    ) throws EntitlementException {
        boolean allowed = (adminSubject == PrivilegeManager.superAdminSubject);
        if (!allowed) {
            allowed = hasAccessToApplication(realm, adminSubject,
                applicationTypeName, ApplicationPrivilege.Action.READ);
        }

        if (!allowed) {
            return Collections.EMPTY_SET;
        }

        PrivilegeIndexStore pis = PrivilegeIndexStore.getInstance(
            adminSubject, realm);
        return pis.getReferredResources(applicationTypeName);
    }

    /**
     * Creates an application.
     *
     * @param realm Realm name.
     * @param name Name of application.
     * @param applicationType application type.
     * @throws EntitlementException if application class is not found.
     */
    public static Application newApplication(
        String realm,
        String name,
        ApplicationType applicationType
    ) throws EntitlementException {
        Class clazz = applicationType.getApplicationClass();
        Class[] parameterTypes = {String.class, String.class,
            ApplicationType.class};
        Constructor constructor;
        try {
            constructor = clazz.getConstructor(parameterTypes);
            Object[] parameters = {realm, name, applicationType};
            return (Application) constructor.newInstance(parameters);
        } catch (NoSuchMethodException ex) {
            throw new EntitlementException(6, ex);
        } catch (SecurityException ex) {
            throw new EntitlementException(6, ex);
        } catch (InstantiationException ex) {
            throw new EntitlementException(6, ex);
        } catch (IllegalAccessException ex) {
            throw new EntitlementException(6, ex);
        } catch (IllegalArgumentException ex) {
            throw new EntitlementException(6, ex);
        } catch (InvocationTargetException ex) {
            throw new EntitlementException(6, ex);
        }
    }
}
