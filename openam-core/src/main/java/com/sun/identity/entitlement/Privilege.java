/*
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
 *
 * Portions Copyrighted 2010-2015 ForgeRock AS.
 * Portions Copyright 2026 3A Systems, LLC.
 */

package com.sun.identity.entitlement;

import com.sun.identity.entitlement.util.SearchAttribute;
import com.sun.identity.shared.JSONUtils;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.sm.SMSEntry;

import org.forgerock.openam.entitlement.CachingEntitlementCondition;
import org.forgerock.openam.entitlement.PolicyConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.Subject;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class representing entitlement privilege
 */
public abstract class Privilege implements IPrivilege {

    /**
     * The system property defining the default Privilege sub-class to use when constructing new privilege instances.
     */
    public static final String PRIVILEGE_CLASS_PROPERTY = "com.sun.identity.entitlement.default.privilege.class";

    /**
     * Default privilege concrete class to use. We use the name rather than the class here so that we can perform
     * lazy initialisation (the OpenSSOPrivilege does a lot of stuff in static initialisers).
     */
    private static final String DEFAULT_PRIVILEGE_CLASS = "com.sun.identity.entitlement.opensso.OpenSSOPrivilege";

    /**
     * application index key
     */
    public static final String APPLICATION_ATTRIBUTE = "application";

    /**
     * application search attribute
     */
    public static final SearchAttribute APPLICATION_SEARCH_ATTRIBUTE = new SearchAttribute(APPLICATION_ATTRIBUTE, "ou");

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
     * Creation date index key
     */
    public static final SearchAttribute CREATION_DATE_SEARCH_ATTRIBUTE = new SearchAttribute(CREATION_DATE_ATTRIBUTE, "ou");

    /**
     * Last modified date index key
     */
    public static final String LAST_MODIFIED_DATE_ATTRIBUTE = "lastmodifieddate";

    /**
     * Last modified date index key
     */
    public static final SearchAttribute LAST_MODIFIED_DATE_SEARCH_ATTRIBUTE = new SearchAttribute(LAST_MODIFIED_DATE_ATTRIBUTE, "ou");

    /**
     * Name attribute name,
     */
    public static final String NAME_ATTRIBUTE = "name";

    /**
     * Name search attribute
     */
    public static final SearchAttribute NAME_SEARCH_ATTRIBUTE = new SearchAttribute(NAME_ATTRIBUTE, "ou");

    /**
     * Resource type uuid reference.
     */
    public static final String RESOURCE_TYPE_UUID_ATTRIBUTE = "resourceTypeUuid";

    /**
     * Resource type uuid reference.
     */
    public static final SearchAttribute RESOURCE_TYPE_UUID_SEARCH_ATTRIBUTE = new SearchAttribute(RESOURCE_TYPE_UUID_ATTRIBUTE, SMSEntry.ATTR_XML_KEYVAL);

    /**
     * Macro used in resource name
     */
    public static final String RESOURCE_MACRO_SELF = "$SELF";

    /**
     * Macro used in condition
     */
    public static final String RESOURCE_MACRO_ATTRIBUTE = "$ATTR";

    /**
     * Privilege description attribute name,
     */
    public static final String DESCRIPTION_ATTRIBUTE = "description";

    /**
     * Privilege description search attribute name,
     */
    public static final SearchAttribute DESCRIPTION_SEARCH_ATTRIBUTE = new SearchAttribute(DESCRIPTION_ATTRIBUTE, "ou");

    private static Class<? extends Privilege> privilegeClass;
    public static final NoSubject NOT_SUBJECT = new NoSubject();

    private boolean active = true;
    private String name;
    private String description;
    private Entitlement entitlement;
    private EntitlementSubject eSubject;
    private EntitlementCondition eCondition;
    private Set<ResourceAttribute> eResourceAttributes;
    private String resourceTypeUuid;

    private String createdBy;
    private String lastModifiedBy;
    private long creationDate;
    private long lastModifiedDate;
    private Set<String> applicationIndexes;

