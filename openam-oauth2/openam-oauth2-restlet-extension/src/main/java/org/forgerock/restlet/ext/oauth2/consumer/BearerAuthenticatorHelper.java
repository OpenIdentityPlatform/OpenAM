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
 * "Portions Copyrighted [2012] [ForgeRock Inc]"
 */
package org.forgerock.restlet.ext.oauth2.consumer;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.logging.Level;

import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ChallengeRequest;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.engine.header.ChallengeWriter;
import org.restlet.engine.header.Header;
import org.restlet.engine.header.HeaderReader;
import org.restlet.engine.util.Base64;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.util.Series;

/**
 * @see <a href="http://tools.ietf.org/html/draft-ietf-oauth-v2-bearer">The
 *      OAuth 2.0 Authorization Protocol: Bearer Tokens</a>
 */
public class BearerAuthenticatorHelper extends AccessTokenExtractor<BearerToken> {

    public final static ChallengeScheme HTTP_OAUTH_BEARER = new ChallengeScheme("HTTP_BEARER",
            OAuth2Constants.Bearer.BEARER, "OAuth 2.0 Authorization Protocol: Bearer Tokens");

    /**
     * Constructor.
     */
    public BearerAuthenticatorHelper() {
        super(BearerAuthenticatorHelper.HTTP_OAUTH_BEARER, true, true);
    }

    /**
     * Constructor.
     * 
     * @param clientSide
     *            Indicates if client side authentication is supported.
     * @param serverSide
     *            Indicates if server side authentication is supported.
     */
    public BearerAuthenticatorHelper(boolean clientSide, boolean serverSide) {
        super(BearerAuthenticatorHelper.HTTP_OAUTH_BEARER, clientSide, serverSide);
    }

    @Override
    public void formatRequest(ChallengeWriter cw, ChallengeRequest challenge, Response response,
            Series<Header> httpHeaders) throws IOException {
        if (challenge.getRealm() != null) {
            cw.appendQuotedChallengeParameter("realm", challenge.getRealm());
        }
    }

