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

import com.google.inject.Inject;

/**
 *
 * Implementation of a LicensePresenter for displaying licenses
 * on the command-line.
 *
 * Requires the user to accept each license as they're displayed.
 *
 * Failure to accept a license will result in a {@link LicenseRejectedException}
 * which should be handled by the calling class. This implementation will not
 * show previously accepted licenses to the user if {@link CLILicensePresenter#presentLicenses(boolean)}
 * is called multiple times.
 *
 * This implementation focuses on printing out to the screen, and is
 * designed for use in the CLI Configurator, Upgrader and ssoadm Installer
 * systems.
 *
 */
public class CLILicensePresenter implements LicensePresenter {

    private final LicenseLocator licenseModule;
    private final User user;

    private static final String YES = "yes";

    /**
     * Injected Constructor
     *
     * @param licenseModule From which to draw the licenses
     * @param user From which to draw user CLI input
     */
    @Inject
    public CLILicensePresenter(LicenseLocator licenseModule, User user) {
        this.licenseModule = licenseModule;
        this.user = user;
    }

    /**
     * Present the licenses to the user - if the preAccept parameter is
     * set, then simply display them through System.out. Otherwise display each
     * one and confirm that the user agrees before displaying the next.
     *
     * Finally, check that all licenses have been agreed to before continuing.
     *
     * @param preAccept If the user has already selected to accept all displayed licenses.
     */
    public void presentLicenses(boolean preAccept) {

        LicenseSet licenses = licenseModule.getRequiredLicenses();

        if (preAccept) {
            licenses.acceptAll();
        } else {

            for (License license : licenses) {

                if (license.isAccepted()) {
                    continue;
                }

                user.show(""); // Blank line
                user.show(license.getLicenseText());

                String input = user.ask("prompt");

                if (input != null && YES.startsWith(input.toLowerCase())) {
                    license.accept();
                } else {
                    license.reject();
                }
            }
        }
    }

    /**
     * Returns a String to be displayed to the user in the event they require a prompt.
     */
    public String getNotice() {
        return user.getMessage("notice");
    }

}
