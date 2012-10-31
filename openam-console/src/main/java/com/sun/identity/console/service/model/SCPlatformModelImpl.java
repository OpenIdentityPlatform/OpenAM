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
 * $Id: SCPlatformModelImpl.java,v 1.3 2008/06/25 05:43:18 qcheng Exp $
 *
 */
/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.console.service.model;

import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMServiceProfileModelImpl;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/* - NEED NOT LOG - */

public class SCPlatformModelImpl
    extends AMServiceProfileModelImpl
    implements SCPlatformModel
{
    public static final String SERVICE_NAME =
        AMAdminConstants.PLATFORM_SERVICE;

    /**
     * Creates a simple model using default resource bundle.
     *
     * @param req HTTP Servlet Request
     * @param map of user information
     */
    public SCPlatformModelImpl(HttpServletRequest req, Map map
        ) throws AMConsoleException {
        super(req, SERVICE_NAME, map);
    }

    /**
     * Creates a simple model using default resource bundle.
     *
     * @param req HTTP Servlet Request
     * @param serviceName Name of Service.
     * @param map of user information
     */
    public SCPlatformModelImpl(
        HttpServletRequest req,
        String serviceName,
        Map map
    ) throws AMConsoleException {
        super(req, serviceName, map);
    }

    /**
     * Returns the XML for property sheet view component.
     *
     * @param realmName Name of Realm.
     * @param viewbeanClassName Class Name of View Bean.
     * @param serviceName Name of Service.
     * @return the XML for property sheet view component.
     * @throws AMConsoleException if XML cannot be created.
     */
    public String getPropertySheetXML(
        String realmName,
        String viewbeanClassName,
        String serviceName
    ) throws AMConsoleException {
        String xml = super.getPropertySheetXML(
            realmName, viewbeanClassName, serviceName);
        return xml;
    }
}
