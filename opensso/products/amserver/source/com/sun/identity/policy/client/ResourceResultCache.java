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
 * $Id: ResourceResultCache.java,v 1.21 2010/01/21 22:18:01 dillidorai Exp $
 *
 */


package com.sun.identity.policy.client;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.JSONUtils;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.am.util.Cache;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOTokenEvent;
import com.iplanet.sso.SSOTokenListener;
import com.iplanet.services.comm.client.AlreadyRegisteredException;
import com.iplanet.services.comm.client.PLLClient;
import com.iplanet.services.comm.client.SendRequestException;
import com.iplanet.services.comm.share.Request;
import com.iplanet.services.comm.share.RequestSet;
import com.iplanet.services.comm.share.Response;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.services.naming.URLNotFoundException;
import com.sun.identity.common.HttpURLConnectionManager;

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.policy.ActionDecision;
import com.sun.identity.policy.PolicyDecision;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyUtils;
import com.sun.identity.policy.ResourceMatch;
import com.sun.identity.policy.ResourceResult;
import com.sun.identity.policy.interfaces.ResourceName;
import com.sun.identity.policy.remote.AdvicesHandleableByAMRequest;
import com.sun.identity.policy.remote.AdvicesHandleableByAMResponse;
import com.sun.identity.policy.remote.PolicyChangeNotification;
import com.sun.identity.policy.remote.PolicyEvaluationException; 
import com.sun.identity.policy.remote.PolicyListenerRequest;
import com.sun.identity.policy.remote.PolicyNotification;
import com.sun.identity.policy.remote.PolicyRequest;
import com.sun.identity.policy.remote.PolicyResponse;
import com.sun.identity.policy.remote.PolicyService;
import com.sun.identity.policy.remote.RemoveListenerRequest;
import com.sun.identity.policy.remote.ResourceResultRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.net.HttpURLConnection;
import java.net.URL;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.Set;
import com.sun.identity.policy.ResBundleUtils;


import com.sun.identity.shared.Constants;
import com.sun.identity.shared.encode.Hash;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Singleton class that implements client side policy decision cache.
 * Handles communication with policy service acting 
 * as a proxy to policy service.  In effect, this is a caching proxy.
 */
class ResourceResultCache implements SSOTokenListener {

    //service>resource>tokenID>scope>result
    private static ResourceResultCache resourceResultCache;

    private PolicyProperties policyProperties;
    private Set remotePolicyListeners 
            = Collections.synchronizedSet(new HashSet(10));

    //serviceName -> resourceName -> sessionId -> scope -> result
    private Map resultCache = new HashMap(10); 

    private PolicyNotificationHandler notificationHandler;
    private Set tokenRegistry = 
        Collections.synchronizedSet(new HashSet(10000));
    private int        cacheTtl;
    private Set        advicesHandleableByAM; 

    private static Debug debug = PolicyEvaluator.debug;

    private static final String POLICY_SERVICE_ID_FOR_NAMING = "policy";

    private static final String POLICY_SERVICE = "policyservice";
    private static final String REST_POLICY_SERVICE = "ws/1/entitlement/entitlement";
    private static final String REST_POLICY_SERVICE_LISTENER = "ws/1/entitlement/listener";
    private static final String REST_LISTENER_NOTIFICATION_URL = "url";
    
    private static final String IPLANET_AM_WEB_AGENT_SERVICE = "iPlanetAMWebAgentService";

    private static final String REST_QUERY_REALM = "realm";
    private static final String REST_QUERY_APPLICATION = "application";
    private static final String REST_QUERY_SUBJECT = "subject";
    private static final String REST_QUERY_RESOURCE = "resource";
    private static final String REST_QUERY_RESOURCES = "resources";
    private static final String REST_QUERY_ACTION = "actionName";
    private static final String REST_QUERY_ENV = "env";

    private static final String JSON_RESOURCE_NAME = "resourceName";
    private static final String JSON_ACTIONS_VALUES = "actionsValues";
    private static final String JSON_ADVICES = "advices";
    private static final String JSON_ATTRIBUTES = "attributes";

    private static final String GET_RESPONSE_ATTRIBUTES 
            = "Get_Response_Attributes";

    private static long requestID = 0;
    private static String REQUEST_ID_LOCK = "REQUEST_ID_LOCK";
    private static String SECRET_MASK = "*********";

    /**
     * Constructs the singleton instance of <code>ResourceResultCache</code>
     * 
     * @param policyProperties object that provides access to configuration
     *        properties such as policy service URL, notification URL etc.
     *        This is nice wrapper over
     *        <code>com.iplanet.am.util.SystemProperties</code>
     */
    private ResourceResultCache(PolicyProperties policyProperties) 
            throws PolicyException {
        this.policyProperties = policyProperties;
        notificationHandler = new PolicyNotificationHandler(this);
        cacheTtl = policyProperties.getCacheTtl();

        if(policyProperties.notificationEnabled()){
            //register notification handler with PLLClient
            registerHandlerWithPLLClient(notificationHandler);
            if (debug.messageEnabled()) {
                debug.message( "RsourceResultCache():"
                        + "added policyNotificationHandler "
                        + "with PLLClient");
            }
        }

        if (debug.messageEnabled()) {
            debug.message( "RsourceResultCache():"
                    + "Singleton Instance Created");
        }
    }

    /**
     * Returns reference to the singleton instance of 
     * <code>ResourceResultCache</code>
     * 
     * @param policyProperties object that provides access to configuration
     *        properties such as policy service URL, notification URL etc.
     *        This is nice wrapper over
     *        <code>com.iplanet.am.util.SystemProperties</code>
     *
     * @return reference to the singleton instance of 
     *         <code>ResourceResultCache</code>
     */
    synchronized static ResourceResultCache getInstance(
            PolicyProperties policyProperties) throws PolicyException {
        if (resourceResultCache == null) {
            resourceResultCache = new ResourceResultCache(policyProperties);
        }  else {
            resourceResultCache.policyProperties = policyProperties;
            resourceResultCache.cacheTtl = policyProperties.getCacheTtl();
        }
        return resourceResultCache;
    } 

    /**
     * Returns reference to the singleton instance of 
     * <code>ResourceResultCache</code>
     * 
     * @return reference to the singleton instance of 
     *         <code>ResourceResultCache</code>
     */
    private synchronized static ResourceResultCache getInstance() {
        if ( (resourceResultCache == null) 
                && debug.warningEnabled()) {
            debug.warning("ResourceResultCache.getInstance():"
                    + "ResourceResultCache has not been created:"
                    + "returning null");
           
        }
        return resourceResultCache;
    } 
    


    /**
     * Returns policy decision
     * @param appToken application sso token to identify the client to policy
     * service
     * @param serviceName name of service for which to get policy decision
     * @param token session token of user for whom to get policy decision
     * @param resourceName resource name for which to get policy decision
     * @param actionNames action names for which to get policy decision
     * @param env environment map to use to get policy decision
     * @param retryCount try this many times before giving up if received policy
     * decision is found to have expired
     * @return policy decision
     * @throws PolicyException if can not get policy decision
     * @throws SSOException if user session token is not valid
     * @throws InvalidAppSSOTokenException if application session token 
     * is not valid
     */ 
    PolicyDecision getPolicyDecision(SSOToken appToken, String serviceName,
            SSOToken token, String resourceName, Set actionNames,
            Map env, int retryCount) 
            throws InvalidAppSSOTokenException, 
            PolicyException, SSOException {
        int count = 0;
        boolean validTtl = false;
        PolicyDecision pd = getPolicyDecision(appToken, serviceName, 
                token, resourceName, actionNames, 
                env, true); //use cache
        if (pd.getTimeToLive() > System.currentTimeMillis()) {
            validTtl = true;
        }
        while (!validTtl && (count < retryCount)) {
            count++;
            if (debug.messageEnabled()) {
                debug.message("ResourceResultCache.getPolicyDecision():"
                        + "Received expired decision, "
                        + "Getting decision again, repeat attempt=" 
                        + count);
            }
            pd = getPolicyDecision(appToken, serviceName, 
                    token, resourceName, actionNames, 
                    env, false);  //do not use cache
            if (pd.getTimeToLive() > System.currentTimeMillis()) {
                validTtl = true;
                break;
            }
        }
        if (!validTtl) {
            if (debug.warningEnabled()) {
                debug.warning("ResourceResultCache.getPolicyDecision():"
                        + "Received expired decision from server");
            }
            Object[] args = {resourceName};
            throw new PolicyEvaluationException(ResBundleUtils.rbName,
                "received_expired_decision", args, null);
        }

        if (actionNames != null) {
            PolicyDecision pd1 = new PolicyDecision();
            Iterator nameIter = actionNames.iterator();
            while (nameIter.hasNext()) {
                String actionName = (String)nameIter.next();
                Map actionDecisions = pd.getActionDecisions();
                ActionDecision ad =
                        (ActionDecision)actionDecisions.get(actionName);
                if (ad != null) {
                    pd1.addActionDecision(ad);
                }
            }
            Map mergedReponseAttrsMap = new HashMap();
            PolicyUtils.appendMapToMap(pd.getResponseAttributes(), 
                    mergedReponseAttrsMap);
            pd1.setResponseAttributes(mergedReponseAttrsMap);
            pd = pd1;
        } else {
            pd = (PolicyDecision)pd.clone();
        }

        return pd;
    }

