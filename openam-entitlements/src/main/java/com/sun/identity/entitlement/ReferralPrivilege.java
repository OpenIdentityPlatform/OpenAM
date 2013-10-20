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
 * $Id: ReferralPrivilege.java,v 1.7 2010/01/08 23:59:31 veiming Exp $
 */

/*
 * Portions Copyrighted 2010-2011 ForgeRock AS
 */

package com.sun.identity.entitlement;

import com.sun.identity.entitlement.interfaces.ResourceName;
import com.sun.identity.shared.JSONUtils;
import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.util.DN;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Referral privilege allows application to be referred to peer and sub realm.
 */
public final class ReferralPrivilege implements IPrivilege, Cloneable {
    private String name;
    private String description;
    private Map<String, Set<String>> mapApplNameToResources;
    private Map<String, Set<String>> origMapApplNameToResources;
    private Set<String> realms;
    private long creationDate;
    private long lastModifiedDate;
    private String lastModifiedBy;
    private String createdBy;
    private boolean active = true;

    private ReferralPrivilege() {
    }

    /**
     * Constructor
     *
     * @param name Name
     * @param map Map of application name to resources.
     * @param realms Realm names
     * @throws EntitlementException if map or realms are empty.
     */
    public ReferralPrivilege(
        String name,
        Map<String, Set<String>> map,
        Set<String> realms
    ) throws EntitlementException {
        if ((name == null) || (name.trim().length() == 0)) {
            throw new EntitlementException(250);
        }

        this.name = name;
        setMapApplNameToResources(map);
        setRealms(realms);
    }

    @Override
    public Object clone() {
        ReferralPrivilege clone = new ReferralPrivilege();
        clone.name = name;
        clone.description = description;

        if (realms != null) {
            clone.realms = new HashSet<String>();
            clone.realms.addAll(realms);
        }

        clone.creationDate = creationDate;
        clone.lastModifiedDate = lastModifiedDate;
        clone.lastModifiedBy = lastModifiedBy;
        clone.createdBy = createdBy;

        if (mapApplNameToResources != null) {
            clone.mapApplNameToResources = new HashMap<String, Set<String>>();
            for (String k : mapApplNameToResources.keySet()) {
                Set<String> s = new HashSet<String>();
                s.addAll(mapApplNameToResources.get(k));
                clone.mapApplNameToResources.put(k, s);
            }
        }

        return clone;
    }

    public static ReferralPrivilege getInstance(JSONObject jo) {
        try {
            ReferralPrivilege r = new ReferralPrivilege();
            r.name = jo.optString("name");
            r.description = jo.optString("description");
            if (jo.has("createdBy")) {
                r.createdBy = jo.getString("createdBy");
            }
            if (jo.has("lastModifiedBy")) {
                r.lastModifiedBy = jo.getString("lastModifiedBy");
            }
            r.creationDate = JSONUtils.getLong(jo, "creationDate");
            r.lastModifiedDate = JSONUtils.getLong(jo, "lastModifiedDate");
            r.mapApplNameToResources = JSONUtils.getMapStringSetString(jo,
                "mapApplNameToResources");
            r.origMapApplNameToResources = JSONUtils.getMapStringSetString(jo,
                "origMapApplNameToResources");
            r.realms = JSONUtils.getSet(jo, "realms");
            return r;
        } catch (JSONException ex) {
            PrivilegeManager.debug.error("ReferralPrivilege.getInstance", ex);
        }
        return null;
    }

    /**
     * Sets the application name to resource name.
     *
     * @param map map of application name to tesource names.
     * @throws EntitlementException if map is empty.
     */
    public void setMapApplNameToResources(Map<String, Set<String>> map)
        throws EntitlementException {

        // map would be null, when a referral policy is created without rule
        // see issue 5291
        if (map != null) {
            for (String k : map.keySet()) {
                Set<String> v = map.get(k);
                if ((v == null) || v.isEmpty()) {
                    throw new EntitlementException(251);
                }
            }
        }

        this.mapApplNameToResources = new HashMap<String, Set<String>>();
        if (map != null) {
            this.mapApplNameToResources.putAll(map);
        }
    }

