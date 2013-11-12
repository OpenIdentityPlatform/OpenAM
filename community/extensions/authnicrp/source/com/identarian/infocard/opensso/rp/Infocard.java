/* The contents of this file are subject to the terms
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
 * $Id: Infocard.java,v 1.13 2009/12/09 11:23:53 ppetitsm Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 * Portions Copyrighted 2008 Patrick Petit Consulting
 */
package com.identarian.infocard.opensso.rp;

import com.identarian.infocard.opensso.rp.exception.InfocardException;
import com.identarian.infocard.opensso.rp.exception.InfocardIdentityException;
import com.identarian.infocard.opensso.rp.rcheck.RoleCheckPlugin;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.authentication.service.AuthException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import java.security.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.servlet.http.HttpServletRequest;
import org.xmldap.util.KeystoreUtil;

/**
 * Sample Login Module.
 */
public class Infocard extends AMLoginModule {

    public static final String amAuthInfocard = "amAuthInfocard";
    public static final String DEFAULT_REQUIRED_CLAIMS =
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/privatepersonalidentifier";
    private static final String DEFAULT_TOKEN_TYPE = "urn:oasis:names:tc:SAML:1.0:assertion";
    /*
     * Process method states
     */
    private static final int DEFAULT_AUTH_LEVEL = 0;
    private static final int BEGIN_STATE = ISAuthConstants.LOGIN_START; //1
    private static final int BINDING_STATE = 2;
    private static final int REGISTRATION_STATE = 3;
    private static final int CHOOSE_USERNAME = 4;
    /*
     * Error return codes: Trigger inormative JSP page defined in infocard.xml
     */
    private static final int NO_USERID_ERROR = 5;
    private static final int NO_PASSWD_ERROR = 6;
    private static final int NO_CONFIRM_ERROR = 7;
    private static final int INVALID_PASSWORD_ERROR = 8;
    private static final int PASSWORD_MISMATCH_ERROR = 9;
    private static final int USER_EXISTS_ERROR = 10;
    private static final int USER_PASSWD_SAME_ERROR = 11;
    private static final int USER_PASSWORD_SAME_ERROR = 12;
    private static final int INTERNAL_ERROR = 13;
    private static final int INFOCARD_ERROR = 14;
    // Global variables
    protected static Debug debug = null;
    protected static ResourceBundle bundle = null;
    protected static String authType = null;
    private static PrivateKey privateKey = null;
    private static String keyStorePath;
    private static String keyStorePasswd;
    private static String keyAlias;
    private static AuthD authd = AuthD.getAuth();
    private int minPasswordLength;
    private boolean ignoreUserProfile;
    private boolean checkVerificationMethod;
    private boolean checkRequiredClaims;
    private String validatedUserID = null;
    private java.security.Principal userPrincipal = null;
    private Map sharedState = null;
    private Map config = null;
    private InfocardIdentity infocardIdentity = null;
    private String serviceStatus = null;
    private String userID = null;
    private String myOwnID = null;
    private String userPasswd = null;
    private String defaultAnonUser = null;
    private String verificationMethod = null;
    private String audience = null;
    private Set<String> requiredClaims = null;
    private String regEx = null;
    private Map<String, Set<String>> idRepoAttributes = null;
    private Map<String, String> infocardIdentityToIdRepoIdentityMap = null;
    private Map<String, String> roleToRoleCheckPluginMap = null;
    private Set<String> identityRoles = null;
    private String errorMsg = null;
    private String issuer = null;
    private String ppid = null;

    /**
     * Creates an instance of this class.
     *
     * @throws LoginException if class cannot be instantiated.
     */
    public Infocard() throws LoginException {
        debug = Debug.getInstance(amAuthInfocard);
    }