    /**
     * Returns policy decision
     * @param appToken application sso token to identify the client to policy
     * service
     *
     * @param serviceName name of service for which to get policy decision
     * @param token session token of user for whom to get policy decision
     * @param resourceName resource name for which to get policy decision
     * @param actionNames action names for which to get policy decision
     * @param env environment map to use to get policy decision
     *
     * @param useCache flag indicating whether to return a locally cached 
     * policy decision.  Locally cached decision is returned only if the 
     * value is <code>true</code>. Otherwise, policy decision is fetched 
     * from policy service and returned.
     *
     * @return policy decision
     * @throws PolicyException if can not get policy decision
     * @throws SSOException if session token is not valid
     */ 
    private PolicyDecision getPolicyDecision(SSOToken appToken, 
            String serviceName, SSOToken token, String resourceName, 
            Set actionNames, Map env, boolean useCache) 
            throws InvalidAppSSOTokenException, 
            PolicyException, SSOException {

        String cacheMode = policyProperties.getCacheMode();
        String rootResourceName = resourceName;
        if (PolicyProperties.SUBTREE.equals(cacheMode)) {
            rootResourceName = getRootResourceName(resourceName, serviceName);
            if (debug.messageEnabled()) {
                debug.message("ResourceResultCache.getPolicyDecision():"
                        + "resourceName=" + resourceName
                        + ":cacheMode=" + cacheMode
                        + ":would get resource results for root resource="
                        + rootResourceName);
            }
        }

        Set resourceResults = getResourceResults(appToken, serviceName, 
                token, rootResourceName, actionNames, env, cacheMode, useCache);
        ResourceName resourceComparator =
                (ResourceName)policyProperties.getResourceComparator(
                serviceName);
        PolicyDecision pd = getPolicyDecisionFromResourceResults(
                resourceResults, resourceName, resourceComparator, serviceName);
        if (debug.messageEnabled()) {
            debug.message("ResourceResultCache.getPolicyDecision():"
                    + ":serviceName=" + serviceName 
                    + ":token=" + token.getPrincipal().getName() 
                    + ":resourceName=" + resourceName 
                    + ":actionNames=" + actionNames + ":env" 
                    + ":cacehMode=" + cacheMode
                    + ":useCache=" + useCache
                    + ":returning policyDecision:" + pd);
        }
        return pd;
    }

    /**
     * Returns a set of <code>ResourceResult</code> objects
     * @param appToken application sso token to identify the client to policy
     * service
     *
     * @param serviceName name of service for which to get resource results
     * @param token session token of user for whom to get  resource results
     * @param resourceName resource name for which to get  resource results
     * @param actionNames action names for which to get  resource results
     * @param env environment map to use to get resource results
     * @param scope the scope to be used while getting resource results
     * @return a set of <code>ResourceResult</code> objects
     *
     * @throws PolicyException if can not get 
     * @throws SSOException if session token is not valid
     * @throws InvalidAppSSOTokenException if application session token 
     * is not valid
     */ 
    private Set getResourceResults(SSOToken appToken, String serviceName,
            SSOToken token, String resourceName, Set actionNames,
            Map env, String scope) 
            throws InvalidAppSSOTokenException, 
            PolicyException, SSOException {
        return getResourceResults(appToken, serviceName, 
                token, resourceName, actionNames, env, scope,
                true); //useCache
    }

    /**
     * Returns a set of <code>ResourceResult</code> objects
     * @param appToken application sso token to identify the client to policy
     * service
     *
     * @param serviceName name of service for which to get resource results
     * @param token session token of user for whom to get resource results
     * @param resourceName resource name for which to get resource results
     * @param actionNames action names for which to get resource results
     * @param env environment map to use to get resource results
     * @param scope the scope to be used while getting resource results
     * @param useCache flag indicating whether to return  locally cached 
     * resource results.  Locally cached resource results are 
     * returned only if the value is <code>true</code>
     *
     * @return a set of <code>ResourceResult</code>
     *
     * @throws PolicyException if can not get resource results
     * @throws SSOException if session token is not valid
     * @throws InvalidAppSSOTokenException if application session token 
     * is not valid
     */ 
    private Set getResourceResults(SSOToken appToken, String serviceName,
            SSOToken token, String resourceName, Set actionNames,
            Map env, String scope, boolean useCache) 
            throws InvalidAppSSOTokenException, 
            PolicyException, SSOException {
        SSOTokenManager.getInstance().validateToken(token);
        String cacheMode = policyProperties.getCacheMode();
        Set resourceResults = null;
        if (debug.messageEnabled()) {
            debug.message("ResourceResultCache.getResourceResults():"
                    + ":serviceName=" + serviceName 
                    + ":token=" + token.getPrincipal().getName() 
                    + ":resourceName=" + resourceName 
                    + ":actionNames=" + actionNames + ":env" 
                    + ":useCache=" + useCache
                    + ":useRESTProtocol()=" + policyProperties.useRESTProtocol()
                    + ":entering ");
        }

        Map resourceTokenIDsMap = null;
        // resultCache -> serviceName -> resourceName -> sessionId -> scope -> result
        synchronized(resultCache) {
            // resourceName -> sessionId -> scope -> result
            resourceTokenIDsMap = (Map)resultCache.get(serviceName);
            if (resourceTokenIDsMap == null) {
                // changed to fix 4295 Policy cache causes frequent 
                // full gc or out of memory issues
                resourceTokenIDsMap 
                        = new Cache(policyProperties.getResultsCacheResourceCap());
                resultCache.put(serviceName, resourceTokenIDsMap);
            }
        }

        Map tokenIDScopesMap = null;
        // resourceTokenIDsMap -> resourceName -> sessionId -> scope -> result
        synchronized(resourceTokenIDsMap) {
            // sessionId -> scope -> result
            tokenIDScopesMap = (Map)resourceTokenIDsMap.get(resourceName);
            if (tokenIDScopesMap == null) {
                // changed to fix 4295 Policy cache causes frequent full 
                // gc or out of memory issues
                tokenIDScopesMap  
                        = new Cache(policyProperties.getResultsCacheSessionCap());
                resourceTokenIDsMap.put(resourceName, tokenIDScopesMap);
            }
        }

        Map scopeResultsMap= null;
        String tokenID =  token.getTokenID().toString();
        // tokenIDScopesMap -> sessionId -> scope -> result
        synchronized(tokenIDScopesMap) {
            scopeResultsMap = (Map)tokenIDScopesMap.get(tokenID);
            if (scopeResultsMap == null) {
                scopeResultsMap = new HashMap();
                tokenIDScopesMap.put(tokenID, scopeResultsMap);
                if (!tokenRegistry.contains(tokenID)) {
                    token.addSSOTokenListener(this);
                    tokenRegistry.add(tokenID);
                }
            }
        }

        Object[] results = null;
        boolean fetchResultsFromServer = false;
        // scopeResultsMap -> scope -> result
        synchronized(scopeResultsMap) {
            results = (Object[])scopeResultsMap.get(scope);
            if ( results == null) {

                //array elements:resourceResults, env, ttl, actionNames
                results = new Object[4];
                scopeResultsMap.put(scope, results);
            }

            if ( !useCache ) {
                if (debug.messageEnabled()) {
                    debug.message("ResourceResultCache.getResourceResults():"
                            + "would contact server since useCache is false");
                }
                fetchResultsFromServer = true;
            } else if (results[0] == null) {
                if (debug.messageEnabled()) {
                    debug.message("ResourceResultCache.getResourceResults():"
                            + "would contact server "
                            + " since results not in cache");
                }
                fetchResultsFromServer = true;
            } else if ((env == null) && (results[1] != null)) {
                if (debug.messageEnabled()) {
                    debug.message("ResourceResultCache.getResourceResults():"
                            + "would contact server since env does not match");
                }
                fetchResultsFromServer = true;
            } else if ((env != null) && !env.equals(results[1])) { 
                if (debug.messageEnabled()) {
                    debug.message("ResourceResultCache.getResourceResults():"
                            + "would contact server since env does not Match");
                }
                fetchResultsFromServer = true;
            } else if (((Long)results[2]).longValue() 
                    < System.currentTimeMillis()) {
                if (debug.messageEnabled()) {
                    debug.message("ResourceResultCache.getResourceResults():"
                            + "would contact server since results ttl has "
                            + " expired");
                }
                fetchResultsFromServer = true;
            } else if ((actionNames == null) && (results[3] != null)) {
                if (debug.messageEnabled()) {
                    debug.message("ResourceResultCache.getResourceResults():"
                            + "would contact server since action names do not "
                            + " match");
                }
                fetchResultsFromServer = true;
            } else if ((actionNames != null) &&  (results[3] == null)) {
                if (debug.messageEnabled()) {
                    debug.message("ResourceResultCache.getResourceResults():"
                            + "would contact server since action names do not "
                            + " Match");
                }
                fetchResultsFromServer = true;
            } else if ((results[3] !=null) &&!((Set)results[3]).containsAll(
                    actionNames))  {
                if (debug.messageEnabled()) {
                    debug.message("ResourceResultCache.getResourceResults():"
                            + "would contact server since cached action names "
                            + " do not cover request action names");
                }
                fetchResultsFromServer = true;
            } else if (resourceResultsHasAdvices((Set)(results[0]))
                    && PolicyProperties.SELF.equals(cacheMode)) { 
                //get from server if there were advices in the cached decision
                //we do this only if cacheMode is self
                fetchResultsFromServer = true;

            }

        }

        // changed to fix 4205 Policy client code has bottleneck when processing notificati 
        // FIXME: remove the check for service name with the some fix on server
        if (fetchResultsFromServer) {
            if(policyProperties.useRESTProtocol() 
                    && IPLANET_AM_WEB_AGENT_SERVICE.equalsIgnoreCase(serviceName)) {
                resourceResults = getRESTResultsFromServer(appToken, 
                        serviceName, token, resourceName, scope, 
                        actionNames, env);
            } else {
                resourceResults = getResultsFromServer(appToken, 
                        serviceName, token, resourceName, scope, 
                        actionNames, env);
            }
            results[0] = resourceResults;

            if (env != null) {
                env = PolicyUtils.cloneMap(env);
            }
            results[1] = env;

            results[2] 
                    = new Long(System.currentTimeMillis() + cacheTtl);

            if (actionNames != null) {
                Set actionNames1 = actionNames;
                actionNames = new HashSet();
                actionNames.addAll(actionNames1);
            }
            results[3] = actionNames;
        } else {
            if (debug.messageEnabled()) {
                debug.message("ResourceResultCache.getResourceResults():"
                        + "would not contact server, "
                        + " would use results from  cache ");
            }
        }


        resourceResults = (Set)(results[0]);
        if (debug.messageEnabled()) {
            debug.message("ResourceResultCache.getResourceResults("
                    + serviceName + ","
                    + token.getPrincipal().getName() + "," 
                    + resourceName + ","
                    + actionNames + ",env)" 
                    + ": returning resourceResults");
        }
        return resourceResults;
    }

