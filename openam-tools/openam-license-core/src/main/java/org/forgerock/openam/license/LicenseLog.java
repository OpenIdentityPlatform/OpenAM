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
 * Stores a persistent log of whether a user has accepted a particular license or not to some backing store. This can
 * then be used to avoid presenting the license to the same user again in the future.
 *
 * @since 12.0.0
 */
public interface LicenseLog {
    /**
     * Logs that the given user has accepted the given license terms.
     *
     * @param license the license that the user accepted
     * @param user the user who accepted the license
     * @param acceptedDate the timestamp at which the license was accepted.
     */
    void logLicenseAccepted(License license, String user, Date acceptedDate);

    /**
     * Determines whether the given user has accepted the given license according to the log.
     *
     * @param license the license to check acceptance for.
     * @param user the user to check.
     * @return true if the user has accepted the given license terms, or false if not.
     */
    boolean isLicenseAccepted(License license, String user);
}
