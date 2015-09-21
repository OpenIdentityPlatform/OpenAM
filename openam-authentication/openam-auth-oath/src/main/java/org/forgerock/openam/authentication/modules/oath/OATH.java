/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012-2015 ForgeRock AS.
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
 *
 * Portions Copyrighted 2014 Nomura Research Institute, Ltd
 */

package org.forgerock.openam.authentication.modules.oath;

import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.modules.hotp.HOTPAlgorithm;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.ResourceBundle;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.PasswordCallback;

import org.forgerock.openam.authentication.modules.oath.plugins.DefaultSharedSecretProvider;
import org.forgerock.openam.authentication.modules.oath.plugins.SharedSecretProvider;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;

/**
 * Implements the OATH specification. OATH uses a OTP to authenticate
 * a token to the server. This class implements two of OATH's protocols for OTP
 * generation and authentication; HMAC-based One Time Password (HOTP) and
 * Time-based One Time Password (TOTP).
 */
public class OATH extends AMLoginModule {

    //debug log name
    protected Debug debug = null;

    private String UUID = null;
    private String userName = null;
    private Map options = null;
    private Map sharedState = null;
    private ResourceBundle bundle = null;

    // static attribute names
    private static final String AUTHLEVEL = "iplanet-am-auth-oath-auth-level";
    private static final String PASSWORD_LENGTH = "iplanet-am-auth-oath-password-length";
    private static final String SECRET_KEY_ATTRIBUTE_NAME = "iplanet-am-auth-oath-secret-key-attribute";
    private static final String WINDOW_SIZE = "iplanet-am-auth-oath-hotp-window-size";
    private static final String COUNTER_ATTRIBUTE_NAME = "iplanet-am-auth-oath-hotp-counter-attribute";
    private static final String TRUNCATION_OFFSET = "iplanet-am-auth-oath-truncation-offset";
    private static final String CHECKSUM = "iplanet-am-auth-oath-add-checksum";
    private static final String TOTP_TIME_STEP = "iplanet-am-auth-oath-size-of-time-step";
    private static final String TOTP_STEPS_IN_WINDOW = "iplanet-am-auth-oath-steps-in-window";
    private static final String ALGORITHM = "iplanet-am-auth-oath-algorithm";
    private static final String LAST_LOGIN_TIME_ATTRIBUTE_NAME = "iplanet-am-auth-oath-last-login-time-attribute-name";
    private static final String MIN_SECRET_KEY_LENGTH = "iplanet-am-auth-oath-min-secret-key-length";
    private static final String SHARED_SECRET_IMPLEMENTATION_CLASS = "forgerock-oath-sharedsecret-implementation-class";

    // module attribute holders
    private int passLen = 0;
    private int minSecretKeyLength = 0;
    private String secretKeyAttrName = null;
    private int windowSize = 0;
    private String counterAttrName = null;
    private String authLevel = null;
    private int truncationOffset = -1;
    private boolean checksum = false;
    private int totpTimeStep = 0;
    private int totpStepsInWindow = 0;
    private long time = 0;
    private String loginTimeAttrName = null;

    private static final int HOTP = 0;
    private static final int TOTP = 1;
    private static final int ERROR = 2;

    private static final int MIN_PASSWORD_LENGTH = 6;
    private int algorithm = 0;
    private String sharedSecretImplClass = null;

    protected String amAuthOATH = null;

    /**
     * Standard constructor sets-up the debug logging module.
     */
    public OATH() {
        amAuthOATH = "amAuthOATH";
        debug = Debug.getInstance(amAuthOATH);
    }

    /**
     * Returns the principal for this module. This class is overridden from
     * AMLoginModule.
     *
     * @return Principal of the authenticated user.
     */
    @Override
    public java.security.Principal getPrincipal() {
        if (UUID != null) {
            return new OATHPrincipal(UUID);
        }
        if (userName != null) {
            return new OATHPrincipal(userName);
        }
        return null;
    }