    private Set getRESTResultsFromServer(SSOToken appToken, String serviceName,
            SSOToken token, String resourceName, String scope, 
            Set actionNames, Map env) 
            throws InvalidAppSSOTokenException, SSOException,
            PolicyException {
        Set<ResourceResult> resourceResults = null;
        try {        
            AMIdentity userIdentity  = IdUtils.getIdentity(token);
            String restUrl = getRESTPolicyServiceURL(token, scope);
            String queryString = buildEntitlementRequestQueryString(
                "/", serviceName, token, resourceName, actionNames, env);
            restUrl = restUrl + "?" + queryString;
            if (debug.messageEnabled()) {
                debug.message("ResourceResultCache.getRESTResultsFromServer():"
                        + ":serviceName=" + serviceName 
                        + ":token=" + token.getPrincipal().getName() 
                        + ":resourceName=" + resourceName 
                        + ":scope=" + scope 
                        + ":actionNames=" + actionNames + ":env" 
                        + ":restUrl=" + restUrl
                        + ":entering");
            }
            String jsonString = getResourceContent(appToken, token, restUrl);
            if (debug.messageEnabled()) {
                debug.message("ResourceResultCache.getRESTResultsFromServer():"
                        + ":server response jsonString=" + jsonString); 
            }
            resourceResults = jsonResourceContentToResourceResults(
                jsonString, serviceName);
        } catch (InvalidAppSSOTokenException e) {
            throw e;
        } catch (Exception e) {
            String[] args = {e.getMessage()};
            throw new PolicyEvaluationException(
                    ResBundleUtils.rbName,
                    "rest_policy_request_exception",
                    args, e);
        }
        if (debug.messageEnabled()) {
            debug.message("ResourceResultCache.getRESTResultsFromServer():"
                    + "returning");
        }
        return resourceResults;
    }

    /**
     * Returns a set of <code>ResourceResult</code> objects from server.
     * Fresh resource results 
     * are fetched from policy server and returned.
     * @param appToken application sso token to identify the client to policy
     * service
     *
     * @param serviceName name of service for which to get resource results
     * @param token session token of user for whom to get resource results
     * @param resourceName resource name for which to get resource results
     * @param scope the scope to be used while getting resource results
     * @param actionNames action names for which to get resource results
     * @param env environment map to use to get resource results
     *
     * @return a set of <code>ResourceResult</code> objects
     *
     * @throws PolicyException if can not get resource results
     * @throws SSOException if session token is not valid
     * @throws InvalidAppSSOTokenException if application session token 
     * is not valid
     */ 
    private Set getResultsFromServer(SSOToken appToken, String serviceName,
            SSOToken token, String resourceName, String scope, 
            Set actionNames, Map env) 
            throws InvalidAppSSOTokenException, SSOException,
            PolicyException {
        Set resourceResults = null;
        Response response = null;
        try {        
            URL policyServiceUrl = getPolicyServiceURL(token);
            if (debug.messageEnabled()) {
                debug.message("ResourceResultCache.getResultsFromServer():"
                        + ":serviceName=" + serviceName 
                        + ":token=" + token.getPrincipal().getName() 
                        + ":resourceName=" + resourceName 
                        + ":scope=" + scope 
                        + ":actionNames=" + actionNames + ":env" 
                        + ":policyServiceURL=" + policyServiceUrl
                        + ":entering");
            }
            ResourceResultRequest rrRequest = new ResourceResultRequest();
            rrRequest.setServiceName(serviceName);
            rrRequest.setResourceName(resourceName);
            rrRequest.setResourceScope(scope);
            rrRequest.setUserSSOToken(token.getTokenID().toString());

            Set responseAttributes = null;
            if (env != null) {
                rrRequest.setEnvParms(env);
                responseAttributes = getResponseAttributes(env);
                if (debug.messageEnabled()) {
                    debug.message("ResourceResultCache.getResultsFromServer():"
                            + "responseAttributes to get="
                            + responseAttributes);
                }
                if (responseAttributes != null) {
                    rrRequest.setResponseAttributes(responseAttributes);
                }
            }

            PolicyRequest policyRequest = new PolicyRequest();
            policyRequest.setAppSSOToken(appToken.getTokenID().toString());
            policyRequest.setMethodID(
                    PolicyRequest.POLICY_REQUEST_GET_RESOURCE_RESULTS);
            policyRequest.setRequestId(newRequestID());
            policyRequest.setResourceResultRequest(rrRequest);

            PolicyService ps = sendPLLRequest(policyServiceUrl, policyRequest);
            if (ps != null) {
                PolicyResponse pr = ps.getPolicyResponse();
                String exceptionMessage = pr.getExceptionMsg();
                if (exceptionMessage != null) {
                    if(exceptionMessage.indexOf(
                                PolicyResponse.APP_SSO_TOKEN_INVALID) >= 0) {
                        if (debug.warningEnabled()) {
                            debug.warning("ResourceResultCache."
                                + "getResultsFromServer():"
                                + " response exception " + exceptionMessage);
                            debug.warning("ResourceResultCache."
                                + "getResultsFromServer():"
                                + " appSSOToken is invalid");
                            debug.warning("ResourceResultCache."
                                + "throwing InvalidAppSSOTokenException");
                        }

                        String[] args = {exceptionMessage};
                        throw new InvalidAppSSOTokenException(
                                ResBundleUtils.rbName,
                                "server_reported_invalid_app_sso_token", 
                                args, null);
                    } else {
                        debug.warning("ResourceResultCache."
                                + "getResultsFromServer():"
                                + "response exception message=" 
                                + exceptionMessage);
                        String[] args = {exceptionMessage};
                        throw new PolicyEvaluationException(
                                ResBundleUtils.rbName,
                                "server_reported_exception", 
                                args, null);
                    }
                } else {
                    resourceResults = pr.getResourceResults();
                }
            }
        } catch (SendRequestException sre) {
            String[] args = {sre.getMessage()};
            throw new PolicyEvaluationException(
                    ResBundleUtils.rbName,
                    "pll_send_request_exception",
                    args, sre);
        }
        if (debug.messageEnabled()) {
            debug.message("ResourceResultCache.getResultsFromServer():"
                    + "returning");
        }
        return resourceResults;
    }

    /**
     * Returns policy decision computed from a set of
     * <code>ResourceResult</code> objects
     *
     * @param resourceResults resource results used to compute policy decision
     * @param resourceName resource name for which to get policy decision
     * @param resourceComparator <code>ResourceName</code>, resource
     * comparison algorithm used to compare resources
     *
     * @return computed policy decision
     *
     * @throws PolicyException if can not get policy decision
     */ 
    private PolicyDecision getPolicyDecisionFromResourceResults(
            Set resourceResults, String resourceName, 
            ResourceName resourceComparator,
            String serviceName) throws PolicyException {

        PolicyDecision pd = new PolicyDecision();
        resourceName = resourceComparator.canonicalize(resourceName);
        Iterator resultsIter = resourceResults.iterator();
        boolean processed = false;
        while (!processed && resultsIter.hasNext()) {
            ResourceResult resourceResult 
                    = (ResourceResult)resultsIter.next();
            processed = mergePolicyDecisions(pd, resourceResult,
                    resourceName, resourceComparator, serviceName);
        }
        return pd;
    }

    /**
     * Merges policy decisions applicable to a resource 
     * from a <code>ResourceResult</code> object.
     *
     * @param pd a collector for merged policy decision
     * @param resourceResult <code>ResourceResult</code> from which
     * to find applicable policy decisions
     * @param resourceName resource name for which to get policy decision
     * @param resourceComparator <code>ResourceName</code>, resource
     * comparison algorithm used to compare resources
     *
     * @param serviceName service name
     *
     * @return a flag indicating whether more <code>ResourceResult</code>
     * objects need to be visited to to compute the policy decision.
     * <code>true</code> is returned if no more <code>ResourceResult</code>
     * objects need to be visited
     * 
     *
     * a <code>ResourceResult</code> object.
     *
     * @throws PolicyException if can not get policy decision
     */ 
    private boolean mergePolicyDecisions(PolicyDecision pd, 
            ResourceResult resourceResult, String resourceName,
            ResourceName resourceComparator, String serviceName) 
            throws PolicyException {
        boolean processed = false;
        if (debug.messageEnabled()) {
            debug.message("ResourceResultCache.mergePolicyDecisions():"
                    + "resourceName=" + resourceName
                    + ":resourceResultResourceName="
                    + resourceResult.getResourceName());
        }

        ResourceMatch  result = resourceComparator.compare(resourceName,
            resourceResult.getResourceName(), true); //wild card compare
        if (result.equals(ResourceMatch.EXACT_MATCH)) {
            resetPolicyDecision(resourceResult.getPolicyDecision(), pd,
                    serviceName);
            processed = true;
        } else if (result.equals(ResourceMatch.WILDCARD_MATCH)) {
            mergePolicyDecisions(resourceResult.getPolicyDecision(), pd,
                    serviceName);
            if (pd.getTimeToLive() < System.currentTimeMillis()) {
                processed = true;
            }
            if (!processed) {
                Set resourceResults = resourceResult.getResourceResults();
                Iterator resultsIter = resourceResults.iterator();
                while (!processed && resultsIter.hasNext()) {
                    ResourceResult subResult 
                            = (ResourceResult)resultsIter.next();
                    processed = mergePolicyDecisions(pd, subResult,
                            resourceName, resourceComparator, serviceName);
                }
            }
        } else if (result.equals(ResourceMatch.SUPER_RESOURCE_MATCH)) {
                Set resourceResults = resourceResult.getResourceResults();
                Iterator resultsIter = resourceResults.iterator();
                while (!processed && resultsIter.hasNext()) {
                    ResourceResult subResult 
                            = (ResourceResult)resultsIter.next();
                    processed = mergePolicyDecisions(pd, subResult,
                            resourceName, resourceComparator, serviceName);
                }
        } // else NO_MATCH or SUBRESOURCE_MATCH nothing to do
        return processed;
    }


    /**
     * Merges two policy decisions
     * @param pd1 policy decision to be merged
     * @param pd2 policy decision to be merged into
     * @param serviceName service name
     * @return merged policy decision
     */
    private PolicyDecision mergePolicyDecisions(PolicyDecision pd1, 
            PolicyDecision pd2, String serviceName) { //pd2 is collector
      Map actionDecisions1 = pd1.getActionDecisions();
      Set actions = new HashSet();
      actions.addAll(actionDecisions1.keySet());
      Iterator iter = actions.iterator();
      while ( iter.hasNext() ) {
          String action = (String) iter.next();
          ActionDecision ad1 = (ActionDecision) actionDecisions1.get(action);
          pd2.addActionDecision(ad1, 
                policyProperties.getTrueValue(serviceName, action), 
                policyProperties.getFalseValue(serviceName, action));
      }
      Map mergedReponseAttrsMap = new HashMap();
      PolicyUtils.appendMapToMap(pd1.getResponseAttributes(), 
            mergedReponseAttrsMap);
      PolicyUtils.appendMapToMap(pd2.getResponseAttributes(), 
            mergedReponseAttrsMap);
      pd2.setResponseAttributes(mergedReponseAttrsMap);
      return pd2;
    }

