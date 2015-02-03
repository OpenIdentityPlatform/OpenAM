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

package org.forgerock.openam.sts;

/**
 * Interface defining concerns of normalizing the deploymentPath of a published STS instance for SMS persistence and
 * for TokenGenerationService caching. Called from the Rest and Soap STSInstanceConfig, and from the sts-publish service.
 */
public interface DeploymentPathNormalization {
    /**
     * @param deploymentPath the identifier for the rest or soap sts instance. Obtained by calling getDeploymentSubPath on
     *                       the SoapSTSInstanceConfig or RestSTSInstanceConfig instance corresponding to the rest/soap
     *                       STS.
     * @return return a normalized deployment path which will serve to identify the sts instance in the sms, and in the
     * TokenGenerationService cache. Because sts instances will be accessed as a url, it cannot have a trailing slash,
     * as this slash is removed in the http request.
     */
    public String normalizeDeploymentPath(String deploymentPath);
}
