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
 * $Id: ApplicationPrivilege.java,v 1.3 2009/11/19 00:08:51 veiming Exp $
 */
package com.sun.identity.entitlement;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class representing delegation application privilege
 */
public class ApplicationPrivilege {
    public enum PossibleAction
        {READ, READ_MODIFY, READ_MODIFY_DELEGATE, READ_DELEGATE};
    public enum Action {READ, MODIFY, DELEGATE};

    private String name;
    private String description;
    private Map<String, Set<String>> applicationResources =
        new HashMap<String, Set<String>>();
    private PossibleAction actions;
    private Set<SubjectImplementation> subjects;
    private EntitlementCondition eCondition;

    private String createdBy;
    private String lastModifiedBy;
    private long creationDate;
    private long lastModifiedDate;

    public ApplicationPrivilege(String name) {
        this.name = name;
    }

    /**
     * Sets entitlement subject.
     *
     * @param entitlementSubjects Entitlement subject
     * @throws EntitlementException if subject is null.
     */
    public void setSubject(Set<SubjectImplementation> entitlementSubjects)
        throws EntitlementException {
        subjects = new HashSet<SubjectImplementation>();

        if (entitlementSubjects == null) {
            throw new EntitlementException(327);
        }
        subjects.addAll(entitlementSubjects);
    }

    /**
     * Returns the name of the privilege.
     *
     * @return name of the privilege.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the description of the privilege.
     *
     * @param desc Description of the privilege.
     */
    public void setDescription(String desc) {
        description = desc;
    }

    /**
     * Returns the description of the privilege.
     * 
     * @return description of the privilege.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the eSubject the privilege
     * @return eSubject of the privilege.
     */
    public Set<SubjectImplementation> getSubjects() {
        return subjects;
    }

    /**
     * Returns the condition the privilege
     * @return condition of the privilege.
     */
    public EntitlementCondition getCondition() {
        return eCondition;
    }

    /**
     * Sets the condition the privilege
     * @param condition condition of the privilege.
     */
    public void setCondition(EntitlementCondition condition) {
        eCondition = condition;
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
     * Returns last modified date.
     *
     * @return last modified date.
     */
    public long getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * Sets creation date.
     *
     * @param date creation date.
     */
    public void setCreationDate(long date) {
        creationDate = date;
    }

    /**
     * Sets last modified date.
     *
     * @param date last modified date.
     */
    public void setLastModifiedDate(long date) {
        lastModifiedDate = date;
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
     * Sets the user ID who created the policy.
     *
     * @param createdBy user ID who created the policy.
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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
     * Returns the user ID who created the policy.
     *
     * @return user ID who created the policy.
     */
    public String getCreatedBy() {
        return createdBy;
    }

    public Set<String> getApplicationNames() {
        return applicationResources.keySet();
    }

    public Set<String> getResourceNames(String applicationName) {
        return applicationResources.get(applicationName);
    }

    public PossibleAction getActionValues() {
        return actions;
    }

    public void addApplicationResource(
        String applicationName,
        Set<String> resources) {
        Set<String> res = applicationResources.get(applicationName);
        if (res == null) {
            res = new HashSet<String>();
            applicationResources.put(applicationName, res);
        }
        res.addAll(resources);
    }

    public void setApplicationResources(Map<String, Set<String>> map) {
        applicationResources = map;
    }

    public void setActionValues(PossibleAction actions) {
        this.actions = actions;
    }
}

