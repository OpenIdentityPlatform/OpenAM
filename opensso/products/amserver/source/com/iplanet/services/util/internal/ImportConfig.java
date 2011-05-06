/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ImportConfig.java,v 1.2 2008/06/25 05:41:42 qcheng Exp $
 *
 */

package com.iplanet.services.util.internal;

import java.io.FileInputStream;

import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.services.ldap.LDAPUser;
import com.iplanet.services.ldap.ServerInstance;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.internal.AuthContext;
import com.sun.identity.authentication.internal.AuthPrincipal;
import com.sun.identity.sm.ServiceManager;

public class ImportConfig {
    private static AuthPrincipal authPcpl;

    static public void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("usage: serverAdmin import xmlFile");
            System.exit(1);
        }
        if (args[0].equals("import")) {
            try {
                FileInputStream fisSchema = new FileInputStream(args[1]);
                DSConfigMgr cfgMgr = DSConfigMgr.getDSConfigMgr();
                ServerInstance sInst = cfgMgr
                        .getServerInstance(LDAPUser.Type.AUTH_ADMIN);
                authPcpl = new AuthPrincipal(sInst.getAuthID());
                AuthContext authCtx = new AuthContext(authPcpl, sInst
                        .getPasswd().toCharArray());

                SSOToken userSSOToken = authCtx.getSSOToken();

                ServiceManager smsMgr = new ServiceManager(userSSOToken);

                smsMgr.registerServices(fisSchema);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println(e);
            }
        }
    }
}
