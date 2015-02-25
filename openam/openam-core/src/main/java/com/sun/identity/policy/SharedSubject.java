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
 * $Id: SharedSubject.java,v 1.3 2008/06/25 05:43:45 qcheng Exp $
 *
 */



package com.sun.identity.policy;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;

import com.sun.identity.policy.interfaces.Subject;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/*
 * Subject implementation that delegates methods to Subject defined
 * at realm.  This implemenation delegates all the methods to Subject
 * defined at the realm except for setValues() call. This implementation
 * throws InvalidNameException on setValues() call. We could not throw 
 * a better exception since we have to maintain backward compatibility.
 */
class SharedSubject implements Subject, Cloneable {

    String subjectName;
    SubjectTypeManager stm;
    SubjectTypeManager mstm;
    private static final Debug debug = PolicyManager.debug;

    SharedSubject(String subjectName, SubjectTypeManager stm) {
        this.subjectName = subjectName;
        this.stm = stm;
    }

    /**
     * Returns the syntax of the name values the
     * <code>Subject</code> implementation can have.
     * @see com.sun.identity.policy.Syntax
     *
     * @param token the <code>SSOToken</code> that will be used
     * to determine the syntax
     *
     * @return syntax of the name values for the <code>Subject</code>
     *
     * @exception SSOException if SSO token is not valid
     * @exception PolicyException if can not get syntax
     *
     */
    public Syntax getValueSyntax(SSOToken token)
            throws SSOException, PolicyException {
        return stm.getSubjectByName(subjectName).getValueSyntax(token);
    }

    /**
     * Returns a list of possible name values for the <code>Subject
     * </code>. The implementation must use the <code>SSOToken
     * </code> provided to determine the possible
     * name values. For example, in a Role implementation
     * this method will return all the roles defined
     * in the organization.
     *
     * @param token the <code>SSOToken</code> that will be used
     * to determine the possible name values
     *
     * @return <code>ValidValues</code> for the <code>Subject</code>
     *
     * @exception SSOException if SSO token is not valid
     * @exception PolicyException if unable to get the valid
     * name values.
     */
    public ValidValues getValidValues(SSOToken token)
            throws SSOException, PolicyException {
        return stm.getSubjectByName(subjectName).getValidValues(token);
    }

    /**
     * Returns a list of possible name values for the <code>Subject
     * </code> that satisfy the given <code>pattern</code>.
     *  The implementation must use the <code>SSOToken
     * </code> provided to determine the possible
     * name values. For example, in a Role implementation with the
     * search filter <code>*admin</code> this method will return all
     * the roles defined in the organization that end with <code>admin</code>
     * 
     * @param token the <code>SSOToken</code> that will be used
     * to determine the possible name values
     * @param pattern search pattern that will be used to narrow
     * the list of valid names.
     * 
     * @return <code>ValidValues</code> object
     *
     * @exception SSOException if SSO token is not valid
     * @exception PolicyException if unable to get the list of valid
     * name values.
     */
    public ValidValues getValidValues(SSOToken token, String pattern)
            throws SSOException, PolicyException {
        return stm.getSubjectByName(subjectName).getValidValues(token, pattern);
    }

    /**
     * Returns the display name for the value for the given locale.
     * For all the valid name values obtained through the methods
     * <code>getValidValues</code> this method must be called
     * by web and command line interface to get the corresponding display name.
     * The <code>locale</code> variable could be used by the
     * plugin to customize
     * the display name for the given locale.
     * The <code>locale</code> variable
     * could be <code>null</code>, in which case the plugin must
     * use the default locale (most probably <code>en_US</code>).
     * This method returns only the display name and should not
     * be used for the method <code>setValues</code>.
     * Alternatively, if the plugin does not have to localize
     * the value, it can just return the <code>value</code> as is.
     *
     * @param value one of the valid value for the plugin
     * @param locale locale for which the display name must be customized
     * @return the display name for the value for the given locale.
     * @exception NameNotFoundException if the given <code>value</code>
     * is not one of the valid name values for the plugin. This implementaion
     * would throw this exception even in the cases where it would run
     * into PolicyException. PolicyException is wrapped in 
     * NameNotFoundException and the PolicyException would be thrown.
     * This is done to maintain backward compatibility and to avoid method
     * singature changes.
     */
    public String getDisplayNameForValue(String value, Locale locale)
            throws NameNotFoundException {
        Subject subject = null;
        try {
            subject = stm.getSubjectByName(subjectName);
        } catch (PolicyException pe) {
	    String[] objs = { subjectName };
	    throw new NameNotFoundException(ResBundleUtils.rbName,
		"realm_subject_not_found", objs,
		subjectName, PolicyException.USER_COLLECTION);
        }

        if ( subject != null) {
            return subject.getDisplayNameForValue(value, locale);
        } else {
	    String[] objs = { subjectName };
	    throw new NameNotFoundException(ResBundleUtils.rbName,
		"realm_subject_not_found", objs,
		subjectName, PolicyException.USER_COLLECTION);
        }
    }

