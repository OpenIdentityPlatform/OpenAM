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

package org.forgerock.openam.rest.resource;

import static org.forgerock.json.resource.RoutingMode.STARTS_WITH;

import javax.inject.Inject;
import java.net.URI;
import java.util.List;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdRepoException;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.RouterContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.servlet.HttpContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.rest.router.RestRealmValidator;
import org.forgerock.openam.rest.router.VersionedRouter;

/**
 * A CREST request handler which will route to resource endpoints, dynamically handling realm URI parameters.
 *
 * This implementation ensures that the context passed along with the request to the endpoint contains
 * a {@link org.forgerock.openam.rest.resource.RealmContext}.
 *
 * @since 12.0.0
 */
public class CrestRealmRouter extends CrestRouter<CrestRealmRouter> implements VersionedRouter<CrestRealmRouter> {

    private final RestRealmValidator realmValidator;
    private final CoreWrapper coreWrapper;

    /**
     * Constructs a new RealmRouter instance.
     *
     * @param realmValidator An instance of the RestRealmValidator.
     * @param coreWrapper An instance of the CoreWrapper.
     */
    @Inject
    public CrestRealmRouter(RestRealmValidator realmValidator, CoreWrapper coreWrapper) {
        this.realmValidator = realmValidator;
        this.coreWrapper = coreWrapper;
        getRouter().addRoute(STARTS_WITH, "/{realm}", this);
    }

    /**
     * <p>Creates a or adds to the {@link org.forgerock.openam.rest.resource.RealmContext}.</p>
     *
     * <p>If multiple realm URI parameters are present then this method will be called repeatedly and for each call
     * the last realm URI parameter will be appended to the realm on the {@code RealmContext}.</p>
     *
     * <p>The realm is attempted to be retrieved from the composite advice query parameter, then from the URI, then
     * from the realm query parameter and finally by resolving the hostname in the request. Each of these locations
     * should be used independently, if they are combined the result may or may not result in a valid realm.</p>
     *
     * @param context The context.
     * @return The augmented context.
     * @throws BadRequestException If the current full realm is not a valid realm.
     */
    @Override
    protected ServerContext transformContext(ServerContext context) throws ResourceException {

        RealmContext realmContext;
        if (context.containsContext(RealmContext.class)) {
            realmContext = context.asContext(RealmContext.class);
        } else {
            realmContext = new RealmContext(context);
        }

        boolean handled = getRealmFromURI(context, realmContext);
        if (!handled) {
            handled = getRealmFromQueryString(context, realmContext);
        }
        if (!handled) {
            getRealmFromServerName(context, realmContext);
        }
        return realmContext;
    }

    private boolean getRealmFromURI(ServerContext context, RealmContext realmContext) throws BadRequestException {
        if (context.containsContext(RouterContext.class)) {
            String subRealm = context.asContext(RouterContext.class).getUriTemplateVariables().get("realm");
            subRealm = validateRealm(realmContext.getResolvedRealm(), subRealm);
            if (subRealm != null) {
                realmContext.addSubRealm(subRealm, subRealm);
                return true;
            }
        }
        return false;
    }

    private boolean getRealmFromQueryString(ServerContext context, RealmContext realmContext)
            throws BadRequestException {
        List<String> realm = context.asContext(HttpContext.class).getParameter("realm");
        if (realm == null || realm.size() != 1) {
            return false;
        }
        String subRealm = validateRealm(realmContext.getResolvedRealm(), realm.get(0));
        if (subRealm != null) {
            realmContext.addSubRealm(subRealm, subRealm);
            return true;
        } else {
            return false;
        }
    }

    private boolean getRealmFromServerName(ServerContext context, RealmContext realmContext)
            throws InternalServerErrorException, BadRequestException {
        String serverName = URI.create(context.asContext(HttpContext.class).getPath()).getHost();
        try {
            SSOToken adminToken = coreWrapper.getAdminToken();
            String orgDN = coreWrapper.getOrganization(adminToken, serverName);
            String realmPath = validateRealm(coreWrapper.convertOrgNameToRealmName(orgDN));
            realmContext.addDnsAlias(serverName, realmPath);
            return true;
        } catch (IdRepoException e) {
            throw new InternalServerErrorException(e);
        } catch (SSOException e) {
            throw new InternalServerErrorException(e);
        }
    }

    private String validateRealm(String realmPath) throws BadRequestException {
        String resolvedRealm = realmPath;
        if (!realmValidator.isRealm(resolvedRealm)) {
            try {
                SSOToken adminToken = coreWrapper.getAdminToken();
                //Need to strip off leading '/' from realm otherwise just generates a DN based of the realm value, which is wrong
                String realm = resolvedRealm;
                if (realm.startsWith("/")) {
                    realm = realm.substring(1);
                }
                String orgDN = coreWrapper.getOrganization(adminToken, realm);
                return coreWrapper.convertOrgNameToRealmName(orgDN);
            } catch (IdRepoException e) {
                //Empty catch, fall through to throw exception
            } catch (SSOException e) {
                //Empty catch, fall through to throw exception
            }
            throw new BadRequestException("Invalid realm, " + resolvedRealm);
        }
        return realmPath;
    }

    private String validateRealm(String realmPath, String subRealm) throws BadRequestException {
        if (subRealm == null || subRealm.isEmpty()) {
            return null;
        }
        if (realmPath.endsWith("/")) {
            realmPath = realmPath.substring(0, realmPath.length() - 1);
        }
        if (!subRealm.startsWith("/")) {
            subRealm = "/" + subRealm;
        }
        if (subRealm.endsWith("/")) {
            subRealm = subRealm.substring(0, subRealm.length() - 1);
        }
        String validatedRealm = validateRealm(realmPath + subRealm);
        if (!realmValidator.isRealm(realmPath + subRealm)) {
            return validatedRealm;
        } else {
            return subRealm;
        }
    }
}
