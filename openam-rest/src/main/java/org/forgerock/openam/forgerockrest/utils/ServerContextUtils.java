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
* Copyright 2014 ForgeRock AS.
*/
package org.forgerock.openam.forgerockrest.utils;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import java.util.Map;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RouterContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.utils.StringUtils;

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

    /**
     * Retrieves a link to the user's SSO Token, if it exists in the context.
     * @param context from which to pull the SSO Token
     */
    public static SSOToken getTokenFromContext(ServerContext context, Debug debug) {

        SSOToken userToken = null;

        if (!context.containsContext(SSOTokenContext.class)) {
            context = new SSOTokenContext(context);
        }

        SSOTokenContext ssoTokenContext = context.asContext(SSOTokenContext.class);

        try {
            userToken = ssoTokenContext.getCallerSSOToken();
        } catch (SSOException e) {
            debug.message("Unable to retrieve caller's SSOToken from context.", e);
        }

        return userToken;
    }

    /**
     * Returns the RouterContext's "id" UriTemplateVariable from the provided ServerContext.
     *
     * @param context from which to pull the id
     * @return the id, otherwise null.
     */
    public static String getId(ServerContext context) {
        if (context.containsContext(RouterContext.class)) {
            RouterContext routerContext = context.asContext(RouterContext.class);
            Map<String, String> templateVars = routerContext.getUriTemplateVariables();

            if (templateVars != null && !templateVars.isEmpty()) {
                return templateVars.get("id");
            }
        }

        return null;
    }

    /**
     * Returns the RouterContext's matchedUri, and appends its id, if there is one.
     * Id is retrieved via {@link ServerContextUtils#getId(org.forgerock.json.resource.ServerContext)}.
     *
     * @param context from which to gather the matched Uri and id information
     * @return a String in the form <code>matchedUri | id</code>, omitting either if they are null.
     */
    public static String getMatchedUri(ServerContext context) {
        String resource = "";
        if (context.containsContext(RouterContext.class)) {
            RouterContext routerContext = context.asContext(RouterContext.class);
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
     * Id is retrieved via {@link ServerContextUtils#getId(org.forgerock.json.resource.ServerContext)}.
     *
     * @param request the request for a resource
     * @param context the context of the request, including its RouterContext
     * @return a String in the form <code>resourceName | id</code>, omitting either if they are null.
     */
    public static String getResourceId(Request request, ServerContext context) {
        String resource = request.getResourceName();
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
}
