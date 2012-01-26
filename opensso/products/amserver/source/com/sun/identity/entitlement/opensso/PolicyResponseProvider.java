/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package com.sun.identity.entitlement.opensso;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.ResourceAttribute;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.shared.JSONUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class is used to wrap legacy response providers from previous OpenAM
 * versions and makes them available to the OpenAM entitlement framework. Note
 * that this class is only used for custom Response Provider implementations.
 * 
 * @author Steve Ferris
 */
public class PolicyResponseProvider implements ResourceAttribute {
    private String className;
    private Set<String> propertyValues;
    private String pResponseProviderName;
    private String propertyName;
    
    /**
     * Default constructor
     */
    public PolicyResponseProvider() {   
    }
    
    /**
     * Used by the entitlement framework to create an instance of this class
     * 
     * @param pResponseProviderName The name of the instance of the rp
     * @param className The underlying (custom) policy 
     * @param propertyName The name of the property for this wrapper
     * @param propertyValues The set of values for this wrapper
     */
    public PolicyResponseProvider(
            String pResponseProviderName,
            String className,
            String propertyName,
            Set<String> propertyValues) {
        this.className = className;
        this.pResponseProviderName = pResponseProviderName;
        this.propertyName = propertyName;
        this.propertyValues = propertyValues;
    }
    
    /**
     * Returns the name of the underlying response provider implementation
     * 
     * @return The class name 
     */
    public String getClassName() {
        return className;
    }

    /**
     * Set the name of the property associated with this wrapper
     * 
     * @param name The property name
     */
    public void setPropertyName(String name) {
        this.propertyName = name;
    }

    /**
     * Get the name of the property associated with this wrapper.
     * 
     * @return The property name
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Get the name of the property values for this wrapper
     * 
     * @return The property values
     */
    public Set<String> getPropertyValues() {
        return propertyValues;
    }
    
    /**
     * Sets the property values associated with this wrapper
     * 
     * @param propertyValues The property values
     */
    public void setPropertyValues(Set<String> propertyValues) {
        this.propertyValues = propertyValues;
    }

    /**
     * Called by the entitlements framework to fetch its resource attributes;
     * cascades the call through to the configured response provider implementation
     * 
     * @param adminSubject The admin user executing the policy eval
     * @param realm The realm of the policy eval
     * @param subject The user who is subject to the policy eval
     * @param resourceName The resource name of the policy eval
     * @param environment environment map from the policy eval client
     * @return The attributes (only one since resource attributes are singled)
     * @throws EntitlementException 
     */
    public Map<String, Set<String>> evaluate(Subject adminSubject, String realm, Subject subject, String resourceName, Map<String, Set<String>> environment) throws EntitlementException {
        try {
            com.sun.identity.policy.interfaces.ResponseProvider rp =
                (com.sun.identity.policy.interfaces.ResponseProvider)
                Class.forName(className).newInstance();
            Map<String, Set<String>> properties = new HashMap<String, Set<String>>();
            properties.put(propertyName, propertyValues);
            rp.setProperties(properties);
            SSOToken token = (subject != null) ? getSSOToken(subject) : null;
            Map<String, Set<String>> result = rp.getResponseDecision(token, environment);
            
            return result;
        } catch (SSOException ex) {
            throw new EntitlementException(510, ex);
        } catch (PolicyException ex) {
            throw new EntitlementException(510, ex);
        } catch (ClassNotFoundException ex) {
            throw new EntitlementException(510, ex);
        } catch (InstantiationException ex) {
            throw new EntitlementException(510, ex);
        } catch (IllegalAccessException ex) {
            throw new EntitlementException(510, ex);
        }
    }

    /**
     * Sets the name of the response provider within the policy
     * 
     * @param pResponseProviderName The provider name
     */
    public void setPResponseProviderName(String pResponseProviderName) {
        this.pResponseProviderName = pResponseProviderName;
    }

    /**
     * Return the response provider name
     * 
     * @return The provider name
     */
    public String getPResponseProviderName() {
        return pResponseProviderName;
    }
    
    /** 
     * Returns the state of the wrapper in a JSON representation
     * 
     * @return the state in JSON format
     */
    public String getState() {
        JSONObject jo = new JSONObject();

        try {
            jo.put("className", className);
            jo.put("propertyValues", propertyValues);
            jo.put("pResponseProviderName", pResponseProviderName);
            jo.put("propertyName", propertyName);
            return jo.toString(2);
        } catch (JSONException ex) {
            PrivilegeManager.debug.error("PolicyCondition.getState", ex);
        }
        return "";
    }

    /**
     * Given a JSON state representation, updates the class appropriately
     * @param state 
     */
    public void setState(String state) {
        try {
            JSONObject jo = new JSONObject(state);
            this.className = jo.optString("className");
            this.propertyValues = JSONUtils.getSet(jo, "propertyValues");
            this.pResponseProviderName = jo.optString("pResponseProviderName");
            this.propertyName = jo.optString("propertyName");
        } catch (JSONException e) {
            PrivilegeManager.debug.error("PolicyCondition.setState", e);
        }
    }
    
    @Override
    public String toString() {
        return getState();
    }
    
    // helper methods here on down
    private static SSOToken getSSOToken(Subject subject) {
        Set privateCred = subject.getPrivateCredentials();
        
        for (Iterator i = privateCred.iterator(); i.hasNext(); ) {
            Object o = i.next();
            
            if (o instanceof SSOToken) {
                return (SSOToken)o;
            }
        }
        
        return null;
    }
    
    private Map<String, Set<String>> getProperties(JSONObject jo) 
        throws JSONException {
        Map<String, Set<String>> result = new HashMap<String, Set<String>>();
        
        for (Iterator i = jo.keys(); i.hasNext(); ) {
            String key = (String)i.next();
            JSONArray arr = (JSONArray)jo.opt(key);
            Set set = new HashSet<String>();
            result.put(key, set);

            for (int j = 0; j < arr.length(); j++) {
                set.add(arr.getString(j));
            }
        }
        
        return result;
    }
}
