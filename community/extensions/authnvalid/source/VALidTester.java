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
 * $Id: VALidTester.java,v 1.1 2009/04/21 10:23:28 ja17658 Exp $
 *
 */

package com.sun.identity.authentication.modules.valid;

import java.io.IOException;
import java.util.List;

import com.validsoft.products.valid.validsupport.*;
import com.validsoft.utils.sockets.exceptions.*;

/* This class can be used to test connectivity to the VALid server */

public class VALidTester {

    public static void main(String args[]) {
        String VALidApp = "valid-application";
        String VALidUser = "username";

        ValidClient client = ValidClientFactory.getInstance().createValidClient();
        if (client != null) {
            try {
		if (client instanceof DefaultValidClient) {
		    System.out.println("VALid: ValidClient class is instanceof DefaultValidClient");
		}
		// (1) Connect to the VALid server
                // client.connectToServer(appName);
		System.out.println("********** VALid: Connecting to server...");
                client.connectToServer(VALidApp);

		// TBD: How to check that the client it proper ?
		System.out.println("********** VALid: Application Name is "+client.getApplicationName());

		// (2) Validate the user
                // ValidationResponse validateResponse = client.validateUser(username);
                ValidationResponse validateResponse = client.validateUser(VALidUser);
		if (validateResponse != null) {
		    System.out.println("********** VALid: Response type: "+validateResponse.getType().toString());
		    switch (validateResponse.getType()) {
			case USER_OK:
		    	    System.out.println("********** VALid: User validated.");
			    break;
			default:
		    	    System.out.println("********** VALid: User NOT validated.");
		    }
		} else {
		    System.out.println("********** VALid: ValidationResponse is null.");
		}

                // 3.) If user validated, get methods
                List methods = client.getMethods();
		System.out.println("********** VALid: Number of methods="+methods.size());
		ContactMethod method = null;
		for (int i=0; i < methods.size(); i++) {
		    System.out.println("********** VALid: i="+i);
		    method = (ContactMethod) methods.get(i);
		    System.out.println("********** VALid: Method("+i+"): Name="+method.getName()+", Id: "+ method.getId()+", Characteristic="+method.getCharacteristic());
			
			
		}

		// (4) Authenticate using a method
		RequestAuthorisationResponse authnresponse = client.requestAuthorisation(2);
		if (authnresponse != null) {
		    System.out.println("********** VALid: Challenge="+authnresponse.getChallenge());
		}
		
            }
            catch (NoAvailableServersException ex) {
                // debug.error("VALid: VALid server not available.");
                ex.printStackTrace();
            }
            catch (IOException ex) {
                // debug.error("VALid: Exception when authenticating user again VALid");
                ex.printStackTrace();
            }
        } else {
            // debug.error("VALid: ValidClient object is null. Cannt authenticate");
        }
	
    }
    
}
