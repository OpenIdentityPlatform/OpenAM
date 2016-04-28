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

package org.forgerock.oauth2.restlet;

import static java.util.Collections.singletonMap;

import java.util.Map;

import org.restlet.data.ChallengeScheme;

/**
 * Some useful constants for interacting with Restlet.
 */
public class RestletConstants {
    /**
     * A map of WWW-Authenticate challenge schemes to the name of the equivalent Restlet ChallengeScheme object.
     */
    public static final Map<String, String> SUPPORTED_RESTLET_CHALLENGE_SCHEMES =
            singletonMap(ChallengeScheme.HTTP_BASIC.getTechnicalName(), ChallengeScheme.HTTP_BASIC.getName());
}
