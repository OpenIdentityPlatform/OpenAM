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
 * $Id: SOAPBindingRequestHandler.java,v 1.2 2008/06/25 05:49:47 qcheng Exp $
 *
 */
 
package com.sun.identity.console.service.model;

import java.util.StringTokenizer;

/* - NEED NOT LOG - */

public class SOAPBindingRequestHandler {
    public String strKey;
    public String strClass;
    public String strSOAPAction;
    public boolean valid;

    public SOAPBindingRequestHandler(String formatedStr) {
	StringTokenizer st = new StringTokenizer(formatedStr, "|");
	int count = st.countTokens();

	if ((count == 2) || (count == 3)) {
	    valid = true;

	    while (st.hasMoreTokens() && valid) {
		String token = st.nextToken();
		if (token.startsWith(SCSOAPBindingModelImpl.KEY_PREFIX)) {
		    strKey = token.substring(
			SCSOAPBindingModelImpl.KEY_PREFIX.length());
		} else if (token.startsWith(
		    SCSOAPBindingModelImpl.CLASS_PREFIX)) {
		    strClass = token.substring(
			SCSOAPBindingModelImpl.CLASS_PREFIX.length());
		} else if (token.startsWith(
		    SCSOAPBindingModelImpl.ACTION_PREFIX)) {
		    strSOAPAction = token.substring(
			SCSOAPBindingModelImpl.ACTION_PREFIX.length());
		} else {
		    valid = false;
		}
	    }
	}
    }

    public static String toString(String key, String clazz, String action) {
	String str = SCSOAPBindingModelImpl.KEY_PREFIX + key + "|" +
	    SCSOAPBindingModelImpl.CLASS_PREFIX + clazz;

	if ((action != null) && (action.trim().length() > 0)) {
	    str += "|" + SCSOAPBindingModelImpl.ACTION_PREFIX + action;
	}

	return str;
    }

    public boolean isValid() {
	return valid;
    }
}