    /**
     * Initializes the module.
     *
     * @param subject
     * @param sharedState
     * @param options
     */
    @Override
    public void init(Subject subject, Map sharedState, Map options) {

        if (options == null || options.isEmpty()) {
            debug.error("options is null or empty");
            return;
        }
        this.config = options;

        if (privateKey == null) {
            keyStorePath = SystemProperties.get("javax.net.ssl.keyStore");
            keyStorePasswd = CollectionHelper.getMapAttr(options, "iplanet-am-auth-infocard-keyStorePassword");
            keyAlias = CollectionHelper.getMapAttr(options, "iplanet-am-auth-infocard-keyStoreAlias");
            try {
                privateKey = getPrivateKey();
            } catch (InfocardException e) {
                debug.error("Configuration error: check the module's Keystore parameters", e);
            }
        }

        // serviceConfig = (ServiceConfig) options.get("ServiceConfig");
        this.sharedState = sharedState;

        java.util.Locale locale = getLoginLocale();
        bundle = amCache.getResBundle(amAuthInfocard, locale);
        if (debug.messageEnabled()) {
            debug.message("DataStore resbundle locale=" + locale);
        }

        authType = bundle.getString("iplanet-am-auth-infocard-service-description");

        int authLevel = CollectionHelper.getIntMapAttr(options,
                                                       "iplanet-am-auth-infocard-auth-level", DEFAULT_AUTH_LEVEL, debug);
        try {
            setAuthLevel(authLevel);
        } catch (Exception e) {
            debug.error("Unable to set auth level " + authLevel, e);
            return;
        }

        serviceStatus = CollectionHelper.getMapAttr(
                options, "iplanet-am-auth-infocard-default-user-status", "Active");

        Set<String> defaultRoles = (Set<String>) options.get("iplanet-am-auth-infocard-default-roles");
        if (debug.messageEnabled()) {
            debug.message("defaultRoles is : " + defaultRoles);
        }
        identityRoles = new HashSet<String>();
        identityRoles.addAll(defaultRoles);

        regEx = CollectionHelper.getMapAttr(options, "iplanet-am-auth-infocard-invalid-chars");

        String passwordLength = CollectionHelper.getMapAttr(options,
                                                            "iplanet-am-auth-infocard-min-password-length", "8");
        if (passwordLength != null && passwordLength.length() != 0) {
            minPasswordLength = Integer.parseInt(passwordLength);
        }

        /*
         * Define the information card parameters as request parameters.
         */
        HttpServletRequest request = getHttpServletRequest();
        if (request != null && options != null) {

            String rclaims = getMapAttrValue(options,
                                             "iplanet-am-auth-infocard-requiredClaims", DEFAULT_REQUIRED_CLAIMS);
            request.setAttribute("requiredClaims", rclaims);

            String oclaims = getMapAttrValue(options,
                                             "iplanet-am-auth-infocard-optionalClaims", "");
            request.setAttribute("optionalClaims", oclaims);

            String tokenType = CollectionHelper.getMapAttr(options,
                                                           "iplanet-am-auth-infocard-tokenType", DEFAULT_TOKEN_TYPE);
            request.setAttribute("tokenType", tokenType);

            String privacyUrl = CollectionHelper.getMapAttr(options,
                                                            "iplanet-am-auth-infocard-privacyUrl");
            if (privacyUrl != null && privacyUrl.length() > 0) {
                request.setAttribute("privacyUrl", privacyUrl);
                // If privacyUrl != null, then privacyVersion must > 0
                String privacyVersion = CollectionHelper.getMapAttr(options,
                                                                    "iplanet-am-auth-infocard-privacyVersion");
                if (privacyVersion == null || privacyVersion.length() == 0) {
                    privacyVersion = "1";
                    request.setAttribute("privacyVersion", privacyVersion);
                }
            }

            issuer = CollectionHelper.getMapAttr(options, "iplanet-am-auth-infocard-issuer");
            if (issuer != null && issuer.length() > 0) {
                // URI specifying token issuer
                request.setAttribute("issuer", issuer);

                // MetadataExchange endpoint of issuer
                String issuerPolicy = CollectionHelper.getMapAttr(options,
                                                                  "iplanet-am-auth-infocard-issuerPolicy");
                if (issuerPolicy == null || issuerPolicy.length() == 0) {
                    issuerPolicy = new String(issuer.concat("/mex"));
                }
                if (!issuerPolicy.startsWith("https")) {
                    issuerPolicy.replaceFirst("http", "https");
                }
                request.setAttribute("issuerPolicy", issuer);
            }
        }
    }

