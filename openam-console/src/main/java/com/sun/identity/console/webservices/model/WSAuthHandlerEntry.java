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
 * $Id: WSAuthHandlerEntry.java,v 1.3 2008/11/26 18:21:43 farble1670 Exp $
 *
 */

package com.sun.identity.console.webservices.model;

import java.util.StringTokenizer;

/* - NEED NOT LOG - */

public class WSAuthHandlerEntry {
    public String strKey;
    public String strClass;

    public WSAuthHandlerEntry(String strKey, String strClass) {
        this.strKey = strKey;
        this.strClass = strClass;
    }
    
    public WSAuthHandlerEntry(String formatedStr) {
	StringTokenizer st = new StringTokenizer(formatedStr, "|");

	if (st.countTokens() == 2) {
	    boolean valid = true;

	    while (st.hasMoreTokens() && valid) {
		String token = st.nextToken();
		if (token.startsWith(WSAuthNServicesModelImpl.KEY_PREFIX)) {
		    strKey = token.substring(
			WSAuthNServicesModelImpl.KEY_PREFIX.length());
		} else if (token.startsWith(
		    WSAuthNServicesModelImpl.CLASS_PREFIX)) {
		    strClass = token.substring(
			WSAuthNServicesModelImpl.CLASS_PREFIX.length());
		} else {
		    valid = false;
		}
	    }
	}
    }

    public static String toString(String key, String clazz) {
	StringBuffer buff = new StringBuffer();
	buff.append(WSAuthNServicesModelImpl.KEY_PREFIX)
	    .append(key)
	    .append("|")
	    .append(WSAuthNServicesModelImpl.CLASS_PREFIX)
	    .append(clazz);
	return buff.toString();
    }

    public String toString() {
	return WSAuthNServicesModelImpl.KEY_PREFIX + strKey + "|" +
	    WSAuthNServicesModelImpl.CLASS_PREFIX + strClass;
    }

    public boolean isValid() {
	return (strKey != null) && (strClass != null);
    }
}
