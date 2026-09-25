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
 * $Id: NotCondition.java,v 1.1 2009/08/19 05:40:33 veiming Exp $
 *
 * Portions copyright 2014-2015 ForgeRock AS.
 * Portions Copyright 2026 3A Systems, LLC.
 */

package com.sun.identity.entitlement;

import org.forgerock.openam.entitlement.PolicyConstants;
import org.forgerock.util.Reject;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.Subject;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * This class wrapped on an Entitlement Condition object to provide boolean
 * NOT.
 * Membership of <code>NotCondition</code> is satisfied in the user is not a
 * member of the nested <code>EntitlementCondition</code>.
 *
 * We @JsonIgnore getEConditions and setEConditions (NOTE the 's' on the end) so that
 * we don't indicate via JSON schema exposed that we take multiple condition types.
 *
 * We extend LogicalCondition but ensure that we are only allowing a single
 * {@link EntitlementCondition} to be referenced by this class.
 */
public class NotCondition extends LogicalCondition {
    private EntitlementCondition eCondition;
    private String pConditionName;

    /**
     * Constructs <code>NotCondition</code>
     */
    public NotCondition() {
    }

    /**
     * Constructs NotCondition
     *
     * @param eCondition wrapped <code>EntitlementCondition</code>(s)
     */
    public NotCondition(EntitlementCondition eCondition) {
        this.eCondition = eCondition;
    }

    /**
     * Constructs <code>NotCondition</code>.
     *
     * @param eConditions wrapped <code>EntitlementCondition</code>(s)
     * @param pConditionName subject name as used in OpenAM policy,
     * this is relevant only when UserECondition was created from
     * OpenAM policy Condition
     */
    public NotCondition(
        EntitlementCondition eConditions,
        String pConditionName
    ) {
        this.eCondition = eConditions;
        this.pConditionName = pConditionName;
    }

    /**
     * Sets state of the object
     *
     * @param state State of the object encoded as string
     */
    @Override
    public void setState(String state) {
        JSONObject memberCondition = null;
        try {
            JSONObject jo = new JSONObject(state);
            setState(jo);
            pConditionName = (jo.has("pConditionName")) ?
                jo.optString("pConditionName") : null;

            clearMemberRejection();
            memberCondition = jo.optJSONObject("memberECondition");
            if (memberCondition != null) {
                String className = memberCondition.getString("className");
                EntitlementCondition member = EntitlementClassResolver.newInstance(
                    className, EntitlementCondition.class);
                member.setState(memberCondition.getString("state"));
                // Assign only once the member has been fully applied, the way
                // LogicalCondition.setState adds to its set only after the member's own setState
                // returned. A half-applied member left in the field is what toJSONObject() writes
                // back out in place of the refused declaration, so a re-save would hand the next
                // load an intact policy - and an enclosing NOT would negate its failure into a
                // grant.
                eCondition = member;
            }
        } catch (InstantiationException ex) {
            PolicyConstants.DEBUG.error("NotCondition.setState", ex);
            rejectMember(memberCondition);
        } catch (IllegalAccessException ex) {
            PolicyConstants.DEBUG.error("NotCondition.setState", ex);
            rejectMember(memberCondition);
        } catch (ClassNotFoundException ex) {
            PolicyConstants.DEBUG.error("NotCondition.setState", ex);
            rejectMember(memberCondition);
        } catch (JSONException ex) {
            PolicyConstants.DEBUG.error("NotCondition.setState", ex);
            // Only when the state did declare a member: a state string that fails to parse before
            // the member is even read is not a rejected member.
            if (memberCondition != null) {
                rejectMember(memberCondition);
            }
        }
    }

    /**
     * Records the refusal and drops whatever the failed load left in the member field - a partly
     * initialised instance, or the member of an earlier successful {@code setState} on this same
     * object. Either would be written back out by {@link #toJSONObject()} instead of the refused
     * declaration, which is the one thing the refusal must survive.
     *
     * @param rejectedMember the declaration that could not be turned into a member.
     */
    private void rejectMember(JSONObject rejectedMember) {
        eCondition = null;
        markMemberRejected(rejectedMember);
    }

    /**
     * Returns state of the object.
     *
     * @return state of the object encoded as string.
     */
    @Override
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
    @Override
    public ConditionDecision evaluate(
        String realm,
        Subject subject,
        String resourceName,
        Map<String, Set<String>> environment
    ) throws EntitlementException {

        if (eCondition == null) {
            return new ConditionDecision(false, Collections.EMPTY_MAP);
        }

        if (hasRejectedMemberInSubtree()) {
            // A member class name was refused somewhere below, so the nested condition is an
            // incomplete representation of the stored policy and fails closed. Negating that
            // failure here would hand back exactly the grant the refusal was meant to withhold.
            PolicyConstants.DEBUG.error(
                "NotCondition.evaluate: failing, nested policy member condition was rejected");
            return ConditionDecision
                    .newFailureBuilder()
                    .build();
        }

        ConditionDecision decision = eCondition.evaluate(realm, subject, resourceName, environment);

        return ConditionDecision
                .newBuilder(!decision.isSatisfied())
                .setResponseAttributes(decision.getResponseAttributes())
                .build();
    }

