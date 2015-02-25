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

package org.forgerock.openam.sts.publish;

import org.forgerock.openam.sts.STSPublishException;

import java.util.List;

/**
 * Interface defining the act of persisting, removing, or reading state corresponding to a published STS instance so that
 * this instance may be reconstituted following a server restart, and so that the single token-generation-service can
 * issue tokens with STS-instance-specific state (e.g. SAML2 configurations and crypto context).
 *
 * The generic type refers to the STSInstanceConfig subclass (RestSTSInstanceConfig or SoapSTSInstanceConfig) which will
 * be persisted.
 *
 */
public interface STSInstanceConfigStore<T> {
    /**
     * Called by the RestSTSInstancePublisherImpl class to persist the configuration corresponding to the published
     * STS instance
     * @param stsId The unique identifier for the STS instance. Currently obtained by calling RestSTSInstanceConfig#getDeploymentSubPath.
     * @param instance The to-be-persisted state.
     */
    void persistSTSInstance(String stsId, T instance) throws STSPublishException;

    /**
     *
     * @param stsId The unique identifier for a particular STSInstanceConfig. Currently obtained by calling
     *              RestSTSInstanceConfig#getDeploymentSubPath.
     * @param realm the realm in which the sts instance was deployed. Necessary for SMS lookup.
     */
    void removeSTSInstance(String stsId, String realm) throws STSPublishException;

    /**
     * This method is called by the token generation service to obtain the STS-instance specific configurations -
     * notably the SAML2Config and the KeystoreConfig - which allows it to issue STS-instance-specific tokens.
     *
     * @param stsId The identifier for the STS instance. Currently obtained by calling RestSTSInstanceConfig#getDeploymentSubPath.
     *              This value is the catenation of the deployment realm with value obtained from
     *              RestSTSInstanceConfig#getDeploymentConfig()#getUriElement(). This value will be used to identify
     *              a particular Rest STS instance to the TokenGenerationService as well as constitute the most discriminating
     *              element in the DN referencing the STS instance state in the SMS/LDAP.
     * @param realm the realm in which the sts instance was deployed. Necessary for SMS lookup.
     * @return The STSInstanceConfig corresponding to this deployment url element.
     */
    public T getSTSInstanceConfig(String stsId, String realm) throws STSPublishException;

    /**
     * This method will be called by some startup context in the {locally|remotely} deployed {REST|SOAP} STS context
     * to obtain all of the previously-published STS instances. The publish service will be called with all obtained
     * instances. This will the set of published STS instances to be reconstituted following a server restart.
     *
     * @return The List of STSInstanceConfig instances (possibly empty) corresponding to the set of previously-published
     * instances.
     */
    public List<T> getAllPublishedInstances() throws STSPublishException;

    /**
     * This method returns whether STS instance config referenced by the realm and id is present in the SMS. It is called
     * by RestSTSPublishServiceRequestHandler#handleUpdate to determine whether the referenced sts id actually corresponds
     * to a previously-published instance
     * @param stsId The unique identifier for a particular STSInstanceConfig. Currently obtained by calling
     *              RestSTSInstanceConfig#getDeploymentSubPath.
     * @param realm the realm in which the sts instance was deployed. Necessary for SMS lookup.
     * @return true if the instance is present
     * @throws STSPublishException If an exception is encountered consulting the SMS.
     */
    public boolean isInstancePresent(String stsId, String realm) throws STSPublishException;
}
