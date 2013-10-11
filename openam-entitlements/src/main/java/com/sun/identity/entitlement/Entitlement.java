/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: Entitlement.java,v 1.7 2010/01/25 23:48:14 veiming Exp $
 *
 * Portions copyright 2010-2013 ForgeRock, Inc.
 */
package com.sun.identity.entitlement;

import com.sun.identity.entitlement.interfaces.ResourceName;
import com.sun.identity.shared.JSONUtils;
import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.util.DN;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class encapsulates entitlement of a subject.
 * <p>
 * Example of how to use this class
 * <pre>
 *     Set set = new HashSet();
 *     set.add("GET");
 *     Evaluator evaluator = new Evaluator(adminToken);
 *     boolean isAllowed = evaluator.hasEntitlement(subject, 
 *         new Entitlement("http://www.sun.com/example", set), 
 *         Collections.EMPTY_MAP);
 * </pre>
 * Or do a sub tree search like this.
 * <pre>
 *     Evaluator evaluator = new Evaluator(adminToken);
 *     List&lt;Entitlement> entitlements = evaluator.getEntitlements(
 *         subject, "http://www.sun.com", Collections.EMPTY_MAP, true);
 *     for (Entitlement e : entitlements) {
 *         String resource = e.getResourceNames();
 *         boolean isAllowed =((Boolean)e.getActionValue("GET")).booleanValue();
 *         ...
 *     }
 * </pre>
 */
public class Entitlement {
    private String name;
    private String applicationName =
        ApplicationTypeManager.URL_APPLICATION_TYPE_NAME;
    private Set<String> resourceNames;
    private Set<String> excludedResourceNames;
    private Map<String, Boolean> actionValues;
    private Map<String, Set<String>> advices;
    private Map<String, Set<String>> attributes;
    private Application application;
    private long timeToLive = Long.MAX_VALUE;

    /**
     * Creates an entitlement object with default service name.
     */
    public Entitlement() {
    }

    public Entitlement(JSONObject jo) throws JSONException {
        name = (String)jo.opt("name");
        applicationName = (String)jo.opt("applicationName");
        resourceNames = JSONUtils.getSet(jo, "resourceNames");
        excludedResourceNames = JSONUtils.getSet(jo, "excludedResourceNames");
        actionValues = JSONUtils.getMapStringBoolean(jo, "actionsValues");
        advices = JSONUtils.getMapStringSetString(jo, "advices");
        attributes = JSONUtils.getMapStringSetString(jo, "attributes");
    }

    /**
     * Creates an entitlement object.
     *
     * @param resourceNames Resource names.
     * @param actionNames Set of action names.
     */
    public Entitlement(Set<String> resourceNames, Set<String> actionNames) {
        setResourceNames(resourceNames);
        setActionNames(actionNames);
    }

    /**
     * Creates an entitlement object.
     *
     * @param resourceName Resource name.
     * @param actionNames Set of action names.
     */
    public Entitlement(String resourceName, Set<String> actionNames) {
        setResourceName(resourceName);
        setActionNames(actionNames);
    }

    /**
     * Creates an entitlement object.
     *
     * @param applicationName Application name.
     * @param resourceName Resource name.
     * @param actionNames Set of action names.
     */
    public Entitlement(
        String applicationName,
        String resourceName,
        Set<String> actionNames
    ) {
        setApplicationName(applicationName);
        setResourceName(resourceName);
        setActionNames(actionNames);
    }

    /**
     * Creates an entitlement object.
     *
     * @param resourceName Resource namess.
     * @param actionValues Map of action name to set of values.
     */
    public Entitlement(
        String resourceName,
        Map<String, Boolean> actionValues
    ) {
        setResourceName(resourceName);
        setActionValues(actionValues);
    }

    /**
     * Creates an entitlement object.
     *
     * @param applicationName applicationName
     * @param resourceName Resource namess.
     * @param actionValues Map of action name to set of values.
     */
    public Entitlement(
        String applicationName,
        String resourceName,
        Map<String, Boolean> actionValues
    ) {
        setApplicationName(applicationName);
        setResourceName(resourceName);
        setActionValues(actionValues);
    }

