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
* Copyright 2014-2016 ForgeRock AS.
*/
package org.forgerock.openam.entitlement.rest.wrappers;

import java.util.Set;

import javax.security.auth.Subject;

import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationManager;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.util.SearchFilter;

/**
 * Simple wrapper for the ApplicationManager class.
 */
public class ApplicationManagerWrapper {

    /**
     * Wrapper for the static method
     * {@link ApplicationManager#saveApplication(Subject, String, Application)}.
     *
     * @param adminSubject An admin-level {@link Subject}.
     * @param realm The realm in which to save the {@link Application}
     * @param application The {@link Application} to save
     * @throws EntitlementException If there was an issue saving the application
     *
     * @return The saved application, which will include any modifications to its fields.
     */
    public Application saveApplication(Subject adminSubject, String realm, Application application) throws
            EntitlementException {
        return ApplicationManager.saveApplication(adminSubject, realm, application);
    }

    /**
     * Wrapper for the static method
     * {@link ApplicationManager#deleteApplication(javax.security.auth.Subject, String, String)}.
     *
     * @param adminSubject An admin-level {@link Subject}.
     * @param realm The realm in which to save the {@link Application}
     * @param name The name of the {@link Application}
     * @throws EntitlementException If there was an issue deleting the application
     */
    public void deleteApplication(Subject adminSubject, String realm, String name)
            throws EntitlementException {
        ApplicationManager.deleteApplication(adminSubject, realm, name);
    }

    /**
     * Wrapper for the static method
     * {@link ApplicationManager#getApplication(javax.security.auth.Subject, String, String)}.
     *
     * @param adminSubject An admin-level {@link Subject}.
     * @param realm The realm in which the {@link Application} exists.
     * @param name The name of the {@link Application}
     * @return the Application if found without issue, null otherwise
     * @throws EntitlementException if there were problems retrieving the application
     */
    public Application getApplication(Subject adminSubject, String realm, String name) throws EntitlementException {
        return ApplicationManager.getApplication(adminSubject, realm, name);
    }

    /**
     * Wrapper for the static method
     * {@link ApplicationManager#getApplicationNames(javax.security.auth.Subject, String)}.
     *
     * @param adminSubject An admin-level {@link Subject}.
     * @param realm The realm from which to gather the {@link Application} names.
     * @return a set of names of applications within the given realm
     * @throws EntitlementException if there were problems retrieving the names
     */
    public Set<String> getApplicationNames(Subject adminSubject, String realm)
            throws EntitlementException {
        return ApplicationManager.getApplicationNames(adminSubject, realm);
    }

    /**
     * Wrapper for the static method
     * {@link ApplicationManager#search(Subject, String, Set)}.
     *
     * @param subject The subject authorizing the update - will be validated for permission.
     * @param realm The realm from which to gather the {@link Application} names.
     * @param searchFilters The constraints that must match Application attribute values.
     * @return the names of those Applications that match the filter.
     * @throws EntitlementException if there were problems retrieving the names
     * @since 12.0.0
     */
    public Set<String> search(Subject subject, String realm, Set<SearchFilter> searchFilters)
            throws EntitlementException {
        return ApplicationManager.search(subject, realm, searchFilters);
    }

    /**
     * Clears the cached applications.
     *
     * @param realm
     *         realm to clear
     */
    public void clearCache(String realm) {
        ApplicationManager.clearCache(realm);
    }

}
