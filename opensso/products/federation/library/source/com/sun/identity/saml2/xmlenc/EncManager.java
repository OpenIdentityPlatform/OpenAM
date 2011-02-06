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
 * $Id: EncManager.java,v 1.2 2008/06/25 05:48:03 qcheng Exp $
 *
 */


package com.sun.identity.saml2.xmlenc;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.common.SAML2Constants;

/**
 * The <code>EncManager</code> class is used to determine
 * the default EncProvider concret class to use, and to provide
 * access to an instance of that class
 */
public final class EncManager {
    
    private static EncProvider ep = null;
    
    static {
	try {
            String encryptClass = SystemPropertiesManager.get(
		SAML2Constants.ENC_PROVIDER,
		"com.sun.identity.saml2.xmlenc.FMEncProvider"
	    );
            ep = (EncProvider)
		Class.forName(encryptClass).newInstance();
	    
        } catch (Exception e) {
            SAML2SDKUtils.debug.error(
		"EncManager static block:" +
		" Exception in constructing xml encryption manager",
		e);
        }	
    }

    private EncManager() {
    }
    
    /**
     * Gets the instance of <code>EncProvider</code>
     * @return <code>EncProvider</code>
     */
    public static EncProvider getEncInstance() {
        return ep;
    }
} 




