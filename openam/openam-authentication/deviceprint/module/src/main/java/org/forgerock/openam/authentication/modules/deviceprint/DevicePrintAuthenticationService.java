/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 */
/*
 * Portions Copyrighted 2013 Syntegrity.
 * Portions Copyrighted 2013 ForgeRock Inc.
 */

package org.forgerock.openam.authentication.modules.deviceprint;

import com.sun.identity.authentication.modules.hotp.HOTPService;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.authentication.modules.deviceprint.exceptions.NotUniqueUserProfileException;
import org.forgerock.openam.authentication.modules.deviceprint.model.DevicePrint;
import org.forgerock.openam.authentication.modules.deviceprint.model.UserProfile;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.servlet.http.HttpServletRequest;

/**
 * Contains the state logic for authenticating using the Device Print Module. Will parse the Device Print information
 * from the client and will compare it against the user's stored profiles to determine a match, otherwise uses the
 * HOTP authentication module to verify the user.
 */
public class DevicePrintAuthenticationService {

    private static final int SEND_OTP = 2;
    private static final int SAVE_PROFILE = 3;

    private static final String BUNDLE_NAME = "amAuthDevicePrintModule";
    private static final String DEBUG_NAME = "amAuthDevicePrint";
    private static final Debug DEBUG = Debug.getInstance(DEBUG_NAME);

    private final HttpServletRequest request;
    private final HOTPService hotpService;
    private final DevicePrintService devicePrintService;
    private final DevicePrintAuthenticationConfig devicePrintAuthenticationConfig;

    private DevicePrint currentDevicePrint;
    private UserProfile selectedUserProfile;

    /**
     * Constructs an instance of the DevicePrintAuthenticationService.
     *
     * @param request The HttpServletRequest that caused the Authentication request.
     * @param hotpService An instance of the HOTP Service.
     * @param devicePrintService An instance of the DevicePrintService.
     * @param devicePrintAuthenticationConfig An instance of the DevicePrintAuthenticationConfig.
     */
    public DevicePrintAuthenticationService(HttpServletRequest request, HOTPService hotpService,
            DevicePrintService devicePrintService, DevicePrintAuthenticationConfig devicePrintAuthenticationConfig) {
        this.request = request;
        this.hotpService = hotpService;
        this.devicePrintService = devicePrintService;
        this.devicePrintAuthenticationConfig = devicePrintAuthenticationConfig;
    }

    /**
     * {@inheritDoc}
     */
    public int process(Callback[] callbacks, int state) throws AuthLoginException {

        int nextState;

        switch (state) {
            // Initial authenticate state, so parse Device print information from client.
            case ISAuthConstants.LOGIN_START: {
                nextState = handleDevicePrintCallback();
                break;
            }
            // Device print information does not match an existing profile so need to confirm the user before
            // continuing.
            case SEND_OTP: {

                PasswordCallback otpCallback = (PasswordCallback) callbacks[0];
                ConfirmationCallback confirmationCallback = (ConfirmationCallback) callbacks[1];

                nextState = handleOTPCallback(otpCallback, confirmationCallback);
                break;
            }
            // A new Device print information profile has been accepted, so the user gets the option of saving the
            // profile or not.
            case SAVE_PROFILE: {
                //Confirm save profile jsp

                ChoiceCallback choiceCallback = (ChoiceCallback) callbacks[0];

                if (choiceCallback.getSelectedIndexes()[0] == 0) {
                    saveProfile();
                } else {
                    // no need to do anything as users doesn't want to save profile
                }

                nextState = ISAuthConstants.LOGIN_SUCCEED;
                break;
            }
            // Just for completeness.
            default: {
                throw new AuthLoginException(BUNDLE_NAME, "invalidauthstate", null);
            }
        }

        return nextState;
    }

