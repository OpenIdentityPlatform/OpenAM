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
 * $Id: LogicalSubject.java,v 1.1 2009/08/19 05:40:33 veiming Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 * Portions Copyright 2026 3A Systems, LLC.
 */

package com.sun.identity.entitlement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.forgerock.openam.entitlement.PolicyConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Base class for <code>OrSubejct</code> and <code>AndSubejct</code>.
 */
public abstract class LogicalSubject implements EntitlementSubject {
    private Set<EntitlementSubject> eSubjects;
    private String pSubjectName;

    /**
     * Set when {@link #setState(String)} refused a member class name. The wrapper is then an
     * incomplete representation of the stored policy, and subclasses whose semantics get weaker as
     * members are removed (notably {@code AndSubject}, which is satisfied by an empty member set)
     * must not evaluate as if the missing constraint had never been written.
     */
    private transient boolean memberRejected;

    /**
     * The refused members, kept verbatim as they appeared in the state read by
     * {@link #setState(String)} so that {@link #toJSONObject()} can write them back out.
     * <p>
     * Without this the refusal would live only as long as the in-memory object: the emitted state
     * would carry the surviving members only, so a re-save (for instance
     * {@code ResavePoliciesStep}, which re-reads and re-stores every policy of every realm) would
     * persist the truncated policy, and the next load would see a wrapper that is indistinguishable
     * from a legitimately smaller one - for {@code AndSubject} an empty member set means
     * <em>satisfied</em>, i.e. a grant to everyone. Re-emitting the refused entry makes the next
     * load hit the same refusal and set {@link #memberRejected} again.
     * <p>
     * The refused member is only ever copied as JSON; its class is still never loaded initialised
     * nor instantiated.
     */
    private transient List<JSONObject> rejectedMembers = new ArrayList<JSONObject>();

    /**
     * Constructor.
     */
    public LogicalSubject() {
    }

    /**
     * Constructor.
     *
     * @param eSubjects wrapped EntitlementSubject(s)
     */
    public LogicalSubject(Set<EntitlementSubject> eSubjects) {
        this.eSubjects = eSubjects;
    }

    /**
     * Constructor.
     *
     * @param eSubjects wrapped EntitlementSubject(s)
     * @param pSubjectName subject name as used in OpenAM policy,
     * this is relevant only when UserESubject was created from
     * OpenAM policy Subject
     */
    public LogicalSubject(
        Set<EntitlementSubject> eSubjects,
        String pSubjectName
    ) {
        this.eSubjects = eSubjects;
        this.pSubjectName = pSubjectName;
    }

    /**
     * Sets state of the object
     * @param state State of the object encoded as string
     */
    public void setState(String state) {
        try {
            JSONObject jo = new JSONObject(state);
            JSONArray memberSubjects = jo.optJSONArray("memberESubjects");
            // Reset outside the null check: the state string being applied fully redefines this
            // object, so a prior refusal must not leak into it (mirrors setESubjects).
            clearMemberRejection();
            if(memberSubjects != null) {
                eSubjects = new HashSet<EntitlementSubject>();
                int len = memberSubjects.length();
                for (int i = 0; i < len; i++) {
                    JSONObject memberSubject = memberSubjects.optJSONObject(i);
                    try {
                        // Read className and state inside the per-member try as well: a member
                        // missing either key has to be refused like any other unusable one. Letting
                        // the JSONException out of the loop would abort the remaining members and
                        // record no refusal at all, which is the truncated-to-empty (i.e. satisfied)
                        // AndSubject this class exists to prevent.
                        if (memberSubject == null) {
                            throw new JSONException("member subject " + i + " is not an object");
                        }
                        String className = memberSubject.getString("className");
                        EntitlementSubject es = EntitlementClassResolver.newInstance(
                            className, EntitlementSubject.class);
                        es.setState(memberSubject.getString("state"));
                        eSubjects.add(es);
                    } catch (JSONException | ClassNotFoundException | InstantiationException
                            | IllegalAccessException e) {
                        // Skip only the offending member instead of aborting the whole list, so a
                        // single rejected class name cannot silently drop the valid siblings, and
                        // record the refusal so evaluation cannot treat the missing member as if it
                        // had never been part of the policy.
                        markMemberRejected(memberSubject);
                        PolicyConstants.DEBUG.error("LogicalSubject.setState: skipping invalid "
                            + "member subject "
                            + ((memberSubject != null) ? memberSubject.optString("className") : ""), e);
                    }
                }
            }
            if (jo.optString("pSubjectName").length() > 0) {
                pSubjectName = jo.optString("pSubjectName");
            } else {
                pSubjectName = null;
            }
        } catch (JSONException e) {
            PolicyConstants.DEBUG.error("LogicalSubject.setState", e);
        }
    }

