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
 * $Id: LDAPAuthUtils.java,v 1.21 2009/12/28 03:01:26 222713 Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2012 ForgeRock AS
 */

package com.sun.identity.idm.plugins.ldapv3;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.common.LDAPConnectionPool;
import com.sun.identity.common.ShutdownListener;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import com.sun.identity.shared.ldap.LDAPAttribute;
import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.LDAPControl;
import com.sun.identity.shared.ldap.LDAPEntry;
import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.LDAPModification;
import com.sun.identity.shared.ldap.LDAPModificationSet;
import com.sun.identity.shared.ldap.LDAPRebind;
import com.sun.identity.shared.ldap.LDAPRebindAuth;
import com.sun.identity.shared.ldap.LDAPReferralException;
import com.sun.identity.shared.ldap.LDAPRequestParser;
import com.sun.identity.shared.ldap.LDAPBindRequest;
import com.sun.identity.shared.ldap.LDAPModifyRequest;
import com.sun.identity.shared.ldap.LDAPSearchRequest;
import com.sun.identity.shared.ldap.LDAPSearchResults;
import com.sun.identity.shared.ldap.LDAPSearchConstraints;
import com.sun.identity.shared.ldap.controls.LDAPPasswordExpiringControl;
import com.sun.identity.shared.ldap.factory.JSSESocketFactory;
import com.sun.identity.shared.ldap.util.LDAPUtilException;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import java.util.Locale;

public class LDAPAuthUtils {
    private boolean returnUserDN;
    private String authDN = "";
    private Set userSearchAttrs = null;
    private String searchFilter = "";
    private String userNamingValue = null;
    private String userNamingAttr = null;
    private String ssl;
    private String baseDN;
    private String serverHost;
    private String secServerHost;
    private int serverPort = 389;
    private int secServerPort;
    private String userDN;
    private String userPassword;
    private String userId;
    private String authPassword = "";
    private String expiryTime;
    private int searchScope = 2;
    private int screenState;
    private int version = 3;
    private Debug debug = null;
    // JSS integration
    private boolean ldapSSL = false;
    // logging message
    private String logMessage = null;
    
    // Resource Bundle used to get l10N message
    private ResourceBundle bundle;
    
    // exception conditions
    public static final int USER_NOT_FOUND = 1;
    static final int CONFIG_ERROR = 4;
    static final int CANNOT_CONTACT_SERVER = 5;
    
    // user states (normal)
    static final int PASSWORD_EXPIRED_STATE = 20;
    public static final int PASSWORD_EXPIRING = 21;
    static final int PASSWORD_MISMATCH = 23;
    static final int PASSWORD_USERNAME_SAME = 24;
    static final int PASSWORD_NOT_UPDATE = 25;
    public static final int SUCCESS = 26;
    static final int WRONG_PASSWORD_ENTERED = 27;
    static final int PASSWORD_UPDATED_SUCCESSFULLY = 28;
    static final int USER_PASSWORD_SAME = 29;
    static final int PASSWORD_MIN_CHARACTERS = 30;
    public static final int SERVER_DOWN = 31;
    static final int PASSWORD_RESET_STATE = 32;
    public static final int USER_FOUND=33;
    public static final String STATUS_UP="UP";
    public static final String STATUS_DOWN="DOWN";
    
    static HashMap connectionPools = new HashMap();
    static HashMap adminConnectionPools = new HashMap();
    static HashMap connectionPoolsStatus = new HashMap();
    private LDAPConnectionPool cPool = null;
    private LDAPConnectionPool acPool = null;
    
    // password control  states
    private final static int NO_PASSWORD_CONTROLS = 0;
    private final static int PASSWORD_EXPIRED = -1;
    
    private final static int MIN_CONNECTION_POOL_SIZE = 1;
    private final static int MAX_CONNECTION_POOL_SIZE = 10;
    private final static String CONNECTION_POOL_SIZE_ATTR =
    "iplanet-am-auth-ldap-connection-pool-size";
    private final static String CONNECTION_POOL_DEFAULT_SIZE_ATTR =
    "iplanet-am-auth-ldap-connection-pool-default-size";
    
