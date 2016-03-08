/*
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
 * $Id: LDAP.java,v 1.17 2010/01/25 22:09:16 qcheng Exp $
 *
 * Portions Copyrighted 2010-2016 ForgeRock AS.
 */

package com.sun.identity.authentication.modules.ldap;

import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.authentication.spi.AMAuthCallBackImpl;
import com.sun.identity.authentication.spi.AMAuthCallBackException;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.authentication.spi.UserNamePasswordValidationException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import java.security.Principal;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import org.forgerock.openam.ldap.LDAPAuthUtils;
import org.forgerock.openam.ldap.LDAPUtilException;
import org.forgerock.openam.ldap.ModuleState;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.SearchScope;

public class LDAP extends AMLoginModule {
    // static variables
    private static final String USER_CREATION_ATTR =
        "iplanet-am-ldap-user-creation-attr-list";
    private static final String INVALID_CHARS =
        "iplanet-am-auth-ldap-invalid-chars";
    private static final String PIPE_SEPARATOR="|";
    private static final String AM_AUTH = "amAuth";
    private boolean sslTrustAll = false;
    private boolean isSecure = false;
    private boolean useStartTLS = false;
    
    private static final String OPERATION_TIMEOUT_ATTR = "openam-auth-ldap-operation-timeout";

    // local variables
    ResourceBundle bundle = null;
    protected String validatedUserID;
    private String userName;
    private String userPassword;
    private int requiredPasswordLength = 0;
    private String regEx;
    private String currentConfigName;
    private String bindDN;
    private String protocolVersion;
    private int currentState;
    protected LDAPAuthUtils ldapUtil;
    private boolean isReset;
    private boolean isProfileCreationEnabled;
    private boolean getCredentialsFromSharedState;

    private AMAuthCallBackImpl callbackImpl = null;

    private Set userCreationAttrs = new HashSet();
    private HashMap userAttrMap = new HashMap();
    private Map sharedState;
    public Map currentConfig;

    protected Debug debug = null;
    protected String amAuthLDAP;
    protected Principal userPrincipal;

    enum LoginScreen {
        LOGIN_START(1, "loginState"),
        PASSWORD_CHANGE(2, "passwordChange"),
        PASSWORD_EXPIRED_SCREEN(3, "passwordExpired"),
        USER_INACTIVE(4, "userInactive"),
        ACCOUNT_LOCKED(5, "accountLocked");

        private static final Map<Integer,LoginScreen> lookup =
                new HashMap<Integer,LoginScreen>();

        static {
            for(LoginScreen ls : EnumSet.allOf(LoginScreen.class)) {
                lookup.put(ls.intValue(), ls);
            }
        }

        private final int state;
        private final String name;

        private LoginScreen(final int state, final String name) {
            this.state = state;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public static LoginScreen get(int screen) {
            return lookup.get(screen);
        }

        int intValue() {
            return state;
        }
    }

    /**
     * TODO-JAVADOC
     */
    public LDAP() {
        amAuthLDAP = "amAuthLDAP";
        debug = Debug.getInstance(amAuthLDAP);
    }

    /**
     * TODO-JAVADOC
     */
    public void init(Subject subject, Map sharedState, Map options) {
        currentConfig = options;
        currentConfigName = (String) options.get(ISAuthConstants.MODULE_INSTANCE_NAME);
        Locale locale = getLoginLocale();
        bundle = amCache.getResBundle(amAuthLDAP, locale);
        if (debug.messageEnabled()) {
            debug.message("LDAP resbundle locale=" + locale);
        }
        this.sharedState = sharedState;
    }