    /**
     * Initializes the authentication module. This function gets the modules
     * settings, and the username from the previous authentication module in
     * the chain.
     *
     * @param subject
     * @param sharedState
     * @param options
     */
    @Override
    public void init(Subject subject,
                     Map sharedState,
                     Map options) {

        if (debug.messageEnabled()) {
            debug.message("OATH::init");
        }
        this.options = options;
        this.sharedState = sharedState;
        bundle = amCache.getResBundle(amAuthOATH, getLoginLocale());

        //get module attributes
        try {
            this.authLevel = CollectionHelper.getMapAttr(options, AUTHLEVEL);
            try {
                this.passLen = Integer.parseInt(CollectionHelper.getMapAttr(options, PASSWORD_LENGTH));
            } catch (NumberFormatException e) {
                passLen = 0;
            }
            try {
                this.minSecretKeyLength = Integer.parseInt(CollectionHelper.getMapAttr(options, MIN_SECRET_KEY_LENGTH));
            } catch (NumberFormatException e) {
                minSecretKeyLength = 0;
            }
            this.secretKeyAttrName = CollectionHelper.getMapAttr(options, SECRET_KEY_ATTRIBUTE_NAME);
            this.windowSize = Integer.parseInt(CollectionHelper.getMapAttr(options, WINDOW_SIZE));
            this.counterAttrName = CollectionHelper.getMapAttr(options, COUNTER_ATTRIBUTE_NAME);
            this.truncationOffset = Integer.parseInt(CollectionHelper.getMapAttr(options, TRUNCATION_OFFSET));
            this.totpTimeStep = Integer.parseInt(CollectionHelper.getMapAttr(options, TOTP_TIME_STEP));
            this.totpStepsInWindow = Integer.parseInt(CollectionHelper.getMapAttr(options, TOTP_STEPS_IN_WINDOW));
            this.loginTimeAttrName = CollectionHelper.getMapAttr(options, LAST_LOGIN_TIME_ATTRIBUTE_NAME);
            this.sharedSecretImplClass = CollectionHelper.getMapAttr(options, SHARED_SECRET_IMPLEMENTATION_CLASS);

            String algorithm = CollectionHelper.getMapAttr(options, ALGORITHM);
            if (algorithm.equalsIgnoreCase("HOTP")) {
                this.algorithm = HOTP;
            } else if (algorithm.equalsIgnoreCase("TOTP")) {
                this.algorithm = TOTP;
            } else {
                // this will be caught when it tries to check OTP
                this.algorithm = ERROR;
            }

            String checksumVal = CollectionHelper.getMapAttr(options, CHECKSUM);
            if (checksumVal.equalsIgnoreCase("False")) {
                checksum = false;
            } else {
                checksum = true;
            }

            // set authentication level
            if (authLevel != null) {
                try {
                    setAuthLevel(Integer.parseInt(authLevel));
                } catch (Exception e) {
                    debug.error("OATH.init(): Unable to set auth level " + authLevel, e);
                }
            }
        } catch (Exception e) {
            debug.error("OATH.init(): Unable to get module attributes", e);
        }

        //get username from previous authentication
        try {
            userName = (String) sharedState.get(getUserKey());
        } catch (Exception e) {
            debug.error("OATH.init(): Unable to get username : ", e);
        }

    }

