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
 * $Id: PolicyRequestHandler.java,v 1.8 2008/12/04 00:38:52 dillidorai Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2011 ForgeRock AS
 */

package com.sun.identity.policy.remote;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.stats.Stats;
import com.iplanet.services.comm.server.RequestHandler;
import com.iplanet.services.comm.share.Request;
import com.iplanet.services.comm.share.Response;
import com.iplanet.services.comm.share.ResponseSet;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.policy.PolicyConfig;
import com.sun.identity.policy.PolicyDecision;
import com.sun.identity.policy.PolicyEvaluator;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyUtils;
import com.sun.identity.policy.ResBundleUtils;
import com.sun.identity.policy.ResourceResult;
import com.sun.identity.policy.ResourceResults;
import com.sun.identity.policy.ServiceTypeManager;
import com.sun.identity.policy.interfaces.PolicyListener;
import com.sun.identity.session.util.RestrictedTokenHelper;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.sm.SMSException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.forgerock.openam.session.util.AppTokenHandler;

/**
 * The <code>PolicyRequestHandler</code> class handles the policy
 * related service requests sent by the Policy Enforcers.
 * Currently it supports two types of requests. One is PolicyRequest 
 * which requests for the policy evaluation decisions, the other is
 * AddPolicyListener which adds a policy listener to a service to
 * receive policy notifications which include policy change 
 * notification and subject change notification.
 */
public class PolicyRequestHandler implements RequestHandler {
    static final String REQUEST_AUTH_LEVEL = "requestAuthLevel";
    static final String REQUEST_AUTH_SCHEMES = "requestAuthSchemes";
    static final String REQUEST_IP = "requestIp";
    static final String REQUEST_TIME = "requestTime";
    static final String REQUEST_TIME_ZONE = "requestTimeZone";

    static Debug debug = PolicyService.debug;

    // serviceName: PolicyEvaluator
    static Map policyEvaluators = Collections.synchronizedMap(new HashMap());

    /*
     * Cache to keep the policy change listener registration info
     * notificationUrl: PolicyListenerRequest 
     */
    static Map listenerRegistry = Collections.synchronizedMap(new HashMap());

    // PolicyService revision number
    String policyServiceRevision;

    /**
     * Default Constructor for <code>PolicyRequestHandler</code>.
     */
    public PolicyRequestHandler() {
    }

    /**
     *  Process the requests and return the responses.
     *
     *  @param requests Requests specified in the policy request
     *  @return the set of the response
     */ 
    public ResponseSet process(
        List<Request> requests,
        HttpServletRequest servletRequest, 
        HttpServletResponse servletResponse,
        ServletContext servletContext
    ) {
 
        ResponseSet resSet = new ResponseSet(PolicyService.POLICY_SERVICE);
        int size = requests.size();
        
        for (Request req : requests) {
            Response res = null;
            try {
                res = processRequest(req);
            } catch (PolicyEvaluationException pe) {
                if (debug.messageEnabled()) {
                    debug.message("PolicyRequesthandler.process"
                            + " caught PolicyEvaluationException:",
                            pe);
                }
                PolicyService ps = new PolicyService();
                try {
                    String rev = getPolicyServiceRevision(); 
                    ps.setRevision(rev);
                } catch (PolicyEvaluationException pee) {
                    debug.error("PolicyRequesthandler.process"
                            + " can not get service revision number, "
                            + ",revision defaulting to :" 
                            + PolicyService.ON_ERROR_REVISION_NUMBER, 
                            pee);
                    ps.setRevision(PolicyService.ON_ERROR_REVISION_NUMBER);
                }
                PolicyResponse pRes = new PolicyResponse();
                pRes.setMethodID(PolicyResponse.POLICY_EXCEPTION);
                pRes.setRequestId(pe.getRequestId());
                pRes.setExceptionMsg(pe.getMessage());
                pRes.setIssueInstant(System.currentTimeMillis());
                ps.setMethodID(PolicyService.POLICY_RESPONSE_ID);
                ps.setPolicyResponse(pRes); 
                res = new Response(ps.toXMLString());
            }
            if (res != null) {
                resSet.addResponse(res);
            }
        }

        return resSet;
    }  

