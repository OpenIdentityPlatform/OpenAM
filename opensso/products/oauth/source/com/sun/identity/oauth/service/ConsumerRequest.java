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
 * $Id: ConsumerRequest.java,v 1.3 2009/12/15 01:27:48 huacui Exp $
 *
 */

package com.sun.identity.oauth.service;

import com.sun.identity.oauth.service.models.Consumer;
import com.sun.identity.oauth.service.util.UniqueRandomString;
import com.sun.jersey.oauth.signature.HMAC_SHA1;
import com.sun.jersey.oauth.signature.RSA_SHA1;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.WebApplicationException;

/**
 * REST Web Service
 *
 * Endpoint for Service Consumer Registration
 *
 * @author Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
 * @author Hua Cui <hua.cui@Sun.COM>
 */
@Path(PathDefs.CONSUMER_REGISTRATION_PATH)
public class ConsumerRequest implements OAuthServiceConstants {
    @Context
    private UriInfo context;

    /** Creates a new instance of ConsumersRegistration */
    public ConsumerRequest() {
    }



    /**
     * POST method for registering a Service Consumer
     * and obtaining corresponding consumer key & secret.
     *
     * @param formParams {@link String} containing the service 
     * consumer's description.
     * This description takes the form of name=value pairs separated by &.
     * The following parameters are supported:
     * <OL>
     * <LI>name - the service consumer's name.</LI>
     * <LI>icon - the service consumer's URI for its icon (MUST be unique).</LI>
     * <LI>service - the service consumer's URI for its service</LI>
     * <LI>rsapublickey - (optional) the RSA public key of the Service Consumer.</LI>
     * </OL>
     * <p>
     *
     * Example of string:
     * <pre>
     *  name=Service XYZ&icon=http://www.example.com/icon.jpg&service=http://www.example.com
     * </pre>
     *
     *
     * @return an HTTP response with content of the created resource.
     * The location URI is set to the newly created OAuth consumer key.
     * The body of the response is of the form:
     * <pre>
     * consumer_key=http://serviceprovider/0123456762121
     * consumer_secret=12345633
     * </pre>
     * Both values are URL encoded.
     */
    @POST
    @Consumes("application/x-www-form-urlencoded")
    public Response postConsumerRegistrations(MultivaluedMap<String, String> formParams) {
        OAuthResourceManager oauthResMgr = OAuthResourceManager.getInstance();
        try {
            Consumer cons = new Consumer();
            String cert = null;
            String tmpsecret = null;
            Boolean keyed = false;

            Set<String> pnames = formParams.keySet();
            Iterator<String> iter = pnames.iterator();
            while (iter.hasNext()) {
                String key = iter.next();
                String val = formParams.getFirst(key);
                if (key.equalsIgnoreCase(C_NAME)) {
                    // Check if a consumer with the same name is already registered,
                    // if so, will not do the registration again.
                    String consumerName = URLDecoder.decode(val);
                    Map<String, String> searchMap = new HashMap<String, String>();
                    searchMap.put(CONSUMER_NAME, consumerName);
                    List<Consumer> consumers= oauthResMgr.searchConsumers(searchMap);
                    if ((consumers != null) && (!consumers.isEmpty())) {
                        String resp = "A consumer is already registered with name "
                                      + consumerName + ".";
                        return Response.ok().entity(resp)
                           .type(MediaType.APPLICATION_FORM_URLENCODED).build();
                    }
                    cons.setConsName(consumerName);
                } else if (key.equalsIgnoreCase(C_CERT)) {
                    cert = val; // The cert is in PEM format (no URL decode needed)
                } else if (key.equalsIgnoreCase(C_SECRET)) {
                    tmpsecret = URLDecoder.decode(val);
                } else if (key.equalsIgnoreCase(C_KEY)) {
                    keyed = true;
                    // Check if a consumer with the same key is already registered,
                    // if so, will not do the registration again.
                    String consumerKey = URLDecoder.decode(val);
                    cons.setConsKey(consumerKey);
                    Map<String, String> searchMap = new HashMap<String, String>();
                    searchMap.put(CONSUMER_KEY, consumerKey);
                    List<Consumer> consumers= oauthResMgr.searchConsumers(searchMap);
                    if ((consumers != null) && (!consumers.isEmpty())) {
                        String resp = "A consumer is already registered with key "
                                      + consumerKey + ".";
                        return Response.ok().entity(resp)
                           .type(MediaType.APPLICATION_FORM_URLENCODED).build();
                    }
                } else {
                    // anything else is ignored for the time being
                }
            }

            if (cert != null) {
                cons.setConsRsakey(cert);
            }

            if (tmpsecret != null) {
                cons.setConsSecret(tmpsecret);
            } else {
                cons.setConsSecret(new UniqueRandomString().getString());
            }
            
            if (!keyed) {
                String baseUri = context.getBaseUri().toString();
                if (baseUri.endsWith("/"))
                        baseUri = baseUri.substring(0, baseUri.length() - 1);
                URI loc = URI.create(baseUri + PathDefs.CONSUMERS_PATH +
                          "/" + new UniqueRandomString().getString());
                String consKey = loc.toString();
                cons.setConsKey(consKey);
            }
            oauthResMgr.createConsumer(null, cons);

            String resp = "consumer_key=" + URLEncoder.encode(cons.getConsKey())
               + "&consumer_secret=" + URLEncoder.encode(cons.getConsSecret());
            return Response.created(URI.create(cons.getConsKey())).entity(resp)
                           .type(MediaType.APPLICATION_FORM_URLENCODED).build();
        } catch (OAuthServiceException e) {
            Logger.getLogger(ConsumerRequest.class.getName()).log(Level.SEVERE, null, e);
            throw new WebApplicationException(e);
        }
    }
}
