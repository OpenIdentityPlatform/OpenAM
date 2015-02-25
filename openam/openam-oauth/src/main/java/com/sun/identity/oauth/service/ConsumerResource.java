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
 * $Id: ConsumerResource.java,v 1.2 2009/12/09 21:47:12 huacui Exp $
 *
 */

package com.sun.identity.oauth.service;

import com.sun.identity.oauth.service.models.Consumer;
import com.sun.jersey.oauth.signature.HMAC_SHA1;
import com.sun.jersey.oauth.signature.RSA_SHA1;
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
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.WebApplicationException;

/**
 * Service Consumer resource handling.
 *
 *
 * @author Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
 * @author Hua Cui <hua.cui@Sun.COM>
 */

@Path(PathDefs.CONSUMERS_PATH + "/{cid}")
public class ConsumerResource implements OAuthServiceConstants {
    @Context
    private UriInfo context;

    /**
     * GET method for retrieving a specific Service Consumer instance
     * and obtaining corresponding metadata (consumer name, URI, secret).
     *
     * @param consID The comsumer ID 
     * @param sigmethod {@link String} to choose the signature algorithm
     * of interest (e.g. <PRE>?signature_method=RSA-SHA1</PRE> will return
     * the RSA public key of the service consumer).
     *
     * @return an HTTP response with URL encoded value of the service metadata.
     */
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    public Response getRegistration(@PathParam(C_ID) String consID,
            @QueryParam(C_SIGNATURE_METHOD) String sigmethod) {
        OAuthResourceManager oauthResMgr = OAuthResourceManager.getInstance();
        try {
            String name =null;
            String icon = null;
            String ckey = context.getAbsolutePath().toString();
            Map<String, String> searchMap = new HashMap<String, String>();
            searchMap.put(CONSUMER_KEY, ckey);
            List<Consumer> consumers= oauthResMgr.searchConsumers(searchMap);
            if ((consumers == null) || consumers.isEmpty()) {
                throw new WebApplicationException(new Throwable(
                     "Consumer key is missing."), BAD_REQUEST);
            }
            Consumer consumer = consumers.get(0);    

            String cs = null;
            if (sigmethod != null) {
                if (sigmethod.equalsIgnoreCase(RSA_SHA1.NAME)) {
                    cs = URLEncoder.encode(consumer.getConsRsakey());
                } else {
                    cs = URLEncoder.encode(consumer.getConsSecret());
                }
            }
            if (consumer.getConsName() != null) {
                name = URLEncoder.encode(consumer.getConsName());
            }
            String resp = C_KEY + "=" + URLEncoder.encode(ckey);
            if (name != null) {
                resp += "&" + C_NAME + "=" + name;
            }
            if (cs != null) {
                resp += "&" + C_SECRET + "=" + cs;
            }
            return Response.ok(resp, MediaType.TEXT_PLAIN).build();
        } catch (OAuthServiceException e) {
            Logger.getLogger(ConsumerResource.class.getName()).log(Level.SEVERE, null, e);
            throw new WebApplicationException(e);
        }
    }

    @DELETE
    @Consumes(MediaType.TEXT_PLAIN)
    public Response deleteRegistration() {
        OAuthResourceManager oauthResMgr = OAuthResourceManager.getInstance();
        try {
            String consKey = context.getAbsolutePath().toString();
            Map<String, String> searchMap = new HashMap<String, String>();
            searchMap.put(CONSUMER_KEY, consKey);
            List<Consumer> consumers= oauthResMgr.searchConsumers(searchMap);
            if ((consumers == null) || consumers.isEmpty()) {
                return Response.status(UNAUTHORIZED).build();
            }
            Consumer consumer = consumers.get(0);    
            oauthResMgr.deleteConsumer(consumer);
            return Response.ok().build();
        } catch (OAuthServiceException e) {
            Logger.getLogger(ConsumerResource.class.getName()).log(Level.SEVERE, null, e);
            throw new WebApplicationException(e);
        }
    }

}