    /**
     * Processes the callback requests.
     *
     * @param callbacks Array of callback object.
     * @param state
     * @return the status of the request.
     * @throws AuthLoginException if there are errors in processing the request.
     */
    @Override
    public int process(Callback[] callbacks, int state)
            throws AuthLoginException {

        int action;
        int retval = ISAuthConstants.LOGIN_IGNORE;

        switch (state) {
            case BEGIN_STATE:
                try {
                    infocardIdentity = getInfocardIdentity();
                    if (infocardIdentity != null) {
                        // Proceed with infocard authentication
                        initAuthInfocard();
                        //Check token validity
                        validateInfocardIdentity();
                        retval = LoginWithInfocard();
                    } else {
                        // No infocard, proceed with credential authentication
                        retval = LoginWithUserNamePasswd(callbacks);
                    }
                } catch (InfocardIdentityException e) {
                    if (debug.errorEnabled()) {
                        debug.error("Error processing Information Card:" + bundle.
                                getString(errorMsg));
                    }
                    replaceHeader(INFOCARD_ERROR, bundle.getString(errorMsg));
                    return INFOCARD_ERROR;
                } catch (InfocardException e) {
                    debug.error("Internal error", e);
                    retval = INTERNAL_ERROR;
                }
                break;

            case BINDING_STATE:

                if (callbacks != null && callbacks.length != 0) {
                    // Callbacks[2] is the confirmation/cancelation callback
                    action = ((ConfirmationCallback) callbacks[2]).
                            getSelectedIndex();
                } else {
                    throw new AuthLoginException(amAuthInfocard, "authFailed", null);
                }
                if (action == 0) {
                    // Confirm Information Card binding
                    retval = registerInfocardWithExistingUser(callbacks);
                } else if (action == 1) {
                    retval = REGISTRATION_STATE;
                } else {
                    // Cancel
                    retval = ISAuthConstants.LOGIN_IGNORE;
                }
                break;

            case REGISTRATION_STATE:
                // Registration will create a new user account and by the
                // Information Card to this account
                if (callbacks != null && callbacks.length != 0) {
                    // callbacks[3] is confirmation callback
                    action = ((ConfirmationCallback) callbacks[3]).
                            getSelectedIndex();
                } else {
                    throw new AuthLoginException(amAuthInfocard, "authFailed", null);
                }
                if (action == 0) { // Register
                    retval = getNewUserCredentials(callbacks);
                    if (retval == ISAuthConstants.LOGIN_SUCCEED) {
                        retval = registerInfocardWithNewUser();
                    }
                } else if (action == 1) { // Cancel
                    //clearCallbacks(callbacks);
                    retval = ISAuthConstants.LOGIN_IGNORE;
                } else if (action == 2) { // Reset Form
                    clearCallbacks(callbacks);
                    retval = REGISTRATION_STATE;
                }
                break;

            case CHOOSE_USERNAME:
                // user name entered already exists, generate
                // a set of user names for user to choose

                // callbacks[0] is the choice of the user ID
                String userChoiceID = getCallbackFieldValue(callbacks[0]);

                if (userChoiceID.equals(myOwnID)) {
                    retval = REGISTRATION_STATE;

                } else {
                    userID = userChoiceID;
                    Set<String> values = new HashSet<String>();
                    values.add(userID);
                    idRepoAttributes.put("uid", values);
                    retval = registerInfocardWithNewUser();
                }
                break;

        }
        if (retval == ISAuthConstants.LOGIN_SUCCEED && infocardIdentity != null) {
            Map<String, Set<String>> claims = infocardIdentity.getClaims();
            Iterator<String> itr = claims.keySet().
                    iterator();
            while (itr.hasNext()) {
                String claimUri = itr.next();
                Set<String> claimValues = infocardIdentity.getClaimValues(claimUri);
                if (claimValues != null && claimValues.size() != 0) {
                    // Must be called from within process()
                    setUserSessionProperty(InfocardClaims.canonicalizeClaimUri(claimUri),
                                           InfocardClaims.canonicalizeClaimValue(claimValues.
                            toString()));
                    if (debug.messageEnabled()) {
                        debug.message("Added claim to user session '" + claimUri + "' values = " + claimValues.
                                toString());
                    }
                }
            }
            // Set role prperties
            itr = identityRoles.iterator();
            while (itr.hasNext()) {
                String roleName = itr.next();
                setUserSessionProperty("claims.based.roles." + roleName, "Yes");
                if (debug.messageEnabled()) {
                        debug.message("Added claim-based role to user session '" + roleName + "' values = Yes");
                    }
            }
        }
        return retval;
    }

    @Override
    public Principal getPrincipal() {

        if (userPrincipal != null) {
            return userPrincipal;
        } else if (validatedUserID != null) {
            userPrincipal = new InfocardPrincipal(validatedUserID);
            return userPrincipal;
        } else {
            return null;
        }

    }

    @Override
    public void destroyModuleState() {
        validatedUserID = null;
        userPrincipal =
                null;
    }

    @Override
    public void nullifyUsedVars() {
        userPrincipal = null;
        sharedState =
                null;
        config =
                null;
        infocardIdentity =
                null;
        serviceStatus =
                null;
        userID =
                null;
        myOwnID =
                null;
        userPasswd =
                null;
        defaultAnonUser =
                null;
        verificationMethod =
                null;
        audience =
                null;
        requiredClaims =
                null;
        regEx =
                null;
        idRepoAttributes =
                null;
        infocardIdentityToIdRepoIdentityMap =
                null;
        roleToRoleCheckPluginMap =
                null;
        identityRoles =
                null;
        errorMsg =
                null;
        issuer =
                null;
        ppid =
                null;
    }

    private void initAuthInfocard() throws InfocardException {

        defaultAnonUser = CollectionHelper.getMapAttr(
                config, "iplanet-am-auth-infocard-default-user-name", "Anonymous");
        audience =
                CollectionHelper.getMapAttr(
                config, "iplanet-am-auth-infocard-audience-url", null);
        requiredClaims =
                getMapAttrValueSet(
                config, "iplanet-am-auth-infocard-requiredClaims", DEFAULT_REQUIRED_CLAIMS);
        checkRequiredClaims =
                Boolean.valueOf(CollectionHelper.getMapAttr(
                config, "iplanet-am-auth-infocard-check-requiredClaims")).
                booleanValue();
        verificationMethod =
                CollectionHelper.getMapAttr(
                config, "iplanet-am-auth-infocard-verificationMethod", null);
        checkVerificationMethod =
                Boolean.valueOf(CollectionHelper.getMapAttr(
                config, "iplanet-am-auth-infocard-check-verificationMethod")).
                booleanValue();

        // Get auth service to determine authentication profile
        OrganizationConfigManager orgConfigMgr = authd.getOrgConfigManager(getRequestOrg());
        ServiceConfig svcConfig;

        try {
            svcConfig = orgConfigMgr.getServiceConfig(ISAuthConstants.AUTH_SERVICE_NAME);
            Map params = svcConfig.getAttributes();
            String dynamicProfile = CollectionHelper.getMapAttr(params, ISAuthConstants.DYNAMIC_PROFILE);
            if (dynamicProfile.equalsIgnoreCase("ignore")) {
                ignoreUserProfile = true;
            }

        } catch (SMSException ex) {
            throw new InfocardException("Failed to get Auth Service config", ex);
        }

        /*
         * Compute user's attributes creation map
         */
        infocardIdentityToIdRepoIdentityMap = new HashMap<String, String>();
        roleToRoleCheckPluginMap =
                new HashMap<String, String>();
        idRepoAttributes =
                new HashMap<String, Set<String>>();
        setInfocardIdentityToIdRepoIdentityMap(config);
        setRoleToRoleCheckPluginMap(config);
    }

