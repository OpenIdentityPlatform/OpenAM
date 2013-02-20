/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package com.sun.identity.authentication.modules.membership;

/**
 *
 * @author steve
 */
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import java.security.Principal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

public class Membership extends AMLoginModule {
    private ResourceBundle bundle;
    private Map sharedState;
    
    // user's valid ID and principal
    private String validatedUserID;
    private MembershipPrincipal userPrincipal;
    
    // configurations
    private Map options;
    
    private String serviceStatus;
    private boolean isDisclaimerExist = true;
    private Set defaultRoles;
    private int requiredPasswordLength;
    private String createMyOwn;
    private String userID;
    private String userName;
    private Map userAttrs;
    private static final String amAuthMembership = "amAuthMembership";
    private final static Debug debug = Debug.getInstance(amAuthMembership);
    private String regEx;
    private static final String INVALID_CHARS =
        "iplanet-am-auth-membership-invalid-chars";
    private boolean getCredentialsFromSharedState;
    private Callback[] callbacks;
    
    /**
     * Initializes this <code>LoginModule</code>.
     *
     * @param subject the <code>Subject</code> to be authenticated.
     * @param sharedState shared <code>LoginModule</code> state.
     * @param options options specified in the login.
     *        <code>Configuration</code> for this particular
     *        <code>LoginModule</code>.
     */
    public void init(Subject subject, Map sharedState, Map options) {    
        java.util.Locale locale = getLoginLocale();
        bundle = amCache.getResBundle(amAuthMembership, locale);
        
        if (debug.messageEnabled()) {
            debug.message("Membership getting resource bundle for locale: " + locale);
        }
        
        this.options = options;
        this.sharedState = sharedState;
    }
    
    /**
     * Takes an array of submitted <code>Callback</code>,
     * process them and decide the order of next state to go.
     * Return STATE_SUCCEED if the login is successful, return STATE_FAILED
     * if the LoginModule should be ignored.
     *
     * @param callbacks an array of <code>Callback</cdoe> for this Login state
     * @param state order of state. State order starts with 1.
     * @return int order of next state. Return STATE_SUCCEED if authentication
     *         is successful, return STATE_FAILED if the
     *         LoginModule should be ignored.
     * @throws AuthLoginException
     */
    public int process(Callback[] callbacks, int state)
    throws AuthLoginException {
        if (debug.messageEnabled()) {
            debug.message("in process(), login state is " + state);
        }
        
        this.callbacks = callbacks;
        ModuleState moduleState = ModuleState.get(state);
        ModuleState nextState = null;
        
        switch (moduleState) {
            case LOGIN_START:
                int action = 0;
                
                // callback[2] is user selected button index
                // action == 0 is a Submit Button
                if (callbacks !=null && callbacks.length != 0) {
                    action = ((ConfirmationCallback)callbacks[2]).getSelectedIndex();
                    if (debug.messageEnabled()) {
                        debug.message("LOGIN page button index: " + action);
                    }
                }
                
                if (action == 0) {
                    // loginUser will attempt to validate the user and return
                    // the next state to display, either an error state or
                    // SUCCESS
                    nextState = loginUser(callbacks);                    
                } else {
                    // new user registration
                    initAuthConfig();
                    clearInfoText(ModuleState.REGISTRATION.intValue());
                    nextState = ModuleState.REGISTRATION;
                }
                
                break;
                       
            case CHOOSE_USERNAMES:
                // user name entered already exists, generate
                // a set of user names for user to choose
                nextState = chooseUserID(callbacks);
                break;
                
            case DISCLAIMER:
                // when disclaimer page exists the user is created
                // after the user agrees to disclaimer
                // callbacks[0] is user selected button index
                int agree = ((ConfirmationCallback)callbacks[0]).getSelectedIndex();
                
                if (debug.messageEnabled()) {
                    debug.message("DISCLAIMER page button index: " + agree);
                }
                
                if (agree == 0) {
                    RegistrationResult result = registerNewUser();
                    
                    if (result.equals(RegistrationResult.NO_ERROR)) {
                        return ISAuthConstants.LOGIN_SUCCEED;
                    } else {
                        switch (result) {
                            case USER_EXISTS_ERROR:
                                setErrorMessage(result,0);
                                nextState = ModuleState.REGISTRATION;
                                break;

                            case PROFILE_ERROR:
                                nextState = ModuleState.PROFILE_ERROR;
                                break;

                            case NO_ERROR:
                                nextState = ModuleState.COMPLETE;
                                break;
                        }
                    }
                } else if (agree == 1) {
                    nextState = ModuleState.DISCLAIMER_DECLINED;
                } else {
                    throw new AuthLoginException(amAuthMembership, "loginException", null);
                }
                
                break;
                
            case REGISTRATION:
                // this is REGISTRATION state, registration will attempt to
                // create a new user profile

                // callbacks[len-1] is a user selected button index
                // next == 0 is a Submit button
                // next == 1 is a Cancel button
                int next = ((ConfirmationCallback) callbacks[callbacks.length-1]).getSelectedIndex();
                
                if (debug.messageEnabled()) {
                    debug.message("REGISTRATION page button index: " + next);
                }
                
                if (next == 0) {
                    //clear infotexts in case they had error messages in the
                    //previous run
                    clearInfoText(ModuleState.REGISTRATION.intValue());
                    ModuleState result = getAndCheckRegistrationFields(callbacks);
                    
                    switch (result) {
                        case DISCLAIMER:
                            nextState = processRegistrationResult();
                            break;

                        case REGISTRATION:
                        case CHOOSE_USERNAMES:
                        case PROFILE_ERROR:
                            if (debug.messageEnabled()) {
                                debug.message("Recoverable error: " + result.toString());
                            }

                            nextState = result;
                            break;
}
                } else if (next == 1) {
                    clearCallbacks(callbacks);
                    nextState = ModuleState.LOGIN_START;
                } else {
                    return ISAuthConstants.LOGIN_IGNORE;
                }     
        }
        
        return nextState.intValue();
    }
    
