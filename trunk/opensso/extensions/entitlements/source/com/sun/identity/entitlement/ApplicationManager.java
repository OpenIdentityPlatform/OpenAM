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
 * $Id: ApplicationManager.java,v 1.22 2009/08/14 22:46:18 veiming Exp $
 */
package com.sun.identity.entitlement;

import com.sun.identity.entitlement.interfaces.ResourceName;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

/**
 * Application Manager handles addition, deletion and listing of applications
 * for each realm.
 */
public final class ApplicationManager {
    private final static Object lock = new Object();
    private static Map<String, Set<Application>> applications =
        new HashMap<String, Set<Application>>();

    private ApplicationManager() {
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
    ) {
        Set<Application> appls = getApplications(adminSubject, realm);
        Set<String> results = new HashSet<String>();
        for (Application appl : appls) {
            results.add(appl.getName());
        }
        return results;
    }

    private static Set<Application> getApplications(Subject adminSubject,
        String realm) {
        Set<Application> appls = applications.get(realm);

        if (appls == null) {
            synchronized (lock) {
                appls = applications.get(realm);
                if (appls == null) {
                    EntitlementConfiguration ec =
                        EntitlementConfiguration.getInstance(
                        adminSubject, realm);
                    appls = ec.getApplications();
                    applications.put(realm, appls);
                }
            }
        }
        return appls;
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
    public static Application getApplicationForEvaluation(
        String realm,
        String name
    ) {
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
    ) {
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
        synchronized (lock) {
            applications.remove(realm);
        }
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
        EntitlementConfiguration ec = EntitlementConfiguration.getInstance(
            adminSubject, realm);
        ec.removeApplication(name);
        clearCache(realm);
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
        validateApplication(adminSubject, realm, application);
        EntitlementConfiguration ec = EntitlementConfiguration.getInstance(
            adminSubject, realm);
        ec.storeApplication(application);
        clearCache(realm);
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
    public static synchronized void clearCache(String realm) {
        applications.remove(realm);
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
        Application appl = getApplication(adminSubject, parentRealm,
            applicationName);
        if (appl == null) {
            Object[] params = {parentRealm, referRealm, applicationName};
            throw new EntitlementException(280, params);
        }

        Application clone = appl.refers(referRealm, resources);
        EntitlementConfiguration ec = EntitlementConfiguration.getInstance(
            adminSubject, referRealm);
        ec.storeApplication(clone);
        clearCache(referRealm);
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
        EntitlementConfiguration ec = EntitlementConfiguration.getInstance(
            adminSubject, referRealm);
        ec.removeApplication(applicationName, resources);
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
