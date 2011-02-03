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
 * $Id: WebServiceApplication.java,v 1.1 2009/08/19 05:40:34 veiming Exp $
 */

package com.sun.identity.entitlement;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class WebServiceApplication extends Application {

    protected WebServiceApplication() {
        super();
    }

    /**
     * Constructs an instance.
     *
     * @param name Name of Application.
     * @param applicationType Its application type.
     */
    public WebServiceApplication(
        String realm,
        String name,
        ApplicationType applicationType
    ) {
        super(realm, name, applicationType);
    }

    @Override
    public Application clone() {
        WebServiceApplication clone = new WebServiceApplication();
        cloneAppl(clone);
        return clone;
    }

    /**
     * Initializes the application resources and actions with WSDL.
     *
     * @param wsdl WSDL. It must be an URL (with http:// or https:// prefix)
     *             or a file (with file:// prefix).
     * @throws EntitlementException if WSDL cannot be parsed.
     */
    public void initialize(String wsdl) throws EntitlementException {
        boolean isFile = false;
        boolean isURL = wsdl.startsWith("http://") ||
            wsdl.startsWith("https://");
        if (!isURL) {
            isFile = wsdl.startsWith("file://");
        }

        if (!isURL && !isFile) {
            Object[] param = {wsdl};
            throw new EntitlementException(8, param);
        }

        if (isURL) {
            try {
                new URL(wsdl);
                WSDLParser parser = new WSDLParser();
                parser.parse(wsdl);
                initialize(parser);
            } catch (MalformedURLException ex) {
                Object[] param = {wsdl};
                throw new EntitlementException(8, param);
            }
        } else {
            File file = new File(wsdl.substring(7));
            if (!file.exists()) {
                Object[] param = {wsdl};
                throw new EntitlementException(8, param);
            }
            WSDLParser parser = new WSDLParser();
            parser.parse(file);
            initialize(parser);
        }
    }


    /**
     * Initializes the application resources and actions with WSDL.
     *
     * @param wsdl WSDL
     * @throws EntitlementException if WSDL cannot be parsed.
     */
    public void initialize(InputStream wsdl) throws EntitlementException {
        WSDLParser parser = new WSDLParser();
        parser.parse(wsdl);
        initialize(parser);
    }

    private void initialize(WSDLParser parser) throws EntitlementException {
        Set<String> actions = parser.getOperationNames();
        if (actions != null) {
            Map<String, Boolean> actionMap = new HashMap<String, Boolean>();
            for (String a : actions) {
                actionMap.put(a, true);
            }
            super.setActions(actionMap);
        }

        Set<String> resources = parser.getResources();
        if (resources != null) {
            Set<String> res = new HashSet<String>();
            for (String r : resources) {
                if (!r.endsWith("/")) {
                    r += "/";
                }
                r += "*";
                res.add(r);
            }
            setResources(res);
        }
    }
}