    /**
     * TODO-JAVADOC
     */
    public boolean initializeLDAP() throws AuthLoginException {
        debug.message("LDAP initialize()");

        try {
            Set<String> primaryServers =
                    CollectionHelper.getServerMapAttrs(currentConfig, "iplanet-am-auth-ldap-server");
            Set<String> secondaryServers =
                    CollectionHelper.getServerMapAttrs(currentConfig, "iplanet-am-auth-ldap-server2");

            String baseDN = CollectionHelper.getServerMapAttr(
                currentConfig, "iplanet-am-auth-ldap-base-dn");
            if (baseDN == null) {
                debug.error("BaseDN for search was null");
            }

            String pLen = CollectionHelper.getMapAttr(currentConfig,
                "iplanet-am-auth-ldap-min-password-length");
            if (pLen != null) {
                try {
                    requiredPasswordLength = Integer.parseInt(pLen);
                } catch (NumberFormatException ex) {
                    debug.error("LDAP.initializeLDAP : " + pLen, ex);
                }
            }
            bindDN = CollectionHelper.getMapAttr(currentConfig,
                "iplanet-am-auth-ldap-bind-dn", "");
            char[] bindPassword = CollectionHelper.getMapAttr(
                currentConfig, "iplanet-am-auth-ldap-bind-passwd", "").toCharArray();
            String userNamingAttr = CollectionHelper.getMapAttr(
                currentConfig,
                "iplanet-am-auth-ldap-user-naming-attribute", "uid");
            Set userSearchAttrs = (Set)currentConfig.get(
                "iplanet-am-auth-ldap-user-search-attributes");
            String searchFilter = CollectionHelper.getMapAttr(
                currentConfig, "iplanet-am-auth-ldap-search-filter", "");

            final String connectionMode = CollectionHelper.getMapAttr(
                currentConfig, "openam-auth-ldap-connection-mode", "LDAP");
            useStartTLS = connectionMode.equalsIgnoreCase("StartTLS");
            isSecure = connectionMode.equalsIgnoreCase("LDAPS") || useStartTLS;
            protocolVersion = CollectionHelper.getMapAttr(
                    currentConfig, "openam-auth-ldap-secure-protocol-version", "TLSv1");

            getUserCreationAttrs(currentConfig);
            String tmp = CollectionHelper.getMapAttr(currentConfig,
                "iplanet-am-auth-ldap-search-scope", "SUBTREE");

            String authLevel = CollectionHelper.getMapAttr(currentConfig,
                "iplanet-am-auth-ldap-auth-level");
            if (authLevel != null) {
                try {
                    setAuthLevel(Integer.parseInt(authLevel));
                } catch (Exception e) {
                    debug.error("Unable to set auth level " + authLevel);
                }
            }

            SearchScope searchScope = SearchScope.WHOLE_SUBTREE;

            if (tmp.equalsIgnoreCase("OBJECT")) {
                searchScope = SearchScope.BASE_OBJECT;
            } else if (tmp.equalsIgnoreCase("ONELEVEL")) {
                searchScope = SearchScope.SINGLE_LEVEL;
            }

            String returnUserDN = CollectionHelper.getMapAttr(
                currentConfig, ISAuthConstants.LDAP_RETURNUSERDN, "true");
            regEx = CollectionHelper.getMapAttr(
                currentConfig, INVALID_CHARS);

            boolean beheraEnabled = Boolean.valueOf(CollectionHelper.getMapAttr(
                currentConfig, "iplanet-am-auth-ldap-behera-password-policy-enabled", "false")
                ).booleanValue();

            sslTrustAll = Boolean.valueOf(CollectionHelper.getMapAttr(
                currentConfig, "iplanet-am-auth-ldap-ssl-trust-all", "false")
                ).booleanValue();
            int heartBeatInterval = CollectionHelper.getIntMapAttr(currentConfig,
                    "openam-auth-ldap-heartbeat-interval", 10, debug);
            String heartBeatTimeUnit = CollectionHelper.getMapAttr(currentConfig,
                    "openam-auth-ldap-heartbeat-timeunit", "SECONDS");
            
            final int operationTimeout = CollectionHelper.getIntMapAttr(currentConfig, OPERATION_TIMEOUT_ATTR , 0 , debug);

            isProfileCreationEnabled = isDynamicProfileCreationEnabled();
            // set the optional attributes here
            ldapUtil = new LDAPAuthUtils(primaryServers, secondaryServers, isSecure, bundle, baseDN, debug);
            ldapUtil.setScope(searchScope);
            ldapUtil.setFilter(searchFilter);
            ldapUtil.setUserNamingAttribute(userNamingAttr);
            ldapUtil.setUserSearchAttribute(userSearchAttrs);
            ldapUtil.setAuthPassword(bindPassword);
            ldapUtil.setAuthDN(bindDN);
            ldapUtil.setReturnUserDN(returnUserDN);
            ldapUtil.setUserAttributes(userCreationAttrs);
            ldapUtil.setTrustAll(sslTrustAll);
            ldapUtil.setUseStartTLS(useStartTLS);
            ldapUtil.setDynamicProfileCreationEnabled(
                isProfileCreationEnabled);
            ldapUtil.setBeheraEnabled(beheraEnabled);
            ldapUtil.setHeartBeatInterval(heartBeatInterval);
            ldapUtil.setHeartBeatTimeUnit(heartBeatTimeUnit);
            ldapUtil.setOperationTimeout(operationTimeout);
            ldapUtil.setProtocolVersion(protocolVersion);

            if (debug.messageEnabled()) {
                debug.message("bindDN-> " + bindDN
                        + "\nrequiredPasswordLength-> " + requiredPasswordLength
                        + "\nbaseDN-> " + baseDN
                        + "\nuserNamingAttr-> " + userNamingAttr
                        + "\nuserSearchAttr(s)-> " + userSearchAttrs
                        + "\nuserCreationAttrs-> " + userCreationAttrs
                        + "\nsearchFilter-> " + searchFilter
                        + "\nsearchScope-> " + searchScope
                        + "\nisSecure-> " + isSecure
                        + "\nuseStartTLS-> " + useStartTLS
                        + "\ntrustAll-> " + sslTrustAll
                        + "\nauthLevel-> " + authLevel
                        + "\nbeheraEnabled->" + beheraEnabled
                        + "\nprimaryServers-> " + primaryServers
                        + "\nsecondaryServers-> " + secondaryServers
                        + "\nheartBeatInterval-> " + heartBeatInterval
                        + "\nheartBeatTimeUnit-> " + heartBeatTimeUnit
                        + "\noperationTimeout-> " + operationTimeout
                        + "\nPattern : " + regEx);
            }
            return true;
        } catch (Exception ex) {
            debug.error("Init Exception", ex);
            throw new AuthLoginException(AM_AUTH, "LDAPex", null, ex);
        }
    }

