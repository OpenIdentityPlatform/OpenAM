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

package org.forgerock.openam.forgerockrest;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationEvaluatorImpl;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationPermission;
import com.sun.identity.delegation.DelegationPermissionFactory;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.entitlement.xacml3.SearchFilterFactory;
import com.sun.identity.entitlement.xacml3.XACMLExportImport;
import com.sun.identity.entitlement.xacml3.XACMLExportImport.ImportStep;
import com.sun.identity.entitlement.xacml3.XACMLPrivilegeUtils;
import com.sun.identity.entitlement.xacml3.core.PolicySet;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.forgerockrest.utils.RestLog;
import org.forgerock.openam.rest.service.RestletRealmRouter;
import org.forgerock.openam.rest.service.XACMLServiceEndpointApplication;
import org.forgerock.util.annotations.VisibleForTesting;
import org.restlet.Request;
import org.restlet.data.Disposition;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.representation.OutputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.OutputStream;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.forgerock.json.resource.ResourceException.BAD_REQUEST;
import static org.forgerock.json.resource.ResourceException.INTERNAL_ERROR;
import static org.forgerock.json.resource.ResourceException.FORBIDDEN;

/**
 * Provides XACML based services
 */
public class XacmlService extends ServerResource {

    private static final String REST = "rest";
    private static final String VERSION = "1.0";

    public static final String QUERY_PARAM_STRING = "filter";

    @VisibleForTesting
    static final String FORGEROCK_AUTH_CONTEXT = "org.forgerock.authentication.context";

    private static final String ROOT_REALM = "/";

    private final XACMLExportImport importExport;
    private final AdminTokenAction admin;
    private final Debug debug;
    private final RestLog restLog;
    private final DelegationEvaluator evaluator;

    /**
     * Constructor with dependencies exposed for unit testing.
     *
     * @param importExport Non null utility functions.
     * @param adminTokenAction Non null admin action function.
     * @param debug The debug instance for logging.
     */
    @Inject
    public XacmlService(XACMLExportImport importExport,
                        AdminTokenAction adminTokenAction,
                        @Named("frRest") Debug debug,
                        RestLog restLog,
                        DelegationEvaluator evaluator) {
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
     */
    @Post
    public Representation importXACML(Representation entity) {
        boolean dryRun = "true".equalsIgnoreCase(getQuery().getFirstValue("dryrun"));
        List<ImportStep> steps;

        try {
            if (!checkPermission("MODIFY")) {
                // not allowed
                throw new ResourceException(new Status(FORBIDDEN));
            }

            String realm = RestletRealmRouter.getRealmFromRequest(getRequest());
            steps = importExport.importXacml(realm, entity.getStream(), getAdminToken(), dryRun);

            if (steps.isEmpty()) {
                throw new ResourceException(new Status(BAD_REQUEST,
                        "No policies found in XACML document", null, null));
            }

            List<Map<String, String>> result = new ArrayList<Map<String, String>>();
            for (XACMLExportImport.ImportStep step : steps) {
                Map<String, String> stepResult = new HashMap<String, String>();
                stepResult.put("status", String.valueOf(step.getDiffStatus().getCode()));
                stepResult.put("name", step.getPrivilege().getName());
                result.add(stepResult);
            }
            getResponse().setStatus(Status.SUCCESS_OK);

            return new JacksonRepresentation<List<Map<String, String>>>(result);

        } catch (EntitlementException e) {
            debug.warning("Importing XACML to policies failed", e);
            throw new ResourceException(new Status(BAD_REQUEST, e, e
                    .getLocalizedMessage(getRequestLocale()), null, null));
        } catch (IOException e) {
            debug.warning("Reading XACML import failed", e);
            throw new ResourceException(new Status(BAD_REQUEST, e, e
                    .getLocalizedMessage(), null, null));
        }
    }

    /**
     * Get the client's preferred locale, or the server default if not specified.
     */
    private Locale getRequestLocale() {
        final HttpServletRequest httpRequest = ServletUtils.getRequest(getRequest());
        return httpRequest == null ? Locale.getDefault() : httpRequest.getLocale();
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
     * @return XACML of the matching Privileges.
     */
    @Get
    public Representation exportXACML() {
        String realm = RestletRealmRouter.getRealmFromRequest(getRequest());
        return exportXACML(realm);
    }

    /**
     * This version of exportXACML here for testing - it saves trying to mock the static getRealmFromRequest
     * @param realm The realm
     * @return Representation object wrapping the converted XACML
     */
    @VisibleForTesting
    Representation exportXACML(String realm) {

        List<String> filters = new ArrayList<String>(Arrays.asList(getQuery().getValuesArray(QUERY_PARAM_STRING)));
        PolicySet policySet;

        try {
            if (!checkPermission("READ")) {
                throw new ResourceException(new Status(FORBIDDEN));
            }

            policySet = importExport.exportXACML(realm, getAdminToken(), filters);
            getResponse().setStatus(Status.SUCCESS_OK);

        } catch (EntitlementException e) {
            debug.warning("Reading Policies failed", e);
            throw new ResourceException(new Status(INTERNAL_ERROR, e.getLocalizedMessage(getRequestLocale()), null,
                    null));
        }
        final PolicySet finalPolicySet = policySet;

        Representation result = new OutputRepresentation(XACMLServiceEndpointApplication.APPLICATION_XML_XACML3) {
            @Override
            public void write(OutputStream outputStream) throws IOException {
                try {
                    XACMLPrivilegeUtils.writeXMLToStream(finalPolicySet, outputStream);
                } catch (EntitlementException e) {
                    throw new IOException(e);
                }
            }
        };
        // OPENAM-4974
        Disposition disposition = new Disposition();
        disposition.setType(Disposition.TYPE_ATTACHMENT);
        disposition.setFilename(getPolicyAttachmentFileName(realm));
        result.setDisposition(disposition);

        return result;
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
     * Check if this user has permission to perform the given action (which will be "read" in the case of export
     * and "modify" in the case of import).
     *
     * @return true if the user has permission, false otherwise.
     */
    protected boolean checkPermission(String action) throws EntitlementException {

        try {
            Request restletRequest = getRequest();
            String urlLastSegment = restletRequest.getResourceRef().getLastSegment();
            String realm = RestletRealmRouter.getRealmFromRequest(restletRequest);

            final Map<String, String> context = (Map<String, String>)
                                        ServletUtils.getRequest(getRequest()).getAttribute(FORGEROCK_AUTH_CONTEXT);
            final String tokenId = context.get("tokenId");
            final SSOToken token = SSOTokenManager.getInstance().createSSOToken(tokenId);

            final Set<String> actions = new HashSet<String>(Arrays.asList(action));
            final DelegationPermissionFactory permissionFactory = new DelegationPermissionFactory();
            final DelegationPermission permissionRequest = permissionFactory.newInstance(realm, REST, VERSION,
                    urlLastSegment, action, actions, Collections.<String, String>emptyMap());

            boolean result = evaluator.isAllowed(token, permissionRequest, Collections.EMPTY_MAP);

            String className = this.getClass().getName();
            if (result) {
                restLog.auditAccessGranted(className, urlLastSegment, className, token);
            } else {
                restLog.auditAccessDenied(className, urlLastSegment, className, token);
            }
            return result;

        } catch (SSOException e) {
            debug.warning("XacmlService permission evaluation failed", e);
            throw new EntitlementException(INTERNAL_ERROR, e);
        } catch (DelegationException e) {
            debug.warning("XacmlService permission evaluation failed", e);
            throw new EntitlementException(INTERNAL_ERROR, e);
        }
    }
}
