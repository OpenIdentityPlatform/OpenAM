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
 * Portions Copyrighted 2011-2013 ForgeRock AS
 */
package com.sun.identity.setup;

import com.sun.identity.shared.debug.Debug;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletContext;

/**
 * This utility class manages LDIF template files.
 */
public class LDIFTemplates {

    private static final List<String> templates = new ArrayList<String>();
    private static final String PATH_PREFIX = "/WEB-INF/template/ldif/";

    static {
        templates.add("ad/");
        templates.add("adam/");
        templates.add("opendj/");
        templates.add("odsee/");
        templates.add("tivoli/");
        templates.add("sfha/");
    }

    private LDIFTemplates() {
    }

    public static void copy(String dir, ServletContext servletCtx) {
        for (String schemaDir : templates) {
            Set<String> resourcePaths = servletCtx.getResourcePaths(PATH_PREFIX + schemaDir);
            for (String resource : resourcePaths) {
                if (resourcePaths != null) {
                    try {
                        StringBuffer content = AMSetupServlet.readFile(resource);
                        String newPath = dir + "/ldif/" + resource.substring(PATH_PREFIX.length());
                        File file = new File(newPath);
                        file.getParentFile().mkdirs();
                        AMSetupServlet.writeToFile(newPath, content.toString());
                    } catch (IOException ioe) {
                        Debug.getInstance(SetupConstants.DEBUG_NAME).error("LDIFTemplates.copy", ioe);
                    }
                }
            }
        }
    }
}