    public int process(Callback[] callbacks, int state)
            throws AuthLoginException {
        currentState = state;
        ModuleState newState;
        LoginScreen loginScreen = LoginScreen.get(state);

        try {

            if (loginScreen.equals(LoginScreen.LOGIN_START)) {
                if (callbacks == null || callbacks.length == 0) {
                    userName = (String) sharedState.get(getUserKey());
                    userPassword = (String) sharedState.get(getPwdKey());

                    if (userName == null || userPassword == null) {
                        return LoginScreen.LOGIN_START.intValue();
                    }

                    getCredentialsFromSharedState = true;
                } else {
                    //callbacks is not null
                    userName = ( (NameCallback) callbacks[0]).getName();
                    userPassword = charToString(((PasswordCallback)
                    callbacks[1]).getPassword(), callbacks[1]);
                }

                if (userPassword == null || userPassword.length() == 0) {
                    if (debug.messageEnabled()) {
                        debug.message("LDAP.process: Password is null/empty");
                    }

                    throw new InvalidPasswordException("amAuth",
                            "invalidPasswd", null);
                }

                //store username password both in success and failure case
                storeUsernamePasswd(userName, userPassword);

                if (initializeLDAP()) {
                    //validate username
                    validateUserName(userName, regEx);
                    ldapUtil.authenticateUser(userName, userPassword);
                    newState = ldapUtil.getState();
                } else {
                    newState = ModuleState.SERVER_DOWN;
                }

                boolean passwordValidationSuccessFlag = true;
                // Validating Password only if authentication
                // information entered is correct
                if (newState == ModuleState.SUCCESS) {
                    try {
                        validatePassword(userPassword);
                    } catch (UserNamePasswordValidationException upve) {
                        if (debug.messageEnabled()) {
                            debug.message("Password does not satisfy " +
                                          "password policy rules specified"
                                          + " in OpenAM");
                        }
                        isReset = true;
                        String invalidMsg = bundle.getString(
                                               "PasswordInvalid");
                        replaceHeader(LoginScreen.PASSWORD_CHANGE.intValue(), invalidMsg);
                        currentState = LoginScreen.PASSWORD_CHANGE.intValue();
                        passwordValidationSuccessFlag = false;
                    }
                }

                if (passwordValidationSuccessFlag) {
                    processLoginScreen(newState);
                }

                return currentState;
            } else if (loginScreen.equals(LoginScreen.PASSWORD_CHANGE)) {
                if (debug.messageEnabled()) {
                    debug.message("you are in Password Screen:" + currentState);
                }
                // callbacks[3] is a user selected button index
                // PwdAction == 0 is a Submit button
                int pwdAction =
                    ((ConfirmationCallback)callbacks[3]).getSelectedIndex();
                if (pwdAction == 0) {
                    String oldPassword = charToString(((PasswordCallback)
                    callbacks[0]).getPassword(), callbacks[0]);
                    String newPassword = charToString(((PasswordCallback)
                    callbacks[1]).getPassword(), callbacks[1]);
                    String confirmPassword = charToString(((PasswordCallback)
                    callbacks[2]).getPassword(), callbacks[2]);

                    try {
                        validatePassword(newPassword);
                        // check minimal password length requirement
                        int newPasswordLength = 0;
                        if (newPassword != null) {
                            newPasswordLength = newPassword.length();
                        }
                        if (newPasswordLength < requiredPasswordLength) {
                            if (debug.messageEnabled()) {
                                debug.message("LDAP.process: new password less"
                                    + " than the minimal length of "
                                    + requiredPasswordLength);
                            }
                            newState = ModuleState.PASSWORD_MIN_CHARACTERS;
                            // add log
                            getLoginState("LDAP").logFailed(newState.name(),
                                "CHANGE_USER_PASSWORD_FAILED", false, null);
                        } else {
                            ldapUtil.changePassword(oldPassword, newPassword,
                                confirmPassword);
                            newState = ldapUtil.getState();

                            if (newState ==
                                ModuleState.PASSWORD_UPDATED_SUCCESSFULLY){
                                // log change password success
                                getLoginState("LDAP").logSuccess(
                                    "changePasswdSucceeded",
                                    "CHANGE_USER_PASSWORD_SUCCEEDED");
                            } else {
                                // add log
                                getLoginState("LDAP").logFailed(newState.name(),
                                    "CHANGE_USER_PASSWORD_FAILED", false, null);
                            }
                        }
                        processPasswordScreen(newState);

                        if (debug.messageEnabled()) {
                            debug.message("Password change state :" + newState);
                        }
                    } catch(UserNamePasswordValidationException upve) {
                        if (debug.messageEnabled()) {
                            debug.message("Password could not be validated, " +
                                          "need a different password");
                        }
                        String invalidMsg = bundle.getString(
                                                "NewPasswordInvalid");
                        replaceHeader(LoginScreen.PASSWORD_CHANGE.intValue(), invalidMsg);
                        currentState = LoginScreen.PASSWORD_CHANGE.intValue();
                    }

                    return currentState;
                } else  {
                    if (isReset) {
                        isReset = false;
                        return LoginScreen.LOGIN_START.intValue();
                    }
                    validatedUserID = ldapUtil.getUserId();
                    return ISAuthConstants.LOGIN_SUCCEED;
                }
            } else {
                setFailureID(ldapUtil.getUserId(userName));
                throw new AuthLoginException(AM_AUTH, "LDAPex", null);
            }
        } catch (LDAPUtilException ex) {
            if (getCredentialsFromSharedState && !isUseFirstPassEnabled()) {
                getCredentialsFromSharedState = false;
                return LoginScreen.LOGIN_START.intValue();
            }
            setFailureID((ldapUtil != null) ?
                ldapUtil.getUserId(userName) : userName);

            if (ex.getResultCode().equals(ResultCode.NO_SUCH_OBJECT)) {
                if (debug.messageEnabled()) {
                    debug.message("The specified user does not exist.");
                }

                throw new AuthLoginException(AM_AUTH, "NoUser", null);
            } else if (ex.getResultCode().equals(ResultCode.INVALID_CREDENTIALS)) {
                if (debug.messageEnabled()) {
                    debug.message("Invalid password.");
                }

                String failureUserID = ldapUtil.getUserId();
                throw new InvalidPasswordException(AM_AUTH, "InvalidUP",
                    null, failureUserID, null);
            } else if (ex.getResultCode().equals(ResultCode.UNWILLING_TO_PERFORM)) {
                if (debug.messageEnabled()) {
                    debug.message("Unwilling to perform. Account inactivated.");
                }

                currentState = LoginScreen.USER_INACTIVE.intValue();
                return currentState;
            } else if (ex.getResultCode().equals(ResultCode.INAPPROPRIATE_AUTHENTICATION)) {
                if (debug.messageEnabled()) {
                    debug.message("Inappropriate authentication.");
                }

                throw new AuthLoginException(AM_AUTH, "InappAuth", null);
            } else if (ex.getResultCode().equals(ResultCode.CONSTRAINT_VIOLATION)) {
                if (debug.messageEnabled()) {
                    debug.message("Exceed password retry limit.");
                }

                throw new AuthLoginException(amAuthLDAP,
                        ISAuthConstants.EXCEED_RETRY_LIMIT, null);
            } else {
                throw new AuthLoginException(AM_AUTH, "LDAPex", null);
            }
        } catch (UserNamePasswordValidationException upve) {
            // Note: Do not set failure Id for this exception
            if (debug.messageEnabled()) {
                debug.message("Invalid Characters detected");
            }

            throw new AuthLoginException(upve);
        }
    }