    /**
     * Creates an entitlement object.
     *
     * @param applicationName Application name.
     * @param resourceNames Resource names.
     * @param actionValues Map of action name to set of values.
     */
    public Entitlement(
        String applicationName,
        Set<String> resourceNames,
        Map<String, Boolean> actionValues
    ) {
        setApplicationName(applicationName);
        setResourceNames(resourceNames);
        setActionValues(actionValues);
    }

    /**
     * Sets the name of the entitlement
     * @param name the name of the entitlement
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the name of the entitlement
     * @return the name of the entitlement
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets resource names.
     *
     * @param resourceNames Resource Names.
     */
    public void setResourceNames(Set<String> resourceNames) {
        this.resourceNames = resourceNames;
    }

    /**
     * Returns resource names.
     *
     * @return resource names.
     */
    public Set<String> getResourceNames() {
        return resourceNames;
    }

    /**
     * Sets resource name.
     *
     * @param resourceName Resource Name.
     */
    public void setResourceName(String resourceName) {
        Set<String> rn = new HashSet<String>();
        rn.add(resourceName);
        setResourceNames(rn);
    }

    /**
     * Returns resource name.
     *
     * @return resource names.
     */
    public String getResourceName() {
        if (resourceNames == null || resourceNames.isEmpty()) {
            return null;
        }
        return resourceNames.iterator().next();
    }

    /**
     * Sets excluded resource names.
     *
     * @param excludedResourceNames excluded resource names.
     */
    public void setExcludedResourceNames(
        Set<String> excludedResourceNames) {
        this.excludedResourceNames = excludedResourceNames;
    }

    /**
     * Returns excluded resource names.
     *
     * @return excluded resource names.
     */
    public Set<String> getExcludedResourceNames() {
        return (excludedResourceNames == null) ? Collections.EMPTY_SET :
            excludedResourceNames;
    }

    /**
     * Returns application name.
     *
     * @return application name.
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * Sets application name.
     *
     * @param applicationName application name.
     */
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * Sets action name
     *
     * @param actionName Action name.
     */
    public void setActionName(String actionName) {
        actionValues = new HashMap<String, Boolean>();
        actionValues.put(actionName, Boolean.TRUE);
    }

    /**
     * Sets action names
     *
     * @param actionNames Set of action names.
     */
    public void setActionNames(Set<String> actionNames) {
        actionValues = new HashMap<String, Boolean>();
        for (String i : actionNames) {
            actionValues.put(i, Boolean.TRUE);
        }
    }

    /**
     * Sets action values map.
     *
     * @param actionValues Action values.
     */
    public void setActionValues(Map<String, Boolean> actionValues) {
        this.actionValues = new HashMap<String, Boolean>(actionValues);
    }

    /**
     * Returns action value.
     *
     * @param name Name of the action.
     * @return action values.
     */
    public Boolean getActionValue(String name) {
        return actionValues.get(name);
    }

    /**
     * Returns action values.
     *
     * @return action values.
     */
    public Map<String, Boolean> getActionValues() {
        return actionValues;
    }

    /**
     * Returns action values.
     *
     * @param name Name of the action.
     * @return action values.
     */
    public Set<Object> getActionValues(String name) {
        Object o = actionValues.get(name);
        if (o instanceof Set) {
            return (Set<Object>) o;
        }

        Set<Object> set = new HashSet<Object>();
        set.add(o);
        return set;
    }

    /**
     * Sets advices.
     *
     * @param advices Advices.
     */
    public void setAdvices(Map<String, Set<String>> advices) {
        this.advices = advices;
    }

    /**
     * Returns advices.
     *
     * @return Advices.
     */
    public Map<String, Set<String>> getAdvices() {
        return advices;
    }

    /**
     * @return Whether this entitlement has any advice.
     */
    public boolean hasAdvice() {
        return advices != null && !advices.isEmpty();
    }

    /**
     * Sets attributes.
     *
     * @param attributes Attributes.
     */
    public void setAttributes(Map<String, Set<String>> attributes) {
        this.attributes = attributes;
    }

    /**
     * Returns attributes.
     *
     * @return Attributes.
     */
    public Map<String, Set<String>> getAttributes() {
        return attributes;
    }

    /**
     * Sets this entitlements TTL
     *
     * @param ttl The TTL to set
     */
    public void setTTL(long ttl) {
        this.timeToLive = ttl;
    }

