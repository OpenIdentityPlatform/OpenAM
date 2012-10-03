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
 * $Id: Hello.java,v 1.2 2009/05/12 05:38:47 kevinserwin Exp $
 */

import java.io.*;
import java.util.*;
import javax.security.auth.callback.*;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.setup.EmbeddedOpenSSO;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;

/**
 *
 */
public class Hello {

    public static void main(String[] argv) {
        String baseDir = System.getProperty("user.home") + "/embeddedOpenSSO";
        System.out.println("baseDir = " + baseDir);
        Map<String, String> configData = new HashMap<String, String>();
        ResourceBundle res = ResourceBundle.getBundle("configparam");
        for (Enumeration e = res.getKeys(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            String val = res.getString(key);
            configData.put(key, val);
        }
        EmbeddedOpenSSO embOpenSSO = new EmbeddedOpenSSO(baseDir, configData);


        boolean isConfigured = embOpenSSO.isConfigured();
        if (!isConfigured) {
            System.out.println("EmbeddedOpenSSO is not configured.");

            System.out.println("Configuring EmbeddedOpenSSO.");
            embOpenSSO.configure();
        } else {
            System.out.println("EmbeddedOpenSSO is configured.");
        }

        embOpenSSO.startup();

        BufferedReader br = new BufferedReader(
            new InputStreamReader(System.in));
        while (true) {
          try {

            System.out.println("\nEnter quit to exit.");
            System.out.print("UserName:");
            String userid = br.readLine();
            if (userid.equals("quit")) {
                break;
            }
            System.out.print("Password:");
            String pwd = br.readLine();
            System.out.print("\n");


            AuthContext ac = new AuthContext("/");
            ac.login();

            if (ac.hasMoreRequirements()) {
                Callback[] callbacks = ac.getRequirements();
                if (callbacks != null) {
                    for (int i = 0; i < callbacks.length; i++) {
                        if (callbacks[i] instanceof NameCallback) {
                            NameCallback nc = (NameCallback) callbacks[i];
                            nc.setName(userid);
                        } else if (callbacks[i] instanceof PasswordCallback) {
                            PasswordCallback pc =
                                (PasswordCallback)callbacks[i];
                            pc.setPassword(pwd.toCharArray());
                        }
                    }
                    ac.submitRequirements(callbacks);
                }
            }
            if (ac.getStatus() != AuthContext.Status.SUCCESS) {
                System.out.println("Authentication Failed");
            } else {
                System.out.println("Authentication is successful.");

		   SSOToken token = ac.getSSOToken();

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
		    }

            }
        
          } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Authentication Failed");
          }
        }
        embOpenSSO.shutdown();

        System.exit(0);
    }
}
