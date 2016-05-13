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

import static org.forgerock.http.routing.RoutingMode.*;
import static org.forgerock.json.resource.Resources.*;
import static org.forgerock.json.resource.Router.*;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import org.forgerock.json.resource.Router;
import org.forgerock.openam.services.push.PushMessage;
import org.forgerock.openam.services.push.PushNotificationDelegate;
import org.forgerock.openam.services.push.PushNotificationException;
import org.forgerock.openam.services.push.PushNotificationServiceConfig;
import org.forgerock.openam.services.push.dispatch.Predicate;
import org.forgerock.services.routing.RouteMatcher;

/**
 * Delegate for communicating with SNS over HTTP.
 */
public class SnsHttpDelegate implements PushNotificationDelegate {

    private final static String ROUTE = "push/sns/message";

    private RouteMatcher<org.forgerock.json.resource.Request> routeMatcher;

    private final AmazonSNSClient client;
    private final Router router;
    private final SnsMessageResource messageEndpoint;
    private final SnsPushMessageConverter pushMessageConverter;
    private final String realm;

    private PushNotificationServiceConfig config;

    /**
     * Generates a new SNS HTTP Delegate, used to communicate over the Internet with
     * the SNS service.
     *
     * @param client AmazonSnsClient - used to put messages on the wire.
     * @param config Necessary to configure this delegate.
     * @param router to attach a newly generate GcmMessageEndpoint upon this delegate's initialization.
     * @param messageEndpoint the endpoint to attach to the router upon initialisation.
     * @param pushMessageConverter a message converter, to ensure the message sent is of the correct format.
     * @param realm the realm in which this delegate exists.
     */
    public SnsHttpDelegate(AmazonSNSClient client, PushNotificationServiceConfig config, Router router,
                           SnsMessageResource messageEndpoint, SnsPushMessageConverter pushMessageConverter,
                           String realm) {
        this.client = client;
        this.config = config;
        this.router = router;
        this.messageEndpoint = messageEndpoint;
        this.pushMessageConverter = pushMessageConverter;
        this.realm = realm;
    }

    @Override
    public void send(PushMessage message) {
        PublishRequest request = convertToSns(message);
        client.publish(request);
    }

    @Override
    public boolean isRequireNewDelegate(PushNotificationServiceConfig newConfig) {
        return !newConfig.equals(config);
    }

    @Override
    public void updateDelegate(PushNotificationServiceConfig newConfig) {
        //This section intentionally left blank.
    }

    @Override
    public String getAuthServiceLocation() {
        return ROUTE + "?_action=authenticate";
    }

    @Override
    public Set<Predicate> getRegistrationMessagePredicates() {
        Predicate predicate = new SnsRegistrationPredicate(realm);
        return Collections.singleton(predicate);
    }

    @Override
    public Set<Predicate> getAuthenticationMessagePredicates() {
        return Collections.emptySet();
    }

    @Override
    public String getRegServiceLocation() {
        return ROUTE + "?_action=register";
    }

    @Override
    public void startServices() throws PushNotificationException {
        routeMatcher = router.addRoute(EQUALS, uriTemplate(ROUTE), newAnnotatedRequestHandler(messageEndpoint));
    }

    @Override
    public void close() throws IOException {
        router.removeRoute(routeMatcher);
    }

    private PublishRequest convertToSns(PushMessage message) {

        PublishRequest request = new PublishRequest()
                .withTargetArn(message.getRecipient())
                .withSubject(message.getSubject());

        request.setMessageStructure("json");
        request.setMessage(pushMessageConverter.toTransferFormat(message));

        return request;
    }

}