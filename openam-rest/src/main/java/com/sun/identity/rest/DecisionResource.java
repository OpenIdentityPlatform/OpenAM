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
 * $Id: DecisionResource.java,v 1.3 2009/12/15 00:44:19 veiming Exp $
 */

package com.sun.identity.rest;

import com.sun.identity.entitlement.ApplicationTypeManager;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Evaluator;
import com.sun.identity.entitlement.JSONEntitlement;
import com.sun.identity.entitlement.PrivilegeManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Exposes the entitlement decision REST resource.
 * 
 * @author Paul C. Bryan <pbryan@sun.com>
 * @author Ravi Hingarajiya <ravi.hingarajiya@sun.com>
 */
@Path("/1/entitlement")
public class DecisionResource extends ResourceBase {

    public static final String JSON_DECISION_ARRAY_KEY = "results";

    private enum Permission {
        deny, allow
    }

    private Evaluator getEvaluator(Subject caller, String application)
        throws EntitlementException {
        return ((application == null) || (application.length() == 0))
            ? new Evaluator(caller) : new Evaluator(caller, application);
    }

    /**
     * Returns entitlement decision of a given user.
     *
     * @param realm Realm name.
     * @param action Action to be evaluated.
     * @param resource Resource to be evaluated.
     * @param application Application name.
     * @param environment environment parameters.
     * @return entitlement decision of a given user. Either "deny" or "allow".
     */
    @GET
    @Produces("text/plain")
    @Path("/decision")
    public String getDecision(
        @Context HttpHeaders headers,
        @Context HttpServletRequest request,
        @QueryParam("realm") @DefaultValue("/") String realm,
        @QueryParam("action") String action,
        @QueryParam("resource") String resource,
        @QueryParam("application") @DefaultValue(
            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME) String application ,
        @QueryParam("env") List<String> environment
    ) {
        if (!realm.startsWith("/")) {
            realm = "/" + realm;
        }
        
        try {
            Subject caller = getCaller(request);
            Map env = getMap(environment);
            Subject subject = getSubject(request);

            validateSubjectAndResource(subject, resource);

            if ((action == null) || (action.trim().length() == 0)) {
                throw new EntitlementException(422);
            }
            Evaluator evaluator = getEvaluator(caller, application);
            return permission(evaluator.hasEntitlement(realm,
                subject, toEntitlement(resource, action),
                env));
        } catch (EntitlementException e) {
            PrivilegeManager.debug.warning("DecisionResource.decision", e);
            throw getWebApplicationException(headers, e, MimeType.PLAIN);
        } catch (RestException e) {
            PrivilegeManager.debug.warning("DecisionResource.decision", e);
            throw getWebApplicationException(headers, e, MimeType.PLAIN);
        }
    }

    /**
     * Returns the entitlements of a given subject.
     *
     * @param realm Realm Name.
     * @param resources resources to be evaluated
     * @param application application name.
     * @param environment environment parameters.
     * @return entitlement of a given subject (in JSON string).
     */
    @GET
    @Produces("application/json")
    @Path("/decisions")
    public String getDecisions(
        @Context HttpHeaders headers,
        @Context HttpServletRequest request,
        @QueryParam("realm") @DefaultValue("/") String realm,
        @QueryParam("resources") List<String> resources,
        @QueryParam("application") @DefaultValue(
            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME) String application,
        @QueryParam("env") List<String> environment
    ) {
        try {
            if (!realm.startsWith("/")) {
                realm = "/" + realm;
            }
            if ((resources == null) || resources.isEmpty()) {
                throw new EntitlementException(424);
            }

            Subject caller = getCaller(request);
            Subject subject = getSubject(request);

            Map env = getMap(environment);
            Set<String> setResources = new HashSet<String>();
            setResources.addAll(resources);
            validateSubject(subject);
            Evaluator evaluator = getEvaluator(caller, application);
            List<Entitlement> entitlements = evaluator.evaluate(
                realm, subject, setResources, env);

            List<JSONObject> results = new ArrayList<JSONObject>();
            if (entitlements != null) {
                for (Entitlement e : entitlements) {
                    Map<String, Boolean> actionValues = e.getActionValues();
                    if ((actionValues != null) && !actionValues.isEmpty()) {
                        JSONEntitlement je = new JSONEntitlement(
                            e.getResourceName(), actionValues, e.getAdvices(),
                            e.getAttributes());
                        results.add(je.toJSONObject());
                    }
                }
            }

            JSONObject jo = new JSONObject();
            jo.put(JSON_DECISION_ARRAY_KEY, results);
            return createResponseJSONString(200, headers, jo);
        } catch (JSONException e) {
            PrivilegeManager.debug.warning("DecisionResource.decisions", e);
            throw getWebApplicationException(e, MimeType.JSON);
        } catch (RestException e) {
            PrivilegeManager.debug.warning("DecisionResource.decisions", e);
            throw getWebApplicationException(headers, e, MimeType.JSON);
        } catch (EntitlementException e) {
            PrivilegeManager.debug.warning("DecisionResource.decisions", e);
            throw getWebApplicationException(headers, e, MimeType.JSON);
        }
    }

