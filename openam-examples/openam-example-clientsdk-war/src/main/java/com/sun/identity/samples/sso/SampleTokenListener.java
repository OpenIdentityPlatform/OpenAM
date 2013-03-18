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
 * $Id: SampleTokenListener.java,v 1.2 2008/06/25 05:41:09 qcheng Exp $
 *
 */

package com.sun.identity.samples.sso;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenEvent;
import com.iplanet.sso.SSOTokenListener;
import com.iplanet.sso.SSOTokenManager;

/** 
 * This is the SSO token listener class to listen for sso token events. 
 * Token events are received when the token state changes. The 
 * <code>ssoTokenChanged</code> is the method that is called when the 
 * event is triggered.
 */
public class SampleTokenListener implements SSOTokenListener {
    
    public SampleTokenListener() {
    }
    
    public void ssoTokenChanged(SSOTokenEvent event) {
        try {
            SSOToken token = event.getToken();
            int type = event.getType();
            long time = event.getTime();
            System.out.println("Token id is: " + token.getTokenID().toString());
            
            if (SSOTokenManager.getInstance().isValidToken(token)) {
                System.out.println("Token is Valid");
            } else {
                System.out.println("Token is Invalid");
            }
            
            switch(type) {
                case SSOTokenEvent.SSO_TOKEN_IDLE_TIMEOUT:
                    System.out.println("Token Idel Timeout event");
                    break;
                case SSOTokenEvent.SSO_TOKEN_MAX_TIMEOUT:
                    System.out.println("Token Max Timeout event");
                    break;
                case SSOTokenEvent.SSO_TOKEN_DESTROY:
                    System.out.println("Token Destroyed event");
                    break;
                default:
                    System.out.println("Unknown Token event");
                    break;
            }
        } catch (SSOException e) {
            System.out.println(e.getMessage());
        }
    }
}