    /**
     * Sets realms.
     *
     * @param realms Realms.
     * @throws EntitlementException if realms is empty.
     */
    public void setRealms(Set<String> realms)
        throws EntitlementException {
        this.realms = new HashSet<String>();
        if ((realms != null) && !realms.isEmpty()) {
            // Issue 5219
            this.realms.addAll(realms);
        }
    }

    /**
     * Returns mapping of application name to resources.
     *
     * @return mapping of application name to resources.
     */
    public Map<String, Set<String>> getMapApplNameToResources() {
        return deepCopyMap(mapApplNameToResources);
    }

    private static Map<String, Set<String>> deepCopyMap(
        Map<String, Set<String>> map) {
        Map<String, Set<String>> result = new HashMap<String, Set<String>>();
        for (String k : map.keySet()) {
            Set<String> set = new HashSet<String>();
            set.addAll(map.get(k));
            result.put(k, set);
        }

        return result;
    }

    /**
     * Returns non canonicalized mapping of application name to resources.
     *
     * @return mapping of application name to resources.
     */
    public Map<String, Set<String>> getOriginalMapApplNameToResources() {
        return (origMapApplNameToResources != null) ?
            deepCopyMap(origMapApplNameToResources) :
            deepCopyMap(mapApplNameToResources);
    }