    /**
     * Processes a request and return its corresponding response.
     *
     * @param req the request.
     * @return the corresponding response.
     */
    private Response processRequest(Request req)
    throws PolicyEvaluationException {
        String content = req.getContent();
       
        if (debug.messageEnabled()) {
            debug.message("PolicyRequestHandler.processRequest(): content is " +
                content);
        }

        PolicyService psReq = PolicyService.parseXML(content);

        if (debug.messageEnabled()) {
            debug.message("PolicyRequestHandler.processRequest(): " +
                "policy service object:" + psReq.toXMLString());
        }

        PolicyService psRes = processPolicyServiceRequest(psReq);

        if (debug.messageEnabled()) { 
            debug.message("PolicyRequestHandler.processRequest(): " +
                "get response from policy framework: \n" + psRes.toXMLString());
        }
        return new Response(psRes.toXMLString());
    }

    /**
     * Processes a policy service request and return a policy service
     * response.
     *
     * @param psReq a policy service request.
     * @return its corresponding policy service response.
     */
    private PolicyService processPolicyServiceRequest(PolicyService psReq)
        throws PolicyEvaluationException {

        PolicyService psRes = null;

        if (psReq == null) {
            debug.error("PolicyRequestHandler."
                    + "processPolicyServiceRequest(): "
                    + " null psReq");
            throw new PolicyEvaluationException(ResBundleUtils.rbName,
                    "invalid_policy_request_type", null, null);
        }

        if (psReq.getMethodID() == PolicyService.POLICY_REQUEST_ID) {

            // This is a PolicyRequest request
            PolicyRequest policyReq = psReq.getPolicyRequest();

            if (policyReq == null) {
                debug.error("PolicyRequestHandler."
                        + "processPolicyServiceRequest(): "
                        + " null policyRequest");
                throw new PolicyEvaluationException(ResBundleUtils.rbName,
                        "invalid_policy_request_type", null, null);
            }

            psRes = new PolicyService();
            psRes.setRevision(getPolicyServiceRevision());

            PolicyResponse policyRes = processPolicyRequest(policyReq);
            policyRes.setIssueInstant(System.currentTimeMillis());
            psRes.setMethodID(PolicyService.POLICY_RESPONSE_ID);
            psRes.setPolicyResponse(policyRes);
            return psRes;
        }
         
        // The method is not valid for a request
        debug.error("PolicyRequestHandler.processPolicyServiceRequest(): " +
            "invalid policy request type");
        throw new PolicyEvaluationException(ResBundleUtils.rbName,
            "invalid_policy_request_type", null, null);
    }