    /**
     * The declared {@code eSubject} / {@code eCondition} of the stored privilege, kept verbatim when
     * {@link #getInstance(JSONObject)} could not rebuild it because its class name was refused or
     * could not be loaded.
     * <p>
     * Without this the privilege would simply come back with the field {@code null}, and a null
     * subject matches <em>every</em> subject ({@link #doesSubjectMatch}) while a null condition is
     * unconditionally satisfied ({@link #doesConditionMatch}) - so an unresolvable class name would
     * turn the stored restriction into a policy that applies to everyone. This is the same failure
     * mode that {@code LogicalSubject}/{@code LogicalCondition} guard against for nested members,
     * one level up, and it is handled the same way: the privilege fails closed while the refusal
     * lasts, and the declaration is written back out by {@link #toMinimalJSONObject()} so that a
     * re-save does not quietly rewrite the policy into a weaker one and the next load refuses it
     * again.
     * <p>
     * Only the read path records a refusal. A privilege that is being written -
     * {@link #getNewInstance(JSONObject)}, or the legacy policy import - has nothing stored yet to
     * preserve, so it refuses the write instead.
     */
    private transient JSONObject unresolvedSubject;
    private transient JSONObject unresolvedCondition;


    static {
        String privilegeClassName = SystemPropertiesManager.get(PRIVILEGE_CLASS_PROPERTY, DEFAULT_PRIVILEGE_CLASS);
        try {
            privilegeClass = Class.forName(privilegeClassName).asSubclass(Privilege.class);
        } catch (ClassNotFoundException ex) {
            PolicyConstants.DEBUG.error("Privilege.<init>", ex);
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
            return privilegeClass.newInstance();
        } catch (InstantiationException ex) {
            throw new EntitlementException(1, ex);
        } catch (IllegalAccessException ex) {
            throw new EntitlementException(1, ex);
        }
    }

