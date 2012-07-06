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
 * $Id: AccessTokenResource.java,v 1.1 2009/11/20 19:31:57 huacui Exp $
 *
 */

package com.sun.identity.oauth.service;

import com.sun.identity.oauth.service.models.AccessToken;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.WebApplicationException;

/**
 *
 * @author Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
 * @author Hua Cui <hua.cui@Sun.COM>
 */

@Path(PathDefs.ACCESS_TOKENS_PATH + "/{id}")
public class AccessTokenResource implements OAuthServiceConstants {
    @Context
    private UriInfo context;

    /**
     * GET method for retrieving a specific Service Consumer instance
     * and obtaining corresponding metadata (consumer name, URI, secret).
     *
     * @param sub (@link int) to retrieve the principal's id. Expected
     * value is either 1 (yes) or 0 (no) (e.g <PRE>&subject=1</PRE>).
     * @param shsec (@link int) to retrieve the shared secret (same
     * value as subject parameter).
     *
     * @return an HTTP response with URL encoded value of the service metadata.
     */
    @GET
    //@Consumes(MediaType.TEXT_PLAIN)
    public Response getAccessToken(@QueryParam(OAUTH_SUBJECT) int sub,
            @QueryParam(OAUTH_SHARED_SECRET) int shsec) {
        OAuthResourceManager oauthResMgr = OAuthResourceManager.getInstance();
        try {
            String resp = "";
            String secret = null;
            String principalId = null;

            String tokenUri = context.getAbsolutePath().toString();
            Map<String, String> searchMap = new HashMap<String, String>();
            searchMap.put(ACCESS_TOKEN_URI, tokenUri);
            List<AccessToken> accTokens= oauthResMgr.searchAccessTokens(searchMap);
            AccessToken token = null;
            if ((accTokens != null) && (!accTokens.isEmpty())) {
                token = accTokens.get(0);
            }
            if (token == null) {
                throw new WebApplicationException(new Throwable("Token invalid."));
            }
            if ((sub == 1) && (token.getAcctPpalid() != null)) {
                principalId = URLEncoder.encode(token.getAcctPpalid());
                resp = OAUTH_SUBJECT + "=" + principalId;
            }
            if ((shsec == 1) && (token.getAcctSecret() != null)) {
                secret = URLEncoder.encode(token.getAcctSecret());
                if (principalId != null) {
                    resp += "&";
                }
                resp += OAUTH_SHARED_SECRET + "=" + secret;
            }
            return Response.ok(resp, MediaType.TEXT_PLAIN).build();
        } catch (OAuthServiceException e) {
            Logger.getLogger(AccessTokenResource.class.getName()).log(Level.SEVERE, null, e);
            throw new WebApplicationException(e);
        }
    }


    @DELETE
    @Consumes(MediaType.TEXT_PLAIN)
    public Response deleteAcctoken() {
        OAuthResourceManager oauthResMgr = OAuthResourceManager.getInstance();
        try {
            String tokenUri = context.getAbsolutePath().toString();
            Map<String, String> searchMap = new HashMap<String, String>();
            searchMap.put(ACCESS_TOKEN_URI, tokenUri);
            List<AccessToken> accTokens= oauthResMgr.searchAccessTokens(searchMap);
            AccessToken token = null;
            if ((accTokens != null) && (!accTokens.isEmpty())) {
                token = accTokens.get(0);
            }
            if (token == null) {
                return Response.status(UNAUTHORIZED).build();
            }
            oauthResMgr.deleteAccessToken(token);
            return Response.ok().build();
        } catch (OAuthServiceException e) {
            Logger.getLogger(AccessTokenResource.class.getName()).log(Level.SEVERE, null, e);
            throw new WebApplicationException(e);
        }
    }

}