    /**
     * Sets the nested <code>EntitlementCondition</code>(s).
     *
     * @param eCondition the nested <code>EntitlementCondition</code>(s)
     */
    public void setECondition(EntitlementCondition eCondition) {
        this.eCondition = eCondition;
        clearMemberRejection();
    }

    /**
     * Returns the nested <code>EntitlementCondition</code>(s).
     *
     * @return the nested <code>EntitlementCondition</code>(s).
     */
    public EntitlementCondition getECondition() {
        return eCondition;
    }

    /**
     * Sets the nested <code>EntitlementCondition</code>(s).
     *
     * @param eConditions the nested <code>EntitlementCondition</code>(s)
     */
    @Override
    @JsonIgnore
    public void setEConditions(Set<EntitlementCondition> eConditions) {
        Reject.ifTrue(eConditions.size() > 1 || eConditions.size() < 1);

        eCondition = eConditions.iterator().next();
        clearMemberRejection();
    }

    /**
     * Returns the nested <code>EntitlementCondition</code>(s).
     *
     * @return the nested <code>EntitlementCondition</code>(s).
     */
    @Override
    @JsonIgnore
    public Set<EntitlementCondition> getEConditions() {
        if (eCondition == null) {
            return null;
        }

        return Collections.singleton(eCondition);
    }

    /**
     * {@inheritDoc}
     * <p>
     * A <code>NotCondition</code> with no member at all counts as damaged too, not only one whose
     * member {@link #setState(String)} refused. Its own {@link #evaluate} already fails on the
     * missing member, but an enclosing <code>NotCondition</code> would negate that failure into a
     * grant. The two are indistinguishable from the outside anyway: a policy stored by a version
     * that dropped the refused member instead of re-emitting it comes back with the member simply
     * absent, and a <code>NOT</code> without a member is not a policy statement that
     * {@link #validate()} accepts in the first place.
     */
    @Override
    @JsonIgnore
    public boolean hasRejectedMemberInSubtree() {
        return (eCondition == null) || super.hasRejectedMemberInSubtree();
    }

    /**
     * Sets OpenAM policy Condition name
     * @param pConditionName subject name as used in OpenAM policy,
     * this is relevant only when UserECondition was created from
     * OpenAM policy Condition
     */
    @Override
    public void setPConditionName(String pConditionName) {
        this.pConditionName = pConditionName;
    }

    /**
     * Returns OpenAM policy Condition name
     * @return  subject name as used in OpenAM policy,
     * this is relevant only when UserECondition was created from
     * OpenAM policy Condition
     */
    @Override
    public String getPConditionName() {
        return pConditionName;
    }

    /**
     * Returns JSONObject mapping of the object
     * @return JSONObject mapping of the object
     * @throws org.json.JSONException if can not map to JSONObject
     */
    @Override
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();
        toJSONObject(jo);
        jo.put("pConditionName", pConditionName);

        if (eCondition != null) {
            JSONObject subjo = new JSONObject();
            subjo.put("className", eCondition.getClass().getName());
            subjo.put("state", eCondition.getState());
            jo.put("memberECondition", subjo);
        } else if (!getRejectedMembers().isEmpty()) {
            // Write the refused member back out unchanged, so that storing this object again keeps
            // the policy as it was written: without it a re-save would emit a NOT with no member at
            // all, losing the refused class name and, with it, the record that this policy is
            // damaged. NotCondition holds a single member, so there is at most one entry.
            jo.put("memberECondition", getRejectedMembers().get(0));
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
            PolicyConstants.DEBUG.error("NotCondition.toString()", e);
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
        if (!getClass().equals(obj.getClass())) {
            return false;
        }
        NotCondition object = (NotCondition) obj;

        if (eCondition == null) {
            if (object.eCondition != null) {
                return false;
            }
        } else {
            if (!eCondition.equals(object.eCondition)) {
                return false;
            }
        }
        if (pConditionName == null) {
            if (object.pConditionName != null) {
                return false;
            }
        } else {
            if (!pConditionName.equals(object.pConditionName)) {
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
        
        if (eCondition != null) {
            code += eCondition.hashCode();
        }
        if (pConditionName != null) {
            code += pConditionName.hashCode();
        }
        return code;
    }

    @Override
    public void validate() throws EntitlementException {
        if (eCondition == null) {
            throw new EntitlementException(EntitlementException.PROPERTY_VALUE_NOT_DEFINED, "condition");
        }
    }
}