    /**
     * Merges two policy decisions
     * @param pd1 policy decision to be merged
     * @param pd2 policy decision to be merged into. Action decisions 
     * present in the policy decision are cleared before merging
     * @param serviceName service name
     * @return merged policy decision
     */
    private PolicyDecision resetPolicyDecision(PolicyDecision pd1, 
            PolicyDecision pd2, String serviceName) { //pd2 is collector
      Map actionDecisions1 = pd1.getActionDecisions();
      Map actionDecisions2 = pd2.getActionDecisions();
      actionDecisions2.clear();
      Set actions = new HashSet();
      actions.addAll(actionDecisions1.keySet());
      Iterator iter = actions.iterator();
      while ( iter.hasNext() ) {
          String action = (String) iter.next();
          ActionDecision ad1 = (ActionDecision) actionDecisions1.get(action);
          pd2.addActionDecision(ad1, 
                policyProperties.getTrueValue(serviceName, action), 
                policyProperties.getFalseValue(serviceName, action));
      }
      Map mergedReponseAttrsMap = new HashMap();
      PolicyUtils.appendMapToMap(pd1.getResponseAttributes(), 
            mergedReponseAttrsMap);
      PolicyUtils.appendMapToMap(pd2.getResponseAttributes(), 
            mergedReponseAttrsMap);
      pd2.setResponseAttributes(mergedReponseAttrsMap);
      return pd2;
    }

    /**
     * Registers a listener with policy service to recieve
     * notifications on policy changes
     * @param appToken session token identifying the client
     * @param serviceName service name
     * @param notificationURL end point on the client that listens for
     * notifications
     */
    void addRemotePolicyListener(SSOToken appToken, 
            String serviceName, String notificationURL) {
        addRemotePolicyListener(appToken, serviceName, notificationURL, 
                false);
    }

    /**
     * Registers a listener with policy service to recieve
     * notifications on policy changes
     * @param appToken session token identifying the client
     * @param serviceName service name
     * @param notificationURL end point on the client that listens for
     * notifications
     *
     * @param reRegister flag indicating whether to register listener
     *  even if it was already registered. <code>true</code> indicates
     * to register listener again even if it was previously registered
     */
    boolean addRemotePolicyListener(SSOToken appToken, 
            String serviceName, String notificationURL, 
            boolean reRegister) {
        boolean  status = false;
        if (debug.messageEnabled()) {
            debug.message("ResourceResultCache.addRemotePolicyListener():"
                    + "serviceName=" + serviceName
                    + ":notificationURL=" + notificationURL);
        } 

        if (remotePolicyListeners.contains(serviceName)
                    && !reRegister) {
            if (debug.messageEnabled()) {
                debug.message("ResourceResultCache.addRemotePolicyListener():"
                        + "serviceName=" + serviceName
                        + ":notificationURL=" + notificationURL
                        + ":is already registered");
            } 
            return status;
        } //else do the following

        URL policyServiceURL = null;
        if (appToken != null) {
            try {
                policyServiceURL 
                        = getPolicyServiceURL(appToken);
            } catch (PolicyException pe) {
                debug.error("ResourceResultCache.addRemotePolicyListener():"
                        + "Can not add policy listner", pe);
            }
        }

        if ((appToken != null) && (policyServiceURL != null)) {
            PolicyListenerRequest listenerReq = new PolicyListenerRequest();
            listenerReq.setServiceName(serviceName);
            listenerReq.setNotificationURL(notificationURL);

            PolicyRequest policyReq = new PolicyRequest();
            policyReq.setAppSSOToken(appToken.getTokenID().toString());
            policyReq.setMethodID(
                    PolicyRequest.POLICY_REQUEST_ADD_POLICY_LISTENER);
            policyReq.setPolicyListenerRequest(listenerReq);

            try {
                PolicyService ps = sendPLLRequest(policyServiceURL, policyReq);
                if (ps != null) {
                    if (debug.messageEnabled()) {
                        debug.message("ResourceResultCache."
                                + "addRemotePolicyListener():"
                                + "result=" + ps.toXMLString());
                    } 
                    PolicyResponse psres = ps.getPolicyResponse();
                    if (psres.getMethodID() 
                            == PolicyResponse.POLICY_ADD_LISTENER_RESPONSE) {
                        status = true;
                        remotePolicyListeners.add(serviceName);
                        if (debug.messageEnabled()) {
                            debug.message("ResourceResultCache."
                                    + "addRemotePolicyListener():"
                                    + "serviceName=" + serviceName
                                    + ":notificationURL=" + notificationURL
                                    + ":policyServiceURL=" + policyServiceURL
                                    + ":add succeeded");
                        } 
                    } 
                } else {
                    debug.error("ResourceResultCache.addRemotePolicyListener():"
                            + " no result");
                } 
            } catch (Exception e) {
                debug.error("ResourceResultCache.addRemotePolicyListener():",e);
            } 
        } 
        return status;
    } 

    /**
     * Removes a listener registered with policy service to recieve
     * notifications on policy changes
     * @param appToken session token identifying the client
     * @param serviceName service name
     * @param notificationURL end point on the client that listens for
     * notifications
     */
     public boolean removeRemotePolicyListener(SSOToken appToken, 
            String serviceName, String notificationURL) {
        boolean status = false;
        URL policyServiceURL = null;
        remotePolicyListeners.remove(notificationURL);
        if (appToken != null) {
            try {
                policyServiceURL = getPolicyServiceURL(appToken);
            } catch (PolicyException pe) {
                debug.error("ResourceResultCache.removeRemotePolicyListener():"
                        + "Can not remove policy listner:", pe);
            }
        }

        if ((appToken != null) && (policyServiceURL != null)) {
            RemoveListenerRequest rmReq = new RemoveListenerRequest();

            rmReq.setServiceName(serviceName);
            rmReq.setNotificationURL(notificationURL);

            PolicyRequest policyReq = new PolicyRequest();
            policyReq.setAppSSOToken(appToken.getTokenID().toString());
            policyReq.setMethodID(
                    PolicyRequest.POLICY_REQUEST_REMOVE_POLICY_LISTENER);
            policyReq.setRemoveListenerRequest(rmReq);

            try {
                PolicyService ps = sendPLLRequest(policyServiceURL, policyReq);
                if (ps != null) {
                    if (debug.messageEnabled()) {
                        debug.message("ResourceResultCache."
                                + "removeRemotePolicyListener():"
                                + "result=" + ps.toXMLString());
                    } 
                    PolicyResponse psres = ps.getPolicyResponse();
                    if (psres.getMethodID() 
                            == PolicyResponse.POLICY_REMOVE_LISTENER_RESPONSE) {
                        status = true;
                    } 
                } else {
                    debug.message("ResourceResultCache."
                            + "removeRemotePolicyListener():"
                            + "no result");
                } 
            } catch (Exception e) {
                debug.error("ResourceResultCache.removeRemotePolicyListener():",
                    e);
            } 
        } 
        return status;
    }

    /**
     * Processes policy notifications forwarded from listener end 
     * point of policy client
     * @param pn policy notification
     */
    static void processPolicyNotification(PolicyNotification pn) 
            throws PolicyEvaluationException {
        if (pn != null) {
            if (debug.messageEnabled()) {
                debug.message("ResourceResultCache:processPolicyNotification():"
                        + pn);
            }
            ResourceResultCache cache = ResourceResultCache.getInstance();
            PolicyChangeNotification pcn = pn.getPolicyChangeNotification();
            String serviceName = pcn.getServiceName();
            if (serviceName != null) {
                if (cache.remotePolicyListeners.contains(serviceName)) {
                    Set affectedResourceNames = pcn.getResourceNames();
                    if (debug.messageEnabled()) {
                        debug.message("ResourceResultCache:"
                                + "processPolicyNotification():" 
                                + "serviceName=" + serviceName
                                + ":affectedResourceNames=" 
                                + affectedResourceNames
                                + ":clearing cache for affected "
                                + "resource names");
                    }
                    clearCacheForResourceNames(serviceName, 
                            affectedResourceNames);
                } else {
                    if (debug.messageEnabled()) {
                        debug.message("ResourceResultCache:"
                                + "processPolicyNotification():" 
                                + "serviceName not registered"
                                + ":no resource names cleared from cache");
                    }
                }
            } else {
                if (debug.messageEnabled()) {
                    debug.message("ResourceResultCache:"
                            + "processPolicyNotification():" 
                            + "serviceName is null"
                            + ":no resource names cleared from cache");
                }
            }
        } else {
            debug.error("ResourceResultCache.processPolicyNotification()" 
            + "PolicyNotification is null");
        } 
    } 

    /** 
     * Registers policy notification handler with <code>PLLClient</code>
     * @param handler policy notification handler
     */
    private void registerHandlerWithPLLClient(
            PolicyNotificationHandler handler) {
        try {
            PLLClient.addNotificationHandler(POLICY_SERVICE_ID_FOR_NAMING, 
                handler);
            if (debug.messageEnabled()) {
                debug.message("ResourceResultCache."
                        + "registerHandlerWithPLLClient():"
                        + "registered notification handler");
            }
        } catch (AlreadyRegisteredException ae) {
            if (debug.warningEnabled()) {
                debug.message("ResourceResultCache."
                        + "registerHandlerWithPLLClient():"
                        + "AlreadyRegisteredException", ae);
            }
        }
    }

   /**
    * Returns policy service URL based on session token
    * @param token session token of user
    * @return policy service URL based on session token
    * @throws PolicyException if can not get policy service URL
    */
    static URL getPolicyServiceURL(SSOToken token) throws
            PolicyException {
        URL policyServiceURL = null;
        try {
            String ssoTokenID = token.getTokenID().toString();
            SessionID sid = new SessionID(ssoTokenID);
            Session session = Session.getSession(sid);
            URL sessionServiceURL = session.getSessionServiceURL();
            String protocol =  sessionServiceURL.getProtocol();
            String host = sessionServiceURL.getHost();
            int port = sessionServiceURL.getPort();
            String uri = sessionServiceURL.getPath();

            String portString = null;
            if ( port == -1) {
                portString = "";
            } else {
                portString = Integer.toString(port);
            }
            policyServiceURL = WebtopNaming.getServiceURL(
                POLICY_SERVICE_ID_FOR_NAMING,
                protocol, host, portString, uri);
        } catch (SessionException se) {
            debug.error("ResourceResultCache.getPolicyServiceURL():"
                    + "Can not find policy service URL", se);
            throw new PolicyEvaluationException(
                    ResBundleUtils.rbName,
                    "policy_service_url_not_found",
                    null, se);
        } catch (URLNotFoundException ue) {
            debug.error("ResourceResultCache.getPolicyServiceURL():"
                    + "Can not find policy service URL", ue);
            throw new PolicyEvaluationException(
                    ResBundleUtils.rbName,
                    "policy_service_url_not_found",
                    null, ue);
        }
        return policyServiceURL;
    }

