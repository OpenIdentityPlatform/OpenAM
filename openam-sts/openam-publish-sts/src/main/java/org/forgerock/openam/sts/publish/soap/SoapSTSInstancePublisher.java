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

package org.forgerock.openam.sts.publish.soap;

import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.soap.config.user.SoapSTSInstanceConfig;

import java.util.List;

/**
 * Represents the concerns of publishing Soap STS instances, and retrieving these representations. Publishing a soap-sts
 * instance involves writing its configuration state to the SMS, so it can be obtained by:
 * 1. a GET on the sts publish service, which is performed by the soap-sts deployment context
 * 2. by the token generation service, via the SoapSTSInstanceConfigStore.
 *
 * This interface will be consumed by the SoapSTSPublishServiceRequestHandler.
 */
public interface SoapSTSInstancePublisher {
    /**
     * Publish the Soap STS instance specified by the instanceConfig parameter. Publishing a Soap STS instance
     * means persisting it to the SMS and exposing it via CREST.
     * @param instanceConfig The configuration state for the to-be-published Soap STS instance.
     * @return the urlElement, including the realm, at which the Soap STS instance has been published.
     * @throws org.forgerock.openam.sts.STSPublishException
     */
    String publishInstance(SoapSTSInstanceConfig instanceConfig) throws STSPublishException;

    /**
     * Remove the Soap STS instance from the SMS, and from the CREST router.
     * @param stsId The sts id, obtained from SoapSTSInstanceConfig#getDeploymentSubPath
     * @param realm The realm in which the Soap STS is to be deployed.
     *
     * @throws STSPublishException
     */
    void removeInstance(String stsId, String realm) throws STSPublishException;

    /**
     * Called to obtain the configuration elements corresponding to previously-published STS instances.
     * @return The SoapSTSInstanceConfig instances corresponding
     * to published STS instances.
     * @throws STSPublishException if exception encountered obtaining persisted instance state
     */
    List<SoapSTSInstanceConfig> getPublishedInstances() throws STSPublishException;

    /**
     * Called to return the config state corresponding to a specified Soap STS instance
     * @param stsId The sts id, obtained from SoapSTSInstanceConfig#getDeploymentSubPath
     * @param realm The realm in which the Soap STS is to be deployed.
     * @return The SoapSTSInstanceConfig corresponding to this published instance
     * @throws STSPublishException
     */
    SoapSTSInstanceConfig getPublishedInstance(String stsId, String realm) throws STSPublishException;

}
