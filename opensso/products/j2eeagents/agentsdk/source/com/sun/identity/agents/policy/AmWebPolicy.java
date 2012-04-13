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
 * $Id: AmWebPolicy.java,v 1.5 2008/06/25 05:51:56 qcheng Exp $
 *
 */

package com.sun.identity.agents.policy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.iplanet.sso.SSOToken;
import com.sun.identity.agents.arch.AgentBase;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.arch.ServiceFactory;
import com.sun.identity.agents.util.NameValuePair;
import com.sun.identity.policy.ActionDecision;
import com.sun.identity.policy.PolicyDecision;
import com.sun.identity.policy.client.PolicyEvaluator;
import com.sun.identity.policy.client.PolicyEvaluatorFactory;
import com.sun.identity.policy.plugins.IPCondition;
import com.sun.identity.shared.Constants;

/**
 * The class handles and evaluates URL policies
 */
public class AmWebPolicy extends AgentBase implements IAmWebPolicy,
    IWebPolicyConfigurationConstants {

    private static final String HTTP_METHOD_GET = "GET";    
    private static final String HTTP_METHOD_POST = "POST";    
    private static final String JSESSION = "JSESSION";    

    public AmWebPolicy(Manager manager) {
        super(manager);
    }
    
    public void initialize() throws AgentException {
        try {
            setAppSSOProvider(
                    ServiceFactory.getAmWebPolicyAppSSOProvider(getManager()));
            setPolicyEvaluator(PolicyEvaluatorFactory.getInstance().
                    getPolicyEvaluator(AM_WEB_SERVICE_NAME,
                    getAppSSOProvider()));
            initParamListOfGET();
            initParamListOfPOST();
            initParamListOfJSESSION();
            if (isLogMessageEnabled()) {
                logMessage("AmWebPolicy.initialize: Initialized.");
            }
        } catch (Exception ex) {
            throw new AgentException("Failed to initialize AmWebPolicy", ex);
        }
    }
   
    private void initParamListOfGET() {
        String[] paramList = getConfigurationStrings(CONFIG_GET_PARAM_LIST);
        if (paramList != null && paramList.length > 0) {
            for (int i=0; i<paramList.length; i++) {
                if (paramList[i] != null
                        && paramList[i].trim().length() > 0) {
                    getParamListOfGET().add(paramList[i]);
                }
            }
        }
        if (isLogMessageEnabled()) {
            logMessage("AmWebPolicy.initParamListOfGET: GET params="
                       + getParamListOfGET());
        }
    }
 
    private void initParamListOfPOST() {
        String[] paramList = getConfigurationStrings(CONFIG_POST_PARAM_LIST);
        if (paramList != null && paramList.length > 0) {
            for (int i=0; i<paramList.length; i++) {
                if (paramList[i] != null
                        && paramList[i].trim().length() > 0) {
                    getParamListOfPOST().add(paramList[i]);
                }
            }
        }
        if (isLogMessageEnabled()) {
            logMessage("AmWebPolicy.initParamListOfPOST: POST params="
                       + getParamListOfPOST());
        }
    }
 
    private void initParamListOfJSESSION() {
        String[] paramList = 
            getConfigurationStrings(CONFIG_JSESSION_PARAM_LIST);
        if (paramList != null && paramList.length > 0) {
            for (int i=0; i<paramList.length; i++) {
                if (paramList[i] != null
                        && paramList[i].trim().length() > 0) {
                    getParamListOfJSESSION().add(paramList[i]);
                }
            }
        }
        if (isLogMessageEnabled()) {
            logMessage("AmWebPolicy.initParamListOfJSESSION: JSESSION params="
                       + getParamListOfJSESSION());
        }
    }
 
    public AmWebPolicyResult checkPolicyForResource(
            SSOToken ssoToken, String resource, String action,
            String ipAddress, String hostName, HttpServletRequest request) {
        AmWebPolicyResult result = AmWebPolicyResult.DENY;
        
        try {
            HashSet        actionNameSet = new HashSet(1);
            actionNameSet.add(action);
            
            PolicyDecision policyDecision =
                    getPolicyEvaluator().getPolicyDecision(ssoToken,
                    resource, actionNameSet,
                    getPolicyEnvironmentMap(ipAddress, hostName, request));
            
            if (policyDecision == null) {
                if (isLogWarningEnabled()) {
                    logWarning("AmWebPolicy: null policy decision for resource:"
                            + resource + ", action: " + action);
                }
            } else {
                if (isLogMessageEnabled()) {
                    logMessage("AmWebPolicy: XML policy decision for resource:"
                            + resource + ", action=" + action
                            + ", XML=" + policyDecision.toXML());
                }
                
                ActionDecision actionDecision = (ActionDecision)
                policyDecision.getActionDecisions().get(action);
                
                if (actionDecision != null) {
                    result = processActionDecision(actionDecision, resource,
                            action);
                } else {
                    if (isLogWarningEnabled()) {
                        logWarning(
                            "AmWebPolicy: empty action decision for resource:"
                            + resource + ", action=" + action);
                    }
                }
                
                //For AM70 and above
                result.setResponseAttributes(
                        policyDecision.getResponseAttributes());
            }
        } catch (Exception ex) {
            logError("AmWebPolicy: Unable to check policy for resource: "
                    + resource + ", action: " + action
                    + "; Access will be denied", ex);
            result = AmWebPolicyResult.DENY;
        }
        
        return result;
    }
    
    private AmWebPolicyResult processActionDecision(
            ActionDecision decision, String resource, String action)
            throws Exception {
        AmWebPolicyResult result = AmWebPolicyResult.DENY;
        Set set = (Set) decision.getValues();
        boolean resultStatus = false;
        
        if (set != null && set.size() > 0) {
            Iterator it = set.iterator();
            while (it.hasNext()) {
                String value = (String) it.next();
                if (value.equals(ALLOW_VALUE)) {
                    if (isLogMessageEnabled()) {
                        logMessage(
                                "AmWebPolicy : Policy for resource " + resource
                                + " with action " + action + " : " + value);
                    }
                    result = getAllowResult(getAdviceNVP(decision));
                    resultStatus = true;
                } else {
                    if (isLogMessageEnabled()) {
                        logMessage(
                                "AmWebPolicy : Policy for resource " + resource
                                + " with action "   + action + " : " + value);
                    }
                    
                    NameValuePair[] advicevals = getAdviceNVP(decision);
                    
                    if((advicevals != null) && (advicevals.length > 0)) {
                        result = getInsufficientCredentialResult(advicevals);
                    } else {
                        result = getDenyResult();
                    }
                    resultStatus = true;
                }
            }
        } else {
            Map advices = decision.getAdvices();
            
            if (!resultStatus && advices != null && advices.size() > 0) {
                if (isLogMessageEnabled()) {
                    logMessage("AmWebPolicy : No Policy found for resource "
                            + resource + " with action " + action);
                }
                result = 
                        getInsufficientCredentialResult(getAdviceNVP(decision));
                resultStatus = true;
            }
        }
        
        return result;
    }
    
    /*
     * This function does the  70 composite advice
     * @param actionDecision
     * @return NameValuePair[] array of name value pair objects
     * @throws Exception
     */
    private NameValuePair[] getAdviceNVP(ActionDecision actionDecision)
    throws Exception {
        
        NameValuePair[] result = null;
        result = getCompositeAdviceNVP(actionDecision);     
        return result;
    }
    
    private NameValuePair[] getCompositeAdviceNVP(ActionDecision actionDecision)
    throws Exception {
        NameValuePair[] result = null;
        
        String adviceName = Constants.COMPOSITE_ADVICE;
        String compositeAdvice =
                getPolicyEvaluator().getCompositeAdvice(actionDecision);
        
        if (compositeAdvice == null) {
            if (isLogMessageEnabled()) {
                logMessage("AmWebPolicy: No Advices found");
            }
        } else {
            result = new NameValuePair[1];
            result[0] = new NameValuePair(adviceName, compositeAdvice);
            if (isLogMessageEnabled()) {
                logMessage("AmWebPolicy: Found composite advice: " + result[0]);
            }
        }
        
        return result;
    }
     
    private AmWebPolicyResult getDenyResult() {
        return getDenyResult(null);
    }
    
    private AmWebPolicyResult getDenyResult(NameValuePair[] adviceList) {
        return new AmWebPolicyResult(AmWebPolicyResultStatus.STATUS_DENY,
                adviceList);
    }
    
    private AmWebPolicyResult getAllowResult() {
        return getAllowResult(null);
    }
    
    private AmWebPolicyResult getAllowResult(NameValuePair[] adviceList) {
        return new AmWebPolicyResult(AmWebPolicyResultStatus.STATUS_ALLOW,
                adviceList);
    }
    
    private AmWebPolicyResult getInsufficientCredentialResult() {
        return getInsufficientCredentialResult(null);
    }
    
    private AmWebPolicyResult getInsufficientCredentialResult(
            NameValuePair[] adviceList) {
        return new AmWebPolicyResult(
                AmWebPolicyResultStatus.STATUS_INSUFFICIENT_CREDENTIALS,
                adviceList);
    }
    
    private PolicyEvaluator getPolicyEvaluator() {
        return _policyEvaluator;
    }
    
    private void setPolicyEvaluator(PolicyEvaluator policyEvaluator) {
        _policyEvaluator = policyEvaluator;
    }
    
    private Map getPolicyEnvironmentMap(String ipAddress, String hostName,
        HttpServletRequest request) {
        Map result = new HashMap();
        
        Set dnsNameSet = new HashSet();
        dnsNameSet.add(hostName);
        
        Set ipAddressSet = new HashSet();
        ipAddressSet.add(ipAddress);
        
        result.put(IPCondition.REQUEST_IP, ipAddressSet);
        result.put(IPCondition.REQUEST_DNS_NAME, dnsNameSet);

        // set Http GET/POST/JSESSION parameter values into env map
        if (request != null) {
            String method = request.getMethod();
            if (method != null) {
                if (method.equalsIgnoreCase(HTTP_METHOD_GET)) {
                    setHttpRequestParamsInEnvironmentMap(result,
                        request, getParamListOfGET(), HTTP_METHOD_GET);
                } else if (method.equalsIgnoreCase(HTTP_METHOD_POST)) {
                    setHttpRequestParamsInEnvironmentMap(result,
                        request, getParamListOfPOST(), HTTP_METHOD_POST);
                }
            }
            HttpSession session = request.getSession();
            if (session != null) {
                Set params = getParamListOfJSESSION(); 
                if ((params != null) && (params.size() > 0)) {
                    Iterator it = params.iterator();
                    while (it.hasNext()) {
                        String paramName = (String)it.next();
                        String value = (String)session.getAttribute(paramName);
                        if ((value != null) && (value.trim().length() > 0)) {
                            Set valueSet = new HashSet();
                            valueSet.add(value);
                            result.put(JSESSION + "." + paramName, valueSet);
                        }
                    }
                }
            }
        }

        if (isLogMessageEnabled()) {
            logMessage("AmWebPolicy.getPolicyEnvironmentMap: envmap=" + result);
        }
        return result;
    }

    private void setHttpRequestParamsInEnvironmentMap(Map result,
        HttpServletRequest request, Set params, String method) {
        if ((params != null) && (params.size() > 0)) { 
            Iterator it = params.iterator();
            while (it.hasNext()) {
                String paramName = (String)it.next();
                String[] values = request.getParameterValues(paramName);
                if ((values != null) && (values.length > 0)) {
                    Set valueSet = new HashSet();
                    for (int i = 0; i < values.length; i++) {
                        if (values[i] != null
                            && values[i].trim().length() > 0) {
                            valueSet.add(values[i]);
                        }
                    }
                    result.put(method + "." + paramName, valueSet);
                }
            }
        }
    }
    
    private IAmWebPolicyAppSSOProvider getAppSSOProvider() {
        return _appSSOProvider;
    }
    
    private void setAppSSOProvider(IAmWebPolicyAppSSOProvider appSSOProvider) {
        _appSSOProvider = appSSOProvider;
    }

    private Set getParamListOfGET() {
        return paramListOfGET;
    }

    private Set getParamListOfPOST() {
        return paramListOfPOST;
    }

    private Set getParamListOfJSESSION() {
        return paramListOfJSESSION;
    }

    private PolicyEvaluator _policyEvaluator;
    private IAmWebPolicyAppSSOProvider _appSSOProvider;
    private Set paramListOfGET = new HashSet();
    private Set paramListOfPOST = new HashSet();
    private Set paramListOfJSESSION = new HashSet();
}
