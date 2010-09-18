/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AMJettyUserRealm.java,v 1.1 2009/01/21 18:39:40 kanduls Exp $
 *
 */

package com.sun.identity.agents.jetty.v61;

import com.sun.identity.agents.arch.IModuleAccess;
import com.sun.identity.agents.realm.AmRealmAuthenticationResult;
import com.sun.identity.agents.realm.AmRealmManager;
import com.sun.identity.agents.realm.IAmRealm;
import java.security.Principal;
import java.util.Iterator;
import java.util.Set;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.plus.jaas.JAASUserPrincipal;
import org.mortbay.jetty.plus.jaas.JAASUserRealm;

public class AMJettyUserRealm extends JAASUserRealm {
    
    private static IAmRealm amRealm = null;
    private static IModuleAccess moduleAccess = null;

    static {
        try {
            amRealm = AmRealmManager.getAmRealmInstance();
            moduleAccess = AmRealmManager.getModuleAccess();
            if ((moduleAccess != null)
                    && moduleAccess.isLogMessageEnabled()) {
                moduleAccess.logMessage(
                    "AmJettyUserRealm: Realm Initialized");
            }
        } catch (Exception ex) {
            if ((moduleAccess != null)
                    && moduleAccess.isLogWarningEnabled()) {
                moduleAccess.logError(
                    "AmJettyUserRealm: Realm Instantiation Error: " + ex);
            }
        }
    }
    
    public AMJettyUserRealm ()
    {
        super();
    }
    
    public AMJettyUserRealm(String name)
    {
        this();
        super.realmName = name;
    }

    public boolean isUserInRole(Principal jettyUser, String role) {
        String username = null;
        boolean hasRole = false;
        Set setRoles = null;
        try {
            if (jettyUser != null) {
                username = jettyUser.getName();
            }
            moduleAccess.logMessage(
                            "AMJettyUserRealm: " + username
                            + " required role for the app " + role);
            if ((role != null) && (username != null)) {
                setRoles = amRealm.getMemberships(username);
                if ((setRoles != null) && (setRoles.size() > 0)) {
                    hasRole = setRoles.contains(role);
                    
                    if ((moduleAccess != null)
                            && moduleAccess.isLogMessageEnabled()
                            && hasRole) {
                        moduleAccess.logMessage(
                            "AMJettyUserRealm: " + username
                            + " has secuity role " + role);
                    } else {
                        if ((moduleAccess != null)
                                && moduleAccess.isLogMessageEnabled()) {
                            Iterator it = setRoles.iterator();
                            StringBuffer roleList = new StringBuffer();

                            while (it.hasNext()) {
                                roleList.append((String) it.next());
                                roleList.append(" ");
                            }

                            moduleAccess.logMessage(
                                "AMJettyUserRealm: " + username
                                + " has roles : " + roleList.toString());
                        }
                    }
                }
            }

            if (!hasRole && (moduleAccess != null)
                    && moduleAccess.isLogMessageEnabled()) {
                moduleAccess.logMessage(
                    "AMJettyUserRealm: " + username + " does not have role "
                    + role);
            }
        } catch (Exception ex) {
            if (moduleAccess != null) {
                moduleAccess.logError(
                    "AMJettyUserRealm: encountered exception "
                    + ex.getMessage() + " while fetching roles for user "
                    + username,
                    ex);
            }
        }
        return hasRole;
    }

    /**
     * Authenticate a user.
     * 
     *
     * @param username provided by the user at login
     * @param credentials provided by the user at login
     * @param request a <code>Request</code> value
     * @return authenticated JAASUserPrincipal or  null if authenticated failed
     */
    public Principal authenticate(String username,
            Object credentials,
            Request request) {
        JAASUserPrincipal jettyUser = null;
        try {
            AmRealmAuthenticationResult result = amRealm.authenticate(
                    username,
                    credentials.toString());
            

            if ((result == null) || (!result.isValid())) {
                if ((moduleAccess != null) && 
                        moduleAccess.isLogMessageEnabled()) {
                    moduleAccess.logMessage(
                            "AMJettyUserRealm: Authentication FAILED for " +
                            username);
                }
            } else {
                jettyUser = new JAASUserPrincipal(this, username);
                if ((moduleAccess != null) && 
                        moduleAccess.isLogMessageEnabled()) {
                    moduleAccess.logMessage(
                            "AMJettyUserRealm: Authentication SUCCESSFUL for " +
                            username);
                }
            }
            if ((moduleAccess != null) && moduleAccess.isLogMessageEnabled()) {
                Set roles = result.getAttributes();

                if ((roles != null) && (roles.size() > 0)) {
                    Iterator it = roles.iterator();
                    StringBuffer bufRoles = new StringBuffer();

                    while (it.hasNext()) {
                        String role = (String) it.next();
                        bufRoles.append(role);
                        bufRoles.append(" ");
                    }

                    moduleAccess.logMessage(
                            "AMJettyUserRealm: User " + username + 
                            " has roles: " + bufRoles.toString());
                }
            }
        } catch (Exception ex) {
            if (moduleAccess != null) {
                moduleAccess.logError(
                        "AMJettyUserRealm: encountered exception " + 
                        ex.getMessage() + " while authenticating user " + 
                        username,
                        ex);
            }
        }
        return jettyUser;
    }
    
    /**
     * Logout a previously logged in user.
     * This can only work for FORM authentication
     * as BasicAuthentication is stateless.
     * 
     * The user's LoginContext logout() method is called.
     * @param user an <code>Principal</code> value
     */
    public void logout(Principal user)
    {
        //Override this function to avoid nullpointer exceptions from the super
        //class.  The super class trys to invoke logincontext logout which is
        //null in this case.
    }
}