    /**
     * Returns the entitlement of a given subject.
     *
     * @param realm Realm Name.
     * @param resource resource to be evaluated
     * @param application application name.
     * @param environment environment parameters.
     * @return entitlement of a given subject (in JSON string).
     */
    @GET
    @Produces("application/json")
    @Path("/entitlement")
    public String getEntitlement(
        @Context HttpHeaders headers,
        @Context HttpServletRequest request,
        @QueryParam("realm") @DefaultValue("/") String realm,
        @QueryParam("resource") String resource,
        @QueryParam("application") @DefaultValue(
            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME) String application,
        @QueryParam("env") List<String> environment
    ) {
        if (!realm.startsWith("/")) {
            realm = "/" + realm;
        }

        Map env = getMap(environment);

        try {
            Subject caller = getCaller(request);
            Subject subject = getSubject(request);

            validateSubjectAndResource(subject, resource);
            Evaluator evaluator = getEvaluator(caller, application);
            List<Entitlement> entitlements = evaluator.evaluate(
                realm, subject, resource, env, false);

            Entitlement e = entitlements.get(0);
            JSONEntitlement jsonE = new JSONEntitlement(e.getResourceName(),
                e.getActionValues(), e.getAdvices(), e.getAttributes());
            return createResponseJSONString(200, headers, jsonE.toJSONObject());
        } catch (JSONException e) {
             PrivilegeManager.debug.warning("DecisionResource.evaluate", e);
             throw getWebApplicationException(e, MimeType.JSON);
        } catch (RestException e) {
            PrivilegeManager.debug.warning("DecisionResource.evaluate", e);
            throw getWebApplicationException(headers, e, MimeType.JSON);
        } catch (EntitlementException e) {
            PrivilegeManager.debug.warning("DecisionResource.evaluate", e);
            throw getWebApplicationException(headers, e, MimeType.JSON);
        }
    }

    /**
     * Returns the entitlements of a given subject.
     *
     * @param realm Realm Name.
     * @param resource resource to be evaluated
     * @param application application name.
     * @param environment environment parameters.
     * @return entitlements of a given subject (in JSON string).
     */
    @GET
    @Produces("application/json")
    @Path("/entitlements")
    public String getEntitlements(
        @Context HttpHeaders headers,
        @Context HttpServletRequest request,
        @QueryParam("realm") @DefaultValue("/") String realm,
        @QueryParam("resource") String resource,
        @QueryParam("application") @DefaultValue(
            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME) String application,
        @QueryParam("env") List<String> environment
    ) {
        if (!realm.startsWith("/")) {
            realm = "/" + realm;
        }

        Map env = getMap(environment);

        try {
            Subject caller = getCaller(request);
            Subject subject = getSubject(request);
            validateSubjectAndResource(subject, resource);
            Evaluator evaluator = getEvaluator(caller, application);
            List<Entitlement> entitlements = evaluator.evaluate(
                realm, subject, resource, env, true);
            List<JSONObject> result = new ArrayList<JSONObject>();

            for (Entitlement e : entitlements) {
                JSONEntitlement json = new JSONEntitlement(e.getResourceName(),
                    e.getActionValues(), e.getAdvices(), e.getAttributes());
                result.add(json.toJSONObject());
            }

            JSONObject jo = new JSONObject();
            jo.put(JSON_DECISION_ARRAY_KEY, result);
            return createResponseJSONString(200, headers, jo);
        } catch (JSONException e) {
            PrivilegeManager.debug.warning("DecisionResource.evaluate", e);
            throw getWebApplicationException(e, MimeType.JSON);
        } catch (RestException e) {
            PrivilegeManager.debug.warning("DecisionResource.evaluate", e);
            throw getWebApplicationException(headers, e, MimeType.JSON);
        } catch (EntitlementException e) {
            PrivilegeManager.debug.warning("DecisionResource.evaluate", e);
            throw getWebApplicationException(headers, e, MimeType.JSON);
        }
    }

    private String permission(boolean b) {
        return (b ? Permission.allow.toString() : Permission.deny.toString());
    }

    private void validateSubjectAndResource(Subject subject, String resource)
        throws EntitlementException {
        validateSubject(subject);
        validateResource(resource);
    }

    private void validateSubject(Subject subject)
        throws EntitlementException {
        if (subject == null) {
            throw new EntitlementException(421);
        }
    }

    private void validateResource(String resource)
        throws EntitlementException {
        if ((resource == null) || (resource.trim().length() == 0)) {
            throw new EntitlementException(420);
        }
    }
}

