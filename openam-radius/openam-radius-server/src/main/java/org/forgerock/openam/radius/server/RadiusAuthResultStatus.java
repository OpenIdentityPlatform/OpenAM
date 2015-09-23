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
 * Copyright 2015 ForgeRock AS.
 */
/**
 *
 */
package org.forgerock.openam.radius.server;

import com.sun.identity.authentication.AuthContext.Status;

/**
 * The result of RADIUS AUTH REQUEST
 */
public enum RadiusAuthResultStatus {
    NOT_STARTED("not_started"),
    IN_PROGRESS("in_progress"),
    SUCCESS("success"),
    FAILED("failed"),
    COMPLETED("completed"),
    RESET("reset"),
    ORG_MISMATCH("org_mismatch"),
    UNKNOWN("unknown");

    private final String statusString;

    /**
     * Constructor
     *
     * @param statusString
     *            a string describing the status. These match those found in AuthContext.Status
     */
    RadiusAuthResultStatus(String statusString) {
        this.statusString = statusString;
    }

    /**
     * Return the status string associated with a RadiusAuthResultResponse enum value.
     *
     * @return the statusString.
     */
    public String getSatusString() {
        return statusString;
    }

    /**
     * Obtain the appropriate RadiusAuthResultStatus from an AM AuthContext.Status
     *
     * @param status
     *            - an <code>AuthStatus.Status</code> that represents the status of the auth request performed by OpenAM
     *            on behalf of this RADIUS auth request.
     * @return - a RadiusAuthResultStatus representing the AuthContext.Status or UNKNOWN if the string contained within
     *         the AuthContext.Status is not recognized.
     */
    public static RadiusAuthResultStatus getResponse(Status status) {
        final String statusString = status.toString();
        for (final RadiusAuthResultStatus r : RadiusAuthResultStatus.values()) {
            if (statusString.equals(r.getSatusString())) {
                return r;
            }
        }
        return UNKNOWN;
    }
}
