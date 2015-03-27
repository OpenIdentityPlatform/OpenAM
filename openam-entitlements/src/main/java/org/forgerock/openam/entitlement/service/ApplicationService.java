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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.entitlement.service;

import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.util.SearchFilter;

import javax.security.auth.Subject;
import java.util.Set;

/**
 * Application service is responsible for the management of applications.
 *
 * @since 13.0.0
 */
public interface ApplicationService {

    /**
     * Returns the application names in a realm.
     * <p/>
     * When performing the search using the Subject {@link PrivilegeManager#superAdminSubject},
     * the provided filters must not contain {@link SearchFilter.Operator#LESS_THAN_OR_EQUAL_OPERATOR }
     * or  {@link SearchFilter.Operator#GREATER_THAN_OR_EQUAL_OPERATOR } as these are not supported by LDAP.
     *
     * @param adminSubject
     *         Admin Subject who has the rights to access configuration datastore.
     * @param realm
     *         Realm name.
     * @param filters
     *         Search Filters
     *
     * @return application names in a realm.
     */
    Set<String> search(Subject adminSubject, String realm, Set<SearchFilter> filters) throws EntitlementException;

    /**
     * Returns the application names in a realm.
     *
     * @param adminSubject
     *         Admin Subject who has the rights to access
     *         configuration datastore.
     * @param realm
     *         Realm name.
     *
     * @return application names in a realm.
     */
    Set<String> getApplicationNames(Subject adminSubject, String realm) throws EntitlementException;

    /**
     * Returns application.
     *
     * @param realm
     *         Realm name.
     * @param name
     *         Name of Application.
     *
     * @return application.
     */
    Application getApplicationForEvaluation(String realm, String name) throws EntitlementException;

    /**
     * Returns application.
     *
     * @param adminSubject
     *         Admin Subject who has the rights to access
     *         configuration datastore.
     * @param realm
     *         Realm name.
     * @param name
     *         Name of Application.
     *
     * @return application.
     */
    Application getApplication(Subject adminSubject, String realm, String name) throws EntitlementException;

    /**
     * Removes application.
     *
     * @param adminSubject
     *         Admin Subject who has the rights to access
     *         configuration datastore.
     * @param realm
     *         Realm Name.
     * @param name
     *         Application Name.
     *
     * @throws EntitlementException
     */
    void deleteApplication(Subject adminSubject, String realm, String name) throws EntitlementException;

    /**
     * Saves application data.
     *
     * @param adminSubject
     *         Admin Subject who has the rights to access
     *         configuration datastore.
     * @param realm
     *         Realm Name.
     * @param application
     *         Application object.
     */
    void saveApplication(Subject adminSubject, String realm, Application application) throws EntitlementException;

    /**
     * Clears the cached applications. Must be called when notifications are
     * received for changes to applications.
     */
    void clearCache(String realm);

    /**
     * Returns referred resources for a realm.
     *
     * @param adminSubject
     *         Admin Subject who has the rights to access
     *         configuration datastore.
     * @param realm
     *         Realm name
     * @param applicationTypeName
     *         Application Type Name.
     *
     * @return referred resources for a realm.
     *
     * @throws EntitlementException
     *         if referred resources cannot be returned.
     */
    Set<String> getReferredResources(Subject adminSubject, String realm, String applicationTypeName)
            throws EntitlementException;

    /**
     * Replaces an existing application with a newer version of itself.
     *
     * @param oldApplication
     *         The application to update
     * @param newApplication
     *         The updated version of the application
     *
     * @retun the new application
     */
    void updateApplication(Application oldApplication, Application newApplication, Subject subject, String realm)
            throws EntitlementException;

}
