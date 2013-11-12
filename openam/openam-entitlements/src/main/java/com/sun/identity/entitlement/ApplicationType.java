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
 * $Id: ApplicationType.java,v 1.1 2009/08/19 05:40:32 veiming Exp $
 *
 * Portions copyright 2013 ForgeRock, Inc.
 */
package com.sun.identity.entitlement;

import com.sun.identity.entitlement.interfaces.ISaveIndex;
import com.sun.identity.entitlement.interfaces.ISearchIndex;
import com.sun.identity.entitlement.interfaces.ResourceName;
import com.sun.identity.entitlement.util.ResourceNameIndexGenerator;
import com.sun.identity.entitlement.util.ResourceNameSplitter;
import java.util.Map;

/**
 * Application Type defines the default supported action names; search and save
 * index generators; and resource comparator.
 */
public final class ApplicationType {
    private String name;
    private Map<String, Boolean> actions;
    private ResourceName resourceCompInstance;
    private ISaveIndex saveIndexInstance;
    private ISearchIndex searchIndexInstance;
    private String applicationClassName;

    /**
     * Constructs an instance.
     *
     * @param name Name of application type;
     * @param actions Supported action names.
     * @param searchIndex Search index generator.
     * @param saveIndex Save index generator.
     * @param resourceComp Resource comparator.
     */
    public ApplicationType(
        String name,
        Map<String, Boolean> actions,
        Class searchIndex,
        Class saveIndex,
        Class resourceComp
    ) throws InstantiationException, IllegalAccessException {
        this.name = name;
        this.actions = actions;

        setSearchIndex(searchIndex);
        setSaveIndex(saveIndex);

        Class resourceCompClass = (resourceComp == null) ?
            URLResourceName.class : resourceComp;
        resourceCompInstance = (ResourceName)resourceCompClass.newInstance();
    }

    /**
     * Sets application class name.
     *
     * @param applicationClassName Application class name.
     */
    public void setApplicationClassName(String applicationClassName) {
        this.applicationClassName = applicationClassName;
    }

    /**
     * Returns application class name.
     *
     * @return application class name.
     */
    public String getApplicationClassName() {
        return applicationClassName;
    }

    /**
     * Returns application class
     *
     * @return application class
     */
    public Class getApplicationClass()
        throws EntitlementException {
        if ((applicationClassName == null) ||
            (applicationClassName.length() == 0)) {
            return Application.class;
        }
        try {
            return Class.forName(applicationClassName);
        } catch (ClassNotFoundException ex) {
            throw new EntitlementException(6, ex);
        }
    }

    /**
     * Returns application name.
     *
     * @return application name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns supported action names and its default values.
     *
     * @return supported action names and its default values.
     */
    public Map<String, Boolean> getActions() {
        return actions;
    }

    /**
     * Sets supported action names and its default values.
     *
     * @param actions supported action names and its default values.
     */
    public void setActions(Map<String, Boolean> actions) {
        this.actions = actions;
    }

    /**
     * Set save index generator.
     *
     * @param saveIndex save index generator.
     */
    public void setSaveIndex(Class saveIndex) throws InstantiationException,
        IllegalAccessException {
        Class saveIndexClass = (saveIndex == null) ?
            ResourceNameIndexGenerator.class : saveIndex;
        saveIndexInstance = (ISaveIndex)saveIndexClass.newInstance();
    }

    /**
     * Set search index generator.
     *
     * @param searchIndex search index generator.
     */
    public void setSearchIndex(Class searchIndex) throws InstantiationException,
        IllegalAccessException {
        Class searchIndexClass = (searchIndex == null) ?
            ResourceNameSplitter.class : searchIndex;
        searchIndexInstance = (ISearchIndex)searchIndexClass.newInstance();
    }

    /**
     * Returns search indexes for a give resource name.
     *
     * @param resource Resource for generating the indexes.
     * @param realm Current realm to be searched.
     * @return search indexes for a give resource name.
     * @throws EntitlementException When an error occurs in the entitlements framework.
     */
    public ResourceSearchIndexes getResourceSearchIndex(String resource, String realm) throws EntitlementException {
        return searchIndexInstance.getIndexes(resource, realm);
    }

    /**
     * Returns save indexes for a give resource name.
     * 
     * @param resource Resource for generating the indexes.
     * @return save indexes for a give resource name.
     */
    public ResourceSaveIndexes getResourceSaveIndex(String resource) {
        return saveIndexInstance.getIndexes(resource);
    }

    /**
     * Returns resource comparator.
     * 
     * @return resource comparator.
     */
    public ResourceName getResourceComparator() {
        return resourceCompInstance;
    }

    /**
     * Returns save index.
     *
     * @return save index.
     */
    public ISaveIndex getSaveIndex() {
        return saveIndexInstance;
    }

    /**
     * Returns search index.
     * 
     * @return search index.
     */
    public ISearchIndex getSearchIndex() {
        return searchIndexInstance;
    }
}
