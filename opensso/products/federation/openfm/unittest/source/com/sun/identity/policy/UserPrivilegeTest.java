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
 * $Id: UserPrivilegeTest.java,v 1.1 2009/08/19 05:41:03 veiming Exp $
 */
package com.sun.identity.policy;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;
import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

/**
 *
 * @author dillidorai
 */
public class UserPrivilegeTest {
    public static final String USER_NAME = "UserPrivilegeTestUser1";
    SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
        AdminTokenAction.getInstance());
    SSOToken userToken = null;
    AMIdentityRepository amir = null;
    AMIdentity user = null;

    @Test
    public void setup() throws Exception {
        amir = new AMIdentityRepository(adminToken, "/");
        String name = USER_NAME;
        Map attrMap = new HashMap();
        Set cnVals = new HashSet();
        cnVals.add(name);
        attrMap.put("cn", cnVals);

        Set snVals = new HashSet();
        snVals.add(name);
        attrMap.put("sn", snVals);

        Set nameVals = new HashSet();
        nameVals.add(name);
        attrMap.put("givenname", nameVals);

        Set passworVals = new HashSet();
        passworVals.add(name);
        attrMap.put("userpassword", passworVals);

        amir.createIdentity(IdType.USER, name, attrMap);
        userToken = createSessionToken("/", name, name, null, -1);

        user = IdUtils.getIdentity(userToken);
    }

    @AfterClass
    public void cleanup() throws Exception {
        Set identities = new HashSet();
        identities.add(user);
        amir.deleteIdentities(identities);
    }

    @Test(dependsOnMethods = {"setup"})
    public void testUpdateEmailAddress() throws Exception {
        Map attrMap = new HashMap();
        Set mailVals = new HashSet();
        mailVals.add("user1@sun.com");
        attrMap.put("mail", mailVals);
        user.setAttributes(attrMap);
        user.store();

    }

    @Test(dependsOnMethods = {"testUpdateEmailAddress"})
    public void testReadEmailAddress() throws Exception {
        Set attrNames = new HashSet();
        attrNames.add("mail");
        Map attrMap = user.getAttributes(attrNames);
        Set mailVals = (Set) attrMap.get("mail");
        if (mailVals == null) {
            throw new Exception("mail values null");
        }
        if (!mailVals.contains("user1@sun.com")) {
            throw new Exception("mail value does not match");
        }
    }

    private SSOToken createSessionToken(String orgName, String userId,
            String password, String module, int level)
            throws Exception {
        AuthContext ac = null;
        try {
            ac = new AuthContext(orgName);
            if (module != null) {
                ac.login(AuthContext.IndexType.MODULE_INSTANCE, module);
            } else if (level != -1) {
                ac.login(AuthContext.IndexType.LEVEL, String.valueOf(level));
            } else {
                ac.login();
            }
        } catch (LoginException le) {
            le.printStackTrace();
            return null;
        }

        try {
            Callback[] callbacks = null;
            // Get the information requested by the plug-ins
            if (ac.hasMoreRequirements()) {
                callbacks = ac.getRequirements();

                if (callbacks != null) {
                    addLoginCallbackMessage(callbacks, userId, password);
                    ac.submitRequirements(callbacks);

                    if (ac.getStatus() == AuthContext.Status.SUCCESS) {
                        //System.out.println("Auth success");
                        Subject authSubject = ac.getSubject();
                        if (authSubject != null) {
                            Iterator principals =
                                    (authSubject.getPrincipals()).iterator();
                            Principal principal;
                            while (principals.hasNext()) {
                                principal = (Principal) principals.next();
                            }
                        }
                    } else if (ac.getStatus() == AuthContext.Status.FAILED) {
                        //System.out.println("Authentication has FAILED");
                    } else {
                    }
                } else {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //SSOTokenManager.getInstance().validateToken(ac.getSSOToken());
        //System.out.println(ac.getSSOToken().getPrincipal().getName());
        return ac.getSSOToken();
    }

    static void addLoginCallbackMessage(Callback[] callbacks, String userId,
            String password)
            throws UnsupportedCallbackException {
        int i = 0;
        try {
            for (i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof NameCallback) {

                    // prompt the user for a username
                    NameCallback nc = (NameCallback) callbacks[i];

                    //System.out.println("userName=" + userId);
                    nc.setName(userId);

                } else if (callbacks[i] instanceof PasswordCallback) {

                    // prompt the user for sensitive information
                    PasswordCallback pc = (PasswordCallback) callbacks[i];

                    //System.out.println("password=" + password);
                    pc.setPassword(password.toCharArray());

                } else {
                }
            }
        } catch (Exception e) {
            //throw new UnsupportedCallbackException(callbacks[i],
            //"Callback exception: " + e);
        }
    }
}
