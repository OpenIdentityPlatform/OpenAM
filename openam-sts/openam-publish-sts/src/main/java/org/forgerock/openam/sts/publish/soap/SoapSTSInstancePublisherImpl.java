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

import org.forgerock.openam.sts.DeploymentPathNormalization;
import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.publish.STSInstanceConfigStore;
import org.forgerock.openam.sts.soap.config.user.SoapSTSInstanceConfig;

import javax.inject.Inject;
import java.util.List;

/**
 * @see org.forgerock.openam.sts.publish.soap.SoapSTSInstancePublisher
 *
 * This class currently simply delegates all functionality to the persistentStore - the level of indirection will
 * be maintained in case functionality in addition to SMS persistence is required.
 */
public class SoapSTSInstancePublisherImpl implements SoapSTSInstancePublisher {
    private final STSInstanceConfigStore<SoapSTSInstanceConfig> persistentStore;
    private final DeploymentPathNormalization deploymentPathNormalization;

    @Inject
    SoapSTSInstancePublisherImpl(STSInstanceConfigStore<SoapSTSInstanceConfig> persistentStore,
                                 DeploymentPathNormalization deploymentPathNormalization) {
        this.persistentStore = persistentStore;
        this.deploymentPathNormalization = deploymentPathNormalization;
    }

    @Override
    public String publishInstance(SoapSTSInstanceConfig instanceConfig) throws STSPublishException {
        final String normalizedDeploymentSubPath =
                deploymentPathNormalization.normalizeDeploymentPath(instanceConfig.getDeploymentSubPath());
        persistentStore.persistSTSInstance(normalizedDeploymentSubPath, instanceConfig.getDeploymentConfig().getRealm(),
                instanceConfig);
        return normalizedDeploymentSubPath;
    }

    @Override
    public void removeInstance(String stsId, String realm) throws STSPublishException {
        persistentStore.removeSTSInstance(stsId, realm);
    }

    @Override
    public List<SoapSTSInstanceConfig> getPublishedInstances() throws STSPublishException {
        return persistentStore.getAllPublishedInstances();
    }

    @Override
    public SoapSTSInstanceConfig getPublishedInstance(String stsId, String realm) throws STSPublishException {
        return persistentStore.getSTSInstanceConfig(stsId, realm);
    }
}
