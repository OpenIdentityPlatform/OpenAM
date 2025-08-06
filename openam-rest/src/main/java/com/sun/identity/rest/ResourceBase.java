/*
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
 * $Id: ResourceBase.java,v 1.4 2009/11/25 18:09:51 veiming Exp $
 *
 * Portions Copyright 2015 ForgeRock AS.
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package com.sun.identity.rest;

import javax.security.auth.Subject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import com.sun.identity.coretoken.CoreTokenException;
import com.sun.identity.entitlement.EntitlementException;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class ResourceBase {

    public static final String STATUS_CODE = "statusCode";
    public static final String STATUS_MESSAGE = "statusMessage";
    public static final String BODY = "body";

    public enum MimeType {PLAIN, JSON};

    private static final String RES_BUNDLE_NAME = "RestException";

    protected Subject getCaller(HttpServletRequest req)
        throws RestException {
        Principal p = req.getUserPrincipal();
        if (p != null) {
            if (p instanceof ISubjectable) {
                try {
                    return ((ISubjectable)p).createSubject();
                } catch (Exception e) {
                    throw new RestException(1, e);
                }
            }
            return toSubject(p.getName());
        }
        throw new RestException(2);
    }

    protected Subject getSubject(HttpServletRequest request) 
        throws RestException {
        return RestServiceManager.getInstance().getAuthZSubject(request);
    }

    protected Subject toSubject(Principal principal) {
        if (principal == null) {
            return null;
        }
        Set<Principal> set = new HashSet<Principal>();
        set.add(principal);
        return new Subject(false, set, new HashSet(), new HashSet());
    }

    protected Subject toSubject(String subject) {
        return (subject == null) ? null :
            toSubject(new AuthSPrincipal(subject));
    }

    protected WebApplicationException getWebApplicationException(
        HttpHeaders headers,
        RestException e,
        MimeType mimeType
    ) {
        if (MimeType.JSON == mimeType) {
            return getWebApplicationException(400, e.getErrorCode(),
                e.getLocalizedMessage(getUserLocale(headers)));
        } else {
            return new WebApplicationException(
                Response.status(400)
                .entity(getLocalizedMessage(headers, 400))
                .type("text/plain; charset=UTF-8").build());
        }
    }

    protected WebApplicationException getWebApplicationException(
        HttpHeaders headers,
        EntitlementException e,
        MimeType mimeType
    ) {
        if (MimeType.JSON == mimeType) {
            return getWebApplicationException(400, e.getErrorCode(),
                e.getLocalizedMessage(getUserLocale(headers)));
        } else {
            return new WebApplicationException(
                Response.status(400)
                .entity(getLocalizedMessage(headers, 400))
                .type("text/plain; charset=UTF-8").build());
        }
    }

    protected WebApplicationException getWebApplicationException(
        HttpHeaders headers,
        CoreTokenException e
    ) {
        return new WebApplicationException (
              Response.status(e.getHttpStatusCode())
              .entity(e.getLocalizedMessage(getUserLocale(headers)))
              .type("text/plain; charset=UTF-8").build());
    }

    protected WebApplicationException getWebApplicationException(
        JSONException e, 
        MimeType mimeType
    ) {
        return getWebApplicationException(400, e, mimeType);
    }

    protected WebApplicationException getWebApplicationException(
        int statusCode,
        Exception e,
        MimeType mimeType
    ) {
        if (MimeType.JSON == mimeType) {
            String statusMessage = e.getLocalizedMessage();
            return getWebApplicationException(statusCode, statusCode,
                statusMessage);
        } else {
            return new WebApplicationException(
                Response.status(statusCode).entity(e.getLocalizedMessage()).
                type("text/plain; charset=UTF-8").build());
        }
    }

    private WebApplicationException getWebApplicationException(
        int statusCode,
        int errorCode,
        String message
    ) {
        String responseJsonString = createResponseJSONString(
            errorCode, message, null);
        ResponseBuilder responseBuilder = Response.status(statusCode);
        responseBuilder.status(statusCode);
        responseBuilder.entity(responseJsonString);
        responseBuilder.type("applicaton/json; charset=UTF-8");
        return new WebApplicationException(responseBuilder.build());
    }
    
    protected Locale getUserLocale(HttpHeaders headers) {
        List<Locale> locales = headers.getAcceptableLanguages();
        return ((locales == null) || locales.isEmpty()) ? Locale.getDefault() :
            locales.get(0);
    }

    protected String createResponseJSONString(
        int statusCode,
        HttpHeaders headers,
        JSONObject body
    ) {
        return createResponseJSONString(statusCode,
            getLocalizedMessage(headers, statusCode), body);
    }

    protected String createResponseJSONString(
        int statusCode,
        HttpHeaders headers,
        String strBody
    ) throws JSONException {
        return createStringResponseJSONString(statusCode,
            getLocalizedMessage(headers, statusCode), strBody);
    }

    private JSONObject createResponseJSON(
        int statusCode,
        String statusMessage
    ) throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put(STATUS_CODE, statusCode);
        if (statusMessage != null) {
            jo.put(STATUS_MESSAGE, statusMessage);
        }
        return jo;
    }

    protected String createStringResponseJSONString(
        int statusCode,
        String statusMessage,
        String strBody
    ) {
        try {
            JSONObject jo = createResponseJSON(statusCode, statusMessage);
            if (strBody != null) {
                jo.put(BODY, strBody);
            }
            return jo.toString();
        } catch (JSONException je) {
            RestServiceManager.DEBUG.error(
                    "ResourceBase.createeResponseJSONString(): hit JSONException",
                    je);
        }
        return "{}";
    }

    protected String createResponseJSONString(
        int statusCode,
        String statusMessage,
        JSONObject body
    ) {
        try {
            JSONObject jo = createResponseJSON(statusCode, statusMessage);
            if (body != null) {
                jo.put(BODY, body);
            }
            return jo.toString();
        } catch (JSONException je) {
            RestServiceManager.DEBUG.error(
                "ResourceBase.createeResponseJSONString(): hit JSONException",
                je);
        }
        return "{}";
    }

    private String getLocalizedMessage(
        HttpHeaders headers,
        int errorCode
    ) {
        Locale locale = getUserLocale(headers);
        return getLocalizedMessage(locale, errorCode);
    }

    /**
     * Returns localized exception message.
     *
     * @param locale Locale of the message.
     * @param errorCode errorCode of the message
     * @return localized exception message.
     */
    protected String getLocalizedMessage(Locale locale, int errorCode) {
        ResourceBundle rb = ResourceBundle.getBundle(RES_BUNDLE_NAME, locale);
        return rb.getString(Integer.toString(errorCode));
    }
}