    private void validateInfocardIdentity() throws InfocardIdentityException {

        if (checkVerificationMethod && infocardIdentity.isClaimSupplied(
                InfocardClaims.getVERIFIED_CLAIMS_URI())) {

            String var = infocardIdentity.getClaimValue(InfocardClaims.
                    getVERIFICATION_METHOD_URI());
            if (!verificationMethod.equals(var)) {
                errorMsg = "missingVerificationMethod";
                throw new InfocardIdentityException(errorMsg);
            }

        }

        String actualIssuer = infocardIdentity.getIssuer();
        if (issuer != null && !actualIssuer.matches(issuer + "*")) {
            errorMsg = "invalidIssuer";
            throw new InfocardIdentityException(errorMsg);
        }

// Override issuer with value from token
        issuer = actualIssuer;

        if (audience != null && !audience.equals(infocardIdentity.getAudience())) {
            errorMsg = "invalidAudience";
            throw new InfocardIdentityException(errorMsg);
        }

// Check all other required claims
        if (checkRequiredClaims && !infocardIdentity.areClaimsSupplied(requiredClaims)) {
            errorMsg = "missingRequiredClaims";
            throw new InfocardIdentityException(errorMsg);
        }

    }

    private void setInfocardIdentityRoles() throws InfocardException {

        Set<String> roles = roleToRoleCheckPluginMap.keySet();
        if (roles != null) {
            Iterator<String> itr = roles.iterator();
            String role, clazz;
            while (itr.hasNext()) {
                role = itr.next();
                clazz = roleToRoleCheckPluginMap.get(role).
                        trim();
                if (clazz != null && clazz.length() > 0) {
                    try {
                        RoleCheckPlugin plugin = (RoleCheckPlugin) Class.forName(clazz).
                                newInstance();
                        if (plugin.isIdentityMatchingRole(infocardIdentity, role)) {
                            identityRoles.add(role);
                        }

                    } catch (Exception e) {
                        throw new InfocardException(e);
                    }

                }
            }
        }
    }

    private void setGroupMembership(AMIdentity userIdentity, String org)
            throws InfocardException {

        Iterator<String> itr = identityRoles.iterator();
        while (itr.hasNext()) {
            String roleName = itr.next();
            AMIdentity groupIdentity = getGroupIdentity(roleName, org);
            if (groupIdentity != null) {
                try {
                    groupIdentity.addMember(userIdentity);
                    if (debug.messageEnabled()) {
                        debug.message(
                                "setGroupMembership: Added user '" +
                                userIdentity.getName() + "' to group = " +
                                groupIdentity.getName());
                    }

                } catch (IdRepoException ex) {
                    throw new InfocardException(ex);
                } catch (SSOException ex) {
                    throw new InfocardException(ex);
                }

            }
        }
    }

    private int LoginWithUserNamePasswd(Callback[] callbacks) throws AuthLoginException {

        String password = null;
        Callback[] idCallbacks;
        boolean getCredentialsFromSharedState = false;

        // That's 'regular' compatibility mode login
        if (callbacks != null && callbacks.length == 0) {
            idCallbacks = new Callback[2];
            userID =
                    (String) sharedState.get(getUserKey());
            password =
                    (String) sharedState.get(getPwdKey());
            if (userID == null || password == null) {
                return ISAuthConstants.LOGIN_START;
            }

            getCredentialsFromSharedState = true;
            NameCallback nameCallback = new NameCallback("dummy");
            nameCallback.setName(userID);
            idCallbacks[0] = nameCallback;
            PasswordCallback passwordCallback = new PasswordCallback("dummy", false);
            passwordCallback.setPassword(password.toCharArray());
            idCallbacks[1] = passwordCallback;
        } else {
            //callbacks is not null
            idCallbacks = callbacks;
            userID =
                    ((NameCallback) callbacks[0]).getName();
            password =
                    String.valueOf(((PasswordCallback) callbacks[1]).getPassword());
        }

        if (password == null || password.length() == 0) {
            if (debug.errorEnabled()) {
                debug.error("User password is null or empty");
            }

            throw new InvalidPasswordException("amAuth", "invalidPasswd", null);
        }

//store user ID and password both in success and failure case
        storeUsernamePasswd(userID, password);

        //initAuthConfig();
        try {
            AMIdentityRepository idrepo = getAMIdentityRepository(getRequestOrg());
            boolean success = idrepo.authenticate(idCallbacks);
            if (success) {
                validatedUserID = userID;
                return ISAuthConstants.LOGIN_SUCCEED;
            } else {
                throw new AuthLoginException(amAuthInfocard, "authFailed", null);
            }

        } catch (IdRepoException ex) {
            if (getCredentialsFromSharedState && !isUseFirstPassEnabled()) {
                return ISAuthConstants.LOGIN_START;
            }

            setFailureID(userID);
            throw new AuthLoginException(amAuthInfocard, "authFailed", null, ex);
        }

    }

