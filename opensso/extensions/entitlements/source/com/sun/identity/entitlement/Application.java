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
 * $Id: Application.java,v 1.32 2009/08/14 22:46:18 veiming Exp $
 */

package com.sun.identity.entitlement;

import com.sun.identity.entitlement.interfaces.ISaveIndex;
import com.sun.identity.entitlement.interfaces.ISearchIndex;
import com.sun.identity.entitlement.interfaces.ResourceName;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Application class contains the information on how an application behaves
 * e.g. how to combine decision and how to compare resources;
 * and the supported actions.
 */
public class Application implements Cloneable {
    private String realm = "/";
    private String name;
    private String description;
    private ApplicationType applicationType;
    private Map<String, Boolean> actions = new HashMap<String, Boolean>();
    private Set<String> conditions;
    private Set<String> subjects;
    private Set<String> resources;
    private Class entitlementCombiner;
    private Class searchIndex;
    private Class saveIndex;
    private Class resourceComparator;
    private Set<String> attributeNames;

    private ResourceName resourceComparatorInstance;
    private ISaveIndex saveIndexInstance;
    private ISearchIndex searchIndexInstance;

    protected Application() {
    }

    /**
     * Constructs an instance.
     *
     * @param name Name of Application.
     * @param applicationType Its application type.
     */
    public Application(
        String realm,
        String name,
        ApplicationType applicationType
    ) {
        this.realm = realm;
        this.name = name;
        this.applicationType = applicationType;
        setActions(applicationType.getActions());
    }

    @Override
    public Application clone() {
        Application clone = new Application();
        cloneAppl(clone);
        return clone;
    }

    protected void cloneAppl(Application clone) {
        clone.name = name;
        clone.realm = realm;
        clone.applicationType = applicationType;

        if (actions != null) {
            clone.actions = new HashMap<String, Boolean>();
            clone.actions.putAll(actions);
        }

        if (conditions != null) {
            clone.conditions = new HashSet<String>();
            clone.conditions.addAll(conditions);
        }

        if (subjects != null) {
            clone.subjects = new HashSet<String>();
            clone.subjects.addAll(subjects);
        }

        if (resources != null) {
            clone.resources = new HashSet<String>();
            clone.resources.addAll(resources);
        }

        clone.entitlementCombiner = entitlementCombiner;
        clone.searchIndex = searchIndex;
        clone.searchIndexInstance = searchIndexInstance;
        clone.saveIndex = saveIndex;
        clone.saveIndexInstance = saveIndexInstance;
        clone.resourceComparator = resourceComparator;
        clone.resourceComparatorInstance = resourceComparatorInstance;

        if (attributeNames != null) {
            clone.attributeNames = new HashSet<String>();
            clone.attributeNames.addAll(attributeNames);
        }
    }

    /**
     * Returns a set of supported actions and its default value.
     *
     * @return set of supported actions and its default value.
     */
    public Map<String, Boolean> getActions() {
        Map<String, Boolean> results = new HashMap<String, Boolean>();
        if (actions != null) {
            results.putAll(actions);
        }
        return results;
    }

    /**
     * Returns application type.
     *
     * @return application type.
     */
    public ApplicationType getApplicationType() {
        return applicationType;
    }
    /**
     * Returns set of supported condition class names.
     *
     * @return set of supported condition class names.
     */
    public Set<String> getConditions() {
        return conditions;
    }

    /**
     * Returns set of supported subject class names.
     *
     * @return set of supported subject class names.
     */
    public Set<String> getSubjects() {
        return subjects;
    }

    /**
     * Returns entitlement combiner class.
     *
     * @return entitlement combiner class.
     */
    public Class getEntitlementCombinerClass() {
        return entitlementCombiner;
    }

