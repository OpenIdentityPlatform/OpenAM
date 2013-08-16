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
 * $Id: PolicyEvaluator.java,v 1.7 2009/10/21 23:50:46 dillidorai Exp $
 *
 */

/*
 * Portions Copyrighted 2013 ForgeRock AS
 */
package com.sun.identity.policy.client;

import com.sun.identity.common.ShutdownListener;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.shared.debug.Debug;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager; 
import com.sun.identity.policy.ActionDecision;
import com.sun.identity.policy.PolicyDecision;
import com.sun.identity.policy.ResBundleUtils;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyUtils;
import com.sun.identity.policy.remote.PolicyEvaluationException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.security.AppSSOTokenProvider;
import com.sun.identity.log.Logger;
import com.sun.identity.log.LogRecord;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.security.AccessController;

/**
 * This class provides methods to get policy decisions 
 * for clients of policy service.
 * This class uses XML/HTTP protocol to 
 * communicate with the Policy Service.
 * Policy client API implementaion caches policy decision locally.
 * The cache is updated through policy change notifications and/or
 * polling.
 *
 * @supported.api
 */
public class PolicyEvaluator {

    static Debug debug = Debug.getInstance("amRemotePolicy");
    private PolicyProperties policyProperties;
    private String serviceName;
    private SSOTokenManager ssoTokenManager;

    /**
     * Reference to singleton ResourceResultCache instance
     */
    private ResourceResultCache resourceResultCache;

    AppSSOTokenProvider appSSOTokenProvider;

    /**
     * Logger object for access messages
     */
    static Logger accessLogger;

    /**
     * Logger object for error messages
     */
    static Logger errorLogger;

    private static final String GET_RESPONSE_ATTRIBUTES 
            = "Get_Response_Attributes";

    private SSOToken appSSOToken;

    /*
     * Number of attempts to make to server if policy decision received
     * from server has expired ttl
     */
    private final static int RETRY_COUNT = 3;

    private String logActions;

