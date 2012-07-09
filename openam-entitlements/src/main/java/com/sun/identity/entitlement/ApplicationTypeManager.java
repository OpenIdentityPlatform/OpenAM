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
 * $Id: ApplicationTypeManager.java,v 1.1 2009/08/19 05:40:32 veiming Exp $
 */

package com.sun.identity.entitlement;

import com.sun.identity.entitlement.interfaces.ISaveIndex;
import com.sun.identity.entitlement.interfaces.ISearchIndex;
import com.sun.identity.entitlement.interfaces.ResourceName;
import java.util.HashSet;
import java.util.Set;
import javax.security.auth.Subject;

/**
 * Application Type manager.
 */
public final class ApplicationTypeManager {
    public static final String URL_APPLICATION_TYPE_NAME =
        "iPlanetAMWebAgentService";
    public static final String DELEGATION_APPLICATION_TYPE_NAME =
        "sunAMDelegationService";
    public static final String WEB_SERVICE_APPLICATION_TYPE_NAME =
        "webservices";
    
    /**
     * Returns application type names.
     *
     * @param adminSubject Admin Subject who has the rights to access
     *        configuration datastore.
     * @return application type names.
     */
    public static Set<String> getApplicationTypeNames(Subject adminSubject) {
        Set<String> names = new HashSet<String>();
        EntitlementConfiguration ec = EntitlementConfiguration.getInstance(
            adminSubject, "/");
        Set<ApplicationType> applications = ec.getApplicationTypes();
        for (ApplicationType a : applications) {
            names.add(a.getName());
        }
        return names;
    }

    /**
     * Returns application type.
     *
     * @param adminSubject Admin Subject who has the rights to access
     *        configuration datastore.
     * @param name Name of application type.
     * @return application type.
     */
    public static ApplicationType getAppplicationType(
        Subject adminSubject,
        String name
    ) {
        EntitlementConfiguration ec = EntitlementConfiguration.getInstance(
            adminSubject, "/");
        Set<ApplicationType> applications = ec.getApplicationTypes();

        for (ApplicationType a : applications) {
            if (a.getName().equals(name)) {
                return a;
            }
        }
        return null;
    }

    /**
     * Removes application type.
     *
     * @param adminSubject Admin Subject who has the rights to access
     *        configuration datastore.
     * @param name Name of application type.
     * @throws EntitlementException if application type cannot be removed.
     */
    public static void removeApplicationType(
        Subject adminSubject,
        String name
    ) throws EntitlementException {
        EntitlementConfiguration ec = EntitlementConfiguration.getInstance(
            adminSubject, "/");
        ec.removeApplicationType(name);
    }

    /**
     * Stores application type.
     *
     * @param adminSubject Admin Subject who has the rights to access
     *        configuration datastore.
     * @param appType Application type.
     */
    public static void saveApplicationType(
        Subject adminSubject,
        ApplicationType appType
    ) throws EntitlementException {
        EntitlementConfiguration ec = EntitlementConfiguration.getInstance(
            adminSubject, "/");
        ec.storeApplicationType(appType);
    }

    /**
     * Returns search index class.
     *
     * @param className Search index implementation class name.
     * @return search index class.
     */
    public static Class getSearchIndex(String className) {
        if (className == null) {
            return null;
        }
        try {
            Class clazz = Class.forName(className);
            Object o = clazz.newInstance();
            if (o instanceof ISearchIndex) {
                return clazz;
            }
        } catch (InstantiationException ex) {
            PrivilegeManager.debug.error(
                "ApplicationTypeManager.getSearchIndex", ex);
        } catch (IllegalAccessException ex) {
            PrivilegeManager.debug.error(
                "ApplicationTypeManager.getSearchIndex", ex);
        } catch (ClassNotFoundException ex) {
            PrivilegeManager.debug.error(
                "ApplicationTypeManager.getSearchIndex", ex);
        }
        return null;
    }

    /**
     * Returns save index class.
     *
     * @param className Save index implementation class name.
     * @return saveindex class.
     */
    public static Class getSaveIndex(String className) {
        if (className == null) {
            return null;
        }
        try {
            Class clazz = Class.forName(className);
            Object o = clazz.newInstance();
            if (o instanceof ISaveIndex) {
                return clazz;
            }
        } catch (InstantiationException ex) {
            PrivilegeManager.debug.error(
                "ApplicationTypeManager.getSaveIndex", ex);
        } catch (IllegalAccessException ex) {
            PrivilegeManager.debug.error(
                "ApplicationTypeManager.getSaveIndex", ex);
        } catch (ClassNotFoundException ex) {
            PrivilegeManager.debug.error(
                "ApplicationTypeManager.getSaveIndex", ex);
        }
        return null;
    }

    /**
     * Returns resource comparator implementation class.
     *
     * @param className Resource comparator implementation class name.
     * @return resource comparator implementation class.
     */
    public static Class getResourceComparator(String className) {
        if (className == null) {
            return null;
        }
        try {
            Class clazz = Class.forName(className);
            Object o = clazz.newInstance();
            if (o instanceof ResourceName) {
                return clazz;
            }
        } catch (InstantiationException ex) {
            PrivilegeManager.debug.error(
                "ApplicationTypeManager.getResourceComparator", ex);
        } catch (IllegalAccessException ex) {
            PrivilegeManager.debug.error(
                "ApplicationTypeManager.getResourceComparator", ex);
        } catch (ClassNotFoundException ex) {
            PrivilegeManager.debug.error(
                "ApplicationTypeManager.getResourceComparator", ex);
        }
        return null;
    }
}
