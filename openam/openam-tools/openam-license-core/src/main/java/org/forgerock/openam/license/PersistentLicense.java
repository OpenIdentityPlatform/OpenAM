/*
 * Copyright 2014 ForgeRock, AS.
 *
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
 */

package org.forgerock.openam.license;

import java.util.Date;

/**
 * A {@link License} that remembers whether the user has accepted it or not in a persistent log file, to avoid
 * prompting the user to accept the license more than once.
 *
 * @since 12.0.0
 */
public class PersistentLicense extends License {
    private final LicenseLog log;
    private final String user;

    /**
     * Creates a new license with the given license text.
     *
     * @param log      a log to store the acceptance data in.
     * @param license  the license to copy.
     * @param user     the user that this license is being displayed to.
     * @throws NullPointerException if the license text is null.
     */
    public PersistentLicense(LicenseLog log, License license, String user) {
        super(license);
        this.log = log;
        this.user = user;
    }

    @Override
    public boolean isAccepted() {
        return super.isAccepted() || log.isLicenseAccepted(this, user);
    }

    @Override
    public void accept() {
        super.accept();
        log.logLicenseAccepted(this, user, new Date());
    }
}
