/*
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
 * $Id: Authenticator.java,v 1.9 2008/08/19 19:08:57 veiming Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 * Portions Copyright 2026 3A Systems, LLC.
 */

package com.sun.identity.cli;

import static org.forgerock.openam.utils.Time.*;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.internal.InvalidAuthContextException;
import com.sun.identity.authentication.service.AMAuthErrorCode;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.Constants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.locale.Locale;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;

/**
 * This class is responsible for authentication of a user.
 */
class Authenticator {
    private static final String LOGIN_STATUS = "iplanet-am-user-login-status";
    private static final String ACCOUNT_LIFE = "iplanet-am-user-account-life";
    private static final String STRING_ACTIVE = "active";
    private static final String DEFINED_INDEX_TYPE = "org.forgerock.openam.ssoadm.auth.indexType";
    private static final String DEFINED_INDEX_NAME = "org.forgerock.openam.ssoadm.auth.indexName";
    /** The realm ssoadm authenticates administrators against. */
    private static final String ROOT_REALM = "/";
    private static Set ACTIVE_STATE_ATTRIBUTES = new HashSet(4);
    private static Authenticator instance = new Authenticator();
    static {
        ACTIVE_STATE_ATTRIBUTES.add(LOGIN_STATUS);
        ACTIVE_STATE_ATTRIBUTES.add(ACCOUNT_LIFE);
    }

    private Authenticator() {
    }

    public static Authenticator getInstance() {
        return instance;
    }

