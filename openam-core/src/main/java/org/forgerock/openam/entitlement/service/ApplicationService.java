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
 */
package org.forgerock.openam.entitlement.service;

import java.util.Set;

import org.forgerock.util.query.QueryFilter;

import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.EntitlementException;

/**
 * Application service to handle all things relating to applications.
 *
 * @since 13.0.0
 */
public interface ApplicationService {

    /**
     * Retrieves an application instance for the passed name.
     *
     * @param applicationName
     *         the application name
     *
     * @return an application instance, null if the application doesn't exist
     *
     * @throws EntitlementException
     *         should some error occur during the application retrieval
     */
    Application getApplication(String applicationName) throws EntitlementException;


    /**
     * Returns the applications that matches the search criteria.
     *
     * @param queryFilter Query Filter
     * @return applications in a realm satisfied by the query.
     *
     * @throws EntitlementException
     *         should some error occur during the application retrieval
     */
    Set<Application> search(QueryFilter<String> queryFilter) throws EntitlementException;

    /**
     * Returns all the application names in a realm.
     *
     * @return application names in a realm.
     *
     * @throws EntitlementException
     *         should some error occur during the application retrieval
     */
    Set<String> getApplicationNames() throws EntitlementException;

    /**
     * Returns all the applications in a realm.
     *
     * @return applications in a realm.
     *
     * @throws EntitlementException
     *         should some error occur during the application retrieval
     */
    Set<Application> getApplications() throws EntitlementException;

    /**
     * Removes application in the realm.
     *
     * @param name Application Name.
     *
     * @throws EntitlementException
     *         should some error occur during the application retrieval
     */
    void deleteApplication(String name) throws EntitlementException;

    /**
     * Saves application data.
     *
     * @param application Application object.
     *
     * @return The saved application, which will include any modifications to its fields.
     *
     * @throws EntitlementException
     *         should some error occur during the application retrieval
     */
    Application saveApplication(Application application) throws EntitlementException;

    /**
     * Clears the cached applications. Must be called when notifications are received for changes to applications.
     */
    void clearCache();

    /**
     * Returns referred resources for a realm.
     *
     * @param applicationTypeName Application Type Name.
     * @return referred resources for a realm.
     *
     * @throws EntitlementException if referred resources cannot be returned.
     */
    Set<String> getReferredResources(String applicationTypeName) throws EntitlementException;
}
