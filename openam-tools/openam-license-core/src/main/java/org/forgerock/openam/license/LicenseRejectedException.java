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

/**
 * Error thrown when a user rejects the license terms. This exception indicates that installation or update should be
 * aborted immediately.
 *
 * @since 12.0.0
 */
public class LicenseRejectedException extends RuntimeException {
    private final License rejectedLicense;

    /**
     * Constructs the exception indicating that the given license was rejected by the user.
     *
     * @param rejectedLicense the license that was rejected.
     */
    public LicenseRejectedException(License rejectedLicense) {
        super("User rejected required license terms");
        this.rejectedLicense = rejectedLicense;
    }

    /**
     * Returns the license that was rejected by the user.
     *
     * @return the rejected license.
     */
    public License getRejectedLicense() {
        return rejectedLicense;
    }
}
