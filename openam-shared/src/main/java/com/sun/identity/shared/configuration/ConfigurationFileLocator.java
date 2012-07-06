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
 * $Id: ConfigurationFileLocator.java,v 1.2 2008/06/25 05:53:00 qcheng Exp $
 *
 */

package com.sun.identity.shared.configuration;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import javax.servlet.ServletContext;

/**
 * This class provides method to locate configuration file.
 */
public class ConfigurationFileLocator {
    public final static String AMCONFIG = "AMConfig";

    private ConfigurationFileLocator() {
    }

    public static String getBootStrapFileName(ServletContext servletCtx)
        throws MalformedURLException {
        String fileName = null;
        if (servletCtx != null) {
            String path = servletCtx.getResource("/").getPath();
            if (path != null) {
                path = path.replaceAll("/", "_");
                fileName = System.getProperty("user.home") + "/" + AMCONFIG +
                    path;
            }
        }
        return fileName;
    }
}
