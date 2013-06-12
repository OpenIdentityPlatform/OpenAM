/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock Inc. All rights reserved.
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
 * "Portions copyright [year] [name of copyright owner]"
 */
package org.forgerock.openam.oauth2.openid;

import com.sun.identity.idm.AMIdentity;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OpenIDConnectDiscovery extends ServerResource {

    @Get
    public Representation discovery(){

        String resource = OAuth2Utils.getRequestParameter(getRequest(), "resource", String.class);
        String rel = OAuth2Utils.getRequestParameter(getRequest(), "rel", String.class);
        String realm = OAuth2Utils.getRealm(getRequest());

        if (resource == null || resource.isEmpty()){
            OAuth2Utils.DEBUG.error("OpenIDConnectDiscovery.discover()::No resource provided in discovery.");
            throw OAuthProblemException.OAuthError.BAD_REQUEST.handle(null,
                    "OpenIDConnectDiscovery.discover()::No resource provided in discovery.");
        }

        if (rel == null || rel.isEmpty() || !rel.equalsIgnoreCase("http://openid.net/specs/connect/1.0/issuer")){
            OAuth2Utils.DEBUG.error("OpenIDConnectDiscovery.discover()::No or invalid rel provided in discovery.");
            throw OAuthProblemException.OAuthError.BAD_REQUEST.handle(null,
                    "OpenIDConnectDiscovery.discover()::No or invalid rel provided in discovery.");
        }

        /*
         Response format
         {
         "subject": "https://example.com:8080/",
         "links":
         [
         {
         "rel": "http://openid.net/specs/connect/1.0/issuer",
         "href": "https://server.example.com"
         }
         ]
         }
         */

        String userid = null;

        //test if the resource is a uri
        try {
            URI object = new URI(resource);
            if (object.getScheme().equalsIgnoreCase("https") ||
                object.getScheme().equalsIgnoreCase("http")){
                //resource is of the form of https://example.com/
                if (object.getPath().isEmpty()){
                } else {
                    //resource is of the form of https://example.com/joe
                    userid = object.getPath();
                    userid = userid.substring(1,userid.length());
                }
            } else if (object.getScheme().equalsIgnoreCase("acct")) {
                //resource is not uri so only option is it is an email of form acct:joe@example.com
                String s = new String(resource);
                s = s.replaceFirst("acct:", "");
                int firstAt = s.indexOf('@');
                userid = s.substring(0,firstAt);
            } else {
                OAuth2Utils.DEBUG.error("OpenIDConnectDiscovery.discover()::Invalid parameters.");
                throw OAuthProblemException.OAuthError.BAD_REQUEST.handle(null,
                        "OpenIDConnectDiscovery.discover()::Invalid parameters.");
            }
        } catch (Exception e){
            OAuth2Utils.DEBUG.error("OpenIDConnectDiscovery.discover()::Invalid parameters.", e);
            throw OAuthProblemException.OAuthError.BAD_REQUEST.handle(null,
                "OpenIDConnectDiscovery.discover()::Invalid parameters.");
        }

        if (userid != null){
            //check if user exists on the server.
            AMIdentity id = null;
            try {
                id = OAuth2Utils.getIdentity(userid, realm);
            } catch (Exception e){
                OAuth2Utils.DEBUG.error("OpenIDConnectDiscovery.discover()::Invalid parameters.", e);
                throw OAuthProblemException.OAuthError.NOT_FOUND.handle(null,
                        "OpenIDConnectDiscovery.discover()::Invalid parameters.");
            }
            if (id == null){
                OAuth2Utils.DEBUG.error("OpenIDConnectDiscovery.discover()::Invalid parameters.");
                throw OAuthProblemException.OAuthError.NOT_FOUND.handle(null,
                        "OpenIDConnectDiscovery.discover()::Invalid parameters.");
            }
        }

        Map<String, Object> response = new HashMap<String, Object>();
        response.put("subject", resource);
        Set<Object> set = new HashSet<Object>();
        Map<String, Object> objectMap = new HashMap<String, Object>();
        objectMap.put("rel", rel);
        objectMap.put("href", OAuth2Utils.getDeploymentURL(getRequest()));
        set.add(objectMap);
        response.put("links",set);

        return new JsonRepresentation(response);
    }
}
