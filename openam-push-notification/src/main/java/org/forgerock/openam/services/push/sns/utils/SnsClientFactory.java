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
* information: "Portions copyright [year] [name of copyright owner]".
*
* Copyright 2016 ForgeRock AS.
*/
package org.forgerock.openam.services.push.sns.utils;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import org.forgerock.openam.services.push.PushNotificationServiceConfig;

/**
 * Factory to generate new Amazon SNS clients for a supplied config. Helps decoupling and unit tests.
 */
public class SnsClientFactory {

    /**
     * Generate a new Amazon SNS Client for the given config.
     * @param config An Amazon SNS client configured using the supplied config.
     * @return a constructed Amazon SNS client.
     */
    public AmazonSNSClient produce(PushNotificationServiceConfig config) {
        AmazonSNSClient service = new AmazonSNSClient(
                new BasicAWSCredentials(config.getAccessKey(), config.getSecret()));
        service.setRegion(Region.getRegion(Regions.fromName(config.getRegion())));
        return service;
    }

}
