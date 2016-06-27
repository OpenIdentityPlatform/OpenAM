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

import static org.forgerock.openam.services.push.PushNotificationConstants.*;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.InvalidParameterException;
import com.amazonaws.services.sns.model.SetEndpointAttributesRequest;
import java.util.HashMap;
import java.util.Map;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.services.push.PushNotificationServiceConfig;
import org.forgerock.openam.utils.StringUtils;

/**
 * A utility class for {@see SnsRegistrationPredicate} to aid testing.
 */
public class SnsPushResponseUpdater {

    private final static String ENABLED = "Enabled";
    private final static String TOKEN = "Token";

    private SnsClientFactory clientFactory;

    /**
     * Generates a new AmazonSNSPushResponseUpdater with the provided factory used to generate
     * AmazonSNSClients.
     * @param clientFactory used to generate amazon SNS clients.
     */
    public SnsPushResponseUpdater(SnsClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    /**
     * Updates the response (held in the content JsonValue) using information communicated
     * back from the client, and gathers the device's communication ID via registering it
     * with Amazon and retrieving and endpoint ARN.
     *
     * @param config The config of the amazon push service.
     * @param content The content of the response message from the push device.
     * @return true if the update was performed successfully.
     */
    public boolean updateResponse(PushNotificationServiceConfig config, JsonValue content) {

        if (content.get(DATA_JSON_POINTER) == null) {
            return false;
        }

        Jwt jwt = new JwtReconstruction().reconstructJwt(content.get(DATA_JSON_POINTER).asString(), Jwt.class);
        JwtClaimsSet claimsSet = jwt.getClaimsSet();

        updateBasicJsonContent(content, claimsSet);
        return updateCommunicationId(content, claimsSet, config);
    }

    private boolean updateCommunicationId(JsonValue content, JwtClaimsSet claimsSet,
                                       PushNotificationServiceConfig config) {
        AmazonSNSClient client = clientFactory.produce(config);

        String communicationType = (String) claimsSet.getClaim(COMMUNICATION_TYPE);
        String deviceId = (String) claimsSet.getClaim(DEVICE_ID);

        String platformApplicationArn;

        if (communicationType.equals(APNS)) {
            platformApplicationArn = config.getAppleEndpoint();
        } else {
            platformApplicationArn = config.getGoogleEndpoint();
        }

        setCommunicationId(client, platformApplicationArn, deviceId, content);

        return content.get(COMMUNICATION_ID).isNotNull()
                && !StringUtils.isBlank(content.get(COMMUNICATION_ID).asString());
    }

    private void updateBasicJsonContent(JsonValue content, JwtClaimsSet claims) {
        String mechanismUid = (String) claims.getClaim(MECHANISM_UID);
        String deviceType = (String) claims.getClaim(DEVICE_TYPE);
        String deviceId = (String) claims.getClaim(DEVICE_ID);
        String communicationType = (String) claims.getClaim(COMMUNICATION_TYPE);

        content.put(MECHANISM_UID, mechanismUid);
        content.put(DEVICE_TYPE, deviceType);
        content.put(DEVICE_ID, deviceId);
        content.put(COMMUNICATION_TYPE, communicationType);
    }

    private void setCommunicationId(AmazonSNSClient client, String arn, String deviceId, JsonValue content) {
        Map<String, String> attributes = standardAtttributes(deviceId);
        String answer;

        try {
            CreatePlatformEndpointRequest createReq = new CreatePlatformEndpointRequest()
                    .withPlatformApplicationArn(arn)
                    .withAttributes(attributes)
                    .withToken(deviceId);

            CreatePlatformEndpointResult result = client.createPlatformEndpoint(createReq);
            answer = result.getEndpointArn();
        } catch (InvalidParameterException e) {
            //endpoint already exists, let's try to re-enable it, first we ensure we get the appropriate endpointArn
            attributes.put(ENABLED, "false");

            CreatePlatformEndpointRequest createReq = new CreatePlatformEndpointRequest()
                    .withPlatformApplicationArn(arn)
                    .withToken(deviceId)
                    .withAttributes(attributes);
            CreatePlatformEndpointResult result = client.createPlatformEndpoint(createReq);

            String endpointArn = result.getEndpointArn();

            attributes.put(ENABLED, "true");

            //then we re-enable it, note the attribute-set
            SetEndpointAttributesRequest attrReq = new SetEndpointAttributesRequest()
                    .withAttributes(attributes)
                    .withEndpointArn(endpointArn);

            client.setEndpointAttributes(attrReq);
            answer = endpointArn;
        }
        content.put(COMMUNICATION_ID, answer);

    }

    private Map<String, String> standardAtttributes(String deviceId) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(ENABLED, "true");
        attributes.put(TOKEN, deviceId);
        return attributes;
    }

}
