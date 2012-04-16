/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AMIdentitySubject.java,v 1.3 2008/06/25 05:43:50 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy.plugins;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;

import com.sun.identity.shared.debug.Debug;

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;

import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyEvaluator;
import com.sun.identity.policy.ValidValues;
import com.sun.identity.policy.ResBundleUtils;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.Syntax;
import com.sun.identity.policy.NameNotFoundException;
import com.sun.identity.policy.InvalidNameException;
import com.sun.identity.policy.SubjectEvaluationCache;

import com.sun.identity.policy.interfaces.Subject;

import com.sun.identity.security.AdminTokenAction;

import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import java.security.Principal;
import java.security.AccessController;

/**
 * AMIdentitySubject is a <code>Subject</code> implementation that checks for 
 * membership in a set of <code>AMIdentity</code> objects using the underlying
 * Identity repository service.
 */

public class AMIdentitySubject implements Subject {

    private Set subjectValues = new HashSet();

    private static Debug debug = Debug.getInstance(
        PolicyManager.POLICY_DEBUG_NAME);

    /** Constructs an <code>AMIdentityObject</code>
     */
    public AMIdentitySubject() {
    }

    /**
     * Initialize the AMIdentitySubject object by using the configuration
     * information passed by the Policy Framework.
     * <p>
     * This implementation  not need anything out of the <code>configParams 
     * I/code> so does no operation.
     *
     * @param configParams configuration parameters as a <code>Map</code>.
     *
     * @exception PolicyException if an error occured during
     * initialization of <code>Subject</code> instance
     */
    public void initialize(Map configParams) throws PolicyException {
	//no op
    }

    /**
     * Returns the syntax of the values the
     * <code>AMIdentitySubject</code> implementation can have.
     * @see com.sun.identity.policy.Syntax
     *
     * @param token the <code>SSOToken</code> that will be used
     * to determine the syntax
     *
     * @return <code>Syntax</code> of the values in this plugin.
     * It returns <code>Syntax.MULTIPLE_CHOICE</code>.
     *
     * @exception SSOException if <code>SSOToken</code> is not valid
     * @exception <code>PolicyException</code> if unable to get the list of 
     * valid names.
     *
     * @return <code>Syntax</code> of the values for the <code>Subject</code>
     */
    public Syntax getValueSyntax(SSOToken token) throws SSOException {
        return (Syntax.MULTIPLE_CHOICE);
    }

    /**
     * Returns a list of possible values for the <code>Subject</code>.
     *
     * @param token the <code>SSOToken</code> that will be used
     * to determine the possible values
     *
     * @return <code>ValidValues</code> object
     *
     * @exception SSOException if <code>SSOToken</code> is not valid
     * @exception PolicyException if unable to get the list of valid
     * names.
     * NOTE: The AMIdentitySubject plugin does not support this 
     * functionality and in turn throws unsupported 
     * <code>PolicyException</code> 
     */
    public ValidValues getValidValues(SSOToken token) throws
        SSOException, PolicyException {
        return (getValidValues(token, "*"));
    }

    /**
     * Returns a list of possible values for the <code>Subject
     * </code> that matches the pattern. 
     *
     * @param token the <code>SSOToken</code> that will be used
     * to determine the possible values
     *
     * @return <code>ValidValues</code> object
     *
     * @exception SSOException if SSO token is not valid
     * @exception PolicyException if unable to get the list of valid
     * names.
     * NOTE: The AMIdentitySubject plugin does not support this 
     * functionality and in turn throws unsupported 
     * <code>PolicyException</code> 
     */
    public ValidValues getValidValues(SSOToken token, String pattern)
        throws SSOException, PolicyException {
        throw (new PolicyException(ResBundleUtils.rbName,
            "am_id_subject_does_not_support_getvalidvalues", null, null));
    }

    /**
     * Returns the display name for the value for the given locale.
     * For all the valid values obtained through the methods
     * <code>getValidValues</code> this method must be called
     * by GUI and CLI to get the corresponding display name.
     * The <code>locale</code> variable could be used by the
     * plugin to customize the display name for the given locale.
     * The <code>locale</code> variable could be <code>null</code>, 
     * in which case the plugin must use the default locale (most probabily 
     * en_US).
     * Alternatively, if the plugin does not have to localize
     * the value, it can just return the <code>value</code> as is.
     *
     * @param value one of the valid value for the plugin
     * @param locale locale for which the display name must be customized
     *
     * @exception NameNotFoundException if the given <code>value</code>
     * is not one of the valid values for the plugin
     */
    public String getDisplayNameForValue(String value, Locale locale)
        throws NameNotFoundException {
        return (value);
    }

