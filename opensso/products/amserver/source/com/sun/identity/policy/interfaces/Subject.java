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
 * $Id: Subject.java,v 1.3 2009/05/05 18:27:47 mrudul_uchil Exp $
 *
 */




package com.sun.identity.policy.interfaces;

import java.util.Set;
import java.util.Map;
import java.util.Locale;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.policy.Syntax;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.InvalidNameException;
import com.sun.identity.policy.NameNotFoundException;
import com.sun.identity.policy.ValidValues;

/**
 * The class <code>Subject</code> defines a collection
 * of users (or subject) to whom the specified <code>Policy</code> is applied.
 * A complete implementation of this interface can have complex
 * boolean operations to determine if the given user identified
 * by the <code>SSOToken</code> belongs to this collection.
 * <p>
 * The interfaces are separated into administrative
 * interfaces and evaluation interfaces. The administrative interfaces
 * will be used by web interface/command line interface component to create a
 * <code>Subject</code> object and the evaluation interfaces will be used by the
 * {@link com.sun.identity.policy.PolicyEvaluator PolicyEvaluator}.
 *
 * @supported.all.api
 */
public interface Subject extends Cloneable {

    /**
     * Returns the syntax of the values the
     * <code>Subject</code> implementation can have.
     * @see com.sun.identity.policy.Syntax
     *
     * @param token the <code>SSOToken</code> that will be used
     * to determine the syntax
     *
     * @return <code>Set</code> of valid names for the user collection.
     *
     * @exception SSOException if <code>SSOToken</code> is not valid
     * @exception <code>PolicyException</code> if unable to get the list of 
     * valid names.
     *
     * @return <code>Syntax</code> of the values for the <code>Subject</code>
     */
    public Syntax getValueSyntax(SSOToken token)
        throws SSOException, PolicyException;

    /**
     * Returns a list of possible values for the <code>Subject
     * </code>. The implementation must use the <code>SSOToken
     * </code> <i>token</i> provided to determine the possible
     * values. For example, in a Role implementation
     * this method will return all the roles defined
     * in the organization.
     *
     * @param token the <code>SSOToken</code> that will be used
     * to determine the possible values
     *
     * @return <code>ValidValues</code> object
     *
     * @exception SSOException if <code>SSOToken</code> is not valid
     * @exception PolicyException if unable to get the list of valid
     * names.
     */
    public ValidValues getValidValues(SSOToken token)
        throws SSOException, PolicyException;

    /**
     * Returns a list of possible values for the <code>Subject
     * </code> that satisfy the given <code>pattern</code>.
     *  The implementation must use the <code>SSOToken
     * </code> <i>token</i> provided to determine the possible
     * values. For example, in a Role implementation with the
     * search filter <code>*admin</code> this method will return all
     * the roles defined in the organization that end with <code>admin</code>
     * 
     * @param token the <code>SSOToken</code> that will be used
     * to determine the possible values
     * @param pattern search pattern that will be used to narrow
     * the list of valid names.
     * 
     * @return <code>ValidValues</code> object
     *
     * @exception SSOException if <code>SSOToken</code> is not valid
     * @exception PolicyException if unable to get the list of valid
     * names.
     */
    public ValidValues getValidValues(SSOToken token, String pattern)
        throws SSOException, PolicyException;

    /**
     * Returns the display name for the value for the given locale.
     * For all the valid values obtained through the methods
     * <code>getValidValues</code> this method must be called
     * by web and command line interface to get the corresponding display name.
     * The <code>locale</code> variable could be used by the
     * plugin to customize the display name for the given locale.
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
     * is not one of the valid values for the plugin
     */
    public String getDisplayNameForValue(String value, Locale locale)
        throws NameNotFoundException;

    /**
     * Returns the values that was set using the
     * method <code>setValues</code>.
     *
     * @return <code>Set</code> of values that have been set for the 
     * user collection.
     */
    public Set getValues();

    /**
     * Initialize (or configure) the <code>Subject</code>
     * object. Usually it will be initialized with the environment
     * parameters set by the system administrator via admin console.
     * For example in a Role implementation, the configuration
     * parameters could specify the directory server name, port, etc.
     *
     * @param configParams configuration parameters as a <code>Map</code>.
     * The values in the map is <code>java.util.Set</code>,
     * which contains one or more configuration parameters.
     *
     * @exception PolicyException if an error occurred during
     * initialization of <code>Subject</code> instance
     */
    public void initialize(Map configParams) throws PolicyException;

    /**
     * Sets the names for the instance of the <code>Subject</code>
     * object. The names are obtained from the <code>Policy</code> object,
     * usually configured when a policy is created. For example
     * in a Role implementation, this would be name of the role.
     *
     * @param names names selected for the instance of
     * the user collection object.
     *
     * @exception InvalidNameException if the given names are not valid
     */
    public void setValues(Set names) throws InvalidNameException;

    /**
     * Determines if the user belongs to this instance
     * of the <code>Subject</code> object.
     * For example, a Role implementation
     * would return <code>true</code> if the user belongs
     * the specified role; <code>false</code> otherwise.
     *
     * @param token single-sign-on token of the user
     *
     * @return <code>true</code> if the user is member of the
     * given subject; <code>false</code> otherwise.
     *
     * @exception SSOException if SSO token is not valid
     * @exception PolicyException if an error occurred while
     * checking if the user is a member of this subject
     */
    public boolean isMember(SSOToken token)
        throws SSOException, PolicyException;

    /**
     * Creates and returns a copy of this object.
     *
     * @return a copy of this object
     */
    public Object clone();
}
