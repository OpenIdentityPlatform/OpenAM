/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AMURLStreamHandlerFactory.java,v 1.2 2008/06/25 05:52:56 qcheng Exp $
 *
 */

package com.sun.identity.protocol;

import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

/**
 * This class provides method to return configured https protocol handler.
 */
public class AMURLStreamHandlerFactory implements URLStreamHandlerFactory {
    static URLStreamHandler prot_handler = null;
    static String prot_handler_string = null;
    static private Debug debug = Debug.getInstance("amComm");
    
    static {
        prot_handler_string = 
                SystemPropertiesManager.get(Constants.PROTOCOL_HANDLER, null);
        if (prot_handler_string != null) {
            prot_handler_string = prot_handler_string + ".https.Handler";
        }
        if (debug.messageEnabled()) {
            debug.message("Configured Protocol Handler : " + 
                    prot_handler_string);
        }
    }

    /**
     * Returns configured https protocol handler.
     * @param protocol - the protocol ("https")
     * @return a <code>URLStreamHandler</code> for https protocol.
     */
    public URLStreamHandler createURLStreamHandler(String protocol) {
        String method = "AMURLStreamHandlerFactory.createURLStreamHandler ";
        
        URLStreamHandler prot_handler = null;
        
        if (protocol.equalsIgnoreCase("https") && 
                (prot_handler_string != null)) {
            try {
                prot_handler = (URLStreamHandler) Class.forName(
                        prot_handler_string).newInstance();
            } catch (ClassNotFoundException e) {
                debug.error(method + 
                        "Failed to find protocol handler class ", e);
            } catch (InstantiationException e) {
                debug.error(method +
                        "Failed to instantiate protocol handler class ", e);
            } catch (IllegalAccessException e) {
                debug.error(method + 
                        "Invalid access for protocol handler class ", e);
            }
        }
            
        return prot_handler;
    }
}
        