    private int LoginWithInfocard() throws AuthLoginException,
                                           InvalidPasswordException, InfocardIdentityException {

        Callback[] callbacks = new Callback[2];

        try {

            ppid = infocardIdentity.getClaimValue(InfocardClaims.getPPID_URI());
            if (ppid == null) {
                errorMsg = "missingPPID";
                throw new InfocardIdentityException(errorMsg);
            }
// TODO: I think we have a problem here because searchUserIdentity returns
// too many entries. Must improve search criteria filter scheme.

            AMIdentityRepository idrepo = getAMIdentityRepository(getRequestOrg());
            AMIdentity repoIdentity = InfocardIdRepoUtils.searchUserIdentity(idrepo, ppid, "*");

            if (repoIdentity != null) { // Information Card already linked with this entry
                userID = repoIdentity.getName();
                InfocardIdRepoData icRepoData =
                        InfocardIdRepoUtils.getInfocardRepoData(repoIdentity, ppid);
                if (icRepoData != null) {
                    userPasswd = icRepoData.getPassword();

                    if (!issuer.equals(icRepoData.getIssuer())) {
                        // Forgery ?
                        errorMsg = "invalidIssuer";
                        if (debug.errorEnabled()) {
                            debug.error("Information Card token doesn't match stored issuer for PPID =" + ppid);
                        }

                        throw new InfocardIdentityException(errorMsg);
                    }

                }
                // should be removed
                if (userPasswd == null || userPasswd.length() == 0) {
                    debug.error("User password is null or empty");
                    throw new InvalidPasswordException("amAuth", "invalidPasswd", null);
                }

                NameCallback nameCallback = new NameCallback("dummy");
                nameCallback.setName(userID);
                callbacks[0] = nameCallback;
                PasswordCallback passwordCallback = new PasswordCallback("dummy", false);
                passwordCallback.setPassword(userPasswd.toCharArray());
                callbacks[1] = passwordCallback;

                //store user ID and password both in success and failure case
                storeUsernamePasswd(userID, userPasswd);

                try {
                    boolean success = idrepo.authenticate(callbacks);
                    if (success) {
                        setInfocardIdentityRoles();
                        validatedUserID = userID;
                        return ISAuthConstants.LOGIN_SUCCEED;
                    } else {
                        // Stored password is invalid. Remove iDRepo Infocard data
                        // and return to BEGIN_STATE
                        InfocardIdRepoUtils.removeRepoInfocardData(repoIdentity, ppid);
                        if (debug.errorEnabled()) {
                            debug.error("User password has changed since Information Card registration. " +
                                        "Deleted stored Information Card for that user: " + userID);
                        }

                        return BEGIN_STATE;
                    }

                } catch (IdRepoException e) {
                    setFailureID(userID);
                    throw new AuthLoginException(amAuthInfocard, "authFailed", null, e);
                }

            } else if (ignoreUserProfile) {
                // Since user profile is ignored, user is allowed to login provided
                // Information Card claims meet validation conditions
                setInfocardIdentityRoles();
                userID = defaultAnonUser;
                storeUsernamePasswd(userID, null);
                validatedUserID = userID;
                return ISAuthConstants.LOGIN_SUCCEED;
            } else {
                // Bind this Information Card with a new or existing user account
                return BINDING_STATE;
            }

        } catch (InfocardException e) {
            setFailureID(userID);
            debug.error("Caught unexpected exception: ", e);
            return INTERNAL_ERROR;
        }

    }

    private int registerInfocardWithExistingUser(Callback[] callbacks)
            throws AuthLoginException {

        if (callbacks != null && callbacks.length != 0) {
            userID = ((NameCallback) callbacks[0]).getName();
            userPasswd =
                    String.valueOf(((PasswordCallback) callbacks[1]).getPassword());

            if (userID == null || userID.length() == 0) {
                return NO_USERID_ERROR;
            }

            if (userPasswd == null || userPasswd.length() == 0) {
                return NO_PASSWD_ERROR;
            }

            storeUsernamePasswd(userID, userPasswd);

            try {
                AMIdentityRepository idrepo = getAMIdentityRepository(getRequestOrg());
                boolean success = idrepo.authenticate(callbacks);
                if (success) {
                    AMIdentity repoIdentity = getUserIdentity(userID, getRequestOrg());
                    setInfocardIdentityRoles();
                    InfocardIdRepoUtils.addRepoInfocardData(repoIdentity, ppid, issuer, userPasswd);
                    validatedUserID = userID;
                    return ISAuthConstants.LOGIN_SUCCEED;
                } else {
                    setFailureID(userID);
                    throw new AuthLoginException(amAuthInfocard, "authFailed", null);
                }

            } catch (IdRepoException e) {
                setFailureID(userID);
                throw new AuthLoginException(amAuthInfocard, "authFailed", null, e);
            } catch (InfocardException e) {
                setFailureID(userID);
                debug.error("Caught unexpected exception: ", e);
                return INTERNAL_ERROR;
            }

        } else {
            throw new AuthLoginException(amAuthInfocard, "authFailed", null);
        }

    }

