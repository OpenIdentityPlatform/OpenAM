/*
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
 *
 * Portions Copyrighted 2013-2016 ForgeRock AS.
 */
package com.sun.identity.entitlement;

import static org.forgerock.openam.entitlement.PolicyConstants.SUPER_ADMIN_SUBJECT;

import java.util.Set;

import javax.security.auth.Subject;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.entitlement.service.ApplicationService;
import org.forgerock.openam.entitlement.service.ApplicationServiceFactory;
import org.forgerock.util.query.QueryFilter;

/**
 * Application Manager handles addition, deletion and listing of applications for each realm.
 */
public final class ApplicationManager {

    private ApplicationManager() {
    }

    private static ApplicationService getApplicationService(Subject subject, String realm) {
        return InjectorHolder.getInstance(ApplicationServiceFactory.class).create(subject, realm);
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
     * Returns the application names in a realm.
     *
     * @param adminSubject Admin Subject who has the rights to access configuration datastore.
     * @param realm Realm name.
     * @return application names in a realm.
     * @throws EntitlementException if problem occur during the operation.
     */
    public static Set<String> getApplicationNames(Subject adminSubject, String realm) throws EntitlementException {
        return getApplicationService(adminSubject, realm).getApplicationNames();
    }

    /**
     * Returns the applications in a realm.
     *
     * @param adminSubject Admin Subject who has the rights to access configuration datastore.
     * @param realm Realm name.
     * @return the applications in the specified realm.
     * @throws EntitlementException if problem occur during the operation.
     */
    public static Set<Application> getApplications(Subject adminSubject, String realm) throws EntitlementException {
        return getApplicationService(adminSubject, realm).getApplications();
    }

    /**
     * Returns application.
     *
     * @param realm Realm name.
     * @param name Name of Application.
     * @return application.
     * @throws EntitlementException if problem occur during the operation.
     */
    public static Application getApplicationForEvaluation(String realm, String name) throws EntitlementException {
        return getApplication(SUPER_ADMIN_SUBJECT, realm, name);
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
     * Clears the cached applications. Must be called when notifications are
     * received for changes to applications.
     *
     * @param realm Realm Name.
     */
    public static void clearCache(String realm) {
        getApplicationService(SUPER_ADMIN_SUBJECT, realm).clearCache();
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
