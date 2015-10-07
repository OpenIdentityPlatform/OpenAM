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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.services;

import static org.apache.commons.collections.MapUtils.isEmpty;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.openam.utils.StringUtils.isBlank;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.inject.Inject;
import javax.mail.MessagingException;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.AbstractRequestHandler;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.forgerockrest.utils.MailServerLoader;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.openam.services.email.MailServer;
import org.forgerock.openam.services.email.MailServerImpl;
import org.forgerock.util.promise.Promise;

/**
 * This CREST resource represents a service as opposed to some resource, hence it only
 * implements action. The single action is "send" which is used to send an email. It
 * utilises the existing email service configuration
 *
 * @since 13.0.0
 */
public final class MailService extends AbstractRequestHandler {

    private static final String MAIL_SERVER_CLASS = "forgerockMailServerImplClassName";
    private static final String MAIL_SUBJECT = "forgerockEmailServiceSMTPSubject";
    private static final String MAIL_BODY = "forgerockEmailServiceSMTPMessage";


    private final MailServerLoader mailServerLoader;

    /**
     * Creates a new CREST email service.
     *
     * @param mailServerLoader
     *         the mail server loader
     */
    @Inject
    public MailService(MailServerLoader mailServerLoader) {
        this.mailServerLoader = mailServerLoader;
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        switch (request.getAction()) {
        case "send":
            try {
                JsonValue response = sendEmail(RealmContext.getRealm(context), request.getContent());
                return newResultPromise(newActionResponse(response));
            } catch (ResourceException rE) {
                return rE.asPromise();
            }
        default:
            return new NotSupportedException().asPromise();
        }
    }

    @SuppressWarnings("unchecked") // Mapping to known type
    private JsonValue sendEmail(String realm, JsonValue jsonValue) throws ResourceException {
        String to = jsonValue.get("to").asString();

        if (isBlank(to)) {
            throw new BadRequestException("to field is missing");
        }

        String mimeType = jsonValue.get("type").asString();

        if (isBlank(mimeType)) {
            throw new BadRequestException("mime type needs to be specified");
        }

        String subject = jsonValue.get("subject").asString();
        String body = jsonValue.get("body").asString();

        Map<String, Set<String>> mailConfigAttributes;

        try {
            ServiceConfigManager configManager = new ServiceConfigManager(RestUtils.getToken(),
                    MailServerImpl.SERVICE_NAME, MailServerImpl.SERVICE_VERSION);
            ServiceConfig mailConfig = configManager.getOrganizationConfig(realm, null);
            mailConfigAttributes = mailConfig.getAttributes();

        } catch (SMSException | SSOException e) {
            throw new InternalServerErrorException("Cannot create the service " + MailServerImpl.SERVICE_NAME, e);
        }

        if (isEmpty(mailConfigAttributes)) {
            throw new InternalServerErrorException("No service mail config found for realm " + realm);
        }

        MailServer mailServer;

        try {
            String attr = mailConfigAttributes.get(MAIL_SERVER_CLASS).iterator().next();
            mailServer = mailServerLoader.load(attr, realm);
        } catch (IllegalStateException e) {
            throw new InternalServerErrorException("Failed to create mail server", e);
        }

        if (isBlank(subject)) {
            subject = mailConfigAttributes.get(MAIL_SUBJECT).iterator().next();
        }

        if (isBlank(body)) {
            body = mailConfigAttributes.get(MAIL_BODY).iterator().next();
        }

        try {
            mailServer.sendEmail(to, subject, body, mimeType);
        } catch (MessagingException e) {
            throw new InternalServerErrorException("Failed to send email", e);
        }

        return json(object(field("success", "true")));
    }

}
