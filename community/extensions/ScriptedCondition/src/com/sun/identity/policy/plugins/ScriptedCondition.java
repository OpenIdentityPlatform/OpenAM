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
 * ScriptedCondition
 * Portions Copyrighted 2013 Robert Meakins
 */

package com.sun.identity.policy.plugins;

import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.policy.ConditionDecision;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.ResBundleUtils;
import com.sun.identity.policy.Syntax;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.shared.debug.Debug;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Collections;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * The class <code>ScriptedCondition</code> is a plugin implementation 
 * of <code>Condition</code> that allows attaching a defined script. The script
 * returns a <code>Boolean</code> or <code>String</code> result from which a
 * <code>ConditionDecision</code> can be built.
 *
 * Currently, the only supported script language is JavaScript.
 * 
 * The script should return either a <code>Boolean</code> or a
 * <code>String</code> with one of the following formats:
 * DECISION
 * DECISION,ADVICE,ADVICEMESSAGE
 * DECISION,ADVICE,ADVICEMESSAGE,ADVICEMESSAGE,...
 * where:
 * - DECISION is either "true" or "false" (just the first letter will also work)
 * - ADVICE is the String format of the advice, as shown in the
 *   <code>Condition</code> interface, e.g. "AuthenticateToRealmConditionAdvice"
 * - ADVICEMESSAGE is a parameter that the advice uses to direct the user
 *   further. Following the previous example, an ADVICEMESSAGE of "/" would
 *   cause the agent to redirect the user to login at the root realm.
 * 
 * The script has access to the <code>SSOToken</code> and "env" parameters and
 * can access elements of these using this syntax:
 * <code>token.get('Principal')</code>
 * <code>env.get('requestIp')</code>
 * 
 * The SSOToken and "env" map are wrapped such that only the "get" method is
 * available to the script. This protects the contained properties from
 * modification by the script.
 * 
 * Example 1:
 * env.get('requestIp').startsWith('192.168.');
 * 
 * Example 2:
 * if (env.get('Principal').endsWith('dc=forgerock,dc=org'))
 * {'true';}
 * else
 * {'false,AuthenticateToRealmConditionAdvice,ForgeRockRealm';}
 */
public class ScriptedCondition implements Condition {

    private static final Debug DEBUG 
        = Debug.getInstance(PolicyManager.POLICY_DEBUG_NAME);

    private static final boolean debugMessageEnabled 
            = DEBUG.messageEnabled(); 

    private Map properties;

    private static final String RESULT_SEPARATOR = ",";
    private static final String SCRIPT_CODE = "ScriptCode";
    private static final String JAVASCRIPT_VAR_NAME_TOKEN = "token";
    private static final String JAVASCRIPT_VAR_NAME_ENV = "env";
    private static List propertyNames = new ArrayList(1);

    static {
        propertyNames.add(SCRIPT_CODE);
    }

    /** No argument constructor 
     */
    public ScriptedCondition() {
    }

    /**
     * Returns a list of property names for the condition.
     *
     * @return list of property names
     */
    public List getPropertyNames() {
        return (new ArrayList(propertyNames));
    }

    /**
     * Returns the syntax for a property name
     * @see com.sun.identity.policy.Syntax
     *
     * @param property property name
     *
     * @return <code>Syntax</code> for the property name
     */
    public Syntax getPropertySyntax(String property) {
        return Syntax.ANY;
    }

    /**
     * Returns the display name for the property name.
     * The <code>locale</code> variable could be used by the plugin to
     * customize the display name for the given locale.
     * The <code>locale</code> variable could be <code>null</code>, in which
     * case the plugin must use the default locale.
     *
     * @param property property name
     * @param locale locale for which the property name must be customized
     * @return display name for the property name
     * @throws PolicyException  if unable to get display name
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
     * @return Set of valid values for the property.
     * @throws PolicyException if unable to get valid values
     */
    public Set getValidValues(String property) throws PolicyException {
        return Collections.EMPTY_SET;
    }

