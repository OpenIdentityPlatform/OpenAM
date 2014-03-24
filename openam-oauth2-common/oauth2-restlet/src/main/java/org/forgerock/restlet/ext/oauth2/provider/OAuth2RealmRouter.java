/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [2012] [ForgeRock Inc]"
 */
package org.forgerock.restlet.ext.oauth2.provider;

import java.util.concurrent.ConcurrentHashMap;

import org.forgerock.openam.oauth2.provider.OAuth2Provider;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.restlet.ext.oauth2.flow.ErrorServerResource;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Status;
import org.restlet.resource.Finder;
import org.restlet.routing.Router;

/**
 * This Router sits on an endpoint /openam/oauth2/ and dispatches the request
 * based on the realm to the next Router This can sit on /openam/{realm}/oauth2/
 * too and do the dispatch
 */
public class OAuth2RealmRouter extends Router implements OAuth2Provider {

    /**
     * The default realm router if no realm property.
     */
    private volatile Restlet defaultRealm;

    /**
     * The default router tested if no other one was available.
     */
    private Finder errorHandler;

    /**
     * Map of available realms
     */
    private volatile ConcurrentHashMap<String, Restlet> realmRoutes =
            new ConcurrentHashMap<String, Restlet>();

    /**
     * Constructor.
     * 
     * @param context
     *            The context.
     */
    public OAuth2RealmRouter(Context context) {
        super(context);
        errorHandler =
                Finder.createFinder(ErrorServerResource.class, null, getContext(), getLogger());
    }

    @Override
    public Restlet getNext(Request request, Response response) {
        Restlet next = super.getNext(request, response);
        if (next == null) {
            String realm = OAuth2Utils.getRealm(request);
            response.setStatus(Status.SUCCESS_ACCEPTED); // TODO Use the default
                                                         // route
            if (null == realm) {
                if (null == defaultRealm) {
                    OAuthProblemException.OAuthError.NOT_FOUND.handle(request,
                            "No Default Realm configured").pushException();
                } else {
                    next = defaultRealm;
                }
            } else {
                next = realmRoutes.get(realm);
                if (null == next) {
                    //OAuthProblemException.OAuthError.NOT_FOUND.handle(
                    //        request,
                    //        1 > realmRoutes.size() ? "There is not Realm configured"
                    //                : "Realm was not configured").pushException();

                    //this.attachRealm(realm, defaultRealm);
                }
            }
            next = next != null ? next : errorHandler;
        }
        return next;
    }

    /**
     * {@inheritDoc}
     */
    public boolean attachRealm(String realm, Restlet next) {
        if (OAuth2Utils.isNotBlank(realm) && null != next) {
            return null != realmRoutes.put(realm, next);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Restlet detachRealm(String realm) {
        if (OAuth2Utils.isNotBlank(realm)) {
            return realmRoutes.remove(realm);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean attachDefaultRealm(Restlet next) {
        if (null != next) {
            synchronized (this) {
                defaultRealm = next;
            }
        }
        return null != defaultRealm && null != next;
    }

    /**
     * {@inheritDoc}
     */
    public Restlet detachDefaultRealm() {
        Restlet last = defaultRealm;
        synchronized (this) {
            defaultRealm = null;
        }
        return last;
    }
}
