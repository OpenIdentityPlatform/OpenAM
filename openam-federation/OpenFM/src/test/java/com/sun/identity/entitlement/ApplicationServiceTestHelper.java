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
 * Copyright 2016 ForgeRock AS.
 */
package com.sun.identity.entitlement;

import java.util.Set;

import javax.security.auth.Subject;

import org.forgerock.openam.entitlement.service.ApplicationService;
import org.forgerock.openam.entitlement.utils.EntitlementUtils;
import org.forgerock.util.query.QueryFilter;

/**
 * @since 13.1.0
 */
public final class ApplicationServiceTestHelper {

    private ApplicationServiceTestHelper() {
    }

    private static ApplicationService getApplicationService(Subject subject, String realm) {
        return EntitlementUtils.getApplicationService(subject, realm);
    }

    /**
     * Returns the applications in a realm.
     *
     * @param subject Subject with rights to access configuration datastore.
     * @param realm Realm name.
     * @param queryFilter Query Filter
     * @return applications in a realm satisfied by the query.
     * @throws EntitlementException if problem occur during the operation.
     */
    public static Set<Application> search(Subject subject, String realm, QueryFilter<String> queryFilter)
            throws EntitlementException {
        return getApplicationService(subject, realm).search(queryFilter);
    }

    /**
     * Returns the requested application.
     *
     * @param adminSubject Admin Subject who has the rights to access configuration datastore.
     * @param realm Realm name.
     * @param name Name of Application.
     * @return the application.
     * @throws EntitlementException if problem occur during the operation.
     */
    public static Application getApplication(Subject adminSubject, String realm, String name)
            throws EntitlementException {
        return getApplicationService(adminSubject, realm).getApplication(name);
    }

    /**
     * Removes application.
     *
     * @param adminSubject Admin Subject who has the rights to access configuration datastore.
     * @param realm Realm Name.
     * @param name Application Name.
     * @throws EntitlementException if problem occur during the operation.
     */
    public static void deleteApplication(Subject adminSubject, String realm, String name) throws EntitlementException {
        getApplicationService(adminSubject, realm).deleteApplication(name);
    }

    /**
     * Saves application data.
     *
     * @param adminSubject Admin Subject who has the rights to access configuration datastore.
     * @param realm Realm Name.
     * @param application Application object.
     * @return The saved application, which will include any modifications to its fields.
     * @throws EntitlementException if problem occur during the operation.
     */
    public static Application saveApplication(Subject adminSubject, String realm, Application application)
            throws EntitlementException {
        return getApplicationService(adminSubject, realm).saveApplication(application);
    }

    /**
     * Returns referred resources for a realm.
     *
     * @param adminSubject Admin Subject who has the rights to access configuration datastore.
     * @param realm Realm name
     * @param applicationTypeName Application Type Name.
     * @return referred resources for a realm.
     * @throws EntitlementException if referred resources cannot be returned.
     */
    public static Set<String> getReferredResources(Subject adminSubject, String realm, String applicationTypeName)
            throws EntitlementException {
        return getApplicationService(adminSubject, realm).getReferredResources(applicationTypeName);
    }
}
