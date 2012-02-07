/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AMIdentityMembershipCondition.java,v 1.2 2008/06/25 05:43:50 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy.plugins;

import java.util.*;

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;

import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.policy.ConditionDecision;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.ResBundleUtils;
import com.sun.identity.policy.Syntax;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;

import com.sun.identity.security.AdminTokenAction;

import java.security.AccessController;

/**
 * The class <code>AMIdentityMembershipCondition</code> is a plugin 
 * implementation of <code>Condition</code> interface.
 * This condition is satisifed only if  the invocator uuid specified
 * in the environment is a member of at least one <code>AMIdentity</code> 
 * object specified in the Condition.
 *
 */
public class AMIdentityMembershipCondition implements Condition {


    private static final Debug DEBUG
        = Debug.getInstance(PolicyManager.POLICY_DEBUG_NAME);

    private Map properties;
    private Set nameValues = new HashSet();;

    private static List propertyNames = new ArrayList(1);

    static {
        propertyNames.add(Condition.AM_IDENTITY_NAME);
    }

    /** No argument constructor 
     */
    public AMIdentityMembershipCondition() {
    }

     /**
      * Returns a set of property names for the condition.
      *
      * @return set of property names
      */
     public List getPropertyNames()
     {
         return (new ArrayList(propertyNames));
     }
 
     /**
      * Returns the syntax for a property name
      * @see com.sun.identity.policy.Syntax
      *
      * @param property property name
      *
      * @return <code>Syntax<code> for the property name
      */
     public Syntax getPropertySyntax(String property)
     {
         return (Syntax.ANY);
     }
      
     /**
      * Gets the display name for the property name.
      * The <code>locale</code> variable could be used by the
      * plugin to customize the display name for the given locale.
      * The <code>locale</code> variable could be <code>null</code>, in which 
      * case the plugin must use the default locale.
      *
      * @param property property name.
      * @param locale locale for which the property name must be customized.
      * @return display name for the property name.
      * @throws PolicyException
      */
     public String getDisplayName(String property, Locale locale) 
       throws PolicyException
     {
         return property;
     }
 
     /**
      * Returns a set of valid values given the property name. This method
      * is called if the property Syntax is either the SINGLE_CHOICE or 
      * MULTIPLE_CHOICE.
      *
      * @param property property name
      * @return Set of valid values for the property.
      * @exception PolicyException if unable to get the valid values.
      */
     public Set getValidValues(String property) throws PolicyException
     {
         return (Collections.EMPTY_SET);
     }


    /**
     * Sets the properties of the condition.
     *  Evaluation of <code>ConditionDecision</code> is influenced by these
     *  properties.
     *  @param properties the properties of the condition that governs
     *         whether a policy applies. The properties should
     *         define value for the key <code>Condition.AM_IDENTITY_NAME<code>. 
     *         The value should be a <code>Set</code>.
     *         Each element of the <code>Set</code> should be
     *         a String, the uuid of <code>AMIdentity</code>. Please note that
     *         properties is not cloned by the method.
     *
     *  @throws PolicyException if properties is null or does not contain
     *          value for the key <code>AM_IDENTITY_NAME</code> or the value 
     *          of the key is  not a <code>Set</code> 
     */
    public void setProperties(Map properties) throws PolicyException {
        this.properties = (Map)((HashMap) properties);
        if ( (properties == null) || ( properties.keySet() == null) ) {
            throw new PolicyException(
                ResBundleUtils.rbName, "properties_can_not_be_null_or_empty",
                null, null);
        }

        //Check if the key is valid
        Set keySet = properties.keySet();
        Iterator keys = keySet.iterator();
        String key = (String) keys.next();
        if ( !Condition.AM_IDENTITY_NAME.equals(key) ) {
            String args[] = { Condition.AM_IDENTITY_NAME };
            throw new PolicyException(
                ResBundleUtils.rbName, "attempt_to_set_invalid_property", 
                args, null);
        }

        // check if the value is valid
        Set nameSet = (Set) properties.get(Condition.AM_IDENTITY_NAME);
        if (( nameSet == null ) || nameSet.isEmpty() ) {
            String args[] = { Condition.AM_IDENTITY_NAME };
            throw new PolicyException(
                ResBundleUtils.rbName, 
                "property_does_not_allow_empty_values", args, null); 
                //check i18n value
        }
        nameValues.addAll(nameSet);
    }


