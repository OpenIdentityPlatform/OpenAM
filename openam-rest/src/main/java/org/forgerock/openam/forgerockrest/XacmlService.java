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

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.entitlement.xacml3.SearchFilterFactory;
import com.sun.identity.entitlement.xacml3.XACMLImportExport;
import com.sun.identity.entitlement.xacml3.XACMLPrivilegeUtils;
import com.sun.identity.entitlement.xacml3.core.PolicySet;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import org.apache.xerces.dom.DocumentImpl;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.rest.service.RestletRealmRouter;
import org.forgerock.openam.rest.service.XACMLServiceEndpointApplication;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.OutputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.Subject;
import java.io.IOException;
import java.io.OutputStream;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provides XACML based services
 */
public class XacmlService extends ServerResource {
    public static final String QUERY_PARAM_STRING = "filter";
    private final XACMLImportExport importExport;
    private final AdminTokenAction admin;
    private final Debug debug;

    /**
     * Constructor with dependencies exposed for unit testing.
     *
     * @param importExport Non null utility functions.
     * @param adminTokenAction Non null admin action function.
     * @param debug The debug instance for logging.
     */
    @Inject
    public XacmlService(XACMLImportExport importExport, AdminTokenAction adminTokenAction,
            @Named("frRest") Debug debug) {
        this.importExport = importExport;
        this.admin = adminTokenAction;
        this.debug = debug;
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
     * @param entity
     * @throws org.forgerock.json.resource.ResourceException
     */
    @Post
    public Representation importXACML(Representation entity) throws ResourceException {
        String realm = RestletRealmRouter.getRealmFromRequest(getRequest());
        try {
            if (importExport.importXacml(realm, entity.getStream(), getAdminToken())) {
                getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
                return new EmptyRepresentation();
            }
            throw ResourceException.getException(ResourceException.BAD_REQUEST, "No policies found in XACML document");
        } catch (EntitlementException e) {
            debug.warning("Importing XACML to policies failed", e);
            throw ResourceException.getException(ResourceException.BAD_REQUEST, e.getMessage());
        } catch (IOException e) {
            debug.warning("Reading XACML import failed", e);
            throw ResourceException.getException(ResourceException.INTERNAL_ERROR, e.getMessage());
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
     * @return XACML of the matching Privileges.
     */
    @Get
    public Representation exportXACML() throws ResourceException {
        String realm = RestletRealmRouter.getRealmFromRequest(getRequest());
        List<String> filters = new ArrayList<String>(
                Arrays.asList(getQuery().getValuesArray(QUERY_PARAM_STRING)));

        final PolicySet policySet;
        try {
            policySet = importExport.exportXACML(realm, getAdminToken(), filters);
        } catch (EntitlementException e) {
            debug.warning("Reading Policies failed", e);
            throw ResourceException.getException(ResourceException.INTERNAL_ERROR, e.getMessage());
        }

        getResponse().setStatus(Status.SUCCESS_OK);
        return new OutputRepresentation(XACMLServiceEndpointApplication.APPLICATION_XML_XACML3) {
            @Override
            public void write(OutputStream outputStream) throws IOException {
                try {
                    XACMLPrivilegeUtils.writeXMLToStream(policySet, outputStream);
                } catch (EntitlementException e) {
                    throw new IOException(e);
                }
            }
        };
    }

}
