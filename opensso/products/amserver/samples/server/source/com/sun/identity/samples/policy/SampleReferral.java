/**
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
 * $Id: SampleReferral.java,v 1.2 2008/09/05 01:45:02 dillidorai Exp $
 *
 */

package com.sun.identity.samples.policy;

import java.io.*;
import java.util.*;

import com.sun.identity.policy.*;
import com.sun.identity.policy.interfaces.Referral;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.iplanet.am.util.SystemProperties;

public class SampleReferral implements Referral {

    static final String SEPARATOR = ":";
    static String PROPERTIES = "samples/policy/SampleReferral.properties";
    static String INSTALL_DIR = SystemProperties.get("com.iplanet.am.installdir");
    static Properties properties = new Properties();
    private String _name;
    private Set _values;

    /** No argument constructor */
    public SampleReferral() {
    }

    /**Initializes the referral with a map of Configuration parameters
     * @param configurationMap a map containing configuration 
     *        information. Each key of the map is a configuration
     *        parameter. Each value of the key would be a set of values
     *        for the parameter. The map is cloned and a reference to the 
     *        clone is stored in the referral
     */
    public void initialize(Map configurationMap) {
    }

    /**Sets the name of this referral 
     * @param name name of this referral
     */
    private void setName(String name) {
        _name = name;
    }

    /**Gets the name of this referral 
     * @return the name of this referral
     */
    private String getName() {
        return _name;
    }            

    /**Sets the values of this referral.
     * @param values a set of values for this referral
     *        Each element of the set has to be a String
     * @throws InvalidNameException if any value passed in the 
     * values is invalid
     */
    public void setValues(Set values) throws InvalidNameException {
        _values = values;
    }

    /**Gets the values of this referral 
     * @return the values of this referral
     *                Each element of the set would be a String
     */
    public Set getValues() {
        return _values;
    }

    /**
     * Returns the display name for the value for the given locale.
     * For all the valid values obtained through the methods
     * <code>getValidValues</code> this method must be called
     * by GUI and CLI to get the corresponding display name.
     * The <code>locale</code> variable could be used by the
     * plugin to customize
     * the display name for the given locale.
     * The <code>locale</code> variable
     * could be <code>null</code>, in which case the plugin must
     * use the default locale (most probabily en_US).
     * This method returns only the display name and should not
     * be used for the method <code>setValues</code>.
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
	return value;
    }

    /**Gets the valid values for this referral 
     * @param token SSOToken
     * @return <code>ValidValues</code> object
     * @throws SSOException, PolicyException
     */
    public ValidValues getValidValues(SSOToken token) 
            throws SSOException, PolicyException {
        return getValidValues(token, "*");
    }

    /**Gets the valid values for this referral 
     * matching a pattern
     * @param token SSOToken
     * @param pattern a pattern to match against the value
     * @return </code>ValidValues</code> object
     * @throws SSOException, PolicyException
     */
    public ValidValues getValidValues(SSOToken token, String pattern)
            throws SSOException, PolicyException {
        Set values = new HashSet();
        values.add(PROPERTIES);
        return (new ValidValues(ValidValues.SUCCESS,
                                values));
    }

    /**Gets the syntax for the value 
     * @param token SSOToken
     * @see com.sun.identity.policy.Syntax
     */
    public Syntax getValueSyntax(SSOToken token)
            throws SSOException, PolicyException {
        return (Syntax.SINGLE_CHOICE);
    }

    /**Gets the name of the ReferralType 
     * @return name of the ReferralType representing this referral
     */
    public String getReferralTypeName() 
    {
        return "SampleReferral";
    }

    /**Gets policy results 
     * @param token SSOToken
     * @param resourceType resource type
     * @param resourceName name of the resource 
     * @param actionNames a set of action names
     * @param envParameters a map of enivronment parameters.
     *        Each key is an environment parameter name.
     *        Each value is a set of values for the parameter.
     * @return policy decision
     * @throws SSOException
         * @throws PolicyException
     */
    public PolicyDecision getPolicyDecision(SSOToken token, String resourceType, 
	    String resourceName, Set actionNames, Map envParameters) 
            throws SSOException, PolicyException {

        PolicyDecision pd = new PolicyDecision();
        Iterator elements = _values.iterator();
        if (!elements.hasNext()) {
            return pd;
        }

        String fileName = (String)elements.next(); 
        fileName = INSTALL_DIR + "/" + fileName;
        try {
            InputStream is = new FileInputStream(fileName);
            if (is == null) {
                return pd;
            }
            properties.load(is);
        } catch (Exception e) {
            return pd;
        }

        String serviceName = getProperty("servicename");
        if (!serviceName.equals(resourceType)) {
            return pd;
        }

        String resName = getProperty("resourcename");
        if (!resName.equals(resourceName)) {
            return pd;
        }
       
        List actionNameList = getPropertyValues("actionnames");
        List actionValueList = getPropertyValues("actionvalues");

        int numOfActions = actionNameList.size();
        int numOfValues = actionValueList.size();

        if ((numOfActions == 0 || (numOfValues == 0) 
                               || numOfActions != numOfValues)) {
            return pd;
        } 

        Iterator namesIter = actionNameList.iterator();
        Iterator valuesIter = actionValueList.iterator();

        for (int i = 0; i < numOfActions; i++) {
            String actionName = (String)namesIter.next();
            String actionValue = (String)valuesIter.next();
            if (actionNames.contains(actionName)) {
                Set values = new HashSet();
                values.add(actionValue);
                ActionDecision ad = new ActionDecision(
                    actionName, values, null, Long.MAX_VALUE);
                pd.addActionDecision(ad);            
            }
        }
        return pd;
    }


    private String getProperty(String key)
    {
        return properties.getProperty(key);
    }


    private List getPropertyValues(String name) {
        List values = new ArrayList();
        String value = getProperty(name);
        if ( value != null ) {
            StringTokenizer st = new StringTokenizer(value, SEPARATOR);
            while ( st.hasMoreTokens() ) {
               values.add(st.nextToken());
            }
        }
        return values;
    }

    /** Gets resource names rooted at the given resource name for the given
     *  serviceType that could be governed by this referral 
     * @param token ssoToken sso token
     * @param serviceTypeName service type name
     * @param rsourceName resource name
     * @return names of sub resources for the given resourceName.
     *         The return value also includes the resourceName.
     *
     * @throws PolicyException
     * @throws SSOException
     */
    public Set getResourceNames(SSOToken token, String serviceTypeName, 
            String resourceName) throws PolicyException, SSOException {
        return null;
    }

}
