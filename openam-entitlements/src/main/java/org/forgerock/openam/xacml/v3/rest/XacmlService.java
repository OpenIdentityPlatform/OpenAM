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
 * Copyright 2015-2016 ForgeRock AS.
 * Portions copyright 2025-2026 3A Systems, LLC.
 */

package org.forgerock.openam.xacml.v3.rest;

import java.io.ByteArrayOutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.security.auth.Subject;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.forgerock.http.header.AcceptLanguageHeader;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.header.MalformedHeaderException;
import org.forgerock.http.protocol.Form;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.forgerockrest.utils.RestLog;
import org.forgerock.openam.forgerockrest.utils.XMLResourceExceptionHandler;
import org.forgerock.openam.http.annotations.Contextual;
import org.forgerock.openam.http.annotations.Get;
import org.forgerock.openam.http.annotations.Post;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.ServerContextHelper;
import org.forgerock.openam.xacml.v3.ImportStep;
import org.forgerock.services.context.Context;
import org.forgerock.util.annotations.VisibleForTesting;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationPermission;
import com.sun.identity.delegation.DelegationPermissionFactory;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.entitlement.xacml3.SearchFilterFactory;
import com.sun.identity.entitlement.xacml3.XACMLExportImport;
import com.sun.identity.entitlement.xacml3.XACMLPrivilegeUtils;
import com.sun.identity.entitlement.xacml3.core.PolicySet;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * Serves XACML 3.0 export and import of the policies of a realm, as a CHF annotated endpoint.
 */
public class XacmlService {

    private static final String REST = "rest";
    private static final String VERSION = "1.0";
    private static final String POLICIES = "policies";

    public static final String QUERY_PARAM_STRING = "filter";

    static final String XACML_CONTENT_TYPE = "application/xacml+xml; version=3.0";
    static final String XML_ERROR_CONTENT_TYPE = "application/xml;charset=UTF-8";
    static final String JSON_CONTENT_TYPE = "application/json";

    private static final String ROOT_REALM = "/";

    private final XACMLExportImport importExport;
    private final PrivilegedAction<SSOToken> admin;
    private final Debug debug;
    private final RestLog restLog;
    private final DelegationEvaluator evaluator;

    /**
     * Constructor with dependencies exposed for unit testing.
     *
     * @param importExport Non null utility functions.
     * @param adminTokenAction Non null admin action function.
     * @param debug The debug instance for logging.
     * @param restLog The REST audit logger.
     * @param evaluator The delegation evaluator.
     */
    @Inject
    public XacmlService(XACMLExportImport importExport, PrivilegedAction<SSOToken> adminTokenAction,
            @Named("frRest") Debug debug, RestLog restLog, DelegationEvaluator evaluator) {
        this.importExport = importExport;
        this.admin = adminTokenAction;
        this.debug = debug;
        this.restLog = restLog;
        this.evaluator = evaluator;
    }

    /**
     * Returns a Subject for the valid admin SSOToken.
     *
     * @return A subject for the valid admin SSOToken.
     */
    private final Subject getAdminToken() {
        return SubjectUtils.createSubject(AccessController.doPrivileged(admin));
    }

    /**
     * Expects to receive XACML formatted XML which will be read and imported.
     *
     * @param context The request context.
     * @param request The request, whose entity is the XACML document.
     * @return A JSON array of the import steps, or an XML error.
     */
    @Post
    public Response importXACML(@Contextual Context context, @Contextual Request request) {
        try {
            if (!checkPermission(context, "MODIFY")) {
                return xmlError(new ForbiddenException());
            }
            // Only the query is read: Request.getForm() would also consume a form-encoded body, and the console
            // posts the XML document with that content type.
            boolean dryRun = "true".equalsIgnoreCase(new Form().fromRequestQuery(request).getFirst("dryrun"));
            List<ImportStep> steps;
            try {
                steps = importExport.importXacml(realm(context), request.getEntity().getRawContentInputStream(),
                        getAdminToken(), dryRun);
            } catch (EntitlementException e) {
                debug.warning("Importing XACML to policies failed", e);
                // getLocalizedMessage may throw for a code without a bundle key; the outer catch turns that into 500.
                return xmlError(new BadRequestException(e.getLocalizedMessage(locale(request)), e));
            }
            if (steps.isEmpty()) {
                return xmlError(new BadRequestException("No policies found in XACML document"));
            }

            List<Map<String, String>> result = new ArrayList<>();
            for (ImportStep step : steps) {
                Map<String, String> stepResult = new HashMap<>();
                stepResult.put("status", String.valueOf(step.getDiffStatus().getCode()));
                stepResult.put("name", step.getName());
                stepResult.put("type", step.getType());
                result.add(stepResult);
            }
            Response response = new Response(Status.OK);
            response.getEntity().setJson(result);
            // After setJson, which forces "application/json; charset=UTF-8"; the contract has no charset.
            response.getHeaders().put(ContentTypeHeader.NAME, JSON_CONTENT_TYPE);
            return response;
        } catch (ResourceException e) {
            return xmlError(e);
        } catch (RuntimeException e) {
            debug.error("XacmlService :: import failed", e);
            return xmlError(new InternalServerErrorException(e.getMessage(), e));
        }
    }