    /**
     * Processes a policy request and return a policy response.
     *
     * @param req a policy request
     * @return its corresponding policy response
     */
    private PolicyResponse processPolicyRequest(PolicyRequest req) 
        throws PolicyEvaluationException
    {
        if (debug.messageEnabled()) {
            debug.message("PolicyRequestHandler.processPolicyRequest(): " +
                " req received:\n" + req.toXMLString());
        }

        PolicyResponse policyRes = new PolicyResponse();
        
        String requestId = req.getRequestId();
        policyRes.setRequestId(requestId);

        String appSSOTokenIDStr = req.getAppSSOToken();
        SSOToken appToken = null;

        try {
            appToken = getSSOToken(appSSOTokenIDStr, null);
        } catch (PolicyException pe) {
            if (debug.warningEnabled()) {
                debug.warning("PolicyRequestHandler: Invalid app sso token, " +
                    appSSOTokenIDStr);
            }
            throw new PolicyEvaluationException(
                    PolicyResponse.APP_SSO_TOKEN_INVALID, requestId);
        }

        // set the app token into the ThreadLocal
        AppTokenHandler.set(appToken);

        if (req.getMethodID() == 
                PolicyRequest.POLICY_REQUEST_ADD_POLICY_LISTENER) {
            PolicyListenerRequest plReq = req.getPolicyListenerRequest();
            boolean addListener = addPolicyListener(plReq);
            if (addListener) {
                policyRes.setMethodID(
                    PolicyResponse.POLICY_ADD_LISTENER_RESPONSE);
            } else {
                String[] objs = {plReq.getNotificationURL()};
                String  message = ResBundleUtils.getString(
                    "failed.add.policy.listener", objs);
                policyRes.setExceptionMsg(message);
                policyRes.setMethodID(PolicyResponse.POLICY_EXCEPTION);
            }
            return policyRes;
        }

        if (req.getMethodID() ==
                PolicyRequest.POLICY_REQUEST_REMOVE_POLICY_LISTENER) {
            RemoveListenerRequest rmReq = req.getRemoveListenerRequest();
            boolean removeListener = removePolicyListener(rmReq);
            if (removeListener) {
                policyRes.setMethodID(
                    PolicyResponse.POLICY_REMOVE_LISTENER_RESPONSE); 
            } else {
                String[] objs = {rmReq.getNotificationURL()};
                String  message = ResBundleUtils.getString(
                    "failed.remove.policy.listener", objs );
                policyRes.setExceptionMsg(message);
                policyRes.setMethodID(PolicyResponse.POLICY_EXCEPTION);
            }
            return policyRes;
        }

        if (req.getMethodID() ==
            PolicyRequest.POLICY_REQUEST_ADVICES_HANDLEABLE_BY_AM_REQUEST) {
            if (debug.messageEnabled()) {
                debug.message("PolicyRequestHandler: request to get  "
                        + " advicesHandleableByAM");
            }

            try {
                Set advices = PolicyConfig.getAdvicesHandleableByAM();
                policyRes.setAdvicesHandleableByAMResponse(
                        new AdvicesHandleableByAMResponse(advices));
                policyRes.setMethodID(
                    PolicyResponse.POLICY_ADVICES_HANDLEABLE_BY_AM_RESPONSE); 
            } catch (PolicyException pe) {
                if (debug.warningEnabled()) {
                    debug.warning("PolicyRequestHandler: could not get "
                            + " advicesHandleableByAM", pe);
                }
                throw new PolicyEvaluationException(ResBundleUtils.rbName,
                    "could_not_get_advices_handleable_by_am", null, pe, 
                    requestId);
            }
            if (debug.messageEnabled()) {
                debug.message("PolicyRequestHandler: returning  "
                        + " advicesHandleableByAM policy response");
            }
            return policyRes;
        }

        if (req.getMethodID() ==
            PolicyRequest.POLICY_REQUEST_GET_RESOURCE_RESULTS) { 
            ResourceResultRequest resourceResultReq =
                req.getResourceResultRequest();
    
            // Get the user's SSO token id string from the request
            String userSSOTokenIDStr = resourceResultReq.getUserSSOToken();
            SSOToken userToken = null;

            if ((userSSOTokenIDStr != null) &&
                !userSSOTokenIDStr.equals(PolicyUtils.EMPTY_STRING) &&
                !userSSOTokenIDStr.equals(PolicyUtils.NULL_STRING)
            ) {
                try {
                    userToken = getSSOToken(userSSOTokenIDStr, appToken);
                } catch (PolicyException pe) {
                    if (debug.warningEnabled()) {
                        debug.warning(
                            "PolicyRequestHandler: Invalid user sso token, " +
                            userSSOTokenIDStr);
                    }
                    throw new PolicyEvaluationException(ResBundleUtils.rbName,
                        "user_sso_token_invalid", null, null, requestId);
                }
            }

            Set resourceResults = new HashSet();
            ResourceResults resourceRst = null;

            // check if the request contains user response attributes
            Set respAttrs = resourceResultReq.getResponseAttributes(); 
            if (debug.messageEnabled()) {
                debug.message(
                   "PolicyRequestHandler.processPolicyRequest(): "
                   + "respAttrs=\n" 
                   + respAttrs);
            }

            Map respDecisions = null;
            if ((respAttrs != null) && (userToken != null)) {
                // get the response decisions wrt the attributes  
                respDecisions = getResponseDecisions(userToken, respAttrs);
            }
           
            // Get the service name and resource name of the request
            String serviceName = resourceResultReq.getServiceName();
            String resourceName = resourceResultReq.getResourceName();

            // Get the resource scope of the request
            String resourceScope = resourceResultReq.getResourceScope();
            if ((resourceScope != null) 
                && resourceScope.equals(
                    ResourceResultRequest.RESPONSE_ATTRIBUTES_ONLY)) {
                // need not to evaluate policies, do attributes only
                ResourceResult resResult = new ResourceResult(
                    resourceName, new PolicyDecision());
                Set results = new HashSet();
                results.add(resResult);
                resourceRst = new ResourceResults(results);
            } else {    
                // Get the environment parameters of the request
                Map envParameters = resourceResultReq.getEnvParms();
                try {
                    convertEnvParams(envParameters);
                } catch (PolicyException pe) {
                    debug.error(
                        "PolicyRequestHandler: Invalid env parameters", pe);
                    throw new PolicyEvaluationException(ResBundleUtils.rbName,
                        "invalid_env_parameters", null, pe, requestId);
                }
                    
                PolicyEvaluator policyEvaluator = null;
        
                try {
                    // Get an instance of the policy evaluator
                    policyEvaluator = getPolicyEvaluator(serviceName);
        
                    // Get the resource result from the policy evaluator
                    resourceRst = new ResourceResults(
                        policyEvaluator.getResourceResults(
                            userToken, resourceName, resourceScope,
                            envParameters));
                    if (debug.messageEnabled()) {
                        debug.message(
                          "PolicyRequestHandler.processPolicyRequest():"
                          + " resource result:\n" 
                          + resourceRst.toXML());
                    }
                } catch (Exception se) {
                    debug.error("PolicyRequestHandler: Evaluation error", se);
                    throw new PolicyEvaluationException(ResBundleUtils.rbName,
                        "evaluation_error", null, se, requestId);
                }
            }

            resourceRst.setResponseDecisions(respDecisions); 
            resourceResults.addAll(resourceRst.getResourceResults());
            policyRes.setResourceResults(resourceResults);
            policyRes.setMethodID(
                PolicyResponse.POLICY_RESPONSE_RESOURCE_RESULT);     
            return policyRes; 
        }
        debug.error("PolicyRequestHandler: Invalid policy request format"); 
        throw new PolicyEvaluationException(ResBundleUtils.rbName,
            "invalid_policy_request_format", null, null);
    }