    /**
     * Returns name.
     *
     * @return name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets description.
     *
     * @param description
     */
    public void setDescription(String description) {
        this.description = description;
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
     * Returns realms.
     *
     * @return realms
     */
    public Set<String> getRealms() {
        Set<String> set = new HashSet<String>();
        set.addAll(realms);
        return set;
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

        for (String app : mapApplNameToResources.keySet()) {
            Application appl = ApplicationManager.getApplication(
                PrivilegeManager.superAdminSubject, realm, app);
            for (String r : mapApplNameToResources.get(app)) {
                ResourceSaveIndexes rsi = appl.getResourceSaveIndex(r);
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

    public String toXML() {
        return toJSON();
    }

    public String toJSON() {
        JSONObject jo = new JSONObject();

        try {
            jo.put("name", name);
            jo.put("description", description);
            jo.put("createdBy", createdBy);
            jo.put("lastModifiedBy", lastModifiedBy);
            jo.put("creationDate", creationDate);
            jo.put("lastModifiedDate", lastModifiedDate);

            jo.put("mapApplNameToResources", mapApplNameToResources);
            if (origMapApplNameToResources != null) {
                jo.put("origMapApplNameToResources",
                    origMapApplNameToResources);
            }
            jo.put("realms", realms);
            return jo.toString(2);
        } catch (JSONException ex) {
            PrivilegeManager.debug.error("ReferralPrivilege.toJSON", ex);
        }
        return "";
    }

    /**
     * Canonicalizes resource name before persistence.
     *
     * @param adminSubject Admin Subject.
     * @param realm Realm Name
     */
    public void canonicalizeResources(Subject adminSubject, String realm)
        throws EntitlementException {
        origMapApplNameToResources = deepCopyMap(mapApplNameToResources);
        for (String appName : mapApplNameToResources.keySet()) {
            ResourceName resComp = getResourceComparator(adminSubject, realm,
                appName);
            Set<String> resources = mapApplNameToResources.get(appName);
            Set<String> temp = new HashSet<String>();
            for (String r : resources) {
                temp.add(resComp.canonicalize(r));
            }
            mapApplNameToResources.put(appName, temp);
        }
    }

    private ResourceName getResourceComparator(
        Subject adminSubject,
        String realm,
        String applName) throws EntitlementException {
        Application appl = ApplicationManager.getApplication(
            PrivilegeManager.superAdminSubject, realm, applName);
        return appl.getResourceComparator();
    }

    public List<Entitlement> evaluate(
        Subject adminSubject,
        String realm,
        Subject subject,
        String applicationName,
        String resourceName,
        Set<String> actionNames,
        Map<String, Set<String>> environment,
        boolean recursive,
        Object context
    ) throws EntitlementException {
        List<Entitlement> results = null;

        if (!active) {
            return Collections.EMPTY_LIST;
        }

        Application application =
            ApplicationManager.getApplication(
            PrivilegeManager.superAdminSubject, realm, applicationName);
        EntitlementCombiner entitlementCombiner =
            application.getEntitlementCombiner();
        entitlementCombiner.init("/", applicationName, resourceName, actionNames, recursive);
        for (String rlm : realms) {
            EntitlementConfiguration ec = EntitlementConfiguration.getInstance(
                PrivilegeManager.superAdminSubject, rlm);
            if (ec.doesRealmExist()) {
                for (String app : mapApplNameToResources.keySet()) {
                    if (app.equals(applicationName)) {
                        Set<String> resourceNames = mapApplNameToResources.get(
                            app);
                        ResourceName comp = getResourceComparator(adminSubject,
                            rlm,
                            app);
                        String resName = comp.canonicalize(resourceName).
                            toLowerCase();
                        Set<String> resources = tagswapResourceNames(subject,
                            resourceNames);

                        boolean applicable = false;
                        for (String r : resources) {
                            ResourceMatch match = comp.compare(resName,
                                comp.canonicalize(r), true);
                            if (!recursive) {
                                applicable = match.equals(
                                    ResourceMatch.EXACT_MATCH) ||
                                    match.equals(ResourceMatch.WILDCARD_MATCH) ||
                                    match.equals(
                                    ResourceMatch.SUB_RESOURCE_MATCH) ||
                                    match.equals(ResourceMatch.SUPER_RESOURCE_MATCH);
                            } else {
                                applicable = !match.equals(
                                    ResourceMatch.NO_MATCH);
                            }
                            if (applicable) {
                                break;
                            }
                        }

                        if (applicable) {
                            PrivilegeEvaluator evaluator = new PrivilegeEvaluator();

                            // create subject for sub realm by copying subject for
                            // this realm and clear the public credentials.
                            // this needs to be revisited later if public
                            // credentials contains realm-independent credentals
                            Subject subjectSubRealm = new Subject(false,
                                subject.getPrincipals(), new HashSet(),
                                subject.getPrivateCredentials());

                            // Fix for OPENAM-790
                            // Ensure that the Entitlement environment contains the correct 
                            // Policy Configuration for the realm being evaluated.
                            Map savedConfig = ec.updatePolicyConfigForSubRealm(environment, rlm);
                            
                            List<Entitlement> entitlements = evaluator.evaluate(
                                rlm,
                                adminSubject, subjectSubRealm, applicationName,
                                resName, environment, recursive);
                            
                            if (savedConfig != null) {
                                ec.restoreSavedPolicyConfig(environment, savedConfig);
                            }
                            
                            if (entitlements != null) {
                                entitlementCombiner.add(entitlements);
                                results = entitlementCombiner.getResults();
                            }
                        }
                    }
                }
            }
        }
        
        if ( results == null ) {
        	results = new ArrayList<Entitlement>(0);
        }
        return results;
    }

    private Set<String> tagswapResourceNames(Subject sbj, Set<String> set)
        throws EntitlementException {
        Set<String> resources = new HashSet<String>();
        Set<String> userIds = new HashSet<String>();

        if (sbj != null) {
            Set<Principal> principals = sbj.getPrincipals();
            if (!principals.isEmpty()) {
                for (Principal p : principals) {
                    String pName = p.getName();
                    if (DN.isDN(pName)) {
                        String[] rdns = LDAPDN.explodeDN(pName, true);
                        userIds.add(rdns[0]);
                    } else {
                        userIds.add(pName);
                    }
                }
            }
        }

        if (!userIds.isEmpty()) {
            for (String r : set) {
                for (String uid : userIds) {
                    resources.add(r.replaceAll("\\$SELF", uid));
                }
            }
        } else {
            resources.addAll(set);
        }

        return resources;
    }

    public Set<String> getApplicationTypeNames(
        Subject adminSubject,
        String realm
    ) throws EntitlementException {
        Set<String> results = new HashSet<String>();
        for (String a : mapApplNameToResources.keySet()) {
            Application appl = ApplicationManager.getApplication(
                PrivilegeManager.superAdminSubject, realm, a);
            results.add(appl.getApplicationType().getName());
        }
        return results;
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
}
