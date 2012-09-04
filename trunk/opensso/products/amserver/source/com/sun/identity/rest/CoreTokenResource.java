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
 * $Id: CoreTokenResource.java,v 1.1 2009/11/19 00:10:38 qcheng Exp $
 */

package com.sun.identity.rest;

import com.sun.identity.coretoken.CoreTokenConstants;
import com.sun.identity.coretoken.CoreTokenException;
import com.sun.identity.coretoken.CoreTokenStoreFactory;
import com.sun.identity.coretoken.CoreTokenUtils;
import com.sun.identity.coretoken.TokenLogUtils;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Defines REST resources for Core Token Service.
 * 
 */
@Path("/1/token")
public class CoreTokenResource extends ResourceBase {
    /**
     * Creates a token.
     *
     * @param headers HTTPHeaders object of the request.
     * @param request HTTPServletRequest object of the request.
     * @param msgBody Message body containing the JSON-encoded token attributes.
     * @return JSON-encoded token.id attribute of the new token.
     */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response createToken(
        @Context HttpHeaders headers,
        @Context HttpServletRequest request,
        String msgBody) {
        String newTokenId = null;
        JSONObject json = null;
        try {
            json = new JSONObject(msgBody);
            String tokenVal = CoreTokenStoreFactory.getInstance().createToken(
                CoreTokenUtils.getAdminSubject(), json);
            // retrieve token.id attribute and set as part of Location header
            JSONObject jObj = new JSONObject(tokenVal);
            newTokenId = jObj.getJSONArray(CoreTokenConstants.TOKEN_ID)
                .getString(0);
            Response.ResponseBuilder builder = Response.status(201);
            builder.entity(tokenVal);
            builder.type("application/json");
            builder.header("Location", request.getRequestURL() + "/"
                + newTokenId);
            Response retResponse = builder.build();

            // logging
            // TODO : get the request session and used in login field
            String[] data = new String[] {
                json.getJSONArray(CoreTokenConstants.TOKEN_TYPE).toString(),
                json.getJSONArray(CoreTokenConstants.TOKEN_SUBJECT).toString(),
                json.names().toString()};
            TokenLogUtils.access(Level.INFO,
                TokenLogUtils.TOKEN_CREATE_SUCCESS, data, null, newTokenId);

            return retResponse;
        } catch (JSONException ex) {
            CoreTokenUtils.debug.error("CoreTokenResource.createToken", ex);
            String[] data = null;
            if (json != null) {
                try {
                    data = new String[]{
                    ex.getLocalizedMessage(),
                    json.getJSONArray(CoreTokenConstants.TOKEN_TYPE).toString(),
                    json.getJSONArray(CoreTokenConstants.TOKEN_SUBJECT).toString(),
                    json.names().toString()};
                } catch (JSONException ex1) {
                }
            } else {
                data = new String[]{ex.getLocalizedMessage(), "", "", ""};
            }
            TokenLogUtils.error(Level.INFO,
                TokenLogUtils.UNABLE_TO_CREATE_TOKEN, data, null, newTokenId);
            throw getWebApplicationException(ex, MimeType.PLAIN);
        } catch (CoreTokenException ce) {
            CoreTokenUtils.debug.error("CoreTokenResource.createToken", ce);
            String[] data = null;
            if (json != null) {
                try {
                    data = new String[]{
                    ce.getLocalizedMessage(),
                    json.getJSONArray(CoreTokenConstants.TOKEN_TYPE).toString(),
                    json.getJSONArray(CoreTokenConstants.TOKEN_SUBJECT).toString(),
                    json.names().toString()};
                } catch (JSONException ex1) {
                }
            } else {
                data = new String[]{ce.getLocalizedMessage(), "", "", ""};
            }
            TokenLogUtils.error(Level.INFO,
                TokenLogUtils.UNABLE_TO_CREATE_TOKEN, data, null, newTokenId);
            throw getWebApplicationException(headers, ce);
        }
    }

    /**
     * Reads token attributes.
     *
     * @param headers HTTPHeaders object of the request.
     * @param request HTTPServletRequest object of the request.
     * @param tokenId token.id of the token to be retrieved.
     * @return JSON-encoded token attributes.
     */
    @GET
    @Produces("application/json")
    @Path("{token.id}")
    public Response readToken(
        @Context HttpHeaders headers,
        @Context HttpServletRequest request,
        @PathParam("token.id") String tokenId) {
        try {
            String tokenVal = CoreTokenStoreFactory.getInstance().readToken(
                CoreTokenUtils.getAdminSubject(), tokenId);
            JSONObject jObj = new JSONObject(tokenVal);
            // retrieve etag attribute and set it as ETag header value.
            String eTag = jObj.getJSONArray(CoreTokenConstants.VERSION_TAG)
                .getString(0);
            // remove version tag in return
            jObj.remove(CoreTokenConstants.VERSION_TAG);
            Response.ResponseBuilder builder = Response.status(200);
            builder.entity(jObj.toString());
            builder.type("application/json");
            builder.header("ETag", eTag);
            Response retResponse = builder.build();

            // logging
            String[] data = new String[] {
                jObj.getJSONArray(CoreTokenConstants.TOKEN_TYPE).toString(),
                jObj.getJSONArray(CoreTokenConstants.TOKEN_SUBJECT).toString()};
            TokenLogUtils.access(Level.INFO,
                TokenLogUtils.TOKEN_READ_SUCCESS, data, null, tokenId);

            return retResponse;
        } catch (CoreTokenException ce) {
            CoreTokenUtils.debug.error("CoreTokenResource.readToken", ce);
            String[] data = new String[] {ce.getLocalizedMessage()};
            TokenLogUtils.error(Level.INFO,
                TokenLogUtils.UNABLE_TO_READ_TOKEN, data, null, tokenId);
            throw getWebApplicationException(headers, ce);
        } catch (JSONException je) {
            CoreTokenUtils.debug.error("CoreTokenResource.readToken", je);
            String[] data = new String[] {je.getLocalizedMessage()};
            TokenLogUtils.error(Level.INFO,
                TokenLogUtils.UNABLE_TO_READ_TOKEN, data, null, tokenId);
            throw getWebApplicationException(je, MimeType.PLAIN);
        }
    }