    private static int minDefaultPoolSize = MIN_CONNECTION_POOL_SIZE;
    private static int maxDefaultPoolSize = MAX_CONNECTION_POOL_SIZE;
    // contains host:port:min:max
    private static Set poolSize = null;
    private Set userAttributes = new HashSet();
    private Map userAttributeValues = new HashMap();
    private boolean isDynamicUSerEnabled;
    String [] attrs = null;
    private static  Debug debug2 = Debug.getInstance("amAuthLDAP");
    
    
    static {
        SSOToken dUserToken = null;
        try {
            // Gets the Admin SSOToken.
            // This API figures out admin DN and password and constructs
            // the SSOToken.
            dUserToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
            
            ServiceSchemaManager scm = new ServiceSchemaManager(
            "iPlanetAMAuthService", dUserToken);
            ServiceSchema schema = scm.getGlobalSchema();
            Map attrs = schema.getAttributeDefaults();
            
            poolSize = (Set)attrs.get(CONNECTION_POOL_SIZE_ATTR);
            
            String defaultPoolSize = CollectionHelper.getMapAttr(attrs,
            CONNECTION_POOL_DEFAULT_SIZE_ATTR,"");
            int index = defaultPoolSize.indexOf(":");
            if (index != -1) {
                try {
                    minDefaultPoolSize = Integer.parseInt(
                    defaultPoolSize.substring(0,index));
                } catch (NumberFormatException ex) {
                    debug2.error("Invalid ldap connection pool min size", ex);
                }
                try {
                    maxDefaultPoolSize = Integer.parseInt(
                    defaultPoolSize.substring(index + 1));
                } catch (NumberFormatException ex) {
                    debug2.error("Invalid ldap connection pool max size", ex);
                }
                
                if (maxDefaultPoolSize < minDefaultPoolSize) {
                    debug2.error("ldap connection pool max size is less" +
                    " than min size");
                    minDefaultPoolSize = MIN_CONNECTION_POOL_SIZE;
                    maxDefaultPoolSize = MAX_CONNECTION_POOL_SIZE;
                }
            } else {
                debug2.error("Invalid ldap connection pool size");
            }
        } catch (Exception ex) {
            debug2.error("Unable to get ldap connection pool size", ex);
        }
        dUserToken = null;
    }
   
    public LDAPAuthUtils(
        String host,
        int port,
        boolean ssl,
        Locale locale,
        Debug debug
    ) throws LDAPUtilException {
        this.bundle = AMResourceBundleCache.getInstance().getResBundle("amAuthLDAP", locale);
        serverHost = host;
        serverPort = port;
        ldapSSL = ssl;
        this.debug = debug;
        if ((serverHost == null) || (serverHost.length() < 1)) {
            debug.message("Invalid host name");
            throw new LDAPUtilException("HostInvalid", (Object[])null);
        }
    }

    private static HashSet getAllHostNames(String hostName, int portNumber, String bindingUser) {
        HashSet obj = new HashSet();
        StringTokenizer tokenStr = new StringTokenizer(hostName);
        while(tokenStr.hasMoreTokens()) {
           String key = tokenStr.nextToken().trim();
           if(key.indexOf(":") > 0) {
              key = key.substring(0,key.indexOf(":")) + ":" + portNumber + ":" + bindingUser;
           } else {
              key = key + ":" + portNumber + ":" + bindingUser;
           }
           obj.add(key);
        }
        return obj;
    }
    
