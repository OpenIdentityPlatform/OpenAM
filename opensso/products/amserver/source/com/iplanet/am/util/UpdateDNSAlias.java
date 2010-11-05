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
 * $Id: UpdateDNSAlias.java,v 1.3 2008/06/25 05:41:28 qcheng Exp $
 *
 */

package com.iplanet.am.util;

import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.internal.AuthPrincipal;
import com.sun.identity.shared.debug.Debug;
import java.util.HashMap;
import java.util.Set;


public class UpdateDNSAlias {

    static Debug debug = Debug.getInstance("amMultiInstall");

    public static void main(String args[]) {

        if (args.length != 5) {
            System.out.println(" Usage: UpdateDNSAlias " +
                    "<add | delete> <orgdn> <dnsalias> <userdn> <passwd>");
            System.exit(1);
        }
        String opt = args[0];
        String orgDn = args[1];
        String dnsAlias = args[2];
        if ((opt == null) || (orgDn == null) || (dnsAlias == null)) {
            debug.error("One or more parameters are null");
            System.exit(1);
        }
        try {
            String bindDN = args[3];
            String password = args[4];
            SSOTokenManager ssom = SSOTokenManager.getInstance();
            SSOToken token = ssom.createSSOToken(new AuthPrincipal(bindDN),
                    password);
            AMStoreConnection asc = new AMStoreConnection(token);
            AMOrganization org = asc.getOrganization(orgDn);
            Set values = org.getAttribute("sunOrganizationAlias");
            HashMap map = new HashMap();
            if (opt.equalsIgnoreCase("add")) {
                if (!values.contains(dnsAlias)) {
                    values.add(dnsAlias);
                }
                map.put("sunOrganizationAlias", values);
                org.setAttributes(map);
                org.store();
            } else if (opt.equalsIgnoreCase("delete")) {
                values.remove(dnsAlias);
                map.put("sunOrganizationAlias", values);
                org.setAttributes(map);
                org.store();
            } else {
                debug.error("Unknown option in AMGenerateServerID");
                System.exit(1);
            }
        } catch (Exception e) {
            debug.error("Exception occured:", e);
        }
        System.exit(0);
    }

}
