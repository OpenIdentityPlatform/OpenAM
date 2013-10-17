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
import org.forgerock.openam.authentication.modules.deviceprint.model.DevicePrint;
import org.forgerock.openam.authentication.modules.deviceprint.model.UserProfile;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.servlet.http.HttpServletRequest;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class DevicePrintAuthenticationServiceTest {

    private DevicePrintAuthenticationService devicePrintAuthenticationService;

    private HttpServletRequest request;
    private HOTPService hotpService;
    private DevicePrintService devicePrintService;
    private DevicePrintAuthenticationConfig devicePrintAuthenticationConfig;

    @BeforeMethod
    public void setUp() {

        request = mock(HttpServletRequest.class);
        hotpService = mock(HOTPService.class);
        devicePrintService = mock(DevicePrintService.class);
        devicePrintAuthenticationConfig = mock(DevicePrintAuthenticationConfig.class);

        devicePrintAuthenticationService = new DevicePrintAuthenticationService(request, hotpService,
                devicePrintService, devicePrintAuthenticationConfig);
    }

    /*

    1) first call ISAuthConstants.LOGIN_START - device print attr populated, device print info not sufficient - should return ISAuthConstants.LOGIN_SUCCEED
    2) first call ISAuthConstants.LOGIN_START - device print attr populated, with invalid stored profiles using OTP - should return 2
    3) first call ISAuthConstants.LOGIN_START - device print attr populated, with a valid stored profile - should return ISAuthConstants.LOGIN_SUCCEED

    4) second call, using OPT, 2 - request OPT to be sent - should return 2
    5) third call, using OPT, 2 - OPT code submitted, with correct code - should return 3
    6) third call, using OPT, 2 - OPT code submitted, with incorrect code - should throw exception

    7) fourth call, 3 - don't save profile - should return ISAuthConstants.LOGIN_SUCCEED, with no profile saved
    8) fourth call, 3 - save profile, having no valid previous profiles - should create new profile, return ISAuthConstants.LOGIN_SUCCEED
    9) fourth call, 3 - save profile, having a valid previous profile - should update previous profile, return ISAuthConstants.LOGIN_SUCCEED

     */

    /**
     * 1) first call ISAuthConstants.LOGIN_START - device print attr populated, device print info not sufficient - should return 2 (SEND_OPT)
     */
    @Test
    public void shouldSendOTPWhenDevicePrintInfoNotSufficient() throws AuthLoginException {

        //Given
        Callback[] callbacks = new Callback[1];
        NameCallback devicePrintCallback = mock(NameCallback.class);
        int state = ISAuthConstants.LOGIN_START;
        DevicePrint devicePrint = mock(DevicePrint.class);

        callbacks[0] = devicePrintCallback;
        given(devicePrintCallback.getName()).willReturn("DEVICE_PRINT_INFO");
        given(devicePrintService.getDevicePrint(request)).willReturn(devicePrint);
        given(devicePrintService.hasRequiredAttributes(devicePrint)).willReturn(false);

        //When
        int nextState = devicePrintAuthenticationService.process(callbacks, state);

        //Then
        assertEquals(nextState, 2);
    }

    /**
     * 2) first call ISAuthConstants.LOGIN_START - device print attr populated, with invalid stored profiles using SMS_OTP - should return 2
     */
    @Test
    public void shouldGotoOTPStateWhenNoValidMatchingStoredDevicePrintProfilesFound() throws AuthLoginException {

        //Given
        Callback[] callbacks = new Callback[1];
        NameCallback devicePrintCallback = mock(NameCallback.class);
        int state = ISAuthConstants.LOGIN_START;
        DevicePrint devicePrint = mock(DevicePrint.class);
        UserProfile selectedUserProfile = null;

        callbacks[0] = devicePrintCallback;
        given(devicePrintCallback.getName()).willReturn("DEVICE_PRINT_INFO");
        given(devicePrintService.getDevicePrint(request)).willReturn(devicePrint);
        given(devicePrintService.hasRequiredAttributes(devicePrint)).willReturn(true);
        given(devicePrintService.getBestMatchingUserProfile(devicePrint)).willReturn(selectedUserProfile);

        //When
        int nextState = devicePrintAuthenticationService.process(callbacks, state);

        //Then
        assertEquals(nextState, 2);
    }

    /**
     * 3) first call ISAuthConstants.LOGIN_START - device print attr populated, with a valid stored profile - should return ISAuthConstants.LOGIN_SUCCEED
     */
    @Test
    public void shouldLoginSuccessfullyWhenValidMatchingStoredDevicePrintProfilesFound() throws AuthLoginException {

        //Given
        Callback[] callbacks = new Callback[1];
        NameCallback devicePrintCallback = mock(NameCallback.class);
        int state = ISAuthConstants.LOGIN_START;
        DevicePrint devicePrint = mock(DevicePrint.class);
        UserProfile validStoredMatchingProfile = mock(UserProfile.class);
        UserProfile selectedUserProfile = validStoredMatchingProfile;

        callbacks[0] = devicePrintCallback;
        given(devicePrintCallback.getName()).willReturn("DEVICE_PRINT_INFO");
        given(devicePrintService.getDevicePrint(request)).willReturn(devicePrint);
        given(devicePrintService.hasRequiredAttributes(devicePrint)).willReturn(true);
        given(devicePrintService.getBestMatchingUserProfile(devicePrint)).willReturn(selectedUserProfile);

        //When
        int nextState = devicePrintAuthenticationService.process(callbacks, state);

        //Then
        assertEquals(nextState, ISAuthConstants.LOGIN_SUCCEED);
    }

    /**
     * 4) second call, using OPT, 2 - request OPT to be sent - should return 2
     */
    @Test
    public void shouldSendOTPWhenRequested() throws AuthLoginException {

        //Given
        Callback[] callbacks = new Callback[2];
        PasswordCallback smsOTPCallback = mock(PasswordCallback.class);
        ConfirmationCallback confirmationCallback = mock(ConfirmationCallback.class);
        int state = 2;

        callbacks[0] = smsOTPCallback;
        callbacks[1] = confirmationCallback;
        given(smsOTPCallback.getPassword()).willReturn(new char[0]);
        given(confirmationCallback.getSelectedIndex()).willReturn(1);

        //When
        int nextState = devicePrintAuthenticationService.process(callbacks, state);

        //Then
        verify(hotpService).sendHOTP();
        assertEquals(nextState, 2);
    }

    /**
     * 5) third call, using OPT, 2 - OPT code submitted, with correct code - should return 3
     */
    @Test
    public void shouldGotoSaveProfilePageWhenSubmittedOTPWithCorrectCode() throws AuthLoginException {

        //Given
        Callback[] callbacks = new Callback[2];
        PasswordCallback smsOTPCallback = mock(PasswordCallback.class);
        ConfirmationCallback confirmationCallback = mock(ConfirmationCallback.class);
        int state = 2;
        String otpCode = "OTPCODE";

        callbacks[0] = smsOTPCallback;
        callbacks[1] = confirmationCallback;
        given(smsOTPCallback.getPassword()).willReturn(otpCode.toCharArray());
        given(confirmationCallback.getSelectedIndex()).willReturn(0);
        given(hotpService.isValidHOTP("OTPCODE")).willReturn(true);
        given(devicePrintService.hasRequiredAttributes(Matchers.<DevicePrint>anyObject())).willReturn(true);

        //When
        int nextState = devicePrintAuthenticationService.process(callbacks, state);

        //Then
        assertEquals(nextState, 3);
    }

    /**
     * 5a) third call, using OPT, 2 - OPT code submitted, with correct code - with Auth Save Profile prop set to "true"
     */
    @Test
    public void shouldAutoSaveProfilePageWhenSubmittedOTPWithCorrectCodeWithAuthSaveProp() throws AuthLoginException {

        //Given
        Callback[] callbacks = new Callback[2];
        PasswordCallback smsOTPCallback = mock(PasswordCallback.class);
        ConfirmationCallback confirmationCallback = mock(ConfirmationCallback.class);
        int state = 2;
        String otpCode = "OTPCODE";

        callbacks[0] = smsOTPCallback;
        callbacks[1] = confirmationCallback;
        given(smsOTPCallback.getPassword()).willReturn(otpCode.toCharArray());
        given(confirmationCallback.getSelectedIndex()).willReturn(0);
        given(hotpService.isValidHOTP("OTPCODE")).willReturn(true);

        given(devicePrintService.hasRequiredAttributes(Matchers.<DevicePrint>anyObject())).willReturn(true);
        given(devicePrintAuthenticationConfig.getBoolean(DevicePrintAuthenticationConfig.AUTO_STORE_PROFILES))
                .willReturn(true);

        //When
        int nextState = devicePrintAuthenticationService.process(callbacks, state);

        //Then
        assertEquals(nextState, ISAuthConstants.LOGIN_SUCCEED);
        verify(devicePrintService).createNewProfile(Matchers.<DevicePrint>anyObject());
    }

    @Test
    public void shouldNotSaveProfileIfRequiredAttributesNotSet() throws AuthLoginException {

        //Given
        Callback[] callbacks = new Callback[2];
        PasswordCallback smsOTPCallback = mock(PasswordCallback.class);
        ConfirmationCallback confirmationCallback = mock(ConfirmationCallback.class);
        int state = 2;
        String otpCode = "OTPCODE";

        callbacks[0] = smsOTPCallback;
        callbacks[1] = confirmationCallback;
        given(smsOTPCallback.getPassword()).willReturn(otpCode.toCharArray());
        given(confirmationCallback.getSelectedIndex()).willReturn(0);
        given(hotpService.isValidHOTP("OTPCODE")).willReturn(true);

        given(devicePrintService.hasRequiredAttributes(Matchers.<DevicePrint>anyObject())).willReturn(false);
        given(devicePrintAuthenticationConfig.getBoolean(DevicePrintAuthenticationConfig.AUTO_STORE_PROFILES))
                .willReturn(true);

        //When
        int nextState = devicePrintAuthenticationService.process(callbacks, state);

        //Then
        assertEquals(nextState, ISAuthConstants.LOGIN_SUCCEED);
        verify(devicePrintService, never()).createNewProfile(Matchers.<DevicePrint>anyObject());
    }

    /**
     * 6) third call, using OPT, 2 - OPT code submitted, with incorrect code - should throw exception
     */
    @Test
    public void shouldThrowExceptionWhenSubmittedtOTPWithIncorrectErrorCode() throws AuthLoginException {

        //Given
        Callback[] callbacks = new Callback[2];
        PasswordCallback smsOTPCallback = mock(PasswordCallback.class);
        ConfirmationCallback confirmationCallback = mock(ConfirmationCallback.class);
        int state = 2;
        String otpCode = "OTPCODEWRONG";

        callbacks[0] = smsOTPCallback;
        callbacks[1] = confirmationCallback;
        given(smsOTPCallback.getPassword()).willReturn(otpCode.toCharArray());
        given(confirmationCallback.getSelectedIndex()).willReturn(0);
        given(hotpService.isValidHOTP("OTPCODEWRONG")).willReturn(false);

        //When
        boolean exceptionCaught = false;
        try {
            devicePrintAuthenticationService.process(callbacks, state);
            fail();
        } catch (AuthLoginException e) {
            exceptionCaught = true;
        }

        //Then
        assertTrue(exceptionCaught);
    }

    @Test
    public void shouldThrowExceptionIfConfirmationCallbackSubmittedWithUnknownOption() throws AuthLoginException {

        //Given
        Callback[] callbacks = new Callback[2];
        PasswordCallback smsOTPCallback = mock(PasswordCallback.class);
        ConfirmationCallback confirmationCallback = mock(ConfirmationCallback.class);
        int state = 2;

        callbacks[0] = smsOTPCallback;
        callbacks[1] = confirmationCallback;
        given(confirmationCallback.getSelectedIndex()).willReturn(2);

        //When
        boolean exceptionCaught = false;
        try {
            devicePrintAuthenticationService.process(callbacks, state);
            fail();
        } catch (AuthLoginException e) {
            exceptionCaught = true;
        }

        //Then
        assertTrue(exceptionCaught);
    }

    /**
     * 7) fourth call, 3 - don't save profile - should return ISAuthConstants.LOGIN_SUCCEED, with no profile saved
     */
    @Test
    public void shouldNotSaveUsersProfile() throws AuthLoginException {

        //Given
        Callback[] callbacks = new Callback[1];
        ChoiceCallback choiceCallback = mock(ChoiceCallback.class);
        int state = 3;

        callbacks[0] = choiceCallback;
        given(choiceCallback.getSelectedIndexes()).willReturn(new int[]{1});

        //When
        int nextState = devicePrintAuthenticationService.process(callbacks, state);

        //Then
        verifyZeroInteractions(devicePrintService);
        assertEquals(nextState, ISAuthConstants.LOGIN_SUCCEED);
    }

    /**
     * 8) fourth call, 3 - save profile, having no valid previous profiles - should create new profile, return ISAuthConstants.LOGIN_SUCCEED
     */
    @Test
    public void shouldCreateUserProfileWhenNoValidPreviousProfiles() throws AuthLoginException {

        //Given
        NameCallback devicePrintCallback = mock(NameCallback.class);
        Callback[] callbacks = new Callback[]{devicePrintCallback};
        int state = ISAuthConstants.LOGIN_START;
        DevicePrint devicePrint = mock(DevicePrint.class);
        given(devicePrintService.getDevicePrint(request)).willReturn(devicePrint);
        given(devicePrintService.hasRequiredAttributes(devicePrint)).willReturn(false);
        devicePrintAuthenticationService.process(callbacks, state);

        callbacks = new Callback[1];
        ChoiceCallback choiceCallback = mock(ChoiceCallback.class);
        state = 3;

        callbacks[0] = choiceCallback;
        given(choiceCallback.getSelectedIndexes()).willReturn(new int[]{0});

        //When
        int nextState = devicePrintAuthenticationService.process(callbacks, state);

        //Then
        verify(devicePrintService).createNewProfile(devicePrint);
        assertEquals(nextState, ISAuthConstants.LOGIN_SUCCEED);
    }

    /**
     * 9) fourth call, 3 - save profile, having a valid previous profile - should update previous profile, return ISAuthConstants.LOGIN_SUCCEED
     */
    @Test
    public void shouldUpdateUserProfileWhenHasValidPreviousProfile() throws AuthLoginException {

        //Given
        NameCallback devicePrintCallback = mock(NameCallback.class);
        Callback[] callbacks = new Callback[]{devicePrintCallback};
        int state = ISAuthConstants.LOGIN_START;
        DevicePrint devicePrint = mock(DevicePrint.class);
        UserProfile validStoredMatchingProfile = mock(UserProfile.class);
        UserProfile selectedUserProfile = validStoredMatchingProfile;
        given(devicePrintService.getDevicePrint(request)).willReturn(devicePrint);
        given(devicePrintService.hasRequiredAttributes(devicePrint)).willReturn(true);
        given(devicePrintService.getBestMatchingUserProfile(devicePrint)).willReturn(selectedUserProfile);

        //When
        int nextState = devicePrintAuthenticationService.process(callbacks, state);

        //Then
        verify(devicePrintService).updateProfile(selectedUserProfile, devicePrint);
        assertEquals(nextState, ISAuthConstants.LOGIN_SUCCEED);
    }

    @Test
    public void shouldThrowExceptionStateIsNotKnown() throws AuthLoginException {

        //Given
        Callback[] callbacks = new Callback[0];
        int state = 4;

        //When
        boolean exceptionCaught = false;
        try {
            devicePrintAuthenticationService.process(callbacks, state);
            fail();
        } catch (AuthLoginException e) {
            exceptionCaught = true;
        }

        //Then
        assertTrue(exceptionCaught);
    }
}