    public AuthContext sessionBasedLogin(
        CommandManager mgr,
        String bindUser,
        String bindPwd
    ) throws CLIException {
        String[] param = {bindUser};
        LogWriter.log(mgr, LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_LOGIN", param, null);
        try {
            AuthContext ac = sessionBasedLoginInternal(
                mgr, bindUser, bindPwd);
            LogWriter.log(mgr, LogWriter.LOG_ACCESS, Level.INFO, 
                "SUCCEED_LOGIN", param, null);
            return ac;
        } catch (CLIException e) {
            String[] params = {bindUser, e.getMessage()};
            LogWriter.log(mgr, LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_LOGIN", params, null);
            throw e;
        }
    }

    /**
     * Records that the remote login was refused, before the internal {@code LoginContext} is tried
     * instead. Without it the SUCCEED_LOGIN of the caller is the only record either path leaves, and
     * an audit reader cannot tell a degraded login from the one that was asked for.
     * <p>
     * A failure to write that record must not stand in the way of the fallback. {@code LogWriter}
     * posts the audit event to the server over HTTP and turns any failure into a {@code CLIException},
     * and the server being out of reach is one of the reasons for being here at all - so logging the
     * refusal would otherwise break {@code ssoadm} in exactly the case the fallback exists for.
     */
    private void recordRefusedRemoteLogin(CommandManager mgr, String bindUser, Exception refusal) {
        String reason = refusal.getMessage() == null ? refusal.getClass().getName() : refusal.getMessage();
        String[] params = {bindUser, reason};
        try {
            LogWriter.log(mgr, LogWriter.LOG_ERROR, Level.INFO, "FAILED_LOGIN", params, null);
        } catch (CLIException ce) {
            // The login matters more than the record of it. ldapLoginInternal logs its own failure.
        }
    }

    private AuthContext sessionBasedLoginInternal(
        CommandManager mgr,
        String bindUser,
        String bindPwd
    ) throws CLIException {
        String indexType = SystemProperties.get(DEFINED_INDEX_TYPE);
        String indexName = SystemProperties.get(DEFINED_INDEX_NAME);
        if (indexType != null && indexName != null) {
            return sessionBasedLoginInternal(mgr, bindUser, bindPwd, indexType, indexName);
        }
        /*
         * Authenticate the way the console does: ConsoleServletBase sends
         * service=adminconsoleservice, which AMConfiguration maps onto the realm's Administrator
         * Authentication Configuration rather than treating it as a chain name of its own
         * (AMConfiguration:182-185 -> getOrgBasedConfig(.., isConsole = true), reading
         * iplanet-am-auth-admin-auth-module). So an administrator using ssoadm is authenticated
         * against the same chain as an administrator opening /openam/console.
         *
         * This used to name the DataStore module and fall back to the LDAP module, which a realm
         * with module based authentication disabled refuses: AMLoginContext gates MODULE_INSTANCE
         * (:321-336, exempting Application and modules of type Federation), authlevel (:1286) and
         * MODULE typed composite advice (:1335). None of the three covers SERVICE.
         */
        AuthContext lc = adminChainLogin(mgr);
        if (lc == null) {
            /*
             * The realm names no Administrator Authentication Configuration: the attribute is
             * [Empty] or absent, so AMConfiguration.parseInstanceConfiguration finds no chain
             * behind adminconsoleservice. Fall back to the Organization Authentication
             * Configuration, the chain a browser login to /openam/UI/Login uses.
             *
             * Only the request that starts the login has been sent by then, so the credentials are
             * offered to one chain alone and no failed authentication is recorded against the
             * administrator. What is left behind, on a realm configured this way, is one pair of
             * client side records - "Failed to login to <authservice>" and "Authentication failed."
             * in the tools' Authentication debug - and the server's own note that it could not
             * create a login context for adminconsoleservice. Neither names a user.
             */
            lc = getAuthContext(mgr, null, null);
        }
        return completeSessionBasedLogin(mgr, lc, bindUser, bindPwd);
    }

    /**
     * Starts a login against the Administrator Authentication Configuration of the top level realm.
     *
     * @return the started {@code AuthContext}, or {@code null} where the realm names no such
     *         configuration, so that the caller can fall back to the organization chain.
     */
    private AuthContext adminChainLogin(CommandManager mgr) throws CLIException {
        try {
            AuthContext lc = new AuthContext(ROOT_REALM);
            lc.login(AuthContext.IndexType.SERVICE, ISAuthConstants.CONSOLE_SERVICE);
            /*
             * Observed against a realm whose administrator chain is [Empty]: the refusal arrives as
             * the exception below rather than as a status, because runLogin() takes the response
             * for an old style server and rethrows what runRemoteLogin() had stored. The status is
             * read as well because that is the shape runRemoteLogin() leaves on its own, and which
             * of the two it is depends on the response rather than on anything decided here.
             */
            if (!namesNoAuthConfiguration(lc.getStatus(), lc.getErrorCode())) {
                return lc;
            }
            // The server made an authentication session for the request that started this login.
            // Give it back rather than leaving one to age out on every ssoadm invocation.
            discard(lc);
            return null;
        } catch (AuthLoginException le) {
            if (namesNoAuthConfiguration(AuthContext.Status.FAILED, le.getErrorCode())) {
                return null;
            }
            throw new CLIException(mgr.getResourceBundle().getString("exception-LDAP-login-failed"),
                ExitCodes.SESSION_BASED_LOGIN_FAILED);
        }
    }

    /**
     * Releases an authentication session that is not going to be used. Best effort: this is
     * housekeeping on the way to the organization chain, and failing it is not a reason to refuse
     * an administrator the login that is still to come.
     */
    private void discard(AuthContext lc) {
        try {
            lc.logout();
        } catch (Exception e) {
            // Nothing to do. The session ages out on its own.
        }
    }

    /** Whether the realm names no authentication configuration for what the login asked for. */
    private boolean namesNoAuthConfiguration(AuthContext.Status status, String errorCode) {
        return status == AuthContext.Status.FAILED
            && AMAuthErrorCode.AUTH_CONFIG_NOT_FOUND.equals(errorCode);
    }

    private AuthContext sessionBasedLoginInternal(
        CommandManager mgr,
        String bindUser,
        String bindPwd,
        String indexType,
        String indexName
    ) throws CLIException {
        return completeSessionBasedLogin(mgr, getAuthContext(mgr, indexType, indexName), bindUser, bindPwd);
    }

    private AuthContext completeSessionBasedLogin(
        CommandManager mgr,
        AuthContext lc,
        String bindUser,
        String bindPwd
    ) throws CLIException {

        processCallback(mgr, lc, bindUser, bindPwd);

        try {
            lc.getSSOToken();
        } catch (Exception e) {
            ResourceBundle rb = mgr.getResourceBundle();
            throw new CLIException(
                rb.getString("exception-session-based-login-failed"),
                ExitCodes.SESSION_BASED_LOGIN_FAILED);
        }

        return lc;
    }

    SSOToken ldapLogin(CommandManager mgr, String bindUser, String bindPwd)
        throws CLIException {

        SSOToken ssoToken = null;
        IOutput outputWriter = mgr.getOutputWriter();
        ResourceBundle rb = mgr.getResourceBundle();

        if (mgr.isVerbose()) {
            outputWriter.printlnMessage(
                rb.getString("verbose-authenticating"));
        }
        
        String[] param = {bindUser};
        String installTime = SystemProperties.get(
            AdminTokenAction.AMADMIN_MODE, "false");
        
        if (installTime.equalsIgnoreCase("false")) {
            LogWriter.log(mgr, LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_LOGIN", param, null);
            try {
                AuthContext lc = sessionBasedLoginInternal(
                    mgr, bindUser, bindPwd);
                ssoToken = lc.getSSOToken();
                mgr.registerSSOToken(ssoToken);
            } catch (Exception e) {
                recordRefusedRemoteLogin(mgr, bindUser, e);
                ssoToken = ldapLoginInternal(mgr, bindUser, bindPwd);
            }
        }
        
        /*
         * Even if installTime is false, ssoToken could be null if none of
         * the OpenSSO servers are running.
         * Hence check for ssoToken and use local AuthContext
         */
        if (ssoToken == null) {
            ssoToken = ldapLoginInternal(mgr, bindUser, bindPwd);
        }
        
        LogWriter.log(mgr, LogWriter.LOG_ACCESS, Level.INFO,
            "SUCCEED_LOGIN", param, null);
        if (mgr.isVerbose()) {
            outputWriter.printlnMessage(
                rb.getString("verbose-authenticated"));
        }
    
        return ssoToken;
    }

    private SSOToken ldapLoginInternal(
        CommandManager mgr,
        String bindUser,
        String bindPwd
    ) throws CLIException {
        SSOToken ssoToken = null;
        ResourceBundle rb = mgr.getResourceBundle();

        try {
            com.sun.identity.authentication.internal.AuthContext ac =
                getLDAPAuthContext(bindUser, bindPwd);
            if (ac.getLoginStatus() ==
            com.sun.identity.authentication.internal.AuthContext.AUTH_SUCCESS)
            {
                ssoToken = ac.getSSOToken();
                AMIdentity amid = new AMIdentity(ssoToken, 
                    ssoToken.getPrincipal().getName(), IdType.USER, "/", null);
                ssoToken.setProperty(Constants.UNIVERSAL_IDENTIFIER,
                    amid.getUniversalId());
            } else {
                throw new CLIException(rb.getString(
                    "exception-LDAP-login-failed"),
                    ExitCodes.LDAP_LOGIN_FAILED);
            }
        } catch (LoginException le) {
            String[] params = {bindUser, le.getMessage()};
            LogWriter.log(mgr, LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_LOGIN", params, null);
            throw new CLIException(rb.getString(
                "exception-LDAP-login-failed"), ExitCodes.LDAP_LOGIN_FAILED);
        } catch (SSOException e) {
            String[] params = {bindUser, e.getMessage()};
            LogWriter.log(mgr, LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_LOGIN", params, null);
            throw new CLIException(e, ExitCodes.LDAP_LOGIN_FAILED);
        } catch (InvalidAuthContextException iace) {
            String[] params = {bindUser, iace.getMessage()};
            LogWriter.log(mgr, LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_LOGIN", params, null);
            throw new CLIException(rb.getString(
                "exception-LDAP-login-failed"), ExitCodes.LDAP_LOGIN_FAILED);
        }
        return ssoToken;
    }

    private com.sun.identity.authentication.internal.AuthContext
        getLDAPAuthContext(
        String bindUser,
        String bindPwd
    ) throws LoginException {
        com.sun.identity.authentication.internal.AuthPrincipal principal =
            new com.sun.identity.authentication.internal.AuthPrincipal(
                bindUser);
        com.sun.identity.authentication.internal.AuthContext authContext =
             new com.sun.identity.authentication.internal.AuthContext(
                principal, bindPwd.toCharArray());
        return authContext;
    }

    private AuthContext getAuthContext(CommandManager mgr, String strIndexType, String indexName)
        throws CLIException {
        try {
            AuthContext lc = new AuthContext(ROOT_REALM);
            if (strIndexType == null) {
                // Organization based authentication: the chain the realm itself is configured with.
                lc.login();
                return lc;
            }
            AuthContext.IndexType indexType = null;
            if (strIndexType.equalsIgnoreCase("module_instance")) {
                indexType = AuthContext.IndexType.MODULE_INSTANCE;
            } else if (strIndexType.equalsIgnoreCase("service")) {
                indexType = AuthContext.IndexType.SERVICE;
            } else if (strIndexType.equalsIgnoreCase("user")) {
                indexType = AuthContext.IndexType.USER;
            } else if (strIndexType.equalsIgnoreCase("role")) {
                indexType = AuthContext.IndexType.ROLE;
            } else if (strIndexType.equalsIgnoreCase("level")) {
                indexType = AuthContext.IndexType.LEVEL;
            } else if (strIndexType.equalsIgnoreCase("composite_advice")) {
                indexType = AuthContext.IndexType.COMPOSITE_ADVICE;
            } else if (strIndexType.equalsIgnoreCase("resource")) {
                indexType = AuthContext.IndexType.RESOURCE;
            }
            lc.login(indexType, indexName);
            return lc;
        } catch (LoginException le) {
            ResourceBundle rb = mgr.getResourceBundle();
            throw new CLIException(rb.getString("exception-LDAP-login-failed"),
                ExitCodes.SESSION_BASED_LOGIN_FAILED);
        }
    }
    
    private void processCallback(
        CommandManager mgr,
        AuthContext lc,
        String bindUser,
        String bindPwd
    ) throws CLIException {

        ResourceBundle rb = mgr.getResourceBundle();
        while (lc.hasMoreRequirements()) {
            Callback[] callbacks =  lc.getRequirements();
            
            if (callbacks != null) {
                setCallbackValues(callbacks, bindUser, bindPwd);
                lc.submitRequirements(callbacks);
            }
        }
        
        if (lc.getStatus() != AuthContext.Status.SUCCESS) {
            throw new CLIException(rb.getString("exception-LDAP-login-failed"),
                ExitCodes.SESSION_BASED_LOGIN_FAILED);
        }
    }
    
    private void setCallbackValues(
        Callback[] callbacks,
        String bindUser,
        String bindPwd
    ) {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof NameCallback) {
                NameCallback nc = (NameCallback)callbacks[i];
                nc.setName(bindUser);
            } else if (callbacks[i] instanceof PasswordCallback) {
                PasswordCallback pc = (PasswordCallback) callbacks[i];
                char pswd[] = bindPwd.toCharArray();
                pc.setPassword(pswd);
            }
        }
    }

    private static String getStringValue(Map map, String key) {
        String strValue = null;
        if ((map != null) && !map.isEmpty()) {
            Set set = (Set)map.get(key);
            if ((set != null) && !set.isEmpty()) {
                strValue = (String)set.iterator().next();
            }
        }
        return strValue;
    }

    private static boolean isExpired(String strDate) {
        boolean expired = false;
        if ((strDate != null) && (strDate.trim().length() > 0)) {
            Date exprDate = Locale.parseNormalizedDateString(strDate);
            expired = exprDate.before(newDate());
        }
        return expired;
    }
}
