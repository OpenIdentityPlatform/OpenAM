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

import java.io.Serializable;
import java.util.Arrays;

/**
 * A license that can either be accepted or rejected. Encapsulates the license text as well as the accept/reject decision
 * from the user.
 *
 * @since 12.0.0
 */
public class License implements Serializable {

    private static final long serialVersionUID = 1l;

    private final String filename;
    private final String text;
    private boolean accepted = false;

    /**
     * Creates a new license with the given license text.
     *
     * @param filename the non-null filename of the license.
     * @param text the non-null text of the license.
     * @throws NullPointerException if the license text is null.
     */
    public License(final String filename, final String text) {
        if (filename == null) {
            throw new NullPointerException("license file name is null");
        }
        if (filename.isEmpty()) {
            throw new IllegalArgumentException("license file name is empty");
        }
        this.filename = filename;

        if (text == null) {
            throw new NullPointerException("license text is null");
        }
        if (text.isEmpty()) {
            throw new IllegalArgumentException("license text is empty");
        }
        this.text = text;
    }

    /**
     * Constructs a license by copying the given license.
     *
     * @param toCopy the license to copy.
     */
    public License(License toCopy) {
        this(toCopy.getFilename(), toCopy.getLicenseText());
    }

    /**
     * Returns true if the user has accepted the license.
     *
     * @return true if the license has been accepted.
     */
    public boolean isAccepted() {
        return accepted;
    }

    /**
     * Records that the user has accepted this license agreement.
     */
    public void accept() {
        accepted = true;
    }

    /**
     * Rejects the license agreement.
     *
     * @throws LicenseRejectedException to ensure that installation is aborted immediately.
     */
    public void reject() {
        accepted = false;
        throw new LicenseRejectedException(this);
    }

    /**
     * Gets the file name of the license.
     *
     * @return The license file name.
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Returns the full text of the license.
     *
     * @return the full text of the license.
     */
    public String getLicenseText() {
        return text;
    }

    /**
     * Returns the individual lines of the license text as an iterable stream. This can be useful for paging.
     * Note: if you want the full text, use the {@link #toString()} method.
     * @return the individual lines of the license text.
     */
    public Iterable<String> lines() {
        return Arrays.asList(text.split("\n"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof  License)) {
            return false;
        }

        License license = (License) o;

        return accepted == license.accepted && text.equals(license.text) && filename.equals(license.filename);
    }

    @Override
    public int hashCode() {
        int result = text.hashCode() + filename.hashCode();
        result = 31 * result + (accepted ? 1 : 0);
        return result;
    }

    /**
     * Returns the full text of the license.
     * @return the full text of the license.
     */
    @Override
    public String toString() {
        return filename + ": " + text;
    }
}
