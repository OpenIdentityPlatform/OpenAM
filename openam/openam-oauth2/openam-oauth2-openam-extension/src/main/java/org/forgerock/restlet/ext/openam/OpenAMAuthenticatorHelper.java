/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [2012] [Forgerock Inc]"
 */

package org.forgerock.restlet.ext.openam;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ChallengeRequest;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Parameter;
import org.restlet.engine.header.ChallengeWriter;
import org.restlet.engine.header.Header;
import org.restlet.engine.header.HeaderReader;
import org.restlet.engine.security.AuthenticatorHelper;
import org.restlet.engine.util.Base64;
import org.restlet.util.Series;

/**
 * An OpenAMAuthenticatorHelper generates the {@code WWW-Authenticate: OpenAM}
 * challenge request and parse the {@code Authorization: OpenAM } challenge
 * response to get the SSOToken ID.
 *
 */
public class OpenAMAuthenticatorHelper extends AuthenticatorHelper {

    public final static ChallengeScheme HTTP_OPENAM = new ChallengeScheme("HTTP_OPENAM", "OpenAM",
            "OpenAM SSO Authorization Tokens");

    private final static String SSO_TOKEN_PARAM = "org.forgerock.openam.authentication";

    /**
     * Constructor.
     */
    public OpenAMAuthenticatorHelper() {
        super(OpenAMAuthenticatorHelper.HTTP_OPENAM, true, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void formatRequest(ChallengeWriter cw, ChallengeRequest challenge, Response response,
            Series<Header> httpHeaders) throws IOException {
        if (challenge.getRealm() != null) {
            cw.appendQuotedChallengeParameter("realm", challenge.getRealm());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void formatResponse(ChallengeWriter cw, ChallengeResponse challenge, Request request,
            Series<Header> httpHeaders) {
        try {
            if (challenge == null) {
                throw new RuntimeException("No challenge provided, unable to encode credentials");
            } else {
                CharArrayWriter credentials = new CharArrayWriter();
                credentials.write(retrieveSSOToken(challenge));
                cw.append(Base64.encode(credentials.toCharArray(), "ISO-8859-1", false));
            }
        } catch (UnsupportedEncodingException e){
            OAuth2Utils.DEBUG.error("OpenAMAuthenticatorHelper::Unsupported encoding, unable to encode credentials", e);
            throw new RuntimeException("Unsupported encoding, unable to encode credentials");
        } catch (IOException e) {
            OAuth2Utils.DEBUG.error("OpenAMAuthenticatorHelper::Unexpected exception, unable to encode credentials", e);
            throw new RuntimeException("Unexpected exception, unable to encode credentials", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void parseRequest(ChallengeRequest challenge, Response response,
            Series<Header> httpHeaders) {
        if (challenge.getRawValue() != null) {
            HeaderReader<Object> hr = new HeaderReader<Object>(challenge.getRawValue());

            try {
                Parameter param = hr.readParameter();

                while (param != null) {
                    try {
                        if ("realm".equals(param.getName())) {
                            challenge.setRealm(param.getValue());
                        } else {
                            challenge.getParameters().add(param);
                        }

                        if (hr.skipValueSeparator()) {
                            param = hr.readParameter();
                        } else {
                            param = null;
                        }
                    } catch (Exception e) {
                        OAuth2Utils.DEBUG.error(
                                "Unable to parse the challenge request header parameter", e);
                    }
                }
            } catch (Exception e) {
                OAuth2Utils.DEBUG.error(
                        "Unable to parse the challenge request header parameter", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void parseResponse(ChallengeResponse challenge, Request request,
            Series<Header> httpHeaders) {
        try {
            byte[] credentialsEncoded = Base64.decode(challenge.getRawValue());
            if (credentialsEncoded == null) {
                OAuth2Utils.DEBUG.warning("OpenAMAuthenticatorHelper::Cannot decode token: " + challenge.getRawValue());
            }
            saveSSOToken(challenge, new String(credentialsEncoded, "ISO-8859-1"));
        } catch (UnsupportedEncodingException e) {
            OAuth2Utils.DEBUG.error("OpenAMAuthenticatorHelper::Unsupported OpenAM encoding error", e);
        } catch (IllegalArgumentException e) {
            OAuth2Utils.DEBUG.error("OpenAMAuthenticatorHelper::Unable to decode the OpenAM token", e);
        }
    }

    public static void saveSSOToken(ChallengeResponse challenge, String token) {
        challenge.getParameters().set(SSO_TOKEN_PARAM, token);
    }

    public static String retrieveSSOToken(ChallengeResponse challenge) {
        return challenge.getParameters().getFirstValue(SSO_TOKEN_PARAM);
    }
}