    private static LDAPConnectionPool createConnectionPool(
        HashMap connectionPools,
        HashMap aConnectionPoolsStatus,
        String hostName,
        int portNumber,
        int verNum,
        boolean isSSL,
        String bindingUser,
        String bindingPwd
    ) throws LDAPException {
        LDAPConnectionPool conPool = null;
        LDAPConnection ldc = null;
        try {
            String key = hostName + ":" + portNumber + ":" + bindingUser;
            HashSet allHostNames = getAllHostNames(hostName, portNumber, bindingUser);
            conPool = (LDAPConnectionPool)connectionPools.get(key);
            
            if (conPool == null) {
                if (debug2.messageEnabled()) {
                    debug2.message("Create LDAPConnectionPool: " + hostName +
                    ":" + portNumber);
                }
               // Since connection pool for search and authentication
               // are different, each gets half the configured size 
                int min = minDefaultPoolSize/2 + 1;
                int max = maxDefaultPoolSize/2;
                if (min >= max) {
                    min = max - 1;
                }
                
                if (poolSize != null && !poolSize.isEmpty()) {
                    String tmpmin = null;
                    String tmpmax = null;
                    Iterator iter = poolSize.iterator();
                    while(iter.hasNext()) {
                        // host:port:min:max
                        String val = (String)iter.next();
                        StringTokenizer stz = new StringTokenizer(val, ":");
                        if (stz.countTokens() == 4) {
                            String h = stz.nextToken();
                            String p = stz.nextToken();
                            if (allHostNames.contains(h + ":" + p + ":" + bindingUser)) {
                                tmpmin = stz.nextToken();
                                tmpmax = stz.nextToken();
                                break;
                            }
                        }
                    }
                    if (tmpmin != null) {
                        try {
                            min = Integer.parseInt(tmpmin);
                            max = Integer.parseInt(tmpmax);
                            if (max < min) {
                                debug2.error("ldap connection pool max size " +
                                "is less than min size");
                                min = minDefaultPoolSize;
                                max = maxDefaultPoolSize;
                            }
                        }
                        catch (NumberFormatException ex) {
                            debug2.error(
                                "Invalid ldap connection pool size",ex);
                            min = minDefaultPoolSize;
                            max = maxDefaultPoolSize;
                        }
                    }
                    
                }
                if (debug2.messageEnabled()) {
                    debug2.message("LDAPAuthUtils.LDAPAuthUtils: min=" +
                    min + ", max=" + max);
                }
                
                synchronized(connectionPools) {
                    conPool = (LDAPConnectionPool)connectionPools.get(key);
                    
                    if (conPool == null) {
                        if (isSSL) {
                            ldc = new LDAPConnection(
                                new JSSESocketFactory(null));
                        }
                        else {
                            ldc = new LDAPConnection();
                        }
                        
                        ldc.connect(hostName, portNumber);
                        ldc.authenticate(verNum, bindingUser, bindingPwd);
                        ShutdownManager shutdownMan = 
                            ShutdownManager.getInstance();
                        if (shutdownMan.acquireValidLock()) {
                            try {
                                conPool = new LDAPConnectionPool(key + 
                                    "-AuthLDAP", min, max, ldc);
                                final LDAPConnectionPool tempConPool = conPool;
                                shutdownMan.addShutdownListener(
                                    new ShutdownListener() {
                                        public void shutdown() {
                                            tempConPool.destroy();
                                        }
                                    }
                                );
                            } finally {
                                shutdownMan.releaseLockAndNotify();
                            }
                        }
                        connectionPools.put(key, conPool);
                        if (aConnectionPoolsStatus != null) {
                            aConnectionPoolsStatus.put(key, STATUS_UP);
                        }
                    }
                }
            }
        } catch (LDAPException e) {
            if (ldc != null) {
                ldc.disconnect();
            }
            throw e;
        } catch (Exception e) {
            if (debug2.messageEnabled()) {
                debug2.message("Unable to create LDAPConnectionPool", e);
            }
            throw new LDAPUtilException(e);
        }
        return conPool;
    }
    
    
    /**
     * Constructor with host, port and base initializers
     *
     * @param host Host name
     * @param port port number
     * @param ssl <code>true</code> if it is SSL.
     * @param searchBaseDN directory base.
     * @param debug Debug object.
     * @throws LDAPUtilException
     */
    public LDAPAuthUtils(
        String host,
        int port,
        boolean ssl,
        Locale locale,
        String searchBaseDN,
        Debug debug
    ) throws LDAPUtilException {
        this(host, port, ssl, locale, debug);
        baseDN = searchBaseDN;
        if (baseDN.length() < 1) {
            debug.message("Invalid  search Base");
            throw new LDAPUtilException("SchBaseInvalid", (Object[])null);
        }
    }
    
    /**
     * Authenticates to the LDAP server using user input.
     *
     * @param user
     * @param password
     * @exception LDAPUtilException
     */
    public void authenticateUser(String user, String password)
            throws LDAPUtilException {
        if (password == null) {
            // password of zero length should be allowed.
            throw new LDAPUtilException("PwdInvalid",
            LDAPUtilException.INVALID_CREDENTIALS, null);
        }
        userId = user;
        userPassword = password;
        searchForUser();
        if (screenState == SERVER_DOWN || screenState == USER_NOT_FOUND) {
            return;
        }
        authenticate();
        
    }
    