    /**
     * Returns the TTL
     *
     * @return The TTL in ms
     */
    public long getTTL() {
        return this.timeToLive;
    }

    /**
     * Returns a set of resource names that match the given resource.
     *
     * @param adminSubject Admin Subject.
     * @param realm Realm Name
     * @param subject Subject who is under evaluation.
     * @param applicationName application name.
     * @param resourceName Resource name.
     * @param environment Environment parameters.
     * @return a set of resource names that match the given resource.
     * @throws EntitlementException if resource names cannot be returned.
     */
    public Set<String> evaluate(
        Subject adminSubject,
        String realm,
        Subject subject,
        String applicationName,
        String resourceName,
        Set<String> actionNames,
        Map<String, Set<String>> environment,
        boolean recursive)
        throws EntitlementException {
        for (String a : actionNames) {
            if (actionValues.keySet().contains(a)) {
                return getMatchingResources(adminSubject, realm,
                    subject, applicationName, resourceName, recursive);
            }
        }
        return Collections.EMPTY_SET;
    }

    protected Set<String> getMatchingResources(
        Subject adminSubject,
        String realm,
        Subject subject,
        String applicationName,
        String resourceName,
        boolean recursive
    ) throws EntitlementException {
        if ((resourceNames == null) || resourceNames.isEmpty()) {
            return Collections.EMPTY_SET;
        }

        if (!this.applicationName.endsWith(applicationName)){
            return Collections.EMPTY_SET;
        }
        
        ResourceName resComparator = getResourceComparator(adminSubject, realm);

        Set<String> matched = new HashSet<String>();

        Set<String> resources = (subject != null) ? 
                tagswapResourceNames(subject, resourceNames) : resourceNames;
        
        for (String r : resources) {
            if (!recursive) {
                if (resComparator instanceof RegExResourceName) {
                    ResourceMatch match = resComparator.compare(
                        resourceName, r, true);
                    if (match.equals(ResourceMatch.EXACT_MATCH) ||
                        match.equals(ResourceMatch.SUPER_RESOURCE_MATCH) ||
                        match.equals(ResourceMatch.WILDCARD_MATCH)) {
                        matched.add(r);
                    }
                } else {
                    ResourceMatch match = resComparator.compare(
                        r, resourceName, false);
                    if (match.equals(ResourceMatch.EXACT_MATCH)) {
                        matched.add(r);
                    } else {
                        match = resComparator.compare(resourceName, r, true);
                        if (match.equals(ResourceMatch.WILDCARD_MATCH)) {
                            matched.add(r);
                        }
                    }
                }
            } else {
                if (resComparator instanceof RegExResourceName) {
                    ResourceMatch match = resComparator.compare(
                        r, resourceName, true);
                    if (!match.equals(ResourceMatch.NO_MATCH)) {
                        matched.add(r);
                    }
                } else {
                    ResourceMatch match = resComparator.compare(
                        resourceName, r, true);
                    if (match.equals(ResourceMatch.WILDCARD_MATCH) ||
                        match.equals(ResourceMatch.SUB_RESOURCE_MATCH)) {
                        matched.add(r);
                    } else {
                        match = resComparator.compare(r, resourceName, false);
                        if (match.equals(ResourceMatch.EXACT_MATCH) ||
                            match.equals(ResourceMatch.SUPER_RESOURCE_MATCH)) {
                            matched.add(r);
                        }
                    }
                }
            }
        }

        for (Iterator<String> i = matched.iterator(); i.hasNext(); ) {
            String r = i.next();
            if ((excludedResourceNames != null) &&
                !excludedResourceNames.isEmpty()) {

                Set<String> excludes = tagswapResourceNames(subject,
                    excludedResourceNames);
                for (String e : excludes) {
                    ResourceMatch match = resComparator.compare(r, e, true);
                    if (match.equals(ResourceMatch.EXACT_MATCH) ||
                        match.equals(ResourceMatch.WILDCARD_MATCH)) {
                        i.remove();
                        break;
                    } else if (recursive && match.equals(
                        ResourceMatch.SUB_RESOURCE_MATCH)) {
                        i.remove();
                        break;
                    }
                }
            }
        }

        return matched;
    }
    