    /**
     * Saves the user's DevicePrint profile or updates, dependant on if the profile is new or existing.
     *
     * @throws NotUniqueUserProfileException If the Device print information's id is not unique.
     */
    private void saveProfile() throws NotUniqueUserProfileException {
        if (selectedUserProfile != null) {
            devicePrintService.updateProfile(selectedUserProfile, currentDevicePrint);
        } else {
            devicePrintService.createNewProfile(currentDevicePrint);
        }
    }

    /**
     * Handles the parsing of the Device Print information from the client and verifying the information contains the
     * required attributes and if the information matches a stored user's profile.
     *
     * @return The next authentication state.
     * @throws AuthLoginException If an error occurs when saving the user's profile
     */
    private int handleDevicePrintCallback() throws AuthLoginException {

        currentDevicePrint = devicePrintService.getDevicePrint(request);

        if (!devicePrintService.hasRequiredAttributes(currentDevicePrint)) {
            // Skipping device print auth module as could not get enough data from the client browser
            DEBUG.warning("DevicePrintModule does not have all required attributes. OTP will be sent and " +
                    "profile will not be stored");
            return SEND_OTP;
        }

        if (hasValidProfile(currentDevicePrint)) {
            //update the latest login time & num times accessed
            devicePrintService.updateProfile(selectedUserProfile, currentDevicePrint);

            return ISAuthConstants.LOGIN_SUCCEED;
        } else {
            //so no profile or no matching profile
            return SEND_OTP;
        }
    }

    /**
     * Handles sending and validating the OTP code, for when a user's device print information does not match
     * an existing profile.
     *
     * @param otpCallback The OTP callback.
     * @param confirmationCallback The Confirmation callback to determine whether to send a OTP code or verify one.
     * @return The next authentication state.
     * @throws AuthLoginException If an error occurs when validating the OTP code.
     */
    private int handleOTPCallback(PasswordCallback otpCallback, ConfirmationCallback confirmationCallback)
            throws AuthLoginException {

        if (confirmationCallback.getSelectedIndex() == 1) {
            // send OTP
            return sendOTP();
        } else if (confirmationCallback.getSelectedIndex() == 0) {
            if (isOTPResponseValid(otpCallback)) {
                if (!devicePrintService.hasRequiredAttributes(currentDevicePrint)) {
                    // If could not get enough data from the client browser then don't give the user a chance to
                    // save profile.
                    return ISAuthConstants.LOGIN_SUCCEED;
                } else if (devicePrintAuthenticationConfig.getBoolean(DevicePrintAuthenticationConfig.AUTO_STORE_PROFILES)) {
                    saveProfile();
                    return ISAuthConstants.LOGIN_SUCCEED;
                }
                return SAVE_PROFILE;
            } else {
                // OTP code is not valid
                throw new AuthLoginException(BUNDLE_NAME, "otpinvalid", null);
            }
        } else {
            // For completeness.
            throw new AuthLoginException(BUNDLE_NAME, "otpcallbackconfirmationunknown", null);
        }
    }

    /**
     * Determines if the Device print information matches an existing stored profile and sets the matching selected
     * profile locally. If no match is found then the matching selected profile is null and false is returned.
     *
     * @param currentDevicePrint The current Device Print information from the client.
     * @return Whether the device print information matches an existing stored profile.
     */
    private boolean hasValidProfile(DevicePrint currentDevicePrint) {

        selectedUserProfile = devicePrintService.getBestMatchingUserProfile(currentDevicePrint);

        return selectedUserProfile != null;
    }

    /**
     * Sends the OTP code.
     *
     * @return The next authentication state.
     * @throws AuthLoginException If an error occurs whilst sending the OTP code.
     */
    private int sendOTP() throws AuthLoginException {
        hotpService.sendHOTP();
        return SEND_OTP;
    }

    /**
     * Determines if the submitted OTP code matches the OTP code that was generated and sent initially.
     *
     * @param otpCallback The OTP callback.
     * @return Whether the OTP is valid.
     */
    private boolean isOTPResponseValid(PasswordCallback otpCallback) {
        return hotpService.isValidHOTP(new String(otpCallback.getPassword()));
    }
}
