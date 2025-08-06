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
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.dpro.session;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.forgerock.openam.core.DNWrapper;
import org.forgerock.openam.cts.api.fields.SessionTokenField;
import org.forgerock.openam.dpro.session.PartialSession.Builder;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.utils.TimeUtils;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.session.util.SessionUtils;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.debug.Debug;

/**
 * A factory implementation to help constructing {@link PartialSession} instances based on {@link PartialToken} and
 * {@link SSOToken} representations of sessions.
 */
@Singleton
public class PartialSessionFactory {

    private final Debug debug;
    private final IdentityUtils identityUtils;
    private final DNWrapper dnWrapper;

    /**
     * Constructs a new instance of PartialSessionFactory.
     *
     * @param debug The debug instance to use for logging.
     * @param identityUtils The identity utils instance to be used for identity related operations.
     * @param dnWrapper The DnWrapper instance to help translation between organization and realm name formats.
     */
    @Inject
    public PartialSessionFactory(@Named(SessionConstants.SESSION_DEBUG) Debug debug, IdentityUtils identityUtils,
            DNWrapper dnWrapper) {
        this.debug = debug;
        this.identityUtils = identityUtils;
        this.dnWrapper = dnWrapper;
    }

    /**
     * Constructs a new {@link PartialSession} instance based on the provided {@link PartialToken}.
     *
     * @param partialToken The partial token representation to convert to Partial Session.
     * @return The PartialSession instance created based on the partial token.
     */
    public PartialSession fromPartialToken(PartialToken partialToken) {
        Builder builder = new Builder();
        for (CoreTokenField field : partialToken.getFields()) {
            if (field.equals(CoreTokenField.USER_ID)) {
                String universalId = partialToken.getValue(CoreTokenField.USER_ID);
                builder.universalId(universalId);
                builder.username(identityUtils.getIdentityName(universalId));
            } else if (field.equals(SessionTokenField.REALM.getField())) {
                builder.realm(partialToken.<String>getValue(SessionTokenField.REALM.getField()));
            } else if (field.equals(SessionTokenField.SESSION_HANDLE.getField())) {
                builder.sessionHandle(SessionUtils.getDecrypted(partialToken.<String>getValue(SessionTokenField.SESSION_HANDLE.getField())));
            } else if (field.equals(SessionTokenField.LATEST_ACCESS_TIME.getField())) {
                builder.latestAccessTime(DateUtils.toUTCDateFormat(TimeUtils.fromUnixTime(Long.valueOf(
                        partialToken.<String>getValue(SessionTokenField.LATEST_ACCESS_TIME.getField()))).getTime()));
            } else if (field.equals(SessionTokenField.MAX_IDLE_EXPIRATION_TIME.getField())) {
                builder.maxIdleExpirationTime(DateUtils.toUTCDateFormat(partialToken.<Calendar>getValue(
                        SessionTokenField.MAX_IDLE_EXPIRATION_TIME.getField()).getTime()));
            } else if (field.equals(SessionTokenField.MAX_SESSION_EXPIRATION_TIME.getField())) {
                builder.maxSessionExpirationTime(DateUtils.toUTCDateFormat(partialToken.<Calendar>getValue(
                        SessionTokenField.MAX_SESSION_EXPIRATION_TIME.getField()).getTime()));
            }
        }

        return builder.build();
    }

    /**
     * Constructs a new {@link PartialSession} instance based on the provided {@link SSOToken}.
     *
     * @param ssoToken The SSO token representation to convert to partial session.
     * @return The PartialSession instance created based on the SSO token.
     */
    public PartialSession fromSSOToken(SSOToken ssoToken) {
        Builder builder = new Builder();
        try {
            String universalId = ssoToken.getProperty("sun.am.UniversalIdentifier");
            builder.username(identityUtils.getIdentityName(universalId));
            builder.universalId(universalId);
            builder.realm(dnWrapper.orgNameToRealmName(ssoToken.getProperty("Organization")));
            builder.sessionHandle(ssoToken.getProperty("SessionHandle"));
            builder.latestAccessTime(DateUtils.toUTCDateFormat(TimeUtils.fromUnixTime(
                    TimeUtils.currentUnixTime() - ssoToken.getIdleTime()).getTime()));
            final long idleTimeLeft = TimeUnit.SECONDS.convert(ssoToken.getMaxIdleTime(), TimeUnit.MINUTES)
                    - ssoToken.getIdleTime();
            builder.maxIdleExpirationTime(DateUtils.toUTCDateFormat(TimeUtils.fromUnixTime(
                    TimeUtils.currentUnixTime() + idleTimeLeft).getTime()));
            builder.maxSessionExpirationTime(DateUtils.toUTCDateFormat(TimeUtils.fromUnixTime(
                    TimeUtils.currentUnixTime() + ssoToken.getTimeLeft()).getTime()));
        } catch (SSOException ssoe) {
            debug.warning("Unable to convert SSOToken to Partial session", ssoe);
        }

        return builder.build();
    }
}