    private Set<String> tagswapResourceNames(Subject sbj, Set<String> set)
        throws EntitlementException {
        if (sbj == null) {
            return set;
        }

        Set<String> resources = new HashSet<String>();
        Set<String> userIds = new HashSet<String>();

        Set<Principal> principals = sbj.getPrincipals();
        if (!principals.isEmpty()) {
            for (Principal p : principals) {
                String pName = p.getName();
                if (DN.isDN(pName)){
                    String[] rdns = LDAPDN.explodeDN(pName, true);
                    userIds.add(rdns[0]);
                } else {
                    userIds.add(pName);
                }
            }
        }

        if (!userIds.isEmpty()) {
            for (String r : set) {
                for (String uid : userIds) {
                    resources.add(r.replaceAll("\\$SELF", uid));
                }
            }
        }

        return resources;
    }

    /**
     * Returns string representation of the object
     * @return string representation of the object
     */
    @Override
    public String toString() {
        String s = null;
        try {
            JSONObject jo = toJSONObject();
            s = (jo == null) ? super.toString() : jo.toString(2);
        } catch (JSONException joe) {
            PrivilegeManager.debug.error("Entitlement.toString()", joe);
        }
        return s;
    }

    /**
     * Returns JSONObject mapping of  the object
     * @return JSONObject mapping of  the object
     * @throws JSONException if can not map to JSONObject
     */
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("name", name);
        jo.put("applicationName", applicationName);

        if (resourceNames != null) {
            jo.put("resourceNames", resourceNames);
        }

        if (excludedResourceNames != null) {
            jo.put("excludedResourceNames", excludedResourceNames);
        }

        if (actionValues != null) {
            jo.put("actionsValues", actionValues);
        }

        if (advices != null) {
            jo.put("advices", advices);
        }