    /**
     * Returns the name values that was set using the
     * method <code>setValues</code>.
     *
     * @return name values that have been set for the user collection
     */
    public Set getValues() {
        Subject subject = null;
        try {
            subject = stm.getSubjectByName(subjectName);
        } catch (PolicyException pe) {
            if (debug.warningEnabled()) {
                debug.warning("Could not find realm subject :" + subjectName
                        + " could not getValues()", pe);
            }
        }

        if (subject != null) {
            return subject.getValues();
        } else {
            return Collections.EMPTY_SET;
        }
    }

    /**
     * Initialize (or configure) the <code>Subject</code>
     * object. Usually it will be initialized with the environment
     * parameters set by the system administrator using configuration service.
     * For example in a Role implementation, the configuration
     * parameters could specify the directory server name, port, etc.
     *
     * @param configParams configuration parameters as a map.
     * The name values in the map is <code>java.util.Set</code>,
     * which contains one or more configuration parameters.
     *
     * @exception PolicyException if an error occurred during
     * initialization of <code>Subject</code> instance
     */
    public void initialize(Map configParams) throws PolicyException {
        //no-op : initialization is done in the subject at the realm
    }

    /**
     * Sets the name values for the instance of this <code>Subject</code>.
     *
     * @param names name name values selected for the instance of
     * the user collection object.
     *
     * @exception InvalidNameException if the given names are not valid
     */
    public void setValues(Set names) throws InvalidNameException {
        throw new InvalidNameException(ResBundleUtils.rbName,
		"can_not_set_values_in_shared_subject",  null, "ALL",
		PolicyException.SUBJECT_TYPE);
    }

    /**
     * Determines if the user identified by <code>SSOToken</cdoe> 
     * is a member of this <code>Subject</code>.
     *
     * @param token single-sign-on token of the user
     *
     * @return <code>true</code> if the user is member of the
     * given subject, <code>false</code> otherwise.
     *
     * @exception SSOException if SSO token is not valid
     * @exception PolicyException if an error occurred while
     * checking if the user is a member of this subject
     */
    public boolean isMember(SSOToken token)
            throws SSOException, PolicyException {
        boolean member = false;
        if (mstm == null) {
            String realmName = stm.getPolicyManager().getOrganizationDN();
            mstm = PolicyCache.getInstance().getPolicyManager(realmName).
                    getSubjectTypeManager();
        }
        Subject subject = mstm.getCachedSubjectByName(subjectName);
        if ( subject != null) {
            member = subject.isMember(token);
        } else {
            if (debug.warningEnabled()) {
                debug.warning("Realm subject: " + subjectName
                        + " not found");
            }
        }
        return member;
    }



    /**
     * Return a hash code for this <code>Subject</code>.
     *
     * @return a hash code for this <code>Subject</code>.
     */
    public int hashCode() {
        return super.hashCode();
    }


    /**
     * Indicates whether some other object is "equal to" this object.
     *
     * @param o another object that will be compared with this object
     * @return <code>true</code> if equal.
     */
    public boolean equals(Object o) {
        return (this == o);
    }

    /**
     * Creates and returns a copy of this object.
     *
     * @return a copy of this object
     */
    public Object clone() {
        Object theClone = null;
        try {
            theClone =  super.clone();
        } catch (CloneNotSupportedException cne) {
        }
        return theClone;
    }
}

