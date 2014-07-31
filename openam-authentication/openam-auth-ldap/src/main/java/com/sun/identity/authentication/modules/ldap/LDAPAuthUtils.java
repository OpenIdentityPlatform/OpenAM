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

/**
 * Portions Copyrighted 2011-2014 ForgeRock AS
 * Portions Copyrighted 2014 Nomura Research Institute, Ltd
 */

package com.sun.identity.authentication.modules.ldap;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import org.forgerock.openam.ldap.LDAPURL;
import static org.forgerock.openam.ldap.LDAPUtils.convertToLDAPURLs;
import static org.forgerock.openam.ldap.LDAPUtils.newFailoverConnectionPool;
import static org.forgerock.openam.utils.CollectionUtils.asList;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.Connections;
import org.forgerock.opendj.ldap.DecodeException;
import org.forgerock.opendj.ldap.DecodeOptions;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.ErrorResultIOException;
import org.forgerock.opendj.ldap.FailoverLoadBalancingAlgorithm;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.LDAPOptions;
import org.forgerock.opendj.ldap.ModificationType;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.SSLContextBuilder;
import org.forgerock.opendj.ldap.SearchResultReferenceIOException;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.TrustManagers;
import org.forgerock.opendj.ldap.controls.Control;
import org.forgerock.opendj.ldap.controls.PasswordExpiredResponseControl;
import org.forgerock.opendj.ldap.controls.PasswordExpiringResponseControl;
import org.forgerock.opendj.ldap.controls.PasswordPolicyErrorType;
import org.forgerock.opendj.ldap.controls.PasswordPolicyRequestControl;
import org.forgerock.opendj.ldap.controls.PasswordPolicyResponseControl;
import org.forgerock.opendj.ldap.controls.PasswordPolicyWarningType;
import org.forgerock.opendj.ldap.requests.BindRequest;
import org.forgerock.opendj.ldap.requests.ModifyRequest;
import org.forgerock.opendj.ldap.requests.Requests;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.BindResult;
import org.forgerock.opendj.ldap.responses.Result;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldif.ConnectionEntryReader;
import org.forgerock.util.thread.listener.ShutdownListener;

public class LDAPAuthUtils {
    private boolean returnUserDN;
    private String authDN = "";
    private Set<String> userSearchAttrs = null;
    private String searchFilter = "";
    private String userNamingValue = null;
    private String userNamingAttr = null;
    private String baseDN;
    private Set<String> primaryServers;
    private Set<String> secondaryServers;
    //only used for logging
    private Set<String> servers;
    private String userDN;
    private String userPassword;
    private String userId;
    private char[] authPassword;
    private String expiryTime;
    private SearchScope searchScope;
    private int heartBeatInterval = 10;
    private String heartBeatTimeUnit;
    private ModuleState screenState;
    private int graceLogins;
    private int operationsTimeout = 0;
    private static final Debug debug = Debug.getInstance("amAuthLDAP");
    // JSS integration
    private boolean ldapSSL = false;
    private boolean beheraEnabled = true;
    private boolean trustAll = true;
    private boolean isAd = false;

    // Resource Bundle used to get l10N message
    private ResourceBundle bundle;

    private static Map<String, ConnectionFactory> connectionPools =
            new ConcurrentHashMap<String, ConnectionFactory>();
    private static Map<String, ConnectionFactory> adminConnectionPools =
            new ConcurrentHashMap<String, ConnectionFactory>();
    private ConnectionFactory cPool = null;
    private ConnectionFactory acPool = null;
    private final static int NO_EXPIRY_TIME = -1;
    private final static int MIN_CONNECTION_POOL_SIZE = 1;
    private final static int MAX_CONNECTION_POOL_SIZE = 10;
    private final static String CONNECTION_POOL_SIZE_ATTR =
    "iplanet-am-auth-ldap-connection-pool-size";
    private final static String CONNECTION_POOL_DEFAULT_SIZE_ATTR =
    "iplanet-am-auth-ldap-connection-pool-default-size";
    private final static String SPACE = " ";
    private final static String COLON = ":";

    private static int minDefaultPoolSize = MIN_CONNECTION_POOL_SIZE;
    private static int maxDefaultPoolSize = MAX_CONNECTION_POOL_SIZE;
    // contains host:port:min:max
    private static Set<String> poolSize = null;
    private Set<String> userAttributes = new HashSet<String>();
    private Map<String, Set<String>> userAttributeValues = new HashMap<String, Set<String>>();
    private boolean isDynamicUserEnabled;
    private String [] attrs = null;
    private static String AD_PASSWORD_EXPIRED = "data 532";
    private static String S4_PASSWORD_EXPIRED = "NT_STATUS_PASSWORD_EXPIRED";
    private static String AD_ACCOUNT_DISABLED = "data 533";
    private static String S4_ACCOUNT_DISABLED = "NT_STATUS_ACCOUNT_DISABLED";
    private static String AD_ACCOUNT_EXPIRED = "data 701";
    private static String S4_ACCOUNT_EXPIRED = "NT_STATUS_ACCOUNT_EXPIRED";
    private static String AD_PASSWORD_RESET = "data 773";
    private static String S4_PASSWORD_RESET = "NT_STATUS_PASSWORD_MUST_CHANGE";
    private static String AD_ACCOUNT_LOCKED = "data 775";
    private static String S4_ACCOUNT_LOCKED = "NT_STATUS_ACCOUNT_LOCKED_OUT";
    private static String LDAP_PASSWD_ATTR = "userpassword";
    private static String AD_PASSWD_ATTR = "UnicodePwd";

