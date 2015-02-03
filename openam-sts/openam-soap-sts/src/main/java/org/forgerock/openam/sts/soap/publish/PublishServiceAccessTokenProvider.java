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

package org.forgerock.openam.sts.soap.publish;

import org.forgerock.openam.sts.STSPublishException;

/**
 * This interface encapsulates the concerns of obtaining the token necessary to consume the sts-publish service in order
 * for the configuration state corresponding to published soap-sts instances may be obtained. TODO: also need to provide
 * a token to consume the TGS - perhaps both should be consumable by the same application token...
 */
public interface PublishServiceAccessTokenProvider {
    public String getPublishServiceAccessToken() throws STSPublishException;

    /**
     * TODO: not sure that this is needed for the final form of authentication - i.e. will application-based authentication
     * require session invalidation?
     */
    public void invalidatePublishServiceAccessToken(String sessionId);
}
