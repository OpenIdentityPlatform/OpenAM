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
 * $Id: LogicalCondition.java,v 1.1 2009/08/19 05:40:33 veiming Exp $
 */
/*
 * Portions Copyrighted 2014-2015 ForgeRock AS.
 * Portions Copyrighted 2026 3A Systems, LLC
 */
package com.sun.identity.entitlement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.forgerock.openam.entitlement.PolicyConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.Subject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class LogicalCondition extends EntitlementConditionAdaptor {
    private Set<EntitlementCondition> eConditions;
    private String pConditionName;

    /**
     * Set when {@link #setState(String)} refused a member class name. The wrapper is then an
     * incomplete representation of the stored policy, and subclasses whose semantics get weaker as
     * members are removed (notably {@code AndCondition}, which is satisfied by an empty member set)
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
     * from a legitimately smaller one - for {@code AndCondition} and for an emptied
     * {@code OrCondition} an empty member set means <em>satisfied</em>, i.e. a grant. Re-emitting
     * the refused entry makes the next load hit the same refusal and set {@link #memberRejected}
     * again.
     * <p>
     * The refused member is only ever copied as JSON; its class is still never loaded initialised
     * nor instantiated.
     */
    private transient List<JSONObject> rejectedMembers = new ArrayList<JSONObject>();

    /**
     * Constructor.
     */
    public LogicalCondition() {
    }

    /**
     * Constructor.
     *
     * @param eConditions wrapped <code>EntitlementCondition</code>(s)
     */
    public LogicalCondition(Set<EntitlementCondition> eConditions) {
        setEConditions(eConditions);
    }

    /**
     * Constructor.
     *
     * @param eConditions wrapped <code>EntitlementCondition</code>(s)
     * @param pConditionName subject name as used in OpenAM policy,
     * this is relevant only when UserECondition was created from
     * OpenAM policy Condition
     */
    public LogicalCondition(
        Set<EntitlementCondition> eConditions,
        String pConditionName
    ) {
        this.pConditionName = pConditionName;
        setEConditions(eConditions);
    }

    /**
     * Sets state of the object
     *
     * @param state State of the object encoded as string
     */
    public void setState(String state) {
        try {
            JSONObject jo = new JSONObject(state);
            setState(jo);
            pConditionName = (jo.has("pConditionName")) ?
                jo.optString("pConditionName") : null;
            JSONArray memberConditions = jo.optJSONArray("memberECondition");
            // Reset outside the null check: the state string being applied fully redefines this
            // object, so a prior refusal must not leak into it (mirrors setEConditions).
            clearMemberRejection();
            if (memberConditions != null) {
                eConditions = new HashSet<EntitlementCondition>();
                int len = memberConditions.length();
                for (int i = 0; i < len; i++) {
                    JSONObject memberCondition =
                        memberConditions.optJSONObject(i);
                    try {
                        // Read className and state inside the per-member try as well: a member
                        // missing either key has to be refused like any other unusable one. Letting
                        // the JSONException out of the loop would abort the remaining members and
                        // record no refusal at all, which is the truncated-to-empty (i.e. satisfied)
                        // AndCondition this class exists to prevent.
                        if (memberCondition == null) {
                            throw new JSONException("member condition " + i + " is not an object");
                        }
                        String className = memberCondition.getString("className");
                        EntitlementCondition ec = EntitlementClassResolver.newInstance(
                            className, EntitlementCondition.class);
                        ec.setState(memberCondition.getString("state"));
                        eConditions.add(ec);
                    } catch (JSONException | ClassNotFoundException | InstantiationException
                            | IllegalAccessException ex) {
                        // Skip only the offending member instead of aborting the whole list, so a
                        // single rejected class name cannot silently drop the valid siblings, and
                        // record the refusal so evaluation cannot treat the missing member as if it
                        // had never been part of the policy.
                        markMemberRejected(memberCondition);
                        PolicyConstants.DEBUG.error("LogicalCondition.setState: skipping invalid "
                            + "member condition "
                            + ((memberCondition != null) ? memberCondition.optString("className") : ""), ex);
                    }
                }
            }
        } catch (JSONException ex) {
            PolicyConstants.DEBUG.error("LogicalCondition.setState", ex);
        }
    }

    /**
     * Returns state of the object.
     *
     * @return state of the object encoded as string.
     */
    public String getState() {
        return toString();
    }

    /**
     * Returns <code>ConditionDecision</code> of
     * <code>EntitlementCondition</code> evaluation
     *
     * @param realm Realm name.
     * @param subject EntitlementCondition who is under evaluation.
     * @param resourceName Resource name.
     * @param environment Environment parameters.
     * @return <code>ConditionDecision</code> of
     * <code>EntitlementCondition</code> evaluation
     * @throws EntitlementException if error occurs.
     */
    public abstract ConditionDecision evaluate(
        String realm,
        Subject subject,
        String resourceName,
        Map<String, Set<String>> environment
    ) throws EntitlementException;

    /**
     * Sets the nested <code>EntitlementCondition</code>(s).
     *
     * @param eConditions the nested <code>EntitlementCondition</code>(s)
     */
    public void setEConditions(Set<EntitlementCondition> eConditions) {
        this.eConditions = new HashSet<EntitlementCondition>();
        clearMemberRejection();
        if (eConditions != null) {
            this.eConditions.addAll(eConditions);
        }
    }

    /**
     * Records that a member could not be rebuilt from the state being applied, keeping the member's
     * JSON so that {@link #toJSONObject()} can write it back out. Subclasses that read the member
     * themselves - {@code NotCondition} keeps a single member of its own - have to call this, or a
     * refusal below them stays invisible to {@link #hasRejectedMemberInSubtree()} and an enclosing
     * {@code NOT} negates the resulting failure back into a grant.
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
     * before evaluating: an empty member set is satisfied both in {@code AndCondition} and in
     * {@code OrCondition}, so the truncated wrapper would grant what the stored policy restricts.
     * {@code AndCondition} has to fail on any refusal, {@code OrCondition} only when nothing
     * survived - dropping a member from a non-empty OR can only make it stricter. A subclass that
     * <em>negates</em> its member must use {@link #hasRejectedMemberInSubtree()} instead.
     *
     * @return <code>true</code> if at least one member was rejected.
     */
    @JsonIgnore
    public boolean isMemberRejected() {
        return memberRejected;
    }

    /**
     * Returns whether this wrapper, or any logical condition nested below it, had a member class
     * name refused by {@link #setState(String)}.
     * <p>
     * {@link #isMemberRejected()} deliberately reports this wrapper's own refusal only: for
     * {@code AndCondition}/{@code OrCondition} a refusal further down is already handled where it
     * happened, because the damaged member fails closed and a failing member can only make an AND
     * or an OR stricter. Negation is the exception - {@code NotCondition} turns its member's
     * decision around, so the damaged member's fail-closed decision would come back out of the
     * {@code NOT} as a grant. It has to look at the whole subtree.
     *
     * @return <code>true</code> if a member was refused anywhere in this subtree.
     */
    @JsonIgnore
    public boolean hasRejectedMemberInSubtree() {
        if (memberRejected) {
            return true;
        }

        // Read the members through the accessor: NotCondition keeps its single member elsewhere
        // and overrides the getter, so the field would not see it.
        Set<EntitlementCondition> members = getEConditions();
        if (members != null) {
            for (EntitlementCondition member : members) {
                if (member instanceof LogicalCondition
                        && ((LogicalCondition) member).hasRejectedMemberInSubtree()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the nested <code>EntitlementCondition</code>(s).
     *
     * @return the nested <code>EntitlementCondition</code>(s).
     */
    public Set<EntitlementCondition> getEConditions() {
        return eConditions;
    }

    /**
     * Sets OpenAM policy Condition name
     * @param pConditionName subject name as used in OpenAM policy,
     * this is relevant only when UserECondition was created from
     * OpenAM policy Condition
     */
    public void setPConditionName(String pConditionName) {
        this.pConditionName = pConditionName;
    }

    /**
     * Returns OpenAM policy Condition name
     * @return  subject name as used in OpenAM policy,
     * this is relevant only when UserECondition was created from
     * OpenAM policy Condition
     */
    public String getPConditionName() {
        return pConditionName;
    }

    /**
     * Returns JSONObject mapping of the object
     * @return JSONObject mapping of the object
     * @throws org.json.JSONException if can not map to JSONObject
     */
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();
        toJSONObject(jo);
        jo.put("pConditionName", pConditionName);

        if ((eConditions != null) && !eConditions.isEmpty()) {
            for (EntitlementCondition eCondition : eConditions) {
                JSONObject subjo = new JSONObject();
                subjo.put("className", eCondition.getClass().getName());
                subjo.put("state", eCondition.getState());
                jo.append("memberECondition", subjo);
            }
        }
        // Write the refused members back out unchanged, so that storing this object again keeps
        // the policy as it was written and the next load refuses them again instead of seeing a
        // wrapper that looks legitimately smaller. See rejectedMembers.
        for (JSONObject rejected : rejectedMembers) {
            jo.append("memberECondition", rejected);
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
            PolicyConstants.DEBUG.error("LogicalCondition.toString()", e);
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
        if (!super.equals(obj)) {
            return false;
        }
        LogicalCondition object = (LogicalCondition) obj;
        // Read both sides through the accessors: subclasses may keep the nested
        // conditions elsewhere and override the getters (NotCondition holds a single
        // condition and leaves these fields null), so reading this.field against
        // object.getter() would compare two different representations.
        Set<EntitlementCondition> thisEConditions = getEConditions();
        Set<EntitlementCondition> otherEConditions = object.getEConditions();
        if (thisEConditions == null) {
            if (otherEConditions != null) {
                return false;
            }
        } else { // eConditions not null
            if (otherEConditions == null) {
                return false;
            } else if (!thisEConditions.containsAll(otherEConditions)) {
                return false;
            } else if (!otherEConditions.containsAll(thisEConditions)) {
                return false;
            }
        }
        String thisPConditionName = getPConditionName();
        String otherPConditionName = object.getPConditionName();
        if (thisPConditionName == null) {
            if (otherPConditionName != null) {
                return false;
            }
        } else {
            if (!thisPConditionName.equals(otherPConditionName)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns hash code of the object
     * @return hash code of the object
     */
    @Override
    public int hashCode() {
        int code = super.hashCode();
        if (eConditions != null) {
            for (EntitlementCondition eCondition : eConditions) {
                code = 31*code + eCondition.hashCode();
            }
        }
        if (pConditionName != null) {
            code = 31*code + pConditionName.hashCode();
        }
        return code;
    }

    @Override
    public void validate() throws EntitlementException {
        if (eConditions == null || eConditions.isEmpty()) {
            throw new EntitlementException(EntitlementException.PROPERTY_VALUE_NOT_DEFINED, "conditions");
        }

        for (EntitlementCondition child : eConditions) {
            child.validate();
        }
    }
}
