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
 * $Id: SAML2SOAPBindingRequestHandler.java,v 1.2 2008/06/25 05:49:46 qcheng Exp $
 *
 */

package com.sun.identity.console.service.model;

import java.util.StringTokenizer;

/* - NEED NOT LOG - */

public class SAML2SOAPBindingRequestHandler {
    public String strKey;
    public String strClass;
    public String strSOAPAction;
    public boolean valid;
    
    public SAML2SOAPBindingRequestHandler(String formatedStr) {
        StringTokenizer st = new StringTokenizer(formatedStr, "|");
        int count = st.countTokens();
        
        if ((count == 2) || (count == 3)) {
            valid = true;
            
            while (st.hasMoreTokens() && valid) {
                String token = st.nextToken();
                if (token.startsWith(SCSAML2SOAPBindingModelImpl.KEY_PREFIX)) {
                    strKey = token.substring(
                        SCSAML2SOAPBindingModelImpl.KEY_PREFIX.length());
                } else if (token.startsWith(
                    SCSAML2SOAPBindingModelImpl.CLASS_PREFIX)) {
                    strClass = token.substring(
                        SCSAML2SOAPBindingModelImpl.CLASS_PREFIX.length());               
                } else {
                    valid = false;
                }
            }
        }
    }
    
    public static String toString(String key, String clazz) {
        String str = SCSAML2SOAPBindingModelImpl.KEY_PREFIX + key + "|" +
            SCSAML2SOAPBindingModelImpl.CLASS_PREFIX + clazz;        
        return str;
    }
    
    public boolean isValid() {
        return valid;
    }
}
