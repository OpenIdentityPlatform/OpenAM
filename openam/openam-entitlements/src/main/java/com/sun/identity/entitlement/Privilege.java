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
 * $Id: Privilege.java,v 1.14 2010/01/08 22:20:47 veiming Exp $
 */

/*
 * Portions Copyrighted 2010-2013 ForgeRock, Inc.
 */

package com.sun.identity.entitlement;

import com.sun.identity.shared.JSONUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import org.forgerock.openam.entitlement.CachingEntitlementCondition;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class representing entitlement privilege
 */
public abstract class Privilege implements IPrivilege {
    /**
     * application index key
     */
    public static final String APPLICATION_ATTRIBUTE = "application";

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
     * Macro used in resource name
     */
    public static final String RESOURCE_MACRO_SELF = "$SELF";

    /**
     * Macro used in condition
     */
    public static final String RESOURCE_MACRO_ATTRIBUTE = "$ATTR";

    /**
     * Privilege description search attribute name,
     */
    public static final String DESCRIPTION_ATTRIBUTE = "description";

    private static Class privilegeClass;
    public static final NoSubject NOT_SUBJECT = new NoSubject();

    private boolean active = true;
    private String name;
    private String description;
    private Entitlement entitlement;
    private EntitlementSubject eSubject;
    private EntitlementCondition eCondition;
    private Set<ResourceAttribute> eResourceAttributes;

    private String createdBy;
    private String lastModifiedBy;
    private long creationDate;
    private long lastModifiedDate;
    private Set<String> applicationIndexes;


    static {
        try {
            //REF: should be customizable
            privilegeClass = Class.forName(
                "com.sun.identity.entitlement.opensso.OpenSSOPrivilege");
        } catch (ClassNotFoundException ex) {
            PrivilegeManager.debug.error("Privilege.<init>", ex);
        }
    }

    /**
     * Returns entitlement privilege.
     *
     * @return entitlement privilege.
     * @throws EntitlementException if entitlementPrivilege cannot be returned.
     */
    public static Privilege getNewInstance() throws EntitlementException {
        if (privilegeClass == null) {
            throw new EntitlementException(2);
        }
        try {
            return (Privilege)privilegeClass.newInstance();
        } catch (InstantiationException ex) {
            throw new EntitlementException(1, ex);
        } catch (IllegalAccessException ex) {
            throw new EntitlementException(1, ex);
        }
    }

    public Privilege() {
    }


    public void validateResourceNames(Subject adminSubject, String realm
        ) throws EntitlementException {
        entitlement.validateResourceNames(adminSubject, realm);
    }

    /**
     * Sets entitlement subject.
     *
     * @param eSubject Entitlement subject
     * @throws EntitlementException if subject is null.
     */
    public void setSubject(EntitlementSubject eSubject)
        throws EntitlementException {
        validateSubject(eSubject);
         this.eSubject = eSubject;
    }

    void validateSubject(EntitlementSubject sbj)
        throws EntitlementException {
        if (sbj == null) {
            sbj = NOT_SUBJECT;
        } else if (!sbj.isIdentity()) {
            Object[] params = {name};
            throw new EntitlementException(310, params);
        }
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
     * Returns the description of the privilege.
     * 
     * @return description of the privilege.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the privilege.
     * 
     * @param description Description of the privilege.
     */
    public void setDescription(String description) {
        this.description = description;
    }
    /**
     * Returns the eSubject the privilege
     * @return eSubject of the privilege.
     */
    public EntitlementSubject getSubject() {
        return eSubject;
    }

    /**
     * Returns the eCondition the privilege
     * @return eCondition of the privilege.
     */
    public EntitlementCondition getCondition() {
        return eCondition;
    }

    /**
     * Returns the eResurceAttributes of  the privilege
     * @return eResourceAttributes of the privilege.
     */
    public Set<ResourceAttribute> getResourceAttributes() {
        return eResourceAttributes;
    }

    /**
     * Returns entitlement defined in the privilege
     * @return entitlement defined in the privilege
     */
    public Entitlement getEntitlement() {
        return entitlement;
    }

    /**
     * Returns privilege Type.
     * @see PrivilegeType
     *
     * @return privilege Type.
     */
    public PrivilegeType getType() {
        return PrivilegeType.UNKNOWN;
    }

    /**
     * Returns a list of entitlement for a given subject, resource name
     * and environment.
     *
     * @param adminSubject Admin Subject
     * @param realm Realm Name
     * @param subject Subject who is under evaluation.
     * @param applicationName Application name.
     * @param resourceName Resource name.
     * @param actionNames Set of action names.
     * @param environment Environment parameters.
     * @param recursive <code>true</code> to perform evaluation on sub resources
     *        from the given resource name.
     * @return a list of entitlement for a given subject, resource name
     *         and environment.
     * @throws EntitlementException if the result cannot be determined.
     */
    public abstract List<Entitlement> evaluate(
        Subject adminSubject,
        String realm,
        Subject subject,
        String applicationName,
        String resourceName,
        Set<String> actionNames,
        Map<String, Set<String>> environment,
        boolean recursive,
        Object context) throws EntitlementException;

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


    public JSONObject toMinimalJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put(NAME_ATTRIBUTE, name);

        if (description != null) {
            jo.put("description", description);
        }
        if (entitlement != null) {
            jo.put("entitlement", entitlement.toJSONObject());
        }

        if (eSubject != null) {
            JSONObject subjo = new JSONObject();
            subjo.put("className", eSubject.getClass().getName());
            subjo.put("state", eSubject.getState());
            jo.put("eSubject", subjo);
        }

        if (eCondition != null) {
            JSONObject subjo = new JSONObject();
            subjo.put("className", eCondition.getClass().getName());
            subjo.put("state", eCondition.getState());
            jo.put("eCondition", subjo);
        }

        if ((eResourceAttributes != null) && !eResourceAttributes.isEmpty()) {
            for (ResourceAttribute r : eResourceAttributes) {
                JSONObject subjo = new JSONObject();
                subjo.put("className", r.getClass().getName());
                subjo.put("state", r.getState());
                jo.append("eResourceAttributes", subjo);
            }
        }

        return jo;
    }

