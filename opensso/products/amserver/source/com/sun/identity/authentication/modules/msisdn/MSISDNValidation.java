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
 * $Id: MSISDNValidation.java,v 1.3 2008/06/25 05:41:59 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */

package com.sun.identity.authentication.modules.msisdn;

import java.util.ResourceBundle;
import java.util.Map;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.plugins.ldapv3.LDAPAuthUtils;
import com.sun.identity.shared.locale.AMResourceBundleCache;

/**
 * A class that searches LDAP for the user having
 * attribute <code>sunIdentityMSISDNNumber</code> matching
 * the authentication client's device <code>msisdn</code> number.
 * LDAP user distinguished name is returned on a successful search.
 */
public class MSISDNValidation {

    private  ResourceBundle bundle = null;
    private static com.sun.identity.shared.debug.Debug debug = null;
    private String userTokenId;
    
    private String errorMsgKey = null;

    private Map options;
    
    /**
     * Holds handle to ResourceBundleCache to quickly get 
     * ResourceBundle for any Locale
     */
    protected static AMResourceBundleCache amCache = 
                    AMResourceBundleCache.getInstance();

    private String userSearchAttr;
    private String serverHost;  
    private int serverPort = 389;
    private String startSearchLoc;  
    private String principalUser;  
    private String principalPasswd;  
    private boolean useSSL;        
    private String userNamingAttr;
    private String returnUserDN;
    private static final String amAuthMSISDN = "amAuthMSISDN";
    private java.util.Locale locale;

    private static final String TRUSTED_GATEWAY_LIST = 
        ISAuthConstants.AUTH_ATTR_PREFIX_NEW + "MSISDNTrustedGatewayList";
    private static final String MSISDN_PARAMETER_NAME = 
        ISAuthConstants.AUTH_ATTR_PREFIX_NEW + "MSISDNParameterNameList";
    private static final String USER_SEARCH_ATTR = 
        ISAuthConstants.AUTH_ATTR_PREFIX_NEW + "MSISDNUserSearchAttribute";
    private static final String PRINCIPAL= 
        ISAuthConstants.AUTH_ATTR_PREFIX_NEW + "MSISDNPrincipalUser";
    private static final String PRINCIPAL_PASSWD = 
        ISAuthConstants.AUTH_ATTR_PREFIX_NEW + "MSISDNPrincipalPasswd";
    private static final String USE_SSL = 
        ISAuthConstants.AUTH_ATTR_PREFIX_NEW + "MSISDNUseSsl";
    private static final String LDAP_URL = 
        ISAuthConstants.AUTH_ATTR_PREFIX_NEW + "MSISDNLdapProviderUrl";
    private static final String MSISDN_AUTH_LEVEL =
        ISAuthConstants.AUTH_ATTR_PREFIX_NEW + "MSISDNAuthLevel";
    private static final String START_SEARCH_DN = 
        ISAuthConstants.AUTH_ATTR_PREFIX_NEW + "MSISDNBaseDn";
    private static final String RETURN_USER_DN = 
        ISAuthConstants.AUTH_ATTR_PREFIX_NEW + "MSISDNReturnUserDN";
    private static final String USER_NAMING_ATTR= 
        ISAuthConstants.AUTH_ATTR_PREFIX_NEW + "MSISDNUserNamingAttribute";
    private static final String DEFAULT_USER_NAMING_ATTR = "uid"; 

    /**
     * Creates <code>MSISNValidation</code> and set up the configuration to 
     * search LDAP for <code>msisdn</code> number.
     *
     * @param options configuration parameters to setup search in LDAP.
     * @param debug for logging debug messages.
     * @param bundle resource bundle for locale specific properties.
     * @param locale login locale.
     */
    protected MSISDNValidation(
        Map options,
        Debug debug,
        ResourceBundle bundle,
        java.util.Locale locale
    ) throws AuthLoginException {
        this.debug = debug;
        this.bundle = bundle;
        this.locale = locale;
        initMSISDNConfig(options);
    }