    @Override
    public void formatResponse(ChallengeWriter cw, ChallengeResponse challenge, Request request,
            Series<Header> httpHeaders) {
        try {
            if (challenge == null) {
                throw new RuntimeException("No challenge provided, unable to encode credentials");
            } else {
                CharArrayWriter credentials = new CharArrayWriter();
                credentials.write(retrieveToken(challenge));
                cw.append(Base64.encode(credentials.toCharArray(), "ISO-8859-1", false));
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported encoding, unable to encode credentials");
        } catch (IOException e) {
            throw new RuntimeException("Unexpected exception, unable to encode credentials", e);
        }
    }

    /**
     * Parses an authenticate header into a challenge request. The header is
     * {@link org.restlet.engine.header.HeaderConstants#HEADER_WWW_AUTHENTICATE}
     * .
     * <p/>
     * Values for the "scope" attribute MUST NOT include characters outside the
     * set %x21 / %x23-5B / %x5D-7E for representing scope values and %x20 for
     * delimiters between scope values. Values for the "error" and
     * "error_description" attributes MUST NOT include characters outside the
     * set %x20-21 / %x23-5B / %x5D-7E. Values for the "error_uri" attribute
     * MUST conform to the URI-Reference syntax, and thus MUST NOT include
     * characters outside the set %x21 / %x23-5B / %x5D-7E.
     * 
     * @param challenge
     *            The challenge request to update.
     * @param response
     *            The parent response.
     * @param httpHeaders
     *            The current response HTTP headers.
     */
    @Override
    public void parseRequest(ChallengeRequest challenge, Response response,
            Series<Header> httpHeaders) {
        /*
         * All challenges defined by this specification MUST use the auth-scheme
         * value "Bearer". This scheme MUST be followed by one or more auth-
         * param values.
         * 
         * A "realm" attribute MAY be included to indicate the scope of
         * protection The "scope" attribute is a space-delimited list of scope
         * values indicating the required scope of the access token for
         * accessing the requested resource.
         * 
         * HTTP/1.1 401 Unauthorized WWW-Authenticate: Bearer realm="example"
         * WWW-Authenticate: Bearer realm="apps", type=1,
         * title="Login to \"apps\"", Basic realm="simple"
         * 
         * And in response to a protected resource request with an
         * authentication attempt using an expired access token:
         * 
         * HTTP/1.1 401 Unauthorized WWW-Authenticate: Bearer
         * realm="example",error
         * ="invalid_token",error_description="The access token expired"
         */
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
                        Context.getCurrentLogger().log(Level.WARNING,
                                "Unable to parse the challenge request header parameter", e);
                    }
                }
            } catch (Exception e) {
                Context.getCurrentLogger().log(Level.WARNING,
                        "Unable to parse the challenge request header parameter", e);
            }
        }
    }

    @Override
    public void parseResponse(ChallengeResponse challenge, Request request,
            Series<Header> httpHeaders) {
        /*
         * bearer oauth header
         * 
         * token is b64token
         * 
         * NOP Authorization: Bearer realm="example" vF9dft4qmT NOP
         * Authorization: Bearer realm=example vF9dft4qmT Authorization: Bearer
         * vF9dft4qmT
         */
        // super.parseResponse(challenge, request, httpHeaders);

        try {
            byte[] credentialsEncoded = Base64.decode(challenge.getRawValue());
            if (credentialsEncoded == null) {
                getLogger().info("Cannot decode token: " + challenge.getRawValue());
            }
            saveToken(challenge, new String(credentialsEncoded, "ISO-8859-1"));
        } catch (UnsupportedEncodingException e) {
            getLogger().log(Level.INFO, "Unsupported OpenAM encoding error", e);
        } catch (IllegalArgumentException e) {
            getLogger().log(Level.INFO, "Unable to decode the OpenAM token", e);
        }

        /*
         * String raw = challenge.getRawValue();
         * 
         * if (raw != null && raw.length() > 0) { StringTokenizer st = new
         * StringTokenizer(raw, ","); String realm = st.nextToken();
         * 
         * if (realm != null && realm.length() > 0) { int eq =
         * realm.indexOf('=');
         * 
         * if (eq > 0) { String value = realm.substring(eq + 1).trim(); //
         * Remove the quotes, first and last after trim...
         * challenge.setRealm(value.substring(1, value.length() - 1)); } }
         * 
         * Series<Parameter> params = new Form();
         * 
         * while (st.hasMoreTokens()) { String param = st.nextToken();
         * 
         * if (param != null && param.length() > 0) { int eq =
         * param.indexOf('=');
         * 
         * if (eq > 0) { String name = param.substring(0, eq).trim(); String
         * value = param.substring(eq + 1).trim(); // Remove the quotes, first
         * and last after trim... params.add(name, value.substring(1,
         * value.length() - 1)); } } }
         * 
         * challenge.setParameters(params); }
         */

    }

    /**
     * Append the access_token to the query of resourceRef TODO implement this
     * method
     * 
     * @param resourceRef
     * @param challengeResponse
     * @param request
     * @return
     */
    @Override
    public Reference updateReference(Reference resourceRef, ChallengeResponse challengeResponse,
            Request request) {
        return super.updateReference(resourceRef, challengeResponse, request);
    }

    public static void saveToken(ChallengeResponse challenge, String token) {
        challenge.getParameters().set(OAuth2Constants.Token.OAUTH_ACCESS_TOKEN, token);
    }

    public static String retrieveToken(ChallengeResponse challenge) {
        return challenge.getParameters().getFirstValue(OAuth2Constants.Token.OAUTH_ACCESS_TOKEN);
    }

    @Override
    protected BearerToken extractRequestToken(ChallengeResponse challengeResponse)
            throws OAuthProblemException {
        Series<Parameter> parameters = challengeResponse.getParameters();
        if (!parameters.isEmpty()) {
            OAuthProblemException exception = extractException(parameters);
            if (null != exception) {
                throw exception;
            }
            if (null != retrieveToken(challengeResponse)) {
                return new BearerToken(parameters);
            }
        }
        return null;
    }

    @Override
    protected BearerToken extractRequestToken(Request request) throws OAuthProblemException {
        if (MediaType.APPLICATION_JSON.equals(request.getEntity().getMediaType())) {
            JacksonRepresentation<Map> representation =
                    new JacksonRepresentation<Map>(request.getEntity(), Map.class);
            // Restore content
            request.setEntity(representation);
            try {
                Map parameters = representation.getObject();
                OAuthProblemException exception = exception = extractException(parameters);
                if (null != exception) {
                    throw exception;
                }
                if (parameters.containsKey(OAuth2Constants.Token.OAUTH_ACCESS_TOKEN)) {
                    return new BearerToken(parameters);
                }
            } catch (IOException e) {
                throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL, e.getMessage(), e);
            }
        } else if (MediaType.APPLICATION_WWW_FORM.equals(request.getEntity().getMediaType())) {
            Form parameters = new Form(request.getEntity());
            // Restore content
            request.setEntity(parameters.getWebRepresentation());
            OAuthProblemException exception = extractException(parameters);
            if (null != exception) {
                throw exception;
            }
            if (null != parameters.getFirst(OAuth2Constants.Token.OAUTH_ACCESS_TOKEN)) {
                return new BearerToken(parameters);
            }
        }
        return null;
    }

    @Override
    protected BearerToken extractRequestToken(Response response) throws OAuthProblemException {
        Map<String, Object> token = null;
        if (MediaType.APPLICATION_JSON.equals(response.getEntity().getMediaType())) {
            // TODO Catch invalid content
            try {
                token = new JacksonRepresentation<Map>(response.getEntity(), Map.class).getObject();
            } catch (IOException e) {
                throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL, e.getMessage(), e);
            }
        }
        if (null != token) {
            OAuthProblemException exception = extractException(token);
            if (exception != null) {
                throw exception;
            }
            if (token.containsKey(OAuth2Constants.Token.OAUTH_ACCESS_TOKEN)) {
                return new BearerToken(token);
            }
        }
        return null;
    }

    @Override
    protected BearerToken extractRequestToken(Form parameters) throws OAuthProblemException {
        OAuthProblemException exception = extractException(parameters);
        if (null != exception) {
            throw exception;
        }
        if (null != parameters.getFirst(OAuth2Constants.Token.OAUTH_ACCESS_TOKEN)) {
            return new BearerToken(parameters);
        }
        return null;
    }

    @Override
    public ChallengeResponse createChallengeResponse(BearerToken token) {
        ChallengeResponse response = new ChallengeResponse(HTTP_OAUTH_BEARER);
        saveToken(response, token.getAccessToken());
        return response;
    }

    @Override
    public ChallengeRequest createChallengeRequest(String realm) {
        return new ChallengeRequest(HTTP_OAUTH_BEARER, realm);
    }

    @Override
    public ChallengeRequest createChallengeRequest(String realm, OAuthProblemException exception) {
        ChallengeRequest cr = createChallengeRequest(realm);
        for (Map.Entry<String, Object> entry : exception.getErrorMessage().entrySet()) {
            cr.getParameters().add(entry.getKey(), entry.getValue().toString());
        }
        return cr;
    }

    @Override
    public Form createForm(BearerToken token) {
        Form form = new Form();
        form.add(OAuth2Constants.Token.OAUTH_ACCESS_TOKEN, token.getAccessToken());
        return form;
    }

}