    /** 
     * Returns properties of this condition.
     *
     * @return properties of this condition.
     */
    public Map getProperties() {
        return properties;
    } 


    /**
     * Gets the decision computed by this condition object. 
     * This condition is satisifed only if  the invocator uuid specified
     * in the environment is a member of at least one <code>AMIdentity</code> 
     * object specified in the Condition. Invocator uuid would be specified as
     * the value of key <code>Condition.INVOCATOR_PRINCIPAL_UUID</code> in the
     * <code>environment</code> parameter. The value should be a <code>Set<code>
     * of <code>String</code> objects.
     *
     * @param token single sign on token of the user
     *
     * @param environment request specific environment map of key/value pairs.
     *
     * @return the condition decision.
     *
     * Policy framework continues evaluating a policy only if it applies
     * to the request as indicated by the <code>ConditionDecision</code>. 
     * Otherwise, further evaluation of the policy is skipped. 
     *
     * @throws SSOException if the token is invalid
     * @throws PolicyException for any other abnormal condition
     */
    public ConditionDecision getConditionDecision(SSOToken token, 
            Map environment) throws SSOException, PolicyException {

        if ( DEBUG.messageEnabled()) {
            DEBUG.message("At AMIdentityMembershipCondition."
                + "getConditionDecision(): "
                + "entering, names:" + nameValues);
            DEBUG.message("At AMIdentityMembershipCondition."
                + "getConditionDecision(): "
                + "environment.invocatorPrincipalUud:" 
                + environment.get(Condition.INVOCATOR_PRINCIPAL_UUID));
        }
        boolean member = false;
        Set invocatorUuidSet = (Set)environment.get(
                Condition.INVOCATOR_PRINCIPAL_UUID);
        if ( (invocatorUuidSet != null) && !invocatorUuidSet.isEmpty()) {
            String invocatorUuid 
                    = (String)(invocatorUuidSet.iterator().next());
            member = isMember(invocatorUuid);
        } else {
            if ( DEBUG.messageEnabled()) {
                DEBUG.message("At AMIdentityMembershipCondition."
                    + "getConditionDecision(): "
                    + "invocatorUuidSet isnull or empty");
            }
        }
        return new ConditionDecision(member);
    }

    /**
     * Returns a copy of this object.
     *
     * @return a copy of this object
     */
    public Object clone() {
        AMIdentityMembershipCondition theClone = null;
        try {
            theClone = (AMIdentityMembershipCondition) super.clone();
        } catch (CloneNotSupportedException e) {
            // this should never happen
            throw new InternalError();
        }
        if (properties != null) {
            theClone.properties = new HashMap();
            Iterator it = properties.keySet().iterator();
            while (it.hasNext()) {
                Object o = it.next();
                Set values = new HashSet();
                values.addAll((Set) properties.get(o));
                theClone.properties.put(o, values);
            }
        }
        return theClone;
    }