    /**
     * Retrieves MSISDN auth config parameters.
     * @param options , map contains MSISDN service attributes
     * @exception throws AuthLoginException on error
     */
    private void initMSISDNConfig(Map options) throws AuthLoginException {
        String errorMsgKey = null;
        if (options != null) {
            debug.message("MSISDN: getting attributes.");
            
            userSearchAttr = CollectionHelper.getMapAttr(
                options, USER_SEARCH_ATTR);
            principalUser = CollectionHelper.getMapAttr(options, PRINCIPAL);
            principalPasswd = CollectionHelper.getMapAttr(
                options, PRINCIPAL_PASSWD);
            useSSL = Boolean.valueOf(CollectionHelper.getMapAttr(
                options, USE_SSL, ISAuthConstants.FALSE_VALUE)).booleanValue();
            serverHost = CollectionHelper.getServerMapAttr(options, LDAP_URL);
            userNamingAttr = CollectionHelper.getMapAttr(
                options, USER_NAMING_ATTR,DEFAULT_USER_NAMING_ATTR);
            returnUserDN = CollectionHelper.getMapAttr(
                options, RETURN_USER_DN, ISAuthConstants.TRUE_VALUE);

            if (serverHost == null) {
                debug.error("Fatal error: LDAP Server and Port misconfigured");
                errorMsgKey = "wrongLDAPServer";
            } else {
                String port = null;
                // set LDAP Parameters
                int index = serverHost.indexOf(':');
                if (index != -1) { 
                    port = serverHost.substring(index+1);
                    serverPort = Integer.parseInt(port);
                    serverHost = serverHost.substring(0, index); 
                }
                startSearchLoc = CollectionHelper.getServerMapAttr(
                    options, START_SEARCH_DN);
                if (startSearchLoc == null) {
                    debug.error(
                        "Fatal error: LDAP Start Search DN misconfigured");
                    errorMsgKey = "wrongStartDN";
                }
            }

            if (debug.messageEnabled()) {
                debug.message("\n ldapProviderUrl="+ serverHost +
                    "\n\t serverPort = " + serverPort +
                    "\n\t startSearchLoc=" + startSearchLoc +
                    "\n\t userSearchAttr=" + userSearchAttr +
                    "\n\t principalUser=" + principalUser +
                    "\n\t serverHost =" + serverHost +
                    "\n\t userNamingAttr =" + userNamingAttr +
                    "\n\t returnUserDN =" + returnUserDN +
                    "\n\t useSSL=" + useSSL);
            }
        } else {
            debug.error("options is null");
            errorMsgKey = "MSISDNValidateEx";
        }
        if (errorMsgKey != null) {
            throw new AuthLoginException(amAuthMSISDN, errorMsgKey,null);
        }
    }

    /**
     * Returns user ID which has <code>sunIdentityMSISDNNumber</code> matching 
     * the <code>msisdn<code> number.
     *
     * @param msisdnNumber to search.
     * @throws AuthLoginException
     */
    protected String getUserId(String msisdnNumber) throws AuthLoginException {
        String validatedUserID = null;
        try {
            LDAPAuthUtils ldapUtil = 
                new LDAPAuthUtils(serverHost,serverPort,useSSL,
                          locale,startSearchLoc,debug) ;
            String searchFilter = new StringBuffer(250).append("(")
                .append(userSearchAttr).append("=")
                .append(msisdnNumber).append(")").toString();

            ldapUtil.setReturnUserDN(returnUserDN);
            ldapUtil.setUserNamingAttribute(userNamingAttr);
            ldapUtil.setFilter(searchFilter);
            ldapUtil.setAuthDN(principalUser);
            ldapUtil.setAuthPassword(principalPasswd);
            ldapUtil.searchForUser();
            switch  (ldapUtil.getState()) {
                case LDAPAuthUtils.USER_FOUND:
                    debug.message("User search successful");
                    validatedUserID = ldapUtil.getUserId();
                    return validatedUserID;
                case LDAPAuthUtils.USER_NOT_FOUND:
                    debug.error("MSISDN - Error finding user");
                    throw new AuthLoginException(amAuthMSISDN,
                            "userNotFound",null);
                case LDAPAuthUtils.SERVER_DOWN:
                    debug.error("Server down");
                    throw new AuthLoginException(amAuthMSISDN,
                            "MSISDNServerDown",null);
                default:
                    throw new AuthLoginException(amAuthMSISDN,
                            "MSISDNValidateEx",null);

            }
        } catch (Exception e) {
            throw new AuthLoginException(e);
        }
    }
}
