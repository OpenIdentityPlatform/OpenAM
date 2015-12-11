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

package org.forgerock.openam.rest.service;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ConcurrentMap;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdRepoException;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.RealmInfo;
import org.forgerock.openam.rest.router.RestRealmValidator;
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

/**
 * A Restlet router which will route to service endpoints, dynamically handling realm URI parameters.
 *
 * @since 12.0.0
 */
public class RestletRealmRouter extends Router {

    public static final String REALM = "realm";
    public static final String REALM_INFO = "realmInfo";
    public static final String REALM_URL = "realmUrl";

    private final RestRealmValidator realmValidator;
    private final CoreWrapper coreWrapper;
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

        Delegate delegate = new Delegate(this);
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
        RealmInfo realmInfo = getRealmFromURI(request);

        if (realmInfo == null) {
            realmInfo = getRealmFromServerName(request);
        }
        if (next != delegateRoute) {
            String overrideRealm = getRealmFromQueryString(request);
            if (overrideRealm != null) {
                realmInfo = realmInfo.withOverrideRealm(overrideRealm);
            }
            request.getAttributes().put(REALM_URL, request.getResourceRef().getBaseRef().toString());
        }

        // Check that the path references an existing realm
        if (!realmValidator.isRealm(realmInfo.getAbsoluteRealm())) {
            String realm = realmInfo.getAbsoluteRealm();
            try {
                SSOToken adminToken = coreWrapper.getAdminToken();
                //Need to strip off leading '/' from realm otherwise just generates a DN based of the realm value, which is wrong
                if (realmInfo.getAbsoluteRealm().startsWith("/")) {
                    realm = realm.substring(1);
                }
                String orgDN = coreWrapper.getOrganization(adminToken, realm);
                realmInfo = realmInfo.withAbsoluteRealm(coreWrapper.convertOrgNameToRealmName(orgDN));
            } catch (IdRepoException | SSOException e) {
                throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid realm, " + realm);
            }
        }

        request.getAttributes().put(REALM, realmInfo.getAbsoluteRealm());
        request.getAttributes().put(REALM_INFO, realmInfo);
        HttpServletRequest httpRequest = ServletUtils.getRequest(request);
        httpRequest.setAttribute(REALM, realmInfo.getAbsoluteRealm());
        httpRequest.setAttribute(REALM_INFO, realmInfo);
        request.getAttributes().remove("subrealm");

        super.doHandle(next, request, response);
    }

    private RealmInfo getRealmFromURI(Request request) {
        RealmInfo realmInfo = (RealmInfo) request.getAttributes().get(REALM_INFO);
        String subrealm = (String) request.getAttributes().get("subrealm");
        if (subrealm != null && !subrealm.isEmpty()) {
            if (realmInfo == null) {
                throw new IllegalStateException("RealmInfo is null! Has not been set from server name");
            } else {
                return realmInfo.appendUriRealm(subrealm);
            }
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

    private RealmInfo getRealmFromServerName(Request request) {
        String serverName = request.getHostRef().getHostDomain();
        try {
            SSOToken adminToken = coreWrapper.getAdminToken();
            String orgDN = coreWrapper.getOrganization(adminToken, serverName);
            return new RealmInfo(coreWrapper.convertOrgNameToRealmName(orgDN));
        } catch (IdRepoException | SSOException e) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
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
