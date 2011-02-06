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
 * $Id: RequestTokenResource.java,v 1.1 2009/11/20 19:31:57 huacui Exp $
 *
 */

package com.sun.identity.oauth.service;

import com.sun.identity.oauth.service.models.RequestToken;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
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

@Path(PathDefs.REQUEST_TOKENS_PATH + "/{id}")
public class RequestTokenResource implements OAuthServiceConstants {
    @Context
    private UriInfo context;

    @DELETE
    @Consumes(MediaType.TEXT_PLAIN)
    public Response deleteReqtoken() {
        OAuthResourceManager oauthResMgr = OAuthResourceManager.getInstance();
        try {
            String tokenuri = context.getAbsolutePath().toString();
            Map<String, String> searchMap = new HashMap<String, String>();
            searchMap.put(REQUEST_TOKEN_URI, tokenuri);
            List<RequestToken> reqTokens= oauthResMgr.searchRequestTokens(searchMap);
            RequestToken token = null;
            if ((reqTokens != null) && (!reqTokens.isEmpty())) {
                token = reqTokens.get(0);
            }
            if (token == null) {
                return Response.status(UNAUTHORIZED).build();
            }
            oauthResMgr.deleteRequestToken(token);
            return Response.ok().build();
        } catch (OAuthServiceException e) {
            Logger.getLogger(RequestTokenResource.class.getName()).log(Level.SEVERE, null, e);
            throw new WebApplicationException(e);
        }
    }

}
