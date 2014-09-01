/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: MSISDN.java,v 1.3 2008/06/25 05:41:58 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2013 ForgeRock, Inc.
 */
package com.sun.identity.authentication.modules.msisdn;


import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import org.forgerock.openam.utils.ClientUtils;

import java.security.Principal;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;

/**
 * MSISDN Authentication module retrieves the device's <code>msisdn</code>
 * number and the Wireless gateway sending the request from the
 * <code>ServletRequest</code>. If the values cannot be retrieved then
 * callbacks to get <code>msisdn</code> number and wap gateway are sent back to
 * the client. The WAP Gateway is validated against a list of valid gateways in
 * the gateway list attribute. If the gateway list attribute is empty then all
 * gateways will trusted , if it is "none" then no gateways will be trusted and
 * if specific values are specified only those gateways will be trusted. Once
 * the gateway is validated the <code>msisdn</code> number is searched for in
 * the repository. This module is written to lookup the <code>msisdn</code>
 * number in LDAP and return the user ID which has the <code>msisdn</code>
 * number. 
 */
public class MSISDN extends AMLoginModule {
    private  ResourceBundle bundle = null;
    private java.util.Locale locale;
    private static com.sun.identity.shared.debug.Debug debug = null;
    private static final int DEFAULT_MSISDN_AUTH_LEVEL = 0;

    private String userTokenId;
    private String errorMsgKey = null;
    private MSISDNPrincipal userPrincipal;
    private Map options;
    private Set gatewayList;
    private Set parameterNameList;
    private Set searchHeaderList;
    private String userSearchAttr;
    private String serverHost;  
    private int serverPort; 
    private String startSearchLoc;  
    private String principleUser;  
    private String principlePasswd;  
    private String useSSL;        
    private boolean validGateway=false;
    private boolean searchAllHeaders=false;

    private static final String amAuthMSISDN = "amAuthMSISDN";
    private static final String TRUSTED_GATEWAY_LIST =
        ISAuthConstants.AUTH_ATTR_PREFIX_NEW + "MSISDNTrustedGatewayList";
    private static final String MSISDN_PARAMETER_NAME =
        ISAuthConstants.AUTH_ATTR_PREFIX_NEW + "MSISDNParameterNameList";
    private static final String MSISDN_AUTH_LEVEL =
        ISAuthConstants.AUTH_ATTR_PREFIX_NEW + "MSISDNAuthLevel";
    private static final String MSISDN_HEADER_SEARCH = 
        ISAuthConstants.AUTH_ATTR_PREFIX_NEW + "MSISDNHeaderSearch";

    private static final String SEARCH_COOKIE = "searchCookie";
    private static final String SEARCH_HEADER = "searchRequest";
    private static final String SEARCH_PARAM = "searchParam";
    private static final int SUBMITTED_CREDENTIALS = 0;

    static {
        debug = com.sun.identity.shared.debug.Debug.getInstance(amAuthMSISDN);
    }

    public MSISDN() {
    }


    /**
     * Retrieves the service schema attributes for the module
     * and initializes the configuration, including locale and getting
     * the resource bundle.
     *
     * @param subject Subject to be authenticated.
     * @param sharedState State shared with other configured
     *        <code>LoginModules</code>.
     * @param options Options specified in the login Configuration for this
     *        particular <code>LoginModule</code>.
     */
    public void init(Subject subject, Map sharedState, Map options) {
        locale = getLoginLocale();
        bundle = amCache.getResBundle(amAuthMSISDN, locale);
        if (debug.messageEnabled()) {
            debug.message("MSISDN resbundle locale="+locale);
        }
        
        this.options = options;
        initAuthConfig();
    }

    /**
     * Returns MSISDN auth config parameters.
     */
    private void initAuthConfig() {
        if (options != null) {
            debug.message("MSISDN: getting attributes.");
            gatewayList = (Set)options.get(TRUSTED_GATEWAY_LIST);
            if ((gatewayList != null) && (!gatewayList.isEmpty()) 
                            && (gatewayList.contains("none"))) {
                if (debug.messageEnabled()) {
                    debug.message("No gateways trusted ");
                }
                errorMsgKey = "MSISDNInvalidGateway";
            } else {
                parameterNameList = (Set)options.get(MSISDN_PARAMETER_NAME);
                searchHeaderList = (Set)options.get(MSISDN_HEADER_SEARCH);
                if ((searchHeaderList == null) || (searchHeaderList.isEmpty())){
                    searchAllHeaders = true;
                    if (debug.messageEnabled()) {
                        debug.message("searchAllHeaders :" + searchAllHeaders);
                    }
                }
                setMSISDNAuthLevel();
            }
        } else {
            debug.error("options is null");
            errorMsgKey = "MSISDNValidateEx";
        }
        return;
    }

    /**
     * Validates the authentication credentials.
     *
     * @param callbacks
     * @param state
     * @return ISAuthConstants.LOGIN_SUCCEED on login success
     * @exception AuthLoginException
     */
    public int process(Callback[] callbacks, int state) 
            throws AuthLoginException {
        debug.message("MSISDN : in process ..");
        if (errorMsgKey != null) {
            debug.message("Error initalizing config");
            throw new AuthLoginException(amAuthMSISDN, errorMsgKey,null);
        }

        HttpServletRequest req = getHttpServletRequest();
        String gateway = null;
        String msisdnNumber = null;
        
        if (req != null) {
            gateway = ClientUtils.getClientIPAddress(req);
            msisdnNumber = getMSISDNNumberFromRequest(req);
        } else {
            debug.message("Null request calling sendCallback"); 
            Map map = sendCallback();
            if (map != null) {
                 msisdnNumber = (String)map.get("msisdnNumber");
                 gateway = (String)map.get("gateway");
            }        
        }

        if (isValidGateway(gateway) && (msisdnNumber != null)) { 
            MSISDNValidation msisdnValidation = 
                            new MSISDNValidation(options,debug,bundle,locale);
            userTokenId = msisdnValidation.getUserId(msisdnNumber);
            storeUsernamePasswd(userTokenId, null);
        } else {
            debug.error("Gateway is invalid OR msisdn number is null"); 
            throw new AuthLoginException(amAuthMSISDN, "MSISDNValidateEx",null);
        }
        return  ISAuthConstants.LOGIN_SUCCEED; 
    }

