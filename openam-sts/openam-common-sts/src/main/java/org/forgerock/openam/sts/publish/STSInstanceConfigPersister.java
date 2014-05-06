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

import org.forgerock.openam.sts.config.user.STSInstanceConfig;

import java.util.List;

/**
 * Interface defining the act of persisting, removing, or reading state corresponding to a published STS instance so that
 * this instance may be reconstituted following a server restart, and that the single token-generation-service can
 * issue tokens with STS-instance-specific state (e.g. SAML2 configurations and crypto context).
 *
 * The generic type refers to the STSInstanceConfig subclass (RestSTSInstanceConfig or SoapSTSInstanceConfig) which will
 * be persisted.
 *
 */
public interface STSInstanceConfigPersister<T> {
    /**
     * Called by the RestSTSInstancePublisherImpl class to persist the configuration corresponding to the published
     * STS instance
     * @param key The key to the instance - probably return value from getDeploymentConfig().getUriElement();
     * @param instance The to-be-persisted state.
     */
    void persistSTSInstance(String key, T instance);

    /**
     *
     * @param key The unique identifier for a particular STSInstanceConfig
     */
    void removeSTSInstance(String key);

    /**
     * This method is called by the token generation service to obtain the STS-instance specific configurations -
     * notably the SAML2Config and the KeystoreConfig - which allows it to issue STS-instance-specific tokens.
     *
     * @param key The value obtained by calling getDeploymentConfig().getUriElement() on an
     *                                STSInstanceConfig instance. This String determines the sub-path to the published
     *                                STS instance, as registered with the router, and as such, it must be unique. This
     *                                value will be used as the attribute used to look-up the STSInstanceConfig. TODO:
     *                                if this attribute is deemed not unique enough, or not the proper format for an
     *                                LDAP index, I could generate a uuid every time a STSInstanceConfig instance is constructed,
     *                                and use that value as a unique identifier/indexed attribute.
     * @return The STSInstanceConfig corresponding to this deployment url element.
     */
    public T getSTSInstanceConfig(String key);

    /**
     * This method will be called by some startup context in the {locally|remotely} deployed {REST|SOAP} STS context
     * to obtain all of the previously-published STS instances. The publish service will be called with all obtained
     * instances. This will the set of published STS instances to be reconstituted following a server restart.
     *
     * @return The List of STSInstanceConfig instances (possibly empty) corresponding to the set of previously-published
     * instances.
     */
    public List<T> getAllPublishedInstances();
}
