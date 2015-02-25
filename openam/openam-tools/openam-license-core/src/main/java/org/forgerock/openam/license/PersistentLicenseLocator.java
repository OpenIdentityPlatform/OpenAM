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

import java.util.ArrayList;
import java.util.List;

/**
 * Decorator license locator that returns {@link PersistentLicense} instances that remember license acceptance in a
 * log file. Delegates actual license locating to another license locator.
 *
 * @since 12.0.0
 */
public class PersistentLicenseLocator implements LicenseLocator {
    private final LicenseLocator delegate;
    private final LicenseLog log;
    private final String username;

    /**
     * Constructs the license locator with the given delegate locator, using the given log for recording license
     * acceptance, and the given user name to use to record acceptance.
     *
     * @param delegate the underlying locator to use to find licenses.
     * @param log the log to record license acceptance in.
     * @param username the user name to record acceptance against.
     */
    public PersistentLicenseLocator(LicenseLocator delegate, LicenseLog log, String username) {
        if (delegate == null) {
            throw new NullPointerException("delegate license locator is null");
        }
        if (log == null) {
            throw new NullPointerException("license log is null");
        }
        if (username == null) {
            throw new NullPointerException("username is null");
        }
        if (username.trim().isEmpty()) {
            throw new IllegalArgumentException("username is empty");
        }
        this.delegate = delegate;
        this.log = log;
        this.username = username;
    }

    /**
     * {@inheritDoc}
     *
     * @return the required licenses, wrapped in {@link PersistentLicense} wrappers that record license acceptance in
     * a persistent log.
     */
    public LicenseSet getRequiredLicenses() {
        List<License> licenses = new ArrayList<License>();
        for (License license : delegate.getRequiredLicenses()) {
            License persistentLicense = new PersistentLicense(log, license, username);
            licenses.add(persistentLicense);
        }
        return new LicenseSet(licenses);
    }
}
