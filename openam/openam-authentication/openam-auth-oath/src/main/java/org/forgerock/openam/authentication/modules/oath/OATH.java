/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock Inc. All rights reserved.
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
 * "Portions Copyrighted 2012-2014 ForgeRock AS"
 */

/*
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
import javax.xml.bind.DatatypeConverter;

/**
 * Implements the OATH specification. OATH uses a OTP to authenticate
 * a token to the server. This class implements two of OATH's protocols for OTP
 * generation and authentication; HMAC-based One Time Password (HOTP) and
 * Time-based One Time Password (TOTP).
 *
 * @author jasonl
 */
public class OATH extends AMLoginModule {

    //debug log name
    protected Debug debug = null;

    private String UUID = null;
    private String userName = null;

    private Map options = null;
    private Map sharedState = null;
    private ResourceBundle bundle = null;

    //static attribute names
    private static final String AUTHLEVEL = "iplanet-am-auth-oath-auth-level";
    private static final String PASSWORD_LENGTH =
            "iplanet-am-auth-oath-password-length";
    private static final String SECRET_KEY_ATTRIBUTE_NAME =
            "iplanet-am-auth-oath-secret-key-attribute";
    private static final String WINDOW_SIZE =
            "iplanet-am-auth-oath-hotp-window-size";
    private static final String COUNTER_ATTRIBUTE_NAME =
            "iplanet-am-auth-oath-hotp-counter-attribute";
    private static final String TRUNCATION_OFFSET =
            "iplanet-am-auth-oath-truncation-offset";
    private static final String CHECKSUM = "iplanet-am-auth-oath-add-checksum";
    private static final String TOTP_TIME_STEP =
            "iplanet-am-auth-oath-size-of-time-step";
    private static final String TOTP_STEPS_IN_WINDOW =
            "iplanet-am-auth-oath-steps-in-window";
    private static final String ALGORITHM = "iplanet-am-auth-oath-algorithm";
    private static final String LAST_LOGIN_TIME_ATTRIBUTE_NAME =
            "iplanet-am-auth-oath-last-login-time-attribute-name";
    private static final String MIN_SECRET_KEY_LENGTH =
            "iplanet-am-auth-oath-min-secret-key-length";

    private int MIN_SECRET_KEY_LENGTH_DEFAULT = 32;