    /**
     * Creates an instance of client policy evaluator 
     *
     * @param serviceName name of the service for which to create 
     * policy evaluator
     *
     * @throws PolicyException if required properties cannot be retrieved.
     * @throws SSOException if application single sign on token is invalid.
     *
     * @supported.api
     */
    public PolicyEvaluator(String serviceName)
			throws PolicyException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("PolicyEvaluator():Creating PolicyEvaluator:" 
                    + "serviceName=" + serviceName );
        }
	init(serviceName, null); //null appSSOTokenProvider
    }

    /**
     * Creates an instance of client policy evaluator 
     *
     * @param serviceName name of the service for which to create 
     *        policy evalautor.
     * @param appSSOTokenProvider an object where application single sign on
     *        token can be obtained.
     * @throws PolicyException if required properties cannot be retrieved.
     * @throws SSOException if application single sign on token is invalid.
     */
    PolicyEvaluator(String serviceName, 
            AppSSOTokenProvider appSSOTokenProvider)
	    throws PolicyException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("PolicyEvaluator():Creating PolicyEvaluator:" 
                    + "serviceName="+ serviceName 
                    + ":appSSOTokenProvider=" + appSSOTokenProvider);
        }
        if (serviceName == null) {
            if (debug.warningEnabled()) {
                debug.warning("PolicyEvaluator():"
                        + "serviceName is null");
            }
            return;
        } //else do the following

	init(serviceName, appSSOTokenProvider);
    }

    /**
     * Initializes an instance of client policy evaluator object
     *
     * @param serviceName name of the service for which to create 
     *        policy evalautor
     * @param appSSOTokenProvider an object where application single sign on
     *        token can be obtained.
     *
     * @throws PolicyException if required properties cannot be retrieved.
     * @throws SSOException if application single sign on token is invalid.
     *
     */
    private void init(final String serviceName,
                      AppSSOTokenProvider appSSOTokenProvider)
	    throws PolicyException, SSOException {
        this.ssoTokenManager = SSOTokenManager.getInstance();
	this.serviceName = serviceName;
	this.appSSOTokenProvider = appSSOTokenProvider;
	this.policyProperties = new PolicyProperties();
        this.logActions = policyProperties.getLogActions();
        this.resourceResultCache 
                = ResourceResultCache.getInstance(policyProperties);
        appSSOToken = getNewAppSSOToken();

        if (PolicyProperties.previouslyNotificationEnabled()) {
            if (policyProperties.useRESTProtocol()) {
                resourceResultCache.removeRESTRemotePolicyListener(appSSOToken,
                        serviceName, PolicyProperties.getPreviousNotificationURL());
            } else {
                resourceResultCache.removeRemotePolicyListener(appSSOToken,
                        serviceName, PolicyProperties.getPreviousNotificationURL());
            }
        }

        if (policyProperties.notificationEnabled()) {

            // register remote policy listener policy service
            if (debug.messageEnabled()) {
                debug.message( "PolicyEvaluator.init():"
                        + "adding remote policy listener with policy "
                        + "service " + serviceName);

            }
            if (policyProperties.useRESTProtocol()) {
                resourceResultCache.addRESTRemotePolicyListener(appSSOToken,
                        serviceName, policyProperties.getRESTNotificationURL());
            } else {
                resourceResultCache.addRemotePolicyListener(appSSOToken,
                        serviceName, policyProperties.getNotificationURL());
            }
            // Add a hook to remove our listener on shutdown.
            ShutdownManager shutdownMan = ShutdownManager.getInstance();
            if (shutdownMan.acquireValidLock()) {
                try {
                    shutdownMan.addShutdownListener(new ShutdownListener() {
                        @Override
                        public void shutdown() {
                            if (policyProperties.useRESTProtocol()) {
                                resourceResultCache.removeRESTRemotePolicyListener(appSSOToken,
                                        serviceName, policyProperties.getRESTNotificationURL());
                                if (debug.messageEnabled()) {
                                    debug.message("PolicyEvaluator: called removeRESTRemotePolicyListener, service "
                                            + serviceName + ", URL " + policyProperties.getRESTNotificationURL());
                                }
                            } else {
                                resourceResultCache.removeRemotePolicyListener(appSSOToken,
                                        serviceName, policyProperties.getNotificationURL());
                                if (debug.messageEnabled()) {
                                    debug.message("PolicyEvaluator: called removeRemotePolicyListener, service "
                                            + serviceName + ", URL " + policyProperties.getNotificationURL());
                                }
                            }
                        }
                    });
                } finally {
                    shutdownMan.releaseLockAndNotify();
                }
            }
        }

        ActionDecision.setClientClockSkew(policyProperties.getClientClockSkew());

        if (debug.messageEnabled()) {
            debug.message("PolicyEvaluator:"
                    + "initialized PolicyEvaluator");
        }
    }

    /**
     * Evaluates a simple privilege of boolean type. The privilege indicates
     * if the user can perform specified action on the specified resource.
     *
     * @param token single sign on token of the user evaluating policies
     * @param resourceName name of the resource the user is trying to access
     * @param actionName name of the action the user is trying to perform on
     * the resource
     *
     * @return the result of the evaluation as a boolean value
     * @throws PolicyException if result could not be computed for any
     *         reason other than single sign on token problem.
     * @throws SSOException if single sign on token is not valid 
     *
     */
    public boolean isAllowed(SSOToken token, String resourceName,
			     String actionName) throws PolicyException,
			     SSOException {
	return isAllowed(token, resourceName, actionName, null); //null env Map
    }

    /**
     * Evaluates simple privileges of boolean type. The privilege indicates
     * if the user can perform specified action on the specified resource.
     * The evaluation also depends on user's application environment parameters.
     *
     * @param token single sign on token of the user evaluating policies.
     * @param resourceName name of the resource the user is trying to access
     * @param actionName name of the action the user is trying to perform on
     * the resource
     * @param envParameters run time environment parameters
     *
     * @return the result of the evaluation as a boolean value
     *
     * @throws PolicyException if result could not be computed for
     *         reason other than single sign on token problem.
     * @throws SSOException if single sign on token is not valid
     *
     * @supported.api
     */
    public boolean isAllowed(SSOToken token, String resourceName,
			     String actionName,
			     Map envParameters) throws PolicyException,
			     SSOException {
        if (debug.messageEnabled()) {
            debug.message("PolicyEvaluator:isAllowed():"
                    + "token=" + token.getPrincipal().getName() 
                    + ":resourceName="+ resourceName 
                    + ":actionName=" + actionName 
                    + ":envParameters) : entering");
        }

        boolean actionAllowed = false;
        Set actionNames = new HashSet(1);
        actionNames.add(actionName);
        PolicyDecision policyDecision = getPolicyDecision(token, resourceName,
                                   actionNames, envParameters);
        ActionDecision actionDecision = 
		(ActionDecision) policyDecision.getActionDecisions()
                .get(actionName);
	String  trueValue = policyProperties.getTrueValue(serviceName,
                actionName);
	String  falseValue = policyProperties.getFalseValue(serviceName,
                actionName);

        if ( (actionDecision != null) && (trueValue != null) 
                    && (falseValue != null)  ) {
            Set set = (Set) actionDecision.getValues();
            if ( (set != null) ) {
                if ( set.contains(falseValue) ) {
                    actionAllowed = false;
                } else if ( set.contains(trueValue) ) {
                    actionAllowed = true;
                }
            }
        }

        String result = actionAllowed ? "ALLOW" : "DENY";
	String[] objs = {resourceName, actionName, result};
        if (PolicyProperties.ALLOW.equals(logActions) && actionAllowed) {
            logAccessMessage(Level.INFO,
                             ResBundleUtils.getString(
                             "policy_eval_allow", objs),token);
        } else if (PolicyProperties.DENY.equals(logActions) && !actionAllowed) {
            logAccessMessage(Level.INFO,
                             ResBundleUtils.getString(
                             "policy_eval_deny", objs),token);
        } else if (PolicyProperties.BOTH.equals(logActions)
                || PolicyProperties.DECISION.equals(logActions)) {
            logAccessMessage(Level.INFO,
                             ResBundleUtils.getString(
                             "policy_eval_result", objs),token);
        } //else nothing to log

        if (debug.messageEnabled()) {
            debug.message("PolicyEvaluator.isAllowed():"
                    + "token=" + token.getPrincipal().getName() 
                    + ":resourceName=" + resourceName 
                    + ":actionName=" + actionName 
                    + ":returning: " + actionAllowed);
        }
        return actionAllowed;
    }

    /**
     * Evaluates privileges of the user to perform the specified actions
     * on the specified resource. 
     *
     * @param token single sign on token of the user evaluating policies.
     * @param resourceName name of the resource the user is trying to access.
     * @param actionNames Set of action names the user is trying to perform on
     * the resource.
     *
     * @return policy decision
     * @throws PolicyException if result could not be computed for any
     *         reason other than single sign on token problem.
     * @throws SSOException if single sign on token is not valid
     */
    public PolicyDecision getPolicyDecision(SSOToken token,
					    String resourceName,
					    Set actionNames)
        throws PolicyException, SSOException {
	return getPolicyDecision(token, resourceName, actionNames, null);
    }

    /**
     * Evaluates privileges of the user to perform the specified actions
     * on the specified resource. The evaluation also depends on user's
     * run time environment parameters.
     *
     * @param token single sign on token of the user evaluating policies.
     * @param resourceName name of the resource the user is trying to access
     * @param actionNames Set of action names the user is trying to perform on
     *        the resource.
     * @param envParameters run-time environment parameters
     * @return policy decision
     * @throws PolicyException if result could not be computed for any
     *         reason other than single sign on token problem.
     * @throws SSOException if single sign on token is invalid or expired.
     *
     * @supported.api
     */
    public PolicyDecision getPolicyDecision(SSOToken token,
					    String resourceName,
					    Set actionNames,
					    Map envParameters)
                throws PolicyException, SSOException {

        //validate the token 
        ssoTokenManager.validateToken(token);

        if (debug.messageEnabled()) {
            debug.message("PolicyEvaluator:getPolicyDecision():"
                    + "token=" + token.getPrincipal().getName() 
                    + ":resourceName=" + resourceName 
                    + ":actionName=" + actionNames + ":entering");
        }

	PolicyDecision pd = null;
        try {
	    pd = resourceResultCache.getPolicyDecision(
                        appSSOToken, serviceName, token, resourceName, 
                        actionNames, envParameters, RETRY_COUNT);
        } catch (InvalidAppSSOTokenException e) {
            if (debug.warningEnabled()) {
                debug.warning("PolicyEvaluator.getPolicyDecision():"
                        + "InvalidAppSSOTokenException occured:"
                        + "getting new appssotoken");
            }
            appSSOToken = getNewAppSSOToken();
            if (policyProperties.notificationEnabled()) {
                if (debug.warningEnabled()) {
                    debug.warning("PolicyEvaluator.getPolicyDecision():"
                            + "InvalidAppSSOTokenException occured:"
                            + "reRegistering remote policy listener");
                }
                reRegisterRemotePolicyListener(appSSOToken);
            }
	    pd = resourceResultCache.getPolicyDecision(
                    appSSOToken, serviceName, token, resourceName, 
                    actionNames, envParameters, RETRY_COUNT);

        }

        if (debug.messageEnabled()) {
            debug.message("PolicyEvaluator:getPolicyDecision():"
                    + "token=" + token.getPrincipal().getName() 
                    + ":resourceName=" + resourceName 
                    + ":actionNames=" + actionNames 
                    + ":returning policyDecision:" + pd.toXML());
        }

	Object[] objs = {resourceName, actionNames, pd.toXML()};
        if (PolicyProperties.DECISION.equals(logActions)) {
                logAccessMessage(Level.INFO,
                                 ResBundleUtils.getString(
                                 "policy_eval_decision", objs),token);
        } //else nothing to log

	return pd;
    }

    /**
     * Returns the application single sign on token, this token will be
     * passed while initializing the <code>PolicyEvaluator</code> or 
     * if the application session token currently being used by 
     * this <code>PolicyEvaluator</code>  has expired
     *
     * @return a valid application single sign on token.
     */
    private SSOToken getNewAppSSOToken() throws PolicyException {
        SSOToken token = null;
        if (debug.messageEnabled()) {
            debug.message("PolicyEvaluator.getNewAppSSOToken():"
                        + "entering");
        }
	if (appSSOTokenProvider != null) {
	    token = appSSOTokenProvider.getAppSSOToken();
            try {
                ssoTokenManager.refreshSession(token);
                if (!ssoTokenManager.isValidToken(token)) {
                    if (debug.messageEnabled()) {
                        debug.message("PolicyEvaluator.getNewAppSSOToken():"
                                    + "AdminTokenAction returned "
                                    + " expired token, trying again");
                    }
                    token = appSSOTokenProvider.getAppSSOToken();
                }
            } catch (SSOException e) {
                if (debug.warningEnabled()) {
                    debug.warning("PolicyEvaluator.getNewAppSSOToken():"
                            + "could not refresh session:", e);
                }
                token = appSSOTokenProvider.getAppSSOToken();
            }
	} else {
	    token = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            try {
                ssoTokenManager.refreshSession(token);
                if (!ssoTokenManager.isValidToken(token)) {
                    if (debug.messageEnabled()) {
                        debug.message("PolicyEvaluator.getNewAppSSOToken():"
                                    + "AdminTokenAction returned "
                                    + " expired token, trying again");
                    }
                    token = (SSOToken) AccessController.doPrivileged(
                            AdminTokenAction.getInstance());
                }
            } catch (SSOException e) {
                if (debug.warningEnabled()) {
                    debug.warning("PolicyEvaluator.getNewAppSSOToken():"
                            + "could not refresh session:", e);
                }
                token = (SSOToken) AccessController.doPrivileged(
                        AdminTokenAction.getInstance());
            }
	}
	if (token == null) {
	    debug.error("PolicyEvaluator.getNewAppSSOToken():, "
			+ "cannot obtain application SSO token");
            throw new PolicyException(ResBundleUtils.rbName,
                "can_not_create_app_sso_token", null, null);
	}
        if (debug.messageEnabled()) {
            debug.message("PolicyEvaluator.getNewAppSSOToken():"
                        + "returning token");
        }
	return token;
    }

    /**
     * Logs an access message from policy client api 
     * @param level logging level
     * @param message message string
     * @param token single sign on token of user
     */
    private void logAccessMessage(Level level, String message,
					SSOToken token) {

	try { 
	    if (accessLogger == null) {
	        accessLogger = (com.sun.identity.log.Logger)
			        Logger.getLogger("amRemotePolicy.access");
	        if (accessLogger == null) {
		    if (debug.warningEnabled()) {	
			debug.warning("PolicyEvaluator.logAccessMessage:"
			    + "Failed to create Logger");
		    }
	            return;
	        }
	    }
	    LogRecord lr = new LogRecord(level, message, token);
	    accessLogger.log(lr, appSSOToken);
	} catch (Throwable ex) {
	    if (debug.warningEnabled()) {	
		debug.warning("PolicyEvaluator.logAccessMessage:Error"
			+ " writing access logs");
	    }
	}

    }

    /**
     * Returns application single sign on token provider 
     *
     * @return <code>AppSSOTokenProvider</code> Object.
     */
    AppSSOTokenProvider getAppSSOTokenProvider() {
	return appSSOTokenProvider;
    }

    /**
     * Gets names of policy advices that could be handled by OpenSSO
     * if PEP redirects user agent to OpenSSO. If the server reports
     * an error indicating the app sso token provided was invalid,
     * new app sso token is obtained from app 
     * sso token  provider and another attempt is made to get policy advices
     * 
     * @param refetchFromServer indicates whether to get the values fresh 
     *      from OpenSSO or return the values from local cache
     * @return names of policy advices that could be handled by OpenSSO
     *         Enterprise if PEP redirects user agent to OpenSSO.
     * @throws InvalidAppSSOTokenException if the server reported that the
     *         app sso token provided was invalid 
     * @throws PolicyEvaluationException if the server reported any other error
     * @throws PolicyException if there are problems in policy module
     *         while getting the result
     * @throws SSOException if there are problems with sso token
     *         while getting the result
     */
    public Set getAdvicesHandleableByAM(boolean refetchFromServer) 
            throws InvalidAppSSOTokenException, PolicyEvaluationException,
            PolicyException, SSOException {
        Set advicesHandleableByAM = null;
        if (debug.messageEnabled()) {	
            debug.message("PolicyEvaluator.getAdvicesHandleableByAM(): Entering"
                    + "refetchFromServer=" + refetchFromServer);
        }
        try {
            advicesHandleableByAM 
                    = resourceResultCache.getAdvicesHandleableByAM(
                    appSSOToken, refetchFromServer);
        } catch (InvalidAppSSOTokenException e) {
            //retry with new app sso token
            if (debug.warningEnabled()) {
                debug.warning("PolicyEvaluator.getAdvicesHandleableByAM():"
                        + "got InvalidAppSSOTokenException, "
                        + " retrying with new app token");
            }
            advicesHandleableByAM 
                    = resourceResultCache.getAdvicesHandleableByAM(
                    getNewAppSSOToken(), refetchFromServer);
        } catch (PolicyException pe) {
            Throwable nestedException = pe.getNestedException();
            if ((nestedException != null) 
                        && (nestedException instanceof SessionException)) {
                //retry with new app sso token
                if (debug.warningEnabled()) {
                    debug.warning("PolicyEvaluator.getAdvicesHandleableByAM():"
                            + "got SessionException, "
                            + " retrying with new app token");
                }
                advicesHandleableByAM 
                        = resourceResultCache.getAdvicesHandleableByAM(
                        getNewAppSSOToken(), refetchFromServer);

            } else {
                throw pe;
            }
        }
        if (debug.messageEnabled()) {	
            debug.message("PolicyEvaluator.getAdvicesHandleableByAM():"
                    + " Returning advicesHandleableByAM=" 
                    + advicesHandleableByAM);
        }
        return advicesHandleableByAM;
    }

    /**
     *  Returns XML string representation of advice map contained in the
     *  actionDecision. This is a convenience method for use by PEP.
     *
     *  @param actionDecision actionDecision that contains the 
     *        advices
     *  @return XML string representation of advice map contained in the
     *      actionDecision subject to the following rule. If the 
     *      actionDecision is null, the return value would be null. 
     *      Otherwise, if the actionDecision does not contain any advice, 
     *      the return value would be null. Otherwise, actionDecision contains
     *      advices. In this case, if the advices contains at least one advice
     *      name that could be handled by AM, the complete advices element is 
     *      serealized to XML and the XML string is returned. Otherwise, null
     *      is returned.
     *  @throws PolicyException for any abnormal condition encountered in
     *      policy module
     *  @throws SSOException for any abnormal condition encountered in
     *      session module
     */
    public String getCompositeAdvice(ActionDecision actionDecision )
            throws PolicyException, SSOException {

        if(debug.messageEnabled()) {
            debug.message("PolicyEvaluator.getCompositeAdvice():"
                    + " entering, actionDecision = " + actionDecision.toXML());
        }

        String compositeAdvice = null;
        boolean matchFound = false;
        Map advices = null;
        if (actionDecision != null) {
            advices = actionDecision.getAdvices();
        }

        //false : use cached value
        Set handleableAdvices = getAdvicesHandleableByAM(false); 

        if(debug.messageEnabled()) {
            debug.message("PolicyEvaluator.getCompositeAdvice():"
                    + " handleableAdvices = " + handleableAdvices);
        }

        if  ((advices != null) && !advices.isEmpty() 
                && (handleableAdvices !=null) 
                && (!handleableAdvices.isEmpty()) ) {
            Set adviceKeys = advices.keySet();

            if(debug.messageEnabled()) {
                debug.message("PolicyEvaluator.getCompositeAdvice():"
                        + " adviceKeys = " + adviceKeys);
            }

            Iterator keyIter = adviceKeys.iterator();
            while (keyIter.hasNext()) {
                Object adviceKey = keyIter.next();
                if (handleableAdvices.contains(adviceKey)) {
                    matchFound = true;
                    if(debug.messageEnabled()) {
                        debug.message("PolicyEvaluator.getCompositeAdvice():"
                                + " matchFound = " + matchFound);
                        debug.message("PolicyEvaluator.getCompositeAdvice():"
                                + " common key = " + adviceKey);
                    }
                    break;
                }
            }
        }

        if (matchFound) {
            compositeAdvice = PolicyUtils.advicesToXMLString(advices);
        }

        if(debug.messageEnabled()) {
            debug.message("PolicyEvaluator.getCompositeAdvice():"
                    + " returning, compositeAdvcie = " + compositeAdvice);
        }

        return compositeAdvice;
    }

    /**
     * Registers this client again with policy service to get policy 
     * change notifications 
     *
     * @param appToken application sso token to use while registering with
     * policy service to get notifcations
     *
     */
    void reRegisterRemotePolicyListener(SSOToken appToken)
            throws PolicyException {
        if (debug.messageEnabled()) {
            debug.message("PolicyEvaluator.reRegisterRemotePolicyListener():"
                    + "entering");
        } 
        resourceResultCache.addRemotePolicyListener(appSSOToken,
                serviceName, policyProperties.getNotificationURL(),
                true); //reRegister

        //clear policy decision cache
        resourceResultCache.clearCachedDecisionsForService(serviceName);

        if (debug.messageEnabled()) {
            debug.message("PolicyEvaluator.reRegisterRemotePolicyListener():"
                    + "returning");
        } 
    }

}
