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
 * $Id: CommandLineSSO.java,v 1.3 2008/06/25 05:41:14 qcheng Exp $
 *
 */

package com.sun.identity.samples.sso;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

/**
 * This sample demonstrates the use of retrieving user profile from the correct
 * user credential.
 */
public class CommandLineSSO {
    
    public static void main(String args[]) throws Exception {
        String orgName = args[0];
        System.out.println("Organization: " + orgName);

        SSOTokenManager manager = SSOTokenManager.getInstance();
        AuthContext lc = getAuthcontext(orgName);
        
        if (lc.getStatus() == AuthContext.Status.SUCCESS) {
            System.out.println("Successful authentication ...");
            SSOToken token = lc.getSSOToken();

            String userDN = token.getPrincipal().getName();
            System.out.println("User Name: " + userDN);

            try {
                AMIdentity userIdentity = IdUtils.getIdentity(token);
                Map attrs = userIdentity.getAttributes();
                System.out.println("User Attributes: ");

                for (Iterator i = attrs.keySet().iterator(); i.hasNext(); ) {
                    String attrName = (String)i.next();
                    Set values = (Set)attrs.get(attrName);
                    System.out.println(attrName + "=" + values);
                }
            } catch (IdRepoException e) {
                e.printStackTrace();
            } finally {
                manager.destroyToken(token);
            }
        } else {
             System.out.println("Authentication Failed ....... ");
        }
        System.exit(0);
    }

    // Creates AuthContext and submits requirements
    private static AuthContext getAuthcontext(String orgName)
        throws AuthLoginException, IOException
    {
        AuthContext lc = new AuthContext(orgName);
        AuthContext.IndexType indexType = AuthContext.IndexType.MODULE_INSTANCE;
        String indexName = "DataStore";
        System.out.println("DataStore: Obtained login context");
        lc.login(indexType, indexName);

        Callback[] callback = lc.getRequirements();
        
        for (int i =0 ; i< callback.length ; i++) {
            if (callback[i] instanceof NameCallback) {
                NameCallback name = (NameCallback) callback[i];
                System.out.print(name.getPrompt());
                name.setName((new BufferedReader(
                    new InputStreamReader(System.in))).readLine());
            } else if (callback[i] instanceof PasswordCallback) {
                PasswordCallback pass = (PasswordCallback) callback[i];
                System.out.print(pass.getPrompt());
                String password = (new BufferedReader(
                    new InputStreamReader(System.in))).readLine();
                pass.setPassword(password.toCharArray());
            }
        }

        lc.submitRequirements(callback);
        return lc;
    }
}
