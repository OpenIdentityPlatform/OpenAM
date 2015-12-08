/*
* The contents of this file are subject to the terms of the Common Development and
* Distribution License (the License). You may not use this file except in compliance with the
* License.
*
* You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
* specific language governing permission and limitations under the License.
*
* When distributing Covered Software, include this CDDL Header Notice in each file and include
* the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
* Header, with the fields enclosed by brackets [] replaced by your own identifying
* information: "Portions copyright [year] [name of copyright owner]".
*
* Copyright 2014-2015 ForgeRock AS.
*/
package org.forgerock.openam.forgerockrest.utils;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;

import java.util.Locale;
import java.util.Map;

import org.forgerock.json.resource.ActionRequest;
import org.forgerock.services.context.Context;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.services.context.SecurityContext;

/**
 * For the convenience of generating information from different types of ServerContexts.
 */
public class ServerContextUtils {

    public static final String CREATE = "CREATE";
    public static final String READ = "READ";
    public static final String UPDATE = "UPDATE";
    public static final String DELETE = "DELETE";
    public static final String PATCH = "PATCH";
    public static final String ACTION = "ACTION";
    public static final String QUERY = "QUERY";

    public static final String ACCEPT_LANGUAGE = "accept-language";

    /**
     * Retrieves a link to the user's SSO Token, if it exists in the context.
     * @param context from which to pull the SSO Token
     */
    public static SSOToken getTokenFromContext(Context context, Debug debug) {
        if (context.containsContext(SSOTokenContext.class)) {
            return context.asContext(SSOTokenContext.class).getCallerSSOToken();
        } else if (context.containsContext(SecurityContext.class)) {
            try {
                return SSOTokenContext.getSsoToken(context);
            } catch (SSOException e) {
                debug.message("Unable to retrieve caller's SSOToken from context.", e);
                return null;
            }
        } else {
                debug.message("No security context found from which caller's SSOToken could be retrieved.");
                return null;
        }
    }

    /**
     * Returns the UriRouterContext's "id" UriTemplateVariable from the provided Context.
     *
     * @param context from which to pull the id
     * @return the id, otherwise null.
     */
    public static String getId(Context context) {
        if (context.containsContext(UriRouterContext.class)) {
            UriRouterContext routerContext = context.asContext(UriRouterContext.class);
            Map<String, String> templateVars = routerContext.getUriTemplateVariables();

            if (templateVars != null && !templateVars.isEmpty()) {
                return templateVars.get("id");
            }
        }

        return null;
    }

    /**
     * Returns the UriRouterContext's matchedUri, and appends its id, if there is one.
     * Id is retrieved via {@link ServerContextUtils#getId(Context)}.
     *
     * @param context from which to gather the matched Uri and id information
     * @return a String in the form <code>matchedUri | id</code>, omitting either if they are null.
     */
    public static String getMatchedUri(Context context) {
        String resource = "";
        if (context.containsContext(UriRouterContext.class)) {
            UriRouterContext routerContext = context.asContext(UriRouterContext.class);
            resource = routerContext.getMatchedUri();
        }

        String id = getId(context);

        if (id != null) {
            resource += "|" + id;
        }

        return resource;
    }

    /**
     * Returns the name of the resource requested, and appends its id, if there is one.
     * Id is retrieved via {@link ServerContextUtils#getId(Context)}. If the resource path can not be found on the
     * request, the Matched Uri on the context will be queried via {@link ServerContextUtils#getMatchedUri(Context)}.
     *
     * @param request the request for a resource
     * @param context the context of the request, including its UriRouterContext
     * @return a String in the form <code>resourceName | id</code>, omitting either if they are null.
     */
    public static String getResourceId(Request request, Context context) {
        String resource = request.getResourcePath();
        if (StringUtils.isEmpty(resource)) {
            return getMatchedUri(context);
        }

        String id = getId(context);

        if (id != null) {
            resource += "|" + id;
        }

        return resource;
    }

    /**
     * Generates for logging a String in the form <code>CREATE | newResourceId</code> from the CreateRequest.
     * @param request from which to generate information
     * @return the logging string
     */
    public static String getCreateString(CreateRequest request) {
        String action = CREATE;

        if (StringUtils.isNotEmpty(request.getNewResourceId())) {
            action += "|" + request.getNewResourceId();
        }

        return action;
    }

    /**
     * Generates for logging a String in the form <code>READ</code> from the CreateRequest.
     * @param request from which to generate information
     * @return the logging string
     */
    public static String getReadString(ReadRequest request) {
        return READ;
    }

    /**
     * Generates for logging a String in the form <code>ACTION | actionId</code> from the CreateRequest.
     * @param request from which to generate information
     * @return the logging string
     */
    public static String getActionString(ActionRequest request) {
        String action = ACTION;

        if (StringUtils.isNotEmpty(request.getAction())) {
            action += "|" + request.getAction();
        }

        return action;
    }

    /**
     * Generates for logging a String in the form <code>DELETE | revision</code> from the CreateRequest.
     * @param request from which to generate information
     * @return the logging string
     */
    public static String getDeleteString(DeleteRequest request) {
        String action = DELETE;

        if (StringUtils.isNotEmpty(request.getRevision())) {
            action += "|" + request.getRevision();
        }

        return action;
    }

    /**
     * Generates for logging a String in the form <code>PATCH | resourceRevision</code> from the PatchRequest.
     * @param request from which to generate information
     * @return the logging string
     */
    public static String getPatchString(PatchRequest request) {
        String action = PATCH;

        if (StringUtils.isNotEmpty(request.getRevision())) {
            action += "|" + request.getRevision();
        }

        return action;
    }

    /**
     * Generates for logging a String in the form <code>UPDATE | resourceRevision</code> from the UpdateRequest.
     * @param request from which to generate information
     * @return the logging string
     */
    public static String getUpdateString(UpdateRequest request) {
        String action = UPDATE;

        if (StringUtils.isNotEmpty(request.getRevision())) {
            action += "|" + request.getRevision();
        }

        return action;
    }

    /**
     * Generates for logging a String in the form <code>QUERY | queryId</code> from the QueryRequest.
     * @param request from which to generate information
     * @return the logging string
     */
    public static String getQueryString(QueryRequest request) {

        String action = QUERY;

        if (StringUtils.isNotEmpty(request.getQueryId())) {
            action += "|" + request.getQueryId();
        }

        return action;

    }

    /**
     * Get the Context as an HttpContext, read the accept-language from the
     * header and create a Locale object from that.
     *
     * @param context The server context from which the language header can be read.
     * @return The Local instance or null if no accept-language header was found.
     */
    public static Locale getLocaleFromContext(Context context) {
        if (context == null) {
            return null;
        }
        Locale locale;
        try {
            final String language = context.asContext(HttpContext.class).getHeaderAsString(ACCEPT_LANGUAGE);
            locale = com.sun.identity.shared.locale.Locale.getLocaleObjFromAcceptLangHeader(language);
        } catch (IllegalArgumentException iae) {
            locale = null;
        }
        return locale;
    }

    /**
     * Gets the resolved realm from the context.
     * @param context The context.
     * @return The resolved realm.
     */
    public static String getRealm(Context context) {
        return context.asContext(RealmContext.class).getResolvedRealm();
    }
}
