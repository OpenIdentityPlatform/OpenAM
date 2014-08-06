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

package org.forgerock.openam.rest;

/**
 * Enum for describing the default behaviour when no resource version is requested.
 */
public enum DefaultVersionBehaviour {
    /**
     * Use the highest version available.
     */
    LATEST,
    /**
     * Use the oldest version available.
     */
    OLDEST,
    /**
     * Do not attempt to default the version - requests without a version will result in an error.
     */
    NONE
}
