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
 * $Id: IDRepoResponseProvider.java,v 1.4 2008/06/25 05:43:51 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy.plugins;

import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyUtils;
import com.sun.identity.policy.PolicyConfig;
import com.sun.identity.policy.ResBundleUtils;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.interfaces.ResponseProvider;
import com.sun.identity.policy.Syntax;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;


import com.sun.identity.shared.debug.Debug;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;
import java.util.Collections;

/**
 * This class is an out of the box implementation of  
 * <code>ResponseProvider</code> interface. It defines 2 types of user 
 * attributes which it can fetch the values of: <code>STATIC</code>
 * and <code>DYNAMIC</code>.
 * It relies on underlying Identity repository service to 
 * fetch the attribute values for the Subject(s) defined in the policy.
 * It computes a <code>Map</code> of response attributes
 * based on the sso token, resource name and  <code>env</code> map passed 
 * in the method call <code>getResponseDecision()</code>.
 *
 * Policy framework would make a call to the ResponseProvider to fetch 
 * response attributes in a policy only if the policy is applicable to a 
 * request as determined by sso token, resource name, Subjects and Conditions.
 *
 */
public class IDRepoResponseProvider implements ResponseProvider {

    private static final Debug DEBUG 
        = Debug.getInstance(PolicyManager.POLICY_DEBUG_NAME);

    public static final String STATIC_ATTRIBUTE = "StaticAttribute";
    public static final String DYNAMIC_ATTRIBUTE = "DynamicAttribute";
    public static final String ATTR_DELIMITER = "=";
    public static final String VAL_DELIMITER = "|";

    private Map properties;
    private static List propertyNames = new ArrayList(2);

    private boolean initialized=false;
    private String orgName = null;
    private Set validDynamicAttrNames = null;
    private Map staticResponse = null;
    private Set responseAttrNames = null; //for dynamic attributes
    private Set repoAttrNames = null; //for dynamic attributes
    private Map responseAttrToRepoAttr = null;

    static {
        propertyNames.add(STATIC_ATTRIBUTE);
        propertyNames.add(DYNAMIC_ATTRIBUTE);
    }

    /**
     * No argument constructor.
     */
    public IDRepoResponseProvider () {

    }


    /** 
     * Initialize the IDRepoResponseProvider object by using the configuration
     * information passed by the Policy Framework.
     * @param configParams the configuration information
     * @exception PolicyException if an error occured during 
     * initialization of the instance
     */

    public void initialize(Map configParams) throws PolicyException {

        if (DEBUG.messageEnabled()) {
            DEBUG.message("IDRepoResponseProvider.initialize():"
                + "entering");
        }

        if (configParams == null) {
            throw (new PolicyException(ResBundleUtils.rbName,
                "idrepo_initialization_failed", null, null));
        }

        // get the organization name
        Set orgNameSet = (Set) configParams.get(
                                     PolicyManager.ORGANIZATION_NAME);
        if ((orgNameSet != null) && (!orgNameSet.isEmpty())) {
            Iterator items = orgNameSet.iterator();
            orgName = (String) items.next();
        }

        validDynamicAttrNames = (Set)configParams.get(
            PolicyConfig.SELECTED_DYNAMIC_ATTRIBUTES);
        if ( validDynamicAttrNames == null) {
            validDynamicAttrNames = Collections.EMPTY_SET;
        }
        initialized = true;

        if (DEBUG.messageEnabled()) {
            DEBUG.message("IDRepoResponseProvider.initialize():"
                + "initialized with:"
                + "orgName=" + orgName
                + ",validDynamicAttrNames=" + validDynamicAttrNames);
        }

    }


    /**
     * Returns a list of property names for the responseprovider.
     *
     * @return <code>List</code> of property names
     */
    public List getPropertyNames()  {
         return propertyNames;
    }

    /**
     * Returns the syntax for a property name
     * @see com.sun.identity.policy.Syntax
     *
     * @param property property name
     *
     * @return <code>Syntax<code> for the property name
     */
    public Syntax getPropertySyntax(String property) {
        if (property.equals(STATIC_ATTRIBUTE)) {
            return (Syntax.ANY);
        }
        if (property.equals(DYNAMIC_ATTRIBUTE)) {
            return (Syntax.MULTIPLE_CHOICE);
        }
        return (Syntax.ANY);
    }
        
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
            throws PolicyException {
        return property;
    }

    /**
     * Returns a set of valid values given the property name. This method
     * is called if the property Syntax is either the SINGLE_CHOICE or
     * MULTIPLE_CHOICE.
     *
     * @param property property name
     * from the PolicyConfig Service configured for the specified realm.
     * @return Set of valid values for the property.
     * @exception PolicyException if unable to get the Syntax.
     */
    public Set getValidValues(String property) throws PolicyException {
        if (!initialized) {
            throw (new PolicyException(ResBundleUtils.rbName,
                "idrepo_response_provider_not_yet_initialized", null, null));
        }
        if (property.equals(DYNAMIC_ATTRIBUTE)) {
            return validDynamicAttrNames;
        } else {
            return Collections.EMPTY_SET;
        }
    }

