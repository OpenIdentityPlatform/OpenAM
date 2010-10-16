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
 * $Id: ResponseProvider.java,v 1.5 2008/08/19 19:09:16 veiming Exp $
 *
 */



package com.sun.identity.policy.interfaces;

import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.Syntax;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The class <code>ResponseProvider</code> defines an interface to allow 
 * pluggable response providers into the OpenSSO framework. These 
 * are used to provide policy response attributes. Policy response attributes 
 * are different from <code>ActionDecision</code>. Policy response attributes 
 * typically provide attribute values of user profile. User profile could 
 * exist in any data store managed by Identity repository. However, reponse 
 * attributes are not restricted to attributes from user profile. 
 * Source of the attribute values is completely at the discretion of the 
 * specific implementation of the <code>ResponseProvider</code>.
 * <p>
 * The response provider is initialized by calling its <code>initialize()
 * </code> method.
 * Its also configured by setting its properites by a call to 
 * <code>setProperties()</code> method.
 * <p>
 * Response attribute names are not checked against schema of the service
 * registered with OpenSSO. (<code>ActionDecision</code> attributes
 * are checked against the schema of the service registered with
 * OpenSSO).
 *
 * A Response Provider computes a <code>Map</code> of response attributes
 * and their values based on the <code>SSOToken</code>, resource name and  
 * environment <code>Map</code> passed in the method call 
 * <code>getResponseDecision()</code>.
 *
 * Policy framework would make a call <code>getResponseDecision</code> from the 
 * <code>ResponseProvider</code>(s) associated with a  policy only if the 
 * policy is applicable to a request as determined by <code>SSOToken</code>, 
 * <code>resource name</code>, <code>Subjects</code> and 
 * <code>Conditions</code>.
 * <p>
 * The only out-of-the-box <code>ResponseProvider</code> implementation 
 * provided with the Policy framework would be 
 * <code>IDRepoResponseProvider</code>.
 *
 * All <code>ResponseProvider</code> implementations should have a public no 
 * argument constructor.
 * @supported.all.api
 *
 */
public interface ResponseProvider extends Cloneable {

    /** 
     * Initialize the <code>ResponseProvider</code>
     * @param configParams <code>Map</code> of the configurational information
     * @exception PolicyException if an error occured during 
     * initialization of the instance
     */

    public void initialize(Map configParams) throws PolicyException;


    /**
     * Returns a list of property names for the Response provider.
     *
     * @return list of property names
     */
    public List getPropertyNames();

    /**
     * Returns the syntax for a property name
     * @see com.sun.identity.policy.Syntax
     *
     * @param property property name
     *
     * @return <code>Syntax<code> for the property name
     */
    public Syntax getPropertySyntax(String property);


    /**
     * Gets the display name for the property name.
     * The <code>locale</code> variable could be used by the plugin to
     * customize the display name for the given locale.
     * The <code>locale</code> variable could be <code>null</code>, in which
     * case the plugin must use the default locale.
     *
     * @param property property name
     * @param locale locale for which the property name must be customized
     * @return display name for the property name.
     * @throws PolicyException
     */
    public String getDisplayName(String property, Locale locale)
        throws PolicyException;

    /**
     * Returns a set of valid values given the property name. This method
     * is called if the property <code>Syntax</code> is either the 
     * <code>SINGLE_CHOICE</code> or <code>MULTIPLE_CHOICE</code>.
     *
     * @param property <code>String</code> representing property name
     * @return Set of valid values for the property.
     * @exception PolicyException if unable to get the <code>Syntax</code>.
     */
    public Set getValidValues(String property) throws 
        PolicyException;

    /** Sets the properties of the responseProvider plugin.
     *  This influences the response attribute-value Map that would be
     *  computed by a call to method <code>getResponseDecision(Map)</code>
     *  These attribute-value pairs are encapsulated in 
     *  <code>ResponseAttribute</code> element tag which is a child of the 
     *  <code>PolicyDecision</code> element in the PolicyResponse xml
     *  if the policy is applicable to the user for the resource, subject and
     *  conditions defined.
     *  @param properties the properties of the <code>ResponseProvider</code>
     *         Keys of the properties have to be String.
     *         Value corresponding to each key have to be a <code>Set</code> 
     *         of String elements. Each implementation of ResponseProvider 
     *         could add further restrictions on the keys and values of this 
     *         map.
     *  @throws PolicyException for any abnormal condition
     */
    public void setProperties(Map properties) throws PolicyException;

    /** Gets the properties of the response provider.
     *  @return properties of the response provider.
     *  @see #setProperties
     */
    public Map getProperties();

    /**
     * Gets the response attributes computed by this ResponseProvider object,
     * based on the <code>SSOToken</code> and <code>Map</code> of 
     * environment parameters.
     *
     * @param token single-sign-on token of the user
     *
     * @param env request specific environment map of key/value pairs
     * @return  a <code>Map</code> of response attributes.
     *          Keys of the Map are attribute names of type <code>static</code>
     *          and <code>dynamic</code>.
     *          Value is a <code>Set</code> of response attribute values 
     *          (<code>String</code>).
     *
     * @throws PolicyException if the decision could not be computed
     * @throws SSOException <code>token is not valid
     *
     */
    public Map getResponseDecision(SSOToken token,  
        Map env) throws PolicyException, SSOException;

    /**
     * Returns a copy of this object.
     *
     * @return an <code>Object</code> which is a copy of this object
     */
    public Object clone();

}