    /**
     * Returns a new instance of entitlement combiner.
     *
     * @return an instance of entitlement combiner.
     */
    public EntitlementCombiner getEntitlementCombiner() {
        if (entitlementCombiner != null) {
            try {
                return (EntitlementCombiner) entitlementCombiner.newInstance();
            } catch (InstantiationException e) {
                PrivilegeManager.debug.error(
                    "Application.getEntitlementCombiner", e);
            } catch (IllegalAccessException e) {
                PrivilegeManager.debug.error(
                    "Application.getEntitlementCombiner", e);
            }
        }
        return null;
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
     * Sets supported action names and its default values.
     *
     * @param actions Set of supported action names and its default values.
     */
    public void setActions(Map<String, Boolean> actions) {
        this.actions.clear();
        this.actions.putAll(actions);
    }

    /**
     * Sets supported condition class names.
     *
     * @param conditions Supported condition class names.
     */
    public void setConditions(Set<String> conditions) {
        this.conditions = conditions;
    }

    /**
     * Sets supported subject class names.
     *
     * @param conditions Supported subject class names.
     */
    public void setSubjects(Set<String> subjects) {
        this.subjects = subjects;
    }

    /**
     * Sets save index.
     *
     * @param saveIndex save index.
     */
    public void setSaveIndex(Class saveIndex) throws InstantiationException,
        IllegalAccessException {
        this.saveIndex = saveIndex;
        if (saveIndex != null) {
            saveIndexInstance = (ISaveIndex) saveIndex.newInstance();
        } else {
            saveIndexInstance = null;
        }
    }

    /**
     * Sets search index generator.
     *
     * @param searchIndex search index generator.
     */
    public void setSearchIndex(Class searchIndex) throws InstantiationException,
        IllegalAccessException {
        this.searchIndex = searchIndex;
        if (searchIndex != null) {
            searchIndexInstance = (ISearchIndex) searchIndex.newInstance();
        } else {
            searchIndexInstance = null;
        }
    }

    /**
     * Sets resource names.
     *
     * @param resources resource names
     */
    public void setResources(Set<String> resources) {
        this.resources = new HashSet<String>();
        if (resources != null) {
            this.resources.addAll(resources);
        }
    }

    /**
     * Adds resource names.
     *
     * @param resources resource names to be added.
     */
    public void addResources(Set<String> resources) {
        if (this.resources == null) {
            this.resources = new HashSet<String>();
        }
        this.resources.addAll(resources);

    }

    /**
     * Removes resource names.
     *
     * @param resources resource names to be removed.
     */
    public void removeResources(Set<String> resources) {
        if (this.resources != null) {
            this.resources.removeAll(resources);
        }
    }

    /**
     * Sets entitlement combiner.
     *
     * @param entitlementCombiner entitlement combiner.
     */
    public void setEntitlementCombiner(Class entitlementCombiner) {
        this.entitlementCombiner = entitlementCombiner;
    }

    /**
     * Sets resource comparator.
     *
     * @param resourceComparator resource comparator.
     */
    public void setResourceComparator(Class resourceComparator) 
        throws InstantiationException, IllegalAccessException {
        this.resourceComparator = resourceComparator;
        if (resourceComparator != null) {
            resourceComparatorInstance = (ResourceName) resourceComparator.
                newInstance();
        } else {
            resourceComparatorInstance = null;
        }
    }

    /**
     * Returns set of resource names.
     *
     * @return set of resource names.
     */
    public Set<String> getResources() {
        return resources;
    }

    /**
     * Returns search indexes for a given resource.
     *
     * @param resource resource to generate the indexes.
     * @return search indexes.
     */
    public ResourceSearchIndexes getResourceSearchIndex(
            String resource) {
        return (searchIndex == null) ?
            applicationType.getResourceSearchIndex(resource) :
            searchIndexInstance.getIndexes(resource);
    }

    /**
     * Returns save indexes for a given resource.
     * 
     * @param resource resource to generate the indexes.
     * @return save indexes.
     */
    public ResourceSaveIndexes getResourceSaveIndex(String resource) {
        return (saveIndex == null) ?
            applicationType.getResourceSaveIndex(resource) :
            saveIndexInstance.getIndexes(resource);
    }

    /**
     * Returns save index class.
     *
     * @return save index class.
     */
    public Class getSaveIndexClass() {
        return saveIndex;
    }

    /**
     * Returns search index class.
     *
     * @return search index class.
     */
    public Class getSearchIndexClass() {
        return searchIndex;
    }

    /**
     * Returns resource comparator class.
     *
     * @return resource comparator class.
     */
    public Class getResourceComparatorClass() {
        return resourceComparator;
    }

    /**
     * Returns resource comparator.
     * 
     * @return resource comparator.
     */
    public ResourceName getResourceComparator() {
        return (resourceComparator == null) ?
            applicationType.getResourceComparator() : 
            resourceComparatorInstance;
    }

    /**
     * Adds a new action with its default value.
     *
     * @param name Action name.
     * @param val Default value.
     * @throws EntitlementException if action cannot be added.
     */
    public void addAction(String name, boolean val)
        throws EntitlementException {
        if (actions == null) {
            actions = new HashMap<String, Boolean>();
        }
        actions.put(name, val);
    }

    /**
     * Removes an action.
     *
     * @param name Action name.
     */
    public void removeAction(String name)
        throws EntitlementException {
        if (actions != null) {
            actions.remove(name);
        }
    }

    /**
     * Sets attribute names.
     *
     * @param names Attribute names.
     */
    public void setAttributeNames(Set<String> names) {
        attributeNames = new HashSet<String>();
        if (names != null) {
            attributeNames.addAll(names);
        }
    }

    /**
     * Returns save index
     *
     * @return save index
     */
    public ISaveIndex getSaveIndex() {
        return saveIndexInstance;
    }

    /**
     * Returns search index
     *
     * @return search index
     */
    public ISearchIndex getSearchIndex() {
        return searchIndexInstance;
    }

    /**
     * Return attribute names.
     *
     * @return attribute names.
     */
    public Set<String> getAttributeNames() {
        return attributeNames;
    }

    /**
     * Returns the results of validating resource name.
     *
     * @param resource Resource to be validated.
     * @return the results of validating resource name.
     */
    public ValidateResourceResult validateResourceName(String resource) {
        ResourceName resComp = getResourceComparator();
        boolean match = false;
        String res = null;
        try {
            res = resComp.canonicalize(resource);

            if ((resources != null) && !resources.isEmpty()) {
                for (String r : resources) {
                    ResourceMatch rm = resComp.compare(res, resComp.canonicalize(r),
                        true);
                    if (rm.equals(ResourceMatch.EXACT_MATCH) ||
                        rm.equals(ResourceMatch.SUB_RESOURCE_MATCH) ||
                        rm.equals(ResourceMatch.WILDCARD_MATCH)) {
                        match = true;
                        break;
                    }
                }
            }
        } catch (EntitlementException ex) {
            Object[] args = {resource};
            return new ValidateResourceResult(
                ValidateResourceResult.VALID_CODE_INVALID,
                "resource.validation.invalid.resource", args);
        }
        
        if (!match) {
            Object[] args = {resource};
            return new ValidateResourceResult(
                ValidateResourceResult.VALID_CODE_DOES_NOT_MATCH_VALID_RESOURCES,
                "resource.validation.does.not.match.valid.resources", args);
        }

        return new ValidateResourceResult(
            ValidateResourceResult.VALID_CODE_VALID, "");
    }

    public Application refers(String realm, Set<String> res) {
        Application clone = clone();
        clone.realm = realm;
        clone.resources.clear();
        clone.resources.addAll(res);
        return clone;
    }

    /**
     * Returns description.
     *
     * @return description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set description.
     *
     * @aparam description description.
     */
    public void setDescription(String description) {
        this.description = description;
    }
}
