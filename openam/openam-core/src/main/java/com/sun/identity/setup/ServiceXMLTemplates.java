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
 * $Id: ServiceXMLTemplates.java,v 1.2 2009/05/02 23:07:18 kevinserwin Exp $
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.setup;

import com.sun.identity.shared.debug.Debug;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Set;
import javax.servlet.ServletContext;

/**
 * This utility class manages service XML template files.
 * @author dennis
 */
public class ServiceXMLTemplates {
    private ServiceXMLTemplates() {
    }
    
    public static void copy(String dir, ServletContext servletCtx) {
        if (servletCtx == null) {
           return;
        }
        File d = new File(dir);
        d.mkdirs();
        String classesDir = "/WEB-INF/classes";
        Set res = servletCtx.getResourcePaths(classesDir);
        
        for (Iterator i = res.iterator(); i.hasNext(); ) {
            String templ = (String) i.next();
            if (templ.endsWith(".xml")) {
                String content = getContent(templ, servletCtx);
                String fileName = templ.substring(classesDir.length()+1);
                try {
                    AMSetupServlet.writeToFile(dir + "/" + fileName, content);
                } catch (IOException e) {
                    Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                        "ServiceXMLTemplates.copy", e);
                }
            }
        }
    }
    
    private static String getContent(
        String templateName, 
        ServletContext servletCtx
    ) {
        InputStreamReader fin = null;
        StringBuilder sbuf = new StringBuilder();

        try {
            fin = new InputStreamReader(
                AMSetupServlet.getResourceAsStream(servletCtx, templateName));
            char[] cbuf = new char[1024];
            int len;
            while ((len = fin.read(cbuf)) > 0) {
                sbuf.append(cbuf, 0, len);
            }
        } catch (IOException e) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                "ServiceXMLTemplates.getContent", e);
        } finally {
            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
        return sbuf.toString();
    }
}