    private int getNewUserCredentials(Callback[] callbacks)
            throws AuthLoginException {

        // callback[0] is for user name
        // callback[1] is for new password
        // callback[2] is for confirm password
        if (callbacks != null && callbacks.length != 0) {
            userID = ((NameCallback) callbacks[0]).getName();
            // check user name
            if ((userID == null) || userID.length() == 0) {
                // no user name was entered, this is required to
                // create the user's profile
                return NO_USERID_ERROR;
            }

//validate username using plugin if any
            validateUserName(userID, regEx);

            // get the passwords from the input form
            userPasswd =
                    String.valueOf(((PasswordCallback) callbacks[1]).getPassword());
            /* Currently there is a bug that needs fix. confirmPassword returns null
            String confirmPassword = String.valueOf(((PasswordCallback) callbacks[2]).getPassword());

            // check passwords

            int status = checkPassword(userID, userPasswd, confirmPassword);

            // Return if any error
            if (status != ISAuthConstants.LOGIN_SUCCEED) {
            debug.error("Check password failed with status= " + status);
            return status;
            }
             */
            // validate password using validation plugin if any
            validatePassword(userPasswd);

            if (userPasswd.equals(userID)) {
                // the user name and password are the same. these fields
                // must be different
                return USER_PASSWORD_SAME_ERROR;
            }

            Set<String> values = new HashSet<String>();
            values.add(userID);
            idRepoAttributes.put("uid", values);

            values =
                    new HashSet<String>();
            values.add(userPasswd);
            idRepoAttributes.put("userPassword", values);

            // check user ID uniqueness
            try {
                if (isUserRegistered(userID)) {
                    String var = infocardIdentity.getClaimValue(
                            InfocardClaims.getGIVEN_NAME_URI());
                    if (var != null && var.length() > 0) {
                        values = new HashSet<String>();
                        values.add(var);
                        idRepoAttributes.put("givenname", values);
                    }

                    var = infocardIdentity.getClaimValue(
                            InfocardClaims.getSURNAME_URI());
                    if (var != null && var.length() > 0) {
                        values = new HashSet<String>();
                        values.add(var);
                        idRepoAttributes.put("sn", values);
                    }

// get a list of user IDs from the generator
                    Set generatedUserIDs = getNewUserIDs(idRepoAttributes, 6);
                    if (generatedUserIDs == null) {
                        // user name generator is disable
                        return USER_EXISTS_ERROR;
                    }

// get a list of user IDs that are not yet being used
                    ArrayList nonExistingUserIDs = getNonExistingUserIDs(generatedUserIDs);

                    resetCallback(CHOOSE_USERNAME, 0);
                    Callback[] origCallbacks = getCallback(CHOOSE_USERNAME);
                    ChoiceCallback origCallback = (ChoiceCallback) origCallbacks[0];
                    String prompt = origCallback.getPrompt();
                    myOwnID =
                            origCallback.getChoices()[0];

                    nonExistingUserIDs.add(myOwnID);

                    String[] choices = ((String[]) nonExistingUserIDs.toArray(new String[0]));

                    ChoiceCallback callback = new ChoiceCallback(prompt, choices, 0, false);
                    callback.setSelectedIndex(0);
                    replaceCallback(CHOOSE_USERNAME, 0, callback);

                    return CHOOSE_USERNAME;
                }

            } catch (SSOException e) {
                setFailureID(userID);
                debug.error("Caught unexpected exception: ", e);
                return INTERNAL_ERROR;
            } catch (IdRepoException e) {
                setFailureID(userID);
                debug.error("Caught unexpected exception: ", e);
                return INTERNAL_ERROR;
            }

        } else {
            throw new AuthLoginException(amAuthInfocard, "authFailed", null);
        }

        return ISAuthConstants.LOGIN_SUCCEED;
    }

    private int registerInfocardWithNewUser() throws AuthLoginException {

        try {
            Set<String> values = new HashSet<String>();
            values.add(serviceStatus);
            idRepoAttributes.put("inetuserstatus", values);
            if (isDynamicProfileCreationEnabled()) {
                addInfocardClaimsToIdRepoAttributes();
            }
            createIdentity(userID, idRepoAttributes, identityRoles);
            // Should optimize this ...
            AMIdentity repoIdentity = getUserIdentity(userID, getRequestOrg());
            setInfocardIdentityRoles();
            setGroupMembership(repoIdentity, getRequestOrg());
            InfocardIdRepoUtils.addRepoInfocardData(repoIdentity, ppid, issuer, userPasswd);
            validatedUserID = userID;
            return ISAuthConstants.LOGIN_SUCCEED;
        } catch (SSOException e) {
            setFailureID(userID);
            debug.error("Caught unexpected exception: ", e);
            return INTERNAL_ERROR;
        } catch (IdRepoException e) {
            setFailureID(userID);
            debug.error("Caught unexpected exception: ", e);
            return INTERNAL_ERROR;
        } catch (InfocardException e) {
            setFailureID(userID);
            debug.error("Caught unexpected exception: ", e);
            return INTERNAL_ERROR;
        }

    }

    protected static String getAuthType() {
        return authType;
    }