    /**
     * Determines if the user is a member of this instance of the 
     * <code>Subject</code> object.
     *
     * @param token single sign on token of the user
     *
     * @return <code>true</code> if the user is member of 
     * this subject; <code>false</code> otherwise.
     *
     * @exception SSOException if SSO token is not valid
     * @exception PolicyException if an error occured while
     * checking if the user is a member of this subject
     */
    private boolean isMember(String invocatorUuid)
            throws SSOException, PolicyException {

        boolean subjectMatch = false;

        if (invocatorUuid == null) {
            if (DEBUG.warningEnabled()) {
                DEBUG.warning("AMIdentityMembershipCondition.isMember():"
                        +"invocatorUuid is null");
                DEBUG.warning("AMIdentityMembershipCondition.isMember():"
                        +"returning false");
            }
            return false;
        }

        if (DEBUG.messageEnabled()) {
                DEBUG.warning("AMIdentityMembershipCondition.isMember():"
                        +"invocatorUuid:" + invocatorUuid);
        }

        if (!nameValues.isEmpty()) {
            Iterator valueIter = nameValues.iterator();
            while (valueIter.hasNext()) {

                String nameValue = (String)valueIter.next();

                if (DEBUG.messageEnabled()) {
                    DEBUG.message("AMIndentityMembershipCondition.isMember(): "
                            + "checking membership with nameValue = " 
                            + nameValue
                            + ", invocatorUuid = " + invocatorUuid);
                }

                try {
                    AMIdentity invocatorIdentity = IdUtils.getIdentity(
                            getAdminToken(), invocatorUuid);
                    if (invocatorIdentity == null) {
                        if (DEBUG.messageEnabled()) {
                            DEBUG.message(
                                    "AMidentityMembershipCondition.isMember():"
                                    + "invocatorIdentity is null for "
                                    + "invocatorUuid = " + invocatorUuid);
                            DEBUG.message(
                                    "AMidentityMembershipCondition.isMember():"
                                    + "returning false");
                        }
                        return false;
                    }

                    AMIdentity nameValueIdentity = IdUtils.getIdentity(
                            getAdminToken(), nameValue);
                    if (nameValueIdentity == null) {
                        if (DEBUG.messageEnabled()) {
                            DEBUG.message(
                                    "AMidentityMembershipCondition.isMember():"
                                    + "nameValueidentity is null for "
                                    + "nameValue = " + nameValue);
                            DEBUG.message(
                                    "AMidentityMembershipCondition.isMember():"
                                    + "returning false");
                        }
                        return false;
                    }

                    IdType invocatorIdType = invocatorIdentity.getType();
                    IdType nameValueIdType = nameValueIdentity.getType();
                    Set allowedMemberTypes = null;
                    if (invocatorIdentity.equals(nameValueIdentity)) {
                        if (DEBUG.messageEnabled()) {
                            DEBUG.message(
                                    "AMidentityMembershipCondition.isMember():"
                                    + "invocatorIdentity equals "
                                    + " nameValueIdentity:"
                                    + "membership=true");
                        }
                        subjectMatch = true;
                    } else if (
                            ((allowedMemberTypes 
                            = nameValueIdType.canHaveMembers()) != null) 
                            && allowedMemberTypes.contains(invocatorIdType)) {
                        subjectMatch = invocatorIdentity.isMember(
                                nameValueIdentity);
                        if (DEBUG.messageEnabled()) {
                            DEBUG.message(
                                    "AMIdentityMembershipCondition.isMember():"
                                    + "invocatorIdentityType " 
                                    + invocatorIdType + 
                                    " can be a member of "
                                    + " nameValueIdentityType " 
                                    + nameValueIdType
                                    + ":membership=" + subjectMatch);
                        }
                    } else {
                        subjectMatch = false;
                        if (DEBUG.messageEnabled()) {
                            DEBUG.message(
                                    "AMIdentityMembershipCondition.isMember():"
                                    + "invocatoridentityType " 
                                    + invocatorIdType + 
                                    " can be a member of "
                                    + " nameValueIdentityType " 
                                    + nameValueIdType
                                    + ":membership=" + subjectMatch);
                        }
                    }
                    if (subjectMatch) {
                        break;
                    }
                } catch (IdRepoException ire) {
                    DEBUG.warning("AMidentityMembershipCondition.isMember():"
                            + "can not check membership for invocator "
                            + invocatorUuid + ", nameValue "
                            + nameValue, ire);
                    String[] args = {invocatorUuid, nameValue};
                    throw (new PolicyException(ResBundleUtils.rbName,
                        "am_id_subject_membership_evaluation_error", args, 
                        ire));
                }
            }
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("AMIdentityMembershipCondition.isMember():"
                    + "invocatorUuidr=" + invocatorUuid 
                    + ",nameValues=" + nameValues 
                    + ",subjectMatch=" + subjectMatch); 
        }
        return subjectMatch;
    }

    /**
     * This method returns an admin <code>SSOToken</code>
     * which can be used to perform privileged operations.
     */
 
    private SSOToken getAdminToken() throws SSOException{
        SSOToken token = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        if (token == null) {
            throw (new SSOException(new PolicyException(
                ResBundleUtils.rbName, "invalid_admin", null, null)));
        }
        return (token);
    }

}
