/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: VersionViewBean.java,v 1.1 2009/08/05 20:15:51 veiming Exp $
 *
 * Portions copyright 2015-2016 ForgeRock AS.
 */

package com.sun.identity.console.version;

import javax.servlet.http.HttpServletRequest;
import org.owasp.esapi.ESAPI;

public class VersionViewBean extends 
    com.sun.web.ui.servlet.version.VersionViewBean {

    public VersionViewBean() {
        super();
    }

    public static String validateVersionFile(
        HttpServletRequest request,
        String versionFile) {
        if (versionFile.length() == 0) {
            return versionFile;
        }
        if (!versionFile.startsWith(request.getContextPath()) &&
            !versionFile.startsWith(getCurrentURL(request))
        ) {
            return "../console/blank.html";
        }
        return versionFile;
    }

    public static String getCurrentURL(HttpServletRequest httpRequest) {
        return httpRequest.getScheme() + "://" +
            httpRequest.getServerName() + ":" +
            httpRequest.getServerPort() +
            httpRequest.getContextPath();
    }

    public static String escapeHTML(String html) {
        return ESAPI.encoder().encodeForHTML(html);
    }
}