    /**
     * Returns the response decisions based on the response attributes.
     *
     * @param token the user's SSO token
     * @param attrs a set of response attributes
     * @return a map which contains the response decisions.
     */
    private Map getResponseDecisions(SSOToken token, Set attrs)
        throws PolicyEvaluationException
    {
        if ((attrs == null) || (attrs.isEmpty())) {
            return null;
        }

        Map userAttrMap = null;
        try {
            AMIdentity id = IdUtils.getIdentity(token);
            userAttrMap = id.getAttributes(attrs);            
        } catch (IdRepoException ie) {
            debug.error("PolicyRequestHandler: " +
                        "failed to get user attributes.", ie);
            throw new PolicyEvaluationException(ie);
        } catch (SSOException se) {
            debug.error("PolicyRequestHandler: bad sso token", se);
            throw new PolicyEvaluationException(se);
        }
        return userAttrMap;
    } 

    /*
     *  Register a policy change listener to the policy framework.
     */
    private boolean addPolicyListener(PolicyListenerRequest policyListenerReq) {
        if (policyListenerReq == null) {
            debug.error("PolicyRequestHandler.addPolicyListener: " +
                "invalid policy listener request received");
            return false;
        }
        
        String serviceTypeName = policyListenerReq.getServiceTypeName();
        String notiURL = policyListenerReq.getNotificationURL();

        if (listenerRegistry.containsKey(notiURL)) {
            if (debug.messageEnabled()) {
                debug.message("PolicyRequestHandler.addPolicyListener: " +
                    "policy listener for service " + serviceTypeName +
                    " has already been registered; the notification URL is " +
                    notiURL); 
            }
            return true;
        }

        PolicyEvaluator policyEvaluator = null;

        try {
            // Get an instance of the policy evaluator
            policyEvaluator = getPolicyEvaluator(serviceTypeName);
     
            if (policyEvaluator != null) {
                // add the policy listener to the policy framework
                policyEvaluator.addPolicyListener(policyListenerReq);
                listenerRegistry.put(notiURL, policyListenerReq);
                if (debug.messageEnabled()) {
                    debug.message("PolicyRequestHandler.addPolicyListener: " +
                        "policy listener for service " + serviceTypeName +
                        " added");
                }
            }
        } catch (PolicyException e) {
            debug.error("PolicyRequestHandler.addPolicyListener: " +
                "failed to add policy change listener", e);
            return false;
        }

        /* Temporarily used for testing notification receiving */
        /* 
        try { 
            com.iplanet.services.comm.client.PLLClient.addNotificationHandler(
                PolicyService.POLICY_SERVICE,
                (new PolicyNotificationHandler()));
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("PolicyRequestHandler: notification handler "
                    +"has already been registered");
            }
        }
         */

        return true;
    }

