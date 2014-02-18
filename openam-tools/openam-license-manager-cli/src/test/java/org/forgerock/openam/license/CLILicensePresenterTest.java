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

import java.io.ByteArrayInputStream;
import java.io.PrintStream;
import java.util.Arrays;
import static org.mockito.BDDMockito.given;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertFalse;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CLILicensePresenterTest {

    private final LicenseLocator locator = mock(LicenseLocator.class);
    private final PrintStream out = Mockito.mock(PrintStream.class);

    @BeforeMethod
    public void setup() {
        LicenseSet licenseSet = new LicenseSet(Arrays.asList(new License("Fake License")));
        given(locator.getRequiredLicenses()).willReturn(licenseSet);
    }

    @Test
    public void acceptLicensesWhenPreAccepted() {
        //given
        UserInput userInput = new UserInput(out, System.in);
        LicensePresenter manager = new CLILicensePresenter(locator, userInput);

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
        //give
        ByteArrayInputStream in = new ByteArrayInputStream("yes".getBytes());
        UserInput userInput = new UserInput(out, in);
        LicensePresenter manager = new CLILicensePresenter(locator, userInput);

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
        ByteArrayInputStream in = new ByteArrayInputStream("no".getBytes());
        UserInput userInput = new UserInput(out, in);
        LicensePresenter manager = new CLILicensePresenter(locator, userInput);

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