    /**
     * Returns connection from pool.  Reauthenticate if necessary
     *
     * @return connection that is available to use
     */
    private LDAPConnection getConnection()
            throws LDAPException {
        if (cPool == null) {
            cPool = createConnectionPool(connectionPools,
            null, serverHost, serverPort, version,
            ldapSSL, authDN, authPassword);
        }
        LDAPConnection ldc = cPool.getConnection();
        return ldc;
    }
    
    
    /**
     * Just call the pool method to release the connection so that the
     * given connection is free for others to use.
     *
     * @param conn connection in the pool to be released for others to use
     */
    private void releaseConnection( LDAPConnection conn ) {
        if (conn == null) {
            return;
        }
        cPool.close(conn);
    }
    /**
     * Get connection from pool.  Reauthenticate if necessary
     * @return connection that is available to use
     */
    private LDAPConnection getAdminConnection() throws LDAPException {
        if (acPool == null) {
            acPool = createConnectionPool(adminConnectionPools,
             connectionPoolsStatus, serverHost, serverPort,
             version, ldapSSL, authDN, authPassword);
        }
        LDAPConnection ldc = acPool.getConnection();
         
        return ldc;
    }
     
     
    /**
     * Just call the pool method to release the connection so that the
     * given connection is free for others to use
     * @param conn connection in the pool to be released for others to use
     */
    private void releaseAdminConnection( LDAPConnection conn ) {
        if (conn == null) {
            return;
        }
         
        acPool.close( conn );
    }
         

   
    /**
     * TODO-JAVADOC
     */
    public void authenticateSuperAdmin(String user, String password)
            throws LDAPUtilException {
        if (password == null || password.length() == 0) {
            throw new LDAPUtilException("PwdInvalid",
            LDAPUtilException.INVALID_CREDENTIALS, null);
        }
        userDN = user;
        userPassword = password;
        authenticate();
        userId = user;
    }
    
    /**
     * Updates to new password by  using the  parameters passed by the user.
     *
     * @param oldPwd Current password entered.
     * @param password New password entered.
     * @param confirmPassword Confirm password.
     * @throws LDAPUtilException
     */
    public void changePassword(
        String oldPwd,
        String password,
        String confirmPassword
    ) throws LDAPUtilException {
        if (password.equals(oldPwd)){
            setState(WRONG_PASSWORD_ENTERED);
            return;
        }
        if (!(password.equals(confirmPassword))) {
            setState(PASSWORD_MISMATCH);
            return;
        }
        if (password.equals(userId)) {
            setState(USER_PASSWORD_SAME);
            return;
        }
        LDAPConnection  modCtx = null;
        try {
            LDAPModificationSet mods =new LDAPModificationSet();
            LDAPAttribute attrOldPwd = new LDAPAttribute("userpassword",
               oldPwd);
            mods.add(LDAPModification.DELETE, attrOldPwd);
            LDAPAttribute attrPwd = new LDAPAttribute("userpassword",
                password);
            mods.add(LDAPModification.ADD,attrPwd);
            try {
                LDAPBindRequest bindRequest =
                    LDAPRequestParser.parseBindRequest(version, userDN, oldPwd);
                LDAPModifyRequest modRequest =
                    LDAPRequestParser.parseModifyRequest(userDN, mods);
                modCtx = getConnection();
                modCtx.authenticate(bindRequest);
                setDefaultReferralCredentials(modCtx);
                modCtx.modify(modRequest);
            } finally {
                if (modCtx != null) {
                    releaseConnection(modCtx);
                }
            }
            setState(PASSWORD_UPDATED_SUCCESSFULLY);
        } catch(LDAPException le) {
            if (le.getLDAPResultCode() ==
                LDAPException.CONSTRAINT_VIOLATION) {
                // set log message to be logged in LDAP modules
                setLogMessage(le.getLocalizedMessage() + " : " +
                    le.getLDAPErrorMessage());
                setState(PASSWORD_MIN_CHARACTERS);
            } else if (le.getLDAPResultCode() == LDAPException.CONNECT_ERROR ||
            le.getLDAPResultCode() == LDAPException.SERVER_DOWN ||
            le.getLDAPResultCode() == LDAPException.UNAVAILABLE) {
                if (debug.messageEnabled()) {
                    debug.message("changepassword:Cannot connect to " +
                    serverHost +": ", le);
                }
                setState(SERVER_DOWN);
                return;
            } else {
                setState(PASSWORD_NOT_UPDATE);
            }
            debug.error("Cannot update : ",le);
        }    
    }

    public void setLogMessage(String logMsg) {
        this.logMessage = logMsg;
    }

    public String getLogMessage() {
        return this.logMessage;
    }
    
    private String buildUserFilter() {
        StringBuffer buf = new StringBuffer(100);
        buf.append("(");
        if (userSearchAttrs.size() == 1) {
            buf.append((String)userSearchAttrs.iterator().next());
            buf.append("=");
            buf.append(userId);
        } else {
            buf.append("|");
            Iterator iter = userSearchAttrs.iterator();
            while (iter.hasNext()) {
                buf.append("(");
                buf.append((String)iter.next());
                buf.append("=");
                buf.append(userId);
                buf.append(")");
            }
        }
        buf.append(")");
        return buf.toString();
    }
    
