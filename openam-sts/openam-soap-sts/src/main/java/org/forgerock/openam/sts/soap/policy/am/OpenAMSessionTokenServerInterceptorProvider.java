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

package org.forgerock.openam.sts.soap.policy.am;

import org.apache.cxf.ws.policy.AbstractPolicyInterceptorProvider;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.token.ThreadLocalAMTokenCache;
import org.forgerock.openam.sts.token.validator.PrincipalFromSession;

import javax.inject.Inject;
import java.util.Arrays;

/**
 * PolicyInterceptorProvider registered with the org.apache.cxf.ws.policy.PolicyInterceptorProviderRegistry on both the
 * client and server sides to support the custom OpenAMSessionToken SecurityPolicy assertions.
 */
public class OpenAMSessionTokenServerInterceptorProvider extends AbstractPolicyInterceptorProvider {
    @Inject
    public OpenAMSessionTokenServerInterceptorProvider(ThreadLocalAMTokenCache threadLocalAMTokenCache,
                                                       PrincipalFromSession principalFromSession) {
        super(Arrays.asList(AMSTSConstants.AM_SESSION_TOKEN_ASSERTION_QNAME));
        getOutInterceptors().add(new OpenAMSessionTokenServerInterceptor(threadLocalAMTokenCache, principalFromSession));
        getInInterceptors().add(new OpenAMSessionTokenServerInterceptor(threadLocalAMTokenCache, principalFromSession));
    }
}