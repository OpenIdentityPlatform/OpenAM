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
 * $Id: Application.java,v 1.7 2010/01/08 22:20:47 veiming Exp $
 *
 * Portions copyright 2013-2016 ForgeRock AS.
 */
package com.sun.identity.entitlement;

import com.sun.identity.entitlement.interfaces.ISaveIndex;
import com.sun.identity.entitlement.interfaces.ISearchIndex;
import com.sun.identity.entitlement.interfaces.ResourceName;
import com.sun.identity.entitlement.util.SearchAttribute;

import org.forgerock.openam.entitlement.EntitlementRegistry;
import org.forgerock.openam.entitlement.PolicyConstants;
import org.forgerock.util.Reject;

import java.util.Date;
import java.util.HashSet;
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
     * Created by search attribute
     */
    public static final SearchAttribute CREATED_BY_SEARCH_ATTRIBUTE = new SearchAttribute(CREATED_BY_ATTRIBUTE, "ou");

    /**
     * Last modified by index key
     */
    public static final String LAST_MODIFIED_BY_ATTRIBUTE = "lastmodifiedby";

    /**
     * Last modified by search attribute
     */
    public static final SearchAttribute LAST_MODIFIED_BY_SEARCH_ATTRIBUTE = new SearchAttribute(LAST_MODIFIED_BY_ATTRIBUTE, "ou");

    /**
     * Creation date index key
     */
    public static final String CREATION_DATE_ATTRIBUTE = "creationdate";

    /**
     * Creation date search attribute
     */
    public static final SearchAttribute CREATION_DATE_SEARCH_ATTRIBUTE = new SearchAttribute(CREATION_DATE_ATTRIBUTE, "ou");

    /**
     * Last modified date index key
     */
    public static final String LAST_MODIFIED_DATE_ATTRIBUTE =
            "lastmodifieddate";

    /**
     * Last modified date search attribute
     */
    public static final SearchAttribute LAST_MODIFIED_DATE_SEARCH_ATTRIBUTE =
            new SearchAttribute(LAST_MODIFIED_DATE_ATTRIBUTE, "ou");

    /**
     * Name attribute name,
     */
    public static final String NAME_ATTRIBUTE = "name";

    /**
     * Name search attribute
     */
    public static final SearchAttribute NAME_SEARCH_ATTRIBUTE = new SearchAttribute(NAME_ATTRIBUTE, "ou");

    /**
     * Description attribute name,
     */
    public static final String DESCRIPTION_ATTRIBUTE = "description";

    /**
     * Description search attribute
     */
    public static final SearchAttribute DESCRIPTION_SEARCH_ATTRIBUTE = new SearchAttribute(DESCRIPTION_ATTRIBUTE, "ou");

    private static final int LEN_CREATED_BY_ATTRIBUTE =
        CREATED_BY_ATTRIBUTE.length();
    private static final int LEN_LAST_MODIFIED_BY_ATTRIBUTE =
        LAST_MODIFIED_BY_ATTRIBUTE.length();
    private static final int LEN_CREATION_DATE_ATTRIBUTE =
        CREATION_DATE_ATTRIBUTE.length();
    private static final int LEN_LAST_MODIFIED_DATE_ATTRIBUTE =
        LAST_MODIFIED_DATE_ATTRIBUTE.length();

    private String name;
    private String displayName;
    private String description;
    private ApplicationType applicationType;
    private Set<String> conditions;
    private Set<String> subjects;
    private final Set<String> resourceTypeUuids = new HashSet<String>();

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

    /**
     * Public, default constructor
     */
    public Application() {
    }

    /**
     * Constructs an instance.
     *
     * @param name Name of Application.
     * @param applicationType Its application type.
     */
    public Application(String name, ApplicationType applicationType) {
        this.name = name;
        this.applicationType = applicationType;
    }

    /**
     * Sets the application type of this Application
     *
     * @param applicationType The non-null application type
     */
    public void setApplicationType(ApplicationType applicationType) {
        Reject.ifNull(applicationType);
        this.applicationType = applicationType;
    }

    @Override
    public Application clone() {
        Application clone = new Application();
        cloneAppl(clone);
        return clone;
    }

    protected void cloneAppl(Application clone) {
        clone.name = name;
        clone.displayName = displayName;
        clone.description = description;
        clone.applicationType = applicationType;

        if (conditions != null) {
            clone.conditions = new HashSet<String>();
            clone.conditions.addAll(conditions);
        }

        if (subjects != null) {
            clone.subjects = new HashSet<String>();
            clone.subjects.addAll(subjects);
        }

        clone.addAllResourceTypeUuids(resourceTypeUuids);

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
                PolicyConstants.DEBUG.error(
                    "Application.getEntitlementCombiner", e);
            } catch (IllegalAccessException e) {
                PolicyConstants.DEBUG.error(
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
     * Returns application display name.
     *
     * @return application display name.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets supported condition class names.
     *
     * @param conditions Supported condition class names.
     */
    public void setConditions(Set<String> conditions) {

        if (conditions == null) {
            conditions = new HashSet<String>();
        }

        this.conditions = conditions;
    }

    /**
     * Sets supported subject class names.
     *
     * @param subjects Supported subject class names.
     */
    public void setSubjects(Set<String> subjects) {

        if (subjects == null) {
            subjects = new HashSet<String>();
        }

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
     * Sets entitlement combiner.
     *
     * @param entitlementCombiner entitlement combiner.
     */
    public void setEntitlementCombiner(Class entitlementCombiner) {
        this.entitlementCombiner = entitlementCombiner;
    }

    /**
     * Sets entitlement combiner using the {@link EntitlementRegistry} to look up
     * the appropriate class.
     *
     * @param entitlementCombiner name of the entitlement combiner to look up
     */
    public void setEntitlementCombinerName(Class<? extends EntitlementCombiner> entitlementCombiner) {
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
     * Retrieves the resource type UUIDs associated with the application.
     *
     * @return The set of associated resource type UUIDs.
     */
    public Set<String> getResourceTypeUuids() {
        return resourceTypeUuids;
    }

    /**
     * Adds the passed set of resource type UUIDs to the application.
     *
     * @param resourceTypeUuids The set of resource type UUIDs.
     */
    public void addAllResourceTypeUuids(final Set<String> resourceTypeUuids) {
        this.resourceTypeUuids.addAll(resourceTypeUuids);
    }

    /**
     * Adds the passed resource type UUID to the application.
     *
     * @param resourceTypeUuid The resource type UUID.
     */
    public void addResourceTypeUuid(final String resourceTypeUuid) {
        this.resourceTypeUuids.add(resourceTypeUuid);
    }

    /**
     * Removes the passed resource type UUID from the application.
     *
     * @param resourceTypeUuid The resource type UUID.
     */
    public void removeResourceTypeUuid(final String resourceTypeUuid) {
        this.resourceTypeUuids.remove(resourceTypeUuid);
    }

    /**
     * Returns search indexes for a given resource.
     *
     * @param resource Resource to generate the indexes.
     * @param realm Current realm to be searched.
     * @return Search indexes.
     * @throws EntitlementException When an error occurs in the entitlements framework.
     */
    public ResourceSearchIndexes getResourceSearchIndex(
            String resource, String realm) throws EntitlementException {
        return (searchIndex == null) ?
            applicationType.getResourceSearchIndex(resource, realm) :
            searchIndexInstance.getIndexes(resource, realm);
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
     * Returns resource comparator for this Application, defaulting to the ApplicationType's
     * resource comparator if none is directly associated with this Application.
     *
     * @return resource comparator, which may be null.
     */
    public ResourceName getResourceComparator() {
        return getResourceComparator(true);
    }

    /**
     * Returns resource comparator for this Application.
     *
     * @param defaultToAppType if <code>true</code>, will return this Application's ApplicationType
     *                         resource comparator if none is set on the Application.
     * @return resource comparator, which may be null.
     */
    public ResourceName getResourceComparator(boolean defaultToAppType) {
        if (resourceComparator == null) {
            if (defaultToAppType) {
                return applicationType.getResourceComparator();
            } else {
                return null;
            }
        } else {
            return resourceComparatorInstance;
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
                    PolicyConstants.DEBUG.error("Application.setMetaData", e);
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
                    PolicyConstants.DEBUG.error("Application.setMetaData", e);
                    Date date = new Date();
                    lastModifiedDate = date.getTime();
                }
            }
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the application display name.
     *
     * @param displayName The application display name.
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean canBeDeleted(String realm) {
        PrivilegeIndexStore pis = PrivilegeIndexStore.getInstance(
                PolicyConstants.SUPER_ADMIN_SUBJECT, realm);
        try {
            return !pis.hasPrivilgesWithApplication(realm, name);
        } catch (EntitlementException ex) {
            PolicyConstants.DEBUG.error("Application.canBeDeleted", ex);
            return false;
        }
    }

    /**
     * An Application is editable if it can be changed by an end user. This method was put
     * in place for subclasses that can not be edited.
     *
     * @return True if the Application can be edited.
     */
    public boolean isEditable() {
        return true;
    }
}