    /**
     * Returns JSONObject mapping of  the object
     * @return JSONObject mapping of  the object
     * @throws JSONException if can not map to JSONObject
     */
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jo = toMinimalJSONObject();
        jo.put("className", getClass().getName());
        jo.put("active", Boolean.toString(active));

        if (description != null) {
            jo.put("description", description);
        }
        if (createdBy != null) {
            jo.put("createdBy", createdBy);
        }
        if (lastModifiedBy != null) {
            jo.put("lastModifiedBy", lastModifiedBy);
        }
        jo.put("lastModifiedDate", lastModifiedDate);
        jo.put("creationDate", creationDate);

        return jo;
    }

    protected abstract void init(JSONObject jo);

    public static Privilege getInstance(JSONObject jo) {
        String className = jo.optString("className");
        try {
            Class clazz = Class.forName(className);
            Privilege privilege = (Privilege)clazz.newInstance();
            privilege.name = jo.optString("name");
            privilege.active = Boolean.parseBoolean(jo.optString("active"));
            privilege.description = jo.optString("description");
            privilege.createdBy = jo.optString("createdBy");
            privilege.lastModifiedBy = jo.optString("lastModifiedBy");
            privilege.creationDate = JSONUtils.getLong(jo,
                "creationDate");
            privilege.lastModifiedDate = JSONUtils.getLong(jo,
                "lastModifiedDate");

            if (jo.has("entitlement")) {
                privilege.entitlement = new Entitlement(
                    jo.getJSONObject("entitlement"));
            }
            privilege.eSubject = getESubject(jo);
            privilege.eCondition = getECondition(jo);
            privilege.eResourceAttributes = getResourceAttributes(jo);
            privilege.init(jo);
            
            return privilege;
        } catch (InstantiationException ex) {
            PrivilegeManager.debug.error("Privilege.getInstance", ex);
        } catch (IllegalAccessException ex) {
            PrivilegeManager.debug.error("Privilege.getInstance", ex);
        } catch (ClassNotFoundException ex) {
            PrivilegeManager.debug.error("Privilege.getInstance", ex);
        } catch (JSONException ex) {
            PrivilegeManager.debug.error("Privilege.getInstance", ex);
        }
        return null;
    }

    private static Set<ResourceAttribute> getResourceAttributes(JSONObject jo)
        throws JSONException{
        if (!jo.has("eResourceAttributes")) {
            return null;
        }
        JSONArray array = jo.getJSONArray("eResourceAttributes");
        Set<ResourceAttribute> results = new HashSet<ResourceAttribute>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject json = (JSONObject)array.get(i);
            try {
                Class clazz = Class.forName(json.getString("className"));
                ResourceAttribute ra = (ResourceAttribute)clazz.newInstance();
                ra.setState(json.getString("state"));
                results.add(ra);
            } catch (InstantiationException ex) {
                PrivilegeManager.debug.error(
                    "Privilege.getResourceAttributes", ex);
            } catch (IllegalAccessException ex) {
                PrivilegeManager.debug.error(
                    "Privilege.getResourceAttributes", ex);
            } catch (ClassNotFoundException ex) {
                PrivilegeManager.debug.error(
                    "Privilege.getResourceAttributes", ex);
            }
        }

        return results;
    }


    private static EntitlementSubject getESubject(JSONObject jo)
        throws JSONException {
        if (!jo.has("eSubject")) {
            return new NoSubject();
        }
        JSONObject sbj = jo.getJSONObject("eSubject");
        try {
            Class clazz = Class.forName(sbj.getString("className"));
            EntitlementSubject eSubject = (EntitlementSubject)
                clazz.newInstance();
            eSubject.setState(sbj.getString("state"));
            return eSubject;
        } catch (InstantiationException ex) {
            PrivilegeManager.debug.error("Privilege.getESubject", ex);
        } catch (IllegalAccessException ex) {
            PrivilegeManager.debug.error("Privilege.getESubject", ex);
        } catch (ClassNotFoundException ex) {
            PrivilegeManager.debug.error("Privilege.getESubject", ex);
        }
        return null;
    }


    private static EntitlementCondition getECondition(JSONObject jo)
        throws JSONException {
        if (!jo.has("eCondition")) {
            return null;
        }

        JSONObject sbj = jo.getJSONObject("eCondition");
        try {
            Class clazz = Class.forName(sbj.getString("className"));
            EntitlementCondition eCondition = (EntitlementCondition)
                clazz.newInstance();
            eCondition.setState(sbj.getString("state"));
            return new CachingEntitlementCondition(eCondition);
        } catch (InstantiationException ex) {
            PrivilegeManager.debug.error("Privilege.getECondition", ex);
        } catch (IllegalAccessException ex) {
            PrivilegeManager.debug.error("Privilege.getECondition", ex);
        } catch (ClassNotFoundException ex) {
            PrivilegeManager.debug.error("Privilege.getECondition", ex);
        }
        return null;
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
        Privilege object = (Privilege) obj;

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

        if (this.active != object.active) {
            return false;
        }

        if (entitlement == null) {
            if (object.getEntitlement() != null) {
                return false;
            }
        } else { // name not null

            if ((object.getEntitlement()) == null) {
                return false;
            } else if (!entitlement.equals(object.getEntitlement())) {
                return false;
            }
        }

        if (eSubject == null) {
            if (object.getSubject() != null) {
                return false;
            }
        } else { // name not null
            if ((object.getSubject()) == null) {
                return false;
            } else if (!eSubject.equals(object.getSubject())) {
                return false;
            }
        }

        if (eResourceAttributes == null) {
            if (object.getResourceAttributes() != null) {
                return false;
            }
        } else { // name not null

            if ((object.getResourceAttributes()) == null) {
                return false;
            } else if (!eResourceAttributes.equals(
                object.getResourceAttributes())) {
                return false;
            }
        }

        if (eCondition == null) {
            if (object.getCondition() != null) {
                return false;
            }
        } else { // name not null

            if ((object.getCondition()) == null) {
                return false;
            } else if (!eCondition.equals(object.getCondition())) {
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
        if (entitlement != null) {
            code += entitlement.hashCode();
        }
        if (eSubject != null) {
            code += eSubject.hashCode();
        }
        if (eCondition != null) {
            code += eCondition.hashCode();
        }
        if (eResourceAttributes != null) {
            code += eResourceAttributes.hashCode();
        }
        return code;
    }

    protected boolean doesSubjectMatch(
        Subject adminSubject,
        String realm,
        Map<String, Set<String>> resultAdvices,
        Subject subject,
        String resourceName,
        Map<String, Set<String>> environment
    ) throws EntitlementException {
        boolean result = true;
        if (getSubject() != null) {
            SubjectAttributesManager mgr =
                SubjectAttributesManager.getInstance(adminSubject, realm);
            SubjectDecision sDecision = getSubject().evaluate(realm,
                mgr, subject, resourceName, environment);
            if (!sDecision.isSatisfied()) {
                Map<String, Set<String>> advices = sDecision.getAdvices();
                if (advices != null) {
                    resultAdvices.putAll(advices);
                }
                result = false;
            }
        }

        if (PrivilegeManager.debug.messageEnabled()) {
            if (result) {
                PrivilegeManager.debug.message(
                    "[PolicyEval] Privilege.doesSubjectMatch: true", null);
            } else {
                PrivilegeManager.debug.message(
                    "[PolicyEval] Privilege.doesSubjectMatch: false", null);
                PrivilegeManager.debug.message("[PolicyEval] Advices: " +
                    resultAdvices.toString(), null);
            }
        }
        return result;
    }

    protected boolean doesConditionMatch(
        String realm,
        Map<String, Set<String>> resultAdvices,
        Subject subject,
        String resourceName,
        Map<String, Set<String>> environment,
        Set decisions
    ) throws EntitlementException {
        boolean result = true;

        if (eCondition != null) {
            ConditionDecision decision = eCondition.evaluate(realm,
                subject, resourceName, environment);
            Map<String, Set<String>> advices = decision.getAdvices();
            if (advices != null) {
                resultAdvices.putAll(advices);
            }
            result = decision.isSatisfied();
            decisions.add(decision);
        }

        if (PrivilegeManager.debug.messageEnabled()) {
            if (result) {
                PrivilegeManager.debug.message(
                    "[PolicyEval] Privilege.doesConditionMatch: true", null);
            } else {
                PrivilegeManager.debug.message(
                    "[PolicyEval] Privilege.doesConditionMatch: false", null);
                PrivilegeManager.debug.message("[PolicyEval] Advices: " +
                    resultAdvices.toString(), null);
            }
        }

        return result;
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
     * Canonicalizes resource name before persistence.
     *
     * @param adminSubject Admin Subject.
     * @param realm Realm Name
     */
    public void canonicalizeResources(Subject adminSubject, String realm)
        throws EntitlementException {
        entitlement.canonicalizeResources(adminSubject, realm);
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
        String realm) throws EntitlementException {
        return (entitlement != null) ? entitlement.getResourceSaveIndexes(
            adminSubject, realm) : null;
    }

    /**
     * Sets name.
     *
     * @param name Name of privilege.
     * @throws EntitlementException if name is null or empty.
     */
    public void setName(String name) throws EntitlementException {
        if ((name == null) || (name.trim().length() == 0)) {
            throw new EntitlementException(3);
        }
        this.name = name;
    }

    /**
     * Sets entitlement.
     *
     * @param entitlement Entitlement.
     * @throws EntitlementException if entitlement is null.
     */
    public void setEntitlement(Entitlement entitlement)
        throws EntitlementException {
        if (entitlement == null) {
            throw new EntitlementException(4);
        }
        this.entitlement = entitlement;
    }

    /**
     * Sets condition.
     *
     * @param condition Condition.
     */
    public void setCondition(EntitlementCondition condition) {
        this.eCondition = condition;
    }

    /**
     * Sets resource attributes.
     *
     * @param set Set of resource attribute.
     */
    public void setResourceAttributes(Set<ResourceAttribute> set) {
        if (set == null) {
            this.eResourceAttributes = null;
        } else {
            this.eResourceAttributes = new HashSet();
            this.eResourceAttributes.addAll(set);
        }
    }

    protected Map<String, Set<String>> getAttributes(Subject adminSubject, 
        String realm, Subject subject, String resourceName,
        Map<String, Set<String>> environment
    ) throws EntitlementException {
        Map<String, Set<String>> result = null;

        if ((eResourceAttributes != null) && !eResourceAttributes.isEmpty()) {
            result = new HashMap<String, Set<String>>();

            for (ResourceAttribute e : eResourceAttributes) {
                Map<String, Set<String>> values = e.evaluate(adminSubject,
                    realm, subject, resourceName, environment);

                for (String k : values.keySet()) {
                    Set<String> v = result.get(k);

                    if (v == null) {
                        v = new HashSet<String>();
                        result.put(k, v);
                    }

                    v.addAll(values.get(k));
                }
            }
        }
        return result;
    }

    /**
     * Returns <code>true</code> if this privilege is active.
     *
     * @return <code>true</code> if this privilege is active.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets this privilege active/inactive.
     * 
     * @param active <code>true</code> if this privilege is to be active.
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    public static Privilege getNewInstance(String jo)
        throws EntitlementException {
        if ((jo == null) || (jo.trim().length() == 0)) {
            throw new EntitlementException(9);
        }
        try {
            return getNewInstance(new JSONObject(jo));
        } catch (JSONException ex) {
            throw new EntitlementException(11);
        }
    }

    public static Privilege getNewInstance(JSONObject jo)
        throws EntitlementException {
        if (privilegeClass == null) {
            throw new EntitlementException(2);
        }
        
        try {
            Privilege privilege = (Privilege) privilegeClass.newInstance();

            if (!jo.has(NAME_ATTRIBUTE)) {
                throw new EntitlementException(3);
            }
            privilege.name = jo.optString(NAME_ATTRIBUTE);
            privilege.description = jo.optString("description");
            if (jo.has("entitlement")) {
                privilege.entitlement = new Entitlement(
                    jo.getJSONObject("entitlement"));
            }
            privilege.eSubject = getESubject(jo);
            privilege.eCondition = getECondition(jo);
            privilege.eResourceAttributes = getResourceAttributes(jo);
            privilege.init(jo);

            return privilege;
        } catch (InstantiationException ex) {
            throw new EntitlementException(1, ex);
        } catch (IllegalAccessException ex) {
            throw new EntitlementException(1, ex);
        } catch (JSONException ex) {
            throw new EntitlementException(1, ex);
        }
    }


    public void setApplicationIndexes(Set<String> indexes) {
        applicationIndexes = indexes;
    }

    public Set<String> getApplicationIndexes() {
        return (applicationIndexes == null) ? Collections.EMPTY_SET :
            applicationIndexes;
    }
}