    /**
     * Returns principal.
     *
     * @return principal.
     */
    public Principal getPrincipal() {
        if (userPrincipal != null) {
            return userPrincipal;
        } else if (validatedUserID != null) {
            userPrincipal = new LDAPPrincipal(validatedUserID);
            return userPrincipal;
        } else {
            return null;
        }
    }

    /**
     * Cleans up state fields.
     */
    public void destroyModuleState() {
        validatedUserID = null;
        userPrincipal = null;
    }

    /**
     * TODO-JAVADOC
     */
    public void nullifyUsedVars() {
        bundle = null;
        userName = null ;
        userPassword = null;
        regEx = null;
        userCreationAttrs = null;
        userAttrMap = null;
        sharedState = null;
        currentConfig = null;

        amAuthLDAP = null;
    }

    private void processLoginScreen(ModuleState newState) throws AuthLoginException {
        try {
            switch (newState) {
                case SUCCESS:
                    validatedUserID = ldapUtil.getUserId();
                    createProfile();
                    currentState = ISAuthConstants.LOGIN_SUCCEED;
                    setForceCallbacksRead(false);
                    break;
                case PASSWORD_EXPIRING:
                {
                    String fmtMsg = bundle.getString("PasswordExp");
                    String msg = com.sun.identity.shared.locale.Locale.
                        formatMessage(fmtMsg, ldapUtil.getExpTime());
                    /**
                     * In case of sharedstate if the chain breaks in ldap
                     * because of abnormal condition like pwd expiring
                     * then the callbacks has to be read fresh so that new
                     * screen appears for the user.
                     */
                    setForceCallbacksRead(true);
                    forceCallbacksInit();
                    replaceHeader(LoginScreen.PASSWORD_CHANGE.intValue(), msg);
                }
                    currentState = LoginScreen.PASSWORD_CHANGE.intValue();
                    break;
                case PASSWORD_RESET_STATE:
                case CHANGE_AFTER_RESET:
                    isReset = true;
                    String resetMsg = bundle.getString("PasswordReset");
                    /**
                     * In case of sharedstate if the chain breaks in ldap
                     * because of abnormal condition like pwd reset
                     * then the callbacks has to be read fresh so that new
                     * screen appears for the user.
                     */
                    setForceCallbacksRead(true);
                    forceCallbacksInit();
                    replaceHeader(LoginScreen.PASSWORD_CHANGE.intValue(), resetMsg);
                    currentState = LoginScreen.PASSWORD_CHANGE.intValue();
                    break;
                case PASSWORD_EXPIRED_STATE:
                    setFailureID(ldapUtil.getUserId(userName));
                    currentState = LoginScreen.PASSWORD_EXPIRED_SCREEN.intValue();
                    break;
                case ACCOUNT_LOCKED:
                    setFailureID(ldapUtil.getUserId(userName));
                    currentState = LoginScreen.ACCOUNT_LOCKED.intValue();
                    break;
                case GRACE_LOGINS:
                {
                    String fmtMsg = bundle.getString("GraceLogins");
                    String msg = com.sun.identity.shared.locale.Locale.
                        formatMessage(fmtMsg, ldapUtil.getGraceLogins());

                    setForceCallbacksRead(true);
                    forceCallbacksInit();
                    if (ldapUtil.getGraceLogins() == 1) {
                        Callback[] callback = getCallback(LoginScreen.PASSWORD_CHANGE.intValue());
                        for (int i = 0; i < callback.length; i++) {
                            Callback cbk = callback[i];
                            if (cbk instanceof ConfirmationCallback) {
                                ConfirmationCallback confirm = (ConfirmationCallback) cbk;
                                String[] options = confirm.getOptions();
                                String[] newOptions = new String[1];
                                System.arraycopy(options, 0, newOptions, 0, 1);
                                ConfirmationCallback newConfirm =
                                        new ConfirmationCallback(confirm.getMessageType(), newOptions, confirm.getDefaultOption());
                                replaceCallback(LoginScreen.PASSWORD_CHANGE.intValue(), i, newConfirm);
                            }
                        }
                    }

                    replaceHeader(LoginScreen.PASSWORD_CHANGE.intValue(), msg);
                }
                    currentState = LoginScreen.PASSWORD_CHANGE.intValue();
                    break;
                case TIME_BEFORE_EXPIRATION:
                {
                    String fmtMsg = bundle.getString("TimeBeforeExpiration");
                    String msg = com.sun.identity.shared.locale.Locale.
                        formatMessage(fmtMsg, ldapUtil.getExpTime());

                    setForceCallbacksRead(true);
                    forceCallbacksInit();
                    replaceHeader(LoginScreen.PASSWORD_CHANGE.intValue(), msg);
                }
                    currentState = LoginScreen.PASSWORD_CHANGE.intValue();
                case USER_NOT_FOUND:
                    throw new LDAPUtilException("noUserMatchFound", (Object[])null);
                case SERVER_DOWN:
                    throw new AuthLoginException(AM_AUTH, "LDAPex", null);
                default:
            }
        } catch (LDAPUtilException ex) {
            if (getCredentialsFromSharedState && !isUseFirstPassEnabled()) {
                getCredentialsFromSharedState = false;
                currentState = LoginScreen.LOGIN_START.intValue();
                return;
            }
            if (newState != ModuleState.USER_NOT_FOUND) {
                debug.error("Unknown Login State:", ex);
            }
            throw new AuthLoginException(AM_AUTH, "LDAPex", null, ex);
        }
    }