    /**
     * Searches and returns user for a specified attribute using parameters
     * specified in constructor and/or by settin properties.
     *
     * @throws LDAPUtilException
     */
    public void searchForUser() throws LDAPUtilException {
        
        // make some special case where searchScope == BASE
        // construct the userDN without searching directory
        // assume that there is only one user attribute
        
        if (searchScope == LDAPConnection.SCOPE_BASE) {
            if (userSearchAttrs.size() == 1) {
                StringBuffer dnBuffer = new StringBuffer();
                dnBuffer.append((String) userSearchAttrs.iterator().next());
                dnBuffer.append("=");
                dnBuffer.append(userId);
                dnBuffer.append(",");
                dnBuffer.append(baseDN);
                userDN = dnBuffer.toString();
                if (debug.messageEnabled()) {
                    debug.message("searchForUser, searchScope = BASE," +
                    "userDN =" + userDN);
                }
                if (!isDynamicUSerEnabled &&
                userSearchAttrs.contains(userNamingAttr)) {
                    return;
                } else if (isDynamicUSerEnabled &&
                (userAttributes == null || userAttributes.isEmpty())) {
                    debug.message("user creation attribute list is empty ");
                    return;
                }
                baseDN=userDN;
            } else {
                if (debug.messageEnabled()) {
                    debug.message("cannot find user entry using scope=0"+
                    "setting scope=1");
                }
                
                searchScope = 1;
            }
        }
        if (searchFilter == null || searchFilter.length() == 0) {
            searchFilter = buildUserFilter();
        } else {
            StringBuffer bindFilter = new StringBuffer(200);
            if (userId != null) {
                bindFilter.append("(&");
                bindFilter.append(buildUserFilter());
                bindFilter.append(searchFilter);
                bindFilter.append(")");
            } else {
                bindFilter.append(searchFilter);
            }
            searchFilter = bindFilter.toString();
        }
        
        String[] res = null;
        userDN = null;
        
        // JSS integration
        LDAPConnection ldc = null;
        
        try {
            if (debug.messageEnabled()) {
                debug.message("Connecting to " + serverHost + ":" +
                serverPort + "\nSearching " + baseDN + " for " +
                searchFilter + "\nscope = " + searchScope);
            }
            
            // Search
            int userAttrSize=0;
            if (attrs == null) {
                if ((userAttributes == null) || (userAttributes.isEmpty())){
                    userAttrSize = 2;
                    attrs = new String[userAttrSize];
                    attrs[0] ="dn";
                    attrs[1]=userNamingAttr;
                } else {
                    userAttrSize = userAttributes.size();
                    attrs = new String[userAttrSize + 2];
                    attrs[0] = "dn";
                    attrs[1] = userNamingAttr;
                    
                    Iterator attrItr = userAttributes.iterator();
                    for (int i = 2; i < userAttrSize + 2; i++) {
                        attrs[i] = (String)attrItr.next();
                    }
                }
            }
            
            if (debug.messageEnabled()) {
                debug.message("userAttrSize is : " + userAttrSize);
            }
            
            LDAPSearchResults results = null;
            LDAPSearchRequest request = LDAPRequestParser.parseSearchRequest(
                baseDN, searchScope, searchFilter, attrs, false);
            try {
                ldc = getAdminConnection();
                results = ldc.search(request);
            } finally {
                if (ldc != null) {
                    releaseAdminConnection(ldc);
                }
            }
            int userMatches = 0;
            LDAPEntry entry = null;
            boolean userNamingValueSet=false;
            while (results.hasMoreElements()) {
                try {
                    entry = results.next();
                    userDN = entry.getDN();
                    userMatches ++;
                } catch (LDAPReferralException refe) {
                    debug.message("LDAPReferral Detected.");
                    
                    //TODO: Referrals can be handled here.
                    continue;
                }
                if (attrs != null && attrs.length > 1) {
                    userNamingValueSet = true;
                    LDAPAttribute attr = entry.getAttribute(userNamingAttr);
                    if (attr != null) {
                        userNamingValue = attr.getStringValueArray()[0];
                    }
                    if (isDynamicUSerEnabled && (attrs.length > 2)) {
                        for (int i = 2; i < userAttrSize + 2; i++) {
                            attr = entry.getAttribute(attrs[i]);
                            if (attr != null) {
                                Set s = new HashSet();
                                for (int j = 0; j < attr.size(); j++) {
                                    s.add(attr.getStringValueArray()[j]);
                                }
                                userAttributeValues.put(attrs[i], s);
                            }
                        }
                    }
                }
            }
            if (userNamingValueSet && (userDN == null ||
            userNamingValue == null)) {
                if (debug.messageEnabled()) {
                    debug.message("Cannot find entries for " + searchFilter);
                }
                setState(USER_NOT_FOUND);
                return;
            } else {
                if (userDN == null) {
                    if (debug.messageEnabled()) {
                        debug.message(
                            "Cannot find entries for " + searchFilter);
                    }
                    setState(USER_NOT_FOUND);
                    return;
                } else {
                    setState(USER_FOUND);
                }
            }
            if (userMatches > 1) {
                // multiple user matches found
                debug.error(
                "searchForUser : Multiple matches found for user '" +
                userId +
                "'. Please modify search start DN/filter/scope " +
                "to make sure unique match returned. Contact your " +
                "administrator to fix the problem");
                throw new LDAPUtilException("multipleUserMatchFound",
                (Object[])null);
            }
        } catch (LDAPException e) {
            debug.message("Search for User error: ", e);
            debug.message("resultCode: " + e.getLDAPResultCode());
            if (e.getLDAPResultCode() == LDAPException.CONNECT_ERROR ||
            e.getLDAPResultCode() == LDAPException.SERVER_DOWN ||
            e.getLDAPResultCode() == LDAPException.UNAVAILABLE) {
                if (debug.messageEnabled()) {
                    debug.message("Cannot connect to " + serverHost +
                    ": ", e);
                }
                setState(SERVER_DOWN);
                return;
            } else if (e.getLDAPResultCode() ==
            LDAPException.INVALID_CREDENTIALS) {
                debug.message("Cannot authenticate ");
                throw new LDAPUtilException("FConnect",
                LDAPException.INVALID_CREDENTIALS, null);
            } else if (e.getLDAPResultCode()
                == LDAPException.UNWILLING_TO_PERFORM
            ) {
                debug.message("Account Inactivated or Locked ");
                throw new LDAPUtilException("FConnect",
                LDAPException.UNWILLING_TO_PERFORM, null);
            } else if (e.getLDAPResultCode() == LDAPException.NO_SUCH_OBJECT) {
                throw new LDAPUtilException("noUserMatchFound",
                LDAPException.NO_SUCH_OBJECT, null);
            } else {
                debug.message("Exception while searching", e);
                setState(USER_NOT_FOUND);
                return;
            }
        }
    }
    