    public Privilege() {
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
        this.unresolvedSubject = null;
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
     * Sets the resource type uuid that this policy makes reference to.
     *
     * @param resourceTypeUuid
     *         the resource type uuid.
     */
    public void setResourceTypeUuid(final String resourceTypeUuid) {
        this.resourceTypeUuid = resourceTypeUuid;
    }

    /**
     * Retrieves the resource type uuid that is associated with this policy.
     *
     * @return the resource type uuid
     */
    public String getResourceTypeUuid() {
        return resourceTypeUuid;
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
     * @param normalisedResourceName The normalised resource name.
     * @param requestedResourceName The requested resource name.
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
        String normalisedResourceName,
        String requestedResourceName,
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
            PolicyConstants.DEBUG.error("Entitlement.toString()", joe);
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
        } else if (unresolvedSubject != null) {
            // Write the declaration back out unchanged: storing this privilege again must not turn
            // it into one that never had a subject, which the next load would apply to everyone.
            // The class is still never loaded initialised nor instantiated - only its name is
            // copied. See unresolvedSubject.
            jo.put("eSubject", unresolvedSubject);
        }

        if (eCondition != null) {
            JSONObject subjo = new JSONObject();
            subjo.put("className", eCondition.getClass().getName());
            subjo.put("state", eCondition.getState());
            jo.put("eCondition", subjo);
        } else if (unresolvedCondition != null) {
            jo.put("eCondition", unresolvedCondition);
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
        jo.put(RESOURCE_TYPE_UUID_ATTRIBUTE, resourceTypeUuid);

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

    public static Privilege getInstance(JSONObject jo) throws EntitlementException {
        String className = jo.optString("className");
        try {
            Privilege privilege = EntitlementClassResolver.newInstance(className, Privilege.class);
            privilege.name = jo.optString("name");
            privilege.active = Boolean.parseBoolean(jo.optString("active"));
            privilege.resourceTypeUuid = jo.optString(RESOURCE_TYPE_UUID_ATTRIBUTE);
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
            // A declared subject / condition that did not come back is one whose class name was
            // refused or could not be loaded. Record it, or the privilege silently loses the
            // restriction and starts applying to everyone: see unresolvedSubject.
            if ((privilege.eSubject == null) && jo.has("eSubject")) {
                privilege.unresolvedSubject = declaredMember(jo, "eSubject");
                PolicyConstants.DEBUG.error("Privilege.getInstance: privilege " + privilege.name
                    + " declares a subject whose class could not be resolved; it will deny");
            }
            if ((privilege.eCondition == null) && jo.has("eCondition")) {
                privilege.unresolvedCondition = declaredMember(jo, "eCondition");
                PolicyConstants.DEBUG.error("Privilege.getInstance: privilege " + privilege.name
                    + " declares a condition whose class could not be resolved; it will fail");
            }
            privilege.eResourceAttributes = getResourceAttributes(jo);
            privilege.init(jo);
            
            return privilege;
        } catch (InstantiationException ex) {
            PolicyConstants.DEBUG.error("Privilege.getInstance", ex);
        } catch (IllegalAccessException ex) {
            PolicyConstants.DEBUG.error("Privilege.getInstance", ex);
        } catch (ClassNotFoundException ex) {
            PolicyConstants.DEBUG.error("Privilege.getInstance", ex);
        } catch (JSONException ex) {
            PolicyConstants.DEBUG.error("Privilege.getInstance", ex);
        }
        return null;
    }

    /**
     * Returns the declaration stored under {@code key} so that it can be written back out verbatim,
     * falling back to an empty object which is unresolvable in exactly the same way - what matters
     * is that the next load refuses it again rather than seeing a privilege with no such
     * restriction at all.
     */
    private static JSONObject declaredMember(JSONObject jo, String key) {
        JSONObject declared = jo.optJSONObject(key);
        return (declared != null) ? declared : new JSONObject();
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
                ResourceAttribute ra = EntitlementClassResolver.newInstance(
                    json.getString("className"), ResourceAttribute.class);
                ra.setState(json.getString("state"));
                results.add(ra);
            } catch (InstantiationException ex) {
                PolicyConstants.DEBUG.error(
                    "Privilege.getResourceAttributes", ex);
            } catch (IllegalAccessException ex) {
                PolicyConstants.DEBUG.error(
                    "Privilege.getResourceAttributes", ex);
            } catch (ClassNotFoundException ex) {
                PolicyConstants.DEBUG.error(
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
        try {
            return newESubject(jo.getJSONObject("eSubject"));
        } catch (InstantiationException ex) {
            PolicyConstants.DEBUG.error("Privilege.getESubject", ex);
        } catch (IllegalAccessException ex) {
            PolicyConstants.DEBUG.error("Privilege.getESubject", ex);
        } catch (ClassNotFoundException ex) {
            PolicyConstants.DEBUG.error("Privilege.getESubject", ex);
        }
        return null;
    }


    private static EntitlementCondition getECondition(JSONObject jo)
        throws JSONException {
        if (!jo.has("eCondition")) {
            return null;
        }

        try {
            return newECondition(jo.getJSONObject("eCondition"));
        } catch (InstantiationException ex) {
            PolicyConstants.DEBUG.error("Privilege.getECondition", ex);
        } catch (IllegalAccessException ex) {
            PolicyConstants.DEBUG.error("Privilege.getECondition", ex);
        } catch (ClassNotFoundException ex) {
            PolicyConstants.DEBUG.error("Privilege.getECondition", ex);
        }
        return null;
    }

    private static EntitlementSubject newESubject(JSONObject sbj)
        throws JSONException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        EntitlementSubject eSubject = EntitlementClassResolver.newInstance(
            sbj.getString("className"), EntitlementSubject.class);
        eSubject.setState(sbj.getString("state"));
        return eSubject;
    }

    private static EntitlementCondition newECondition(JSONObject sbj)
        throws JSONException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        EntitlementCondition eCondition = EntitlementClassResolver.newInstance(
            sbj.getString("className"), EntitlementCondition.class);
        eCondition.setState(sbj.getString("state"));
        // Caching moved to #doesConditionMatch(..) method
        return eCondition;
    }

    /**
     * Rebuilds the declared subject of a privilege that is being <em>written</em>, refusing the
     * write outright when its class name cannot be resolved.
     * <p>
     * {@link #getInstance} records the refusal instead and denies while it lasts, because there the
     * privilege already exists in the store and dropping it would be its own kind of damage. On a
     * write there is nothing to preserve yet: a subject that silently came back {@code null} would
     * be stored as a privilege that never had one, and a privilege without a subject matches
     * everyone ({@link #doesSubjectMatch}). Mirrors the legacy policy path, where
     * {@code PrivilegeUtils.mapGenericSubject} throws for the same reason and with the same codes.
     *
     * @param jo the privilege declaration.
     * @return the declared subject, or {@link NoSubject} when none was declared.
     * @throws EntitlementException if the declared class is not a usable EntitlementSubject.
     */
    private static EntitlementSubject getDeclaredESubject(JSONObject jo)
        throws JSONException, EntitlementException {
        if (!jo.has("eSubject")) {
            return new NoSubject();
        }
        JSONObject sbj = jo.getJSONObject("eSubject");
        try {
            return newESubject(sbj);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            throw refused(sbj.optString("className"), EntitlementSubject.class, ex);
        }
    }

    /**
     * The condition counterpart of {@link #getDeclaredESubject}: a null condition is unconditionally
     * satisfied, so a declaration that could not be rebuilt must stop the write rather than be
     * stored as a privilege that never carried the constraint.
     *
     * @param jo the privilege declaration.
     * @return the declared condition, or {@code null} when none was declared.
     * @throws EntitlementException if the declared class is not a usable EntitlementCondition.
     */
    private static EntitlementCondition getDeclaredECondition(JSONObject jo)
        throws JSONException, EntitlementException {
        if (!jo.has("eCondition")) {
            return null;
        }
        JSONObject cond = jo.getJSONObject("eCondition");
        try {
            return newECondition(cond);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            throw refused(cond.optString("className"), EntitlementCondition.class, ex);
        }
    }

    /**
     * Maps a refusal to the error code the administrator already sees for the same refusal on the
     * legacy policy path ({@code PolicyCondition.getPolicyCondition},
     * {@code PrivilegeUtils.mapGenericSubject}), keeping the cause.
     *
     * @param className the class name that was refused.
     * @param expectedType the entitlement type it was expected to implement.
     * @param ex the refusal.
     * @return the exception to fail the write with.
     */
    private static EntitlementException refused(String className, Class<?> expectedType,
        ReflectiveOperationException ex) {
        if (ex instanceof EntitlementClassResolver.RejectedTypeException) {
            return new EntitlementException(EntitlementException.POLICY_CLASS_CAST_EXCEPTION,
                new String[]{className, expectedType.getName()}, ex);
        }
        if (ex instanceof ClassNotFoundException) {
            return new EntitlementException(EntitlementException.UNKNOWN_POLICY_CLASS,
                new String[]{className}, ex);
        }
        if (ex instanceof InstantiationException) {
            return new EntitlementException(EntitlementException.POLICY_CLASS_NOT_INSTANTIABLE,
                new String[]{className}, ex);
        }
        return new EntitlementException(EntitlementException.POLICY_CLASS_NOT_ACCESSIBLE,
            new String[]{className}, ex);
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

    protected SubjectDecision doesSubjectMatch(
        Subject adminSubject,
        String realm,
        Subject subject,
        String resourceName,
        Map<String, Set<String>> environment
    ) throws EntitlementException {
        SubjectDecision decision;

        if (unresolvedSubject != null) {
            // The stored privilege declares a subject whose class could not be resolved. A null
            // subject matches everyone, so evaluating as if none had been written would hand out
            // exactly what the declaration restricts: deny while the refusal lasts.
            PolicyConstants.DEBUG.error("Privilege.doesSubjectMatch: denying, privilege " + name
                + " declares a subject whose class could not be resolved");
            return new SubjectDecision(false, Collections.<String, Set<String>>emptyMap());
        }

        if (getSubject() != null) {
            SubjectAttributesManager mgr = SubjectAttributesManager.getInstance(adminSubject, realm);
            decision = getSubject().evaluate(realm, mgr, subject, resourceName, environment);
        } else {
            decision = new SubjectDecision(true, Collections.<String, Set<String>>emptyMap());
        }

        if (PolicyConstants.DEBUG.messageEnabled()) {
            if (decision.isSatisfied()) {
                PolicyConstants.DEBUG.message("[PolicyEval] Privilege.doesSubjectMatch: true");
            } else {
                PolicyConstants.DEBUG.message("[PolicyEval] Privilege.doesSubjectMatch: false");
                PolicyConstants.DEBUG.message("[PolicyEval] Advices: " + decision.getAdvices());
            }
        }
        return decision;
    }

    protected ConditionDecision doesConditionMatch(
        String realm,
        Subject subject,
        String resourceName,
        Map<String, Set<String>> environment
    ) throws EntitlementException {
        ConditionDecision decision;

        if (unresolvedCondition != null) {
            // Same one level up from the logical wrappers: a null condition is unconditionally
            // satisfied, so a declaration that could not be rebuilt must fail rather than vanish.
            PolicyConstants.DEBUG.error("Privilege.doesConditionMatch: failing, privilege " + name
                + " declares a condition whose class could not be resolved");
            return ConditionDecision.newFailureBuilder().build();
        }

        if (eCondition != null) {
            EntitlementCondition cachedCondition = new CachingEntitlementCondition(eCondition);
            decision = cachedCondition.evaluate(realm, subject, resourceName, environment);
        } else {
            decision = ConditionDecision.newSuccessBuilder().build();
        }

        if (PolicyConstants.DEBUG.messageEnabled()) {
            if (decision.isSatisfied()) {
                PolicyConstants.DEBUG.message("[PolicyEval] Privilege.doesConditionMatch: true");
            } else {
                PolicyConstants.DEBUG.message("[PolicyEval] Privilege.doesConditionMatch: false");
                PolicyConstants.DEBUG.message("[PolicyEval] Advices: " + decision.getAdvice());
            }
        }

        return decision;
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
            throw new EntitlementException(EntitlementException.NULL_ENTITLEMENT);
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
        this.unresolvedCondition = null;
    }

    /**
     * Returns whether the stored privilege declares a subject whose class could not be resolved, in
     * which case {@link #doesSubjectMatch} denies instead of matching everyone.
     *
     * @return <code>true</code> if the declared subject could not be rebuilt.
     */
    boolean isSubjectUnresolved() {
        return unresolvedSubject != null;
    }

    /**
     * Returns whether the stored privilege declares a condition whose class could not be resolved,
     * in which case {@link #doesConditionMatch} fails instead of being unconditionally satisfied.
     *
     * @return <code>true</code> if the declared condition could not be rebuilt.
     */
    boolean isConditionUnresolved() {
        return unresolvedCondition != null;
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
            this.eResourceAttributes = new LinkedHashSet<ResourceAttribute>(set);
        }
    }

    protected Map<String, Set<String>> getAttributes(Subject adminSubject, 
        String realm, Subject subject, String resourceName,
        Map<String, Set<String>> environment
    ) throws EntitlementException {
        Map<String, Set<String>> result = new HashMap<>();

        if ((eResourceAttributes != null) && !eResourceAttributes.isEmpty()) {
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
            // Unlike getInstance(..) this is a write path - the privilege is being created or
            // updated - so an unresolvable class name refuses the write instead of being recorded:
            // see getDeclaredESubject.
            privilege.eSubject = getDeclaredESubject(jo);
            privilege.eCondition = getDeclaredECondition(jo);
            // Validate the privilege condition when creating a new instance
            if (privilege.eCondition != null) {
                privilege.eCondition.validate();
            }
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

