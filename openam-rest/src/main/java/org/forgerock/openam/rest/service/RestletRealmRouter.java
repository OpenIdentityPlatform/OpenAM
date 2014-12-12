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

package org.forgerock.openam.rest.service;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.client.AuthClientUtils;
import com.sun.identity.idm.IdRepoException;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.rest.router.RestRealmValidator;
import org.forgerock.openam.utils.StringUtils;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Status;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.resource.ResourceException;
import org.restlet.routing.Route;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.restlet.routing.TemplateRoute;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.concurrent.ConcurrentMap;

/**
 * A Restlet router which will route to service endpoints, dynamically handling realm URI parameters.
 *
 * @since 12.0.0
 */
public class RestletRealmRouter extends Router {

    // Keyword for Realm Attribute
    static final String REALM = "realm";

    private final RestRealmValidator realmValidator;
    private final CoreWrapper coreWrapper;
    private final Delegate delegate;
    private final TemplateRoute delegateRoute;

    /**
     * Constructs a new RealmRouter instance.
     *
     * @param realmValidator An instance of the RestRealmValidator.
     * @param coreWrapper An instance of the CoreWrapper.
     */
    public RestletRealmRouter(RestRealmValidator realmValidator, CoreWrapper coreWrapper) {
        this.realmValidator = realmValidator;
        this.coreWrapper = coreWrapper;

        delegate = new Delegate(this);
        delegateRoute = createRoute("/{subrealm}", delegate, Template.MODE_STARTS_WITH);
        super.setDefaultRoute(delegateRoute);
    }

    @Override
    public void setDefaultRoute(Route defaultRoute) {
        throw new UnsupportedOperationException("Default route is handled internally for realm routing");
    }

    /**
     * <p>Takes the last realm URI parameter from the request and appends to the growing full realm value.</p>
     *
     * <p>i.e. last realm URI parameter: realm2, current full realm value: /realm1, after appending: /realm1/realm2.</p>
     *
     * @param next {@inheritDoc}
     * @param request {@inheritDoc}
     * @param response {@inheritDoc}
     */
    @Override
    protected void doHandle(Restlet next, Request request, Response response) {
        String realm = getRealmFromURI(request);

        if (realm == null) {
            realm = getRealmFromServerName(request);
        }

        if (next != delegateRoute) {
            if (StringUtils.isEmpty((String) request.getAttributes().get("subrealm"))) {
                String subrealm = getRealmFromQueryString(request);
                if (realm == null) {
                    realm = subrealm;
                } else if (subrealm != null && !subrealm.isEmpty()) {
                    realm += realm.endsWith("/") ? subrealm.substring(1) : subrealm;
                }
            }
        }

        request.getAttributes().put(REALM, realm);
        HttpServletRequest httpRequest = ServletUtils.getRequest(request);
        httpRequest.setAttribute(REALM, realm);
        request.getAttributes().remove("subrealm");

        // Check that the path references an existing realm
        validateRealm(request, realm);

        super.doHandle(next, request, response);
    }

    private String getRealmFromURI(Request request) {
        String realm = (String) request.getAttributes().get(REALM);
        String subrealm = (String) request.getAttributes().get("subrealm");
        if (subrealm != null && !subrealm.isEmpty()) {
            return realm.equals("/") ? realm + subrealm : realm + "/" + subrealm;
        }
        return null;
    }

    private String getRealmFromQueryString(Request request) {
        String realm = request.getResourceRef().getQueryAsForm().getFirstValue(REALM);
        if (realm == null) {
            return null;
        }
        return realm;
    }

    private String getRealmFromServerName(Request request) {
        String serverName = request.getHostRef().getHostDomain();
        try {
            SSOToken adminToken = coreWrapper.getAdminToken();
            String orgDN = coreWrapper.getOrganization(adminToken, serverName);
            return coreWrapper.convertOrgNameToRealmName(orgDN);
        } catch (IdRepoException e) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
        } catch (SSOException e) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
        }
    }

    private void validateRealm(Request request, String realm) {
        if (!realmValidator.isRealm(realm)) {
            try {
                SSOToken adminToken = coreWrapper.getAdminToken();
                //Need to strip off leading '/' from realm otherwise just generates a DN based of the realm value, which is wrong
                if (realm.startsWith("/")) {
                    realm = realm.substring(1);
                }
                String orgDN = coreWrapper.getOrganization(adminToken, realm);
                realm = coreWrapper.convertOrgNameToRealmName(orgDN);
                request.getAttributes().put(REALM, realm);
                HttpServletRequest httpRequest = ServletUtils.getRequest(request);
                httpRequest.setAttribute(REALM, realm);
                return;
            } catch (IdRepoException e) {
                //Empty catch, fall through to throw exception
            } catch (SSOException e) {
                //Empty catch, fall through to throw exception
            }
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid realm, " + realm);
        }
    }

    /**
     * Returns the realm from the given request.
     *
     * @param request Non null request to examine.
     * @return Null if no realm was found, otherwise the given Realm as a String.
     */
    public static String getRealmFromRequest(Request request) {
        ConcurrentMap<String, Object> attributes = request.getAttributes();
        if (attributes == null || attributes.get(REALM) == null) {
            return null;
        }
        return attributes.get(REALM).toString();
    }

    /**
     * Restlet eagerly starts/loads its routes so cannot have a direct route back to itself as causes a stack overflow.
     * To get round this adding in a delegate to lazy start/load the dynamic realm route back to itself.
     *
     * @since 12.0.0
     */
    private static final class Delegate extends Restlet {

        private final Router router;

        private Delegate(Router router) {
            this.router = router;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handle(Request request, Response response) {
            router.handle(request, response);
        }
    }
}