    /**
     * Returns state of the object
     * @return state of the object encoded as string
     */
    public String getState() {
        return toString();
    }

    /**
     * Sets the nested EntitlementSubject(s)
     * @param eSubjects the nested EntitlementSubject(s)
     */
    public void setESubjects(Set<EntitlementSubject> eSubjects) {
        this.eSubjects = eSubjects;
        clearMemberRejection();
    }

    /**
     * Records that a member could not be rebuilt from the state being applied, keeping the member's
     * JSON so that {@link #toJSONObject()} can write it back out. Subclasses that read the member
     * themselves - {@code NotSubject} keeps a single member of its own - have to call this, or a
     * refusal below them stays invisible to {@link #hasRejectedMemberInSubtree()} and an enclosing
     * {@code NOT} negates the resulting deny back into a match.
     *
     * @param rejectedMember the refused member as it appeared in the state, or <code>null</code>
     *        when it was not even a JSON object; an empty object is then re-emitted in its place,
     *        which the next load refuses again and so keeps the wrapper fail-closed across a save.
     */
    protected void markMemberRejected(JSONObject rejectedMember) {
        memberRejected = true;
        rejectedMembers.add((rejectedMember != null) ? rejectedMember : new JSONObject());
    }

    /**
     * Clears a recorded refusal. Called whenever the members are redefined wholesale, so that
     * programmatic construction is unaffected by what a previous state string contained.
     */
    protected void clearMemberRejection() {
        memberRejected = false;
        rejectedMembers.clear();
    }

    /**
     * Returns the refused members, kept verbatim for re-emission by {@link #toJSONObject()}.
     *
     * @return the refused members; never <code>null</code>.
     */
    @JsonIgnore
    protected List<JSONObject> getRejectedMembers() {
        return rejectedMembers;
    }

    /**
     * Returns whether {@link #setState(String)} refused a member class name, leaving this wrapper
     * with fewer members than the stored policy declares.
     * <p>
     * Any subclass whose evaluation gets <em>weaker</em> as members are dropped must consult this
     * before evaluating: {@code AndSubject} is satisfied by an empty member set, so evaluating the
     * truncated set would grant what the stored policy restricts. {@code OrSubject} denies on an
     * empty set and only gets stricter as members are dropped, so it needs no guard. A subclass
     * that <em>negates</em> its member must use {@link #hasRejectedMemberInSubtree()} instead.
     *
     * @return <code>true</code> if at least one member was rejected.
     */
    @JsonIgnore
    public boolean isMemberRejected() {
        return memberRejected;
    }