    //module attribute holders
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
    private int algorithm = 0;

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
                this.passLen = Integer.parseInt(
                        CollectionHelper.getMapAttr(options, PASSWORD_LENGTH));
            } catch (NumberFormatException e) {
                passLen = 0;
            }
            try {
                this.minSecretKeyLength = Integer.parseInt(
                        CollectionHelper.getMapAttr(options, MIN_SECRET_KEY_LENGTH));
            } catch (NumberFormatException e) {
                minSecretKeyLength = 0; //Default value has been delete, set to 0
            }
            this.secretKeyAttrName = CollectionHelper.getMapAttr(
                    options, SECRET_KEY_ATTRIBUTE_NAME);
            this.windowSize = Integer.parseInt(CollectionHelper.getMapAttr(
                    options, WINDOW_SIZE));
            this.counterAttrName = CollectionHelper.getMapAttr(
                    options, COUNTER_ATTRIBUTE_NAME);
            this.truncationOffset = Integer.parseInt(
                    CollectionHelper.getMapAttr(options, TRUNCATION_OFFSET));
            this.totpTimeStep = Integer.parseInt(
                    CollectionHelper.getMapAttr(options, TOTP_TIME_STEP));
            this.totpStepsInWindow = Integer.parseInt(
                    CollectionHelper.getMapAttr(options, TOTP_STEPS_IN_WINDOW));
            this.loginTimeAttrName = CollectionHelper.getMapAttr(
                    options, LAST_LOGIN_TIME_ATTRIBUTE_NAME);


            String algorithm = CollectionHelper.getMapAttr(options, ALGORITHM);
            if (algorithm.equalsIgnoreCase("HOTP")) {
                this.algorithm = HOTP;
            } else if (algorithm.equalsIgnoreCase("TOTP")) {
                this.algorithm = TOTP;
            } else {
                //this will be caught when it tries to check OTP
                this.algorithm = ERROR;
            }

            String checksumVal = CollectionHelper.getMapAttr(options, CHECKSUM);
            if (checksumVal.equalsIgnoreCase("False")) {
                checksum = false;
            } else {
                checksum = true;
            }

            //set authentication level
            if (authLevel != null) {
                try {
                    setAuthLevel(Integer.parseInt(authLevel));
                } catch (Exception e) {
                    debug.error("OATH" + ".init() : " +
                            "Unable to set auth level " + authLevel,
                            e);
                }
            }
        } catch (Exception e) {
            debug.error("OATH" + ".init() : " +
                    "Unable to get module attributes", e);
        }

        //get username from previous authentication
        try {
            userName = (String) sharedState.get(getUserKey());
        } catch (Exception e) {
            debug.error("OATH" + ".init() : " + "Unable to get username : ", e);
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
                    throw new AuthLoginException("amAuth", "noInternalSession",
                            null);
                }
                SSOToken token = mgr.createSSOToken(isess.getID().toString());
                UUID = token.getPrincipal().getName();
                userName = token.getProperty("UserToken");
                if (debug.messageEnabled()) {
                    debug.message("OATH" + ".process() : " +
                            "Username from SSOToken : " + userName);
                }

                if (userName == null || userName.length() == 0) {
                    throw new AuthLoginException("amAuth", "noUserName", null);
                }
            }

            switch (state) {
                case ISAuthConstants.LOGIN_START:
                    //process callbacks
                    //callback[0] = Password CallBack (OTP)
                    //callback[1] = Confirmation CallBack (Submit OTP)
                    if (callbacks == null || callbacks.length != 2) {
                        throw new AuthLoginException(amAuthOATH,
                                "authFailed",
                                null);
                    }

                    //get OTP
                    String OTP = String.valueOf(((PasswordCallback)
                            callbacks[0]).getPassword());
                    if (OTP == null || OTP.length() == 0) {
                        debug.error("OATH" +
                                ".process() : " +
                                "invalid OTP code");
                        setFailureID(userName);
                        throw new InvalidPasswordException("amAuth",
                                "invalidPasswd",
                                null);
                    }

                    //get Arrival time of the OTP
                    time = System.currentTimeMillis() / 1000L;

                    //check HOTP
                    if (checkOTP(OTP)) {
                        return ISAuthConstants.LOGIN_SUCCEED;
                    } else {
                        //the OTP is out of the window or incorect
                        setFailureID(userName);
                        throw new InvalidPasswordException("amAuth",
                                "invalidPasswd", null);
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
            //error message already printed in the getIdentity function
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        Set<String> secretKeySet = null;
        try {
            if (secretKeyAttrName == null || secretKeyAttrName.isEmpty()) {
                debug.error("OATH" +
                        ".checkOTP() : " +
                        "invalid secret key attribute name : ");
                throw new AuthLoginException(amAuthOATH, "authFailed", null);
            }

            secretKeySet = id.getAttribute(secretKeyAttrName);
        } catch (IdRepoException e) {
            debug.error("OATH" +
                    ".checkOTP() : " +
                    "error getting secret key attribute : ",
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

        //check secretKey attribute
        if (secretKeySet == null || secretKeySet.isEmpty()) {
            //no secretkey
            debug.error("OATH" +
                    ".checkOTP() : " +
                    "Secret key setting is empty or null");
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        String secretKey = null;
        int counter = 0;

        //get secret key
        secretKey = (String) (secretKeySet.iterator().next());

        //get rid of white space in string (messes witht he data converter)
        secretKey = secretKey.replaceAll("\\s+", "");
        //convert secretKey to lowercase
        secretKey = secretKey.toLowerCase();
        //make sure secretkey is even length
        if ((secretKey.length() % 2) != 0) {
            secretKey = "0" + secretKey;
        }

        if (minSecretKeyLength <= 0) {
            debug.error("OATH" +
                    ".checkOTP() : " +
                    " Min Secret Key Length is not a valid value");
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        //check size of key
        if (secretKey.isEmpty() || secretKey == null) {
            debug.error("OATH" +
                    ".checkOTP() : " +
                    "Secret key is not a valid value");
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        //make sure secretkey is not smaller than minSecretKeyLength
        if (secretKey.length() < minSecretKeyLength) {
            debug.error("OATH" +
                    ".checkOTP() : " +
                    "Secret key of length " + secretKey.length() + " is less than the minimum secret key length");
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        //convert secretkey hex string to hex.     
        //byte [] secretKeyBytes = hexStrToBytes(secretKey);
        byte[] secretKeyBytes = DatatypeConverter.parseHexBinary(secretKey);

        //check password length MUST be 6 or higher according to RFC
        if (passLen < 6) {
            debug.error("OATH" +
                    ".checkOTP() : " +
                    "Password length is smaller than 6");
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        String otpGen = null;
        try {
            if (algorithm == HOTP) {
                /*
                 * HOTP check section
                 */

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

                // we have to do counter+1 becasue counter is the last previous 
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
                otpGen = TOTPAlgorithm.generateTOTP(secretKey,
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
                    otpGen = TOTPAlgorithm.generateTOTP(secretKey,
                            Long.toHexString(time1),
                            passLenStr);
                    if (otpGen.equals(otp)) {
                        setLoginTime(id, time1);
                        return true;
                    }

                    //check time step before current time

                    otpGen = TOTPAlgorithm.generateTOTP(secretKey, 
                                                        Long.toHexString(time2), 
                                                        passLenStr);
                    if (otpGen.equals(otp) && sameWindow){
                        debug.error("OATH" +
                                ".checkOTP() : " +
                                "Loging in in the same window with a OTP that is older than the current times OTP");
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
     * Gets the AMIdentity of a user with username equal to uName.
     *
     * @param uName username of the user to get.
     * @return The AMIdentity of user with username equal to uName.
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
            IdSearchResults searchResults =
                    amIdRepo.searchIdentities(IdType.USER, uName, idsc);
            if (searchResults != null) {
                results = searchResults.getSearchResults();
            }

            if (results == null || results.size() != 1) {
                throw new IdRepoException("OATH" +
                        ".getIdentity : " +
                        "More than one user found");

            }

            theID = results.iterator().next();
        } catch (IdRepoException e) {
            debug.error("OATH" +
                    ".getIdentity : " +
                    "error searching Identities with username : " +
                    userName,
                    e);
        } catch (SSOException e) {
            debug.error("OATH" +
                    ".getIdentity : " +
                    "AuthOATH module exception : ",
                    e);
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
