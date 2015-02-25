/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.openam.session.service;

import com.iplanet.sso.SSOToken;

/**
 * Implementation of this class gets executed every time when an SSO Session
 * times out (either idle or max timeout). A new instance of the timeout handler
 * is created upon session timeout. The listed methods are called just before
 * the session gets removed, so it is safe to use the passed in {@link SSOToken}
 * instances. Because of this behavior it is encouraged that implementations
 * don't run lengthy operations.
 *
 * @author Peter Major
 * @supported.all.api
 */
public interface SessionTimeoutHandler {

    /**
     * Executed on idle timeout
     *
     * @param token The {@link SSOToken} instance for the timed out session
     */
    public void onIdleTimeout(SSOToken token);

    /**
     * Executed on max timeout
     *
     * @param token The {@link SSOToken} instance for the timed out session
     */
    public void onMaxTimeout(SSOToken token);
}
