/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: AccessTokenRequest.java,v 1.3 2010/01/20 17:51:37 huacui Exp $
 *
 */

package com.sun.identity.oauth.service;

import com.sun.jersey.oauth.server.OAuthServerRequest;
import com.sun.jersey.api.core.HttpContext;
import com.sun.identity.oauth.service.models.AccessToken;
import com.sun.identity.oauth.service.models.Consumer;
import com.sun.identity.oauth.service.models.RequestToken;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import com.sun.jersey.oauth.signature.OAuthParameters;
import com.sun.jersey.oauth.signature.OAuthSecrets;
import com.sun.jersey.oauth.signature.OAuthSignature;
import com.sun.jersey.oauth.signature.OAuthSignatureException;
import com.sun.jersey.oauth.signature.RSA_SHA1;
import com.sun.identity.oauth.service.util.UniqueRandomString;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;

/**
 * REST Web Service
 *
 * @author Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
 * @author Hua Cui <hua.cui@Sun.COM>
 */

@Path(PathDefs.ACCESS_TOKEN_REQUEST_PATH)
public class AccessTokenRequest implements OAuthServiceConstants {
    @Context
    private UriInfo context;

    /** Creates a new instance of ReqTokenRequestResource */
    public AccessTokenRequest() {
    }


    /**
     * POST method for creating a request for Rquest Token
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/x-www-form-urlencoded")
    public Response postAccessTokenRequest(
            @Context HttpContext hc,
            @Context Request req,
            String content) {
        boolean sigIsOk = false;
        OAuthResourceManager oauthResMgr = OAuthResourceManager.getInstance();
        try {
            Consumer cons = null;
            OAuthServerRequest request = new OAuthServerRequest(hc.getRequest());
            OAuthParameters params = new OAuthParameters();
            params.readRequest(request);

            if (params.getToken() == null)
                throw new WebApplicationException(new Throwable(
                     OAUTH_TOKEN + " MUST be present."), BAD_REQUEST);
            
            // Check the existence of oauth verifier
            String requestVerifier = params.get(OAUTH_VERIFIER);
            if ((requestVerifier == null) || (requestVerifier.isEmpty())) {
                throw new WebApplicationException(new Throwable(
                     OAUTH_VERIFIER + " MUST be present."), BAD_REQUEST);
            }

            Map<String, String> searchMap = new HashMap<String, String>();
            searchMap.put(REQUEST_TOKEN_URI, params.getToken());
            List<RequestToken> reqTokens= oauthResMgr.searchRequestTokens(searchMap);
            RequestToken rt = null;
            if ((reqTokens != null) && (!reqTokens.isEmpty())) {
                rt = reqTokens.get(0);
            }
            if (rt == null) {
                throw new WebApplicationException(new Throwable(
                    "Token invalid."), BAD_REQUEST);
            }

            String conskey = params.getConsumerKey();
            if (conskey == null) {
                throw new WebApplicationException(new Throwable(
                    "Consumer key is missing."), BAD_REQUEST);
            }

            String signatureMethod = params.getSignatureMethod();
            if (signatureMethod == null) {
                throw new WebApplicationException(new Throwable(
                    "Signature Method is missing."), BAD_REQUEST);
            }

            // Check that the verifiers match
            String reqTokenVerifier = rt.getVerifier();
            if (!requestVerifier.equals(reqTokenVerifier)) {
                throw new WebApplicationException(new Throwable(
                    "The oauth_verifier parameter is not valid."), BAD_REQUEST);
            }

            cons = rt.getConsumerId();
            if (cons == null) {
                throw new WebApplicationException(new Throwable(
                 "Consumer key invalid or service not registered"), BAD_REQUEST);
            }

            String secret = null;
            if (signatureMethod.equalsIgnoreCase(RSA_SHA1.NAME)) {
                secret = cons.getConsRsakey();
            } else {
                secret = cons.getConsSecret();
            }

            OAuthSecrets secrets = new OAuthSecrets().consumerSecret(
               secret).tokenSecret(rt.getReqtSecret());
            try {
                sigIsOk = OAuthSignature.verify(request, params, secrets);
            } catch (OAuthSignatureException ex) {
                Logger.getLogger(AccessTokenRequest.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (!sigIsOk) {
                throw new WebApplicationException(new Throwable(
                     "Signature invalid."), BAD_REQUEST);
            }

            // We're good to go.
            AccessToken newtok = new AccessToken();
            newtok.setAcctOnetime((short)1);
            newtok.setAcctPpalid(rt.getReqtPpalid());

            String baseUri = context.getBaseUri().toString();
            if (baseUri.endsWith("/")) {
                baseUri = baseUri.substring(0, baseUri.length() - 1);
            }
            URI loc = URI.create(baseUri + PathDefs.ACCESS_TOKENS_PATH 
                       + "/" + new UniqueRandomString().getString());
            newtok.setAcctUri(loc.toString());
            newtok.setAcctSecret(new UniqueRandomString().getString());

            newtok.setConsumerId(rt.getConsumerId());
            // for now val = uri
            newtok.setAcctVal(newtok.getAcctUri());

            oauthResMgr.createAccessToken(null, newtok);
            oauthResMgr.deleteRequestToken(rt);

            // Preparing the response.
            String resp = OAUTH_TOKEN + "=" + newtok.getAcctVal()
                 + "&" + OAUTH_TOKEN_SECRET + "=" + newtok.getAcctSecret();
            return Response.created(loc).entity(resp).type(
                     MediaType.APPLICATION_FORM_URLENCODED).build();
        } catch (OAuthServiceException e) {
            Logger.getLogger(AccessTokenRequest.class.getName()).log(Level.SEVERE, null, e);
            throw new WebApplicationException(e); // 500 error
        }
    }

}