    /**
     * Processes the OTP input by the user. Checks the OTP for validity, and
     * resynchronizes the server as needed.
     *
     * @param callbacks
     * @param state
     * @return -1 for success; 0 for failure
     * @throws AuthLoginException upon any errors
     */
    @Override
    public int process(Callback[] callbacks, int state) throws AuthLoginException {
        try {
            //check for session and get username and UUID
            if (userName == null || userName.length() == 0) {
                // session upgrade case. Need to find the user ID from the old
                // session
                SSOTokenManager mgr = SSOTokenManager.getInstance();
                InternalSession isess = getLoginState("OATH").getOldSession();
                if (isess == null) {
                    throw new AuthLoginException("amAuth", "noInternalSession", null);
                }
                SSOToken token = mgr.createSSOToken(isess.getID().toString());
                UUID = token.getPrincipal().getName();
                userName = token.getProperty("UserToken");
                if (debug.messageEnabled()) {
                    debug.message("OATH.process(): Username from SSOToken : " + userName);
                }
                if (userName == null || userName.length() == 0) {
                    throw new AuthLoginException("amAuth", "noUserName", null);
                }
            }

            switch (state) {
                case ISAuthConstants.LOGIN_START:
                    // process callbacks
                    // callback[0] = Password CallBack (OTP)
                    // callback[1] = Confirmation CallBack (Submit OTP)
                    if (callbacks == null || callbacks.length != 2) {
                        throw new AuthLoginException(amAuthOATH, "authFailed", null);
                    }

                    // check password length MUST be 6 or higher according to RFC
                    if (passLen < MIN_PASSWORD_LENGTH) {
                        debug.error("OATH.process(): Password length is less than " + MIN_PASSWORD_LENGTH);
                        throw new AuthLoginException(amAuthOATH, "authFailed", null);
                    }

                    // get OTP
                    String OTP = String.valueOf(((PasswordCallback) callbacks[0]).getPassword());
                    if (StringUtils.isEmpty(OTP)) {
                        debug.error("OATH.process(): invalid OTP code");
                        setFailureID(userName);
                        throw new InvalidPasswordException("amAuth", "invalidPasswd", null);
                    }

                    if (minSecretKeyLength <= 0) {
                        debug.error("OATH.process(): Min Secret Key Length is not a valid value");
                        throw new AuthLoginException(amAuthOATH, "authFailed", null);
                    }

                    if (StringUtils.isEmpty(secretKeyAttrName)) {
                        debug.error("OATH.process():  secret key attribute name is empty");
                        throw new AuthLoginException(amAuthOATH, "authFailed", null);
                    }

                    // get Arrival time of the OTP
                    time = System.currentTimeMillis() / 1000L;

                    // check HOTP
                    if (checkOTP(OTP)) {
                        return ISAuthConstants.LOGIN_SUCCEED;
                    } else {
                        // the OTP is out of the window or incorect
                        setFailureID(userName);
                        throw new InvalidPasswordException("amAuth", "invalidPasswd", null);
                    }
            }
        } catch (SSOException e) {
            debug.error("OATH" + ".process() : " + "SSOException", e);
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        return ISAuthConstants.LOGIN_IGNORE;
    }

    /**
     * Called to cleanup the class level variables.
     */
    @Override
    public void destroyModuleState() {
        UUID = null;
        userName = null;
    }

    /**
     * Called to cleanup the class level variables that won't be used again.
     */
    @Override
    public void nullifyUsedVars() {
        options = null;
        sharedState = null;
        bundle = null;
        secretKeyAttrName = null;
        counterAttrName = null;
        authLevel = null;
        amAuthOATH = null;
        loginTimeAttrName = null;
    }

    /**
     * Checks the input OTP
     *
     * @param otp The OTP to verify
     * @return true if the OTP is valid; false if the OTP is invalid, or out of
     *         sync with server.
     * @throws AuthLoginException on any error
     */
    private boolean checkOTP(String otp) throws AuthLoginException {

        //get user id
        AMIdentity id = null;
        id = getIdentity(userName);
        if (id == null) {
            // error message already printed in the getIdentity function
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        byte[] secretKeyBytes = getSharedSecret(id);

        String otpGen = null;
        try {
            if (algorithm == HOTP) {
                /*
                 * HOTP check section
                 */
                int counter = 0;
                Set<String> counterSet = null;
                try {
                    if (counterAttrName == null || counterAttrName.isEmpty()) {
                        debug.error("OATH" +
                                ".checkOTP() : " +
                                "invalid counter attribute name : ");
                        throw new AuthLoginException(amAuthOATH, "authFailed", null);
                    }
                    counterSet = id.getAttribute(counterAttrName);
                } catch (IdRepoException e) {
                    debug.error("OATH" +
                                    ".checkOTP() : " +
                                    "error getting counter attribute : ",
                            e);
                    throw new AuthLoginException(amAuthOATH, "authFailed", null);
                } catch (SSOException e) {
                    debug.error("OATH" +
                                    ".checkOTP() : " +
                                    "error invalid repo id : " +
                                    id,
                            e);
                    throw new AuthLoginException(amAuthOATH, "authFailed", null);
                }
                //check counter value
                if (counterSet == null || counterSet.isEmpty()) {
                    //throw exception
                    debug.error("OATH" +
                            ".checkOTP() : " +
                            "Counter value is empty or null");
                    throw new AuthLoginException(amAuthOATH, "authFailed", null);
                }
                try {
                    counter = Integer.parseInt(
                            (String) (counterSet.iterator().next()));
                } catch (NumberFormatException e) {
                    debug.error("OATH" +
                                    ".checkOTP() : " +
                                    "Counter is not a valid number",
                            e);
                    throw new AuthLoginException(amAuthOATH, "authFailed", null);
                }

                //check window size
                if (windowSize < 0) {
                    debug.error("OATH" +
                            ".checkOTP() : " +
                            "Window size is not valid");
                    throw new AuthLoginException(amAuthOATH, "authFailed", null);
                }

                // we have to do counter+1 because counter is the last previous 
                //accepted counter
                counter++;

                //test the counter in the lookahead window
                for (int i = 0; i <= windowSize; i++) {
                    otpGen = HOTPAlgorithm.generateOTP(secretKeyBytes,
                            counter + i,
                            passLen,
                            checksum,
                            truncationOffset);
                    if (otpGen.equals(otp)) {
                        //OTP is correct set the counter value to counter+i
                        setCounterAttr(id, counter + i);
                        return true;
                    }
                }
            } else if (algorithm == TOTP) {
                /*
                 * TOTP check section
                 */

                //get Last login time
                Set<String> lastLoginTimeSet = null;
                try {
                    if (loginTimeAttrName == null || loginTimeAttrName.isEmpty()) {
                        debug.error("OATH" +
                                ".checkOTP() : " +
                                "invalid login time attribute name : ");
                        throw new AuthLoginException(amAuthOATH, "authFailed", null);
                    }
                    lastLoginTimeSet = id.getAttribute(loginTimeAttrName);
                } catch (IdRepoException e) {
                    debug.error("OATH" +
                                    ".checkOTP() : " +
                                    "error getting last login time attribute : ",
                            e);
                    throw new AuthLoginException(amAuthOATH, "authFailed", null);
                } catch (SSOException e) {
                    debug.error("OATH" +
                                    ".checkOTP() : " +
                                    "error invalid repo id : " +
                                    id,
                            e);
                    throw new AuthLoginException(amAuthOATH, "authFailed", null);
                }
                long lastLoginTime = 0;
                if (lastLoginTimeSet != null && !lastLoginTimeSet.isEmpty()) {
                    lastLoginTime = Long.parseLong(
                            (String) (lastLoginTimeSet.iterator().next()));
                }

                //Check TOTP values for validity
                if (lastLoginTime < 0) {
                    debug.error("OATH" +
                            ".checkOTP() : " +
                            "invalid login time value : ");
                    throw new AuthLoginException(amAuthOATH, "authFailed", null);
                }

                //must be greater than 0 or we get divide by 0, and cant be negetive
                if (totpTimeStep <= 0) {
                    debug.error("OATH" +
                            ".checkOTP() : " +
                            "invalid TOTP time step interval : ");
                    throw new AuthLoginException(amAuthOATH, "authFailed", null);
                }

                if (totpStepsInWindow < 0) {
                    debug.error("OATH" +
                            ".checkOTP() : " +
                            "invalid TOTP steps in window value : ");
                    throw new AuthLoginException(amAuthOATH, "authFailed", null);
                }

                //get Time Step
                long localTime = time;
                localTime /= totpTimeStep;

                boolean sameWindow = false;

                //check if we are in the time window to prevent 2
                //logins within the window using the same OTP

                if (lastLoginTime >= (localTime - totpStepsInWindow) &&
                        lastLoginTime <= (localTime + totpStepsInWindow)) {
                    if (debug.messageEnabled()) {
                        debug.message("OATH" +
                                ".checkOTP() : " +
                                "Logging in in the same TOTP window");
                    }
                    sameWindow = true;
                }

                String passLenStr = Integer.toString(passLen);
                otpGen = TOTPAlgorithm.generateTOTP(secretKeyBytes,
                        Long.toHexString(localTime),
                        passLenStr);
                if (otpGen.equals(otp)) {
                    setLoginTime(id, localTime);
                    return true;
                }

                for (int i = 1; i <= totpStepsInWindow; i++) {
                    long time1 = localTime + i;
                    long time2 = localTime - i;

                    //check time step after current time
                    otpGen = TOTPAlgorithm.generateTOTP(secretKeyBytes,
                            Long.toHexString(time1),
                            passLenStr);
                    if (otpGen.equals(otp)) {
                        setLoginTime(id, time1);
                        return true;
                    }

                    //check time step before current time

                    otpGen = TOTPAlgorithm.generateTOTP(secretKeyBytes,
                            Long.toHexString(time2),
                            passLenStr);
                    if (otpGen.equals(otp) && sameWindow){
                        debug.error("OATH" +
                                ".checkOTP() : " +
                                "Logging in in the same window with a OTP that is older than the current times OTP");
                        return false;
                    } else if(otpGen.equals(otp) && !sameWindow)  {
                        setLoginTime(id, time2);
                        return true;
                    }
                }

            } else {
                debug.error("OATH" +
                        ".checkOTP() : " +
                        "No OTP algorithm selected");
                throw new AuthLoginException(amAuthOATH, "authFailed", null);
            }
        } catch (Exception e) {
            debug.error("OATH" +
                            ".checkOTP() : " +
                            "checkOTP process failed : ",
                    e);
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }
        return false;
    }

    /**
     * Get the shared secret using the configured SharedSecretProvider plugin.
     * @param id  the user we are trying to login
     * @return the shared secret
     * @throws AuthLoginException if unable to get shared secret or invoke shared secret provider.
     */
    private byte[] getSharedSecret(AMIdentity id) throws AuthLoginException {
        String secretKey = null;
        byte[] secretKeyBytes = null;
        Set<String> secretKeySet = null;

        try {
            secretKeySet = id.getAttribute(secretKeyAttrName);
        } catch (IdRepoException e) {
            debug.error("OATH.getSharedSecret(): error getting secret key attribute: ", e);
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        } catch (SSOException e) {
            debug.error("OATH.getSharedSecret(): error invalid repo id: " + id, e);
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        // check secretKey attribute
        if (!CollectionUtils.isEmpty(secretKeySet)) {
            secretKey = secretKeySet.iterator().next();
        }

        // check size of key
        if (StringUtils.isEmpty(secretKey)) {
            debug.error("OATH.getSharedSecret(): Secret key is empty");
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        SharedSecretProvider sharedSecretProvider = null;
        try {
            if (!StringUtils.isEmpty(sharedSecretImplClass)) {
                sharedSecretProvider = Class.forName(sharedSecretImplClass)
                        .asSubclass(SharedSecretProvider.class).newInstance();

            } else {
                //
                debug.error("OATH.getSharedSecret(): SharedSecretProvider class is empty falling back to default implementation");
                sharedSecretProvider = new DefaultSharedSecretProvider();
            }
            debug.message("Invoking SharedSecretProvider hook using:" + sharedSecretImplClass);
            secretKeyBytes = sharedSecretProvider.getSharedSecret(secretKey);

        } catch (ClassNotFoundException e) {
            debug.error("OATH.getSharedSecret() Unable to find SharedSecretProvider Class:" + sharedSecretImplClass, e);
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        } catch (InstantiationException e) {
            debug.error("OATH.getSharedSecret()", e);
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        } catch (IllegalAccessException e) {
            debug.error("OATH.getSharedSecret()", e);
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        if (null == secretKeyBytes) {
            debug.error("OATH.getSharedSecret() SharedSecretProvider returned null value");
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        // since the minkeyLength accounts is for a hex encoded format, we need to adjust the byte length
        if ((secretKeyBytes.length*2) < minSecretKeyLength) {
            debug.error("OATH.getSharedSecret(): Secret key of length " + (secretKeyBytes.length *2)
                    + " is less than the minimum secret key length of " + minSecretKeyLength);
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }
        return secretKeyBytes;
    }


    /**
     * Gets the AMIdentity of a user with username equal to uName.
     *
     * @param uName username of the user to get.
     * @return The AMIdentity of user with username equal to uName or null
     * if error while trying to find user.
     */
    private AMIdentity getIdentity(String uName) {
        AMIdentity theID = null;
        AMIdentityRepository amIdRepo = getAMIdentityRepository(getRequestOrg());

        IdSearchControl idsc = new IdSearchControl();
        idsc.setRecursive(true);
        idsc.setAllReturnAttributes(true);
        // search for the identity
        Set<AMIdentity> results = Collections.EMPTY_SET;
        try {
            idsc.setMaxResults(0);
            IdSearchResults searchResults = amIdRepo.searchIdentities(IdType.USER, uName, idsc);
            if (searchResults != null) {
                results = searchResults.getSearchResults();
            }

            if (results == null || results.isEmpty()) {
                throw new IdRepoException("OATH.getIdentity : User " + userName + " is not found");
            } else if (results.size() > 1) {
                throw new IdRepoException("OATH.getIdentity: More than one user found for the userName: " + userName);
            }
            theID = results.iterator().next();
        } catch (IdRepoException e) {
            debug.error("OATH.getIdentity: error searching Identities with username : " + userName, e);
        } catch (SSOException e) {
            debug.error("OATH.getIdentity: AuthOATH module exception : ", e);
        }
        return theID;
    }

    /**
     * Sets the HOTP counter for a user.
     *
     * @param id      The user id to set the counter for.
     * @param counter The counter value to set the attribute too.
     * @throws AuthLoginException on any error.
     */
    private void setCounterAttr(AMIdentity id, int counter)
            throws AuthLoginException {
        Map<String, Set> map = new HashMap<String, Set>();
        Set<String> values = new HashSet<String>();
        String counterS = Integer.toString(counter);
        values.add(counterS);
        map.put(counterAttrName, values);
        try {
            id.setAttributes(map);
            id.store();
        } catch (IdRepoException e) {
            debug.error("OATH" +
                            ".setCounterAttr : " +
                            "error setting counter attribute to : " +
                            counter,
                    e);
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        } catch (SSOException e) {
            debug.error("OATH" +
                            ".setCounterAttr : " +
                            "error invalid token for id : " +
                            id,
                    e);
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }
        return;

    }

    /**
     * Sets the last login time of a user.
     *
     * @param id   The id of the user to set the attribute of.
     * @param time The time to set the attribute too.
     * @throws AuthLoginException on any error.
     */
    private void setLoginTime(AMIdentity id, long time)
            throws AuthLoginException {
        Map<String, Set> map = new HashMap<String, Set>();
        Set<String> values = new HashSet<String>();
        String timeS = Long.toString(time);
        values.add(timeS);
        map.put(loginTimeAttrName, values);
        try {
            id.setAttributes(map);
            id.store();
        } catch (IdRepoException e) {
            debug.error("OATH" +
                            ".setLoginTime : " +
                            "error setting time attribute to : " +
                            timeS,
                    e);
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        } catch (SSOException e) {
            debug.error("OATH" +
                            ".setLoginTime : " +
                            "error invalid token for id : " +
                            id,
                    e);
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }
        return;
    }
}
