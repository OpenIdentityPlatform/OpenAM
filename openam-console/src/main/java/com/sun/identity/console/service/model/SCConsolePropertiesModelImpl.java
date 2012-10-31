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
 * $Id: SCConsolePropertiesModelImpl.java,v 1.2 2008/06/25 05:43:18 qcheng Exp $
 *
 */

package com.sun.identity.console.service.model;

import com.sun.identity.console.base.AMViewConfig;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMServiceProfileModelImpl;
import com.sun.identity.sm.ServiceManager;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/* - NEED NOT LOG - */

public class SCConsolePropertiesModelImpl
    extends AMServiceProfileModelImpl
{
    public SCConsolePropertiesModelImpl(
        HttpServletRequest req,
        String serviceName,
        Map map
    ) throws AMConsoleException {
        super(req, serviceName, map);

        if (ServiceManager.isRealmEnabled()) {
            AMViewConfig config = AMViewConfig.getInstance();
            xmlBuilder.discardAttribute(
                config.getRealmEnableHiddenConsoleAttrNames());
        }
    }
}