    private void processPasswordScreen(ModuleState newState)
            throws AuthLoginException {
        switch (newState) {
            case PASSWORD_UPDATED_SUCCESSFULLY:
                validatedUserID = ldapUtil.getUserId();
                createProfile();
                currentState = ISAuthConstants.LOGIN_SUCCEED;
                // Instantiating the callback implementation variable. This
                // will be used to notify the plug-in classes that a
                // successful password change was performed.
                try {
                    callbackImpl =
                        AMAuthCallBackImpl.getInstance(this.getRequestOrg());
                    // We need the current system time since this is required
                    // as part of the callback method parameter.
                    Long now = new Long(currentTimeMillis());
                    // We now notify the plug-in that a successful
                    // password change was performed.
                    callbackImpl.processedPasswordChange(now, validatedUserID);
                } catch (AMAuthCallBackException acbe) {
                    if (debug.errorEnabled()) {
                        debug.error("process : unable to get " +
                        "AMAuthCallBackImpl instance or callback module " +
                        "raised an exception.", acbe);
                    }
                }
                break;
            case PASSWORD_NOT_UPDATE:
                replaceHeader(LoginScreen.PASSWORD_CHANGE.intValue(), bundle.getString("PInvalid"));
                currentState = LoginScreen.PASSWORD_CHANGE.intValue();
                break;
            case PASSWORD_MISMATCH:
                replaceHeader(LoginScreen.PASSWORD_CHANGE.intValue(), bundle.getString("PasswdMismatch"));
                currentState = LoginScreen.PASSWORD_CHANGE.intValue();
                break;
            case WRONG_PASSWORD_ENTERED:
                replaceHeader(LoginScreen.PASSWORD_CHANGE.intValue(), bundle.getString("PasswdSame"));
                currentState = LoginScreen.PASSWORD_CHANGE.intValue();
                break;
            case PASSWORD_MIN_CHARACTERS:
                replaceHeader(LoginScreen.PASSWORD_CHANGE.intValue(), bundle.getString("PasswdMinChars"));
                currentState = LoginScreen.PASSWORD_CHANGE.intValue();
                break;
            case USER_PASSWORD_SAME:
                replaceHeader(LoginScreen.PASSWORD_CHANGE.intValue(), bundle.getString("UPsame"));
                currentState = LoginScreen.PASSWORD_CHANGE.intValue();
                break;
            case INSUFFICIENT_PASSWORD_QUALITY:
                replaceHeader(LoginScreen.PASSWORD_CHANGE.intValue(), bundle.getString("inPwdQual"));
                currentState = LoginScreen.PASSWORD_CHANGE.intValue();
                break;
            case PASSWORD_IN_HISTORY:
                replaceHeader(LoginScreen.PASSWORD_CHANGE.intValue(), bundle.getString("pwdInHist"));
                currentState = LoginScreen.PASSWORD_CHANGE.intValue();
                break;
            case PASSWORD_TOO_SHORT:
                replaceHeader(LoginScreen.PASSWORD_CHANGE.intValue(), bundle.getString("pwdToShort"));
                currentState = LoginScreen.PASSWORD_CHANGE.intValue();
                break;
            case PASSWORD_TOO_YOUNG:
                replaceHeader(LoginScreen.PASSWORD_CHANGE.intValue(), bundle.getString("pwdToYoung"));
                currentState = LoginScreen.PASSWORD_CHANGE.intValue();
                break;
            default:

        }
    }