        if (attributes != null) {
            jo.put("attributes", attributes);
        }
        return jo;
    }

    /**
     * Returns <code>true</code> if the passed in object is equal to this object
     * @param obj object to check for equality
     * @return  <code>true</code> if the passed in object is equal to this object
     */
    @Override
    public boolean equals(Object obj) {
        boolean equalled = true;
        if (obj == null) {
            return false;
        }
        if (!getClass().equals(obj.getClass())) {
            return false;
        }
        Entitlement object = (Entitlement) obj;

        if (name == null) {
            if (object.getName() != null) {
                return false;
            }
        } else { // name not null

            if ((object.getName()) == null) {
                return false;
            } else if (!name.equals(object.getName())) {
                return false;
            }
        }

        if (applicationName == null) {
            if (object.getApplicationName() != null) {
                return false;
            }
        } else { // serviceName not null

            if ((object.getApplicationName()) == null) {
                return false;
            } else if (!applicationName.equals(object.getApplicationName())) {
                return false;
            }
        }

        if (resourceNames == null) {
            if (object.getResourceNames() != null) {
                return false;
            }
        } else { // resourceNames not null

            if ((object.getResourceNames()) == null) {
                return false;
            } else if (!resourceNames.equals(object.getResourceNames())) {
                return false;
            }
        }

        if ((excludedResourceNames == null) || excludedResourceNames.isEmpty()) {
            if ((object.excludedResourceNames != null) &&
                !object.excludedResourceNames.isEmpty()) {
                return false;
            }
        } else {
            if (object.excludedResourceNames == null) {
                return false;
            }
            if (!excludedResourceNames.equals(
                object.excludedResourceNames)) {
                return false;
            }
        }

        if (actionValues == null) {
            if ((object.getActionValues() != null) &&
                !object.getActionValues().isEmpty()) {
                return false;
            }
        } else { // actionValues not null

            if ((object.getActionValues()) == null) {
                return false;
            } else if (!actionValues.equals(
                    object.getActionValues())) {
                return false;
            }
        }

        if (advices == null) {
            if ((object.getAdvices() != null) &&
                !object.getAdvices().isEmpty()) {
                return false;
            }
        } else { // advices not null

            if ((object.getAdvices()) == null) {
                return false;
            } else if (!advices.equals(
                    object.getAdvices())) {
                return false;
            }
        }

        if (attributes == null) {
            if ((object.getAttributes() != null) &&
                !object.getAttributes().isEmpty()) {
                return false;
            }
        } else { // attributes not null

            if ((object.getAttributes()) == null) {
                return false;
            } else if (!attributes.equals(
                    object.getAttributes())) {
                return false;
            }
        }

        return equalled;
    }

    /**
     * Returns hash code of the object
     * @return hash code of the object
     */
    @Override
    public int hashCode() {
        int code = 0;
        if (name != null) {
            code += name.hashCode();
        }
        if (applicationName != null) {
            code += applicationName.hashCode();
        }
        if (resourceNames != null) {
            code += resourceNames.hashCode();
        }
        if (excludedResourceNames != null) {
            code += excludedResourceNames.hashCode();
        }
        if (actionValues != null) {
            code += actionValues.hashCode();
        }
        if (advices != null) {
            code += advices.hashCode();
        }
        if (attributes != null) {
            code += attributes.hashCode();
        }
        return code;
    }

    /**
     * Returns resource search indexes.
     *
     * @param adminSubject Admin Subject.
     * @param realm Realm Name
     * @return resource search indexes.
     */
    public ResourceSearchIndexes getResourceSearchIndexes(
        Subject adminSubject, 
        String realm
    ) throws EntitlementException {
        ResourceSearchIndexes result = null;
        ApplicationType applType = getApplication(
            adminSubject, realm).getApplicationType();

        for (String r : resourceNames) {
            ResourceSearchIndexes rsi = applType.getResourceSearchIndex(r, realm);
            if (result == null) {
                result = rsi;
            } else {
                result.addAll(rsi);
            }
        }
        return result;
    }

    /**
     * Returns resource save indexes.
     *
     * @param adminSubject Admin Subject.
     * @param realm Realm Name
     * @return resource save indexes.
     */
    public ResourceSaveIndexes getResourceSaveIndexes(
        Subject adminSubject, 
        String realm
    ) throws EntitlementException {
        ResourceSaveIndexes result = null;
        Application appl = getApplication(adminSubject, realm);

        // application can be null if the referred privilege is removed.
        // get the application from root realm
        if (appl == null) {
            appl = getApplication(adminSubject, "/");
        }

        if (appl != null) {
            ApplicationType applType = appl.getApplicationType();

            for (String r : resourceNames) {
                ResourceSaveIndexes rsi = applType.getResourceSaveIndex(r);
                if (result == null) {
                    result = rsi;
                } else {
                    result.addAll(rsi);
                }
            }
        }
        return result;
    }

    /**
     * Returns application for this entitlement.
     *
     * @param adminSubject Admin Subject.
     * @param realm Realm Name
     * @return application for this entitlement.
     */
    public Application getApplication(Subject adminSubject, String realm) 
        throws EntitlementException {
        if (application == null) {
            application = ApplicationManager.getApplication(
                PrivilegeManager.superAdminSubject, realm, applicationName);
        }
        if (application == null) {
            PrivilegeManager.debug.error("Entitlement.getApplication null" +
                "realm=" + realm + " applicationname=" + applicationName,null);
        }
        return application;
    }

    ResourceName getResourceComparator(Subject adminSubject, String realm) 
        throws EntitlementException {
        return getApplication(PrivilegeManager.superAdminSubject,
            realm).getResourceComparator();
    }

    void validateResourceNames(Subject adminSubject, String realm
    ) throws EntitlementException {
        if ((resourceNames != null) && !resourceNames.isEmpty()) {
            Application app = getApplication(adminSubject, realm);
            for (String r : resourceNames) {
                ValidateResourceResult result = app.validateResourceName(r);
                if (!result.isValid()) {
                    Object[] args = {r};
                    throw new EntitlementException(303, args);
                }
            }
        }
    }

    /**
     * Canonicalizes resource name before persistence.
     *
     * @param adminSubject Admin Subject.
     * @param realm Realm Name
     */
    public void canonicalizeResources(Subject adminSubject, String realm)
        throws EntitlementException {
        ResourceName resComp = getResourceComparator(adminSubject, realm);
        if ((resourceNames != null) && !resourceNames.isEmpty()) {
            Set<String> temp = new HashSet<String>();
            for (String r : resourceNames) {
                temp.add(resComp.canonicalize(r));
            }
            resourceNames = temp;
        }
        
        if ((excludedResourceNames != null) && !excludedResourceNames.isEmpty())
        {
            Set<String> temp = new HashSet<String>();
            for (String r : excludedResourceNames) {
                temp.add(resComp.canonicalize(r));
            }
            excludedResourceNames = temp;
        }

    }
}