    static {
        SSOToken dUserToken;

        try {
            // Gets the Admin SSOToken.
            // This API figures out admin DN and password and constructs
            // the SSOToken.
            dUserToken = AccessController.doPrivileged(AdminTokenAction.getInstance());

            ServiceSchemaManager scm = new ServiceSchemaManager(
                "iPlanetAMAuthService", dUserToken);
            ServiceSchema schema = scm.getGlobalSchema();
            Map attrs = schema.getAttributeDefaults();

            poolSize = (Set<String>) attrs.get(CONNECTION_POOL_SIZE_ATTR);

            String defaultPoolSize = CollectionHelper.getMapAttr(attrs,
            CONNECTION_POOL_DEFAULT_SIZE_ATTR,"");
            int index = defaultPoolSize.indexOf(COLON);

            if (index != -1) {
                try {
                    minDefaultPoolSize = Integer.parseInt(
                    defaultPoolSize.substring(0,index));
                } catch (NumberFormatException ex) {
                    debug.error("Invalid ldap connection pool min size", ex);
                }

                try {
                    maxDefaultPoolSize = Integer.parseInt(
                    defaultPoolSize.substring(index + 1));
                } catch (NumberFormatException ex) {
                    debug.error("Invalid ldap connection pool max size", ex);
                }

                if (maxDefaultPoolSize < minDefaultPoolSize) {
                    debug.error("ldap connection pool max size is less" +
                        " than min size");
                        minDefaultPoolSize = MIN_CONNECTION_POOL_SIZE;
                        maxDefaultPoolSize = MAX_CONNECTION_POOL_SIZE;
                }
            } else {
                debug.error("Invalid ldap connection pool size");
            }
        } catch (Exception ex) {
            debug.error("Unable to get ldap connection pool size", ex);
        }
    }

    /**
     * Constructor initializing the basic parameters.
     *
     * @param primaryServers List of primary servers.
     * @param secondaryServers List of secondary servers.
     * @param ldapSSL <code>true</code> if it is SSL.
     * @param bundle ResourceBundle to be used for getting localized messages.
     * @param baseDN Directory Base DN.
     * @throws LDAPUtilException If the provided search base was invalid.
     */
    public LDAPAuthUtils(Set<String> primaryServers, Set<String> secondaryServers, boolean ldapSSL,
            ResourceBundle bundle, String baseDN) throws LDAPUtilException {
        this.primaryServers = primaryServers;
        this.secondaryServers = secondaryServers;
        servers = new LinkedHashSet<String>(primaryServers);
        servers.addAll(secondaryServers);
        this.bundle = bundle;
        this.ldapSSL = ldapSSL;

        this.baseDN = baseDN;

        if (baseDN == null || baseDN.length() < 1) {
            debug.message("Invalid  search Base");
            throw new LDAPUtilException("SchBaseInvalid", (Object[]) null);
        }
    }