    /*
     *  Remove a policy change listener from the policy framework.
     */
    private boolean removePolicyListener(
        RemoveListenerRequest removeListenerReq) { 
        if (removeListenerReq == null) {
            debug.error("PolicyRequestHandler.removePolicyListener: " +
                "invalid remove policy listener request received");
            return false;
        }
        
        String serviceTypeName = removeListenerReq.getServiceName();
        String notiURL = removeListenerReq.getNotificationURL();

        if (!listenerRegistry.containsKey(notiURL)) {
            if (debug.messageEnabled()) {
                debug.message("PolicyRequestHandler.removePolicyListener: " +
                    "policy listener to be removed for service " +
                    serviceTypeName +
                    " has not been registered yet; the notification URL is " +
                    notiURL);
            }
            return true;
        }

        PolicyListener policyListener = (PolicyListener)
            listenerRegistry.get(notiURL);
        if (policyListener == null) {
            listenerRegistry.remove(notiURL);
            return true;
        }

        PolicyEvaluator policyEvaluator = null;
        try {
            // Get an instance of the policy evaluator
            policyEvaluator = getPolicyEvaluator(serviceTypeName);
     
            if (policyEvaluator != null) {
                // remove the policy listener from the policy framework
                policyEvaluator.removePolicyListener(policyListener);
                listenerRegistry.remove(notiURL);
                if (debug.messageEnabled()) {
                    debug.message("PolicyRequestHandler.removePolicyListener: "+
                        "policy listener for service " + serviceTypeName +
                        " removed");
                }
            }
        } catch (PolicyException e) {
            debug.error("PolicyRequestHandler.removePolicyListener: " +
                "failed to remove policy change listener", e);
            return false;
        }

        return true;
    }


    /**
     * Convert the environment parameters from sets to their proper
     * data types and put back into the map
     */
    private void convertEnvParams(Map envParams)
        throws PolicyException
    {
        if ((envParams == null) || (envParams.isEmpty())) {
            return;
        }

        // convert REQUEST_IP from a set to a String
        Set reqIPSet = (Set)envParams.get(REQUEST_IP);
        String reqIP = null;

        if (reqIPSet != null) {
            if (!reqIPSet.isEmpty()) {
                Iterator items = reqIPSet.iterator();
                reqIP = (String)items.next();
                envParams.put(REQUEST_IP, reqIP);
            } else {
                envParams.put(REQUEST_IP, null);
            }
        }

        if (debug.messageEnabled()) {
            debug.message("PolicyRequestHandler.convertEnvParams(): " +
                REQUEST_IP + " is " + reqIP);
        }

        Set reqTimeSet = (Set)envParams.get(REQUEST_TIME);
        Long reqTime = null;
        if (reqTimeSet != null) {
            if (!reqTimeSet.isEmpty()) {
                Iterator items = reqTimeSet.iterator();
                String reqTimeStr = (String)items.next();
                reqTime = new Long(reqTimeStr);
                envParams.put(REQUEST_TIME, reqTime);
            } else {
                envParams.put(REQUEST_TIME, null);
            }
        }
        if (debug.messageEnabled()) {
            debug.message("PolicyRequestHandler.convertEnvParams(): " +
                REQUEST_TIME + " is " + reqTime);
        }

       // convert REQUEST_TIME_ZONE from a set to a TimeZone
        try {
            Set reqTimeZoneSet = (Set)envParams.get(REQUEST_TIME_ZONE);
            TimeZone reqTimeZone = null;
            if (reqTimeZoneSet != null) {
                if (!reqTimeZoneSet.isEmpty()) {
                    Iterator items = reqTimeZoneSet.iterator();
                    String reqTimeZoneStr = (String)items.next();
                    reqTimeZone = TimeZone.getTimeZone(reqTimeZoneStr);
                    envParams.put(REQUEST_TIME_ZONE, reqTimeZone);
                } else {
                    envParams.put(REQUEST_TIME_ZONE, null);
                }
            }
            if (debug.messageEnabled()) {
                debug.message("PolicyRequestHandler.convertEnvParams(): " +
                    REQUEST_TIME_ZONE + " is " + reqTimeZone);
            }
        } catch (Exception e) {
            throw new PolicyException(ResBundleUtils.rbName, 
                    "invalid_request_time_zone_in_request", null, e);
        }
    }