    private AMIdentity getUserIdentity(String userName, String org) {

        AMIdentity repoIdentity = null;

        if (org != null && org.length() != 0) {
            try {
                repoIdentity = AuthD.getAuth().
                        getIdentity(IdType.USER, userName, org);
                if (repoIdentity.isExists() && repoIdentity.isActive()) {
                    if (debug.messageEnabled()) {
                        debug.message(
                                "getUserIdentity: Found identity for '" +
                                userName + "' in realm = " + repoIdentity.
                                getRealm() +
                                " id = " + repoIdentity.getUniversalId());
                    }

                }
            } catch (AuthException e1) {
                debug.error("getUserIdentity: caught exception", e1);
            } catch (IdRepoException e2) {
                debug.error("getUserIdentity: caught exception", e2);
            } catch (SSOException e3) {
                debug.error("getUserIdentity: caught exception", e3);
            }

        }
        return repoIdentity;
    }

    private AMIdentity getGroupIdentity(String groupName, String org) {

        AMIdentity repoIdentity = null;

        if (org != null && org.length() != 0) {
            try {
                repoIdentity = AuthD.getAuth().
                        getIdentity(IdType.GROUP, groupName, org);
                if (repoIdentity.isExists()) {
                    if (debug.messageEnabled()) {
                        debug.message(
                                "getGroupIdentity: Found identity for '" +
                                groupName + "' in realm = " + repoIdentity.
                                getRealm() +
                                " id = " + repoIdentity.getUniversalId());
                    }

                }
            } catch (AuthException e1) {
                debug.error("getUserIdentity: caught exception", e1);
            } catch (IdRepoException e2) {
                debug.error("getUserIdentity: caught exception", e2);
            } catch (SSOException e3) {
                debug.error("getUserIdentity: caught exception", e3);
            }

        }
        return repoIdentity;
    }

    private ArrayList getNonExistingUserIDs(Set userIDs)
            throws IdRepoException, SSOException {

        ArrayList<String> validUserIDs = new ArrayList<String>();
        Iterator it = userIDs.iterator();

        while (it.hasNext()) {
            String uid = (String) it.next();
            // check if user already exists with the same user ID
            if (!isUserRegistered(uid)) {
                validUserIDs.add(uid);
            }

        }
        return validUserIDs;
    }

    /**
     * When user click cancel button, these input field should be reset
     * to blank.
     */
    private void clearCallbacks(Callback[] callbacks) {
        for (int i = 0; i <
                        callbacks.length; i++) {
            if (callbacks[i] instanceof NameCallback) {
                NameCallback nc = (NameCallback) callbacks[i];
                nc.setName("");
            }

        }
    }

    /**
     * Returns the first input value for the given Callback.
     * Returns null if there is no value for the Callback.
     */
    private static String getCallbackFieldValue(Callback callback) {

        Set values = getCallbackFieldValues(callback);
        Iterator it = values.iterator();
        if (it.hasNext()) {
            return ((String) it.next());
        }

        return null;
    }

    private static Set getCallbackFieldValues(Callback callback) {
        Set<String> values = new HashSet<String>();

        if (callback instanceof NameCallback) {
            String value = ((NameCallback) callback).getName();
            if (value != null && value.length() != 0) {
                values.add(value);
            }

        } else if (callback instanceof PasswordCallback) {
            String value = getPassword((PasswordCallback) callback);
            if (value != null && value.length() != 0) {
                values.add(value);
            }

        } else if (callback instanceof ChoiceCallback) {
            String[] vals = ((ChoiceCallback) callback).getChoices();
            int[] selectedIndexes =
                    ((ChoiceCallback) callback).getSelectedIndexes();
            for (int i = 0; i <
                            selectedIndexes.length; i++) {
                values.add(vals[selectedIndexes[i]]);
            }

        }
        return values;
    }

    private static String getPassword(PasswordCallback callback) {
        char[] tmpPassword = callback.getPassword();
        if (tmpPassword == null) {
            // treat a NULL password as an empty password
            tmpPassword = new char[0];
        }

        char[] pwd = new char[tmpPassword.length];
        System.arraycopy(tmpPassword, 0, pwd, 0, tmpPassword.length);

        return (new String(pwd));
    }

    private int checkPassword(String uid, String password, String confirmPassword) {

        if ((password == null) || password.length() == 0) {
            // missing the password field
            debug.error("password was missing from the form");
            return NO_PASSWD_ERROR;

        } else {
            // compare the length of the user entered password with
            // the length required
            if (password.length() < minPasswordLength) {
                debug.error("password was not long enough");
                return INVALID_PASSWORD_ERROR;
            }

// length OK, now make sure the user entered a confirmation
// password
            if ((confirmPassword == null) || confirmPassword.length() == 0) {
                // no confirm password field entered
                debug.error("no confirmation password");
                return NO_CONFIRM_ERROR;
            } else {
                // does the confirmation password match the actual password
                if (!password.equals(confirmPassword)) {
                    // the password and the confirmation password don't match
                    return PASSWORD_MISMATCH_ERROR;
                }

            }

            // the user name and password are the same. these fields
            // must be different
            if (password.equals(uid)) {
                return USER_PASSWD_SAME_ERROR;
            }

        }
        return ISAuthConstants.LOGIN_SUCCEED;
    }

