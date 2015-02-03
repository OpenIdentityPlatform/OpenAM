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

import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.InstanceConfigMarshaller;
import org.forgerock.openam.sts.publish.common.STSInstanceConfigStoreBase;
import org.forgerock.openam.sts.soap.config.user.SoapSTSInstanceConfig;
import org.slf4j.Logger;

import javax.inject.Inject;

/**
 * @see org.forgerock.openam.sts.publish.common.STSInstanceConfigStoreBase
 * This subclass serves only to pass the name of the soap sts service up to the functionality defined in the
 * STSInstanceConfigBase class, and to provide the generic type for this class.
 */
public class SoapSTSInstanceConfigStore extends STSInstanceConfigStoreBase<SoapSTSInstanceConfig>  {
    @Inject
    SoapSTSInstanceConfigStore(InstanceConfigMarshaller<SoapSTSInstanceConfig> instanceConfigMarshaller, Logger logger) {
        super(instanceConfigMarshaller, AMSTSConstants.SOAP_STS_SERVICE_NAME, logger);
    }
}
