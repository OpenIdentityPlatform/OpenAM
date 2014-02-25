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
* Copyright 2014 ForgeRock AS.
*/

package org.forgerock.openam.license;

import java.util.Arrays;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertFalse;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CLILicensePresenterTest {

    private final LicenseLocator locator = mock(LicenseLocator.class);
    private User user;

    @BeforeMethod
    public void setup() {
        LicenseSet licenseSet = new LicenseSet(Arrays.asList(new License("...", "Fake License")));
        given(locator.getRequiredLicenses()).willReturn(licenseSet);
        user = mock(User.class);
    }

    @Test
    public void acceptLicensesWhenPreAccepted() {
        //given
        LicensePresenter manager = new CLILicensePresenter(locator, user);

        boolean error = false;

        //when
        try {
            manager.presentLicenses(true);
        } catch(LicenseRejectedException lre) {
            error = true;
        }

        //then
        assertFalse(error);
    }

    @Test
    public void acceptLicensesWhenUserInput() {
        //given
        given(user.ask(anyString())).willReturn("y");
        LicensePresenter manager = new CLILicensePresenter(locator, user);

        boolean error = false;

        //when
        try {
            manager.presentLicenses(false);
        } catch(LicenseRejectedException lre) {
            error = true;
        }

        //then
        assertFalse(error);
    }

    @Test
    public void presentLicensesErrorsWhenNotAcceptLicense() {
        //given
        given(user.ask(anyString())).willReturn("no");
        LicensePresenter manager = new CLILicensePresenter(locator, user);

        boolean error = false;

        //when
        try {
            manager.presentLicenses(false);
        } catch(LicenseRejectedException lre) {
            error = true;
        }

        //then
        assertTrue(error);
    }

}
