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
 * $Id: AMGenerateServerID.java,v 1.4 2008/06/25 05:41:27 qcheng Exp $
 *
 */

package com.iplanet.am.util;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.internal.AuthPrincipal;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.shared.debug.Debug;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/**
 * This class is basically used to generate two byte string server id that can
 * be used to update the platform server list. This class will be used either
 * during multiserver install or during existing dit install to update the
 * server entry list. By default, during our install, our platform server list
 * has it's default server id as "01" separated by delimiter "|". This class
 * also takes care for not generating existing server id in the current platform
 * server list.
 */

public class AMGenerateServerID {

    static private char[] alphabet = "0123456789abcdefghijklmnopqrstuvwxyz"
            .toCharArray();

    static String delimiter = "|";

    static Debug debug = Debug.getInstance("amMultiInstall");

    public static void main(String args[]) {

        if (args.length != 4) {
            System.out.println(
                    " Usage: AMGenerateServerID create|delete " +
                    "<serverurl> amadminDN amadminPassword");
            System.exit(1);
        }
        String opt = args[0];
        String serverUrl = args[1];
        try {
            String bindDN = args[2];
            String password = args[3];
            SSOTokenManager ssom = SSOTokenManager.getInstance();
            SSOToken token = ssom.createSSOToken(new AuthPrincipal(bindDN),
                password);
            Set servers = ServerConfiguration.getServerInfo(token);
            
            for (Iterator iter = servers.iterator(); iter.hasNext(); ) {
                String server = (String)iter.next();
                if (server.startsWith(serverUrl)) {
                    debug.message("server already exists., exiting");
                    System.exit(0);
                }
            }

            if (opt.equalsIgnoreCase("create")) {
                if (debug.messageEnabled()) {
                    debug.message("New server entry:" + serverUrl);
                }
                ServerConfiguration.createServerInstance(token, serverUrl, 
                    Collections.EMPTY_SET, "");
            } else if (opt.equalsIgnoreCase("delete")) {
                if (ServerConfiguration.deleteServerInstance(token, serverUrl)){
                    debug.message("Server entry to be removed:" + serverUrl);
                } else {
                    debug.message("Can not find server in server's list:"
                            + serverUrl);
                    System.exit(1);
                }
            } else {
                debug.message("Unknown option in AMGenerateServerID");
                System.exit(1);
            }
        } catch (Exception e) {
            debug.error("Exception occured:", e);
        }
    }

    /**
     * This method returns server id for a corresponding server
     */
    public static String returnId(Set servers, String server) {
        String serverId = null;
        Iterator iter = servers.iterator();
        while (iter.hasNext()) {
            String serverEntry = (String) iter.next();
            if (!serverEntry.startsWith(server)) {
                continue;
            }
            int index = serverEntry.indexOf(delimiter);
            if (index != -1) {
                serverId = serverEntry.substring(index + 1, serverEntry
                        .length());
                return serverId;
            }
        }
        return serverId;
    }

    /**
     * This function returns a two byte string but by skipping if finds it in
     * the passing hashed set. This function generates two byte strings in an
     * order starting from "01, 02, 03,.........,aa, ab,.....,zz,...ZZ"
     */
    public static String getTwoByteString(Set skip) {
        for (int i = 0; i < alphabet.length; i++) {
            char[] out = new char[2];
            out[0] = alphabet[i];
            for (int j = 1; j < alphabet.length; j++) {
                out[1] = alphabet[j];
                String temp = new String(out);
                if ((skip != null) && skip.contains(temp)) {
                    continue;
                }
                return temp;
            }
        }
        // Return a random string, it should never come to this.
        return "xx";
    }

}
