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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.token.validator;

import org.forgerock.openam.sts.TokenValidationException;

import java.security.Principal;

/**
 * Defines an interface where a Principal can be obtained from an OpenAM Session. This functionality will be consumed
 * by TokenValidator instances to set the principal corresponding to a validated token if the to-be-validated token
 * does not have an obvious principal identifier (e.g. an OpenAM Session token, or an OpenIDConnect ID Token). This allows
 * the OpenAM authN context to do any principal/attribute mapping as part of the authN process, mappings that will be
 * harvested by the getPrincipal method method of the AMLoginModule. This interface will consume the /users/?_action=idFromSession
 * resource to achieve this mapping.
 */
public interface PrincipalFromSession {
    Principal getPrincipalFromSession(String sessionId) throws TokenValidationException;
}
