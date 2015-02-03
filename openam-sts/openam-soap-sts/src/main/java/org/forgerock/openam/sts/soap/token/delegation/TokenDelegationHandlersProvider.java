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

package org.forgerock.openam.sts.soap.token.delegation;

import org.apache.cxf.sts.token.delegation.TokenDelegationHandler;
import org.forgerock.guava.common.collect.Lists;
import org.forgerock.openam.sts.soap.config.user.SoapSTSInstanceConfig;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Returns a List of TokenDelegationHandler instances which will be invoked when delegated tokens are encountered in the
 * issue operation (due to the presence of ActAs or OnBehalfOf elements). Provides the DefaultTokenDelegationHandler
 * if appropriate, and wraps any user-specified implementations of the TokenDelegationHandler interface in the
 * CustomDelegationHandlerWrapper to set AMSession id in the ThreadLocalAMTokenCache.
*/
public class TokenDelegationHandlersProvider implements Provider<List<TokenDelegationHandler>> {
    private final SoapSTSInstanceConfig stsInstanceConfig;
    private final ThreadLocalAMTokenCache threadLocalAMTokenCache;
    private final Logger logger;

    @Inject
    TokenDelegationHandlersProvider(SoapSTSInstanceConfig stsInstanceConfig, ThreadLocalAMTokenCache threadLocalAMTokenCache, Logger logger) {
        this.stsInstanceConfig = stsInstanceConfig;
        this.threadLocalAMTokenCache = threadLocalAMTokenCache;
        this.logger = logger;
    }

    public List<TokenDelegationHandler> get() {
        if (!stsInstanceConfig.delegationRelationshipsSupported()) {
            return Collections.emptyList();
        } else {
            if (stsInstanceConfig.getSoapDelegationConfig().getCustomDelegationTokenHandlers().isEmpty()) {
                /*
                Entering this branch means that the user did not specify the class names of any TokenDelegationHandler
                implementations, so I will simply return the List containing and instance of the DefaultTokenDelegationHandler,
                which will approve the delegation token if it has been previously validated.
                 */
                return (List) Lists.newArrayList(new DefaultTokenDelegationHandler());
            } else {
                List customDelegationHandlers = new ArrayList<TokenDelegationHandler>();
                for (String className : stsInstanceConfig.getSoapDelegationConfig().getCustomDelegationTokenHandlers()) {
                    try {
                        final TokenDelegationHandler customHandler =
                                Class.forName(className).asSubclass(TokenDelegationHandler.class).newInstance();
                        customDelegationHandlers.add(new CustomDelegationHandlerWrapper(
                                customHandler,
                                threadLocalAMTokenCache,
                                !stsInstanceConfig.getSoapDelegationConfig().getValidatedDelegatedTokenTypes().isEmpty(),
                                logger));
                    } catch (Exception e) {
                        logger.error("Exception caught instantiating class " + className
                                + " as a custom TokenDelegationHandler implementation. The exception: " + e);
                    }
                }
                return customDelegationHandlers;
            }
        }
    }
}
