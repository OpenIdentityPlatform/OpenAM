/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AmASLoginModule.java,v 1.2 2008/06/25 05:52:11 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */
package com.sun.identity.agents.appserver.v81;

import java.util.Iterator;
import java.util.Set;
import javax.security.auth.login.LoginException;
import com.sun.enterprise.security.auth.login.PasswordLoginModule;
import com.sun.identity.agents.arch.IModuleAccess;
import com.sun.identity.agents.realm.AmRealmAuthenticationResult;
import com.sun.identity.agents.realm.AmRealmManager;
import com.sun.identity.agents.realm.IAmRealm;


/**
 * Sun(TM) ONE Application Server Agent realm login module.
 *
 * <P>Provides a SONEIS-based implementation of a password login module.
 * Processing is delegated to the AgentRealm class.
 *
 * @see com.sun.enterprise.security.auth.login.PasswordLoginModule
 * @see package com.sun.amagent.as.realm.AmAS81Realm;
 *
 */
public class AmASLoginModule extends PasswordLoginModule {

    /**
     * Perform file authentication. Delegates to AgentRealm.
     *
     * @throws LoginException If login fails (JAAS login() behavior).
     *
     */
    protected void authenticate() throws LoginException {

        AmASRealm agentRealm = getAgentRealm();
        String userName = getUserName();
        //Using protected variable from AppservPasswordLoginModule superclass
        String password = _password;

        if( agentRealm == null) {
            throw new LoginException("AmAS81LoginModule requires AgentRealm.");
        }


        IAmRealm amRealm = getRealmInstance();
        if (amRealm != null) {
            IModuleAccess modAccess = AmRealmManager.getModuleAccess();
            AmRealmAuthenticationResult authResult = 
                    amRealm.authenticate(userName, password);

            if (authResult.isValid()) {

                Set memberships = authResult.getAttributes();
                String[] groups = new String[memberships.size() + 1];
                 groups[0] = agentRealm.getAnyoneRole();
                Iterator it = memberships.iterator();
                int index = 1;
                while (it.hasNext()) {
                    groups[index++] = (String) it.next();
                }

                if (modAccess.isLogMessageEnabled()) {
                    StringBuffer buff = new StringBuffer("[");
                    for (int i=0; i<groups.length; i++) {
                        buff.append(groups[i]);
                        if (i < groups.length -1) {
                            buff.append(", ");
                        }
                    }
                    buff.append("]");
                    modAccess.logMessage("AmAS81LoginModule: User: "
                            + userName + ", groups: " + buff.toString());
                 }

                commitAuthentication(userName, password, agentRealm, groups);
            } else {
                    throw new LoginException("Failed to authenticate user");
            }
        } else {
            throw new LoginException("Failed to obtain service realm");
        }
    }

    private AmASRealm getAgentRealm() {
        AmASRealm result = null;
        if (_currentRealm instanceof AmASRealm) {
            result = (AmASRealm) _currentRealm;
        }
        return result;
    }

    private String getUserName() {
        return _username;
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

    private AmASRealm          _agentRealm;
}

