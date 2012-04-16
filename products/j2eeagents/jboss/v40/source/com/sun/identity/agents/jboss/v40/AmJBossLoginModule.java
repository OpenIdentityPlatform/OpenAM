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
 * $Id: AmJBossLoginModule.java,v 1.1 2008/12/11 15:01:02 naghaon Exp $
 *
 */

package com.sun.identity.agents.jboss.v40;

import com.sun.identity.agents.arch.IModuleAccess;
import com.sun.identity.agents.realm.AmRealmAuthenticationResult;
import com.sun.identity.agents.realm.AmRealmManager;
import com.sun.identity.agents.realm.IAmRealm;

import org.jboss.security.auth.spi.UsernamePasswordLoginModule;
import org.jboss.security.SimpleGroup;
import java.security.acl.Group;
import javax.security.auth.login.LoginException;

import java.security.Principal;
import java.util.Iterator;
import java.util.Set;


/**
 * <b>AmJBossLoginModule</b> is the custome login module used
 * to authenticate OpenSSO users to JBoss.
 *
 */
public class AmJBossLoginModule extends UsernamePasswordLoginModule {
    private static final String STR_JBOSS_ROLES = "Roles";
    private transient SimpleGroup userRoles = new SimpleGroup(STR_JBOSS_ROLES);
    
    /**
     * Default constructor
     */
    public AmJBossLoginModule() {
    }

    /**
     * Validate the credentials with AM. This method gets called within login()
     * method.  
     *
     * @param inputPassword the password to validate.
     * @param expectedPassword ignored
     * @return valid true/false
     *
     */
    public boolean validatePassword(
            String inputPassword,
            String expectedPassword) {
        
        boolean valid = false;
        IModuleAccess moduleAccess = AmRealmManager.getModuleAccess();
        if (moduleAccess.isLogMessageEnabled()) {
            moduleAccess.logMessage("AmJBossLoginModule.validatePassword()");
        }

        // retrieve username
        String username  = getUsername();
        
        IAmRealm amRealm = getRealmInstance();
        if (amRealm != null) {
            try {
            
                // Authenticate username and inputPassword with AM
                AmRealmAuthenticationResult result = amRealm.authenticate(
                    username,
                    inputPassword);
            
                if ((result == null) || (!result.isValid())) {
                    if ((moduleAccess != null)
                    && moduleAccess.isLogMessageEnabled()) {
                        moduleAccess.logMessage(
                            "AmJBossLoginModule: Authentication FAILED for "
                            + username);
                    }
                } else {
                
                    // Create the set of roles the user belongs to
                    Set roles = result.getAttributes();
                
                    if ((roles != null) && (roles.size() > 0)) {
                        StringBuffer bufRoles = new StringBuffer();
                        Iterator it= roles.iterator();
                        while(it.hasNext()){
                            String role = (String) it.next();
                        
                            //Add each named role to the "Roles" principal
                            if(role != null) {
                                Principal p = super.createIdentity(role);
                                userRoles.addMember(p);
                            }
                        }

                        if ((moduleAccess != null)
                        && moduleAccess.isLogMessageEnabled()) {
                            moduleAccess.logMessage(
                                "AmJBossLoginModule: Authentication " + 
                                "SUCCESSFUL for " + username);
                        }

                        valid = true;
                
                    }
                }
            } catch (Exception ex) {
            
                if (moduleAccess != null) {
                    moduleAccess.logError(
                        "AmJBossLoginModule: encountered exception "
                        + ex.getMessage() + " while authenticating user "
                        + username,
                        ex);
                }
            }
        } else {
            if (moduleAccess.isLogWarningEnabled()) {
                moduleAccess.logWarning("AmJBossLoginModule : " +
                        "Failed to obtain AMRealm");
            }
        }
        
        return valid;
    }
    
    /**
     * This should return at least one Group with the name "Roles",
     * which will contain all the roles the user is in.
     *
     * @return roleSets The list of all the user's roles, inside a
     * 			Group called "Roles"
     * @throws LoginException
     */
    public Group[] getRoleSets() throws LoginException {
        Group[] roleSets = {userRoles};
        return roleSets;
    }
    
    /**
     * Overriden to return an empty password string as typically one cannot
     * obtain a user's password.  the validatePassword() is overriden so
     * this is ok.
     *
     * @return empty password String
     */
    public String getUsersPassword() throws LoginException {
        return "";
    }

    private IAmRealm getRealmInstance() {
        IAmRealm result = null;

        try {
            result = AmRealmManager.getAmRealmInstance();
        } catch(Exception ex) {
            // No handling required
        }

        return result;
    }

}