    /** Sets the properties of the condition.  
     *  Evaluation of <code>ConditionDecision</code> is influenced by these
     *  properties.
     *  @param properties the properties of the condition that governs
     *         whether a policy applies. The properties should 
     *         define value for the key <code>SCRIPT_CODE</code>. 
     *         The value should be a Set with only one element. The element
     *         should be a <code>String</code>, the script which is evaluated to
     *         produce a decision.
     *
     *  @throws PolicyException if properties is <code>null</code> 
     *          or does not contain
     *          value for the key <code>SCRIPT_CODE</code> or 
     *          the value of the key is not a Set with one 
     *         <code>String</code> element 
     *
     */
    public void setProperties(Map properties) throws PolicyException {
        this.properties = properties;
        validateProperties();
    }

    /** Returns the properties of the condition.  
     *  @return  unmodifiable map view of properties that govern the 
     *           evaluation of the condition.
     *           Please note that properties is not cloned before returning
     *  @see #setProperties(Map)
     */
    public Map getProperties() {
        return (properties == null)
                ? null : Collections.unmodifiableMap(properties);
    }

    /**
     * Returns the decision computed by this condition object, based on the 
     * map of environment parameters 
     *
     * @param token single sign on token of the user
     *
     * @param env request specific environment map of key/value pairs
     *
     * @return the condition decision. The condition decision encapsulates
     *         whether a policy applies for the request and advice messages
     *         generated by the condition.  
     *
     * Policy framework continues evaluating a  policy only if it applies 
     * to the request  as indicated by the <code>ConditionDecision</code>. 
     * Otherwise, further evaluation of the policy is skipped. 
     * However, the advice messages encapsulated in the 
     * <code>ConditionDecision</code> are aggregated and passed up, encapsulated
     * in the policy decision.
     * 
     * The ScriptedCondition will evaluate a script, which should return a
     * <code>String</code> object. 
     *
     * @throws PolicyException if the condition has not been initialized with a
     *        successful call to <code>setProperties(Map)</code>, if the script
     *        execution encounters an exception, or if the script result cannot
     *        be parsed correctly.
     * @throws SSOException if the token is invalid
     *
     * @see #setProperties(Map)
     * @see com.sun.identity.policy.ConditionDecision
     */
    public ConditionDecision getConditionDecision(SSOToken token, Map env) 
            throws PolicyException, SSOException {
        boolean allowed = false;
        
        if (debugMessageEnabled) {
            DEBUG.message("ScriptedCondition.getConditionDecision(): begin");
        }
        
        // Obtain the configured script
        String scriptCode = getScriptFromProperties();
        
        // Put the SSOToken in a wrapper, to prevent the script from changing it
        ImmutableSSOTokenWrapper ssoTokenWrapper = new ImmutableSSOTokenWrapper(token);
        
        // Create the script engine and populate it with context vars
        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine engine = sem.getEngineByName("JavaScript");
        sem.put(JAVASCRIPT_VAR_NAME_TOKEN, ssoTokenWrapper);
        sem.put(JAVASCRIPT_VAR_NAME_ENV, Collections.unmodifiableMap(env));
        
        Object scriptResult = null;
        try {
            // Execute the script and obtain the return value
            scriptResult = engine.eval(scriptCode);
        } catch (ScriptException se) {
            DEBUG.error("ScriptedCondition.getConditionDecision(): Script evaluation error", se);
            throw new PolicyException("Script evaluation error", se);
        }
        
        if (debugMessageEnabled) {
            DEBUG.message("ScriptedCondition.getConditionDecision(): Script result: " + String.valueOf(scriptResult));
        }
        
        if (scriptResult instanceof Boolean) {
            allowed = (Boolean)scriptResult;
            return new ConditionDecision(allowed);
        } else if (scriptResult instanceof String) {
            allowed = parseResultForDecision((String)scriptResult);
        } else {
            DEBUG.error("ScriptedCondition.getConditionDecision(): Script return value is not a Boolean or String.");
            throw new PolicyException("Script return value is not a Boolean or String.");
        }
        
        if (allowed) {
            return new ConditionDecision(allowed);
        }
        
        // If decision is "not allowed", check for advices
        Map advices = parseResultForAdvices((String)scriptResult);
        return new ConditionDecision(allowed, advices);
    }

