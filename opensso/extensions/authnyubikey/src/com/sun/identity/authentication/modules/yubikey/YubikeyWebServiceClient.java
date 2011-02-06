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
 * $Id: YubikeyWebServiceClient.java,v 1.1 2008/11/20 04:28:42 superpat7 Exp $
 *
 */

package com.sun.identity.authentication.modules.yubikey;

import com.sun.identity.shared.debug.Debug;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class YubikeyWebServiceClient {
    private String authSvcUrl;
    private int clientId;
    private static Debug debug = YubikeyLoginModule.debug;

    public YubikeyWebServiceClient( String authSvcUrl, int clientId) {
        this.authSvcUrl = authSvcUrl;
        this.clientId = clientId;
    }

    public boolean validateToken(String otp) {
		boolean result = false;
        BufferedReader in = null;

		try {
	        URL url = new URL(authSvcUrl + "?id=" + clientId + "&otp=" + otp);
            if (debug.messageEnabled()) {
                debug.message("YubikeyWebServiceClient: url = " + url);
            }
	        URLConnection urlConn = url.openConnection();
	        in = new BufferedReader(new InputStreamReader(
                urlConn.getInputStream()));
            String response = "";
	        String inputLine;
	        while ((inputLine = in.readLine()) != null) {
	        	response += inputLine + "\n";
                if (inputLine.equals("status=OK")) {
                    result = true;
                }
	        }

            if (debug.messageEnabled()) {
                debug.message("YubikeyWebServiceClient: response = " + response);
            }
            // TODO - HMAC - until this is done, be sure to use 'https'
            // service URL!!!
		} catch (Exception e) {
            Logger.getLogger(
                YubikeyWebServiceClient.class.getName()).log(
                Level.SEVERE, null, e);
        } finally {
            if ( in != null ) {
                try {
                    in.close();
                } catch (IOException ex) {
                    Logger.getLogger(
                        YubikeyWebServiceClient.class.getName()).log(
                        Level.SEVERE, null, ex);
                }
            }
        }

		return result;
    }
}
