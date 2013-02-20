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
 * $Id: NoBrowserAuthorization.java,v 1.1 2009/11/20 19:31:57 huacui Exp $
 *
 */
/**
 * Portions Copyrighted 2012 ForgeRock AS
 */
package com.sun.identity.oauth.service;

import com.sun.identity.oauth.service.models.RequestToken;
import com.sun.identity.oauth.service.util.OAuthServiceUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;


/**
 * @author Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
 * @author Paul C. Bryan <pbryan@sun.com>
 * @author Hua Cui <hua.cui@Sun.COM>
 */
@Path(PathDefs.NO_BROWSER_AUTHORIZATION_PATH)
public class NoBrowserAuthorization implements OAuthServiceConstants {
    
    /** Creates a new instance of AuthorizationFactory */
    public NoBrowserAuthorization() {
    }

    /**
     * GET method to authenticate & obtain user's consent.
     * This endpoint does not use callback and does not rely on
     * browser-based authorization but rather submits the credentials
     * to a predefined OpenSSO endpoint.
     *
     * @param username (@String) is the user name to authenticate at the OpenSSO
     * instance
     * @param password (@String) is the user's password
     * @param requestToken (@String) is the request token to authorize
     * @return 200 in case of success, 403 if authentications fails, 400 otherwise.
     */
    @GET
    public Response NoBrowserAuthorization(
    @QueryParam(USERNAME) String username,
    @QueryParam(PASSWORD) String password,
    @QueryParam(REQUEST_TOKEN) String requestToken) {

        if (username == null || password == null || requestToken == null) {
            throw new WebApplicationException(new Throwable("Request invalid."));
        }

        // authenticate the user and get the OpenSSO session token
        String tokenId = null;
        try {
            tokenId = OAuthServiceUtils.authenticate(username, password, false);
        } catch (OAuthServiceException oe) {
            Logger.getLogger(NoBrowserAuthorization.class.getName()).log(Level.SEVERE, null, oe);
            return Response.status(FORBIDDEN).build();
        }

        if (tokenId == null) {
            return Response.status(BAD_REQUEST).build();
        }
        
        // Based on the session token, get the UUID of the user
        String subject = null;
        try {
            subject = OAuthServiceUtils.getUUIDByTokenId(tokenId);
        } catch (OAuthServiceException oe) {
            Logger.getLogger(NoBrowserAuthorization.class.getName()).log(Level.SEVERE, null, oe);
            return Response.status(FORBIDDEN).build();
        }

        if (subject == null) {
            return Response.status(FORBIDDEN).build();
        }
        
        OAuthResourceManager oauthResMgr = OAuthResourceManager.getInstance();
        try {
            Map<String, String> searchMap = new HashMap<String, String>();
            searchMap.put(REQUEST_TOKEN_URI, requestToken);
            List<RequestToken> reqTokens= oauthResMgr.searchRequestTokens(searchMap);
            RequestToken rt = null;
            if ((reqTokens != null) && (!reqTokens.isEmpty())) {
                rt = reqTokens.get(0);
            }
            if (rt == null) {
                throw new WebApplicationException(new Throwable(
                                 "Request token invalid."));
            }
            rt.setReqtPpalid(subject);
            oauthResMgr.updateRequestToken(rt);
            return Response.ok().build();
        } catch (OAuthServiceException e) {
            Logger.getLogger(NoBrowserAuthorization.class.getName()).log(Level.SEVERE, null, e);
            throw new WebApplicationException(e);
        }
    }

}