    /**
     * Returns a copy of this object.
     *
     * @return a copy of this object
     */
    public Object clone() {
        ScriptedCondition theClone = null;
        try {
            theClone = (ScriptedCondition) super.clone();
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
     * Checks the properties set using setProperties() method for
     * validity like, not null, presence of SCRIPT_CODE property,
     * and no other invalid property.
     */
    private boolean validateProperties() throws PolicyException {
        if ( (properties == null) || ( properties.keySet() == null) ) {
            throw new PolicyException(
                    ResBundleUtils.rbName,
                    "properties_can_not_be_null_or_empty", null, null);
        }

        Set keySet = properties.keySet();
        //Check if the required key(s) are defined
        if ( !keySet.contains(SCRIPT_CODE) ) {
            String args[] = { SCRIPT_CODE };
            throw new PolicyException(
                    ResBundleUtils.rbName,"property_value_not_defined", 
                    args, null);
        }

        //Check if all the keys are valid 
        Iterator keys = keySet.iterator();
        while ( keys.hasNext()) {
            String key = (String) keys.next();
            if ( !SCRIPT_CODE.equals(key) ) {
                String args[] = {key};
                throw new PolicyException(
                        ResBundleUtils.rbName,
                        "attempt_to_set_invalid_property ",
                        args, null);
            }
        }

        //validate SCRIPT_CODE
        Set scriptCodeSet = null;
        try {
            scriptCodeSet = (Set) properties.get(SCRIPT_CODE);
        } catch (ClassCastException e) {
            String args[] = { SCRIPT_CODE };
            throw new PolicyException(
                    ResBundleUtils.rbName, "property_is_not_a_Set", 
                    args, e);
        }
        if ( scriptCodeSet != null ) {
            validateScriptCode(scriptCodeSet);
        }

        return true;

    }

    /**
     * Validates the realm names provided to the setProperties()
     * call for the SCRIPT_CODE key. Checks for null and throws
     * Exception if null or not a String.
     */

    private boolean validateScriptCode(Set scriptCodeSet) 
            throws PolicyException {
        if (scriptCodeSet.isEmpty()) {
            String args[] = { SCRIPT_CODE };
            throw new PolicyException(
                    ResBundleUtils.rbName,
                    "property_does_not_allow_empty_values", 
                    args, null);
        }
        String scriptCode = null;
        Iterator scriptCodeSetIter = scriptCodeSet.iterator();
        try {
            scriptCode = (String) scriptCodeSetIter.next();
        } catch (ClassCastException e) {
            String args[] = { SCRIPT_CODE };
            throw new PolicyException(
                    ResBundleUtils.rbName,"property_is_not_a_String", 
                    args, null);
        }
        return true;
    }

    /*
     * Examine the <code>String</code> returned by the script for a decision.
     * Only the first character of the <code>String</code> is examined; a 't' or
     * 'T' will cause the <code>ConditionDecision</code> to be true and an 'f'
     * or 'F' will cause it to be false.
     */
    private static boolean parseResultForDecision(String scriptResult)
            throws PolicyException
    {
        boolean decision = false;
        
        if (debugMessageEnabled) {
            DEBUG.message("ScriptedCondition.parseResultForDecision(): begin");
        }
        
        if (scriptResult.length() == 0) {
            throw new PolicyException("ScriptedCondition.parseResultForDecision(): Script returned no result.");
        }
        
        char firstLetter = scriptResult.trim().charAt(0);
        if (firstLetter == 't' || firstLetter == 'T') {
            decision = true;
        } else if (firstLetter == 'f' || firstLetter == 'F') {
            decision = false;
        } else {
            throw new PolicyException("Script did not return true or false. "
                    + "scriptResult=" + scriptResult);
        }
        
        if (debugMessageEnabled) {
            DEBUG.message("ScriptedCondition.parseResultForDecision(): Parsed decision: " + decision);
        }
        
        return decision;
    }
    
    /*
     * Examine the <code>String</code> returned by the script for any "advices"
     * and advice parameters which may have been returned.
     */
    private static Map parseResultForAdvices(String scriptResult)
            throws PolicyException
    {
        Map advices = new HashMap();
        
        if (debugMessageEnabled) {
            DEBUG.message("ScriptedCondition.parseResultForAdvices(): begin");
        }
        
        String[] results = scriptResult.split(RESULT_SEPARATOR);
        
        if (results.length == 2) {
            throw new PolicyException("Script returned advice without"
                    + " a parameter.");
            
        } else if (results.length > 2) {
            Set adviceMessages = new HashSet(results.length - 2);
            for (int i = 2; i < results.length; i++) {
                adviceMessages.add(results[i]);
            }
            advices.put(results[1], adviceMessages);
        }
        
        if (debugMessageEnabled) {
            DEBUG.message("ScriptedCondition.parseResultForAdvices(): Parsed advices: " + advices.toString());
        }
        
        return advices;
    }
    
    private String getScriptFromProperties()
            throws PolicyException
    {
        Set scriptCodeSet = null;
        try {
            scriptCodeSet = (Set) properties.get(SCRIPT_CODE);
        } catch (ClassCastException e) {
            String args[] = { SCRIPT_CODE };
            throw new PolicyException(
                    ResBundleUtils.rbName, "property_is_not_a_Set", 
                    args, e);
        }
        
        if (scriptCodeSet.isEmpty()) {
            String args[] = { SCRIPT_CODE };
            throw new PolicyException(
                    ResBundleUtils.rbName,
                    "property_does_not_allow_empty_values", 
                    args, null);
        }
        String scriptCode = null;
        Iterator scriptCodeSetIter = scriptCodeSet.iterator();
        try {
            scriptCode = (String) scriptCodeSetIter.next();
        } catch (ClassCastException e) {
            String args[] = { SCRIPT_CODE };
            throw new PolicyException(
                    ResBundleUtils.rbName,"property_is_not_a_String", 
                    args, null);
        }
        return scriptCode;
    }
    
    private class ImmutableSSOTokenWrapper implements Map {
        SSOToken token = null;
        
        public ImmutableSSOTokenWrapper(SSOToken ssoToken) {
            token = ssoToken;
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException("Operation not supported.");
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean containsKey(Object key) {
            return (get(key) != null);
        }

        @Override
        public boolean containsValue(Object value) {
            throw new UnsupportedOperationException("Operation not supported.");
        }

        @Override
        public Object get(Object key) {
            try {
                return token.getProperty(key.toString());
            } catch (SSOException ex) {
                
            }
            return null;
        }

        @Override
        public Object put(Object key, Object value) {
            throw new UnsupportedOperationException("Operation not supported.");
        }

        @Override
        public Object remove(Object key) {
            throw new UnsupportedOperationException("Operation not supported.");
        }

        @Override
        public void putAll(Map m) {
            throw new UnsupportedOperationException("Operation not supported.");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("Operation not supported.");
        }

        @Override
        public Set keySet() {
            throw new UnsupportedOperationException("Operation not supported.");
        }

        @Override
        public Collection values() {
            throw new UnsupportedOperationException("Operation not supported.");
        }

        @Override
        public Set entrySet() {
            throw new UnsupportedOperationException("Operation not supported.");
        }
    }
    
}