    /**     
     * Returns the User Principal.
     *
     * @return MSISDN Principal.
     */
    public Principal getPrincipal() {
        if ((userPrincipal == null) && (userTokenId != null)) {
            userPrincipal = new MSISDNPrincipal(userTokenId);
        }

        return userPrincipal;
    }
    
    /**
     * Send callbacks to get gateway and msisdnNumber
     *
     * @return Map containing gateway and msisdnNumber
     */
    private Map sendCallback() {
        Map map = null;
        try {
            CallbackHandler callbackHandler = getCallbackHandler();
            if (callbackHandler == null) {
                throw new AuthLoginException(amAuthMSISDN,
                        "NoCallbackHandler",null);
            }
            Callback[] callbacks = new Callback[2];
            callbacks[0] = new NameCallback(bundle.getString("gateway"));
            callbacks[1] = new PasswordCallback(
                bundle.getString("msisdn"), true);
            callbackHandler.handle(callbacks);

            // map to hold return
            map = new HashMap();

            // process return
            int len = callbacks.length;
            for (int i = 0; i < len; i ++) {
                Callback cb = callbacks[i];
                if (cb instanceof PasswordCallback) {
                    char[] pass = ((PasswordCallback) cb).getPassword();
                    if (pass != null) {
                        map.put("msisdnNumber", new String(pass));
                    }
                } else if (cb instanceof NameCallback) {
                    String gateway = ((NameCallback) cb).getName();
                    if (gateway != null) {
                        map.put("gateway", gateway);
                    }
                }
            }
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("sendCallback", e);
            }
        }
        return map;
    }

    /** Sets the auth level */
    private void setMSISDNAuthLevel()  {
        String tmp = CollectionHelper.getMapAttr(options,MSISDN_AUTH_LEVEL);
        int authLevel = DEFAULT_MSISDN_AUTH_LEVEL;
        if (tmp != null && tmp.length() > 0) {
            try {
                authLevel = Integer.parseInt(tmp);
            } catch (Exception e) {
                debug.error("Invalid auth level " + tmp);
            }
        }
        if (debug.messageEnabled()) {
            debug.message("Set auth level to " + authLevel);
        }
        setAuthLevel(authLevel);
    }


    /**
     * check if gateway is trusted.
     * true if list is empty OR does not contain "none" OR
     * matches the one of the gateways in the list.
     */ 
    private boolean isValidGateway(String gateway) {
        return (gatewayList != null) && ((gatewayList.isEmpty()) 
                                     || (gatewayList.contains(gateway))); 
    }

    /**
     * Retreives the MSISDN number and WAP Gateway from 
     * HTTPServletRequest
     */
    private String getMSISDNNumberFromRequest(HttpServletRequest req) {
        String msisdnNumber = null;
        if ((parameterNameList != null) && (!parameterNameList.isEmpty())) {
            Iterator it = parameterNameList.iterator();
            String parameterName = null;
            int index;

           // search request headers for msisdn number
            while(it.hasNext()) {
                parameterName = it.next().toString();
                if (debug.messageEnabled()) {
                    debug.message("parameterName : " + parameterName);
                }
                // see if it's cookie
                if (searchAllHeaders || 
                    searchHeaderList.contains(SEARCH_COOKIE)) {
                    Cookie cookieArray[] = req.getCookies();
                    if (cookieArray != null) {
                        for (int i = 0; i < cookieArray.length; i++) {
                            String cookieName = cookieArray[i].getName();
                            if (cookieName != null && 
                                cookieName.equalsIgnoreCase(parameterName)) {
                                msisdnNumber = cookieArray[i].getValue();
                                break; // break from for loop
                            }
                        }// end for
                    }
                    // if MSISDN number is found in cookies come out
                    if (msisdnNumber != null) {
                        break; // break from while loop
                    }
                }// end inner if

                if (searchAllHeaders || 
                            searchHeaderList.contains(SEARCH_HEADER)) {
                    // check in headers.
                    msisdnNumber = req.getHeader(parameterName);
                }
                if (msisdnNumber != null) {
                    break;
                }
                // check in query/body
                if (searchAllHeaders || 
                    searchHeaderList.contains(SEARCH_PARAM)) {
                    msisdnNumber = req.getParameter(parameterName);
                }
                if (msisdnNumber != null) {
                    break;
                }
            }
        }
        return msisdnNumber;
    }

    /**
     * Cleans up module state.
     */
    public void destroyModuleState() {
        userPrincipal = null;
        userTokenId = null;
    }

    /**
     * TODO-JAVADOC
     */
    public void nullifyUsedVars() {
        bundle = null;
        locale = null;
        errorMsgKey = null;
        options = null;

        gatewayList = null;
        parameterNameList = null;
        searchHeaderList = null;
        userSearchAttr = null;
        serverHost = null;
        startSearchLoc = null;
        principleUser = null;
        principlePasswd = null;
        useSSL = null;
    }
}
