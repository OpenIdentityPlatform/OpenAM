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
 * $Id: DecisionResource.java,v 1.10 2009/08/14 22:46:18 veiming Exp $
 */

package com.sun.identity.entitlement;

import com.sun.identity.entitlement.util.AuthSPrincipal;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.security.auth.Subject;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

/**
 * Exposes the entitlement decision REST resource.
 * 
 * @author Paul C. Bryan <pbryan@sun.com>
 */
@Path("/1/entitlement/decision")
public class DecisionResource {

    public enum Permission { deny, allow }

    private @Context SecurityContext security;

    @GET
    @Produces("text/plain")
    public String decision(
     @QueryParam("realm") String realm,
     @QueryParam("subject") String subject,
     @QueryParam("action") String action,
     @QueryParam("resource") String resource,
     @QueryParam("application") String application) {

// FIXME: once OAuth filter comes stock, let's use it to authenticate the caller
Subject caller = toSubject("id=amAdmin,ou=user,dc=opensso,dc=java,dc=net");
//        Subject caller = toSubject(security.getUserPrincipal());

        try {
            Evaluator evaluator = (application == null ?
             new Evaluator(caller) : new Evaluator(caller, application));

            return permission(evaluator.hasEntitlement(realm, toSubject(subject),
             toEntitlement(resource, action), Collections.EMPTY_MAP));
        }

        // fail safe
        catch (EntitlementException ee) {
            return permission(false);
        }
    }

    private Entitlement toEntitlement(String resource, String action) {
        HashSet set = new HashSet<String>();
        set.add(action);
        return new Entitlement(resource, set);
    }

    private Subject toSubject(Principal principal) {
        if (principal == null) {
            return null;
        }
        Set<Principal> set = new HashSet<Principal>();
        set.add(principal);
        return new Subject(false, set, new HashSet(), new HashSet());
    }

    private Subject toSubject(String subject) {
        if (subject == null) {
            return null;
        }
        return toSubject(new AuthSPrincipal(subject));
    }

    private String permission(boolean b) {
        return (b ? Permission.allow.toString() : Permission.deny.toString());
    }
}

