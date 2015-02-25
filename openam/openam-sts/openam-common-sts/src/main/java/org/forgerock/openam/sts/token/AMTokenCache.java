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
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.token;

import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.ws.security.handler.RequestData;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenValidationException;

/**
 * Cached the OpenAM session Id obtained from successful authn requests in part of the SecurityPolicy enforcement layer
 * or STS token validation layers so that it can be pulled later and used to authenticate token creation operations.
 */
public interface AMTokenCache {
    void cacheAMSessionId(RequestData data, String sessionId) throws TokenValidationException;
    String getAMSessionId(TokenProviderParameters tokenParameters) throws TokenCreationException;
}
