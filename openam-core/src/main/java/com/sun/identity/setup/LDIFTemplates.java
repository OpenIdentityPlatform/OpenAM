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
 * $Id: LDIFTemplates.java,v 1.5 2009/10/27 05:33:39 hengming Exp $
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.setup;

import com.sun.identity.shared.debug.Debug;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletContext;

/**
 * This utility class manages LDIF template files.
 */
public class LDIFTemplates {
    private static List templates;

    static {
        templates = new ArrayList();
        templates.add("ad/ad_user_schema.ldif");
        templates.add("adam/adam_user_schema.ldif");
        templates.add("opendj/opendj_config_schema.ldif");
        templates.add("opendj/opendj_user_schema.ldif");
        templates.add("opendj/opendj_embinit.ldif");
        templates.add("opendj/opendj_userinit.ldif");
        templates.add("opendj/opendj_user_index.ldif");
        templates.add("opendj/opendj_plugin.ldif");
        templates.add("opendj/opendj_remove_user_schema.ldif");
        templates.add("odsee/odsee_config_schema.ldif");
        templates.add("odsee/odsee_config_index.ldif");
        templates.add("odsee/odsee_user_index.ldif");
        templates.add("odsee/odsee_user_schema.ldif");
        templates.add("odsee/odsee_plugin.ldif");
        templates.add("odsee/odsee_userinit.ldif");
        templates.add("odsee/amsdk_plugin/amsdk_init_template.ldif");
        templates.add("odsee/amsdk_plugin/amsdk_sunone_schema2.ldif");
        templates.add("tivoli/tivoli_user_schema.ldif");
        templates.add("sfha/cts-add-schema.ldif");
        templates.add("sfha/cts-container.ldif");
        templates.add("sfha/cts-indices-schema.ldif");
        templates.add("sfha/odsee-cts-indices-schema.ldif");
    }

    private LDIFTemplates() {
    }

    public static void copy(String dir, ServletContext servletCtx) {
        for (Iterator i = templates.iterator(); i.hasNext(); ) {
            String templ = (String) i.next();
            String content = getContent(templ, servletCtx);
            String newFile = dir + "/ldif/" + templ;
            File file = new File(newFile);
            file.getParentFile().mkdirs();
            try {
                AMSetupServlet.writeToFile(newFile, content);
            } catch (IOException e) {
                Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                        "LDIFTemplates.copy", e);
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
            InputStream inputStream = AMSetupServlet.getResourceAsStream(servletCtx,
                    "/WEB-INF/template/ldif/" + templateName);
            if (inputStream == null) {
                return null;
            }
            fin = new InputStreamReader(inputStream);
            char[] cbuf = new char[1024];
            int len;
            while ((len = fin.read(cbuf)) > 0) {
                sbuf.append(cbuf, 0, len);
            }
        } catch (IOException e) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                    "LDIFTemplates.getContent", e);
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
