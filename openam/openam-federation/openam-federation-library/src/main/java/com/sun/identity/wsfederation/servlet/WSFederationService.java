/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: WSFederationService.java,v 1.1 2009/12/14 23:42:49 mallas Exp $
 *
 */
package com.sun.identity.wsfederation.servlet;



import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;

import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.wsfederation.profile.SPCache;
import com.sun.identity.wsfederation.common.WSFederationUtils;

/**
 * The ws-federation service enables the applications to retrieve the user's
 * single sign-on SAML Assertion.
 */

@Path("wsfederationservice")
public class WSFederationService {

    @Context
    private UriInfo context;

    public static final String RP = "RP";

    public static final String IP = "IP";

    /**
     * Returns the cached SAML Assertion for a given user session. The user's
     * SAML Assertion here is retrieved through respective SP or the IDP Cache.
     * Currently only the SP stores the Assertion in the cache.
     * @param token the user's session.
     * @param entityID the entityID
     * @param entityRole the entity role for e.g. RP or IP
     * @return the SAML Assertion xml string
     * @return null if there is a failure.
     */
    @GET
    @Produces("application/xml")
    public String getAssertion(@QueryParam("token") String token,
            @QueryParam("entityID") String entityID,
            @QueryParam("entityRole") String entityRole) {
        
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            Object session = sessionProvider.getSession(token);
            if(!sessionProvider.isValid(session)) {
               if(WSFederationUtils.debug.warningEnabled()) {
                  WSFederationUtils.debug.warning("WSFederationService." +
                          "getAssertion: invalid session");
               }
               return null;
            }
            String[] assertionID =
                    sessionProvider.getProperty(session, "AssertionID");
            if(assertionID.length == 0) {
               if(WSFederationUtils.debug.warningEnabled()) {
                  WSFederationUtils.debug.warning("WSFederationService." +
                          "getAssertion: assertionID is null");
               }
            }
            if(entityRole == null || entityRole.equals(RP)) {
               return (String)SPCache.assertionByIDCache.get(assertionID[0]);
            } else {
               // TODO: handle IP case later depending on the use case.
               return null;
            }

        } catch (SessionException se) {
            WSFederationUtils.debug.warning("WSFederationService." +
                          "getAssertion: session exception", se);
            return null;
        }
    }
}
