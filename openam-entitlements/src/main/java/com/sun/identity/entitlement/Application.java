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
 * $Id: Application.java,v 1.7 2010/01/08 22:20:47 veiming Exp $
 */

package com.sun.identity.entitlement;

import com.sun.identity.entitlement.interfaces.ISaveIndex;
import com.sun.identity.entitlement.interfaces.ISearchIndex;
import com.sun.identity.entitlement.interfaces.ResourceName;
import java.util.Date;
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
    /**
     * Created by index key
     */
    public static final String CREATED_BY_ATTRIBUTE = "createdby";

    /**
     * Last modified by index key
     */
    public static final String LAST_MODIFIED_BY_ATTRIBUTE = "lastmodifiedby";

    /**
     * Creation date index key
     */
    public static final String CREATION_DATE_ATTRIBUTE = "creationdate";

    /**
     * Last modified date index key
     */
    public static final String LAST_MODIFIED_DATE_ATTRIBUTE =
        "lastmodifieddate";

    /**
     * Name search attribute name,
     */
    public static final String NAME_ATTRIBUTE = "name";

    /**
     * Description search attribute name,
     */
    public static final String DESCRIPTION_ATTRIBUTE = "description";

    private static final int LEN_CREATED_BY_ATTRIBUTE =
        CREATED_BY_ATTRIBUTE.length();
    private static final int LEN_LAST_MODIFIED_BY_ATTRIBUTE =
        LAST_MODIFIED_BY_ATTRIBUTE.length();
    private static final int LEN_CREATION_DATE_ATTRIBUTE =
        CREATION_DATE_ATTRIBUTE.length();
    private static final int LEN_LAST_MODIFIED_DATE_ATTRIBUTE =
        LAST_MODIFIED_DATE_ATTRIBUTE.length();

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
    private String createdBy;
    private String lastModifiedBy;
    private long creationDate = -1;
    private long lastModifiedDate = -1;

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
        clone.description = description;
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

        clone.createdBy = createdBy;
        clone.creationDate = creationDate;
        clone.lastModifiedBy = lastModifiedBy;
        clone.lastModifiedDate = lastModifiedDate;
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
     * @param subjects Supported subject class names.
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
     * Adds attribute names.
     *
     * @param names Attribute names.
     */
    public void addAttributeNames(Set<String> names) {
        if (names != null) {
            if (attributeNames == null) {
                attributeNames = new HashSet<String>();
            }
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

        try {
            String res = resComp.canonicalize(resource);

            if ((resources != null) && !resources.isEmpty()) {
                if (resComp instanceof RegExResourceName) {
                    for (String r : resources) {
                        ResourceMatch rm = resComp.compare(r, res, true);
                        if (rm.equals(ResourceMatch.EXACT_MATCH) ||
                            rm.equals(ResourceMatch.WILDCARD_MATCH) ||
                            rm.equals(ResourceMatch.SUB_RESOURCE_MATCH)) {
                            match = true;
                            break;
                        }
                    }
                } else {
                    for (String r : resources) {
                        ResourceMatch rm = resComp.compare(resComp.canonicalize(
                            r),
                            res, false);
                        if (rm.equals(ResourceMatch.EXACT_MATCH) ||
                            rm.equals(ResourceMatch.SUB_RESOURCE_MATCH)) {
                            match = true;
                            break;
                        } else {
                            rm = resComp.compare(res, resComp.canonicalize(r),
                                true);
                            if (rm.equals(ResourceMatch.WILDCARD_MATCH)) {
                                match = true;
                                break;
                            }
                        }
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
     * @param description description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns creation date.
     *
     * @return creation date.
     */
    public long getCreationDate() {
        return creationDate;
    }

    /**
     * Sets the creation date.
     *
     * @param creationDate creation date.
     */
    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * Returns last modified date.
     *
     * @return last modified date.
     */
    public long getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * Sets the last modified date.
     *
     * @param lastModifiedDate last modified date.
     */
    public void setLastModifiedDate(long lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    /**
     * Returns the user ID who last modified the policy.
     *
     * @return user ID who last modified the policy.
     */
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * Sets the user ID who last modified the policy.
     *
     * @param lastModifiedBy user ID who last modified the policy.
     */
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    /**
     * Returns the user ID who created the policy.
     *
     * @return user ID who created the policy.
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the user ID who created the policy.
     *
     * @param createdBy user ID who created the policy.
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Returns meta information.
     *
     * @return meta information.
     */
    public Set<String> getMetaData() {
        Set<String> meta = new HashSet<String>(8);
        if (createdBy != null) {
            meta.add(CREATED_BY_ATTRIBUTE + "=" + createdBy);
        }
        if (creationDate != -1) {
            meta.add(CREATION_DATE_ATTRIBUTE + "=" +
                Long.toString(creationDate));
        }
        if (lastModifiedDate != -1) {
            meta.add(LAST_MODIFIED_DATE_ATTRIBUTE + "=" +
                Long.toString(lastModifiedDate));
        }
        if (lastModifiedBy != null) {
            meta.add(LAST_MODIFIED_BY_ATTRIBUTE + "=" + lastModifiedBy);
        }

        return meta;
    }

    public void setMetaData(Set<String> meta) {
        for (String m : meta) {
            if (m.startsWith(CREATED_BY_ATTRIBUTE + "=")) {
                createdBy = m.substring(LEN_CREATED_BY_ATTRIBUTE + 1);
            } else if (m.startsWith(CREATION_DATE_ATTRIBUTE + "=")) {
                String s = m.substring(LEN_CREATION_DATE_ATTRIBUTE + 1);
                try {
                    creationDate = Long.parseLong(s);
                } catch (NumberFormatException e) {
                    PrivilegeManager.debug.error("Application.setMetaData", e);
                    Date date = new Date();
                    creationDate = date.getTime();
                }
            } else if (m.startsWith(LAST_MODIFIED_BY_ATTRIBUTE + "=")) {
                lastModifiedBy = m.substring(LEN_LAST_MODIFIED_BY_ATTRIBUTE + 1);
            } else  if (m.startsWith(LAST_MODIFIED_DATE_ATTRIBUTE + "=")) {
                String s = m.substring(LEN_LAST_MODIFIED_DATE_ATTRIBUTE + 1);
                try {
                    lastModifiedDate = Long.parseLong(s);
                } catch (NumberFormatException e) {
                    PrivilegeManager.debug.error("Application.setMetaData", e);
                    Date date = new Date();
                    lastModifiedDate = date.getTime();
                }
            }
        }
    }

    protected void setRealm(String realm) {
        this.realm = realm;
    }

    public boolean canBeDeleted() {
        PrivilegeIndexStore pis = PrivilegeIndexStore.getInstance(
            PrivilegeManager.superAdminSubject, realm);
        try {
            return !pis.hasPrivilgesWithApplication(realm, name);
        } catch (EntitlementException ex) {
            PrivilegeManager.debug.error("Application.canBeDeleted", ex);
            return false;
        }
    }
}