    /** Sets the properties of the responseProvider plugin.
     *  This influences the response attribute-value Map that would be
     *  computed by a call to method <code>getResponseDecision(Map)</code>
     *  These attribute-value pairs are encapsulated in 
     *  <code>ResponseAttribute</code> element tag which is a child of the 
     *  <code>PolicyDecision</code> element in the PolicyResponse xml
     *  if the policy is applicable to the user for the resource, subject and
     *  conditions defined.
     *  @param properties the properties of the responseProvider
     *         Keys of the properties have to be String.
     *         Value corresponding to each key have to be a Set of String
     *         elements. Each implementation of ResponseProvider could add 
     *         further restrictions on the keys and values of this map.
     *  @throws PolicyException for any abnormal condition
     */
    public void setProperties(Map properties) throws PolicyException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("IDRepoResponseProvider.setProperties():"
                + "entering with properties=" + properties);
        }
        if ( (properties == null) || ( properties.isEmpty()) ) {
            throw new PolicyException(
                ResBundleUtils.rbName, "properties_can_not_be_null_or_empty",
                null, null);
        }
        this.properties = properties;

        //Check if the keys needed for this provider are present namely
         // STATIC_ATTRIBUTE and DYNAMIC_ATTRIBUTE
        if (!properties.containsKey(STATIC_ATTRIBUTE) &&
                !properties.containsKey(DYNAMIC_ATTRIBUTE)) {
            String args[] = { STATIC_ATTRIBUTE,DYNAMIC_ATTRIBUTE };
            throw new PolicyException(
                ResBundleUtils.rbName, "missing_required_property", 
                       args, null);
        }

        //validates STATIC_ATTRIBUTE and caches parsed static attributes map
        Set staticSet = (Set)properties.get(STATIC_ATTRIBUTE);
        if (staticSet != null) {
            validateStaticAttribute(staticSet);
        }

        //validates DYNAMIC_ATTRIBUTE and caches parsed
        // responseAttrNames, repoAttrNames, responseAttrToRepoAttr
        Set dynamicSet = (Set)properties.get(DYNAMIC_ATTRIBUTE);
        if (dynamicSet != null) {
            validateDynamicAttribute(dynamicSet);
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("IDRepoResponseProvider.setProperties():"
                + "returning");
        }
    }

    /** Gets the properties of the responseprovider
     *  @return properties of the responseprovider
     *  @see #setProperties
     */
    public Map getProperties() {
        return (properties == null) 
                ? null : Collections.unmodifiableMap(properties);
    }

    /**
     * Gets the response attributes computed by this ResponseProvider object,
     * based on the sso token and map of environment parameters
     *
     * @param token single-sign-on token of the user
     *
     * @param env specific environment map of key/value pairs
     * @return  a Map of response attributes.
     *          Keys of the Map are attribute names STATIC_ATTRIBUTE or
     *          DYNAMIC_ATTRIBUTE.
     *          Value is a Set of Strings representing response attribute 
     *          values.
     *
     * @throws PolicyException if the decision could not be computed
     * @throws SSOException if SSO token is not valid
     *
     */
    public Map getResponseDecision(SSOToken token, 
            Map env) throws PolicyException, SSOException { 

        if (DEBUG.messageEnabled()) {
            DEBUG.message("IDRepoResponseProvider.getResponseDecision():"
                + "entering");
        }

        Map returnValues = new HashMap();

        if (staticResponse != null) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("IDRepoResponseProvider."
                        + "getResponseDecision():"
                        + "adding staticResponse=" + staticResponse);
            }
            PolicyUtils.appendMapToMap(staticResponse, returnValues);
        }

        if ((repoAttrNames != null) && !repoAttrNames.isEmpty()) {
            Map dynamicResponse = new HashMap();
            Map idRepoMap = new HashMap();
            try {
                if (token.getPrincipal() != null) {
                    AMIdentity id = IdUtils.getIdentity(token);
                    idRepoMap = id.getAttributes(repoAttrNames);
                    if (idRepoMap != null) {
                        for (Iterator iter = responseAttrNames.iterator();
                                iter.hasNext(); ) {
                            String responseAttrName = (String)iter.next();
                            Set idRepoAttrNames 
                                = (Set)responseAttrToRepoAttr.get(
                                responseAttrName);
                            Set values = new HashSet();
                            for (Iterator iter1 = idRepoAttrNames.iterator();
                                    iter1.hasNext();) {
                                String repoAttrName = (String)iter1.next();
                                Set subValues 
                                        = (Set)idRepoMap.get(repoAttrName);
                                if (subValues != null) {
                                    values.addAll(subValues);
                                }
                            }
                            dynamicResponse.put(responseAttrName, values);
                        }
                    }
                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("IDRepoResponseProvider."
                                + "getResponseDecision():"
                                + "adding dynamicResponse=" + dynamicResponse);
                    }
                    PolicyUtils.appendMapToMap(dynamicResponse,returnValues);
                } else {
                    DEBUG.error("IDRepoResponseProvider:"+
                        "getResponseDecision(): Principal is null");
                    throw (new PolicyException(ResBundleUtils.rbName,
                        "token_principal_null", null, null));
                }
            } catch (IdRepoException ide) {
                DEBUG.error("IDRepoResponseProvider:"+
                    "getResponseDecision():" +"IdRepoException", ide);
                throw new PolicyException(ide);
            }
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("IDRepoResponseProvider.getResponseDecision():"
                + "returning response=" + returnValues);
        }

        return returnValues;
    }

    /**
     * This method validates the STATIC_ATTRIBUTE data
     * for format and caches parsed static attributes map
     * Needs to be in "attr=val" format. 
     * Else, throws PolicyException
     */
    private void validateStaticAttribute(Set staticSet) 
            throws PolicyException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("IDRepoResponseProvider.validateStaticAttribute():"
                + "entering with staticSet=" + staticSet);
        }
        if (!staticSet.isEmpty()) {
            staticResponse = new HashMap();
            for (Iterator it = staticSet.iterator(); it.hasNext();) {
                String attrValueString = (String)it.next();
                if (attrValueString.indexOf(ATTR_DELIMITER) == -1 ) {
                    clearProperties();
                    DEBUG.error("IDRepoResponseProvider"
                        + ".validateStaticAttribute():"
                        + " Invalid format in defining StaticAttribute, needs"
                        + " to be attr=value format");
                    String args[] = {attrValueString};
                    throw new PolicyException(ResBundleUtils.rbName, 
                            "invalid_format_static_property", args, null);
                } else {
                    int index = attrValueString.indexOf(ATTR_DELIMITER);
                    String attrName = attrValueString.substring(0,index).trim();
                    String attrValue = attrValueString.substring(index+1);
                    Set values = PolicyUtils.delimStringToSet(attrValue, 
                            VAL_DELIMITER);
                    PolicyUtils.appendElementToMap(attrName, values, 
                            staticResponse);
                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("IDRepoResponseProvider."
                        + "validateStaticAttribute():"
                        + "attrName=" + attrName
                        + ",values=" + values);
                        DEBUG.message("IDRepoResponseProvider."
                        + "validateStaticAttribute():"
                        + "caching staticResponse:"
                        + staticResponse );
                    }
                }
            }
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("IDRepoResponseProvider.validateStaticAttribute():"
                + "returning");
        }
    } 

    /**
     * This method validates the DYNAMIC_ATTRIBUTE data
     * for format and  caches parsed
     * responseAttrNames, repoAttrNames
     * Strings in the Set need to be in "responseAttr=repoAttr" format 
     * Else, throws PolicyException
     */
    private void validateDynamicAttribute(Set dynamicSet) 
            throws PolicyException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("IDRepoResponseProvider.validateDynamicAttribute():"
                + "entering with dynamicSet=" + dynamicSet);
        }

        responseAttrNames = new HashSet();
        repoAttrNames = new HashSet();
        responseAttrToRepoAttr = new HashMap();

        /* check if the attribute names being set in DYNAMIC_ATTRIBUTE
         * are valid i.e are as defined in policy config service.
         * Parse and store responseAttrNames and repoAttrNames
         */
        if (DEBUG.messageEnabled()) {
            DEBUG.message("IDRepoResponseProvider.validateDynamicAttribute():"
                +"valid dynamic attributes:" + validDynamicAttrNames);
        }

        Set dynamicAttrs = ((Set)properties.get(DYNAMIC_ATTRIBUTE));
        if (DEBUG.messageEnabled()) {
            DEBUG.message("IDRepoResponseProvider.validateDynamicAttribute():"
                +"selected dynamic attributes:" + dynamicAttrs);
        }
        Iterator dynamicAttrsIter = dynamicAttrs.iterator();
        while (dynamicAttrsIter.hasNext()) {
            String attr = (String) dynamicAttrsIter.next();
            if (!validDynamicAttrNames.contains(attr)) {
                if (DEBUG.warningEnabled()) {
                    DEBUG.warning("IDReporesponseProvider."
                        +"validateDynamicAttribute():Invalid dynamic property "
                        +"encountered:"+attr);
                }
                continue;
            }
            String[] attrNames = parseDynamicAttrName(attr);
            String responseAttrName = attrNames[0];
            String repoAttrName = attrNames[1];
            responseAttrNames.add(responseAttrName);
            repoAttrNames.add(repoAttrName);

            addToResponseAttrToRepoAttrMap(responseAttrName, repoAttrName);
            
            if (DEBUG.messageEnabled()) {
                DEBUG.message("IDRepoResponseProvider."
                    + "validateDynamicAttribute():"
                    +"responseAttrName=" + responseAttrName
                    + ", repoAttrName=" + repoAttrName);
            }
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("IDRepoResponseProvider.validateDynamicAttribute():"
                + "responseAttrToRepoAttr=" + responseAttrToRepoAttr);
            DEBUG.message("IDRepoResponseProvider.validateDynamicAttribute():"
                + "returning");
        }
    } 

    private String[] parseDynamicAttrName(String dynamicAttrName) 
            throws PolicyException {
        String[] parsedNames = new String[2];
        if (dynamicAttrName != null) {
            int delimiterIndex 
                    = dynamicAttrName.indexOf(ATTR_DELIMITER);
            if (delimiterIndex == 0) {
                    clearProperties();
                        String args[] = { dynamicAttrName };
                        throw new PolicyException(
                        ResBundleUtils.rbName, 
                        "invalid_dynamic_property_being_set", args, null);
            } else if (delimiterIndex < 0) {
                String value = dynamicAttrName.trim();
                if (value.length() == 0) {
                    clearProperties();
                        String args[] = { dynamicAttrName };
                        throw new PolicyException(
                        ResBundleUtils.rbName, 
                        "invalid_dynamic_property_being_set", args, null);
                }
                parsedNames[0] = value;
                parsedNames[1] = value;
            } else {
                String value1 
                        = dynamicAttrName.substring(0, delimiterIndex).trim();
                String value2 
                        = dynamicAttrName.substring(delimiterIndex + 1).trim();
                if ((value1.length() == 0) || (value2.length() == 0)) {
                    clearProperties();
                        String args[] = { dynamicAttrName };
                        throw new PolicyException(
                        ResBundleUtils.rbName, 
                        "invalid_dynamic_property_being_set", args, null);
                }
                parsedNames[0] = value1;
                parsedNames[1] = value2;
            }

        }
        return parsedNames;
    }

    private void addToResponseAttrToRepoAttrMap(String responseAttrName, 
            String repoAttrName) throws PolicyException {
        Set idRepoAttrNames 
                = (Set)responseAttrToRepoAttr.get(responseAttrName);
        if (idRepoAttrNames == null) {
            idRepoAttrNames = new HashSet();
            responseAttrToRepoAttr.put(responseAttrName, idRepoAttrNames);
        }
        idRepoAttrNames.add(repoAttrName);
    }

    private void clearProperties() {
        properties = null;
        staticResponse = null;
        responseAttrNames = null;
        repoAttrNames = null;
        responseAttrToRepoAttr = null;
    }

    /**
     * Returns a copy of this object.
     *
     * @return a copy of this object
     */
    public Object clone() {
        IDRepoResponseProvider theClone = null;
        try {
            theClone = (IDRepoResponseProvider)super.clone();
        } catch (CloneNotSupportedException e) {
            // this should never happen
            throw new InternalError();
        }

        if (validDynamicAttrNames != null) {
            theClone.validDynamicAttrNames = new HashSet();
            theClone.validDynamicAttrNames.addAll(validDynamicAttrNames);
        }

        if (properties != null) {
            theClone.properties = new HashMap();
            Iterator iter = properties.keySet().iterator();
            while (iter.hasNext()) {
                Object obj = iter.next();
                Set values = new HashSet();
                values.addAll((Set) properties.get(obj));
                theClone.properties.put(obj, values);
            }
        }

        if (staticResponse != null) {
            theClone.staticResponse = new HashMap();
            Iterator iter = staticResponse.keySet().iterator();
            while (iter.hasNext()) {
                Object obj = iter.next();
                Set values = new HashSet();
                values.addAll((Set) staticResponse.get(obj));
                theClone.staticResponse.put(obj, values);
            }
        }

        if (responseAttrNames != null) {
            theClone.responseAttrNames = new HashSet();
            theClone.responseAttrNames.addAll(responseAttrNames);
        }

        if (repoAttrNames != null) {
            theClone.repoAttrNames = new HashSet();
            theClone.repoAttrNames.addAll(repoAttrNames);
        }

        if (responseAttrToRepoAttr != null) {
            theClone.responseAttrToRepoAttr = new HashMap();
            Iterator iter = responseAttrToRepoAttr.keySet().iterator();
            while (iter.hasNext()) {
                Object obj = iter.next();
                Set values = new HashSet();
                values.addAll((Set) responseAttrToRepoAttr.get(obj));
                theClone.responseAttrToRepoAttr.put(obj, values);
            }
        }

        return theClone;
    }
}
