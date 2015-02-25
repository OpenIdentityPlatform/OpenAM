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
 * Indicates that a license file could not be found by the license locator. This is a deployment/packaging error.
 *
 * @since 12.0.0
 */
public class MissingLicenseException extends IllegalStateException {
    private final String licenseName;
    /**
     * Initialises the exception reporting that the given named license could not be found.
     *
     * @param licenseName the name of the license or license file that could not be found.
     */
    public MissingLicenseException(String licenseName) {
        super("A required license could not be found: " + licenseName);
        this.licenseName = licenseName;
    }

    /**
     * Returns the name of the license that was rejected by the user.
     *
     * @return the rejected license name.
     */
    public String getLicenseName() {
        return licenseName;
    }
}