    /**
     * Processes session token change ntofication
     * @param tokenEvent session token change notification event
     */
    public void ssoTokenChanged(SSOTokenEvent tokenEvent) {
        String tokenID = tokenEvent.getToken().getTokenID().toString();
        if (debug.messageEnabled()) {
            debug.message("ResourceResultCache.ssoTokenChanged():"
                + "for tokenID=" + SECRET_MASK); //mask tokenID
        }

        try {
            synchronized(resultCache) {
                Set services = (Set)resultCache.keySet();
                Iterator serviceIter = services.iterator();
                while (serviceIter.hasNext()) {
                    String serviceName = (String)serviceIter.next();
                    Map resourceTokenIDsMap = (Map)resultCache.get(serviceName);
                    synchronized(resourceTokenIDsMap) {
                        Set resources = (Set)resourceTokenIDsMap.keySet();
                        Iterator resourceIter = resources.iterator();
                        while (resourceIter.hasNext()) {
                            String resource = (String)resourceIter.next();
                            Map tokenIDScopesMap 
                                    = (Map)resourceTokenIDsMap.get(resource);
                            if (tokenIDScopesMap != null) {
                                tokenIDScopesMap.remove(tokenID);
                            }
                            
                            boolean tokenPresent = tokenRegistry.remove(tokenID);
                            if ( (tokenPresent == false) &&
                                (debug.messageEnabled()) ) {
                                debug.message("ResourceResultCache. tokenID= "
                                    + SECRET_MASK
                                    + " not found in Token Registry.");
                            }

                            if (debug.messageEnabled()) {
                                debug.message("ResourceResultCache."
                                    + "ssoTokenChanged():"
                                    + "removing cache results for "
                                    + "tokenID=" + SECRET_MASK //mask tokenID
                                    + ":serviceName=" + serviceName 
                                    + ":resource=" + resource);
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            if (debug.warningEnabled()) {
                debug.warning("ResourceResultCache.ssoTokenChanged():"
                    + "Exception caught", t);
            }
        }
    }

    /**
     * Clears cached decisions for a set of resources
     * @param serviceName service name
     * @param affectedResourceNames affected resource names
     */
    private static void clearCacheForResourceNames(String serviceName, 
            Set affectedResourceNames) {
        if ((affectedResourceNames == null) 
                || affectedResourceNames.isEmpty()) {
            return;
        }  

        Map resourceTokenIDsMap 
                = (Map)(resourceResultCache.resultCache).get(serviceName);
        if ((resourceTokenIDsMap == null)
                || resourceTokenIDsMap.isEmpty()) {
            return;
        } 

        ResourceName resourceComparator 
                = resourceResultCache.policyProperties
                .getResourceComparator(serviceName);
        Iterator arIter = affectedResourceNames.iterator();
        while (arIter.hasNext()) {
            String affectedRN = (String)arIter.next();
            if (debug.messageEnabled()) {
                debug.message("ResourceResultCache."
                        + "clearCacheForResourceNames():"
                        + "affectedResourceName=" + affectedRN);
            }
            synchronized (resourceTokenIDsMap) {
                Set cachedResourceNames = resourceTokenIDsMap.keySet();
                Iterator crIter = cachedResourceNames.iterator();
                while (crIter.hasNext()) {
                    String cachedRN = (String)crIter.next();
                    if (debug.messageEnabled()) {
                        debug.message("ResourceResultCache."
                                + "clearCacheForResourceNames():"
                                + "affectedResourceName=" + affectedRN
                                + ":cachedResourceName=" + cachedRN);
                    }
                    if (affectedRN.equals(cachedRN)) {
                        crIter.remove();
                        if (debug.messageEnabled()) {
                            debug.message("ResourceResultCache."
                                    + "clearCacheForResourceNames():"
                                    + "cleared cached results for "
                                    + "resourceName=" + cachedRN
                                    + ":affectedResourceName=" + affectedRN
                                    + ":match=SAME RESOURCE NAME");
                        }
                    } else {
                        ResourceMatch rm 
                                = resourceComparator.compare( cachedRN, 
                                affectedRN, true); //wildcard compare
                        if (rm.equals(ResourceMatch.EXACT_MATCH)) {
                            crIter.remove();
                            if (debug.messageEnabled()) {
                                debug.message("ResourceResultCache."
                                        + "clearCacheForResourceNames():"
                                        + "cleared cached results for "
                                        + "resourceName=" + cachedRN
                                        + ":affectedResourceName=" + affectedRN
                                        + ":match=EXACT_MATCH");
                            }
                        } else if (rm.equals(ResourceMatch.WILDCARD_MATCH)) {
                            crIter.remove();
                            if (debug.messageEnabled()) {
                                debug.message("ResourceResultCache."
                                        + "clearCacheForResourceNames():"
                                        + "cleared cached results for "
                                        + "resourceName=" + cachedRN
                                        + ":affectedResourceName=" + affectedRN
                                        + ":match=WILD_CARD_MATCH");
                            }
                        } else if (rm.equals(
                                ResourceMatch.SUB_RESOURCE_MATCH)) {
                            crIter.remove();
                            if (debug.messageEnabled()) {
                                debug.message("ResourceResultCache."
                                        + "clearCacheForResourceNames():"
                                        + "cleared cached results for "
                                        + "resourceName=" + cachedRN
                                        + ":affectedResourceName=" + affectedRN
                                        + ":match=SUB_RESOURCE_MACTH");
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns response attribute names specified in environment map
     * @param env environment map
     */
    private Set getResponseAttributes(Map env) {
        Set responseAttributes = null;
        if ( env != null) {
            responseAttributes = (Set) env.get(GET_RESPONSE_ATTRIBUTES);
        }
        return responseAttributes;
    }
    
    /** 
     * Returns a new request ID. Used in identifying request messages
     * sent to policy service
     * @return a new request ID. Used in identifying request messages
     * sent to policy service
     */
    private String newRequestID() {
        String requestIDString = null;
        synchronized(REQUEST_ID_LOCK) {
            requestIDString = String.valueOf(requestID++);
        }
        return requestIDString;
    }

    /**
     * Returns root resource name
     * @param resource resource name from which to compute root resource name
     * @param serviceName service name
     * @return root resource name computed from resource name
     */
    private String getRootResourceName(String resource, String serviceName) {
        ResourceName resourceComparator 
                = policyProperties.getResourceComparator(serviceName);
        String rootResource = "";
        if ((resource != null) && (resource.length() != 0)) {
            String[] resources = resourceComparator.split(resource);
            rootResource = resources[0];
            int index = resource.indexOf(rootResource);
            if ( index > 0 ) {
                rootResource = resource.substring(0, index) + rootResource;
            }
        } 
        return rootResource;
    }

    /** 
     * Returns names of policy advices that could be handled by OpenSSO
     * Enterprise if PEP redirects user agent to OpenSSO.
     *
     * @param appToken application sso token that would be used while
     *        communicating to OpenSSO
     * @param refetchFromServer indicates whether to get the values fresh 
     *      from OpenSSO or return the values from local cache. 
     *      If the server reports app sso token is invalid, a new app sso
     *      token is created and one more call is made to the server.
     * @return names of policy advices that could be handled by OpenSSO
     *         Enterprise
     * @throws InvalidAppSSOTokenException if the server reported that the
     *         app sso token provided wasinvalid
     * @throws PolicyEvaluationException if the server reported any other error
     * @throws PolicyException if there are problems in getting the advice 
     *          names
     * @throws SSOException if the appToken is detected to be invalid
     *         at the client
     */
    Set getAdvicesHandleableByAM(SSOToken appToken, boolean refetchFromServer) 
            throws InvalidAppSSOTokenException, PolicyException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("ResourceResultCache.getAdvicesHandleableByAM():"
                    + ":entering");
        } 

        if ( (advicesHandleableByAM != null) && !refetchFromServer ) {
            if (debug.messageEnabled()) {
                debug.message("ResourceResultCache.getAdvicesHandleableByAM():"
                        + ":returning cached advices"
                        + advicesHandleableByAM);
            } 
            return advicesHandleableByAM;
        }

        URL policyServiceURL = null;
        if (appToken != null) {
            try {
                policyServiceURL 
                        = getPolicyServiceURL(appToken);
            } catch (PolicyException pe) {
                debug.error("ResourceResultCache.getAdvicesHandleableByAM():",
                        pe);
                throw pe;
            }
        }

        if ((appToken != null) && (policyServiceURL != null)) {
            PolicyRequest policyReq = new PolicyRequest();
            policyReq.setAppSSOToken(appToken.getTokenID().toString());
            policyReq.setAdvicesHandleableByAMRequest(new
                    AdvicesHandleableByAMRequest());
            policyReq.setMethodID(
                PolicyRequest.POLICY_REQUEST_ADVICES_HANDLEABLE_BY_AM_REQUEST);

            try {
                PolicyService ps = sendPLLRequest(policyServiceURL, policyReq);
        	if (ps != null) {
                    if (debug.messageEnabled()) {
                        debug.message("ResourceResultCache."
                                + "getAdvicesHandleableByAM():"
                                + "result=" + ps.toXMLString());
                    } 
                    PolicyResponse psres = ps.getPolicyResponse();

                    String exceptionMessage = psres.getExceptionMsg();
                    if (exceptionMessage != null) {
                        if(exceptionMessage.indexOf(ResBundleUtils.getString(
                                "app_sso_token_invalid")) >= 0) {
                            if (debug.warningEnabled()) {
                                debug.warning("ResourceResultCache."
                                    + "getAdvicesHandleableByAM():"
                                    + " response exception " 
                                    + exceptionMessage);
                                debug.warning("ResourceResultCache."
                                    + "AdvicesHandleableByAM():"
                                    + " appSSOToken is invalid");
                                debug.warning("ResourceResultCache."
                                    + "throwing InvalidAppSSOTokenException");
                            }

                            String[] args = {exceptionMessage};
                            throw new InvalidAppSSOTokenException(
                                    ResBundleUtils.rbName,
                                    "server_reported_invalid_app_sso_token", 
                                    args, null);
                        } else {
                            if (debug.warningEnabled()) {
                                debug.warning("ResourceResultCache."
                                        + "AdvicesHandleableByAM():"
                                        + "response exception message=" 
                                        + exceptionMessage);
                            }
                            String[] args = {exceptionMessage};
                            throw new PolicyEvaluationException(
                                    ResBundleUtils.rbName,
                                    "server_reported_exception", 
                                    args, null);
                        }
                    }

                    if (psres.getMethodID() == PolicyResponse.
                        POLICY_ADVICES_HANDLEABLE_BY_AM_RESPONSE) 
                    {
                        AdvicesHandleableByAMResponse  
                            advicesHandleableByAMResponse 
                                = psres.getAdvicesHandleableByAMResponse();
                        if (debug.messageEnabled()) {
                            debug.message("ResourceResultCache."
                                    + "getAdvicesHandleableByAM():"
                                    + advicesHandleableByAMResponse);
                        } 
                        if (advicesHandleableByAMResponse != null) {
                            advicesHandleableByAM = 
                                advicesHandleableByAMResponse.
                                getAdvicesHandleableByAM();
                        }
                    } 
                } else {
                    debug.error("ResourceResultCache.getAdvicesHandleableByAM()"
                            +":no result");
                } 
            } catch (SendRequestException e) {
                debug.error("ResourceResultCache.getAdvicesHandleableByAM():",
                        e);
                throw new PolicyException(e);
            } 
        } 
        if (advicesHandleableByAM == null) {
            advicesHandleableByAM = Collections.EMPTY_SET;
        }
        if (debug.messageEnabled()) {
            debug.message("ResourceResultCache.getAdvicesHandleableByAM():"
                    + ":returning advicesHandleableByAM"
                    + advicesHandleableByAM);
        } 
        return advicesHandleableByAM;
    }

    /**
     * Clears cached policy decisions
     * @param serviceName service name for which cached decisions
     * would be cleared
     */
    void clearCachedDecisionsForService(String serviceName) {
        if (debug.messageEnabled()) {
            debug.message("ResourceResultCache."
                    + "clearCachedDecisionsForService():"
                    + "serviceName=" + serviceName);
        } 
        synchronized(resultCache) {
            resultCache.remove(serviceName);
        }
    }

    /**
     * Return a PolicyService object based on the XML document received
     * from remote Policy Server. This is in response to a request that we
     * send to the Policy server.
     * @param policyServiceUrl The URL of the Policy Service
     * @param preq The SessionRequest XML document
     * @return PolicyService 
     * @exception SendRequestException is thrown if there was an error in
     * sending the XML document or PolicyException if there are any parsing
     * errors.     
     */
    public static PolicyService sendPLLRequest(URL policyServiceUrl,
                           PolicyRequest preq) throws SendRequestException,
                           PolicyException{
        String lbcookie = null;
        try {
            lbcookie = getLBCookie(preq);
        } catch (Exception e) {
            throw new SendRequestException(e);
        }
        
        PolicyService policyService = new PolicyService();
        policyService.setMethodID(PolicyService.POLICY_REQUEST_ID);
        policyService.setPolicyRequest(preq); 
        String xmlString = policyService.toXMLString();

        Request request = new Request(xmlString);
        RequestSet requestSet 
                = new RequestSet(PolicyService.POLICY_SERVICE);
        
        requestSet.addRequest(request);
        if (debug.messageEnabled()) {
            debug.message("ResourceResultCache.sendPLLRequest:"
                    + "sending PLL request to URL=" + policyServiceUrl 
                    + ":\nPLL message=" + xmlString);
        }
        Vector responses = PLLClient.send(policyServiceUrl, lbcookie, requestSet);
        Response response = (Response) responses.elementAt(0);
        PolicyService ps = PolicyService.parseXML(response.getContent());
        if (debug.messageEnabled()) {
            debug.message("ResourceResultCache.sendPLLRequest:"
                                + "result=" + ps.toXMLString());
        }
        return ps;
        
     }
    
    /**
     * Returns lbcookie value for the Session
     * @param  a policy request
     * @return lbcookie name and value pair
     * @throws Exception if session in request is invalid
     */
    public static String getLBCookie(PolicyRequest preq) throws Exception{
       String lbcookie = null;
       ResourceResultRequest rrReq = preq.getResourceResultRequest();
       if(rrReq !=null ) {
           lbcookie = Session.getLBCookie(rrReq.getUserSSOToken());
       } else {
           lbcookie = Session.getLBCookie(preq.getAppSSOToken());
       }
       return lbcookie;
    }
    

    private boolean resourceResultsHasAdvices(Set resourceResults) {
        boolean hasAdvices = false;
        if (resourceResults != null) {
            Iterator rrIter = resourceResults.iterator();
            while (rrIter.hasNext()) {
                ResourceResult rr = (ResourceResult)rrIter.next();
                if (rr.hasAdvices()) {
                    hasAdvices =true;
                    break;
                }
            }
        }
        return hasAdvices;
    }
    
    private String getRESTPolicyServiceURL(SSOToken token, String scope) 
            throws SSOException, PolicyException {
        URL policyServiceURL = getPolicyServiceURL(token);
        String restUrl = policyServiceURL.toString();
        restUrl = restUrl.replace(POLICY_SERVICE, REST_POLICY_SERVICE);
        if (PolicyProperties.SUBTREE.equals(scope)) {
            restUrl = restUrl + "s";
        }
        if (debug.messageEnabled()) {
            debug.message("ResourceResultCache.getRESTPolicyServiceURL():"
                + "restPolicyServiceUrl=" + restUrl);
        }
        return restUrl;
    }

    private static Set<String> mapActionBooleanToString(String serviceName, String actionName, 
            Set actValues) {
        Set values = null;
        if (actValues != null) {
            values = new HashSet<String>();
            values.addAll(actValues);
            if (values.remove("true")) {
                values.add("allow");
            }
            if (values.remove("false")) {
                values.add("deny");
            }
        } 
        return values;
    }

    String getResourceContent(SSOToken appToken, SSOToken userToken, String url)
        throws PolicyException {
        StringBuilder sb = new StringBuilder();
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        try {
            conn = HttpURLConnectionManager.getConnection(new URL(url));
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(false);
            setCookieAndHeader(conn, appToken, userToken);
            conn.connect();

            reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"));
            int len;
            char[] buf = new char[1024];
            while ((len = reader.read(buf, 0, buf.length)) != -1) {
                sb.append(buf, 0, len);
            }
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP) { // got a 302
                        if (debug.warningEnabled()) {
                            debug.warning("ResourceResultCache.getResourceContent():"
                                    + "got 302 redirect");
                            debug.warning("ResourceResultCache.getResourceContent():"
                                    + "throwing InvalidAppSSOTokenException");
                        }

                        String[] args = {conn.getResponseMessage()};
                        throw new InvalidAppSSOTokenException(
                                ResBundleUtils.rbName,
                                "rest_call_to_server_caused_302",
                                args, null);
            
            } else if (responseCode != HttpURLConnection.HTTP_OK) {
                if (debug.warningEnabled()) {
                    debug.warning(
                        "ResourceResultCache.getResourceContent():" +
                        "REST call failed with HTTP response code:" +
                        responseCode);
                }
                throw new PolicyException(
                    "Entitlement REST call failed with error code:" +
                    responseCode);
            }
        } catch (UnsupportedEncodingException uee) {
            // should not happen
            debug.error("ResourceResultCache.getResourceContent():"
                    + "UnsupportedEncodingException:" + uee.getMessage());
        } catch (IOException ie) {
            debug.error("IOException:" + ie);
            throw new PolicyException(ResBundleUtils.rbName,
                    "rest_call_failed_with_io_exception", null, ie);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (conn != null) {
                    conn.disconnect();
                }
                
            } catch (Exception e) {
                // ignore
            }
        }
        return sb.toString();
    }

    private void setCookieAndHeader(
        HttpURLConnection conn,
        SSOToken appToken,
        SSOToken userToken
    ) throws UnsupportedEncodingException {
        String cookieValue = appToken.getTokenID().toString();
        if (Boolean.parseBoolean(
            SystemProperties.get(Constants.AM_COOKIE_ENCODE, "false"))) {
            cookieValue = URLEncoder.encode(cookieValue, "UTF-8");
        }
        String cookieName = SystemProperties.get(Constants.AM_COOKIE_NAME,
            "iPlanetDirectoryPro");
        conn.setRequestProperty("Cookie", cookieName + "=" + cookieValue);

        String userTokenId = userToken.getTokenID().toString();
        String userTokenIdHeader = "ssotoken:" + userTokenId;
        conn.setRequestProperty("X-Query-Parameters", userTokenIdHeader);
    }

    Set<ResourceResult> jsonResourceContentToResourceResults(
            String jsonResourceContent, String serviceName) 
            throws JSONException, PolicyException {
        Set<ResourceResult> resourceResults = null;
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(jsonResourceContent);
        } catch(JSONException e) {
            debug.error("ResourceResultCache.jsonResourceContentToResourceResults():"
                    + "json parsing error of response: " + jsonResourceContent);
            throw new PolicyEvaluationException(
                    ResBundleUtils.rbName,
                    "error_rest_reponse", 
                    null, null);
        }
        int statusCode = jsonObject.optInt("statusCode");
        if (statusCode != 200) {
            debug.error("ResourceResultCache.jsonResourceContentToResourceResults():"
                    + "statusCode=" + statusCode + ", error response");
            throw new PolicyEvaluationException(
                    ResBundleUtils.rbName,
                    "error_rest_reponse", 
                    null, null);
        }

        jsonObject = jsonObject.optJSONObject("body");
        if (jsonObject == null) {
            debug.error("ResourceResultCache.jsonResourceContentToResourceResults():"
                    + "does not have decisions object");
            throw new PolicyEvaluationException(
                    ResBundleUtils.rbName,
                    "error_rest_reponse", 
                    null, null);
        }
        JSONArray jsonArray = jsonObject.optJSONArray("results");
        if (jsonArray != null) {
                ResourceName resourceComparator =
                        (ResourceName)policyProperties.getResourceComparator(
                        serviceName);
                ResourceResult virtualResourceResult = new ResourceResult(
                    ResourceResult.VIRTUAL_ROOT,
                    new PolicyDecision());
                int arrayLen = jsonArray.length();
                for (int i = 0; i < arrayLen; i++) {
                    JSONObject jo = jsonArray.optJSONObject(i);
                    if (jo != null) {
                        ResourceResult rr = jsonEntitlementToResourceResult(jo, 
                                serviceName);
                        virtualResourceResult.addResourceResult(rr, resourceComparator);
                    }
                }
                resourceResults = virtualResourceResult.getResourceResults();
        } else {
            String resourceName = jsonObject.optString("resourceName");
            if (resourceName != null) {
                ResourceResult resourceResult 
                        = jsonEntitlementToResourceResult(jsonObject, serviceName);
                resourceResults = new HashSet<ResourceResult>();
                resourceResults.add(resourceResult);
            } else {
                debug.error("ResourceResultCache.jsonResourceContentToResourceResults():"
                        + "does not have results or resourceName object");
                throw new PolicyEvaluationException(
                        ResBundleUtils.rbName,
                        "error_rest_reponse", 
                        null, null);
            }
        }

        return resourceResults;
    }

    ResourceResult jsonEntitlementToResourceResult(JSONObject jsonEntitlement, 
            String serviceName) throws JSONException {
        String resultResourceName = jsonEntitlement.optString(JSON_RESOURCE_NAME); 
        Map<String, Set<String>> actionsValues = JSONUtils.getMapStringSetString(
                jsonEntitlement, JSON_ACTIONS_VALUES);
        Map<String, Set<String>> advices = JSONUtils.getMapStringSetString(
                jsonEntitlement, JSON_ADVICES);
        Map<String, Set<String>> attributes = JSONUtils.getMapStringSetString(
                jsonEntitlement, JSON_ATTRIBUTES);
        Set<String> actNames = (actionsValues != null) 
                ? actionsValues.keySet() : null;
        PolicyDecision pd = new PolicyDecision();
        if (actNames != null) {
            for (String actName : actNames) {
                Set<String> actValues = actionsValues.get(actName);
                actValues  = mapActionBooleanToString(serviceName, actName, actValues);
                ActionDecision ad = new ActionDecision(actName, actValues);
                ad.setAdvices(advices);
                pd.addActionDecision(ad);
            }
        }
        pd.setResponseDecisions(attributes);
        ResourceResult resourceResult = new ResourceResult(
                resultResourceName,
                pd);
        return resourceResult;
    }


    /**
     * Registers a REST listener with policy service to recieve
     * notifications on policy changes
     * @param appToken session token identifying the client
     * @param serviceName service name
     * @param notificationURL end point on the client that listens for
     * notifications
     */
    void addRESTRemotePolicyListener(SSOToken appToken, 
            String serviceName, String notificationURL) {
        addRESTRemotePolicyListener(appToken, serviceName, notificationURL, 
                false);
    }

    /**
     * Registers a REST listener with policy service to recieve
     * notifications on policy changes
     * @param appToken session token identifying the client
     * @param serviceName service name
     * @param notificationURL end point on the client that listens for
     * notifications
     *
     * @param reRegister flag indicating whether to register listener
     *  even if it was already registered. <code>true</code> indicates
     * to register listener again even if it was previously registered
     */
    boolean addRESTRemotePolicyListener(SSOToken appToken, 
            String serviceName, String notificationURL, 
            boolean reRegister) {
        boolean  status = false;
        if (debug.messageEnabled()) {
            debug.message("ResourceResultCache.addRESTRemotePolicyListener():"
                    + "serviceName=" + serviceName
                    + ":notificationURL=" + notificationURL);
        } 

        if (remotePolicyListeners.contains(serviceName)
                    && !reRegister) {
            if (debug.messageEnabled()) {
                debug.message("ResourceResultCache.addRESTRemotePolicyListener():"
                        + "serviceName=" + serviceName
                        + ":notificationURL=" + notificationURL
                        + ":is already registered");
            } 
            return status;
        } //else do the following

        if (appToken != null) {
            try {
                String policyServiceListenerURL = null;
                policyServiceListenerURL 
                        = getRESTPolicyServiceListenerURL(appToken);
                String rootURL = getRootURL(notificationURL);
                if (debug.messageEnabled()) {
                    debug.message("ResourceResultCache."
                            + "addRESTRemotePolicyListener():"
                            + "serviceName=" + serviceName
                            + ":notificationURL=" + notificationURL
                            + ":rootURL=" + rootURL
                            + ":policyServiceListenerURL=" + policyServiceListenerURL
                            );
                } 
                Set<String> resourceNames = new HashSet<String>();
                resourceNames.add(rootURL);
                String queryString = buildRegisterListenerQueryString(
                    appToken, serviceName, resourceNames);
                queryString += "&url=" +
                    URLEncoder.encode(notificationURL, "UTF-8");
                String resourceContent = postForm(appToken,
                    policyServiceListenerURL, queryString);
                // FIXME: what do we check in the content?
                // FIXME: check the response, detect error conditions?
                if (debug.messageEnabled()) {
                    debug.message("ResourceResultCache."
                            + "addRESTRemotePolicyListener():"
                            + ":resourceContent=" + resourceContent
                            );
                } 
                status = true;
                remotePolicyListeners.add(serviceName);
            } catch (UnsupportedEncodingException e) {
                debug.error("ResourceResultCache.addRESTRemotePolicyListener():"
                        + "Can not add policy listner", e);
            } catch (SSOException se) {
                debug.error("ResourceResultCache.addRESTRemotePolicyListener():"
                        + "Can not add policy listner", se);
            } catch (PolicyException pe) {
                debug.error("ResourceResultCache.addRESTRemotePolicyListener():"
                        + "Can not add policy listner", pe);
            }
        } else {
            // log a debug message: not registering listener
            if (debug.messageEnabled()) {
                debug.message("ResourceResultCache.addRESTRemotePolicyListener():"
                        + "not adding listener, app sso token is null");
            }
        }

        return status;
    } 

    /**
     * Removes a REST listener registered with policy service to recieve
     * notifications on policy changes
     * @param appToken session token identifying the client
     * @param serviceName service name
     * @param notificationURL end point on the client that listens for
     * notifications
     */
     public boolean removeRESTRemotePolicyListener(SSOToken appToken, 
            String serviceName, String notificationURL) {
        boolean status = false;
        URL policyServiceURL = null;
        remotePolicyListeners.remove(notificationURL);
        if (appToken != null) {
            try {
                policyServiceURL = getPolicyServiceURL(appToken);
            } catch (PolicyException pe) {
                debug.error("ResourceResultCache.removeRemotePolicyListener():"
                        + "Can not remove policy listner:", pe);
            }
        }

        if (appToken != null) {
            try {
                String policyServiceListenerURL = null;
                policyServiceListenerURL 
                        = getRESTPolicyServiceListenerURL(appToken);
                if (debug.messageEnabled()) {
                    debug.message("ResourceResultCache."
                            + "removeRESTRemotePolicyListener():"
                            + "serviceName=" + serviceName
                            + ":notificationURL=" + notificationURL
                            + ":policyServiceListenerURL=" + policyServiceListenerURL
                            );
                } 
                StringBuilder sb = new StringBuilder();
                sb.append(policyServiceListenerURL).append("/");
                sb.append(URLEncoder.encode(notificationURL, "UTF-8"));
                Set<String> resourceNames = null;
                sb.append("?");
                sb.append(buildRegisterListenerQueryString(
                    appToken, serviceName, resourceNames));
                String restUrl = sb.toString();
                String resourceContent = deleteRESTResourceContent(
                    appToken, restUrl);
                // FIXME: what do we check in the content
                if (debug.messageEnabled()) {
                    debug.message("ResourceResultCache."
                            + "removeRESTRemotePolicyListener():"
                            + ":resourceContent=" + resourceContent
                            );
                } 
                remotePolicyListeners.remove(notificationURL);
            } catch (UnsupportedEncodingException e) {
                debug.error("ResourceResultCache.addRESTRemotePolicyListener():"
                        + "Can not add policy listner", e);
            } catch (SSOException se) {
                debug.error("ResourceResultCache.addRESTRemotePolicyListener():"
                        + "Can not add policy listner", se);
            } catch (PolicyException pe) {
                debug.error("ResourceResultCache.removeRESTRemotePolicyListener():"
                        + "Can not remove policy listner", pe);
            }
        } else {
            // log a debug message: not removing listener
            // log a debug message: not registering listener
            if (debug.messageEnabled()) {
                debug.message("ResourceResultCache.removeRESTRemotePolicyListener():"
                        + "not removing listener, app sso token is null");
            }
        }

        return status;
    }

    /**
     * Processes REST policy notifications forwarded from listener end 
     * point of policy client
     * @param pn REST policy notification
     */
    static void processRESTPolicyNotification(String pn) //pn has to be JSON string
            throws PolicyEvaluationException {
        // samplePn = "{realm: "/", privilgeName: "p1", resources: ["r1", "r2"]}";
        if (pn != null) {
            if (debug.messageEnabled()) {
                debug.message("ResourceResultCache:processRESTPolicyNotification(), jsonString:"
                        + pn);
            }
            ResourceResultCache cache = ResourceResultCache.getInstance();
            // FIXME after servre side is fixed to provide serviceName in notification
            String serviceName = "iPlanetAMWebAgentService"; 
            Set<String> affectedResourceNames = null;  
            try {
                JSONObject jo = new JSONObject(pn);
                JSONArray jsonArray = jo.optJSONArray("resources");
                if (jsonArray != null) {
                    int arrayLen = jsonArray.length();
                    for (int i = 0; i < arrayLen; i++) {
                        String  resName = jsonArray.optString(i);
                        if (affectedResourceNames == null) {
                            affectedResourceNames = new HashSet<String>();
                        }
                        affectedResourceNames.add(resName);
                    }
                }
            } catch (JSONException je) {
                debug.error("ResourceResultCache.processRESTPolicyNotification():"
                    + "pn=" + pn);
                throw new PolicyEvaluationException("notification_not_valid_json");
            }
            if (serviceName != null && affectedResourceNames != null) {
                if (cache.remotePolicyListeners.contains(serviceName)) {
                    if (debug.messageEnabled()) {
                        debug.message("ResourceResultCache:"
                                + "processRESTPolicyNotification():" 
                                + "serviceName=" + serviceName
                                + ":affectedResourceNames=" 
                                + affectedResourceNames
                                + ":clearing cache for affected "
                                + "resource names");
                    }
                    clearCacheForResourceNames(serviceName, 
                            affectedResourceNames);
                } else {
                    if (debug.messageEnabled()) {
                        debug.message("ResourceResultCache:"
                                + "processRESTPolicyNotification():" 
                                + "serviceName not registered"
                                + ":no resource names cleared from cache");
                    }
                }
            } else {
                if (debug.messageEnabled()) {
                    debug.message("ResourceResultCache:"
                            + "processRESTPolicyNotification():" 
                            + "serviceName or affectedResourceNames is null"
                            + ":no resource names cleared from cache");
                }
            }
        } else {
            debug.error("ResourceResultCache.processRESTPolicyNotification()" 
                    + "PolicyNotification is null");
        } 
    } 

    private String getRESTPolicyServiceListenerURL(SSOToken token) 
            throws SSOException, PolicyException {
        String restUrl = null;
        URL policyServiceURL = getPolicyServiceURL(token);
        restUrl = policyServiceURL.toString();
        restUrl = restUrl.replace(POLICY_SERVICE, REST_POLICY_SERVICE_LISTENER);
        if (debug.messageEnabled()) {
            debug.message("ResourceResultCache.getRESTPolicyServiceURL():"
                + "restPolicyServiceListenerUrl=" + restUrl);
        }
        return restUrl;
    }
    
    String postForm(SSOToken appToken, String url, String formContent)
        throws PolicyException {
        if (debug.messageEnabled()) {
            debug.message("ResourceResultCache."
                    + "postForm():"
                    + "url=" + url
                    + ", formContent=" + formContent);
        }
        StringBuilder sb = new StringBuilder();
        HttpURLConnection conn = null;
        OutputStream out = null;
        BufferedReader reader = null;
        try {
            conn = HttpURLConnectionManager.getConnection(new URL(url));
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            setCookieAndHeader(conn, appToken, appToken);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", 
                    "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", 
                    Integer.toString(formContent.length()));

            conn.connect();

            out =   conn.getOutputStream();
            out.write(formContent.getBytes("UTF-8"));
            out.write("\r\n".getBytes("UTF-8"));
            out.flush();
            out.close();

            reader =  new BufferedReader(
                    new InputStreamReader(
                    conn.getInputStream(), "UTF-8"));
            int len;
            char[] buf = new char[1024];
            while ((len = reader.read(buf, 0, buf.length)) != -1) {
                sb.append(buf, 0, len);
            }
            int responseCode = conn.getResponseCode();
            // any 200 series response code is success
            if (responseCode < 200 || responseCode > 299) {
                    if (debug.warningEnabled()) {
                        debug.warning("ResourceResultCache."
                                + "postForm():"
                                + "REST call failed with HTTP response code:" 
                                + responseCode);
                    }
                    throw new PolicyException(
                            "Entitlement REST call failed with error code:" 
                            + responseCode);
            }
        } catch (UnsupportedEncodingException uee) {
            // should not happen
            debug.error("ResourceResultCache.postFormParams():"
                    + "UnsupportedEncodingException:" + uee.getMessage());
        } catch (IOException ie) {
            debug.error("ResourceResultCache.postForm():IOException:" 
                    + ie.getMessage(), ie);
            throw new PolicyException(ResBundleUtils.rbName,
                    "rest_call_failed_with_io_exception", null, ie);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (conn != null) {
                    conn.disconnect();
                }
                
            } catch (Exception e) {
                // ignore
            }
        }
        return sb.toString();
    }

    private String deleteRESTResourceContent(SSOToken appToken, String url)
        throws PolicyException {
        StringBuilder sb = new StringBuilder();
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        try {
            conn = HttpURLConnectionManager.getConnection(new URL(url));
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            setCookieAndHeader(conn, appToken, appToken);
            conn.setRequestMethod("DELETE");
            conn.connect();

            reader = new BufferedReader(new InputStreamReader(
                conn.getInputStream(), "UTF-8"));
            int len;
            char[] buf = new char[1024];
            while ((len = reader.read(buf, 0, buf.length)) != -1) {
                sb.append(buf, 0, len);
            }
            int responseCode = conn.getResponseCode();
            if (responseCode != conn.HTTP_OK) {
                    if (debug.warningEnabled()) {
                        debug.warning("ResourceResultCache."
                                + "deleteRESTResourceContent():"
                                + "REST call failed with HTTP response code:" 
                                + responseCode);
                    }
                    throw new PolicyException(
                            "Entitlement REST call failed with error code:" 
                             + responseCode);
            }
        } catch (UnsupportedEncodingException uee) {
            // should not happen
            debug.error("ResourceResultCache.deleteRESTResourceContent():"
                    + "UnsupportedEncodingException:" + uee.getMessage());
        } catch (IOException ie) {
            debug.error("IOException:" + ie);
            throw new PolicyException(ResBundleUtils.rbName,
                    "rest_call_failed_with_io_exception", null, ie);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (conn != null) {
                    conn.disconnect();
                }
                
            } catch (Exception e) {
                // ignore
            }
        }
        return sb.toString();
    }

    static String buildRegisterListenerQueryString(
            SSOToken appToken,
            String serviceName, // called application in entitlement
            Set<String> resourceNames) throws PolicyException {
        StringBuilder sb = new StringBuilder();
        try {
            if (appToken == null) {
                if (debug.warningEnabled()) {
                    debug.warning("ResourceResultCache.builRegisterListenerdQueryString():"
                            + "admin is null");
                }
                throw new PolicyException(ResBundleUtils.rbName,
                        "admin_can_not_be_null", null, null); 
            } else {
                String tokenId = appToken.getTokenID().toString();
                String hashedTokenId = Hash.hash(tokenId);
                sb.append(REST_QUERY_SUBJECT).append("=");
                sb.append(URLEncoder.encode(hashedTokenId, "UTF-8"));
            }

            if ((serviceName == null) || (serviceName.length() == 0)) {
                if (debug.warningEnabled()) {
                    debug.warning("ResourceResultCache.builRegisterListenerdQueryString():"
                            + "serviceName can not be null");
                }
                throw new PolicyException(ResBundleUtils.rbName,
                        "service_name_can_not_be_null", null, null);
            } else {
                if (sb.length() > 0) {
                    sb.append("&");
                }
                sb.append(REST_QUERY_APPLICATION).append("=");
                sb.append(URLEncoder.encode(serviceName, "UTF-8"));
            }

            if ((resourceNames == null) || resourceNames.isEmpty()) {
                if (debug.warningEnabled()) {
                    debug.warning("ResourceResultCache.builRegisterListenerdQueryString():"
                            + "resoureNames is null or empty");
                }
            } else {
                for (String resourceName : resourceNames) {
                    if (sb.length() > 0) {
                        sb.append("&");
                    }
                    sb.append(REST_QUERY_RESOURCES).append("=");
                    sb.append(URLEncoder.encode(resourceName, "UTF-8"));
                }
            }
        } catch (UnsupportedEncodingException use) {
            // should not happen
            debug.error("ResourceResultCache.buildRegisterListenerQueryString():" 
                    + use.getMessage());
        }
        return sb.toString();
    }

    static String buildEntitlementRequestQueryString(
            String realm, 
            String serviceName, 
            SSOToken userToken,
            String resource,
            Set actionNames,
            Map envMap) throws PolicyException {
        StringBuilder sb = new StringBuilder();
        try {
            realm = (realm == null || (realm.trim().length() == 0)) ? "/"
                    : realm;
            realm = URLEncoder.encode(realm, "UTF-8");
            sb.append(REST_QUERY_REALM).append("=");
            sb.append(realm);

            if ((serviceName == null) || (serviceName.length() == 0)) {
                if (debug.warningEnabled()) {
                    debug.warning("ResourceResultCache."
                            + "buildEntitlementRequestQueryString():"
                            + "serviceName can not be null");
                }
                throw new PolicyException(ResBundleUtils.rbName,
                        "service_name_can_not_be_null", null, null);
            } else {
                sb.append("&").append(REST_QUERY_APPLICATION).append("=");
                sb.append(URLEncoder.encode(serviceName, "UTF-8"));
            }

            if (userToken == null) {
                if (debug.warningEnabled()) {
                    debug.warning("ResourceResultCache."
                            + "buildEntitlementRequestQueryString():"
                            + "subject can not be null");
                }
                throw new PolicyException(ResBundleUtils.rbName,
                        "subject_can_not_be_null", null, null);
            } else {
                String userTokenId = userToken.getTokenID().toString();
                String hashedUserTokenId = Hash.hash(userTokenId);
                sb.append("&").append(REST_QUERY_SUBJECT).append("=");
                sb.append(URLEncoder.encode(hashedUserTokenId, "UTF-8"));
            }
            if ((resource == null) || (resource.trim().length() == 0)) {
                if (debug.warningEnabled()) {
                    debug.warning("ResourceResultCache."
                            + "buildEntitlementRequestQueryString():"
                            + "resource can not be null");
                }
                throw new PolicyException(ResBundleUtils.rbName,
                        "resource_can_not_be_null", null, null);
            } else {
                sb.append("&").append(REST_QUERY_RESOURCE).append("=");
                sb.append(URLEncoder.encode(resource, "UTF-8"));
            }
            if ((actionNames != null) && !actionNames.isEmpty()) {
                for (Object actObj: actionNames) {
                    sb.append("&").append(REST_QUERY_ACTION).append("=");
                    sb.append(URLEncoder.encode(actObj.toString(), "UTF-8"));
                }
            }
            if ((envMap != null) && !envMap.isEmpty()) {
                String encodedEq = URLEncoder.encode("=", "UTF-8");
                Set keys = envMap.keySet();
                for (Object keyOb : keys) {
                    Set values = (Set)envMap.get(keyOb);
                    String key = URLEncoder.encode(keyOb.toString(), "UTF-8");
                    if ((values != null) && !values.isEmpty()) {
                        for (Object valueOb : values) {
                            sb.append("&").append(REST_QUERY_ENV).append("=");
                            sb.append(key);
                            sb.append(encodedEq);
                            sb.append(URLEncoder.encode(valueOb.toString(), "UTF-8"));
                        }
                    }
                }
            }
        } catch (UnsupportedEncodingException use) {
            // should not happen
            debug.error("ResourceResultCache.buildEntitlementRequestQueryString():" 
                    + use.getMessage());
        }
        return sb.toString();
    }

    String getRootURL(String url) {
        if (url == null) {
            return null;
        }
        int dsi = url.indexOf("//");
        if (dsi == -1) {
            return url;
        }
        int si = url.indexOf("/", dsi + 3);
        if (si == -1) {
            return url;
        }
        return (url.substring(0, si));
    }

    
}