    private void createProfile() {
        if (isProfileCreationEnabled && userCreationAttrs.size() > 0) {
            Map userAttributeValues = ldapUtil.getUserAttributeValues();

            if (debug.messageEnabled()) {
                debug.message("user creation attributes: " +
                        userAttributeValues);
            }

            Map userValues= getAttributeMap(userAttributeValues);
            setUserAttributes(userValues);
        }
    }

    private String charToString(char[] tmpPassword, Callback cbk) {
        if (tmpPassword == null) {
            // treat a NULL password as an empty password
            tmpPassword = new char[0];
        }
        char[] pwd = new char[tmpPassword.length];
        System.arraycopy(tmpPassword, 0, pwd, 0, tmpPassword.length);
        ((PasswordCallback) cbk).clearPassword();
        return new String(pwd);
    }

    /**
     * Retrieves the user creation attribute list from the
     * ldap configuration. The format of each line in the attribute
     * list is localAttribute:externalAttribute , this indicates the
     * the mapping of the local attribute in local iDS to the external
     * attribute in remote iDS.
     * This method parses each line in the list to separate the local
     * attribute and external attribute and creates a set of
     * external attributes and a Map with key as the internal
     * attribute and value the external attribute.
     */
    private void getUserCreationAttrs(Map currentConfig) {
        Set attrs = (Set)currentConfig.get(USER_CREATION_ATTR);

        if (debug.messageEnabled()) {
            debug.message("attrs is : " + attrs);
        }

        if ((attrs != null) && (!attrs.isEmpty())) {
            Iterator attrIterator = attrs.iterator();

            while (attrIterator.hasNext()) {
                String userAttr = (String) attrIterator.next();
                int i = userAttr.indexOf(PIPE_SEPARATOR);

                if (i != -1) {
                    String localAttr =  userAttr.substring(0,i);
                    String extAttr = userAttr.substring(i+1,userAttr.length());

                    if ( (extAttr == null)  || (extAttr.length() == 0)) {
                        userCreationAttrs.add(localAttr);
                        userAttrMap.put(localAttr,localAttr);
                    } else {
                        userCreationAttrs.add(extAttr);
                        userAttrMap.put(localAttr,extAttr);
                    }
                } else {
                    userCreationAttrs.add(userAttr);
                    userAttrMap.put(userAttr,userAttr);
                }
            }
        }
    }

    /**
     * this method retrieves the key which is the external
     * attribute from the attributeValues and maps it
     * to the local attribute. A new map with localAttribute
     * as the key and value is the value of the attribute in
     * external iDS is created.
     */
    private Map getAttributeMap(Map attributeValues) {
        if (debug.messageEnabled()) {
            debug.message("In getAttribute Map: " + attributeValues);
        }

        Map newAttrMap = new HashMap();
        Iterator userIterator = userAttrMap.keySet().iterator();

        while (userIterator.hasNext()) {
            String key = (String) userIterator.next();
            String value = (String) userAttrMap.get(key);
            Set newValue = (Set) attributeValues.get(value);

            if (debug.messageEnabled()) {
                debug.message("key is : " + key);
                debug.message("value is : " + newValue);
            }

            if (newValue != null) {
                newAttrMap.put(key,newValue);
            }
        }

        if (debug.messageEnabled()) {
            debug.message("New attr map is : " + newAttrMap);
        }

        return newAttrMap;
    }
}
