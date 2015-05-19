/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2012-2015 ForgeRock AS.
 */

/*
 * Portions Copyrighted 2014-2015 Nomura Research Institute, Ltd.
 */

package org.forgerock.openam.authentication.modules.oath;

import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import com.sun.identity.authentication.modules.hotp.HOTPAlgorithm;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.xml.bind.DatatypeConverter;
import org.apache.commons.codec.DecoderException;
import org.forgerock.openam.utils.qr.GenerationUtils;

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
    private static final String USER_OATH_ACTIVATED_ATTRIBUTE_NAME =
            "iplanet-am-auth-oath-skippable-attr-name";
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

    //module attribute holders
    private int userConfiguredSkippable = 0;
    private String skippableAttrName = null;
    private boolean isOptional;
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

    private static final int LOGIN_START = ISAuthConstants.LOGIN_START;
    private static final int LOGIN_OPTIONAL = 2;
    private static final int LOGIN_NORMAL = 3;
    private static final int REGISTER_DEVICE = 4;

    private static final int SKIP_OATH_INDEX = 2;
    private static final int REGISTER_DEVICE_OPTION_VALUE_INDEX = 1;
    private static final int LOGIN_OPTION_VALUE_INDEX = 0;

    private static final int SCRIPT_OUTPUT_CALLBACK_INDEX = 1;
    private static final int CONFIRMATION_CALLBACK_INDEX = 2;

    private static final int NOT_SET = 0;
    private static final int SKIPPABLE = 1;
    private static final int NOT_SKIPPABLE = 2;


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
     * @param subject For whom this module is initializing.
     * @param sharedState Previously chained module data.
     * @param options Configuration for this module.
     */
    @Override
    public void init(Subject subject, Map sharedState, Map options) {

        if (debug.messageEnabled()) {
            debug.message("OATH::init");
        }

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

            this.skippableAttrName = CollectionHelper.getMapAttr(options, USER_OATH_ACTIVATED_ATTRIBUTE_NAME);

            this.secretKeyAttrName = CollectionHelper.getMapAttr(
                    options, SECRET_KEY_ATTRIBUTE_NAME);
            this.windowSize = Integer.parseInt(CollectionHelper.getMapAttr(
                    options, WINDOW_SIZE));
            this.counterAttrName = CollectionHelper.getMapAttr(
                    options, COUNTER_ATTRIBUTE_NAME);
            this.truncationOffset = Integer.parseInt(
                    CollectionHelper.getMapAttr(options, TRUNCATION_OFFSET));

            this.isOptional = !getLoginState("OATH").is2faMandatory();

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
                this.algorithm = ERROR;
            }

            String checksumVal = CollectionHelper.getMapAttr(options, CHECKSUM);
            checksum = !checksumVal.equalsIgnoreCase("False");

            //set authentication level
            if (authLevel != null) {
                try {
                    setAuthLevel(Integer.parseInt(authLevel));
                } catch (Exception e) {
                    if (debug.errorEnabled()) {
                        debug.error("OATH" + ".init() : Unable to set auth level " + authLevel, e);
                    }
                }
            }
        } catch (Exception e) {
            debug.error("OATH.init() : Unable to get module attributes", e);
        }

        //get username from previous authentication
        try {
            userName = (String) sharedState.get(getUserKey());
        } catch (Exception e) {
            debug.error("OATH.init() : Unable to get username : ", e);
        }

    }

    /**
     * Processes the OTP input by the user. Checks the OTP for validity, and
     * resynchronizes the server as needed.
     *
     * @param callbacks Incoming from the UI.
     * @param state State of the module to process this access.
     * @return -1 for success; 0 for failure, any other int to move to that state.
     * @throws AuthLoginException upon any errors.
     */
    @Override
    public int process(Callback[] callbacks, int state) throws AuthLoginException {
        try {
            //check for session and get username and UUID
            if (userName == null || userName.length() == 0) {
                // session upgrade case. Need to find the user ID from the old
                SSOTokenManager mgr = SSOTokenManager.getInstance();
                InternalSession isess = getLoginState("OATH").getOldSession();
                if (isess == null) {
                    throw new AuthLoginException("amAuth", "noInternalSession", null);
                }
                SSOToken token = mgr.createSSOToken(isess.getID().toString());
                UUID = token.getPrincipal().getName();
                userName = token.getProperty("UserToken");
                if (debug.messageEnabled()) {
                    debug.message("OATH.process() : Username from SSOToken : " + userName);
                }

                if (userName == null || userName.length() == 0) {
                    throw new AuthLoginException("amAuth", "noUserName", null);
                }
            }

            final AMIdentity id = getIdentity();

            try {
                detectNecessity(id); //figures out whether we're optional or not, based on server + user setting
            } catch (Exception e) {
                throw new AuthLoginException(amAuthOATH, "authFailed", null);
            }

            switch (state) {
                case LOGIN_START:

                    if (!isOptional) {
                        return LOGIN_NORMAL;
                    } else if (userConfiguredSkippable == SKIPPABLE) { //isOptional and set to be skipped
                        return ISAuthConstants.LOGIN_SUCCEED;
                    } else if (userConfiguredSkippable == NOT_SET) { //isOptional and not set
                        return LOGIN_OPTIONAL;
                    } else { //isOptional and set to utilise
                        return LOGIN_NORMAL;
                    }

                case LOGIN_OPTIONAL:
                    //process callbacks
                    //callback[0] = Password CallBack (OTP)
                    //callback[1] = Confirmation CallBack (Submit OTP/Register device)
                    //callback[2] = Configure account to skip OATH
                    if (callbacks == null) {
                        throw new AuthLoginException(amAuthOATH, "authFailed", null);
                    }

                    int loginIndex = ((ConfirmationCallback) callbacks[1]).getSelectedIndex();
                    if (loginIndex == SKIP_OATH_INDEX) {
                        setUserSkipOath(id, true);
                        return ISAuthConstants.LOGIN_SUCCEED;
                    }

                    //fall through is INTENTIONAL HERE
                case LOGIN_NORMAL:

                    int selectedIndex = ((ConfirmationCallback) callbacks[1]).getSelectedIndex();
                    if (selectedIndex == REGISTER_DEVICE_OPTION_VALUE_INDEX) {
                        substituteUIStrings(id);

                        return REGISTER_DEVICE;
                    }

                    //get OTP
                    String OTP = String.valueOf(((PasswordCallback) callbacks[0]).getPassword());
                    if (OTP.length() == 0) {
                        debug.error("OATH.process() : invalid OTP code");
                        setFailureID(userName);
                        throw new InvalidPasswordException("amAuth", "invalidPasswd", null);
                    }

                    //get Arrival time of the OTP
                    time = System.currentTimeMillis() / 1000L;

                    //check HOTP
                    if (checkOTP(OTP, id)) {
                        return ISAuthConstants.LOGIN_SUCCEED;
                    } else {
                        //the OTP is out of the window or incorrect
                        setFailureID(userName);
                        throw new InvalidPasswordException("amAuth", "invalidPasswd", null);
                    }
                case REGISTER_DEVICE:

                    if (callbacks == null) {
                        throw new AuthLoginException(amAuthOATH, "authFailed", null);
                    }

                    int registerIndex = ((ConfirmationCallback) callbacks[CONFIRMATION_CALLBACK_INDEX]).getSelectedIndex();
                    if (registerIndex == LOGIN_OPTION_VALUE_INDEX) {
                        setUserSkipOath(id, false);
                        return LOGIN_START;
                    } else {
                        throw new AuthLoginException(amAuthOATH, "authFailed", null);
                    }
                default:
                    throw new AuthLoginException("amAuth", "invalidLoginState", new Object[]{state});
            }
        } catch (SSOException e) {
            debug.error("OATH.process() : SSOException", e);
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        } catch (IdRepoException e) {
            debug.error("OATH.process() : SSOException", e);
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }
    }

    /**
     * Sets the state of this OATH module such that it has all necessary information to proceed, informed
     * of its setup as a optional/enforced module.
     *
     * First checks the state of the auth module.
     *
     * If auth module is not optional, then default flow.
     * If auth module is optional, then read the state of the user's selection (via the userActivatedAttrName attr.)
     * If they have it activated, then default flow (this is required).
     * If they have it blank, then default flow (this is required).
     * If they have it set to 'can ignore' (e.g. userCanIgnoreAttrName's value is set to "true"), then ignore.
     */
    private void detectNecessity(AMIdentity identity) throws IdRepoException, SSOException {

        //not optional if they haven't selected anywhere to save the user's preference
        if (isOptional && skippableAttrName == null) {
            isOptional = false;
        }

        //value is stored as "true" or "false" or is null.
        if (skippableAttrName != null) {
            Set response = identity.getAttribute(skippableAttrName);
            if (response != null && !response.isEmpty()) { //sets skippable to true if set in user
                String tmp = (String) response.iterator().next();
                userConfiguredSkippable = Integer.valueOf(tmp);
            }
        }

    }

    private void substituteUIStrings(AMIdentity id) throws AuthLoginException {
        replaceCallback(REGISTER_DEVICE, SCRIPT_OUTPUT_CALLBACK_INDEX, createQRCodeCallback(id));
    }

    /**
    * There is a hack here to reverse a hack in RESTLoginView.js. Implementing the code properly in RESTLoginView.js so
    * as to remove this hack will take too long at present, and stands in the way of completion of this module's
    * QR code additions. I have opted to simply reverse the hack in this singular case.
    *
    * In the below code returning the ScriptTextOutputCallback, the String used in its construction is
    * defined as follows:
     *
    * createQRDomElementJS
    *           Adds the DOM element, in this case a div, in which the QR code will appear.
    * QRCodeGenerationUtilityFunctions.
    *   getQRCodeGenerationJavascriptForAuthenticatorAppRegistration(authenticatorAppRegistrationUri)
    *           Adds a specific call to the Javascript library code, sending the app registration url as the
    *           text to encode as a QR code. This QR code will then appear in the previously defined DOM
    *           element (which must have an id of 'qr').
    * hideButtonHack
    *           A hack to reverse a hack in RESTLoginView.js. See more detailed comment above.
    */
    private Callback createQRCodeCallback(AMIdentity id) throws AuthLoginException {

        String hideButtonHack = "if(document.getElementsByClassName('button')[0] != undefined){document" +
                ".getElementsByClassName('button')[0].style.visibility = 'visible';}\n";

        String createQRDomElementJS = getCreateQRDomElementJS();
        String authenticatorAppRegistrationUri = getAuthenticatorAppRegistrationUri(id);

        return new ScriptTextOutputCallback(createQRDomElementJS +
                GenerationUtils.
                        getQRCodeGenerationJavascriptForAuthenticatorAppRegistration(authenticatorAppRegistrationUri) +
                hideButtonHack);

    }

    /**
    * Because of the lack of support for HTML DOM element output in the XUI, we have to do this faff to get a div in
    * there, which we can then populate with the constructed QR image.
    *
    * In the returned String, the Javascript if statement is used to handle presentation in the classic UI.
    */
    private String getCreateQRDomElementJS() {
        String qrImagePositioningCss = "node.style.marginTop='20px';";
        String qrImageCentralizationCss = "node.style.textAlign='center';";

        return "var node = document.createElement('div'); node.id='qr'; "
                + qrImagePositioningCss + qrImageCentralizationCss +
                "if(document.getElementById('callback_0') != undefined)" +
                "{document.getElementById('callback_0').appendChild(node);}" +
                "else" +
                "{document.getElementsByClassName('TextOutputCallback_0')[0].appendChild(node);}\n";

    }

    private String getAuthenticatorAppRegistrationUri(AMIdentity id) throws AuthLoginException {
        String secretHex = getSecretKey(id);

        AuthenticatorAppRegistrationURIBuilder builder = new AuthenticatorAppRegistrationURIBuilder(id, secretHex);

        int algorithm = this.algorithm;
        try {
            if (algorithm == HOTP) {
                int counter = getCounter(id);
                return builder.getAuthenticatorAppRegistrationUriForHOTP(counter);
            } else if (algorithm == TOTP) {
                return builder.getAuthenticatorAppRegistrationUriForTOTP(totpTimeStep);
            } else {
                debug.error("OATH .checkOTP() : No OTP algorithm selected");
                throw new AuthLoginException(amAuthOATH, "authFailed", null);
            }
        } catch (DecoderException de) {
            debug.error("OATH .getCreateQRDomElementJS() : Could not decode secret key from hex to plain text", de);
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }
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
    private boolean checkOTP(String otp, AMIdentity id) throws AuthLoginException {
        if (id == null) {
            debug.error("OATH.checkOTP() : Identity was null.");
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        String secretKey = getSecretKey(id);

        if (minSecretKeyLength <= 0) {
            debug.error("OATH.checkOTP() : Min Secret Key Length is not a valid value");
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        //check size of key
        if (secretKey == null || secretKey.isEmpty()) {
            debug.error("OATH.checkOTP() : Secret key is not a valid value");
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        //make sure secretkey is not smaller than minSecretKeyLength
        if (secretKey.length() < minSecretKeyLength) {
            if (debug.errorEnabled()) {
                debug.error("OATH.checkOTP() : Secret key of length "
                        + secretKey.length() + " is less than the minimum secret key length");
            }
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        //convert secretkey hex string to hex.     
        byte[] secretKeyBytes = DatatypeConverter.parseHexBinary(secretKey);

        //check password length MUST be 6 or higher according to RFC
        if (passLen < 6) {
            debug.error("OATH.checkOTP() : Password length is smaller than 6");
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        String otpGen;
        try {
            if (algorithm == HOTP) {
                /*
                 * HOTP check section
                 */

                int counter = getCounter(id);

                // we have to do counter+1 because counter is the last previous accepted counter
                counter++;

                //test the counter in the lookahead window
                for (int i = 0; i <= windowSize; i++) {
                    otpGen = HOTPAlgorithm.generateOTP(secretKeyBytes, counter + i, passLen, checksum,
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
                Set<String> lastLoginTimeSet;
                try {
                    if (loginTimeAttrName == null || loginTimeAttrName.isEmpty()) {
                        debug.error("OATH.checkOTP() : invalid login time attribute name : ");
                        throw new AuthLoginException(amAuthOATH, "authFailed", null);
                    }
                    lastLoginTimeSet = id.getAttribute(loginTimeAttrName);
                } catch (IdRepoException e) {
                    debug.error("OATH.checkOTP() : error getting last login time attribute : ", e);
                    throw new AuthLoginException(amAuthOATH, "authFailed", null);
                } catch (SSOException e) {
                    debug.error("OATH.checkOTP() : error invalid repo id : " + id, e);
                    throw new AuthLoginException(amAuthOATH, "authFailed", null);
                }
                long lastLoginTime = 0;
                if (lastLoginTimeSet != null && !lastLoginTimeSet.isEmpty()) {
                    lastLoginTime = Long.parseLong(lastLoginTimeSet.iterator().next());
                }

                //Check TOTP values for validity
                if (lastLoginTime < 0) {
                    debug.error("OATH.checkOTP() : invalid login time value : ");
                    throw new AuthLoginException(amAuthOATH, "authFailed", null);
                }

                //must be greater than 0 or we get divide by 0, and cant be negetive
                if (totpTimeStep <= 0) {
                    debug.error("OATH.checkOTP() : invalid TOTP time step interval : ");
                    throw new AuthLoginException(amAuthOATH, "authFailed", null);
                }

                if (totpStepsInWindow < 0) {
                    debug.error("OATH.checkOTP() : invalid TOTP steps in window value : ");
                    throw new AuthLoginException(amAuthOATH, "authFailed", null);
                }

                //get Time Step
                long localTime = time;
                localTime /= totpTimeStep;

                boolean sameWindow = false;

                //check if we are in the time window to prevent 2 logins within the window using the same OTP

                if (lastLoginTime >= (localTime - totpStepsInWindow) &&
                        lastLoginTime <= (localTime + totpStepsInWindow)) {
                    if (debug.messageEnabled()) {
                        debug.message("OATH.checkOTP() : Logging in in the same TOTP window");
                    }
                    sameWindow = true;
                }

                String passLenStr = Integer.toString(passLen);
                otpGen = TOTPAlgorithm.generateTOTP(secretKey, Long.toHexString(localTime), passLenStr);

                if (otpGen.equals(otp)) {
                    setLoginTime(id, localTime);
                    return true;
                }

                for (int i = 1; i <= totpStepsInWindow; i++) {
                    long time1 = localTime + i;
                    long time2 = localTime - i;

                    //check time step after current time
                    otpGen = TOTPAlgorithm.generateTOTP(secretKey, Long.toHexString(time1), passLenStr);

                    if (otpGen.equals(otp)) {
                        setLoginTime(id, time1);
                        return true;
                    }

                    //check time step before current time
                    otpGen = TOTPAlgorithm.generateTOTP(secretKey, Long.toHexString(time2), passLenStr);

                    if (otpGen.equals(otp) && sameWindow){
                        debug.error("OATH.checkOTP() : Logging in in the same window with a OTP that is older " +
                                "than the current times OTP");
                        return false;
                    } else if(otpGen.equals(otp) && !sameWindow)  {
                        setLoginTime(id, time2);
                        return true;
                    }
                }

            } else {
                debug.error("OATH.checkOTP() : No OTP algorithm selected");
                throw new AuthLoginException(amAuthOATH, "authFailed", null);
            }
        } catch (Exception e) {
            debug.error("OATH.checkOTP() : checkOTP process failed : ", e);
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }
        return false;
    }

    private String getSecretKey(AMIdentity id) throws AuthLoginException {
        Set<String> secretKeySet = getSecretKeySet(id);
        String secretKey = secretKeySet.iterator().next();

        //get rid of white space in string (messes with the data converter)
        secretKey = secretKey.replaceAll("\\s+", "");
        //convert secretKey to lowercase
        secretKey = secretKey.toLowerCase();
        //make sure secretkey is even length
        if ((secretKey.length() % 2) != 0) {
            secretKey = "0" + secretKey;
        }

        return secretKey;
    }

    private Set<String> getSecretKeySet(AMIdentity id) throws AuthLoginException {
        Set<String> secretKeySet;
        try {
            if (secretKeyAttrName == null || secretKeyAttrName.isEmpty()) {
                debug.error("OATH .getSecretKeySet() : invalid secret key attribute name : null or empty string");
                throw new AuthLoginException(amAuthOATH, "authFailed", null);
            }

            secretKeySet = id.getAttribute(secretKeyAttrName);
        } catch (IdRepoException e) {
            debug.error("OATH .getSecretKeySet() : error getting secret key attribute : ",
                    e);
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        } catch (SSOException e) {
            debug.error("OATH .getSecretKeySet() : error invalid repo id : " + id, e);
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        //check secretKey attribute
        if (secretKeySet == null || secretKeySet.isEmpty()) {
            debug.error("OATH.getSecretKeySet() : Secret key setting is empty or null");
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        return secretKeySet;
    }

    private int getCounter(AMIdentity id) throws AuthLoginException {
        int counter;

        Set<String> counterSet = getCounterSet(id);

        try {
            counter = Integer.parseInt((counterSet.iterator().next()));
        } catch (NumberFormatException e) {
            debug.error("OATH .getCounter() : Counter is not a valid number", e);
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        //check window size
        if (windowSize < 0) {
            debug.error("OATH .getCounter() : Window size is not valid");
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        return counter;
    }

    private Set<String> getCounterSet(AMIdentity id) throws AuthLoginException {
        Set<String> counterSet;
        try {
            if (counterAttrName == null || counterAttrName.isEmpty()) {
                debug.error("OATH .getCounterSet() : invalid counter attribute name : null or empty string");
                throw new AuthLoginException(amAuthOATH, "authFailed", null);
            }
            counterSet = id.getAttribute(counterAttrName);
        } catch (IdRepoException e) {
            debug.error("OATH .getCounterSet() : error getting counter attribute : ", e);
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        } catch (SSOException e) {
            debug.error("OATH .getCounterSet() : error invalid repo id : " + id, e);
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }
        //check counter value
        if (counterSet == null || counterSet.isEmpty()) {
            debug.error("OATH .getCounterSet() : Counter value is empty or null");
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }

        return counterSet;
    }

    /**
     * Gets the AMIdentity of a user with username equal to uName.
     *
     * @return The AMIdentity of user with username equal to uName.
     */
    private AMIdentity getIdentity() {
        AMIdentity theID = null;
        AMIdentityRepository amIdRepo = getAMIdentityRepository(getRequestOrg());

        IdSearchControl idsc = new IdSearchControl();
        idsc.setRecursive(true);
        idsc.setAllReturnAttributes(true);
        // search for the identity
        Set<AMIdentity> results = Collections.EMPTY_SET;
        try {
            idsc.setMaxResults(0);
            IdSearchResults searchResults = amIdRepo.searchIdentities(IdType.USER, userName, idsc);
            if (searchResults != null) {
                results = searchResults.getSearchResults();
            }
            if (results.isEmpty()) {
                debug.error("OATH.getIdentity : User " + userName + " is not found");
            } else if (results.size() > 1) {
                debug.error("OATH.getIdentity : More than one user found for the userName " + userName);
            } else {
                theID = results.iterator().next();
            }
        } catch (IdRepoException e) {
            debug.error("OATH.getIdentity : Error searching Identities with username : " + userName, e);
        } catch (SSOException e) {
            debug.error("OATH.getIdentity : Module exception : ", e);
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
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        map.put(counterAttrName, Collections.singleton(Integer.toString(counter)));
        try {
            id.setAttributes(map);
            id.store();
        } catch (IdRepoException e) {
            if (debug.errorEnabled()) {
                debug.error("OATH.setCounterAttr : error setting counter attribute to : " + counter, e);
            }
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        } catch (SSOException e) {
            if (debug.errorEnabled()) {
                debug.error("OATH.setCounterAttr : error invalid token for id : " + id, e);
            }
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }
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
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();

        map.put(loginTimeAttrName, map.put(counterAttrName, Collections.singleton(Long.toString(time))));
        try {
            id.setAttributes(map);
            id.store();
        } catch (IdRepoException e) {
            if (debug.errorEnabled()) {
                debug.error("OATH.setLoginTime : error setting time attribute to : " + time, e);
            }
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        } catch (SSOException e) {
            if (debug.errorEnabled()) {
                debug.error("OATH.setLoginTime : error invalid token for id : " + id, e);
            }
            throw new AuthLoginException(amAuthOATH, "authFailed", null);
        }
    }

    private void setUserSkipOath(AMIdentity id, boolean userSkipOath) throws IdRepoException, SSOException {
        final HashMap<String, Set<String>> attributesToWrite = new HashMap<String, Set<String>>();
        attributesToWrite.put(skippableAttrName,
                userSkipOath ?
                        Collections.singleton(String.valueOf(SKIPPABLE)) :
                        Collections.singleton(String.valueOf(NOT_SKIPPABLE)));
        id.setAttributes(attributesToWrite);
        id.store();
    }
}