    /**
     * Returns the values that was set using the
     * method <code>setValues</code>.
     *
     * @return <code>Set</code of values that have been set for the user 
     * collection
     */
    public Set getValues() {
        if (subjectValues == null) {
            return (Collections.EMPTY_SET);
        }
        return (subjectValues);
    }

    /**
     * Sets the values identifying <code>AMIdentity</code> objects on which
     * membership would be checked
     *
     * @param names <code>universalId(s)</code> of <code>AMIdentity</code>
     * objects on which memberships would be checked
     *
     * @exception InvalidNameException if the given names are not valid
     *
     *
     */
    public void setValues(Set names) throws InvalidNameException {
        if (names == null) {
            throw (new InvalidNameException(ResBundleUtils.rbName,
                "amidentity_subject_invalid_subject_values", null, null,
                PolicyException.USER_COLLECTION));
        }
        subjectValues.addAll(names);
        if (debug.messageEnabled()) {
            debug.message("AMIdentitySubejct set subjectValues to: " 
                    + subjectValues);
        }
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
    public boolean isMember(SSOToken token)
            throws SSOException, PolicyException {

        String tokenID = null;
        String userDN = null;
        if (token != null) {
            Object tokenIDObject = token.getTokenID();
            if (tokenIDObject != null) {
                tokenID = tokenIDObject.toString();
            }
        }

        if (tokenID == null) {
            if (debug.warningEnabled()) {
                debug.warning("AMIdentitySubject.isMember():"
                        +"tokenID is null");
                debug.warning("AMIdentitySubject.isMember():"
                        +"returning false");
            }
            return false;
        } else {
            Principal principal = token.getPrincipal();
            if(principal != null) {
                userDN = principal.getName();
            }
            if (userDN == null) {
                if (debug.warningEnabled()) {
                    debug.warning("AMIdentitySubject.isMember():"
                            +"userDN is null");
                    debug.warning("AMIdentitySubject.isMember():"
                            +"returning false");
                }
                return false;
            }
        }

        boolean listenerAdded = false;
        boolean subjectMatch = false;

        if (debug.messageEnabled()) {
            debug.message("AMIndentitySubject.isMember(): "
                    + "entering with userDN = " + userDN);
        }

        if (subjectValues.size() > 0) {
            Iterator valueIter = subjectValues.iterator();
            while (valueIter.hasNext()) {
                Boolean matchFound = null;

                /* Actually this is universal id of AMIdentity object
                 * 
                 */
                String subjectValue = (String)valueIter.next();

                if (debug.messageEnabled()) {
                    debug.message("AMIndentitySubject.isMember(): "
                            + "checking membership with userDN = " + userDN
                            + ", subjectValue = " + subjectValue);
                }

                if ((matchFound = SubjectEvaluationCache.isMember(
                        tokenID, "AMIdentitySubject" ,subjectValue)) != null) {
                    if (debug.messageEnabled()) {
                        debug.message("AMIdentitySubject.isMember():"
                                + "got membership from SubjectEvaluationCache "
                                + " for userDN = " + userDN
                                + ", subjectValue = " + subjectValue
                                + ", result = " + matchFound.booleanValue());
                    }
                    boolean result = matchFound.booleanValue();
                    if (result) {
                        if (debug.messageEnabled()) {
                            debug.message("AMIndentitySubject.isMember(): "
                                    + " returning membership status = " 
                                    + result);
                        }
                        return result;
                    } else {
                        continue;
                    }
                }

                // got here so entry not in subject evalauation cache
                if (debug.messageEnabled()) {
                    debug.message("AMIdentitySubject:isMember():entry for "
                            + subjectValue + " not in subject evaluation "
                            +"cache, so compute using IDRepo api");
                    }

                try {
                    AMIdentity subjectIdentity = IdUtils.getIdentity(
                            getAdminToken(), subjectValue);
                    if (subjectIdentity == null) {
                        if (debug.messageEnabled()) {
                            debug.message("AMidentitySubject.isMember():"
                                    + "subjectIdentity is null for "
                                    + "subjectValue = " + subjectValue);
                            debug.message("AMidentitySubject.isMember():"
                                    + "returning false");
                        }
                        return false;
                    }


                    AMIdentity tmpIdentity = IdUtils.getIdentity(token);
                    String univId = IdUtils.getUniversalId(tmpIdentity);
                    AMIdentity userIdentity = IdUtils.getIdentity(
                        getAdminToken(), univId);
                    if (userIdentity == null) {
                        if (debug.messageEnabled()) {
                            debug.message("AMidentitySubject.isMember():"
                                    + "userIdentity is null");
                            debug.message("AMidentitySubject.isMember():"
                                    + "returning false");
                        }
                        return false;
                    }

                    if (debug.messageEnabled()) {
                        debug.message("AMidentitySubject.isMember():"
                                + "user uuid = " 
                                + IdUtils.getUniversalId( userIdentity) 
                                + ", subject uuid = " 
                                + IdUtils.getUniversalId(subjectIdentity) );
                    }

                    IdType userIdType = userIdentity.getType();
                    IdType subjectIdType = subjectIdentity.getType();
                    Set allowedMemberTypes = null;
                    if (userIdentity.equals(subjectIdentity)) {
                        if (debug.messageEnabled()) {
                            debug.message("AMidentitySubject.isMember():"
                                    + "userIdentity equals subjectIdentity:"
                                    + "membership=true");
                        }
                        subjectMatch = true;
                    } else if (
                            ((allowedMemberTypes 
                            = subjectIdType.canHaveMembers()) != null) 
                            && allowedMemberTypes.contains(userIdType)) {
                        subjectMatch = userIdentity.isMember(subjectIdentity);
                        if (debug.messageEnabled()) {
                            debug.message("AMIdentitySubject.isMember():"
                                    + "userIdentity type " + userIdType + 
                                    " can be a member of "
                                    + "subjectIdentityType " + subjectIdType
                                    + ":membership=" + subjectMatch);
                        }
                    } else {
                        subjectMatch = false;
                        if (debug.messageEnabled()) {
                            debug.message("AMIdentitySubject.isMember():"
                                    + "userIdentity type " + userIdType + 
                                    " can not be a member of "
                                    + "subjectIdentityType " + subjectIdType
                                    + ":membership=" + subjectMatch);
                        }
                    }

                    if (debug.messageEnabled()) {
                        debug.message("AMIdentitySubject.isMember: adding "
                            +"entry in SubjectEvaluationCache for "
                            + ", for userDN = " + userDN
                            + ", subjectValue = " + subjectValue
                            + ", subjectMatch = " + subjectMatch);
                    }
                    SubjectEvaluationCache.addEntry(tokenID, 
                        "AMIdentitySubject", subjectValue, subjectMatch);
                    if (!listenerAdded) {
                        if (!PolicyEvaluator.ssoListenerRegistry.containsKey(
                                tokenID)) {
                            token.addSSOTokenListener(
                                PolicyEvaluator.ssoListener);
                            PolicyEvaluator.ssoListenerRegistry.put(
                                    tokenID, PolicyEvaluator.ssoListener);
                            if (debug.messageEnabled()) {
                                debug.message("AMIdentitySubject.isMember():"
                                        + " sso listener added ");
                            }
                            listenerAdded = true;
                        }
                    }
                    if (subjectMatch) {
                        break;
                    }
                } catch (IdRepoException ire) {
                    debug.warning("AMidentitySubject.isMember():"
                            + "can not check membership for user "
                            + userDN + ", subject "
                            + subjectValue, ire);
                    String[] args = {userDN, subjectValue};
                    throw (new PolicyException(ResBundleUtils.rbName,
                        "am_id_subject_membership_evaluation_error", args, 
                        ire));
                }
            }
        }
        if (debug.messageEnabled()) {
            if (!subjectMatch) { 
                debug.message("AMIdentitySubject.isMember(): user " + userDN 
                      + " is not a member of this subject"); 
            } else {
                debug.message("AMIdentitySubject.isMember(): User " + userDN 
                      + " is a member of this subject"); 
            }
        }
        return subjectMatch;
    }


   /** 
    * Return a hash code for this <code>AMIdentitySubject</code>.
    *
    * @return a hash code for this <code>AMIdentitySubject</code> object.
    */

    public int hashCode() {
        return subjectValues.hashCode();
    }


    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param o another object that will be compared with this one
     *
     * @return <code>true</code> if eqaul; <code>false</code>
     * otherwise
     */
    public boolean equals(Object o) {
        if (o instanceof AMIdentitySubject) {
            AMIdentitySubject subject = (AMIdentitySubject) o;
            return(subjectValues.equals(subject.subjectValues));
        }
        return (false);
    }

    /**
     * Creates and returns a copy of this object.
     *
     * @return a copy of this object
     */
    public Object clone() {
        AMIdentitySubject theClone = null;
        try {
            theClone = (AMIdentitySubject) super.clone();
        } catch (CloneNotSupportedException e) {
            // this should never happen
            throw new InternalError();
        }
        if (subjectValues != null) {
            theClone.subjectValues = new HashSet();
            theClone.subjectValues.addAll(subjectValues);
        }
        return theClone;
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
