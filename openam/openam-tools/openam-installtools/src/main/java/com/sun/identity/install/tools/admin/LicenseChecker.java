/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: LicenseChecker.java,v 1.3 2008/06/25 05:51:17 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2014 ForgeRock AS.
 */

package com.sun.identity.install.tools.admin;

import org.forgerock.openam.install.tools.logs.DebugLog;
import org.forgerock.openam.license.CLILicensePresenter;
import org.forgerock.openam.license.LicenseLocator;
import org.forgerock.openam.license.LicenseLog;
import org.forgerock.openam.license.LicensePresenter;
import org.forgerock.openam.license.LicenseRejectedException;
import org.forgerock.openam.license.MissingLicenseException;
import org.forgerock.openam.license.PersistentLicenseLocator;
import org.forgerock.openam.license.User;

import javax.inject.Inject;

/**
 * Checks whether the user has accepted the license terms, and if not, presents them to the user (on the CLI) to
 * accept.
 */
public class LicenseChecker {

    /**
     * Message used to indicate a missing license file.
     */
    static final String MSG_ERROR_NO_LICENSE_FILE = "error_no_license_file";

    /**
     * Initial message to display before license text.
     */
    static final String MSG_LICENSE_HEADER = "license_header";

    /**
     * Used to present the license to the user.
     */
    private final LicensePresenter licensePresenter;

    /**
     * Whether all licenses have already been accepted.
     */
    private boolean accepted;

    /**
     * Interface for interacting with the user.
     */
    private final User user;

    /**
     * Log for installation debug messages.
     */
    private final DebugLog debug;

    /**
     * Constructs the license checker with the given license locator.
     *
     * @param locator used to locate required license terms.
     * @param log the log to use to record license acceptance.
     * @param user the interface to interact with the current user.
     * @param debug the debug log.
     * @throws NullPointerException if any of the arguments are null.
     * @throws IllegalArgumentException if the {@link org.forgerock.openam.license.User#getName()} is null or
     * empty.
     */
    @Inject
    public LicenseChecker(LicenseLocator locator, LicenseLog log, User user, DebugLog debug) {
        if (locator == null) {
            throw new NullPointerException("License locator is null");
        }
        if (log == null) {
            throw new NullPointerException("License log is null");
        }
        if (user == null) {
            throw new NullPointerException("User is null");
        }
        if (debug == null) {
            throw new NullPointerException("Debug log is null");
        }

        debug.log("Starting LicenseChecker");
        this.user = user;
        this.debug = debug;

        // Lookup current user name
        String userName = user.getName();
        debug.log("User Name: " + userName);

        if (userName != null && !userName.trim().isEmpty()) {
            // If a user name is known then wrap in persistence layer to avoid prompting the same user repeatedly for
            // the same license terms.
            locator = new PersistentLicenseLocator(locator, log, userName);
        } else {
            debug.log("Unable to determine username: disabling license acceptance logging");
        }

        this.licensePresenter = new CLILicensePresenter(locator, user);
    }

    /**
     * Checks whether the user has accepted all required license terms and prompts them to do so via the CLI if not.
     *
     * @return true if the user accepts all license terms, or false otherwise.
     */
    public boolean checkLicenseAcceptance() {
        if (!accepted) {
            debug.log("License not yet accepted.");

            user.tell(MSG_LICENSE_HEADER);

            try {
                licensePresenter.presentLicenses(accepted);
                accepted = true;
                debug.log("License agreement accepted by user.");
            } catch (MissingLicenseException ex) {
                debug.log("License file not found: " + ex.getLicenseName());
                user.tell(MSG_ERROR_NO_LICENSE_FILE);
                throw ex;
            } catch (LicenseRejectedException ex) {
                accepted = false;
                debug.log("User rejected license: " + ex.getRejectedLicense().getFilename());
            }
        } else {
            debug.log("License already accepted by the user");
        }

        return accepted;
    }

}