    /**
     * Connect to LDAP server using parameters specified in
     * constructor and/or by settin properties attempt to authenticate.
     * checks for the password controls and  sets to the appropriate states
     */
    private void authenticate() throws LDAPUtilException {
        LDAPConnection ldc = null;
        LDAPControl[] controls = null;
        try {
            try {
            LDAPBindRequest request = LDAPRequestParser.parseBindRequest(
                version,userDN, userPassword);
            ldc = getConnection();
                ldc.authenticate(request);
                controls = ldc.getResponseControls();
            } finally {
                if (ldc != null) {
                    releaseConnection(ldc);
                }
            }
            
            /* Were any controls returned? */
            int seconds = checkControls(controls);
            switch(seconds) {
                case NO_PASSWORD_CONTROLS:
                    debug.message("No controls returned");
                    setState(SUCCESS);
                    break;
                case PASSWORD_EXPIRED:
                    if(debug.messageEnabled()){
                        debug.message(
                            "Password expired and must be reset" );
                    }
                    setState(PASSWORD_RESET_STATE);
                    break;
                default:
                    setExpTime(seconds);
                    if (debug.messageEnabled()) {
                        debug.message("Password expires in " + seconds +
                        " seconds");
                    }
                    setState(PASSWORD_EXPIRING);
            }
            
        } catch(LDAPException e) {
            if (e.getLDAPResultCode() ==
            LDAPException.INVALID_CREDENTIALS) {
                if (checkControls(controls) == PASSWORD_EXPIRED) {
                    if(debug.messageEnabled()){
                        debug.message( "Password expired and must be reset" );
                    }
                    setState(PASSWORD_EXPIRED_STATE);
                    return;
                } else {
                    debug.message("Failed auth due to invalid credentials");
                    throw new LDAPUtilException("CredInvalid",
                    LDAPException.INVALID_CREDENTIALS, null);
                }
            } else if (e.getLDAPResultCode() == LDAPException.NO_SUCH_OBJECT) {
                debug.message("user does not exist");
                throw new LDAPUtilException("UsrNotExist",
                    LDAPException.NO_SUCH_OBJECT, null);
            } else if (e.getLDAPResultCode() == LDAPException.CONNECT_ERROR ||
                e.getLDAPResultCode() == LDAPException.SERVER_DOWN ||
                e.getLDAPResultCode() == LDAPException.UNAVAILABLE
            ) {
                if (debug.messageEnabled()) {
                    debug.message("Cannot connect to " + serverHost +
                    ": ", e);
                }
                setState(SERVER_DOWN);
                return;
            } else if (e.getLDAPResultCode() ==
                LDAPException.UNWILLING_TO_PERFORM
            ) {
                debug.message("Account Inactivated or Locked ");
                throw new LDAPUtilException("FConnect",
                    LDAPException.UNWILLING_TO_PERFORM, null);
            } else if (e.getLDAPResultCode() ==
                LDAPException.INAPPROPRIATE_AUTHENTICATION
            ) {
                debug.message(
                    "Failed auth due to inappropriate authentication");
                throw new LDAPUtilException("InappAuth",
                LDAPException.INAPPROPRIATE_AUTHENTICATION, null);
            } else if (e.getLDAPResultCode() ==
                LDAPException.CONSTRAINT_VIOLATION)
            {
                debug.message("Exceed password retry limit.");
                throw new LDAPUtilException(ISAuthConstants.EXCEED_RETRY_LIMIT,
                    LDAPException.CONSTRAINT_VIOLATION, null);
            } else {
                if (debug.messageEnabled()) {
                    debug.message( "Cannot authenticate to " + serverHost+
                    ": " ,e );
                }
                throw new LDAPUtilException("FAuth", (Object[])null);
            }
        }    
    }
    