    /**
     * Updates a token.
     *
     * @param headers HTTPHeaders object of the request.
     * @param request HTTPServletRequest object of the request.
     * @param tokenId value of token.id in the request path parameter.
     * @param eTag value of the If-Match header in the request.
     * @param msgBody Message body containing the JSON-encoded token attributes.
     */
    @PUT
    @Consumes("application/json")
    @Path("{token.id}")
    public void updateToken(
        @Context HttpHeaders headers,
        @Context HttpServletRequest request,
        @PathParam("token.id") String tokenId,
        @HeaderParam("If-Match") String eTag,
        String msgBody) {
        try {
            JSONObject jObj = new JSONObject(msgBody);
            CoreTokenStoreFactory.getInstance().updateToken(
                CoreTokenUtils.getAdminSubject(), tokenId, eTag, jObj);

            // logging
            String[] data = new String[] {jObj.names().toString()};
            TokenLogUtils.access(Level.INFO,
                TokenLogUtils.TOKEN_UPDATE_SUCCESS, data, null, tokenId);
        } catch (CoreTokenException ce) {
            CoreTokenUtils.debug.error("CoreTokenResource.updateToken", ce);
            String[] data = new String[] {ce.getLocalizedMessage()};
            TokenLogUtils.error(Level.INFO,
                TokenLogUtils.UNABLE_TO_UPDATE_TOKEN, data, null, tokenId);
            throw getWebApplicationException(headers, ce);
        } catch (JSONException je) {
            CoreTokenUtils.debug.error("CoreTokenResource.updateToken", je);
            String[] data = new String[] {je.getLocalizedMessage()};
            TokenLogUtils.error(Level.INFO,
                TokenLogUtils.UNABLE_TO_UPDATE_TOKEN, data, null, tokenId);
            throw getWebApplicationException(je, MimeType.PLAIN);
        }
    }

    /**
     * Deletes a token.
     *
     * @param headers HTTPHeaders object of the request.
     * @param request HTTPServletRequest object of the request.
     * @param tokenId value of token.id in the request path parameter.
     */
    @DELETE
    @Path("{token.id}")
    public void deleteToken(
        @Context HttpHeaders headers,
        @Context HttpServletRequest request,
        @PathParam("token.id") String tokenId) {
        try {
            CoreTokenStoreFactory.getInstance().deleteToken(
                CoreTokenUtils.getAdminSubject(), tokenId);

            // logging
            TokenLogUtils.access(Level.INFO,
                TokenLogUtils.TOKEN_DELETE_SUCCESS, null, null, tokenId);
        } catch (CoreTokenException ex) {
            CoreTokenUtils.debug.error("CoreTokenResource.deleteToken", ex);
            String[] data = new String[] {ex.getLocalizedMessage()};
            TokenLogUtils.error(Level.INFO,
                TokenLogUtils.UNABLE_TO_DELETE_TOKEN, data, null, tokenId);
            throw getWebApplicationException(headers, ex);
        } 
    }

    /**
     * Searches tokens.
     * 
     * @param headers HTTPHeaders object of the request.
     * @param request HTTPServletRequest object of the request.
     * @return JSON array of tokens matching the queryString
     */
    @GET
    @Produces("application/json")
    public String searchTokens(
        @Context HttpHeaders headers,
        @Context HttpServletRequest request) {
        String query = null;
        try {
            query = request.getQueryString();
            JSONArray jArray = CoreTokenStoreFactory.getInstance().searchTokens(
                CoreTokenUtils.getAdminSubject(), query);
            String retArray = jArray.toString();

            // logging
            String[] data = new String[] {query, "" + jArray.length()};
            TokenLogUtils.access(Level.INFO, 
                TokenLogUtils.TOKEN_SEARCH_SUCCESS, data, null, null);

            return retArray;
        } catch (CoreTokenException ex) {
            CoreTokenUtils.debug.error("CoreTokenResource.searchToken", ex);
            String[] data = new String[] {query, ex.getLocalizedMessage()};
            TokenLogUtils.error(Level.INFO,
                TokenLogUtils.UNABLE_TO_SEARCH_TOKEN, data, null, null);
            throw getWebApplicationException(headers, ex);
        }
    }
}
