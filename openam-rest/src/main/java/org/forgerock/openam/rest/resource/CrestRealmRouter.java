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

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.client.AuthClientUtils;
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

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.List;

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
        String realm;

        realm = getRealmFromPolicyAdvice(context);

        if (realm == null) {
            realm = getRealmFromURI(context);
        }

        if (realm == null) {
            realm = getRealmFromQueryString(context);
        }

        if (realm == null) {
            realm = getRealmFromServerName(context);
        }

        RealmContext realmContext;
        if (context.containsContext(RealmContext.class)) {
            realmContext = context.asContext(RealmContext.class);
            realmContext.addSubRealm(realm);
        } else {
            realmContext = new RealmContext(context, realm);
        }

        // Check that the path references an existing realm
        return validateRealm(realmContext);
    }

    private String getRealmFromPolicyAdvice(ServerContext context) {
        List<String> advice = context.asContext(HttpContext.class).getParameter(AuthClientUtils.COMPOSITE_ADVICE);
        if (advice == null || advice.size() != 1) {
            return null;
        }

        try {
            String decodedXml = URLDecoder.decode(advice.get(0), "UTF-8");
            return coreWrapper.getRealmFromPolicyAdvice(decodedXml);
        } catch (UnsupportedEncodingException uee) {
            //Empty catch
        }
        return null;
    }

    private String getRealmFromURI(ServerContext context) {
        if (context.containsContext(RouterContext.class)) {
            return context.asContext(RouterContext.class).getUriTemplateVariables().get("realm");
        }
        return null;
    }

    private String getRealmFromQueryString(ServerContext context) {
        List<String> realm = context.asContext(HttpContext.class).getParameter("realm");
        if (realm == null || realm.size() != 1) {
            return null;
        }
        return realm.get(0);
    }

    private String getRealmFromServerName(ServerContext context) throws InternalServerErrorException {
        String serverName = URI.create(context.asContext(HttpContext.class).getPath()).getHost();
        try {
            SSOToken adminToken = coreWrapper.getAdminToken();
            String orgDN = coreWrapper.getOrganization(adminToken, serverName);
            return coreWrapper.convertOrgNameToRealmName(orgDN);
        } catch (IdRepoException e) {
            throw new InternalServerErrorException(e);
        } catch (SSOException e) {
            throw new InternalServerErrorException(e);
        }
    }

    private ServerContext validateRealm(RealmContext realmContext) throws BadRequestException {
        if (!realmValidator.isRealm(realmContext.getRealm())) {
            try {
                SSOToken adminToken = coreWrapper.getAdminToken();
                //Need to strip off leading '/' from realm otherwise just generates a DN based of the realm value, which is wrong
                String realm = realmContext.getRealm();
                if (realm.startsWith("/")) {
                    realm = realm.substring(1);
                }
                String orgDN = coreWrapper.getOrganization(adminToken, realm);
                return new RealmContext(realmContext.getParent(), coreWrapper.convertOrgNameToRealmName(orgDN));
            } catch (IdRepoException e) {
                //Empty catch, fall through to throw exception
            } catch (SSOException e) {
                //Empty catch, fall through to throw exception
            }
            throw new BadRequestException("Invalid realm, " + realmContext.getRealm());
        }
        return realmContext;
    }
}