    private boolean isUserRegistered(String userID)
            throws IdRepoException, SSOException {

        AMIdentityRepository amIdRepo = getAMIdentityRepository(getRequestOrg());

        IdSearchControl idsc = new IdSearchControl();
        idsc.setRecursive(true);
        idsc.setTimeOut(0);
        idsc.setAllReturnAttributes(true);
        // search for the identity
        Set results = Collections.EMPTY_SET;
        try {
            idsc.setMaxResults(0);
            IdSearchResults searchResults = amIdRepo.searchIdentities(IdType.USER, userID, idsc);
            if (searchResults != null) {
                results = searchResults.getSearchResults();
            }

        } catch (IdRepoException e) {
            if (debug.messageEnabled()) {
                debug.message("IdRepoException : Error searching " + " Identities with username : " + e.
                        getMessage());
            }

        }
        return !results.isEmpty();
    }

    private void setInfocardIdentityToIdRepoIdentityMap(Map currentConfig) {

        Set claimUriToAttrList = (Set) currentConfig.get("iplanet-am-auth-infocard-creation-attr-list");

        if ((claimUriToAttrList != null)) {
            if (debug.messageEnabled()) {
                debug.message("User attributes creation list = " + claimUriToAttrList);
            }

            Iterator itr = claimUriToAttrList.iterator();
            while (itr.hasNext()) {
                String entry = (String) itr.next();
                int i = entry.indexOf("|");
                if (i != -1) {
                    String claimUri = entry.substring(0, i).
                            trim();
                    String attrName = entry.substring(i + 1, entry.length()).
                            trim();
                    if ((attrName != null) && (attrName.length() != 0) &&
                        (claimUri != null) && (claimUri.length() != 0)) {
                        infocardIdentityToIdRepoIdentityMap.put(claimUri, attrName);
                    }

                }
            }
        }
    }

    private void setRoleToRoleCheckPluginMap(Map currentConfig) {

        Set roleToPluginClass = (Set) currentConfig.get("iplanet-am-auth-infocard-role-to-plugin-list");

        if ((roleToPluginClass != null)) {
            if (debug.messageEnabled()) {
                debug.message("role to plugin class list = " + roleToPluginClass);
            }

            Iterator itr = roleToPluginClass.iterator();
            while (itr.hasNext()) {
                String entry = (String) itr.next();
                int i = entry.indexOf("|");
                if (i != -1) {
                    String role = entry.substring(0, i).
                            trim();
                    String className = entry.substring(i + 1, entry.length()).
                            trim();
                    if ((role != null) && (role.length() != 0) &&
                        (className != null) && (className.length() != 0)) {
                        roleToRoleCheckPluginMap.put(role, className);
                    }

                }
            }
        }
    }

    private void addInfocardClaimsToIdRepoAttributes() {

        Iterator itr = infocardIdentityToIdRepoIdentityMap.keySet().
                iterator();
        while (itr.hasNext()) {
            String claimUri = (String) itr.next();
            String attrName = (String) infocardIdentityToIdRepoIdentityMap.get(claimUri);
            Set<String> claimValues = infocardIdentity.getClaimValues(claimUri);
            if (claimValues != null && claimValues.size() != 0) {
                idRepoAttributes.put(attrName, claimValues);
            }

        }
        if (debug.messageEnabled()) {
            debug.message("IdRepo attributes creation list = " + idRepoAttributes);
        }

    }

    private InfocardIdentity getInfocardIdentity() throws InfocardIdentityException {

        InfocardIdentity identity = null;

        HttpServletRequest request = getHttpServletRequest();
        if (request != null) {
            String samlToken = request.getParameter("xmlToken");
            if (samlToken != null && samlToken.length() != 0) {
                try {
                    identity = new InfocardIdentity(samlToken, privateKey);
                } catch (InfocardIdentityException e) {
                    errorMsg = e.getMessage();
                    throw new InfocardIdentityException(errorMsg);
                }

            }
        }
        return identity;
    }

    /**
     * Get the private. Must correspond to the server's SSL cert private key
     */
    private static synchronized PrivateKey getPrivateKey()
            throws InfocardException {

        PrivateKey key = null;
        try {
            KeystoreUtil keystore = new KeystoreUtil(keyStorePath, keyStorePasswd);
            key =
                    keystore.getPrivateKey(keyAlias, keyStorePasswd);
        } catch (Exception e) {
            throw new InfocardException("Caught keystore exception", e);
        }

        return key;
    }

    private static void debugPrintMap(String name, Map map) {
        Set keys = map.keySet();
        for (Iterator iter = keys.iterator(); iter.hasNext();) {
            String key = (String) iter.next();
            System.out.println(">>> " + name + "(" + key + ")=" + map.get(key));
        }

    }

    private static String getMapAttrValue(Map map, String attrName, String defValue) {

        String value = defValue;
        Set keys = map.keySet();

        for (Iterator iter = keys.iterator(); iter.hasNext();) {
            String key = (String) iter.next();
            if (key.equals(attrName)) {
                String var = map.get(key).
                        toString();
                var =
                        var.substring(1, var.length() - 1);
                value =
                        var;
                break;

            }




        }
        return value;
    }

    private static Set getMapAttrValueSet(Map map, String attrName, String defValue) {

        Set value = null;
        Set keys = map.keySet();

        for (Iterator iter = keys.iterator(); iter.hasNext();) {
            String key = (String) iter.next();
            if (key.equals(attrName)) {
                value = (Set) map.get(key);
                break;

            }
        }
        if (value == null) {
            value = new HashSet();
            value.add(defValue);
        }

        return value;
    }
}
            