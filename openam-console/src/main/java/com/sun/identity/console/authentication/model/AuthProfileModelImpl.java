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
 * $Id: AuthProfileModelImpl.java,v 1.2 2008/06/25 05:42:46 qcheng Exp $
 *
 * Portions Copyrighted 2025 3A Systems LLC.
 *
 */

package com.sun.identity.console.authentication.model;

import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMServiceProfileModelImpl;
import com.sun.identity.sm.SchemaType;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import jakarta.servlet.http.HttpServletRequest;

/* - LOG COMPLETE - */ 

public  class AuthProfileModelImpl
    extends AMServiceProfileModelImpl
{
    private static Set AUTH_SCHEMA_TYPES = new HashSet();
    static {
        AUTH_SCHEMA_TYPES.add(SchemaType.ORGANIZATION);
        AUTH_SCHEMA_TYPES.add(SchemaType.DYNAMIC);
    }

    public AuthProfileModelImpl(
        HttpServletRequest req,
        String serviceName,
        Map map
    ) throws AMConsoleException {
        super(req, serviceName, map);
    }

    public Set getDisplaySchemaTypes() {
        return AUTH_SCHEMA_TYPES;
    }
}