    /**
     * checks for  an LDAP v3 server whether the  control has returned
     * if a password has expired or password is expiring and password
     * policy is enabled on the server.
     * @return PASSWOR_EXPIRED if password has expired
     * @return number of seconds until expiration if password is going to expire
     */
    private int checkControls(LDAPControl[] controls) {
        int status = NO_PASSWORD_CONTROLS;
        if ((controls != null) && (controls.length >= 1)) {
            LDAPPasswordExpiringControl expgControl = null;
            for (int i = 0; i < controls.length; i++) {
                if (controls[i].getType() ==
                    LDAPControl.LDAP_PASSWORD_EXPIRED_CONTROL) {
                    return PASSWORD_EXPIRED;
                }
                if (controls[i].getType() ==
                    LDAPControl.LDAP_PASSWORD_EXPIRING_CONTROL) {
                    expgControl = (LDAPPasswordExpiringControl)controls[i];
                }
            }
            if (expgControl != null) {
                try {
                        /* Return the number of seconds until expiration */
                    return expgControl.getSecondsToExpiration();
                } catch(NumberFormatException e) {
                    if (debug.messageEnabled()) {
                        debug.message( "Unexpected message <" +
                        expgControl.getMessage() +
                        "> in password expiring control" );
                    }
                }
            }
        }
        return NO_PASSWORD_CONTROLS;
    }
    
    // LDAP parameters are set  here .......
    /**
     * TODO-JAVADOC
     */
    public String getUserId() {
        if (returnUserDN) {
            return userDN;
        }
        else {
            return userNamingValue;
        }
    }
    
    /**
     * TODO-JAVADOC
     */
    public String getUserId(String name) {
        String uid = getUserId();
        if (uid != null) {
            return uid;
        } else {
            return name;
        }
    }
    
    /**
     * TODO-JAVADOC
     */
    public void setUserNamingAttribute(String s) throws LDAPUtilException{
        if (s == null || s.length() < 1) {
            throw new LDAPUtilException("UNAttr", (Object[])null);
        }
        userNamingAttr = s;
    }

    /**
     * TODO-JAVADOC
     */
    public  void setUserSearchAttribute(Set attr) throws LDAPUtilException {
        if (attr == null || attr.isEmpty()) {
            throw new LDAPUtilException("USchAttr", (Object[])null);
        }
        userSearchAttrs = attr;
    }

    /**
     * TODO-JAVADOC
     */
    public void setFilter(String filter) {
        searchFilter = filter;
    }

    /**
     * TODO-JAVADOC
     */
    public void setBase(String basedn) {
        baseDN = basedn;
    }

    private void setAuthProtocol(String protocol) {
        ssl = protocol;
    }
    
    /**
     * Sets the DN to authenticate as; null or empty for anonymous.
     *
     * @param authdn the DN to authenticate as
     */
    public  void setAuthDN(String authdn) {
        authDN = authdn;
    }
    