    private ModuleState processRegistrationResult()
    throws AuthLoginException {
        ModuleState nextState = null;
        
        if (isDisclaimerExist) {
            if (debug.messageEnabled()) {
                debug.message("Move to disclaimer page");
            }

            nextState = ModuleState.DISCLAIMER;
        } else {
            if (debug.messageEnabled()) {
                debug.message("No disclaimer, register user");
            }

            RegistrationResult regResult = registerNewUser();

            switch (regResult) {
                case USER_EXISTS_ERROR:
                    setErrorMessage(regResult,0);
                    nextState = ModuleState.REGISTRATION;
                    break;
                case PROFILE_ERROR:
                    nextState = ModuleState.PROFILE_ERROR;
                    break;
                case NO_ERROR:
                    nextState = ModuleState.COMPLETE;
            }
        }
        
        return nextState;
    }
    
    /**
     * User input value will be store in the callbacks[].
     * When user click cancel button, these input field should be reset
     * to blank.
     */
    private void clearCallbacks(Callback[] callbacks) {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof NameCallback) {
                NameCallback nc = (NameCallback) callbacks[i];
                nc.setName("");
            }
        }
    }    
    
    /**
     * Returns <code>Principal</code>.
     *
     * @return <code>Principal</code>
     */
    public Principal getPrincipal() {
        if (userPrincipal != null) {
            return userPrincipal;            
        } else if (validatedUserID != null) {
            userPrincipal = new MembershipPrincipal(validatedUserID);
            return userPrincipal;            
        } else {
            return null;
        }
    }

    /**
     * Destroy the module state
     */
    @Override
    public void destroyModuleState() {
        validatedUserID = null;
    }
    
    /**
     * Set all the used variables to null
     */
    @Override
    public void nullifyUsedVars() {
        bundle = null;
        sharedState = null;
        options = null;
        serviceStatus = null;
        defaultRoles = null;
        userID = null;
        userName = null ;
        userAttrs = null;
        regEx = null;
        callbacks = null;
    }    

    /**
     * Initializes registration configurations.
     */
    private void initAuthConfig() throws AuthLoginException {
        if (options == null || options.isEmpty()) {
            debug.error("options is null or empty");
            throw new AuthLoginException(amAuthMembership, "unable-to-initialize-options", null);
        }
        
        try {
            String authLevel = CollectionHelper.getMapAttr(options,
                "iplanet-am-auth-membership-auth-level");
            
            if (authLevel != null) {
                try {
                    int tmp = Integer.parseInt(authLevel);
                    setAuthLevel(tmp);
                } catch (NumberFormatException e) {
                    // invalid auth level
                    debug.error("invalid auth level " + authLevel, e);
                }
            }    
            
            regEx = CollectionHelper.getMapAttr(options, INVALID_CHARS);        
            serviceStatus = CollectionHelper.getMapAttr(options,
                "iplanet-am-auth-membership-default-user-status", "Active");
	            
            if (getNumberOfStates() >= ModuleState.DISCLAIMER.intValue()) {
                isDisclaimerExist = true;
            } else {
                isDisclaimerExist = false;
            }
 
            defaultRoles = (Set) options.get("iplanet-am-auth-membership-default-roles");  
            
            if (debug.messageEnabled()) {
                debug.message("defaultRoles is : " + defaultRoles);
            }
     
            String tmp = CollectionHelper.getMapAttr(options, "iplanet-am-auth-membership-min-password-length");
            
            if (tmp != null) {
                requiredPasswordLength = Integer.parseInt(tmp);
            }       
        } catch(Exception ex){
            debug.error("unable to initialize in initAuthConfig(): ", ex);
            throw new AuthLoginException(amAuthMembership, "Membershipex", null, ex);        	
        }
    }    
   
    
    private ModuleState loginUser(Callback[] callbacks) throws AuthLoginException {
    	String password = null;
        Callback[] idCallbacks = new Callback[2];
        
        try {
            if (callbacks !=null && callbacks.length == 0) {
                userName = (String) sharedState.get(getUserKey());
                password = (String) sharedState.get(getPwdKey());
                
                if (userName == null || password == null) {
                    return ModuleState.LOGIN_START;
                }
                
                getCredentialsFromSharedState = true;
                NameCallback nameCallback = new NameCallback("dummy");
                nameCallback.setName(userName);
                idCallbacks[0] = nameCallback;
                PasswordCallback passwordCallback = new PasswordCallback("dummy",false);
                passwordCallback.setPassword(password.toCharArray());
                idCallbacks[1] = passwordCallback;
            } else {
                idCallbacks = callbacks;
                //callbacks is not null
                userName = ( (NameCallback) callbacks[0]).getName();
                password = String.valueOf(((PasswordCallback)
                    callbacks[1]).getPassword());
            }

            if (password == null || password.length() == 0) {
                if (debug.messageEnabled()) {
                    debug.message("Membership.loginUser: Password is null/empty");
                } 
                
                throw new InvalidPasswordException("amAuth", "invalidPasswd", null); 
            }

            //store username password both in success and failure case
            storeUsernamePasswd(userName, password);
            initAuthConfig();
                 
            AMIdentityRepository idrepo = getAMIdentityRepository(
                getRequestOrg());
            boolean success = idrepo.authenticate(idCallbacks);
            
            if (success) {
                validatedUserID = userName;
                return ModuleState.COMPLETE;
            } else {
                throw new AuthLoginException(amAuthMembership, "authFailed", null);
            }
        } catch (IdRepoException ex) {
            if (getCredentialsFromSharedState && !isUseFirstPassEnabled()) {
                getCredentialsFromSharedState = false;
                return ModuleState.LOGIN_START;
            }		
            
            if (debug.warningEnabled()) {
                debug.warning("idRepo Exception");
            }
            setFailureID(userName);
            throw new AuthLoginException(amAuthMembership, "authFailed", null, ex);
        }
    }   

    /**
     * Creates user profile and sets the membership profile attributes.
     * This registration should be done after getting and checking all
     * registration fields.
     */
    private RegistrationResult registerNewUser() 
    throws AuthLoginException {
        if (debug.messageEnabled()) {
            debug.message("trying to register(create) a new user: " + userID);
        }
        
        try {
            if (userExists(userID)) {
                if (debug.messageEnabled()) {
                    debug.message("unable to register, user " + userID + " already exists");
                }
                return RegistrationResult.USER_EXISTS_ERROR;
            }
            
            Set<String> vals = new HashSet<String>();
            // set user status
            vals.add(serviceStatus);
            userAttrs.put("inetuserstatus", vals);
            createIdentity(userID,userAttrs,defaultRoles);
        } catch (SSOException ssoe) {
            debug.error("profile exception occured: ", ssoe);
            
            return RegistrationResult.PROFILE_ERROR;
            
        } catch (IdRepoException ire) {
            // log constraint violation message
            getLoginState("Membership").logFailed(ire.getMessage(),
                "CREATE_USER_PROFILE_FAILED", false, null);
            debug.error("profile exception occured: ", ire);
            
            return RegistrationResult.PROFILE_ERROR;
            
        }
        
        validatedUserID = userID;
        
        if (debug.messageEnabled()) {
            debug.message("registration is completed, created user: " + validatedUserID);
        }
        
        return RegistrationResult.NO_ERROR;
    }
    
    /**
     * Returns and checks registration fields. Returns error state for none 
     * recoverable errors, REGISTRATION for recoverable errors and DISCLAIMER if
     * completed.
     */
    private ModuleState getAndCheckRegistrationFields(Callback[] callbacks)
    throws AuthLoginException {
        // callback[0] is for user name
        // callback[1] is for new password
        // callback[2] is for confirm password        
        Map<String, Set<String>> attrs = new HashMap<String, Set<String>>();
        
        // get the value of the user name from the input form
        userID = getCallbackFieldValue(callbacks[0]);

        // check user name
        if ((userID == null) || userID.length() == 0) {
            // no user name was entered, this is required to
            // create the user's profile
            updateRegistrationCallbackFields(callbacks);
            setErrorMessage(RegistrationResult.NO_USER_NAME_ERROR, 0);
            return ModuleState.REGISTRATION;
        }
        
        //validate username using plugin if any
        validateUserName(userID, regEx);
        
        // get the passwords from the input form
        String password = getPassword((PasswordCallback)callbacks[1]);
        String confirmPassword = getPassword((PasswordCallback)callbacks[2]);
        
        // check passwords
        RegistrationResult checkPasswdResult = checkPassword(password, confirmPassword);
        if (debug.messageEnabled()) {
            debug.message("state returned from checkPassword(): " + checkPasswdResult);
        }
        
        if (!checkPasswdResult.equals(RegistrationResult.NO_ERROR)) {
            // the next state to display is returned from checkPassword
            updateRegistrationCallbackFields(callbacks);
            setErrorMessage(checkPasswdResult, 1);
            return ModuleState.REGISTRATION;
        }  
        
        // validate password using validation plugin if any
        validatePassword(confirmPassword);
        
        if (password.equals(userID)) {
            // the user name and password are the same. these fields
            // must be different
            updateRegistrationCallbackFields(callbacks);
            setErrorMessage(RegistrationResult.USER_PASSWORD_SAME_ERROR, 1);
            return ModuleState.REGISTRATION;
        }
        
        // get the registration fields, also check required fields
        for (int i = 0; i < callbacks.length; i++) {
            String attrName = getAttribute(ModuleState.REGISTRATION.intValue(), i);
            Set<String> values = getCallbackFieldValues(callbacks[i]);
            
            if (isRequired(ModuleState.REGISTRATION.intValue(), i)) {
                if (values.isEmpty()) {
                    if (debug.messageEnabled()) {
                        debug.message("Empty value for required field :" + attrName);
                    }
                    
                    updateRegistrationCallbackFields(callbacks);
                    setErrorMessage(RegistrationResult.MISSING_REQ_FIELD_ERROR, i);
                    return ModuleState.REGISTRATION;
                }
            }
            
            if (attrName != null && attrName.length() != 0) {
                attrs.put(attrName, values);
            }
        }
        
        userAttrs = attrs;
        
        // check user ID uniqueness
        try {
            if (userExists(userID)) {
                if (debug.messageEnabled()) {
                    debug.message("user ID " + userID + " already exists");
                }
                
                // get a list of user IDs from the generator
                Set generatedUserIDs = getNewUserIDs(attrs, 0);
                if (generatedUserIDs == null) {
                    // user name generator is disable
                    updateRegistrationCallbackFields(callbacks);
                    setErrorMessage(RegistrationResult.USER_EXISTS_ERROR, 0);
                    return ModuleState.REGISTRATION;
                }
                
                // get a list of user IDs that are not yet being used
                List<String> nonExistingUserIDs = getNonExistingUserIDs(generatedUserIDs);
                
                resetCallback(ModuleState.CHOOSE_USERNAMES.intValue(), 0);
                Callback[] origCallbacks = getCallback(ModuleState.CHOOSE_USERNAMES.intValue());
                ChoiceCallback origCallback = (ChoiceCallback) origCallbacks[0];
                String prompt = origCallback.getPrompt();
                createMyOwn = origCallback.getChoices()[0];
                
                nonExistingUserIDs.add(createMyOwn);                
                String[] choices = ((String[]) nonExistingUserIDs.toArray(new String[0]));
                
                ChoiceCallback callback = new ChoiceCallback(prompt, choices, 0, false);
                callback.setSelectedIndex(0);
                replaceCallback(ModuleState.CHOOSE_USERNAMES.intValue(), 0, callback);
                
                return ModuleState.CHOOSE_USERNAMES;
            }            
        } catch (SSOException pe) {
            debug.error("profile exception occured: ", pe);
            return ModuleState.PROFILE_ERROR;
            
        } catch (IdRepoException pe) {
            debug.error("profile exception occured: ", pe);
            return ModuleState.PROFILE_ERROR;           
        }
        
        return ModuleState.DISCLAIMER;
    }
    
    /**
     * Checks the passwords and returned error state or SUCCEEDED if
     * the passwords are valid.
     */
    private RegistrationResult checkPassword(String password, String confirmPassword) {
        if ((password == null) || password.length() == 0) {
            if (debug.messageEnabled()) {
                debug.message("password was missing from the form");
            }
            
            return RegistrationResult.NO_PASSWORD_ERROR;
            
        } else {
            // compare the length of the user entered password with
            // the length required
            if (password.length() < requiredPasswordLength) {
                if (debug.messageEnabled()) {
                    debug.message("password was not long enough");
                }
                
                return RegistrationResult.PASSWORD_TOO_SHORT;
            }
            
            // length OK, now make sure the user entered a confirmation
            // password
            if ((confirmPassword == null) || confirmPassword.length() == 0) {
                if (debug.messageEnabled()) {
                    debug.message("no confirmation password");
                }
                
                return RegistrationResult.NO_CONFIRMATION_ERROR;
                
            } else {
                // does the confirmation password match the actual password
                if (!password.equals(confirmPassword)) {
                    // the password and the confirmation password don't match
                    return RegistrationResult.PASSWORD_MISMATCH_ERROR;
                }
            }
        }
        
        return RegistrationResult.NO_ERROR;
    }
    
    /**
     * Returns the user choice user ID and proceed to the next state.
     */
    private ModuleState chooseUserID(Callback[] callbacks)
    throws AuthLoginException {
        ModuleState result = null;
        // callbacks[0] is the choice of the user ID
        String userChoiceID = getCallbackFieldValue(callbacks[0]);
        
        if (userChoiceID.equals(createMyOwn)) {
            return ModuleState.REGISTRATION;
            
        } else {
            String attrName = getAttribute(ModuleState.REGISTRATION.intValue(), 0);
            userID = userChoiceID;
            Set<String> values = new HashSet<String>();
            values.add(userID);
            userAttrs.put(attrName, values);
            
            result = processRegistrationResult();
        }
        
        return result;
    }
    
    /**
     * Returns the password from the PasswordCallback.
     */
    private String getPassword(PasswordCallback callback) {
        char[] tmpPassword = callback.getPassword();
        
        if (tmpPassword == null) {
            // treat a NULL password as an empty password
            tmpPassword = new char[0];
        }
        
        char[] pwd = new char[tmpPassword.length];
        System.arraycopy(tmpPassword, 0, pwd, 0, tmpPassword.length);
        
        return (new String(pwd));
    }    
    
    /**
     * Returns the input values as a <code>Set<String></code> for different types of Callback.
     * An empty <code>Set</code> will be returned if there is no value for the
     * Callback, or the Callback is not supported.
     */
    private Set<String> getCallbackFieldValues(Callback callback) {
        Set<String> values = new HashSet<String>();
        
        if (callback instanceof NameCallback) {
            String value = ((NameCallback)callback).getName();
            if (value != null && value.length() != 0) {
                values.add(value);
            }
        } else if (callback instanceof PasswordCallback) {
            String value = getPassword((PasswordCallback)callback);
            if (value != null && value.length() != 0) {
                values.add(value);
            }
        } else if (callback instanceof ChoiceCallback) {
            String[] vals = ((ChoiceCallback)callback).getChoices();
            int[] selectedIndexes = ((ChoiceCallback)callback).getSelectedIndexes();
            
            for (int i = 0; i < selectedIndexes.length; i++) {
                values.add(vals[selectedIndexes[i]]);
            }
        }
        
        return values;
    }
    
    /**
     * Returns the first input value for the given Callback.
     * Returns null if there is no value for the Callback.
     */
    private String getCallbackFieldValue(Callback callback) {
        Set<String> values = getCallbackFieldValues(callback);
        Iterator<String> it = values.iterator();
        
        if (it.hasNext()) {
            return it.next();
        }
        
        return null;
    } 
    
    /**
     * Returns a list of user IDs from the specified set of user IDs that
     * are not exist under the specified people container.
     */
    private List<String> getNonExistingUserIDs(Set<String> userIDs)
    throws IdRepoException, SSOException {
        List<String> validUserIDs = new ArrayList<String>();
        
        for (String uid : userIDs) {
            // check if user already exists with the same user ID
            if (!userExists(uid)) {
                validUserIDs.add(uid);
            }
        }
        
        return validUserIDs;
    }
    
    
    /** check if user exists */
    private boolean userExists(String userID)
        throws IdRepoException, SSOException {
        AMIdentityRepository amIdRepo = getAMIdentityRepository(
            getRequestOrg());

        IdSearchControl idsc = new IdSearchControl();
        idsc.setRecursive(true);
        idsc.setTimeOut(0);
        idsc.setAllReturnAttributes(true);
        // search for the identity
        Set results = Collections.EMPTY_SET;
        
        try {
            idsc.setMaxResults(0);
            IdSearchResults searchResults =
            amIdRepo.searchIdentities(IdType.USER, userID, idsc);
            if (searchResults != null) {
                results = searchResults.getSearchResults();
            }
        } catch (IdRepoException e) {
            if (debug.messageEnabled()) {
                debug.message("IdRepoException : Error searching "
                + " Identities with username : "
                + e.getMessage());
            }
        }

        return !results.isEmpty();
    }
    
    private void setErrorMessage(RegistrationResult error, int callback) 
    throws AuthLoginException {
        if (error.equals(RegistrationResult.PASSWORD_TOO_SHORT)) {
            String errorText = bundle.getString(error.toString());
            String msg = com.sun.identity.shared.locale.Locale.
                        formatMessage(errorText, requiredPasswordLength);
            substituteInfoText(ModuleState.REGISTRATION.intValue(), callback, msg);
        } else {
            substituteInfoText(ModuleState.REGISTRATION.intValue(), callback, bundle.getString(error.toString()));
        }
    }
    
    private void updateRegistrationCallbackFields(Callback[] submittedCallbacks) 
    throws AuthLoginException {
        Callback[] origCallbacks = getCallback(ModuleState.REGISTRATION.intValue());
        
        for (int c = 0; c < origCallbacks.length; c++) {
            if (origCallbacks[c] instanceof NameCallback) {
                NameCallback nc = (NameCallback) origCallbacks[c];
                nc.setName(((NameCallback) submittedCallbacks[c]).getName());
                replaceCallback(ModuleState.REGISTRATION.intValue(), c, nc);
            } else if (origCallbacks[c] instanceof PasswordCallback) {
                PasswordCallback pc = (PasswordCallback) origCallbacks[c];
                pc.setPassword(((PasswordCallback) submittedCallbacks[c]).getPassword());
                replaceCallback(ModuleState.REGISTRATION.intValue(), c, pc);
            } else {
                continue;
            }
        }
    }
}

