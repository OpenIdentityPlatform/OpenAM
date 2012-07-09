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
 * $Id: SubjectAttributesManager.java,v 1.3 2009/09/24 22:37:43 hengming Exp $
 */

package com.sun.identity.entitlement;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.security.auth.Subject;

/**
 * Manages multiple instances of <class>SubjectAttributesCollector</class>,
 * and to be called by <class>Evaluator</class> and <class>
 * EntitlementSubject</class> implementations to obtain users' attributes and
 * memberships.
 */
public class SubjectAttributesManager {
    private String realmName;
    private SubjectAttributesCollector attrCollector;
    private static final String DEFAULT_SUBJECT_ATTRIBUTES_COLLECTOR_NAME =
        "OpenSSO";
    private static final String DEFAULT_IMPL =
        "com.sun.identity.entitlement.opensso.OpenSSOSubjectAttributesCollector";
    private static Map<String, SubjectAttributesManager> instances =
        new HashMap<String, SubjectAttributesManager>();
    private Subject adminSubject;

    private static ReadWriteLock instancesLock = new ReentrantReadWriteLock();

    private SubjectAttributesManager(Subject adminSubject, String realmName) {
        this.realmName = realmName;
        this.adminSubject = adminSubject;
        
        EntitlementConfiguration ec = EntitlementConfiguration.getInstance(
            adminSubject, realmName);

        Map<String, Set<String>> configMap = null;
        try {
            configMap = ec.getSubjectAttributesCollectorConfiguration(
            DEFAULT_SUBJECT_ATTRIBUTES_COLLECTOR_NAME);
        } catch (EntitlementException ex) {
            if (PrivilegeManager.debug.warningEnabled()) {
                PrivilegeManager.debug.warning(
                    "SubjectAttributesManager.<init>", ex);
            }
        }

        String implClass = null;
        if (configMap != null) {
            Set<String> tmpSet = configMap.get("class");
            if ((tmpSet != null) && (!tmpSet.isEmpty())) {
                implClass = tmpSet.iterator().next();
            }
        }

        if (implClass == null) {
            implClass = DEFAULT_IMPL;
        }

        try {
            attrCollector = (SubjectAttributesCollector)Class.forName(
                implClass).newInstance();
            attrCollector.init(realmName, configMap);
        } catch (ClassNotFoundException ex) {
            PrivilegeManager.debug.error("SubjectAttributesManager.<init>",
                ex);
        } catch (InstantiationException ex) {
            PrivilegeManager.debug.error("SubjectAttributesManager.<init>",
                ex);
        } catch (IllegalAccessException ex) {
            PrivilegeManager.debug.error("SubjectAttributesManager.<init>",
                ex);
        }
    }

    /**
     * Returns an instance of <code>SubjectAttributesManager</code>.
     *
     * @param adminSubject subject who has rights to access PIP.
     * @return an instance of <code>SubjectAttributesManager</code>.
     */
    public static SubjectAttributesManager getInstance(Subject adminSubject) {
        return getInstance(adminSubject, "/");
    }

    /**
     * Returns the <code>SubjectAttributesManager</code> of a given subject.
     *
     * @param adminSubject subject who has rights to access PIP.
     * @param subject Subject
     * @return <code>SubjectAttributesManager</code> of a given subject.
     */
    public static SubjectAttributesManager getInstance(
        Subject adminSubject,
        Subject subject) {
        //TOFIX get realm from subject;
        return getInstance(adminSubject,"/");
    }

    /**
     * Returns the <code>SubjectAttributesManager</code> of a given realm.
     *
     * @param adminSubject subject who has rights to access PIP.
     * @param realmName Name of realm.
     * @return <code>SubjectAttributesManager</code> of a given realm.
     */
    public static SubjectAttributesManager getInstance(
        Subject adminSubject,
        String realmName) {
        SubjectAttributesManager sam = null;

        instancesLock.readLock().lock();
        try {
            sam = instances.get(realmName);
        } finally {
            instancesLock.readLock().unlock();
        }

        if (sam == null) {
            sam = new SubjectAttributesManager(adminSubject, realmName);

            instancesLock.writeLock().lock();
            try {
                SubjectAttributesManager temp = instances.get(realmName);
                if(temp == null) {
                    instances.put(realmName, sam);
                } else {
                    sam = temp;
                }
            } finally {
                instancesLock.writeLock().unlock();
            }
        }

        return sam;
    }

