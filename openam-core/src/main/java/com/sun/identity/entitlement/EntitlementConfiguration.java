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
 * $Id: EntitlementConfiguration.java,v 1.7 2010/01/08 23:59:31 veiming Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */

package com.sun.identity.entitlement;

import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.forgerock.util.query.QueryFilter;

/**
 * Entitlement Configuration
 */
public interface EntitlementConfiguration {

    String POLICY_EVAL_THREAD_SIZE = "evalThreadSize";
    String POLICY_SEARCH_THREAD_SIZE = "searchThreadSize";
    String POLICY_CACHE_SIZE = "policyCacheSize";
    String INDEX_CACHE_SIZE = "indexCacheSize";

    /**
     * Returns the application with the specified name.
     *
     * @return The application or null if the application could not be found.
     */
    Application getApplication(String name);

    /**
     * Returns a set of registered applications.
     *
     * @return a set of registered applications.
     */
    Set<Application> getApplications();

    /**
     * Removes application.
     *
     * @param name name of application to be removed.
     * @throws EntitlementException if application cannot be removed.
     */
    void removeApplication(String name) throws EntitlementException;

    /**
     * Stores the application to data store.
     *
     * @param application Application object.
     * @throws EntitlementException if application cannot be stored.
     */
    void storeApplication(Application application) throws EntitlementException;

    /**
     * Returns a set of registered application type.
     *
     * @return A set of registered application type.
     */
    Set<ApplicationType> getApplicationTypes();

    /**
     * Removes application type.
     *
     * @param name name of application type to be removed.
     * @throws EntitlementException  if application type cannot be removed.
     */
    void removeApplicationType(String name) throws EntitlementException;

    /**
     * Stores the application type to data store.
     *
     * @param applicationType Application type  object.
     * @throws EntitlementException if application type cannot be stored.
     */
    void storeApplicationType(ApplicationType applicationType) throws EntitlementException;

    /**
     * Returns set of attribute values of a given attribute name,
     *
     * @param attributeName attribute name.
     * @return set of attribute values of a given attribute name,
     */
    Set<String> getConfiguration(String attributeName);

    /**
     * Returns subject attribute names.
     *
     * @param application Application name.
     * @return subject attribute names.
     * @throws EntitlementException if subject attribute names cannot be
     * returned.
     */
    Set<String> getSubjectAttributeNames(String application) throws EntitlementException;

    /**
     * Adds subject attribute names.
     *
     * @param application Application name.
     * @param names Set of subject attribute names.
     * @throws EntitlementException if subject attribute names cannot be
     *         added.
     */
    void addSubjectAttributeNames(String application, Set<String> names) throws EntitlementException;

    /**
     * Returns subject attributes collector configuration.
     *
     * @param name subject attributes collector name
     * @return subject attributes collector configuration.
     * @throws EntitlementException if subject attributes collector
     * configuration cannot be returned.
     */
    Map<String, Set<String>> getSubjectAttributesCollectorConfiguration(String name) throws EntitlementException;

    /**
     * Sets subject attributes collector configuration.
     *
     * @param name subject attributes collector name
     * @param attrMap subject attributes collector configuration map.
     * @throws EntitlementException if subject attributes collector
     * configuration cannot be set.
     */
    void setSubjectAttributesCollectorConfiguration(String name, Map<String, Set<String>> attrMap)
            throws EntitlementException;

    /**
     * Returns <code>true</code> if OpenAM policy data is migrated to a
     * form that entitlements service can operates on them.
     *
     * @return <code>true</code> if OpenAM policy data is migrated to a
     * form that entitlements service can operates on them.
     */
    boolean hasEntitlementDITs();

    /**
     * Returns <code>true</code> if the system is migrated to support
     * entitlement services.
     *
     * @return <code>true</code> if the system is migrated to support
     * entitlement services.
     */
    boolean migratedToEntitlementService();
    
    /**
     * Returns <code>true</code> if the network monitoring for entitlements
     * is enabled
     *
     * @return <code>true</code> if the network monitoring for entitlements
     * is enabled.
     */
    boolean networkMonitorEnabled();
    
    /**
     * Allows the network monitoring to be enabled/disabled
     * 
     * @param enabled Is the network monitoring enabled
     */
    void setNetworkMonitorEnabled(boolean enabled);

    /**
     * Returns <code>true</code> if the system stores privileges in
     * XACML format and supports exporting privileges in XACML format
     *
     *
     * @return <code>true</code> if the system stores privileges in
     * XACML format and supports exporting privileges in XACML format
     */
    boolean xacmlPrivilegeEnabled();

    /**
     * Returns a set of applications for the given search criteria.
     *
     * @param subject Admin Subject
     * @param queryFilter Query filter.
     * @return a set of applications for the given search criteria.
     * @throws EntitlementException if search failed.
     */
    Set<Application> searchApplications(Subject subject, QueryFilter<String> queryFilter) throws EntitlementException;

    /**
     * Reindex Applications.
     */
    void reindexApplications();

    /**
     * For letting us know whether or not the Agent monitoring is enabled in core.
     *
     * @return true if monitoring is enabled, false otherwise
     */
    boolean isMonitoringRunning();

    /**
     * Informs us of the size of the policy window set in the configurable options.
     *
     * @return the value of the window size as configured.
     */
    int getPolicyWindowSize();
}
