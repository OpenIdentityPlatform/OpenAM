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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openam.sts.soap.audit;

import static java.net.HttpURLConnection.HTTP_CREATED;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.audit.events.AuditEvent;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.AuditConstants.EventName;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.HttpURLConnectionWrapper;
import org.forgerock.openam.sts.HttpURLConnectionWrapperFactory;
import org.forgerock.openam.sts.soap.bootstrap.SoapSTSAccessTokenProvider;
import org.slf4j.Logger;

/**
 * Responsible for sending locally created audit events to the OpenAM AuditService.
 *
 * @since 13.0.0
 */
public final class SoapSTSAuditEventPublisher implements AuditEventPublisher {

    private final String openamAuditServiceVersion;
    private final SoapSTSAccessTokenProvider soapSTSAccessTokenProvider;
    private final HttpURLConnectionWrapperFactory httpURLConnectionWrapperFactory;
    private final String openamAuditServiceUrl;
    private final String amSessionCookieName;
    private final Logger logger;

    @Inject
    SoapSTSAuditEventPublisher(
            HttpURLConnectionWrapperFactory httpURLConnectionWrapperFactory,
            @Named(AMSTSConstants.REST_CREATE_ACCESS_AUDIT_EVENT_URL) String openamAuditServiceUrl,
            @Named(AMSTSConstants.AM_SESSION_COOKIE_NAME) String amSessionCookieName,
            @Named(AMSTSConstants.CREST_VERSION_AUDIT_SERVICE) String openamAuditServiceVersion,
            SoapSTSAccessTokenProvider soapSTSAccessTokenProvider,
            Logger logger) {
        this.httpURLConnectionWrapperFactory = httpURLConnectionWrapperFactory;
        this.openamAuditServiceUrl = openamAuditServiceUrl;
        this.amSessionCookieName = amSessionCookieName;
        this.openamAuditServiceVersion = openamAuditServiceVersion;
        this.soapSTSAccessTokenProvider = soapSTSAccessTokenProvider;
        this.logger = logger;
    }

    /**
     * Send create request to OpenAM server's CREST AuditService with audit event JSON as payload.
     *
     * @param topic Coarse-grained categorization of the AuditEvent's type.
     * @param auditEvent AuditEvent to be published.
     */
    @Override
    public void tryPublish(String topic, AuditEvent auditEvent) {
        try {

            String sessionId = null;
            try {
                sessionId = soapSTSAccessTokenProvider.getAccessToken();
                Map<String, String> headerMap = new HashMap<>();
                headerMap.put(AMSTSConstants.CONTENT_TYPE, AMSTSConstants.APPLICATION_JSON);
                headerMap.put(AMSTSConstants.CREST_VERSION_HEADER_KEY, openamAuditServiceVersion);
                headerMap.put(AMSTSConstants.COOKIE, createAMSessionCookie(sessionId));

                HttpURLConnectionWrapper.ConnectionResult connectionResult = httpURLConnectionWrapperFactory
                        .httpURLConnectionWrapper(buildAuditAccessUrl())
                        .withoutAuditTransactionIdHeader()
                        .setRequestHeaders(headerMap)
                        .setRequestMethod(AMSTSConstants.GET)
                        .setRequestPayload(auditEvent.getValue().toString())
                        .makeInvocation();

                if (connectionResult.getStatusCode() != HTTP_CREATED) {
                    logger.error("Failed to record audit event: [status code {}] {}",
                            connectionResult.getStatusCode(),
                            connectionResult.getResult());
                }
            } finally {
                if (sessionId != null) {
                    soapSTSAccessTokenProvider.invalidateAccessToken(sessionId);
                }
            }

        } catch (Exception e) {
            logger.error("Failed to publish audit event: {}", e.getMessage(), e);
        }
    }

    @Override
    public boolean isAuditing(String realm, String topic, EventName eventName) {
        return true;
    }

    private URL buildAuditAccessUrl() throws MalformedURLException {
        return new URL(openamAuditServiceUrl);
    }

    private String createAMSessionCookie(String sessionId) {
        return amSessionCookieName + AMSTSConstants.EQUALS + sessionId;
    }

}