    /**
     * Returns the subject search indexes for a given privilege.
     *
     * @param privilege Privilege object.
     * @return the subject search indexes for a given privilege.
     * @throws com.sun.identity.entitlement.EntitlementException if indexes
     * cannot be obtained.
     */
    public static Set<String> getSubjectSearchIndexes(Privilege privilege)
        throws EntitlementException {
        Set searchIndexes = new HashSet();
        EntitlementSubject es = privilege.getSubject();
        if (es != null) {
            Map<String, Set<String>> sis = es.getSearchIndexAttributes();
            for (String attrName : sis.keySet()) {
                Set<String> attrValues = sis.get(attrName);
                for (String v : attrValues) {
                    searchIndexes.add(attrName + "=" + v);
                }
            }
        } else {
            searchIndexes.add(
                SubjectAttributesCollector.NAMESPACE_IDENTITY + "=" +
                SubjectAttributesCollector.ATTR_NAME_ALL_ENTITIES);
        }
        return (searchIndexes);
    }

    /**
     * Returns the required attribute name for a given privilege.
     *
     * @param privilege Privilege object.
     * @return the required attribute name for a given privilege.
     */
    public static Set<String> getRequiredAttributeNames(Privilege privilege) {
        EntitlementSubject e = privilege.getSubject();
        return (e != null) ? e.getRequiredAttributeNames() :
            Collections.EMPTY_SET;
    }

    /**
     * Returns the subject search filter for a given subject.
     *
     * @param subject Subject object.
     * @param applicationName Name of application.
     * @return subject search filter for a given subject.
     * @throws com.sun.identity.entitlement.EntitlementException if search
     * filter cannot be obtained.
     */
    public Set<String> getSubjectSearchFilter(
        Subject subject,
        String applicationName)
        throws EntitlementException {
        Set<String> results = new HashSet<String>();
        results.add(SubjectAttributesCollector.NAMESPACE_IDENTITY + "=" +
            SubjectAttributesCollector.ATTR_NAME_ALL_ENTITIES);
        if (subject != null) {
            Set<String> names = getApplicationAttributeNames(
                realmName, applicationName);
            SubjectAttributesManager sam = SubjectAttributesManager.getInstance(
                adminSubject, realmName);
            Map<String, Set<String>> values = sam.getAttributes(subject, names);

            if (values != null) {
                for (String k : values.keySet()) {
                    Set<String> set = values.get(k);
                    for (String v : set) {
                        results.add(k + "=" + v);
                    }
                }
            }
        }
        return results;
    }

    /**
     * Returns the attribute values of the given user represented by
     * <class>Subject</class> object.
     * @param subject identity of the user.
     * @param attrNames requested attribute names.
     * @return a map of attribute names and their values.
     * @throws com.sun.identity.entitlement.EntitlementException if attribute
     * values cannot be obtained.
     */
    public Map<String, Set<String>> getAttributes(
        Subject subject,
        Set<String> attrNames
    ) throws EntitlementException {
        return attrCollector.getAttributes(subject, attrNames);
    }

    /**
     * Returns <code>true</code> if attribute value for the given user
     * represented by <class>Subject</class> object is present.
     *
     * @param subject identity of the user
     * @param attrName attribute name to check
     * @param attrValue attribute value to check
     * @return <code>true</code> if attribute value for the given user
     * represented by <class>Subject</class> object is present.
     * @throws com.sun.identity.entitlement.EntitlementException
     */
    public boolean hasAttribute(
        Subject subject,
        String attrName,
        String attrValue
    ) throws EntitlementException {
        return attrCollector.hasAttribute(subject, attrName, attrValue);
    }

    /**
     * Returns application attribute names.
     *
     * @param realm Realm name
     * @param applicationName Application name.
     * @return application attribute names.
     * @throws EntitlementException if application attributes cannot be
     * returned.
     */
    public Set<String> getApplicationAttributeNames(
        String realm,
        String applicationName
    ) throws EntitlementException {
        EntitlementConfiguration ec = EntitlementConfiguration.getInstance(
            adminSubject, realm);
        return ec.getSubjectAttributeNames(applicationName);
    }

    /**
     * Returns available subject attribute names.
     *
     * @return a set of available subject attribute names or null if not found
     * @throws EntitlementException if available subject attribute names
     * cannot be returned.
     */
    public Set<String> getAvailableSubjectAttributeNames()
        throws EntitlementException{

        return attrCollector.getAvailableSubjectAttributeNames();
    }

    /**
     * Returns true if group membership search index is enabled or false
     * otherwise.
     *
     * @return true if group membership search index is enabled or false
     * otherwise.
     */
    public boolean isGroupMembershipSearchIndexEnabled() {
        return attrCollector.isGroupMembershipSearchIndexEnabled();
    }

    /**
     * Returns the attribute values of the given user represented by
     * <class>Subject</class> object.
     * @param subject identity of the user.
     * @param attrNames requested attribute names.
     * @return a map of attribute names and their values.
     * @throws com.sun.identity.entitlement.EntitlementException if attribute
     * values cannot be obtained.
     */
    public Map<String, Set<String>> getUserAttributes(
        Subject subject,
        Set<String> attrNames
    ) throws EntitlementException {
        return attrCollector.getUserAttributes(subject, attrNames);
    }
}