    /**
     * Sets the password for the DN to authenticate as.
     *
     * @param authpassword the password to use in authentication.
     */
    public  void setAuthPassword(String authpassword) {
        authPassword = authpassword;
    }

    /**
     * Sets the search scope using an integer.
     *
     * @param scope Search scope (one of <code>LDAPConnection.SCOPE_BASE</code>,
     *        <code>LDAPConnection.SCOPE_SUB</code>,
     *        <code>LDAPConnection.SCOPE_ONE</code>).
     */
    public void setScope(int scope) {
        searchScope = scope;
    }
    
    /**
     * Returns the latest screen state.
     *
     * @return The latest screen state.
     */
    public int getState() {
        return screenState;
    }

    /**
     * Sets a screen state for retrieval by a client.
     *
     * @param code Screen state.
     */
    public void setState(int code ) {
        screenState = code;
    }

    private void setExpTime(int sec) {
        expiryTime = null;
        int days = sec/(24*60*60);
        int hours = (sec%(24*60*60))/3600;
        int minutes = (sec%3600)/60;
        int seconds = sec%60;
        if (hours <= 0 && minutes <= 0 && seconds <= 0) {
            expiryTime = days+" days: ";
            return;
        } else {
            String DAYS = bundle.getString("days");
            String HRS = bundle.getString("hours");
            String MIN = bundle.getString("minutes");
            String SEC = bundle.getString("seconds");
            expiryTime = days+" "+DAYS+": "+
            hours+" "+HRS+": "+
            minutes+" "+MIN+": "+
            seconds+" "+SEC;
        }
    }

    /**
     * Sets a screen state for retrieval by a client.
     *
     * @return Return the number of seconds until expiration
     */
    public String getExpTime() {
        return expiryTime;
    }
    
    /**
     * Returns <code>true</code> if the connection represented by this object
     * is open at this time.
     *
     * @return <code>true</code> if connected to an LDAP server over this
     *         connection.
     *         Returns <code>false</code> if not connected to an LDAP server.
     */
    public boolean isServerRunning(String host, int port) {
        LDAPConnection ldapCon = null;
        boolean running = false;
        try {
            if (ldapSSL) {
                ldapCon = new LDAPConnection(new JSSESocketFactory(null));
            }
            else {
                ldapCon = new LDAPConnection();
            }
            ldapCon.connect(host, port);
            running = ldapCon.isConnected();
            ldapCon.disconnect();
        } catch (Exception ldapEx) {
            debug.message("Primary Server is not running");
        }
        return running;
        
    }

    /**
     * TODO-JAVADOC
     */
    public void setReturnUserDN(String s) {
        if (s.equalsIgnoreCase("true")) {
            returnUserDN = true;
        } else {
            returnUserDN = false;
        }
    }
    
    
    /**
     * Sets attributes names to be use in the search for the
     * user prior to authenticating with the bind. These attributes
     * and their values will be set in the <code>userAttributeValues</code>
     * Map which may be retrieved via <code>getUserAttributeValues.</code>
     *
     * @param attributeNames Set of attributes to be retrieved during
     *        user search.
     */
    public void setUserAttributes(Set attributeNames) {
        userAttributes = attributeNames;
    }
    
    /**
     * Returns the attributes and their values obtained from the user
     * search during authentication.
     *
     * @return Map of attribute name to a set of values.
     */
    public Map getUserAttributeValues() {
        return userAttributeValues;
    }
    
    /**
     * Sets the value of the dynamic profile creation configured in
     * Authentication service.
     *
     * @param isEnable
     */
    public void setDynamicProfileCreationEnabled(boolean isEnable) {
        isDynamicUSerEnabled = isEnable;
    }
    
    /**
     * Sets the return attributes for ldap search.
     *
     * @param attrs Array containing attributes.
     */
    public void setUserAttrs(String[] attrs) {
        this.attrs = attrs;
    }
    
    /**
     * This method implements the LDAPRebind interface for automatic
     * referrals with client control. The same user DN and password
     * which was used for the original connection for authentication
     * is used.
     */
    protected void setDefaultReferralCredentials(LDAPConnection conn) {
        final LDAPConnection mConn = conn;
        LDAPRebind reBind = new LDAPRebind() {
            public LDAPRebindAuth getRebindAuthentication(String host,int port){
                return new LDAPRebindAuth(mConn.getAuthenticationDN(),
                mConn.getAuthenticationPassword());
            }
        };
        LDAPSearchConstraints cons = conn.getSearchConstraints();
        cons.setReferrals(true);
        cons.setRebindProc(reBind);
        conn.setSearchConstraints(cons);
    }
}