    private ConnectionFactory createConnectionPool(Map<String, ConnectionFactory> connectionPools,
        String bindingUser, char[] bindingPwd) throws ErrorResultException, LDAPUtilException {
        ConnectionFactory connPool = null;

        try {
            String configName = servers.toString() + ":" + bindingUser;
            connPool = connectionPools.get(configName);
            if (connPool == null) {
                synchronized(connectionPools) {
                    connPool = connectionPools.get(configName);
                    LDAPOptions options = new LDAPOptions();
                    
                    options.setTimeout(operationsTimeout, TimeUnit.SECONDS);                  

                    if (connPool == null) {
                        if (debug.messageEnabled()) {
                            debug.message("Create ConnectionPool for servers:\n" + servers);
                        }

                        // Since connection pool for search and authentication
                        // are different, each gets half the configured size
                        int min = minDefaultPoolSize / 2 + 1;
                        int max = maxDefaultPoolSize / 2;

                        if (min >= max) {
                            min = max - 1;
                        }

                        Set<LDAPURL> primaryUrls = convertToLDAPURLs(primaryServers);
                        Set<LDAPURL> secondaryUrls = convertToLDAPURLs(secondaryServers);
                        if (poolSize != null && !poolSize.isEmpty()) {
                            String tmpmin = null;
                            String tmpmax = null;

                            for (String val : poolSize) {
                                // host:port:min:max
                                StringTokenizer stz = new StringTokenizer(val, ":");

                                if (stz.countTokens() == 4) {
                                    LDAPURL url = LDAPURL.valueOf(stz.nextToken() + ":" + stz.nextToken());
                                    if (primaryUrls.contains(url) || secondaryUrls.contains(url)) {
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
                                        debug.error("ldap connection pool max size is less than min size");
                                        min = minDefaultPoolSize;
                                        max = maxDefaultPoolSize;
                                    }
                                } catch (NumberFormatException ex) {
                                    debug.error("Invalid ldap connection pool size", ex);
                                    min = minDefaultPoolSize;
                                    max = maxDefaultPoolSize;
                                }
                            }
                        }

                        if (debug.messageEnabled()) {
                            debug.message("LDAPAuthUtils.LDAPAuthUtils: min="
                                    + min + ", max=" + max);
                        }
                        if (ldapSSL) {
                            SSLContextBuilder builder = new SSLContextBuilder();

                            if (trustAll) {
                                builder.setTrustManager(TrustManagers.trustAll());
                            }

                            SSLContext sslContext = builder.getSSLContext();
                            options.setSSLContext(sslContext);
                        }

                        final ConnectionFactory connFactory;
                        ConnectionFactory primaryCf = newFailoverConnectionPool(primaryUrls, bindingUser,
                                bindingPwd, max, heartBeatInterval, heartBeatTimeUnit, options);
                        if (secondaryServers.isEmpty()) {
                            connFactory = primaryCf;
                        } else {
                            ConnectionFactory secondaryCf = newFailoverConnectionPool(secondaryUrls, bindingUser,
                                    bindingPwd, max, heartBeatInterval, heartBeatTimeUnit, options);
                            connFactory = Connections.newLoadBalancer(
                                    new FailoverLoadBalancingAlgorithm(asList(primaryCf, secondaryCf)));
                        }

                        ShutdownManager shutdownMan = com.sun.identity.common.ShutdownManager.getInstance();
                        shutdownMan.addShutdownListener(new ShutdownListener() {
                                    public void shutdown() {
                            connFactory.close();
                                    }
                                });

                        connPool = connFactory;
                        connectionPools.put(configName, connPool);
                    }
                }
            }
        } catch (GeneralSecurityException gse) {
            debug.error("Unable to create connection pool", gse);
            throw new LDAPUtilException(gse);
        }

        return connPool;
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
                ResultCode.INVALID_CREDENTIALS, null);
        }

        userId = user;
        userPassword = password;
        //retry just once if connection was closing
        boolean shouldRetry = false;
        do {
           try {
               searchForUser();
               if (screenState == ModuleState.SERVER_DOWN || screenState == ModuleState.USER_NOT_FOUND) {
                       return;
               }
               authenticate();
               shouldRetry = false;
           } catch (LDAPUtilException e) {
               // cases for err=53
               // - disconnect in progress
               // - backend unavailable (read-only, etc)
               // - server locked down
               // - reject unauthenticated requests
               // - low disk space (updates only)
               // - bind with no password (binds only)
               // retrying in case "disconnect in progress"

               // there should be arg to err 53 from LDAPAuthUtils
               if (ResultCode.UNWILLING_TO_PERFORM.equals(e.getResultCode())) {
                     // if the flag is already on, then we've already retried.
                     // not retrying more than once
                     if (!shouldRetry) {
                    	 Object[] errMsg = e.getMessageArgs();
                         debug.error("Retying user authentication due to err("+ResultCode.UNWILLING_TO_PERFORM+") '"+errMsg[0]+"'");
                         // datastore was closing recycled connection. retry.
                         shouldRetry = true;
                     } else {
                         shouldRetry = false;
                         throw e;
                     }
               } else {
                    // generic failure. do not retry
                    throw e;
               }
           }
        } while (shouldRetry);

    }

    /**
     * Returns connection from pool.  Re-authenticate if necessary
     *
     * @return connection that is available to use
     */
    private Connection getConnection() throws ErrorResultException, LDAPUtilException {
        if (cPool == null) {
            cPool = createConnectionPool(connectionPools, null, null);
        }

        return cPool.getConnection();
    }

    /**
     * Get connection from pool.  Re-authenticate if necessary
     * @return connection that is available to use
     */
    private Connection getAdminConnection() throws ErrorResultException, LDAPUtilException {
        if (acPool == null) {
            acPool = createConnectionPool(adminConnectionPools, authDN, authPassword);
        }

        return acPool.getConnection();
    }

    /**
     * Updates to new password by using the parameters passed by the user.
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
            setState(ModuleState.WRONG_PASSWORD_ENTERED);
            return;
        }
        if (!(password.equals(confirmPassword))) {
            setState(ModuleState.PASSWORD_MISMATCH);
            return;
        }
        if (password.equals(userId)) {
            setState(ModuleState.USER_PASSWORD_SAME);
            return;
        }

        Connection modConn = null;
        List<Control> controls;

        try {
            ModifyRequest mods = Requests.newModifyRequest(userDN);

            if (beheraEnabled) {
                mods.addControl(PasswordPolicyRequestControl.newControl(false));
            }

            if (!isAd) {
                mods.addModification(ModificationType.DELETE, LDAP_PASSWD_ATTR, oldPwd);
                mods.addModification(ModificationType.ADD, LDAP_PASSWD_ATTR, password);
                modConn = getConnection();
                modConn.bind(userDN, oldPwd.toCharArray());
            } else {
                mods.addModification(ModificationType.DELETE, AD_PASSWD_ATTR, updateADPassword(oldPwd));
                mods.addModification(ModificationType.ADD, AD_PASSWD_ATTR, updateADPassword(password));
                modConn = getAdminConnection();
            }

            Result modResult = modConn.modify(mods);
            controls = processControls(modResult);

            // Were there any password policy controls returned?
            PasswordPolicyResult result = checkControls(controls);

            if (result == null) {
                if (debug.messageEnabled()) {
                    debug.message("No controls returned");
                }

                setState(ModuleState.PASSWORD_UPDATED_SUCCESSFULLY);
            } else {
                processPasswordPolicyControls(result);
            }
        } catch (ErrorResultException ere) {
            if (ere.getResult().getResultCode().equals(ResultCode.CONSTRAINT_VIOLATION)) {
                PasswordPolicyResult result = checkControls(processControls(ere.getResult()));
                if (result != null) {
                    processPasswordPolicyControls(result);
                } else {
                    if (isAd) {
                        setState(ModuleState.PASSWORD_NOT_UPDATE);
                    } else {
                        setState(ModuleState.INSUFFICIENT_PASSWORD_QUALITY);
                    }
                }
            } else if (ere.getResult().getResultCode().equals(ResultCode.CLIENT_SIDE_CONNECT_ERROR) ||
                ere.getResult().getResultCode().equals(ResultCode.CLIENT_SIDE_SERVER_DOWN) ||
                ere.getResult().getResultCode().equals(ResultCode.UNAVAILABLE) ||
                ere.getResult().getResultCode().equals(ResultCode.CLIENT_SIDE_TIMEOUT)) {
                if (debug.messageEnabled()) {
                    debug.message("changepassword:Cannot connect to " + servers + ": ", ere);
                }

                setState(ModuleState.SERVER_DOWN);
                return;
            } else if (ere.getResult().getResultCode().equals(ResultCode.UNWILLING_TO_PERFORM)) {
                // Were there any password policy controls returned?
                PasswordPolicyResult result = checkControls(processControls(ere.getResult()));

                if (result != null) {
                    processPasswordPolicyControls(result);
                } else {
                    setState(ModuleState.INSUFFICIENT_PASSWORD_QUALITY);
                }
            } else if (ere.getResult().getResultCode().equals(ResultCode.INVALID_CREDENTIALS)) {
                Result r = ere.getResult();

                if (r != null) {
                    // Were there any password policy controls returned?
                    PasswordPolicyResult result = checkControls(processControls(r));

                    if (result != null) {
                        processPasswordPolicyControls(result);
                    }
                }
                setState(ModuleState.PASSWORD_NOT_UPDATE);
            } else {
                setState(ModuleState.PASSWORD_NOT_UPDATE);
            }

            if (debug.warningEnabled()) {
                debug.warning("Cannot update : ", ere);
            }
        } finally {
            if (modConn != null) {
                modConn.close();
            }
        }
    }

    private String buildUserFilter() {
        Filter filter;
        if (userSearchAttrs.size() == 1) {
            filter = Filter.equality(userSearchAttrs.iterator().next(), userId);
        } else {
            List<Filter> searchFilters = new ArrayList<Filter>(userSearchAttrs.size());
            for (String searchAttr : userSearchAttrs) {
                searchFilters.add(Filter.equality(searchAttr, userId));
            }
            filter = Filter.or(searchFilters);
        }

        return filter.toString();
    }

    private ByteString updateADPassword(String password) {
        String quotedPassword = "\"" + password + "\"";

        byte[] newUnicodePassword = quotedPassword.getBytes(Charset.forName("UTF-16LE"));

        return ByteString.wrap(newUnicodePassword);
    }

    /**
     * Searches and returns user for a specified attribute using parameters
     * specified in constructor and/or by setting properties.
     *
     * @throws LDAPUtilException
     */
    public void searchForUser()
    throws LDAPUtilException {
        // make some special case where searchScope == BASE
        // construct the userDN without searching directory
        // assume that there is only one user attribute
        if (searchScope == SearchScope.BASE_OBJECT) {
            if (userSearchAttrs.size() == 1) {
                StringBuilder dnBuffer = new StringBuilder();
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

                if (!isDynamicUserEnabled &&
                    userSearchAttrs.contains(userNamingAttr)) {
                    return;
                } else if (isDynamicUserEnabled &&
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

                searchScope = SearchScope.SINGLE_LEVEL;
            }
        }
        if (searchFilter == null || searchFilter.length() == 0) {
            searchFilter = buildUserFilter();
        } else {
            StringBuilder bindFilter = new StringBuilder(200);

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

        userDN = null;
        Connection conn = null;

        try {
            if (debug.messageEnabled()) {
                debug.message("Connecting to " + servers + "\nSearching " + baseDN + " for " + searchFilter
                        + "\nscope = " + searchScope);
            }

            // Search
            int userAttrSize=0;
            if (attrs == null) {
                if ((userAttributes == null) || (userAttributes.isEmpty())){
                    userAttrSize = 2;
                    attrs = new String[userAttrSize];
                    attrs[0] ="dn";
                    attrs[1] = userNamingAttr;
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

            ConnectionEntryReader results;
            SearchRequest searchForUser = Requests.newSearchRequest(baseDN, searchScope, searchFilter, attrs);

            int userMatches = 0;
            SearchResultEntry entry;
            boolean userNamingValueSet=false;

            try {
                conn = getAdminConnection();
                results = conn.search(searchForUser);
                while (results.hasNext()) {
                    if (results.isEntry()) {
                        entry = results.readEntry();
                        userDN = entry.getName().toString();
                        userMatches++;

                        if (attrs != null && attrs.length > 1) {
                            userNamingValueSet = true;
                            Attribute attr = entry.getAttribute(userNamingAttr);

                            if (attr != null) {
                                userNamingValue = attr.firstValueAsString();
                            }

                            if (isDynamicUserEnabled && (attrs.length > 2)) {
                                for (int i = 2; i < userAttrSize + 2; i++) {
                                    attr = entry.getAttribute(attrs[i]);

                                    if (attr != null) {
                                        Set<String> s = new HashSet<String>();
                                        Iterator<ByteString> values = attr.iterator();

                                        while (values.hasNext()) {
                                            s.add(values.next().toString());
                                        }

                                        userAttributeValues.put(attrs[i], s);
                                    }
                                }
                            }
                        }
                    } else {
                        //read and ignore references
                        results.readReference();
                    }
                }
            } finally {
                if (conn != null) {
                    conn.close();
                }
            }

            if (userNamingValueSet && (userDN == null ||
                userNamingValue == null)) {
                if (debug.messageEnabled()) {
                    debug.message("Cannot find entries for " + searchFilter);
                }

                setState(ModuleState.USER_NOT_FOUND);
                return;
            } else {
                if (userDN == null) {
                    if (debug.messageEnabled()) {
                        debug.message(
                            "Cannot find entries for " + searchFilter);
                    }

                    setState(ModuleState.USER_NOT_FOUND);
                    return;
                } else {
                    setState(ModuleState.USER_FOUND);
                }
            }
            if (userMatches > 1) {
                // multiple user matches found
                debug.error(
                    "searchForUser : Multiple matches found for user '" + userId +
                    "'. Please modify search start DN/filter/scope " +
                    "to make sure unique match returned. Contact your " +
                    "administrator to fix the problem");
                throw new LDAPUtilException("multipleUserMatchFound",
                    (Object[])null);
            }
        } catch (ErrorResultIOException erio) {
            if (debug.warningEnabled()) {
                debug.warning("Search for User error: ", erio);
                debug.warning("resultCode: " + erio.getCause().getResult().getResultCode());
            }

            if (erio.getCause().getResult().getResultCode().equals(ResultCode.CLIENT_SIDE_CONNECT_ERROR) ||
                erio.getCause().getResult().getResultCode().equals(ResultCode.CLIENT_SIDE_SERVER_DOWN) ||
                erio.getCause().getResult().getResultCode().equals(ResultCode.UNAVAILABLE) ||
                erio.getCause().getResult().getResultCode().equals(ResultCode.CLIENT_SIDE_TIMEOUT)) {
                if (debug.warningEnabled()) {
                    debug.warning("Cannot connect to " + servers, erio);
                }
                setState(ModuleState.SERVER_DOWN);
                return;
            }

            throw new LDAPUtilException(erio);
        } catch (SearchResultReferenceIOException srrio) {
            debug.error("Unable to complete search for user: " + userId, srrio);
            throw new LDAPUtilException(srrio);
        } catch (ErrorResultException ere) {
            if (debug.warningEnabled()) {
                debug.warning("Search for User error: ", ere);
                debug.warning("resultCode: " + ere.getResult().getResultCode());
            }

            if (ere.getResult().getResultCode().equals(ResultCode.CLIENT_SIDE_CONNECT_ERROR) ||
                ere.getResult().getResultCode().equals(ResultCode.CLIENT_SIDE_SERVER_DOWN) ||
                ere.getResult().getResultCode().equals(ResultCode.UNAVAILABLE) ||
                ere.getResult().getResultCode().equals(ResultCode.CLIENT_SIDE_TIMEOUT)) {
                if (debug.warningEnabled()) {
                    debug.warning("Cannot connect to " + servers, ere);
                }

                setState(ModuleState.SERVER_DOWN);
            } else if (ere.getResult().getResultCode().equals(ResultCode.INVALID_CREDENTIALS)) {
                if (debug.warningEnabled()) {
                    debug.warning("Cannot authenticate ");
                }

                throw new LDAPUtilException("FConnect",
                    ResultCode.INVALID_CREDENTIALS, null);
            } else if (ere.getResult().getResultCode().equals(ResultCode.UNWILLING_TO_PERFORM)) {
                if (debug.warningEnabled()) {
                    debug.message("Account Inactivated or Locked ");
                }

                throw new LDAPUtilException("FConnect",
                    ResultCode.UNWILLING_TO_PERFORM, null);
            } else if (ere.getResult().getResultCode().equals(ResultCode.NO_SUCH_OBJECT)) {
                throw new LDAPUtilException("noUserMatchFound",
                    ResultCode.NO_SUCH_OBJECT, null);
            } else {
                if (debug.warningEnabled()) {
                    debug.warning("Exception while searching", ere);
                }

                setState(ModuleState.USER_NOT_FOUND);
            }
        }
    }

    /**
     * Connect to LDAP server using parameters specified in
     * constructor and/or by setting properties attempt to authenticate.
     * checks for the password controls and  sets to the appropriate states
     */
    private void authenticate()
    throws LDAPUtilException {
        Connection conn = null;
        List<Control> controls = null;

        try {
            try {
                BindRequest bindRequest =
                        Requests.newSimpleBindRequest(userDN, userPassword.toCharArray());

                if (beheraEnabled) {
                    bindRequest.addControl(PasswordPolicyRequestControl.newControl(false));
                }

                conn = getConnection();
                BindResult bindResult = conn.bind(bindRequest);
                controls = processControls(bindResult);
            } finally {
                if (conn != null) {
                    conn.close();
                }
            }

            // Were there any password policy controls returned?
            PasswordPolicyResult result = checkControls(controls);

            if (result == null) {
                if (debug.messageEnabled()) {
                    debug.message("No controls returned");
                }

                setState(ModuleState.SUCCESS);
            } else {
                processPasswordPolicyControls(result);
            }
        } catch(ErrorResultException ere) {
            if (ere.getResult().getResultCode().equals(ResultCode.INVALID_CREDENTIALS)) {
                if (!isAd) {
                    controls = processControls(ere.getResult());
                    PasswordPolicyResult result = checkControls(controls);

                    if (result != null && result.getPasswordPolicyErrorType() != null &&
                        result.getPasswordPolicyErrorType().equals(PasswordPolicyErrorType.PASSWORD_EXPIRED)) {
                        if (result.getPasswordPolicyWarningType() != null) {
                            //there is a warning about the grace logins, so in
                            //this case the credential was actually wrong
                            throw new LDAPUtilException("CredInvalid",
                                    ResultCode.INVALID_CREDENTIALS, null);
                        } else {
                            if(debug.messageEnabled()) {
                                debug.message("Password expired and must be reset");
                            }
                            setState(ModuleState.PASSWORD_EXPIRED_STATE);
                        }
                    } else if (result != null && result.getPasswordPolicyErrorType() != null &&
                        result.getPasswordPolicyErrorType().equals(PasswordPolicyErrorType.ACCOUNT_LOCKED)) {

                        if (debug.messageEnabled()) {
                            debug.message("Account Locked");
                        }

                        processPasswordPolicyControls(result);
                    } else {
                        if (debug.messageEnabled()) {
                            debug.message("Failed auth due to invalid credentials");
                        }

                        throw new LDAPUtilException("CredInvalid",
                                ResultCode.INVALID_CREDENTIALS, null);
                    }
                } else {
                    PasswordPolicyResult result = checkADResult(ere.getResult().getDiagnosticMessage());

                    if (result != null) {
                        processPasswordPolicyControls(result);
                    } else {
                        if (debug.messageEnabled()) {
                            debug.message("Failed auth due to invalid credentials");
                        }

                        throw new LDAPUtilException("CredInvalid",
                                ResultCode.INVALID_CREDENTIALS, null);
                    }
                }
            } else if (ere.getResult().getResultCode().equals(ResultCode.NO_SUCH_OBJECT)) {
                if (debug.messageEnabled()) {
                    debug.message("user does not exist");
                }

                throw new LDAPUtilException("UsrNotExist",
                    ResultCode.NO_SUCH_OBJECT, null);
            } else if (ere.getResult().getResultCode().equals(ResultCode.CLIENT_SIDE_CONNECT_ERROR) ||
                ere.getResult().getResultCode().equals(ResultCode.CLIENT_SIDE_SERVER_DOWN) ||
                ere.getResult().getResultCode().equals(ResultCode.UNAVAILABLE) ||
                ere.getResult().getResultCode().equals(ResultCode.CLIENT_SIDE_TIMEOUT)) {
                if (debug.messageEnabled()) {
                    debug.message("Cannot connect to " + servers, ere);
                }

                setState(ModuleState.SERVER_DOWN);
            } else if (ere.getResult().getResultCode().equals(ResultCode.UNWILLING_TO_PERFORM)) {
                if (debug.messageEnabled()) {
                    debug.message(servers + " unwilling to perform auth request");
                }
                // cases for err=53
                // - disconnect in progress
                // - backend unavailable (read-only, etc)
                // - server locked down
                // - reject unauthenticated requests
                // - low disk space (updates only)
                // - bind with no password (binds only)
                String[] args = { ere.getMessage() };

                throw new LDAPUtilException("FConnect", ResultCode.UNWILLING_TO_PERFORM, args);
            } else if (ere.getResult().getResultCode().equals(ResultCode.INAPPROPRIATE_AUTHENTICATION)) {
                if (debug.messageEnabled()) {
                    debug.message("Failed auth due to inappropriate authentication");
                }

                throw new LDAPUtilException("amAuth", "InappAuth",
                    ResultCode.INAPPROPRIATE_AUTHENTICATION, null);
            } else if (ere.getResult().getResultCode().equals(ResultCode.CONSTRAINT_VIOLATION)) {
                if (debug.messageEnabled()) {
                    debug.message("Exceed password retry limit.");
                }

                throw new LDAPUtilException(ISAuthConstants.EXCEED_RETRY_LIMIT,
                    ResultCode.CONSTRAINT_VIOLATION, null);
            } else {
                if (debug.messageEnabled()) {
                    debug.message("Cannot authenticate to " + servers, ere);
                }

                throw new LDAPUtilException("amAuth", "FAuth", null, null);
            }
        }
    }

    private List<Control> processControls(Result result) {
        if (result == null) {
            return Collections.EMPTY_LIST;
        }

        List<Control> controls = new ArrayList<Control>();
        DecodeOptions options = new DecodeOptions();
        Control c;

        try {
            c = result.getControl(PasswordExpiredResponseControl.DECODER, options);

            if (c != null) {
                controls.add(c);
            }
        } catch (DecodeException de) {
            if (debug.warningEnabled()) {
                debug.warning("unable to decode PasswordExpiredResponseControl", de);
            }
        }

        try {
            c = result.getControl(PasswordExpiringResponseControl.DECODER, options);

            if (c != null) {
                controls.add(c);
            }
        } catch (DecodeException de) {
            if (debug.warningEnabled()) {
                debug.warning("unable to decode PasswordExpiringResponseControl", de);
            }
        }

        try {
            c = result.getControl(PasswordPolicyResponseControl.DECODER, options);

            if (c != null) {
                controls.add(c);
            }
        } catch (DecodeException de) {
            if (debug.warningEnabled()) {
                debug.warning("unable to decode PasswordPolicyResponseControl", de);
            }
        }

        return controls;
    }

    private void processPasswordPolicyControls(PasswordPolicyResult result) {
        if (result.getPasswordPolicyErrorType() != null) {
            switch (result.getPasswordPolicyErrorType()) {
                case ACCOUNT_LOCKED:
                    if (debug.messageEnabled()) {
                        debug.message("Account is locked" );
                    }

                    setState(ModuleState.ACCOUNT_LOCKED);
                    break;
                case CHANGE_AFTER_RESET:
                    if (debug.messageEnabled()) {
                        debug.message("Password must be changed after reset" );
                    }

                    setState(ModuleState.PASSWORD_RESET_STATE);
                    break;
                case INSUFFICIENT_PASSWORD_QUALITY:
                    if (debug.messageEnabled()) {
                        debug.message("Insufficient password quality" );
                    }

                    setState(ModuleState.INSUFFICIENT_PASSWORD_QUALITY);
                    break;
                case MUST_SUPPLY_OLD_PASSWORD:
                    if (debug.messageEnabled()) {
                        debug.message("Must supply old password" );
                    }

                    setState(ModuleState.MUST_SUPPLY_OLD_PASSWORD);
                    break;
                case PASSWORD_EXPIRED:
                    if (debug.messageEnabled()) {
                        debug.message("Password expired and must be reset" );
                    }

                    setState(ModuleState.PASSWORD_RESET_STATE);
                    break;
                case PASSWORD_IN_HISTORY:
                    if (debug.messageEnabled()) {
                        debug.message("Password in history" );
                    }

                    setState(ModuleState.PASSWORD_IN_HISTORY);
                    break;
                case PASSWORD_MOD_NOT_ALLOWED:
                    if (debug.messageEnabled()) {
                        debug.message("password modification is not allowed" );
                    }

                    setState(ModuleState.PASSWORD_MOD_NOT_ALLOWED);
                    break;
                case PASSWORD_TOO_SHORT:
                    if (debug.messageEnabled()) {
                        debug.message("password too short" );
                    }

                    setState(ModuleState.PASSWORD_TOO_SHORT);
                    break;
                case PASSWORD_TOO_YOUNG:
                    if (debug.messageEnabled()) {
                        debug.message("password too young" );
                    }

                    setState(ModuleState.PASSWORD_TOO_YOUNG);
                    break;
            }
        }

        if (result.getPasswordPolicyWarningType() != null) {
            switch (result.getPasswordPolicyWarningType()) {
                case GRACE_LOGINS_REMAINING:
                    setGraceLogins(result.getValue());

                    if (debug.messageEnabled()) {
                        debug.message("Number of grace logins remaining " + result.getValue());
                    }

                    if (graceLogins != 0) {
                        setState(ModuleState.GRACE_LOGINS);
                    } else {
                        //the password reset would need one more bind, so fail the auth
                        setState(ModuleState.PASSWORD_EXPIRED_STATE);
                    }
                    break;
                case TIME_BEFORE_EXPIRATION:
                    setExpTime(result.getValue());

                    if (debug.messageEnabled()) {
                        debug.message("Password expires in " + result.getValue() + " seconds");
                    }

                    setState(ModuleState.PASSWORD_EXPIRING);
                    break;
            }
        }
    }

    /**
     * checks for  an LDAP v3 server whether the control has returned
     * if a password has expired or password is expiring and password
     * policy is enabled on the server.
     *
     * @return The PasswordPolicyResult or null if there were no controls
     */
    private PasswordPolicyResult checkControls(List<Control> controls) {
        PasswordPolicyResult result = null;

        if ((controls != null) && (!controls.isEmpty())) {
            for (Control control : controls) {
                if (control instanceof PasswordExpiredResponseControl) {
                    if (result == null) {
                        result = new PasswordPolicyResult(PasswordPolicyErrorType.PASSWORD_EXPIRED);
                    } else {
                        result.setPasswordPolicyErrorType(PasswordPolicyErrorType.PASSWORD_EXPIRED);
                    }
                }

                if (control instanceof PasswordPolicyResponseControl) {
                    PasswordPolicyErrorType policyErrorType =
                            ((PasswordPolicyResponseControl) control).getErrorType();

                    if (policyErrorType != null) {
                        switch (policyErrorType) {
                            case ACCOUNT_LOCKED:
                                if (result == null) {
                                    result = new PasswordPolicyResult(PasswordPolicyErrorType.ACCOUNT_LOCKED);
                                } else {
                                    result.setPasswordPolicyErrorType(PasswordPolicyErrorType.ACCOUNT_LOCKED);
                                }

                                break;
                            case CHANGE_AFTER_RESET:
                                if (result == null) {
                                    result = new PasswordPolicyResult(PasswordPolicyErrorType.CHANGE_AFTER_RESET);
                                } else {
                                    result.setPasswordPolicyErrorType(PasswordPolicyErrorType.CHANGE_AFTER_RESET);
                                }

                                break;
                            case INSUFFICIENT_PASSWORD_QUALITY:
                                if (result == null) {
                                    result = new PasswordPolicyResult(PasswordPolicyErrorType.INSUFFICIENT_PASSWORD_QUALITY);
                                } else {
                                    result.setPasswordPolicyErrorType(PasswordPolicyErrorType.INSUFFICIENT_PASSWORD_QUALITY);
                                }

                                break;
                            case MUST_SUPPLY_OLD_PASSWORD:
                                if (result == null) {
                                    result = new PasswordPolicyResult(PasswordPolicyErrorType.MUST_SUPPLY_OLD_PASSWORD);
                                } else {
                                    result.setPasswordPolicyErrorType(PasswordPolicyErrorType.MUST_SUPPLY_OLD_PASSWORD);
                                }

                                break;
                            case PASSWORD_EXPIRED:
                                if (result == null) {
                                    result = new PasswordPolicyResult(PasswordPolicyErrorType.PASSWORD_EXPIRED);
                                } else {
                                    result.setPasswordPolicyErrorType(PasswordPolicyErrorType.PASSWORD_EXPIRED);
                                }

                                break;
                            case PASSWORD_IN_HISTORY:
                                if (result == null) {
                                    result = new PasswordPolicyResult(PasswordPolicyErrorType.PASSWORD_IN_HISTORY);
                                } else {
                                    result.setPasswordPolicyErrorType(PasswordPolicyErrorType.PASSWORD_IN_HISTORY);
                                }

                                break;
                            case PASSWORD_MOD_NOT_ALLOWED:
                                if (result == null) {
                                    result = new PasswordPolicyResult(PasswordPolicyErrorType.PASSWORD_MOD_NOT_ALLOWED);
                                } else {
                                    result.setPasswordPolicyErrorType(PasswordPolicyErrorType.PASSWORD_MOD_NOT_ALLOWED);
                                }

                                break;
                            case PASSWORD_TOO_SHORT:
                                if (result == null) {
                                    result = new PasswordPolicyResult(PasswordPolicyErrorType.PASSWORD_TOO_SHORT);
                                } else {
                                    result.setPasswordPolicyErrorType(PasswordPolicyErrorType.PASSWORD_TOO_SHORT);
                                }

                                break;
                            case PASSWORD_TOO_YOUNG:
                                if (result == null) {
                                    result = new PasswordPolicyResult(PasswordPolicyErrorType.PASSWORD_TOO_YOUNG);
                                } else {
                                    result.setPasswordPolicyErrorType(PasswordPolicyErrorType.PASSWORD_TOO_YOUNG);
                                }

                                break;
                        }
                    }

                    PasswordPolicyWarningType policyWarningType =
                            ((PasswordPolicyResponseControl) control).getWarningType();

                    if (policyWarningType != null) {
                        switch (policyWarningType) {
                            case GRACE_LOGINS_REMAINING:
                                if (result == null) {
                                    result = new PasswordPolicyResult(PasswordPolicyWarningType.GRACE_LOGINS_REMAINING,
                                            ((PasswordPolicyResponseControl) control).getWarningValue());
                                } else {
                                    result.setPasswordPolicyWarningType(PasswordPolicyWarningType.GRACE_LOGINS_REMAINING,
                                            ((PasswordPolicyResponseControl) control).getWarningValue());
                                }

                                break;
                            case TIME_BEFORE_EXPIRATION:
                                if (result == null) {
                                    result = new PasswordPolicyResult(PasswordPolicyWarningType.TIME_BEFORE_EXPIRATION,
                                        ((PasswordPolicyResponseControl) control).getWarningValue());
                                } else {
                                    result.setPasswordPolicyWarningType(PasswordPolicyWarningType.TIME_BEFORE_EXPIRATION,
                                        ((PasswordPolicyResponseControl) control).getWarningValue());
                                }

                                break;
                        }
                    }
                }

                if (control instanceof PasswordExpiringResponseControl) {
                    PasswordExpiringResponseControl expiringControl =
                            (PasswordExpiringResponseControl) control;

                    if (control.hasValue()) {
                        if (result == null) {
                            result = new PasswordPolicyResult(PasswordPolicyWarningType.TIME_BEFORE_EXPIRATION,
                                    expiringControl.getSecondsUntilExpiration());
                        } else {
                            result.setPasswordPolicyWarningType(PasswordPolicyWarningType.TIME_BEFORE_EXPIRATION,
                                    expiringControl.getSecondsUntilExpiration());
                        }
                    } else {
                        if (result == null) {
                            result = new PasswordPolicyResult(PasswordPolicyWarningType.TIME_BEFORE_EXPIRATION,
                                    NO_EXPIRY_TIME);
                        } else {
                            result.setPasswordPolicyWarningType(PasswordPolicyWarningType.TIME_BEFORE_EXPIRATION,
                                    NO_EXPIRY_TIME);
                        }
                    }

                }
            }
        }

        return result;
    }

    /**
     * Given a specific AD diagnostic message, creates a valid PasswordPolicyResult
     *
     * @param diagMessage The message from AD describing the status of the account
     * @return The correct PasswordPolicyResult or null if not matched
     */
    private PasswordPolicyResult checkADResult(String diagMessage) {
        if (diagMessage.contains(AD_PASSWORD_EXPIRED) || diagMessage.contains(S4_PASSWORD_EXPIRED)) {
            return new PasswordPolicyResult(PasswordPolicyErrorType.PASSWORD_EXPIRED);
        } else if (diagMessage.contains(AD_ACCOUNT_DISABLED) || diagMessage.contains(S4_ACCOUNT_DISABLED)) {
            return new PasswordPolicyResult(PasswordPolicyErrorType.ACCOUNT_LOCKED);
        } else if (diagMessage.contains(AD_ACCOUNT_EXPIRED) || diagMessage.contains(S4_ACCOUNT_EXPIRED)) {
            return new PasswordPolicyResult(PasswordPolicyErrorType.ACCOUNT_LOCKED);
        } else if (diagMessage.contains(AD_PASSWORD_RESET) || diagMessage.contains(S4_PASSWORD_RESET)) {
            return new PasswordPolicyResult(PasswordPolicyErrorType.CHANGE_AFTER_RESET);
        } else if (diagMessage.contains(AD_ACCOUNT_LOCKED) || diagMessage.contains(S4_ACCOUNT_LOCKED)) {
            return new PasswordPolicyResult(PasswordPolicyErrorType.ACCOUNT_LOCKED);
        } else {
            return null;
        }
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
    public  void setUserSearchAttribute(Set<String> attr) throws LDAPUtilException {
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
    public void setAuthPassword(char[] authpassword) {
        authPassword = authpassword;
    }

    /**
     * Sets the search scope using an integer.
     *
     * @param scope Search scope (one of <code>SearchScope.BASE_OBJECT</code>,
     *        <code>SearchScope.SUBORDINATES</code>,
     *        <code>SearchScope.SINGLE_LEVEL</code>,
     *        <code>SearchScope.WHOLE_SUBTREE</code>).
     */
    public void setScope(SearchScope scope) {
        searchScope = scope;
    }

    /**
     * Sets if the behera password policy scheme should be used
     *
     * @param beheraEnabled true if behera is supported
     */
    public void setBeheraEnabled(boolean beheraEnabled) {
        this.beheraEnabled = beheraEnabled;
    }

    /**
     * Sets the heartbeat interval.
     *
     * @param heartBeatInterval The heartbeat interval.
     */
    public void setHeartBeatInterval(int heartBeatInterval) {
        this.heartBeatInterval = heartBeatInterval;
    }

    /**
     * Sets the heartbeat timeunit.
     *
     * @param heartBeatTimeUnit The timeunit for the heartbeat interval.
     */
    public void setHeartBeatTimeUnit(String heartBeatTimeUnit) {
        this.heartBeatTimeUnit = heartBeatTimeUnit;
    }
     
    /**
     * Sets the ldap operation timeout.
     *
     * @param timeout The operation timeout in seconds.
     */
    public void setOperationTimeout(int timeout) {
        this.operationsTimeout = timeout;
    }
    
    /**
     * Sets if the directory is Active Directory
     *
     * @param isAd true if the module is AD
     */
    public void setAD(boolean isAd) {
        this.isAd = isAd;
    }

    /**
     * Enabled trust all server certificates
     *
     * @param trustAll true if we should trust all certs
     */
    public void setTrustAll(boolean trustAll) {
        this.trustAll = trustAll;
    }

    /**
     * Returns the latest screen state.
     *
     * @return The latest screen state.
     */
    public ModuleState getState() {
        return screenState;
    }

    /**
     * Sets a screen state for retrieval by a client.
     *
     * @param code Screen state.
     */
    public void setState(ModuleState code) {
        screenState = code;
    }

    private void setExpTime(int sec) {
        expiryTime = null;
        StringBuilder expTime = new StringBuilder();

        int days = sec / (24*60*60);
        int hours = (sec%(24*60*60)) / 3600;
        int minutes = (sec%3600) / 60;
        int seconds = sec%60;

        if (hours <= 0 && minutes <= 0 && seconds <= 0) {
            expTime.append(days).append(" days: ");
            expiryTime = expTime.toString();
        } else {
            expTime.append(days).append(SPACE).append(bundle.getString("days")).append(COLON).append(SPACE);
            expTime.append(hours).append(SPACE).append(bundle.getString("hours")).append(COLON).append(SPACE);
            expTime.append(minutes).append(SPACE).append(bundle.getString("minutes")).append(COLON).append(SPACE);
            expTime.append(seconds).append(SPACE).append(bundle.getString("seconds"));

            expiryTime = expTime.toString();
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

    private void setGraceLogins(int graceLogins) {
        this.graceLogins = graceLogins;
    }

    public int getGraceLogins() {
        return graceLogins;
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
    public Map<String, Set<String>> getUserAttributeValues() {
        return userAttributeValues;
    }

    /**
     * Sets the value of the dynamic profile creation configured in
     * Authentication service.
     *
     * @param isEnable
     */
    public void setDynamicProfileCreationEnabled(boolean isEnable) {
        isDynamicUserEnabled = isEnable;
    }

    /**
     * Sets the return attributes for ldap search.
     *
     * @param attrs Array containing attributes.
     */
    public void setUserAttrs(String[] attrs) {
        this.attrs = attrs;
    }

    class PasswordPolicyResult {
        private PasswordPolicyErrorType errorResultType;
        private PasswordPolicyWarningType warningResultType;
        private int value;

        public PasswordPolicyResult() {
            // do nothing
        }

        public PasswordPolicyResult(PasswordPolicyErrorType errorResultType) {
            this.errorResultType = errorResultType;
        }

        public PasswordPolicyResult(PasswordPolicyWarningType warningResultType) {
            this.warningResultType = warningResultType;
        }

        public PasswordPolicyResult(PasswordPolicyWarningType warningResultType, int value) {
            this.warningResultType = warningResultType;
            this.value = value;
        }

        public PasswordPolicyResult(PasswordPolicyErrorType errorResultType,
                                    PasswordPolicyWarningType warningResultType, int value) {
            this.errorResultType = errorResultType;
            this.warningResultType = warningResultType;
            this.value = value;
        }

        public PasswordPolicyErrorType getPasswordPolicyErrorType() {
            return errorResultType;
        }

        public PasswordPolicyWarningType getPasswordPolicyWarningType() {
            return warningResultType;
        }

        public void setPasswordPolicyErrorType(PasswordPolicyErrorType errorResultType) {
            this.errorResultType = errorResultType;
        }

        public void setPasswordPolicyWarningType(PasswordPolicyWarningType warningResultType, int value) {
            this.warningResultType = warningResultType;
            this.value = value;
        }

       public void setPasswordPolicyWarningType(PasswordPolicyWarningType warningResultType) {
            this.warningResultType = warningResultType;
        }

        public int getValue() {
            return value;
        }
    }
}