    /**
     * Returns whether this wrapper, or any logical subject nested below it, had a member class name
     * refused by {@link #setState(String)}.
     * <p>
     * {@link #isMemberRejected()} deliberately reports this wrapper's own refusal only: for
     * {@code AndSubject}/{@code OrSubject} a refusal further down is already handled where it
     * happened, because the damaged member denies and a denying member can only make an AND or an
     * OR stricter. Negation is the exception - {@code NotSubject} turns its member's decision
     * around, so the damaged member's deny would come back out of the {@code NOT} as a match. It
     * has to look at the whole subtree.
     *
     * @return <code>true</code> if a member was refused anywhere in this subtree.
     */
    @JsonIgnore
    public boolean hasRejectedMemberInSubtree() {
        if (memberRejected) {
            return true;
        }

        // Read the members through the accessor: NotSubject keeps its single member elsewhere and
        // overrides the getter, so the field would not see it.
        Set<EntitlementSubject> members = getESubjects();
        if (members != null) {
            for (EntitlementSubject member : members) {
                if (member instanceof LogicalSubject
                        && ((LogicalSubject) member).hasRejectedMemberInSubtree()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the nested EntitlementSubject(s)
     * @return  the nested EntitlementSubject(s)
     */
    public Set<EntitlementSubject> getESubjects() {
        return eSubjects;
    }

    /**
     * Sets OpenAM policy Subject name
     * @param pSubjectName subject name as used in OpenAM policy,
     * this is relevant only when UserESubject was created from
     * OpenAM policy Subject
     */
    public void setPSubjectName(String pSubjectName) {
        this.pSubjectName = pSubjectName;
    }

    /**
     * Returns OpenAM policy Subject name
     * @return  subject name as used in OpenAM policy,
     * this is relevant only when UserESubject was created from
     * OpenAM policy Subject
     */
    public String getPSubjectName() {
        return pSubjectName;
    }

    /**
     * Returns JSONObject mapping of the object
     * @return JSONObject mapping of the object
     * @throws org.json.JSONException if can not map to JSONObject
     */
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();
        if (pSubjectName != null) {
            jo.put("pSubjectName", pSubjectName);
        }
        if (eSubjects != null) {
            for (EntitlementSubject eSubject : eSubjects) {
                JSONObject subjo = new JSONObject();
                subjo.put("className", eSubject.getClass().getName());
                subjo.put("state", eSubject.getState());
                jo.append("memberESubjects", subjo);
            }
        }
        // Write the refused members back out unchanged, so that storing this object again keeps
        // the policy as it was written and the next load refuses them again instead of seeing a
        // wrapper that looks legitimately smaller. See rejectedMembers.
        for (JSONObject rejected : rejectedMembers) {
            jo.append("memberESubjects", rejected);
        }
        return jo;
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
        } catch (JSONException e) {
            PolicyConstants.DEBUG.error("LogicalSubject.toString", e);
        }
        return s;
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
        LogicalSubject object = (LogicalSubject) obj;
        if (eSubjects == null) {
            if (object.getESubjects() != null) {
                return false;
            }
        } else { // eSubjects not null
            if ((object.getESubjects()) == null) {
                return false;
            } else if (!eSubjects.containsAll(object.getESubjects())) {
                return false;
            } else if (!object.getESubjects().containsAll(eSubjects)) {
                return false;
            }
        }
        if (pSubjectName == null) {
            if (object.getPSubjectName() != null) {
                return false;
            }
        } else {
            if (!pSubjectName.equals(object.getPSubjectName())) {
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
        if (eSubjects != null) {
            for (EntitlementSubject eSubject : eSubjects) {
                code += eSubject.hashCode();
            }
        }
        if (pSubjectName != null) {
            code += pSubjectName.hashCode();
        }
        return code;
    }

    /**
     * Returns the search index attributes.
     *
     * @return the search index attributes.
     */
    public Map<String, Set<String>> getSearchIndexAttributes() {
        Map<String, Set<String>> results = new HashMap<String, Set<String>>();
        if (eSubjects == null) {
            return results;
        }
        
        for (EntitlementSubject e : eSubjects) {
            Map<String, Set<String>> map = e.getSearchIndexAttributes();
            if ((map != null) && !map.isEmpty()) {
                for (String s : map.keySet()) {
                    Set<String> val = map.get(s);

                    if (s.equals(SubjectAttributesCollector.NAMESPACE_IDENTITY)
                        &&
                        val.contains(
                        SubjectAttributesCollector.ATTR_NAME_ALL_ENTITIES)) {
                        return createSearchAllIndexMap();
                    }

                    Set<String> set = results.get(s);
                    if (set == null) {
                        set = new HashSet<String>();
                        results.put(s, set);
                    }
                    set.addAll(val);
                }
            }
        }
        return results;
    }

    private Map<String, Set<String>> createSearchAllIndexMap() {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add(SubjectAttributesCollector.ATTR_NAME_ALL_ENTITIES);
        map.put(SubjectAttributesCollector.NAMESPACE_IDENTITY, set);
        return map;
    }

    /**
     * Returns the required attribute names.
     *
     * @return the required attribute names.
     */
    public Set<String> getRequiredAttributeNames() {
        Set<String> results = new HashSet<String>();
        if (eSubjects == null) {
            return results;
        }
        for (EntitlementSubject e : eSubjects) {
            results.addAll(e.getRequiredAttributeNames());
        }
        return results;
    }

   /**
     * Returns <code>true</code> is this subject is an identity object.
     *
     * @return <code>true</code> is this subject is an identity object.
     */
    public boolean isIdentity() {
        if ((eSubjects != null) && !eSubjects.isEmpty()) {
            for (EntitlementSubject e : eSubjects) {
                if (e.isIdentity()) {
                    return true;
                }
            }
        }
        return false;
    }
}
