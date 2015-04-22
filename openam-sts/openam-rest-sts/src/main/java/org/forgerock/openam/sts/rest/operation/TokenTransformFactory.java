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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.rest.operation;

import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.config.user.TokenTransformConfig;

/**
 * Factory interface to build a TokenTransform instance given a TokenTransformConfig instance. Called by the
 * TokenTranslateOperationImpl to build the {@code Set<TokenTransform>} instance corresponding to the set of supported
 * token transformations.
 */
public interface TokenTransformFactory {
    /**
     *
     * @param tokenTransformConfig the configuration instance defining an input token, and output token, and whether the
     *                             interim OpenAM session should be invalidated after the output token is generated
     * @return a TokenTransform implementation which can realize the specified token transformation
     * @throws STSInitializationException if the any of the input or output token types are unexpected, and thus cannot
     * be accommodated by a TokenTransform instance
     */
    TokenTransform buildTokenTransform(TokenTransformConfig tokenTransformConfig) throws STSInitializationException;
}
