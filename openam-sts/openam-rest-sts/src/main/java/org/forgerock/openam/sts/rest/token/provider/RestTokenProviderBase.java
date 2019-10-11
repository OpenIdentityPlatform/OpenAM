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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.rest.token.provider;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.internal.AuthSSOToken;
import com.sun.identity.security.AdminTokenAction;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.provider.AMSessionInvalidator;
import org.forgerock.openam.sts.token.provider.TokenServiceConsumer;
import org.forgerock.openam.sts.token.validator.ValidationInvocationContext;
import org.slf4j.Logger;

import java.security.AccessController;

/**
 * Base class for rest token provider implementations.
 */
public abstract class RestTokenProviderBase<T> implements RestTokenProvider<T> {
    protected final TokenServiceConsumer tokenServiceConsumer;
    protected final AMSessionInvalidator amSessionInvalidator;
    protected final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    protected final String stsInstanceId;
    protected final String realm;
    protected final ValidationInvocationContext validationInvocationContext;
    protected final Logger logger;

    public RestTokenProviderBase(TokenServiceConsumer tokenServiceConsumer,
                                 AMSessionInvalidator amSessionInvalidator,
                                 ThreadLocalAMTokenCache threadLocalAMTokenCache,
                                 String stsInstanceId,
                                 String realm,
                                 ValidationInvocationContext validationInvocationContext,
                                 Logger logger) {
        this.tokenServiceConsumer = tokenServiceConsumer;
        this.amSessionInvalidator = amSessionInvalidator;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.stsInstanceId = stsInstanceId;
        this.realm = realm;
        this.validationInvocationContext = validationInvocationContext;
        this.logger = logger;
    }

    protected String getAdminToken() {
    	SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
    	while(token instanceof AuthSSOToken ) {
    		logger.warn("token is internal AuthSSOToken: {} try to invalidate and get SSOTokenImpl", token.getTokenID());
    		AdminTokenAction.invalid();
    		token = AccessController.doPrivileged(AdminTokenAction.getInstance());
    	}
        return token.getTokenID().toString();
    }

}