    /**
     * Returns the instance of the policy evaluator for the service
     * specified by serviceName from the cache.
     */
    private PolicyEvaluator getPolicyEvaluator(String serviceName)
        throws PolicyException
    {
        PolicyEvaluator pe = (PolicyEvaluator)policyEvaluators.get(serviceName);

        if (pe == null) {
            // The instance is not yet in the cache, create one.
            try {
                pe = new PolicyEvaluator(serviceName);
            } catch (SSOException e) {
                debug.error("PolicyRequestHandler.getPolicyEvaluator(String): "+
                    "failed to get a policy evaluator for service " +
                    serviceName, e);
                throw new PolicyException(ResBundleUtils.rbName,
                    "unable_to_get_an_evaluator", null, e);
            }
            // save the instance in the cache.
            policyEvaluators.put(serviceName, pe);
        }
        return pe; 
    }

    /**
     * Returns sso token based on the sso token id string.
     */
    private SSOToken getSSOToken(String idString, SSOToken context)
        throws PolicyException
    {
        SSOToken token = null;

        // Get the user's SSO token based on the token id string
        try {
            token = RestrictedTokenHelper.resolveRestrictedToken(
                idString, context);
        } catch (Exception e) {
            throw new PolicyException(ResBundleUtils.rbName,
                "invalid_sso_token", null, null);
        }
        return token;
    }

    /**
     * Returns policy service revision number 
     */
    synchronized String getPolicyServiceRevision() 
            throws PolicyEvaluationException {
        if (policyServiceRevision == null) {
            try {
                policyServiceRevision = String.valueOf(
                    ServiceTypeManager.getPolicyServiceRevisionNumber());
            } catch (SMSException e) {
                debug.error("PolicyRequestHandler.getPolicyServiceRevision():"
                    + "Unable to get policy service revision", e);
                throw new PolicyEvaluationException(ResBundleUtils.rbName,
                    "unable_to_get_policy_serivce_revision", null, null);
            } catch (PolicyException e) {
                debug.error("PolicyRequestHandler.getPolicyServiceRevision():"
                    + "Unable to get policy service revision", e);
                throw new PolicyEvaluationException(ResBundleUtils.rbName,
                    "unable_to_get_policy_serivce_revision", null, null);
            } catch (SSOException e) {
                debug.error("PolicyRequestHandler.getPolicyServiceRevision():"  
                    + "Unable to get policy service revision", e);
                throw new PolicyEvaluationException(ResBundleUtils.rbName,
                    "unable_to_get_policy_serivce_revision", null, null);
            }
        }
        return policyServiceRevision;
    }


    public static void printStats(Stats policyStats) {
        /* record stats for policyEvaluators,  listenerRegistry */
        policyStats.record("PolicyRequestHandler:Number of PolicyEvaluators "
                + " in  cache : " 
                + policyEvaluators.size());
        policyStats.record("PolicyRequestHandler:Number of policy change "
                + " listeners in " + " cache : " + listenerRegistry.size());
    }
}
