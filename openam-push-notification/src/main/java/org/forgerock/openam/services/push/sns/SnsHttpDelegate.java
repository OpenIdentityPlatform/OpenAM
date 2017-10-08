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

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.forgerock.openam.services.push.PushMessage;
import org.forgerock.openam.services.push.PushNotificationDelegate;
import org.forgerock.openam.services.push.PushNotificationException;
import org.forgerock.openam.services.push.PushNotificationServiceConfig;
import org.forgerock.openam.services.push.dispatch.MessageDispatcher;
import org.forgerock.openam.services.push.dispatch.Predicate;

/**
 * Delegate for communicating with SNS over HTTP.
 */
public class SnsHttpDelegate implements PushNotificationDelegate {

    private final static String ROUTE = "push/sns/message";
    private final static String AUTHENTICATE_ACTION = "authenticate";
    private final static String REGISTER_ACTION = "register";

    private final AmazonSNSClient client;
    private final SnsPushMessageConverter pushMessageConverter;
    private final String realm;
    private final MessageDispatcher messageDispatcher;

    private PushNotificationServiceConfig config;

    /**
     * Generates a new SNS HTTP Delegate, used to communicate over the Internet with
     * the SNS service.
     * @param client AmazonSnsClient - used to put messages on the wire.
     * @param config Necessary to configure this delegate.
     * @param pushMessageConverter a message converter, to ensure the message sent is of the correct format.
     * @param realm the realm in which this delegate exists.
     * @param messageDispatcher the MessageDispatcher used to redirect incoming messages to their caller.
     */
    public SnsHttpDelegate(AmazonSNSClient client, PushNotificationServiceConfig config,
                           SnsPushMessageConverter pushMessageConverter, String realm,
                           MessageDispatcher messageDispatcher) {
        this.client = client;
        this.config = config;
        this.pushMessageConverter = pushMessageConverter;
        this.realm = realm;
        this.messageDispatcher = messageDispatcher;
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
        return (realm.endsWith("/") ? realm : realm + "/") + ROUTE + "?_action=" + AUTHENTICATE_ACTION;
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
        return (realm.endsWith("/") ? realm : realm + "/") + ROUTE + "?_action=" + REGISTER_ACTION;
    }

    @Override
    public void startServices() throws PushNotificationException {
        //This section intentionally left blank.
    }

    @Override
    public MessageDispatcher getMessageDispatcher() {
        return messageDispatcher;
    }

    @Override
    public void close() throws IOException {
        //This section intentionally left blank.
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