    /**
     * Export all Policies defined within the Realm used to access this end point.
     *
     * The end point supports the query parameter "filter" which can be used multiple
     * times to define the Search Filters which will restrict the output to only those
     * Privileges which match the Search Filters.
     *
     * See {@link SearchFilterFactory} for more details on the format of these
     * Search Filters.
     *
     * @param context The request context.
     * @param request The request.
     * @return XACML of the matching Privileges, or an XML error.
     */
    @Get
    public Response exportXACML(@Contextual Context context, @Contextual Request request) {
        try {
            String realm = realm(context);
            if (!checkPermission(context, "READ")) {
                return xmlError(new ForbiddenException());
            }
            List<String> queryFilters = new Form().fromRequestQuery(request).get(QUERY_PARAM_STRING);
            List<String> filters = queryFilters == null ? new ArrayList<String>() : new ArrayList<>(queryFilters);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try {
                PolicySet policySet = importExport.exportXACML(realm, getAdminToken(), filters);
                XACMLPrivilegeUtils.writeXMLToStream(policySet, buffer);
            } catch (EntitlementException e) {
                debug.warning("Reading Policies failed", e);
                return xmlError(new InternalServerErrorException(e.getLocalizedMessage(locale(request)), e));
            }

            Response response = new Response(Status.OK);
            response.getEntity().setBytes(buffer.toByteArray());
            response.getHeaders().put(ContentTypeHeader.NAME, XACML_CONTENT_TYPE);
            // OPENAM-4974
            response.getHeaders().put("Content-Disposition",
                    "attachment; filename=" + getPolicyAttachmentFileName(realm));
            return response;
        } catch (ResourceException e) {
            return xmlError(e);
        } catch (RuntimeException e) {
            debug.error("XacmlService :: export failed", e);
            return xmlError(new InternalServerErrorException(e.getMessage(), e));
        }
    }

    private static String realm(Context context) {
        return RealmContext.getRealm(context).asPath();
    }

    private static Locale locale(Request request) {
        try {
            AcceptLanguageHeader header = request.getHeaders().get(AcceptLanguageHeader.class);
            return header == null ? Locale.getDefault() : header.getLocales().getPreferredLocale();
        } catch (MalformedHeaderException e) {
            return Locale.getDefault();
        }
    }

    private Response xmlError(ResourceException exception) {
        Response response = new Response(Status.valueOf(exception.getCode()));
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Transformer transformer = XMLUtils.getTransformerFactory().newTransformer();
            transformer.transform(new DOMSource(XMLResourceExceptionHandler.asXMLDOM(
                    exception.toJsonValue().asMap())), new StreamResult(out));
            response.getEntity().setBytes(out.toByteArray());
        } catch (TransformerException e) {
            throw new IllegalStateException("Could not write XML to response", e);
        }
        response.getHeaders().put(ContentTypeHeader.NAME, XML_ERROR_CONTENT_TYPE);
        return response;
    }

    /**
     * Figure the name of the attachment file that will be created to contain the policies.  See OPENAM-4974.
     * File naming agreed with Andy H.
     *
     * @param realm The realm
     * @return A suitable file name, involving the realm in a meaningful way.
     */
    private String getPolicyAttachmentFileName(String realm) {
        String result;
        if (ROOT_REALM.equals(realm)) {
            result = "realm-policies";
        } else {
            result = realm.substring(1).replace('/', '-') + "-realm-policies";
        }
        return result + ".xml";
    }

    /**
     * Check if this user has permission to perform the given action (which will be "READ" in the case of export
     * and "MODIFY" in the case of import).
     *
     * @return true if the user has permission, false otherwise.
     */
    @VisibleForTesting
    boolean checkPermission(Context context, String action) throws ResourceException {
        try {
            // Null without a session (OptionalSSOTokenSessionModule admits the request); createSSOToken then fails,
            // which keeps master's 500 for an unauthenticated request (user decision: no 401 in this PR).
            String tokenId = ServerContextHelper.getCookieFromServerContext(context);
            SSOToken token = SSOTokenManager.getInstance().createSSOToken(tokenId);
            DelegationPermission permission = new DelegationPermissionFactory().newInstance(realm(context), REST,
                    VERSION, POLICIES, action, Collections.singleton(action), Collections.<String, String>emptyMap());
            return checkPermission(permission, token, POLICIES);
        } catch (SSOException | DelegationException e) {
            debug.warning("XacmlService permission evaluation failed", e);
            throw new InternalServerErrorException("XacmlService permission evaluation failed", e);
        }
    }

    /**
     * This is the most atomic of the checkPermissions and finally gives us something we can test without having to
     * mock out most of the universe.
     *
     * @param permissionRequest the permission request, forged from the realm, URL segment, action and other things
     * @param token the user's SSO token
     * @param urlLastSegment the last segment of the URL
     * @return true if the user has the "action" permission (action being "READ" or "MODIFY"), false otherwise.
     */
    @VisibleForTesting
    boolean checkPermission(DelegationPermission permissionRequest, SSOToken token, String urlLastSegment)
                                                throws DelegationException, SSOException {

        boolean result = evaluator.isAllowed(token, permissionRequest, Collections.EMPTY_MAP);
        String className = this.getClass().getName();
        if (result) {
            restLog.auditAccessGranted(className, urlLastSegment, className, token);
        } else {
            restLog.auditAccessDenied(className, urlLastSegment, className, token);
        }
        return result;
    }
}
