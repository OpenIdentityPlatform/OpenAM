/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms of the Common
 * Development and Distribution License (the License). You may not use
 * this file except in compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL Header Notice in each
 * file and include the License file at opensso/legal/CDDLv1.0.txt. If
 * applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: ListenerResource.java,v 1.5 2009/12/15 00:44:19 veiming Exp $
 *
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package com.sun.identity.rest;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.EntitlementListener;
import com.sun.identity.entitlement.ListenerManager;
import java.util.List;
import javax.security.auth.Subject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import org.json.JSONException;

/**
 * Exposes the entitlement listener REST resource.
 */
@Path("/1/entitlement/listener")
public class ListenerResource extends ResourceBase {
    @POST
    @Produces("application/json")
    public String addListener(
        @Context HttpHeaders headers,
        @Context HttpServletRequest request,
        @FormParam("url") String url,
        @FormParam("resources") List<String> resources,
        @FormParam("application") @DefaultValue("iPlanetAMWebAgentService")
            String application
    ) {
        try {
            Subject caller = getCaller(request);
            EntitlementListener l = new EntitlementListener(url,
                application, resources);
            ListenerManager.getInstance().addListener(caller, l);
            return createResponseJSONString(201, headers, "Created");
        } catch (RestException e) {
            throw getWebApplicationException(headers, e, MimeType.JSON);
        } catch (JSONException e) {
            throw getWebApplicationException(e, MimeType.JSON);
        } catch (EntitlementException e) {
            throw getWebApplicationException(headers, e, MimeType.JSON);
        }
    }

    @DELETE
    @Produces("application/json")
    @Path("/{url}")
    public String deleteListener(
        @Context HttpHeaders headers,
        @Context HttpServletRequest request,
        @PathParam("url") String url
    ) {
        try {
            Subject caller = getCaller(request);
            ListenerManager.getInstance().removeListener(caller, url);
            return createResponseJSONString(200, headers, "OK");
        } catch (RestException e) {
            throw getWebApplicationException(headers, e, MimeType.JSON);
        } catch (JSONException e) {
            throw getWebApplicationException(e, MimeType.JSON);
        } catch (EntitlementException e) {
            throw getWebApplicationException(headers, e, MimeType.JSON);
        }
    }

    @GET
    @Produces("application/json")
    @Path("/{url}")
    public String getListener(
        @Context HttpHeaders headers,
        @Context HttpServletRequest request,
        @PathParam("url") String url
    ) {
        try {
            Subject caller = getCaller(request);
            EntitlementListener listener = ListenerManager.getInstance()
                .getListener(caller, url);
            if (listener == null) {
                String[] param = {url.toString()};
                throw new EntitlementException(427, param);
            }
            return createResponseJSONString(200, headers,
                listener.toJSON());
        } catch (JSONException e) {
            throw getWebApplicationException(e, MimeType.JSON);
        } catch (RestException e) {
            throw getWebApplicationException(headers, e, MimeType.JSON);
        } catch (EntitlementException e) {
            throw getWebApplicationException(headers, e, MimeType.JSON);
        }
    }
}

