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
package org.forgerock.openam.services.push.sns;

import static org.forgerock.openam.services.push.PushNotificationConstants.*;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.services.push.dispatch.Predicate;

/**
 * Acts to register (via communication with SNS) the device that is currently talking to the server
 * from the mobile app.
 */
public class SnsRegistrationPredicate implements Predicate {

    private final AmazonSNSClient client;
    private final String appleEndpoint;
    private final String googleEndpoint;

    /**
     * Generate a new SnsRegistrationPredicate, which will use the supplied client and endpoint information
     * to register devices with SNS.
     *
     * @param client AmazonSNSClient used to communicate with Amazon.
     * @param appleEndpoint The apple-registration ARN.
     * @param googleEndpoint The google-registration ARN.
     */
    public SnsRegistrationPredicate(AmazonSNSClient client, String appleEndpoint, String googleEndpoint) {
        this.client = client;
        this.appleEndpoint = appleEndpoint;
        this.googleEndpoint = googleEndpoint;
    }

    /**
     * Communicates with Amazon to ensure that the device communicating with us is registered with
     * Amazon, and to retrieve the appropriate endpoint ARN which will later be used to
     * communicate with this device.
     *
     * Finally, expands the contents out to be readable by the registration module itself, including
     * the communicationId.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean perform(JsonValue content) {
        Jwt jwt = new JwtReconstruction().reconstructJwt(content.get(DATA_JSON_POINTER).asString(), Jwt.class);

        JwtClaimsSet claimsSet = jwt.getClaimsSet();
        String communicationType = (String) claimsSet.getClaim(COMMUNICATION_TYPE);
        String deviceId = (String) claimsSet.getClaim(DEVICE_ID);
        String mechanismUid = (String) claimsSet.getClaim(MECHANISM_UID);
        String deviceType = (String) claimsSet.getClaim(DEVICE_TYPE);

        String platformApplicationArn;

        if (communicationType.equals(APNS)) {
            platformApplicationArn = appleEndpoint;
        } else {
            platformApplicationArn = googleEndpoint;
        }

        CreatePlatformEndpointRequest request = new CreatePlatformEndpointRequest()
                .withPlatformApplicationArn(platformApplicationArn)
                .withToken(deviceId);

        CreatePlatformEndpointResult communicationId = client.createPlatformEndpoint(request);

        content.put(DEVICE_ID, deviceId);
        content.put(COMMUNICATION_TYPE, communicationType);
        content.put(COMMUNICATION_ID, communicationId.getEndpointArn());
        content.put(MECHANISM_UID, mechanismUid);
        content.put(DEVICE_TYPE, deviceType);

        return true;
    }
}
